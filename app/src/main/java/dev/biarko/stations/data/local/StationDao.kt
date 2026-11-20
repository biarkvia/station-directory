package dev.biarko.stations.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
internal abstract class StationDao {
    @Transaction
    open suspend fun cachedStations(): List<StationSummary>? =
        if (hasCatalog()) stations() else null

    @Transaction
    open suspend fun cachedDevices(stationId: String): List<DeviceEntity>? =
        if (devicesCached(stationId) == true) devices(stationId) else null

    @Transaction
    open suspend fun replaceStations(rows: List<StationSummary>) {
        val incomingIds = rows.mapTo(HashSet()) { it.id }
        for (id in stationIds()) {
            if (id !in incomingIds) deleteStation(id)
        }
        upsertSummaries(rows)
        markCatalogCached(CatalogEntity())
    }

    @Transaction
    open suspend fun replaceDevices(stationId: String, rows: List<DeviceEntity>) {
        if (markDevicesCached(stationId) == 0) return
        clearDevices(stationId)
        insertDevices(rows)
    }

    @Query("SELECT EXISTS(SELECT 1 FROM catalog)")
    protected abstract suspend fun hasCatalog(): Boolean

    @Query("SELECT id, name, address, country, deviceCount, position FROM stations ORDER BY position")
    protected abstract suspend fun stations(): List<StationSummary>

    @Query("SELECT devicesCached FROM stations WHERE id = :stationId")
    protected abstract suspend fun devicesCached(stationId: String): Boolean?

    @Query("SELECT * FROM devices WHERE stationId = :stationId ORDER BY position")
    protected abstract suspend fun devices(stationId: String): List<DeviceEntity>

    @Query("SELECT id FROM stations")
    protected abstract suspend fun stationIds(): List<String>

    @Query("DELETE FROM stations WHERE id = :id")
    protected abstract suspend fun deleteStation(id: String)

    @Upsert(entity = StationEntity::class)
    protected abstract suspend fun upsertSummaries(rows: List<StationSummary>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun markCatalogCached(catalog: CatalogEntity)

    @Query("UPDATE stations SET devicesCached = 1 WHERE id = :stationId")
    protected abstract suspend fun markDevicesCached(stationId: String): Int

    @Query("DELETE FROM devices WHERE stationId = :stationId")
    protected abstract suspend fun clearDevices(stationId: String)

    @Insert
    protected abstract suspend fun insertDevices(rows: List<DeviceEntity>)
}
