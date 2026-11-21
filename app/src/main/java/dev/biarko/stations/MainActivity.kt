package dev.biarko.stations

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.biarko.stations.ui.DirectoryTheme
import dev.biarko.stations.ui.StationDirectory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as StationsApplication).repository
        setContent { DirectoryTheme { StationDirectory(repository) } }
    }
}
