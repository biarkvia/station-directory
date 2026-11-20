package dev.biarko.stations.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CatalogEntity::class, StationEntity::class, DeviceEntity::class], version = 1)
internal abstract class StationDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao

    companion object {
        fun open(context: Context, name: String = "stations.db"): StationDatabase =
            Room.databaseBuilder(context.applicationContext, StationDatabase::class.java, name).build()
    }
}
