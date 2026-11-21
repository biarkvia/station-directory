package dev.biarko.stations.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.biarko.stations.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> DirectoryPage(
    title: String,
    subtitle: String?,
    state: CachedListState<T>,
    @StringRes noCacheTitle: Int,
    onRefresh: () -> Unit,
    onBack: (() -> Unit)? = null,
    content: LazyListScope.(List<T>) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.refreshing && state.items != null,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "header") {
                DirectoryHeader(title, subtitle, state.refreshing, onRefresh, onBack)
            }
            val visibleError = state.error.takeUnless { state.checkingCache }
            visibleError?.let { error ->
                item(key = "error") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                stringResource(if (state.items == null) noCacheTitle else R.string.saved_data),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(stringResource(error), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = onRefresh) { Text(stringResource(R.string.retry)) }
                        }
                    }
                }
            }
            if (state.items != null) {
                content(state.items)
            } else if (visibleError == null) {
                item(key = "loading") {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.loading))
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectoryHeader(
    title: String,
    subtitle: String?,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onBack: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(22.dp), color = colors.inverseSurface, contentColor = colors.inverseOnSurface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back))
                    }
                }
                Text(
                    title,
                    Modifier.weight(1f).semantics { heading() },
                    style = with(MaterialTheme.typography) { if (onBack == null) headlineLarge else headlineSmall },
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onRefresh, enabled = !refreshing) {
                    Icon(
                        painterResource(R.drawable.ic_refresh),
                        stringResource(if (refreshing) R.string.refreshing else R.string.refresh),
                        tint = colors.inversePrimary,
                    )
                }
            }
            if (subtitle != null) {
                Text(subtitle, color = colors.inversePrimary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
internal fun ListMessage(message: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(message, style = MaterialTheme.typography.titleMedium)
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
internal fun CodeTile(code: String) {
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            code,
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun DirectoryCard(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val border = BorderStroke(1.dp, colors.outlineVariant)
    if (onClick == null) {
        Surface(shape = shape, color = colors.surfaceContainerLowest, border = border, content = content)
    } else {
        Surface(
            onClick = onClick, shape = shape, color = colors.surfaceContainerLowest, border = border, content = content,
        )
    }
}
