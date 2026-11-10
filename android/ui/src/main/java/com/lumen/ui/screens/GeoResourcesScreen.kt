package com.lumen.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

private const val IRAN_GEO_SOURCE = "https://github.com/Chocolate4U/Iran-sing-box-rules/"

private val GEO_SOURCES = listOf(
    IRAN_GEO_SOURCE
)

@Composable
fun GeoResourcesScreen(
    resources: List<GeoResourceUiModel>,
    source: String,
    isUpdating: Boolean,
    onDownload: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    var selectedSource by remember { mutableStateOf(IRAN_GEO_SOURCE) }
    Column(
        modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 100.dp)
    ) {
        LumenScreenHeader(
            title = s.geoResourceFiles,
            onBack = onBack,
            applyStatusBarPadding = false
        )
        Spacer(Modifier.height(8.dp))
        LumenDropdown(
            label = s.geoSourceLabel,
            options = GEO_SOURCES,
            selected = selectedSource,
            onSelected = { if (!isUpdating) selectedSource = IRAN_GEO_SOURCE },
            optionLabel = { "IRAN  ·  Chocolate4U" }
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onDownload(IRAN_GEO_SOURCE) },
            enabled = !isUpdating,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(if (isUpdating) s.geoUpdating else s.geoDownloadAction)
            }
        }
        Spacer(Modifier.height(16.dp))
        SectionHeader(s.geoResourceFiles)
        if (resources.isEmpty()) {
            SettingsCard {
                Text(
                    s.geoNotLoaded,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        } else {
            resources.forEach { resource ->
                GeoResourceCard(resource)
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GeoResourceCard(resource: GeoResourceUiModel) {
    val art = com.lumen.ui.theme.expressivePalette()
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(art.blue)) {
        com.lumen.ui.components.ExpressiveMotif(com.lumen.ui.components.Motif.Fan, art.onBlue.copy(alpha = .09f),
            Modifier.align(Alignment.BottomEnd).size(112.dp))
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(resource.name, fontFamily = com.lumen.ui.theme.LumenMono,
                style = MaterialTheme.typography.labelLarge, color = art.onBlue)
            Spacer(Modifier.height(18.dp))
            Text(formatGeoBytes(resource.sizeBytes), style = MaterialTheme.typography.headlineLarge, color = art.onBlue)
            Spacer(Modifier.height(6.dp))
            Text(DateFormat.getDateTimeInstance().format(Date(resource.modifiedAt)),
                style = MaterialTheme.typography.bodySmall, color = art.onBlue.copy(alpha = .7f))
        }
    }
}

internal fun formatGeoBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = -1
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return String.format(java.util.Locale.US, "%.1f %s", value, units[unit.coerceAtLeast(0)])
}
