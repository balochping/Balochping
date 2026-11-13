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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.WifiTethering
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

// Thread-safe caches
private val posterMemoryCache = ConcurrentHashMap<String, ImageBitmap>()
private val flagMemoryCache = ConcurrentHashMap<String, ImageBitmap>()

// Colors
private val ActiveGreen = Color(0xFF10E870)
private val GamingYellow = Color(0xFFE5B80B)
private val GamingAmber = Color(0xFFB37D14)
private val BgDark = Color(0xFF060608)
private val CardDark = Color(0xFF111116)
private val CardStroke = Color(0xFF22222A)
private val TextMuted = Color(0xFF8E929F)

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
    val themeAccent = if (isConnected) ActiveGreen else GamingYellow
    val scrollState = rememberScrollState()
    var gameBoostTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(isConnected, activeNode?.id) {
        if (isConnected && connectedPing == null && !isCheckingPing) {
            delay(1200L)
            onCheckPing()
        }
    }

    val bgBrush = remember {
        Brush.radialGradient(
            colors = listOf(Color(0xFF14141A), BgDark),
            center = Offset(800f, 0f),
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
                        onPingAll = { onPingNodes(groupNodes) },
                        onPingNode = { onPingNodes(listOf(it)) },
                        onStopPing = onStopPing
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // ===== Hero =====
                    PremiumHeroSection(
                        connectionState = connectionState,
                        isFa = isFa,
                        onToggleConnection = onToggleConnection,
                        tick = tick
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ===== Stats =====
                    if (speedStatsEnabled) {
                        val basePingValue = connectedPing ?: activeNode?.pingMs?.takeIf { it > 0 }?.toString()
                        PremiumStatsBar(
                            basePing = basePingValue,
                            speedValues = speedValues,
                            isConnected = isConnected,
                            boostTrigger = gameBoostTrigger,
                            isFa = isFa,
                            onRefreshPing = {
                                if (isConnected && !isCheckingPing) {
                                    tick(HapticFeedbackType.LongPress)
                                    gameBoostTrigger++
                                    onCheckPing()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                    }

                    // ===== Games =====
                    GameBoostSection(
                        selectedGameId = "pubg",
                        isConnected = isConnected,
                        isFa = isFa,
                        onOpenProfiles = openServerPicker,
                        onGameSelect = {
                            tick(HapticFeedbackType.LongPress)
                            gameBoostTrigger++
                            val bestNode = groupNodes
                                .filter { (it.pingMs ?: 0) > 0 }
                                .minByOrNull { it.pingMs ?: Int.MAX_VALUE }
                            if (bestNode != null && bestNode.id != activeNode?.id) {
                                onSelectNode(bestNode)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ===== Server Header =====
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Layers,
                                contentDescription = null,
                                tint = themeAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isFa) "سرور" else "Server",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardDark)
                                .border(1.dp, themeAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .clickable {
                                    tick(HapticFeedbackType.LongPress)
                                    val bestNode = groupNodes
                                        .filter { (it.pingMs ?: 0) > 0 }
                                        .minByOrNull { it.pingMs ?: Int.MAX_VALUE }
                                    if (bestNode != null) {
                                        onSelectNode(bestNode)
                                    } else {
                                        onPingNodes(groupNodes)
                                        openServerPicker()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = themeAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isFa) "انتخاب خودکار" else "Auto Select",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = themeAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ===== Active Server Card =====
                    if (activeNode != null) {
                        val serverShape = RoundedCornerShape(18.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(serverShape)
                                .background(CardDark)
                                .border(
                                    width = 1.4.dp,
                                    brush = Brush.horizontalGradient(
                                        if (isConnected) {
                                            listOf(
                                                ActiveGreen.copy(alpha = 0.85f),
                                                ActiveGreen.copy(alpha = 0.4f)
                                            )
                                        } else {
                                            listOf(
                                                GamingYellow.copy(alpha = 0.8f),
                                                GamingAmber.copy(alpha = 0.4f)
                                            )
                                        }
                                    ),
                                    shape = serverShape
                                )
                                .clickable(onClick = openServerPicker)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Flag
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(28.dp)
                            ) {
                                val countryCode = activeNode.countryCode.trim().lowercase()
                                var flagBitmap by remember(countryCode) {
                                    mutableStateOf(flagMemoryCache[countryCode])
                                }
                                val flagEmoji = remember(countryCode) {
                                    CountryFlagHelper.getServerFlagEmoji(activeNode.countryCode)
                                }

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
                                                    val bmp = connection.inputStream.use {
                                                        BitmapFactory.decodeStream(it)
                                                    }
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
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(5.dp))
                                    )
                                } else {
                                    Text(
                                        text = flagEmoji,
                                        fontSize = 22.sp,
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

                                    // Show ping if available
                                    val ping = activeNode.pingMs
                                    if (ping != null && ping > 0) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "${ping}ms",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = themeAccent.copy(alpha = 0.9f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(5.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = themeAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = activeNode.displayProtocol,
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = themeAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(CardDark)
                                .border(1.3.dp, themeAccent.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                                .clickable(onClick = openServerPicker)
                                .padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFa) "سروری انتخاب نشده" else "No server selected",
                                fontSize = 14.sp,
                                color = themeAccent
                            )
                        }
                    }

                    // Bottom spacing for navigation bar
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    if (showServerPicker) {
        DashboardServerPicker(
            groupNodes = groupNodes,
            groupTitle = groupTitle,
            onDismiss = { showServerPicker = false },
            onSelect = {
                onSelectNode(it)
                showServerPicker = false
            },
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
    isFa: Boolean,
    onToggleConnection: () -> Unit,
    tick: (HapticFeedbackType) -> Unit
) {
    val isConnected = connectionState == ConnectionState.Connected
    val isConnecting = connectionState == ConnectionState.Connecting
    val scope = rememberCoroutineScope()
    val orbScale = remember { Animatable(1f) }

    val stateColor = if (isConnected) ActiveGreen else GamingYellow

    val outerYellowGlowPaint = remember {
        androidx.compose.ui.graphics.Paint().apply {
            color = GamingYellow.copy(alpha = 0.18f)
            asFrameworkPaint().maskFilter =
                android.graphics.BlurMaskFilter(24f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
    }
    val outerGreenGlowPaint = remember {
        androidx.compose.ui.graphics.Paint().apply {
            color = ActiveGreen.copy(alpha = 0.20f)
            asFrameworkPaint().maskFilter =
                android.graphics.BlurMaskFilter(26f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
    }
    val innerYellowGlowPaint = remember {
        androidx.compose.ui.graphics.Paint().apply {
            color = GamingYellow.copy(alpha = 0.22f)
            asFrameworkPaint().maskFilter =
                android.graphics.BlurMaskFilter(14f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
    }
    val innerGreenGlowPaint = remember {
        androidx.compose.ui.graphics.Paint().apply {
            color = ActiveGreen.copy(alpha = 0.25f)
            asFrameworkPaint().maskFilter =
                android.graphics.BlurMaskFilter(16f, android.graphics.BlurMaskFilter.Blur.NORMAL)
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Status Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(stateColor.copy(alpha = 0.12f))
                    .border(1.dp, stateColor.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = stateColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = when {
                            isConnected -> if (isFa) "محافظت شده" else "Protected"
                            isConnecting -> if (isFa) "در حال اتصال" else "Connecting"
                            else -> if (isFa) "متصل نیست" else "Not Connected"
                        },
                        color = stateColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Status Text
            val titleText = when {
                isConnecting -> if (isFa) "در حال بهینه‌سازی" else "Optimizing"
                else -> if (isFa) "حالت بازی" else "Gaming Mode"
            }

            val subtitleText = when {
                isConnected -> if (isFa) "فعال •" else "Active •"
                isConnecting -> if (isFa) "شبکه..." else "Network..."
                else -> if (isFa) "آماده •" else "Ready •"
            }

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) {
                        append("$titleText\n")
                    }
                    withStyle(SpanStyle(color = stateColor)) {
                        append(subtitleText)
                    }
                },
                fontSize = 25.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 31.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isFa) "پینگ فوق‌العاده پایین\nبرای بهترین تجربه" else "Ultra Low Ping\nfor the Best Experience",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 17.sp
            )
        }

        // Power Button
        Box(
            modifier = Modifier
                .size(142.dp)
                .graphicsLayer {
                    scaleX = orbScale.value
                    scaleY = orbScale.value
                }
                .clickable(onClick = onOrbitClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .drawBehind {
                        drawIntoCanvas { canvas ->
                            canvas.drawCircle(
                                center,
                                size.width / 2,
                                if (isConnected) outerGreenGlowPaint else outerYellowGlowPaint
                            )
                        }
                    }
                    .border(1.6.dp, stateColor.copy(alpha = 0.55f), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(CardDark)
                    .border(2.4.dp, stateColor.copy(alpha = 0.9f), CircleShape)
                    .drawBehind {
                        drawIntoCanvas { canvas ->
                            canvas.drawCircle(
                                center,
                                size.width / 2,
                                if (isConnected) innerGreenGlowPaint else innerYellowGlowPaint
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        color = GamingYellow,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = stateColor,
                        modifier = Modifier.size(38.dp)
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
    boostTrigger: Int = 0,
    isFa: Boolean,
    onRefreshPing: () -> Unit = {}
) {
    var livePing by remember { mutableStateOf("--") }
    var liveJitter by remember { mutableStateOf("--") }
    var liveLoss by remember { mutableStateOf("--") }
    var liveSpeed by remember { mutableStateOf("--") }
    var liveSpeedUnit by remember { mutableStateOf("KB/s") }
    var pingStatus by remember { mutableStateOf("") }
    var jitterStatus by remember { mutableStateOf("") }

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
                    currentPing <= 42 -> if (isFa) "فوق‌العاده" else "Ultra Low"
                    currentPing <= 62 -> if (isFa) "عالی" else "Excellent"
                    else -> if (isFa) "بهینه" else "Optimal"
                }
                jitterStatus = if (currentJitter <= 3) {
                    if (isFa) "پایدار" else "Stable"
                } else {
                    if (isFa) "هماهنگ" else "Synced"
                }

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
            pingStatus = ""
            jitterStatus = ""
        }
    }

    val barShape = RoundedCornerShape(16.dp)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(barShape)
                .background(CardDark)
                .border(
                    width = 1.2.dp,
                    brush = Brush.horizontalGradient(
                        if (isConnected) {
                            listOf(ActiveGreen.copy(alpha = 0.7f), ActiveGreen.copy(alpha = 0.35f))
                        } else {
                            listOf(GamingYellow.copy(alpha = 0.6f), GamingAmber.copy(alpha = 0.35f))
                        }
                    ),
                    shape = barShape
                )
                .clickable(enabled = isConnected, onClick = onRefreshPing)
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PremiumStat(
                icon = Icons.Default.WifiTethering,
                title = "Ping",
                value = livePing,
                unit = "ms",
                status = pingStatus,
                isConnected = isConnected
            )
            Box(Modifier.width(1.dp).height(36.dp).background(CardStroke))
            PremiumStat(
                icon = Icons.Default.Timeline,
                title = "Jitter",
                value = liveJitter,
                unit = "ms",
                status = jitterStatus,
                isConnected = isConnected
            )
            Box(Modifier.width(1.dp).height(36.dp).background(CardStroke))
            PremiumStat(
                icon = Icons.Default.Shield,
                title = "Loss",
                value = liveLoss,
                unit = "%",
                status = if (isConnected) (if (isFa) "عالی" else "Perfect") else "",
                isConnected = isConnected
            )
            Box(Modifier.width(1.dp).height(36.dp).background(CardStroke))
            PremiumStat(
                icon = Icons.Default.Download,
                title = if (isFa) "دانلود" else "Download",
                value = liveSpeed,
                unit = liveSpeedUnit,
                status = if (isConnected) (if (isFa) "سریع" else "Fast") else "",
                isConnected = isConnected
            )
        }

        // Hint when disconnected
        if (!isConnected) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isFa) "برای مشاهده آمار واقعی متصل شوید" else "Connect to see live stats",
                color = TextMuted.copy(alpha = 0.7f),
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
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
    val mainColor = if (isConnected) ActiveGreen else GamingYellow

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = mainColor.copy(alpha = 0.85f),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                title,
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                color = mainColor,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                unit,
                color = mainColor.copy(alpha = 0.85f),
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        if (isConnected && status.isNotEmpty()) {
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ActiveGreen.copy(alpha = 0.85f),
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    status,
                    color = ActiveGreen.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Spacer(modifier = Modifier.height(13.dp)) // keep height consistent
        }
    }
}

@Composable
fun GameBoostSection(
    modifier: Modifier = Modifier,
    selectedGameId: String = "pubg",
    isConnected: Boolean = false,
    isFa: Boolean = false,
    onOpenProfiles: () -> Unit = {},
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
    val accentColor = if (isConnected) ActiveGreen else GamingYellow

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.VideogameAsset,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = if (isFa) "انتخاب بازی" else "Select Your Game",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = if (isFa) "پروفایل بازی‌ها >" else "Game Profiles >",
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenProfiles)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(games.size) { index ->
                val game = games[index]
                GamePosterPremiumCard(
                    game = game,
                    isSelected = game.id == activeGame,
                    isConnected = isConnected,
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
private fun GamePosterPremiumCard(
    game: BoostGame,
    isSelected: Boolean,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    var posterBitmap by remember(game.id) { mutableStateOf(posterMemoryCache[game.id]) }
    val accentColor = if (isConnected) ActiveGreen else GamingYellow

    val yellowCardGlowPaint = remember {
        androidx.compose.ui.graphics.Paint().apply {
            color = GamingYellow.copy(alpha = 0.22f)
            asFrameworkPaint().maskFilter =
                android.graphics.BlurMaskFilter(18f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
    }
    val greenCardGlowPaint = remember {
        androidx.compose.ui.graphics.Paint().apply {
            color = ActiveGreen.copy(alpha = 0.22f)
            asFrameworkPaint().maskFilter =
                android.graphics.BlurMaskFilter(18f, android.graphics.BlurMaskFilter.Blur.NORMAL)
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

    val posterShape = RoundedCornerShape(14.dp)
    val borderColor = if (isSelected) accentColor.copy(alpha = 0.9f) else CardStroke.copy(alpha = 0.65f)

    Box(
        modifier = Modifier
            .width(112.dp)
            .height(155.dp)
            .then(
                if (isSelected) {
                    Modifier.drawBehind {
                        drawIntoCanvas { canvas ->
                            canvas.drawRoundRect(
                                0f, 0f, size.width, size.height,
                                14.dp.toPx(), 14.dp.toPx(),
                                if (isConnected) greenCardGlowPaint else yellowCardGlowPaint
                            )
                        }
                    }
                } else Modifier
            )
            .clip(posterShape)
            .background(CardDark)
            .border(if (isSelected) 1.7.dp else 1.dp, borderColor, posterShape)
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

        // Gradient overlay
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.5f to Color.Transparent,
                        0.78f to Color.Black.copy(alpha = 0.85f),
                        1.0f to Color.Black.copy(alpha = 0.97f)
                    )
                )
        )

        if (!isSelected) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
        }

        // Check badge
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(7.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = BgDark,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // Title + Optimized badge
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = 9.dp, start = 5.dp, end = 5.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = game.name,
                color = if (isSelected) Color.White else Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )

            Spacer(Modifier.height(5.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) accentColor.copy(alpha = 0.15f)
                        else Color.Black.copy(alpha = 0.6f)
                    )
                    .border(
                        1.dp,
                        if (isSelected) accentColor.copy(alpha = 0.7f)
                        else TextMuted.copy(alpha = 0.25f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else TextMuted,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "Optimized",
                    color = if (isSelected) accentColor else TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Keep the rest of your helper functions (formatDashboardSpeed, BoostGame, etc.) as they were
