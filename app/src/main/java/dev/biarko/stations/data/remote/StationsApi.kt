package dev.biarko.stations.data.remote

import dev.biarko.stations.domain.Device
import dev.biarko.stations.domain.Station
import dev.biarko.stations.domain.StationId
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import java.io.Closeable
import java.io.IOException
import java.time.format.DateTimeParseException

internal class StationsApi(
    private val stationsUrl: Url = Url("https://test-ucs-mock-api.vercel.app/api/v1/stations"),
    timeoutMillis: Long = 12_000,
) : StationService, Closeable {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(OkHttp) {
        expectSuccess = true
        install(HttpTimeout) { requestTimeoutMillis = timeoutMillis }
    }

    override suspend fun stations(): List<Station> = get(stationsUrl) { body ->
        json.decodeFromString<List<StationDto>>(body).map(StationDto::toStation)
    }

    override suspend fun devices(stationId: StationId): List<Device> {
        val url = URLBuilder(stationsUrl).apply {
            appendPathSegments(stationId.value, "devices", encodeSlash = true)
        }.build()
        return get(url) { body ->
            json.decodeFromString<List<DeviceDto>>(body).map(DeviceDto::toDevice)
        }
    }

    private suspend fun <T> get(url: Url, decode: (String) -> T): T {
        val body = try {
            client.get(url) { accept(ContentType.Application.Json) }.bodyAsText()
        } catch (error: ResponseException) {
            throw StationApiException.Http(url.encodedPath, error.response.status.value, error)
        } catch (error: IOException) {
            currentCoroutineContext().ensureActive()
            throw StationApiException.Transport(url.encodedPath, error)
        }

        return try {
            decode(body)
        } catch (error: IllegalArgumentException) {
            throw StationApiException.InvalidResponse(url.encodedPath, error)
        } catch (error: DateTimeParseException) {
            throw StationApiException.InvalidResponse(url.encodedPath, error)
        }
    }

    override fun close() = client.close()
}
