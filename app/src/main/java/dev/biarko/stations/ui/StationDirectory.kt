package dev.biarko.stations.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.biarko.stations.data.StationRepository
import dev.biarko.stations.domain.Device
import dev.biarko.stations.domain.Station
import dev.biarko.stations.domain.StationId
import kotlinx.serialization.Serializable

@Serializable
private data object Stations : NavKey

@Serializable
private data class StationDevices(val id: String) : NavKey

@Composable
internal fun StationDirectory(repository: StationRepository) {
    val backStack = rememberNavBackStack(Stations)
    val catalogModel = viewModel<CachedListViewModel<Station>> {
        CachedListViewModel(repository.stations, repository::refreshStations)
    }
    val catalog by catalogModel.state.collectAsStateWithLifecycle()
    val goBack: () -> Unit = {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                onBack = goBack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<Stations> {
                        StationsScreen(catalog, catalogModel::refresh) { station ->
                            if (backStack.last() == Stations) backStack.add(StationDevices(station.id.value))
                        }
                    }
                    entry<StationDevices> { route ->
                        val id = StationId(route.id)
                        val model = viewModel<CachedListViewModel<Device>> {
                            CachedListViewModel(repository.devices(id)) { repository.refreshDevices(id) }
                        }
                        val state by model.state.collectAsStateWithLifecycle()
                        val station = catalog.items?.find { it.id == id }
                        DevicesScreen(station, catalog.items != null && station == null, state, model::refresh, goBack)
                    }
                },
            )
        }
    }
}
