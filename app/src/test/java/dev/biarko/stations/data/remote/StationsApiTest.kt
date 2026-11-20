package dev.biarko.stations.data.remote

import dev.biarko.stations.domain.DeviceStatus
import dev.biarko.stations.domain.DeviceType
import dev.biarko.stations.domain.Station
import dev.biarko.stations.domain.StationId
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ConnectException
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit

class StationsApiTest {
    private val server = MockWebServer()
    private lateinit var url: Url
    private lateinit var api: StationsApi

    @Before
    fun setUp() {
        server.start()
        url = Url(server.url("/api/v1/stations").toString())
        api = StationsApi(url)
    }

    @After
    fun tearDown() {
        api.close()
        server.close()
    }

    @Test
    fun `stations are read from the top level array and additional fields are ignored`() = runBlocking {
        server.enqueue(MockResponse(body = """
            [{"id":"s1","name":"Praha – Chodov","address":"Roztylská","country":"CZ","deviceCount":3,"region":"Praha"}]
        """.trimIndent()))

        val expected = Station(StationId("s1"), "Praha – Chodov", "Roztylská", "CZ", 3)
        assertEquals(listOf(expected), api.stations())
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/stations", request.target)
    }

    @Test
    fun `devices retain their type status and optional last seen instant`() = runBlocking {
        server.enqueue(MockResponse(body = """
            [
              {"id":"1","name":"Till","type":"POS","status":"ONLINE","lastSeen":"2026-11-15T19:42:00+01:00"},
              {"id":"2","name":"Office","type":"BOS","status":"OFFLINE","lastSeen":null},
              {"id":"3","name":"Server","type":"FCS","status":"ERROR","lastSeen":null},
              {"id":"4","name":"Terminal","type":"OPT","status":"ONLINE","lastSeen":null}
            ]
        """.trimIndent()))

        val devices = api.devices(StationId("s1"))
        val expectedTypes = listOf(DeviceType.POS, DeviceType.BOS, DeviceType.FCS, DeviceType.OPT)
        val expectedStatuses = listOf(
            DeviceStatus.ONLINE, DeviceStatus.OFFLINE, DeviceStatus.ERROR, DeviceStatus.ONLINE,
        )
        assertEquals(expectedTypes, devices.map { it.type })
        assertEquals(expectedStatuses, devices.map { it.status })
        assertEquals(Instant.parse("2026-11-15T18:42:00Z"), devices.first().lastSeen)
        assertNull(devices[1].lastSeen)
    }

    @Test
    fun `future device codes keep their exact spelling`() = runBlocking {
        server.enqueue(MockResponse(body = """
            [{"id":"d5","name":"Charger","type":"EVSE-v2","status":"PendingService","voltage":230}]
        """.trimIndent()))
        val charger = api.devices(StationId("s1")).single()
        assertEquals("d5", charger.id)
        assertEquals("Charger", charger.name)
        assertEquals("EVSE-v2", charger.type.code)
        assertEquals("PendingService", charger.status.code)
        assertNull(charger.lastSeen)
    }

    @Test
    fun `each selected station has its own devices URL`() = runBlocking {
        for ((id, segment) in listOf("praha" to "praha", "brno/01" to "brno%2F01")) {
            server.enqueue(MockResponse(body = "[]"))
            assertTrue(api.devices(StationId(id)).isEmpty())
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/stations/$segment/devices", request.target)
        }
    }

    @Test
    fun `a slow successful response can contain an empty station list`() = runBlocking {
        server.enqueue(MockResponse.Builder().body("[]").headersDelay(2, TimeUnit.SECONDS).build())
        assertTrue(api.stations().isEmpty())
    }

    @Test
    fun `server errors retain the status code and selected station`() {
        for (status in listOf(500, 503, 504)) {
            server.enqueue(MockResponse(code = status, body = "Service unavailable"))
            val error = assertThrows(StationApiException.Http::class.java) {
                runBlocking { api.devices(StationId("s2")) }
            }
            assertEquals("/api/v1/stations/s2/devices", error.path)
            assertEquals(status, error.statusCode)
            assertEquals("GET ${error.path} returned HTTP $status", error.message)
            assertNotNull(error.cause)
        }
    }

    @Test
    fun `a request timeout retains its cause`() {
        server.enqueue(MockResponse.Builder().body("[]").headersDelay(1, TimeUnit.SECONDS).build())
        StationsApi(url, timeoutMillis = 150).use { api ->
            val error = assertThrows(StationApiException.Transport::class.java) {
                runBlocking { api.stations() }
            }
            assertEquals("/api/v1/stations", error.path)
            assertTrue(error.cause is HttpRequestTimeoutException)
        }
    }

    @Test
    fun `an unreachable server reports a transport error for the selected station`() {
        server.close()
        val error = assertThrows(StationApiException.Transport::class.java) {
            runBlocking { api.devices(StationId("s1")) }
        }
        assertEquals("/api/v1/stations/s1/devices", error.path)
        assertTrue(error.cause is ConnectException)
    }

    @Test
    fun `cancelling an in-flight request preserves coroutine cancellation`() = runBlocking {
        server.enqueue(MockResponse.Builder().body("[]").headersDelay(2, TimeUnit.SECONDS).build())
        var failure: Exception? = null
        val job = launch(Dispatchers.IO) {
            try {
                api.stations()
            } catch (error: Exception) {
                failure = error
            }
        }
        assertNotNull(server.takeRequest(3, TimeUnit.SECONDS))
        job.cancelAndJoin()
        assertTrue(failure is CancellationException)
    }

    @Test
    fun `invalid station payloads report the request and original cause`() {
        val negativeCount = """[{"id":"s1","name":"A","address":"B","country":"CZ","deviceCount":-1}]"""
        for (body in listOf("not JSON", "[{}]", negativeCount)) {
            server.enqueue(MockResponse(body = body))
            val error = assertThrows(StationApiException.InvalidResponse::class.java) {
                runBlocking { api.stations() }
            }
            assertEquals("/api/v1/stations", error.path)
            assertNotNull(error.cause)
        }
    }

    @Test
    fun `an invalid last seen timestamp reports the station and parsing error`() {
        server.enqueue(MockResponse(body = """
            [{"id":"d1","name":"Till","type":"POS","status":"ONLINE","lastSeen":"yesterday"}]
        """.trimIndent()))
        val error = assertThrows(StationApiException.InvalidResponse::class.java) {
            runBlocking { api.devices(StationId("s1")) }
        }
        assertEquals("/api/v1/stations/s1/devices", error.path)
        assertTrue(error.cause is DateTimeParseException)
    }
}
