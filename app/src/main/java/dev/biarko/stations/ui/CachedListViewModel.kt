package dev.biarko.stations.ui

import android.database.sqlite.SQLiteException
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.biarko.stations.R
import dev.biarko.stations.data.remote.StationApiException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class CachedListState<T>(
    val items: List<T>? = null,
    val checkingCache: Boolean = true,
    val refreshing: Boolean = false,
    @get:StringRes val error: Int? = null,
)

internal class CachedListViewModel<T>(
    private val cache: Flow<List<T>?>,
    private val updateCache: suspend () -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CachedListState<T>())
    val state = mutableState.asStateFlow()
    private var observer: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (state.value.refreshing) return
        mutableState.update { it.copy(refreshing = true, error = null) }
        if (observer?.isActive != true) {
            observer = viewModelScope.launch {
                try {
                    cache.collect { items -> mutableState.update { it.copy(items = items, checkingCache = false) } }
                } catch (_: SQLiteException) {
                    mutableState.update { it.copy(checkingCache = false, error = R.string.storage_error) }
                }
            }
        }
        viewModelScope.launch {
            val error = try {
                updateCache()
                null
            } catch (failure: StationApiException) {
                when (failure) {
                    is StationApiException.Http -> R.string.server_error
                    is StationApiException.Transport -> R.string.connection_error
                    is StationApiException.InvalidResponse -> R.string.response_error
                }
            } catch (_: SQLiteException) {
                R.string.storage_error
            }
            mutableState.update { it.copy(refreshing = false, error = error ?: it.error) }
        }
    }
}
