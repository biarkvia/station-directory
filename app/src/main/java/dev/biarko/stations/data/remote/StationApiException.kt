package dev.biarko.stations.data.remote

import java.io.IOException

internal sealed class StationApiException(
    val path: String,
    message: String,
    cause: Throwable,
) : IOException(message, cause) {
    class Http(path: String, val statusCode: Int, cause: Throwable) :
        StationApiException(path, "GET $path returned HTTP $statusCode", cause)

    class Transport(path: String, cause: IOException) :
        StationApiException(path, "GET $path failed: ${cause.message}", cause)

    class InvalidResponse(path: String, cause: RuntimeException) :
        StationApiException(path, "Invalid response from GET $path: ${cause.message}", cause)
}
