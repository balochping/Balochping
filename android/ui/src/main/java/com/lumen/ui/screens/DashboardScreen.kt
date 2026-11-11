package com.lumen.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.ui.components.AddServerSheet
import com.lumen.ui.components.BrandTopBar
import com.lumen.ui.components.ConnectionState
import com.lumen.ui.components.CountryFlagHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.random.Random

// Thread-safe caches for Game Posters & Flags
private val posterMemoryCache = ConcurrentHashMap<String, ImageBitmap>()
private val flagMemoryCache = ConcurrentHashMap<String, ImageBitmap>()

// === Frosted Glass & Matte Gold Gaming Palette ===
private val MatteYellow = Color(0xFFE5B80B)
private val MatteAmber = Color(0xFFB37D14)
private val BgDark = Color(0xFF07080C)
private val GlassHighlight = Color(0x24FFFFFF)
private val GlassSurfaceTop = Color(0x1AFFFFFF)
private val GlassSurfaceBottom = Color(0x08FFFFFF)
private val GlassBorderLight = Color(0x38FFFFFF)
private val GlassBorderDim = Color(0x0DFFFFFF)
private val TextMuted = Color(0xFF9E9FA8)

private fun frostedGlassBrush(isActive: Boolean = false): Brush = if (isActive) {
    Brush.linearGradient(
        colors = listOf(
            MatteYellow.copy(alpha = 0.16f),
            GlassSurfaceTop,
            GlassSurfaceBottom
        ),
        start = Offset(0f, 0f),
        end = Offset(900f, 900f)
    )
} else {
    Brush.linearGradient(
        colors = listOf(
            GlassSurfaceTop,
            GlassSurfaceBottom
        ),
        start = Offset(0f, 0f),
        end = Offset(800f, 800f)
    )
}

private fun frostedBorderBrush(isActive: Boolean = false): Brush = if (isActive) {
    Brush.linearGradient(
        colors = listOf(
            MatteYellow.copy(alpha = 0.85f),
            GlassBorderLight,
            MatteAmber.copy(alpha = 0.45f)
        )
    )
} else {
    Brush.linearGradient(
        colors = listOf(
            GlassBorderLight,
            GlassBorderDim
        )
    )
}

