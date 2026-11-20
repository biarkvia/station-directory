package dev.biarko.stations.data.remote

import dev.biarko.stations.domain.Station
import dev.biarko.stations.domain.StationId
import kotlinx.serialization.Serializable

@Serializable
internal data class StationDto(
    val id: String,
    val name: String,
    val address: String,
    val country: String,
    val deviceCount: Int,
) {
    fun toStation() = Station(
        id = StationId(id),
        name = name,
        address = address,
        country = country,
        deviceCount = deviceCount,
    )
}
