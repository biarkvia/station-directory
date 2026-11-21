package dev.biarko.stations

import android.app.Activity
import android.os.Bundle

class CrashProbeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.post { verifyCrashReporting() }
    }
}

private fun verifyCrashReporting(): Nothing =
    throw IllegalStateException("Station Directory release mapping check")
