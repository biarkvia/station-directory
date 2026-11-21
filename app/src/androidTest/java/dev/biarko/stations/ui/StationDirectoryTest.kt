package dev.biarko.stations.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.biarko.stations.data.StationRepository
import dev.biarko.stations.data.local.StationDatabase
import dev.biarko.stations.data.remote.StationApiException
import dev.biarko.stations.data.remote.StationService
import dev.biarko.stations.domain.Device
import dev.biarko.stations.domain.DeviceStatus
import dev.biarko.stations.domain.DeviceType
import dev.biarko.stations.domain.Station
import dev.biarko.stations.domain.StationId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class StationDirectoryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var database: StationDatabase
    private val prague = Station(StationId("prague"), "Praha", "Roztylská", "CZ", 2)
    private val brno = Station(StationId("brno"), "Brno", "Drážní", "CZ", 0)
    private val till = Device("1", "Pokladna", DeviceType.POS, DeviceStatus.ONLINE, null)
    private val office = Device("2", "Backoffice", DeviceType.BOS, DeviceStatus.OFFLINE, null)
    private val failure = StationApiException.Transport("/api/v1/stations", IOException("Offline"))
    private var fetchStations: suspend () -> List<Station> = { listOf(prague, brno) }
    private var fetchDevices: suspend (StationId) -> List<Device> = { id ->
        if (id == prague.id) listOf(till, office) else emptyList()
    }

    @Before
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(compose.activity, StationDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        compose.activityRule.scenario.close()
        database.close()
    }

    private fun createRepository(): StationRepository {
        val source = object : StationService {
            override suspend fun stations() = fetchStations()
            override suspend fun devices(stationId: StationId) = fetchDevices(stationId)
        }
        return StationRepository(database, source)
    }

    private fun openDirectory(repository: StationRepository = createRepository()) {
        compose.setContent { DirectoryTheme { StationDirectory(repository) } }
    }

    private fun awaitText(text: String) {
        compose.waitUntil(8_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun chooseStatus(status: String) =
        compose.onNode(hasText(status) and hasClickAction()).performClick()

    @Test
    fun networkFailureWaitsForTheFirstCacheReadBeforeReportingMissingData() {
        val diskRead = CompletableDeferred<List<Station>>()
        compose.setContent {
            val model = viewModel<CachedListViewModel<Station>> {
                CachedListViewModel(flow { emit(diskRead.await()) }) { throw failure }
            }
            DirectoryTheme {
                StationsScreen(model.state.collectAsStateWithLifecycle().value, model::refresh) {}
            }
        }
        awaitText("Loading…")
        compose.onNodeWithText("No saved station list").assertDoesNotExist()
        diskRead.complete(listOf(prague))
        awaitText("Showing saved data")
        compose.onNodeWithText("Praha").assertIsDisplayed()
    }

    @Test
    fun deviceFiltersCanBeClearedAndBackReturnsToTheCatalog() {
        openDirectory()
        awaitText("Praha")
        compose.onNodeWithText("Praha").performClick()
        awaitText("Pokladna")
        chooseStatus("Offline")
        compose.onNodeWithText("Backoffice").assertIsDisplayed()
        compose.onNodeWithText("Pokladna").assertDoesNotExist()
        chooseStatus("Error")
        compose.onNodeWithText("Show all devices").performClick()
        compose.onNodeWithText("All").assertIsSelected()
        compose.onNodeWithText("Pokladna").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        awaitText("Brno")
        compose.onNodeWithText("Brno").performClick()
        awaitText("No devices at this station")
        compose.onNodeWithContentDescription("Back to stations").performClick()
        awaitText("Praha")
    }

    @Test
    fun firstLoadFailureCanBeRetriedAndAnEmptyResponseIsDisplayed() {
        val response = CompletableDeferred<List<Station>>()
        fetchStations = { response.await() }
        openDirectory()
        awaitText("Loading…")
        response.completeExceptionally(failure)
        awaitText("No saved station list")
        compose.runOnIdle { fetchStations = { emptyList() } }
        compose.onNodeWithText("Retry").performClick()
        awaitText("No stations in the directory")
        compose.onNodeWithText("No saved station list").assertDoesNotExist()
    }

    @Test
    fun pullingToRefreshKeepsTheCatalogVisibleAfterHttp503AndRetryReplacesIt() {
        openDirectory()
        awaitText("Praha")
        val response = CompletableDeferred<List<Station>>()
        val requestStarted = CompletableDeferred<Unit>()
        compose.runOnIdle {
            fetchStations = {
                requestStarted.complete(Unit)
                response.await()
            }
        }
        compose.onNode(hasScrollAction()).performTouchInput { swipeDown() }
        compose.waitUntil(5_000) { requestStarted.isCompleted }
        compose.onNodeWithText("Praha").assertIsDisplayed()
        response.completeExceptionally(StationApiException.Http("/api/v1/stations", 503, IOException("Unavailable")))
        awaitText("Showing saved data")
        compose.onNodeWithText("The server couldn’t complete the request. Try again.").assertIsDisplayed()
        compose.onNodeWithText("Praha").assertIsDisplayed()
        compose.runOnIdle { fetchStations = { listOf(brno) } }
        compose.onNodeWithText("Retry").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Praha")).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Brno").assertIsDisplayed()
    }

    @Test
    fun deviceRefreshUsesTheSelectedIdAndFutureStatusesRemainFilterable() {
        var selectedId: StationId? = null
        fetchDevices = { id ->
            selectedId = id
            throw failure
        }
        openDirectory()
        awaitText("Brno")
        compose.onNodeWithText("Brno").performClick()
        awaitText("No saved device list for this station")
        assertEquals(brno.id, selectedId)
        val charger = Device("3", "Charger", DeviceType("EVSE"), DeviceStatus("PendingService"), null)
        compose.runOnIdle { fetchDevices = { listOf(charger) } }
        compose.onNodeWithText("Retry").performClick()
        awaitText("Charger")
        chooseStatus("PendingService")
        compose.onNodeWithText("Charger").assertIsDisplayed()
        compose.onNodeWithText("Last seen unknown").assertIsDisplayed()
        compose.runOnIdle { fetchDevices = { throw failure } }
        compose.onNodeWithContentDescription("Refresh").performClick()
        awaitText("Showing saved data")
        compose.onNodeWithText("Charger").assertIsDisplayed()
    }

    @Test
    fun latePragueResponseCannotReplaceDevicesOnTheBrnoScreen() {
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<List<Device>>()
        val refreshFinished = CompletableDeferred<Unit>()
        fetchDevices = { id ->
            if (id == prague.id) {
                currentCoroutineContext().job.invokeOnCompletion { refreshFinished.complete(Unit) }
                started.complete(Unit)
                withContext(NonCancellable) { response.await() }
            } else {
                listOf(office)
            }
        }

        try {
            openDirectory()
            awaitText("Praha")
            compose.onNodeWithText("Praha").performClick()
            compose.waitUntil(5_000) { started.isCompleted }
            compose.onNodeWithContentDescription("Back to stations").performClick()
            awaitText("Brno")
            compose.onNodeWithText("Brno").performClick()
            awaitText("Backoffice")
            response.complete(listOf(till))
            compose.waitUntil(5_000) { refreshFinished.isCompleted }
            compose.onNodeWithText("Brno").assertIsDisplayed()
            compose.onNodeWithText("Backoffice").assertIsDisplayed()
            compose.onNodeWithText("Pokladna").assertDoesNotExist()
        } finally {
            response.complete(listOf(till))
        }
    }

    @Test
    fun offlineCatalogDistinguishesAnUnvisitedStationFromASavedEmptyList() {
        val repository = createRepository()
        runBlocking {
            repository.refreshStations()
            repository.refreshDevices(brno.id)
        }
        fetchStations = { throw failure }
        fetchDevices = { throw failure }
        openDirectory(repository)
        awaitText("Showing saved data")
        compose.onNodeWithText("Brno").performClick()
        awaitText("No devices at this station")
        awaitText("Showing saved data")
        compose.onNodeWithContentDescription("Back to stations").performClick()
        awaitText("Praha")
        compose.onNodeWithText("Praha").performClick()
        awaitText("No saved device list for this station")
        compose.onNodeWithText("No devices at this station").assertDoesNotExist()

        compose.runOnIdle { fetchDevices = { listOf(till) } }
        compose.onNodeWithText("Retry").performClick()
        awaitText("Pokladna")
        compose.onNodeWithText("No saved device list for this station").assertDoesNotExist()
        compose.onNodeWithText("Showing saved data").assertDoesNotExist()
    }
}
