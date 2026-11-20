package dev.biarko.stations.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.biarko.stations.domain.Station
import dev.biarko.stations.domain.StationId

@Entity(tableName = "catalog")
internal data class CatalogEntity(@PrimaryKey val id: Int = 1)

@Entity(tableName = "stations", primaryKeys = ["id"])
internal data class StationEntity(
    @Embedded val summary: StationSummary,
    @ColumnInfo(defaultValue = "0") val devicesCached: Boolean,
)

internal data class StationSummary(
    val id: String,
    val name: String,
    val address: String,
    val country: String,
    val deviceCount: Int,
    val position: Int,
) {
    fun toStation() = Station(StationId(id), name, address, country, deviceCount)
}

internal fun Station.toSummary(position: Int) =
    StationSummary(id.value, name, address, country, deviceCount, position)
