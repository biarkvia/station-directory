package dev.biarko.stations.data.remote

import dev.biarko.stations.domain.Device
import dev.biarko.stations.domain.DeviceStatus
import dev.biarko.stations.domain.DeviceType
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
internal data class DeviceDto(
    val id: String,
    val name: String,
    val type: String,
    val status: String,
    val lastSeen: String? = null,
) {
    fun toDevice() = Device(
        id = id,
        name = name,
        type = DeviceType(type),
        status = DeviceStatus(status),
        lastSeen = lastSeen?.let(Instant::parse),
    )
}
