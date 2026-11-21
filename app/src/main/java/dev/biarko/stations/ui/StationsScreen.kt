package dev.biarko.stations.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.biarko.stations.R
import dev.biarko.stations.domain.Station

@Composable
internal fun StationsScreen(state: CachedListState<Station>, onRefresh: () -> Unit, onOpen: (Station) -> Unit) {
    val subtitle = state.items?.takeIf { it.isNotEmpty() }?.let { stations ->
        val countries = remember(stations) { stations.distinctBy(Station::country).size }
        stringResource(
            R.string.directory_summary,
            pluralStringResource(R.plurals.station_count, stations.size, stations.size),
            pluralStringResource(R.plurals.country_count, countries, countries),
        )
    }
    DirectoryPage(
        title = stringResource(R.string.stations_title),
        subtitle = subtitle,
        state = state,
        noCacheTitle = R.string.no_saved_stations,
        onRefresh = onRefresh,
    ) { stations ->
        if (stations.isEmpty()) item { ListMessage(stringResource(R.string.empty_stations)) }
        items(stations, key = { "station/${it.id.value}" }) { station ->
            DirectoryCard(onClick = { onOpen(station) }) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CodeTile(station.country)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            station.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            station.address,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            pluralStringResource(R.plurals.device_count, station.deviceCount, station.deviceCount),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Icon(
                        painterResource(R.drawable.ic_next), null, Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
