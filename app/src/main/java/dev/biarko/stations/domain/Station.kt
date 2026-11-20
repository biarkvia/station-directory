package dev.biarko.stations.domain

@JvmInline
internal value class StationId(val value: String)

internal data class Station(
    val id: StationId,
    val name: String,
    val address: String,
    val country: String,
    val deviceCount: Int,
) {
    init {
        require(deviceCount >= 0) { "Station ${id.value} has a negative device count: $deviceCount" }
    }
}
