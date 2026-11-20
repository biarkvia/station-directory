package dev.biarko.stations

import android.app.Application
import dev.biarko.stations.data.StationRepository
import dev.biarko.stations.data.local.StationDatabase
import dev.biarko.stations.data.remote.StationsApi

class StationsApplication : Application() {
    internal val repository by lazy {
        StationRepository(StationDatabase.open(this), StationsApi())
    }
}
