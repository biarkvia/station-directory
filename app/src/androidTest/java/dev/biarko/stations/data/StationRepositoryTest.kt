package dev.biarko.stations.data

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.biarko.stations.data.local.StationDatabase
import dev.biarko.stations.data.remote.StationApiException
import dev.biarko.stations.data.remote.StationService
import dev.biarko.stations.domain.Device
import dev.biarko.stations.domain.DeviceStatus
import dev.biarko.stations.domain.DeviceType
import dev.biarko.stations.domain.Station
import dev.biarko.stations.domain.StationId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.Instant
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class StationRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "test-${UUID.randomUUID()}.db"
    private val prague = Station(StationId("p"), "Praha", "Roztylská", "CZ", 2)
    private val brno = Station(StationId("b"), "Brno", "Drážní", "CZ", 1)
    private val till = Device("1", "Till", DeviceType.POS, DeviceStatus.ONLINE, null)
    private val charger = Device(
        "2", "Charger", DeviceType("EVSE-v2"), DeviceStatus("PendingService"),
        Instant.parse("2026-11-15T18:42:00.123456789Z"),
    )
    private var fetchStations: suspend () -> List<Station> = { listOf(prague, brno) }
    private var fetchDevices: suspend (StationId) -> List<Device> = { error("Unexpected request for ${it.value}") }
    private val service = object : StationService {
        override suspend fun stations() = fetchStations()
        override suspend fun devices(stationId: StationId) = fetchDevices(stationId)
    }
    private lateinit var database: StationDatabase
    private lateinit var repository: StationRepository

    @Before
    fun openDatabase() {
        database = StationDatabase.open(context, databaseName)
        repository = StationRepository(database, service)
    }

    @After
    fun closeDatabase() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    private fun reopenDatabase() {
        database.close()
        openDatabase()
    }

    @Test
    fun cachedListsKeepServerOrderAndDeviceValuesAfterReopening() = runBlocking {
        repository.refreshStations()
        fetchDevices = { listOf(charger, till) }
        repository.refreshDevices(prague.id)

        reopenDatabase()
        fetchStations = { error("Offline") }
        fetchDevices = { error("Offline") }
        assertEquals(listOf(prague, brno), repository.stations.first())
        assertEquals(listOf(charger, till), repository.devices(prague.id).first())
        assertNull(repository.devices(brno.id).first())
    }

    @Test
    fun emptyDirectorySurvivesReopeningAndRemovesOldDeviceCaches() = runBlocking {
        assertNull(repository.stations.first())
        repository.refreshStations()
        fetchDevices = { listOf(till) }
        repository.refreshDevices(prague.id)
        fetchStations = { emptyList() }
        repository.refreshStations()

        reopenDatabase()
        assertEquals(emptyList<Station>(), repository.stations.first())
        fetchStations = { listOf(prague) }
        repository.refreshStations()
        assertNull(repository.devices(prague.id).first())
    }

    @Test
    fun emptyDeviceListReplacesOnlyItsStationAndSurvivesCatalogRefresh() = runBlocking {
        repository.refreshStations()
        fetchDevices = { id -> listOf(till.copy(name = id.value)) }
        repository.refreshDevices(prague.id)
        repository.refreshDevices(brno.id)
        fetchDevices = { emptyList() }
        repository.refreshDevices(prague.id)
        val renamed = brno.copy(name = "Brno – Slatina")
        fetchStations = { listOf(renamed, prague) }
        repository.refreshStations()

        reopenDatabase()
        assertEquals(listOf(renamed, prague), repository.stations.first())
        assertEquals(emptyList<Device>(), repository.devices(prague.id).first())
        assertEquals(listOf(till.copy(name = brno.id.value)), repository.devices(brno.id).first())
    }

    @Test
    fun failedRequestsPreserveBothCachedAndNeverFetchedLists() {
        runBlocking {
            repository.refreshStations()
            fetchDevices = { listOf(till) }
            repository.refreshDevices(prague.id)
        }
        val unavailable = StationApiException.Http("/api/v1/stations", 503, IOException("Unavailable"))
        fetchStations = { throw unavailable }
        fetchDevices = { throw unavailable }
        assertThrows(StationApiException.Http::class.java) { runBlocking { repository.refreshStations() } }
        for (id in listOf(prague.id, brno.id)) {
            assertThrows(StationApiException.Http::class.java) { runBlocking { repository.refreshDevices(id) } }
        }
        runBlocking {
            assertEquals(listOf(prague, brno), repository.stations.first())
            assertEquals(listOf(till), repository.devices(prague.id).first())
            assertNull(repository.devices(brno.id).first())
        }
    }

    @Test
    fun failedInsertRollsBackDeletionAndTheCachedFlag() {
        runBlocking {
            repository.refreshStations()
            fetchDevices = { listOf(charger) }
            repository.refreshDevices(prague.id)
        }
        fetchDevices = { listOf(till, till) }
        for (id in listOf(prague.id, brno.id)) {
            assertThrows(SQLiteConstraintException::class.java) { runBlocking { repository.refreshDevices(id) } }
        }
        runBlocking {
            assertEquals(listOf(charger), repository.devices(prague.id).first())
            assertNull(repository.devices(brno.id).first())
        }
    }

    @Test(timeout = 15_000)
    fun lateDeviceResponseCannotRestoreARemovedStation() = runBlocking {
        repository.refreshStations()
        val response = CompletableDeferred<List<Device>>()
        fetchDevices = { response.await() }
        val pending = async(start = CoroutineStart.UNDISPATCHED) { repository.refreshDevices(prague.id) }
        fetchStations = { listOf(brno) }
        repository.refreshStations()
        response.complete(listOf(till))
        pending.await()

        assertEquals(listOf(brno), repository.stations.first())
        fetchStations = { listOf(prague, brno) }
        repository.refreshStations()
        assertNull(repository.devices(prague.id).first())
    }

    @Test(timeout = 15_000)
    fun overlappingRefreshesKeepTheLatestDataAndAllowOtherStationsToLoad() = runBlocking {
        repository.refreshStations()
        val firstResponse = CompletableDeferred<List<Device>>()
        fetchDevices = { firstResponse.await() }
        val first = async(start = CoroutineStart.UNDISPATCHED) { repository.refreshDevices(prague.id) }
        fetchDevices = { listOf(charger) }
        val second = async(start = CoroutineStart.UNDISPATCHED) { repository.refreshDevices(prague.id) }
        repository.refreshDevices(brno.id)
        assertEquals(listOf(charger), repository.devices(brno.id).first())
        firstResponse.complete(listOf(till))
        first.await()
        second.await()
        assertEquals(listOf(charger), repository.devices(prague.id).first())
    }

    @Test(timeout = 15_000)
    fun observerReceivesCompletedSnapshotsIncludingAnEmptyList() = runBlocking {
        val snapshots = Channel<List<Station>?>(Channel.UNLIMITED)
        val observer = launch { repository.stations.collect { snapshots.send(it) } }
        try {
            assertNull(snapshots.receive())
            repository.refreshStations()
            assertEquals(listOf(prague, brno), snapshots.receive())
            fetchStations = { emptyList() }
            repository.refreshStations()
            assertEquals(emptyList<Station>(), snapshots.receive())
        } finally {
            observer.cancelAndJoin()
        }
    }
}
