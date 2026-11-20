package dev.biarko.stations.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StationTest {
    private val station = Station(StationId("chodov"), "Chodov", "Roztylská", "CZ", 0)

    @Test
    fun `a station can have no devices`() {
        assertEquals(0, station.deviceCount)
    }

    @Test
    fun `a negative device count identifies the invalid station`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            station.copy(deviceCount = -1)
        }
        assertEquals("Station chodov has a negative device count: -1", error.message)
    }
}
