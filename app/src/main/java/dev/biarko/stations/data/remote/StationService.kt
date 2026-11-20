package dev.biarko.stations.data.remote

import dev.biarko.stations.domain.Device
import dev.biarko.stations.domain.Station
import dev.biarko.stations.domain.StationId

internal interface StationService {
    suspend fun stations(): List<Station>
    suspend fun devices(stationId: StationId): List<Device>
}
