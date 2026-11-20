package dev.biarko.stations.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import dev.biarko.stations.domain.Device
import dev.biarko.stations.domain.DeviceStatus
import dev.biarko.stations.domain.DeviceType
import java.time.Instant

@Entity(
    tableName = "devices",
    primaryKeys = ["stationId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = StationEntity::class,
            parentColumns = ["id"],
            childColumns = ["stationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class DeviceEntity(
    val stationId: String,
    val id: String,
    val name: String,
    val type: String,
    val status: String,
    val lastSeen: String?,
    val position: Int,
) {
    fun toDevice() = Device(id, name, DeviceType(type), DeviceStatus(status), lastSeen?.let(Instant::parse))
}

internal fun Device.toEntity(stationId: String, position: Int) =
    DeviceEntity(stationId, id, name, type.code, status.code, lastSeen?.toString(), position)
