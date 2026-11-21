package dev.biarko.stations.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.biarko.stations.R
import dev.biarko.stations.domain.Device
import dev.biarko.stations.domain.DeviceStatus
import dev.biarko.stations.domain.Station
import java.text.DateFormat
import java.util.Date

@Composable
internal fun DevicesScreen(
    station: Station?,
    removed: Boolean,
    state: CachedListState<Device>,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
) {
    if (removed) {
        ListMessage(stringResource(R.string.station_removed), stringResource(R.string.back), onBack)
        return
    }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val devices = state.items.orEmpty()
    val visible = remember(devices, selected) { devices.filter { selected == null || it.status.code == selected } }
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale) }
    DirectoryPage(
        title = station?.name ?: stringResource(R.string.devices_title),
        subtitle = station?.let { stringResource(R.string.station_address, it.address, it.country) },
        state = state,
        noCacheTitle = R.string.no_saved_devices,
        onRefresh = onRefresh,
        onBack = onBack,
    ) { all ->
        if (all.isNotEmpty() || selected != null) {
            item(key = "filters") {
                Column(Modifier.padding(vertical = 4.dp)) {
                    Text(
                        if (selected == null) pluralStringResource(R.plurals.device_count, all.size, all.size)
                        else stringResource(R.string.filtered_count, visible.size, all.size),
                        Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    StatusFilters(all, selected) { selected = it }
                }
            }
        }
        if (all.isEmpty()) {
            item { ListMessage(stringResource(R.string.empty_devices)) }
        } else if (visible.isEmpty()) {
            item {
                ListMessage(
                    stringResource(R.string.no_matching_devices, statusLabel(selected.orEmpty())),
                    stringResource(R.string.clear_filter),
                ) { selected = null }
            }
        } else {
            items(visible, key = { "device/${it.id}" }) { device -> DeviceCard(device, formatter) }
        }
    }
}

@Composable
private fun StatusFilters(devices: List<Device>, selected: String?, select: (String?) -> Unit) {
    val statuses = remember(devices, selected) {
        buildSet {
            add(DeviceStatus.ONLINE.code)
            add(DeviceStatus.OFFLINE.code)
            add(DeviceStatus.ERROR.code)
            devices.forEach { add(it.status.code) }
            selected?.let(::add)
        }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
            selected = selected == null,
            onClick = { select(null) },
            label = { Text(stringResource(R.string.all_statuses)) },
        )
        statuses.forEach { code ->
            FilterChip(selected = selected == code, onClick = { select(code) }, label = { Text(statusLabel(code)) })
        }
    }
}

@Composable
private fun statusLabel(code: String): String = when (DeviceStatus(code)) {
    DeviceStatus.ONLINE -> stringResource(R.string.online)
    DeviceStatus.OFFLINE -> stringResource(R.string.offline)
    DeviceStatus.ERROR -> stringResource(R.string.device_error)
    else -> code
}

@Composable
private fun DeviceCard(device: Device, formatter: DateFormat) {
    val colors = MaterialTheme.colorScheme
    DirectoryCard {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CodeTile(device.type.code)
                Row(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val dot = when (device.status) {
                        DeviceStatus.ONLINE -> colors.primary
                        DeviceStatus.ERROR -> colors.error
                        else -> colors.onSurfaceVariant
                    }
                    Box(Modifier.size(8.dp).background(dot, CircleShape))
                    Text(statusLabel(device.status.code), style = MaterialTheme.typography.labelLarge)
                }
            }
            val lastSeen = device.lastSeen?.let { stringResource(R.string.last_seen, formatter.format(Date.from(it))) }
            Text(
                lastSeen ?: stringResource(R.string.last_seen_unknown),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
