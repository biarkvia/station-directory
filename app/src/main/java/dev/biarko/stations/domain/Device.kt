package dev.biarko.stations.domain

import java.time.Instant

internal data class Device(
    val id: String,
    val name: String,
    val type: DeviceType,
    val status: DeviceStatus,
    val lastSeen: Instant?,
)

@JvmInline
internal value class DeviceType(val code: String) {
    companion object {
        val FCS = DeviceType("FCS")
        val BOS = DeviceType("BOS")
        val POS = DeviceType("POS")
        val OPT = DeviceType("OPT")
    }
}

@JvmInline
internal value class DeviceStatus(val code: String) {
    companion object {
        val ONLINE = DeviceStatus("ONLINE")
        val OFFLINE = DeviceStatus("OFFLINE")
        val ERROR = DeviceStatus("ERROR")
    }
}
