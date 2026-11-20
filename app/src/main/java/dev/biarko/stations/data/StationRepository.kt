package dev.biarko.stations.data

import dev.biarko.stations.data.local.DeviceEntity
import dev.biarko.stations.data.local.StationDatabase
import dev.biarko.stations.data.local.StationSummary
import dev.biarko.stations.data.local.toEntity
import dev.biarko.stations.data.local.toSummary
import dev.biarko.stations.data.remote.StationService
import dev.biarko.stations.domain.Device
import dev.biarko.stations.domain.Station
import dev.biarko.stations.domain.StationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

internal class StationRepository(
    database: StationDatabase,
    private val service: StationService,
) {
    private val dao = database.stationDao()
    private val changes = database.invalidationTracker
    private val catalogRefresh = Mutex()
    private val deviceRefreshes = ConcurrentHashMap<StationId, Mutex>()

    val stations: Flow<List<Station>?> = changes.createFlow("catalog", "stations")
        .map { dao.cachedStations()?.map(StationSummary::toStation) }
        .distinctUntilChanged()

    fun devices(stationId: StationId): Flow<List<Device>?> = changes.createFlow("stations", "devices")
        .map { dao.cachedDevices(stationId.value)?.map(DeviceEntity::toDevice) }
        .distinctUntilChanged()

    suspend fun refreshStations() = catalogRefresh.withLock {
        val rows = service.stations().mapIndexed { position, station -> station.toSummary(position) }
        dao.replaceStations(rows)
    }

    suspend fun refreshDevices(stationId: StationId) {
        deviceRefreshes.getOrPut(stationId) { Mutex() }.withLock {
            val rows = service.devices(stationId).mapIndexed { position, device ->
                device.toEntity(stationId.value, position)
            }
            dao.replaceDevices(stationId.value, rows)
        }
    }
}