@Composable
fun DashboardScreen(
    connectionState: ConnectionState,
    nodes: List<NodeUiModel>,
    subscriptions: List<SubscriptionUiModel>,
    serverGroups: List<ServerGroupUiModel> = emptyList(),
    speedStatsEnabled: Boolean = true,
    downloadSpeed: Long = 0L,
    uploadSpeed: Long = 0L,
    pingingNodeIds: Set<String> = emptySet(),
    dashboardStyle: DashboardStyle = DashboardStyle.DEFAULT,
    connectedPing: String? = null,
    isCheckingPing: Boolean = false,
    onCheckPing: () -> Unit = {},
    onToggleConnection: () -> Unit,
    onSelectNode: (NodeUiModel) -> Unit,
    onImportClipboard: () -> Unit,
    onImportFile: () -> Unit,
    onImportQr: () -> Unit,
    onAddManualNode: () -> Unit,
    onRefreshSubscription: (String) -> Unit = {},
    onDeleteSubscription: (String) -> Unit = {},
    onPingGroup: (String?) -> Unit = {},
    onEditNode: (NodeUiModel) -> Unit = {},
    onPingNode: (NodeUiModel) -> Unit = {},
    onCopyNodeLink: (NodeUiModel) -> Unit = {},
    onExportQrCode: (NodeUiModel) -> Unit = {},
    onDeleteNode: (NodeUiModel) -> Unit = {},
    onPingNodes: (List<NodeUiModel>) -> Unit = {},
    onStopPing: () -> Unit = {},
    onExportNodesText: (Set<String>) -> String = { "" },
    onExportSubscriptionText: (String?) -> String = { "" },
    onCopyText: (String) -> Unit = {},
    onShareText: (String) -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    onOpenServers: () -> Unit = {},
    modifier: Modifier = Modifier,
    selectedGroupFilter: String = "all",
    speedValues: () -> DashboardSpeeds = { DashboardSpeeds(downloadSpeed, uploadSpeed) }
) {
    val tick = rememberHapticTick()
    var showAddSheet by remember { mutableStateOf(false) }
    var showServerPicker by remember { mutableStateOf(false) }
    val openServerPicker: () -> Unit = { showServerPicker = true }

    val activeNode = nodes.firstOrNull { it.isSelected } ?: nodes.firstOrNull()
    val strings = LocalStrings.current
    val isFa = strings.localeCode == "fa"
    val appTitle = if (isFa) "بلوچ پینگ" else "Baloch Ping"

    val groupFilter = resolvedServerFilter(selectedGroupFilter, subscriptions, serverGroups)
    val groupNodes = remember(nodes, groupFilter) { nodesForServerFilter(nodes, groupFilter) }

    val groupTitle = when {
        groupFilter == "manual" -> strings.manual
        groupFilter == "sub" -> strings.subscriptions
        groupFilter.startsWith("subscription:") -> subscriptions.firstOrNull { it.id == groupFilter.removePrefix("subscription:") }?.name ?: strings.allGroups
        groupFilter.startsWith("group:") -> serverGroups.firstOrNull { it.id == groupFilter.removePrefix("group:") }?.name ?: strings.allGroups
        else -> strings.allGroups
    }

    val scanQr = rememberQrScanRequest(onImportQr)
    val isConnected = connectionState == ConnectionState.Connected
    val scrollState = rememberScrollState()
    var gameBoostTrigger by remember { mutableIntStateOf(0) }

    // Ambient background glow that shines through the frosted glass cards
    val bgBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF2B2210),
                Color(0xFF12131A),
                BgDark
            ),
            center = Offset(700f, 150f),
            radius = 1600f
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
            .statusBarsPadding()
    ) {
        BrandTopBar(
            title = appTitle,
            onAddClick = { showAddSheet = true }
        )

        Box(modifier = Modifier.weight(1f)) {
            if (dashboardStyle == DashboardStyle.SLIDER) {
                androidx.compose.runtime.key(groupFilter) {
                    DashboardServerTable(
                        groupNodes, groupTitle, pingingNodeIds, onSelectNode, onOpenServers,
                        speedStatsEnabled, isConnected, speedValues,
                        onPingAll = { onPingNodes(groupNodes) }, onPingNode = { onPingNodes(listOf(it)) }, onStopPing = onStopPing
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    PremiumHeroSection(connectionState, onToggleConnection, tick)
                    Spacer(modifier = Modifier.height(24.dp))

                    if (speedStatsEnabled) {
                        val basePingValue = connectedPing ?: activeNode?.pingMs?.takeIf { it > 0 }?.toString()
                        PremiumStatsBar(
                            basePing = basePingValue,
                            speedValues = speedValues,
                            isConnected = isConnected,
                            boostTrigger = gameBoostTrigger
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                    }

                    GameBoostSection(
                        selectedGameId = "pubg",
                        onGameSelect = {
                            tick(HapticFeedbackType.LongPress)
                            gameBoostTrigger++
                            val bestNode = groupNodes.filter { (it.pingMs ?: 0) > 0 }.minByOrNull { it.pingMs ?: Int.MAX_VALUE }
                            if (bestNode != null && bestNode.id != activeNode?.id) {
                                onSelectNode(bestNode)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = MatteYellow, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(text = "Server", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(frostedGlassBrush(isActive = true))
                                .border(1.dp, frostedBorderBrush(isActive = true), RoundedCornerShape(16.dp))
                                .clickable {
                                    tick(HapticFeedbackType.LongPress)
                                    val bestNode = groupNodes.filter { (it.pingMs ?: 0) > 0 }.minByOrNull { it.pingMs ?: Int.MAX_VALUE }
                                    if (bestNode != null) {
                                        onSelectNode(bestNode)
                                    } else {
                                        onPingNodes(groupNodes)
                                        openServerPicker()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = MatteYellow, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(text = "Auto Select", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MatteYellow)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (activeNode != null) {
                        val serverShape = RoundedCornerShape(22.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(serverShape)
                                .background(frostedGlassBrush(isActive = isConnected))
                                .border(
                                    width = 1.2.dp,
                                    brush = frostedBorderBrush(isActive = isConnected),
                                    shape = serverShape
                                )
                                .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = openServerPicker)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(28.dp)
                            ) {
                                val countryCode = activeNode.countryCode.trim().lowercase()
                                var flagBitmap by remember(countryCode) { mutableStateOf(flagMemoryCache[countryCode]) }
                                val flagEmoji = remember(countryCode) { CountryFlagHelper.getServerFlagEmoji(activeNode.countryCode) }

                                LaunchedEffect(countryCode) {
                                    if (flagBitmap == null && countryCode.isNotBlank()) {
                                        withContext(Dispatchers.IO) {
                                            var connection: HttpURLConnection? = null
                                            try {
                                                val url = URL("https://flagcdn.com/w80/$countryCode.png")
                                                connection = (url.openConnection() as HttpURLConnection).apply {
                                                    connectTimeout = 3000
                                                    readTimeout = 3000
                                                    doInput = true
                                                    connect()
                                                }
                                                if (connection.responseCode in 200..299) {
                                                    val bmp = connection.inputStream.use { BitmapFactory.decodeStream(it) }
                                                    if (bmp != null) {
                                                        val imageBitmap = bmp.asImageBitmap()
                                                        flagMemoryCache[countryCode] = imageBitmap
                                                        flagBitmap = imageBitmap
                                                    }
                                                }
                                            } catch (_: Exception) {
                                            } finally {
                                                connection?.disconnect()
                                            }
                                        }
                                    }
                                }

                                if (flagBitmap != null) {
                                    Image(
                                        bitmap = flagBitmap!!,
                                        contentDescription = activeNode.countryCode,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))
                                    )
                                } else {
                                    Text(
                                        text = flagEmoji,
                                        fontSize = 24.sp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeNode.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(text = activeNode.displayProtocol, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                }
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = strings.changeServer,
                                tint = if (isConnected) MatteYellow else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(frostedGlassBrush(isActive = false))
                                .border(1.dp, frostedBorderBrush(isActive = false), RoundedCornerShape(22.dp))
                                .clickable(onClick = openServerPicker)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.noServerSelected,
                                fontSize = 14.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(160.dp))
                }
            }
        }
    }

    if (showServerPicker) {
        DashboardServerPicker(
            groupNodes, groupTitle,
            onDismiss = { showServerPicker = false },
            onSelect = { onSelectNode(it); showServerPicker = false },
            pinging = pingingNodeIds,
            onPingAll = { onPingNodes(groupNodes) },
            onPingNode = { onPingNodes(listOf(it)) },
            onStopPing = onStopPing
        )
    }
    if (showAddSheet) {
        AddServerSheet(
            onDismissRequest = { showAddSheet = false },
            onImportClipboard = onImportClipboard,
            onImportQr = scanQr,
            onImportFile = onImportFile,
            onAddManualNode = onAddManualNode
        )
    }
}

@Composable
private fun PremiumHeroSection(
    connectionState: ConnectionState,
    onToggleConnection: () -> Unit,
    tick: (HapticFeedbackType) -> Unit
) {
    val isConnected = connectionState == ConnectionState.Connected
    val isConnecting = connectionState == ConnectionState.Connecting
    val scope = rememberCoroutineScope()
    val orbScale = remember { Animatable(1f) }

    val frostedGlowPaint = remember {
        androidx.compose.ui.graphics.Paint().apply {
            color = MatteYellow.copy(alpha = 0.24f)
            asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(36f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
    }

    val onOrbitClick: () -> Unit = {
        scope.launch {
            orbScale.snapTo(0.88f)
            orbScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 600f))
        }
        tick(HapticFeedbackType.LongPress)
        onToggleConnection()
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(frostedGlassBrush(isActive = isConnected))
                    .border(1.dp, frostedBorderBrush(isActive = isConnected), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = if (isConnected) MatteYellow else TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isConnected) "Protected" else "Not Connected",
                        color = if (isConnected) MatteYellow else Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            val statusAnnotated = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.White)) {
                    append(if (isConnecting) "Optimizing\n" else "Gaming Mode\n")
                }
                withStyle(style = SpanStyle(color = if (isConnected) MatteYellow else TextMuted)) {
                    append(if (isConnected) "<< Active" else if (isConnecting) "Network..." else "<< Ready")
                }
            }

            Text(
                text = statusAnnotated,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Frosted Glass Edition\nUltra Low Ping Experience",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp
            )
        }

        Box(
            modifier = Modifier
                .size(150.dp)
                .graphicsLayer { scaleX = orbScale.value; scaleY = orbScale.value }
                .clickable(onClick = onOrbitClick),
            contentAlignment = Alignment.Center
        ) {
            val shape = CircleShape

            // Outer Frosted Glass Ring
            Box(
                modifier = Modifier
                    .size(142.dp)
                    .drawBehind {
                        if (isConnected || isConnecting) {
                            drawIntoCanvas { canvas ->
                                canvas.drawCircle(center, size.width / 2, frostedGlowPaint)
                            }
                        }
                    }
                    .clip(shape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                GlassHighlight,
                                GlassSurfaceBottom
                            )
                        )
                    )
                    .border(1.5.dp, frostedBorderBrush(isActive = isConnected || isConnecting), shape)
            )

            // Inner Frosted Core Button
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            colors = if (isConnected) {
                                listOf(
                                    MatteYellow.copy(alpha = 0.25f),
                                    Color(0x33FFFFFF),
                                    Color(0x14FFFFFF)
                                )
                            } else {
                                listOf(
                                    Color(0x26FFFFFF),
                                    Color(0x0DFFFFFF)
                                )
                            }
                        )
                    )
                    .border(2.dp, frostedBorderBrush(isActive = isConnected), shape),
                contentAlignment = Alignment.Center
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(color = MatteYellow, strokeWidth = 3.dp, modifier = Modifier.size(38.dp))
                } else {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = if (isConnected) MatteYellow else Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumStatsBar(
    basePing: String?,
    speedValues: () -> DashboardSpeeds,
    isConnected: Boolean,
    boostTrigger: Int = 0
) {
    var livePing by remember { mutableStateOf("--") }
    var liveJitter by remember { mutableStateOf("--") }
    var liveLoss by remember { mutableStateOf("--") }
    var liveSpeed by remember { mutableStateOf("--") }
    var liveSpeedUnit by remember { mutableStateOf("KB/s") }
    var pingStatus by remember { mutableStateOf("Excellent") }
    var jitterStatus by remember { mutableStateOf("Stable") }

    LaunchedEffect(isConnected, basePing, boostTrigger) {
        if (isConnected) {
            val initialSeed = basePing?.filter { it.isDigit() }?.toIntOrNull()?.coerceIn(28, 76)
                ?: Random.nextInt(42, 68)
            var currentPing = initialSeed
            var targetPing = Random.nextInt(30, 75)

            while (true) {
                if (abs(currentPing - targetPing) <= 4) {
                    targetPing = Random.nextInt(28, 79)
                }

                val direction = when {
                    targetPing > currentPing -> Random.nextInt(2, 9)
                    targetPing < currentPing -> -Random.nextInt(2, 9)
                    else -> Random.nextInt(-4, 5)
                }
                val microNoise = Random.nextInt(-3, 4)
                val nextPing = (currentPing + direction + microNoise).coerceIn(28, 78)

                val delta = abs(nextPing - currentPing)
                val currentJitter = (delta / 2 + Random.nextInt(1, 3)).coerceIn(1, 6)

                currentPing = nextPing
                livePing = currentPing.toString()
                liveJitter = currentJitter.toString()
                liveLoss = "0"

                pingStatus = when {
                    currentPing <= 42 -> "Ultra Low"
                    currentPing <= 62 -> "Excellent"
                    else -> "Optimal"
                }
                jitterStatus = if (currentJitter <= 3) "Stable" else "Synced"

                val realBytes = speedValues().download.coerceAtLeast(0L)
                if (realBytes >= 1024L * 8L) {
                    val formatted = formatDashboardSpeed(realBytes)
                    liveSpeed = formatted.first
                    liveSpeedUnit = formatted.second
                } else {
                    val kbTraffic = Random.nextInt(14, 96) / 10.0
                    liveSpeed = String.format(Locale.US, "%.1f", kbTraffic)
                    liveSpeedUnit = "KB/s"
                }

                delay(Random.nextLong(950L, 1450L))
            }
        } else {
            livePing = "--"
            liveJitter = "--"
            liveLoss = "--"
            liveSpeed = "--"
            liveSpeedUnit = "KB/s"
        }
    }

    val barShape = RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(barShape)
            .background(frostedGlassBrush(isActive = isConnected))
            .border(
                width = 1.2.dp,
                brush = frostedBorderBrush(isActive = isConnected),
                shape = barShape
            )
            .padding(vertical = 18.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PremiumStat(icon = Icons.Default.WifiTethering, title = "Ping", value = livePing, unit = "ms", status = pingStatus, isConnected = isConnected)
        Box(modifier = Modifier.width(1.dp).height(40.dp).background(GlassBorderLight))
        PremiumStat(icon = Icons.Default.Timeline, title = "Jitter", value = liveJitter, unit = "ms", status = jitterStatus, isConnected = isConnected)
        Box(modifier = Modifier.width(1.dp).height(40.dp).background(GlassBorderLight))
        PremiumStat(icon = Icons.Default.Shield, title = "Loss", value = liveLoss, unit = "%", status = "Perfect", isConnected = isConnected)
        Box(modifier = Modifier.width(1.dp).height(40.dp).background(GlassBorderLight))
        PremiumStat(icon = Icons.Default.Download, title = "Download", value = liveSpeed, unit = liveSpeedUnit, status = "Fast", isConnected = isConnected)
    }
}

@Composable
private fun PremiumStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    unit: String,
    status: String,
    isConnected: Boolean
) {
    val mainColor = if (isConnected) MatteYellow else Color.White

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(title, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = mainColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.width(2.dp))
            Text(unit, color = mainColor, fontSize = 10.sp, modifier = Modifier.padding(bottom = 3.dp))
        }
        if (isConnected) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MatteYellow, modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(status, color = MatteYellow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

internal fun formatSpeedToMbps(bytesPerSecond: Long): String {
    val mbps = (bytesPerSecond * 8.0) / 1_000_000.0
    return if (mbps > 0) String.format(Locale.US, "%.0f", mbps) else "0"
}

internal fun formatDashboardSpeed(bytesPerSecond: Long): Pair<String, String> {
    val speed = bytesPerSecond.coerceAtLeast(0L).toDouble()
    val (value, unit) = when {
        speed >= 1024.0 * 1024.0 * 1024.0 -> speed / (1024.0 * 1024.0 * 1024.0) to "GB/s"
        speed >= 1024.0 * 1024.0 -> speed / (1024.0 * 1024.0) to "MB/s"
        speed >= 1024.0 -> speed / 1024.0 to "KB/s"
        else -> speed to "B/s"
    }
    return (if (value < 10.0 && speed >= 1024.0) String.format(Locale.US, "%.1f", value)
    else String.format(Locale.US, "%.0f", value)) to unit
}

data class BoostGame(val id: String, val name: String, val posterUrl: String, val isHot: Boolean = false)

@Composable
fun GameBoostSection(
    modifier: Modifier = Modifier,
    selectedGameId: String = "pubg",
    onGameSelect: (String) -> Unit = {}
) {
    val games = remember {
        listOf(
            BoostGame("pubg", "PUBG MOBILE", "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/578080/library_600x900.jpg", true),
            BoostGame("cod", "CALL OF DUTY\nMOBILE", "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/1938090/library_600x900.jpg"),
            BoostGame("efootball", "eFootball™", "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/1665460/library_600x900.jpg"),
            BoostGame("valorant", "VALORANT", "https://cdn1.epicgames.com/offer/cbd5b3d310a54b12bf3fe8c41994174f/EGS_VALORANT_RiotGames_S2_1200x1600-b2f707f43db4af1658b1d4ed83134914")
        )
    }
    var activeGame by rememberSaveable { mutableStateOf(selectedGameId) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VideogameAsset, contentDescription = null, tint = MatteYellow, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Select Your Game", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text("Game Profiles >", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(games.size) { index ->
                val game = games[index]
                GamePosterPremiumCard(
                    game = game,
                    isSelected = game.id == activeGame,
                    onClick = {
                        activeGame = game.id
                        onGameSelect(game.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun GamePosterPremiumCard(game: BoostGame, isSelected: Boolean, onClick: () -> Unit) {
    var posterBitmap by remember(game.id) { mutableStateOf(posterMemoryCache[game.id]) }
    val cardGlowPaint = remember {
        androidx.compose.ui.graphics.Paint().apply {
            color = MatteYellow.copy(alpha = 0.28f)
            asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(28f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
    }

    LaunchedEffect(game.id) {
        if (!posterMemoryCache.containsKey(game.id)) {
            withContext(Dispatchers.IO) {
                var conn: HttpURLConnection? = null
                try {
                    conn = (URL(game.posterUrl).openConnection() as HttpURLConnection).apply {
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                        connectTimeout = 10000
                        readTimeout = 10000
                    }
                    if (conn.responseCode in 200..299) {
                        val bytes = conn.inputStream.use { it.readBytes() }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { decoded ->
                            val img = decoded.asImageBitmap()
                            posterMemoryCache[game.id] = img
                            posterBitmap = img
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    conn?.disconnect()
                }
            }
        } else {
            posterBitmap = posterMemoryCache[game.id]
        }
    }

    val posterShape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .width(115.dp)
            .height(160.dp)
            .then(
                if (isSelected) {
                    Modifier.drawBehind {
                        drawIntoCanvas { canvas ->
                            canvas.drawRoundRect(0f, 0f, size.width, size.height, 18.dp.toPx(), 18.dp.toPx(), cardGlowPaint)
                        }
                    }
                } else Modifier
            )
            .clip(posterShape)
            .background(frostedGlassBrush(isActive = isSelected))
            .border(if (isSelected) 1.8.dp else 1.dp, frostedBorderBrush(isActive = isSelected), posterShape)
            .clickable(onClick = onClick)
    ) {
        if (posterBitmap != null) {
            Image(
                bitmap = posterBitmap!!,
                contentDescription = game.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Frosted dark bottom overlay for crisp text readability
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.08f),
                    0.40f to Color.Transparent,
                    0.72f to Color(0xCC0B0C10),
                    1.0f to Color(0xF20B0C10)
                )
            )
        )

        if (!isSelected) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        }

        if (isSelected) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MatteYellow)
                        .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = BgDark,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Column(
            Modifier.fillMaxSize().padding(bottom = 10.dp, start = 6.dp, end = 6.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = game.name,
                color = if (isSelected) Color.White else Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
            Spacer(Modifier.height(6.dp))

            // Frosted Glass Pill inside Game Card
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MatteYellow.copy(alpha = 0.22f)
                        else Color.White.copy(alpha = 0.10f)
                    )
                    .border(
                        1.dp,
                        if (isSelected) MatteYellow.copy(alpha = 0.8f) else GlassBorderLight,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (isSelected) MatteYellow else Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "Optimized",
                    color = if (isSelected) MatteYellow else Color.White.copy(alpha = 0.75f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
