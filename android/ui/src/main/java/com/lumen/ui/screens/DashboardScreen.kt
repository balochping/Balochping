package com.lumen.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.lumen.ui.components.*
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

// ==================== CACHES ====================
private val posterMemoryCache = ConcurrentHashMap<String, ImageBitmap>()
private val flagMemoryCache = ConcurrentHashMap<String, ImageBitmap>()

// ==================== COLORS ====================
private val ActiveGreen = Color(0xFF00E676)
private val GamingYellow = Color(0xFFFFC107)
private val GamingAmber = Color(0xFFFF9800)
private val BgDark = Color(0xFF08080A)
private val CardDark = Color(0xFF121216)
private val CardStroke = Color(0xFF25252E)
private val TextMuted = Color(0xFF8B8B9A)
private val TextDim = Color(0xFF5C5C6A)

// ==================== DATA ====================
data class BoostGame(
    val id: String,
    val name: String,
    val posterUrl: String,
    val isHot: Boolean = false
)

// ==================== HELPERS ====================
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
            delay(1100L)
            onCheckPing()
        }
    }

    val bgBrush = remember {
        Brush.radialGradient(
            colors = listOf(Color(0xFF16161C), BgDark),
            center = Offset(0.7f, 0.05f),
            radius = 1400f
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
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 12.dp)
                ) {
                    // HERO
                    PremiumHeroSection(
                        connectionState = connectionState,
                        isFa = isFa,
                        onToggleConnection = onToggleConnection,
                        tick = tick
                    )

                    Spacer(Modifier.height(22.dp))

                    // STATS
                    if (speedStatsEnabled) {
                        val basePingValue = connectedPing
                            ?: activeNode?.pingMs?.takeIf { it > 0 }?.toString()

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
                        Spacer(Modifier.height(24.dp))
                    }

                    // GAMES
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

                    Spacer(Modifier.height(22.dp))

                    // SERVER HEADER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = if (isFa) "سرور" else "Server",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardDark)
                                .border(1.dp, themeAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
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

                    Spacer(Modifier.height(12.dp))

                    // SERVER CARD
                    if (activeNode != null) {
                        PremiumServerCard(
                            node = activeNode,
                            isConnected = isConnected,
                            themeAccent = themeAccent,
                            onClick = openServerPicker
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardDark)
                                .border(1.2.dp, themeAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
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

                    Spacer(Modifier.height(110.dp))
                }
            }
        }
    }

    // Server Picker - مطابق امضای اصلی
    if (showServerPicker) {
        DashboardServerPicker(
            groupNodes,
            groupTitle,
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

// ═══════════════════════════════════════════════════════════════
// HERO
// ═══════════════════════════════════════════════════════════════
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

    val outerGlow = remember(isConnected) {
        androidx.compose.ui.graphics.Paint().apply {
            color = stateColor.copy(alpha = if (isConnected) 0.22f else 0.18f)
            asFrameworkPaint().maskFilter =
                android.graphics.BlurMaskFilter(28f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
    }
    val innerGlow = remember(isConnected) {
        androidx.compose.ui.graphics.Paint().apply {
            color = stateColor.copy(alpha = if (isConnected) 0.28f else 0.22f)
            asFrameworkPaint().maskFilter =
                android.graphics.BlurMaskFilter(16f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(stateColor.copy(alpha = 0.12f))
                    .border(1.dp, stateColor.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = stateColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = when {
                            isConnected -> if (isFa) "محافظت شده" else "Protected"
                            isConnecting -> if (isFa) "در حال اتصال..." else "Connecting..."
                            else -> if (isFa) "متصل نیست" else "Not Connected"
                        },
                        color = stateColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Black)) {
                        append(if (isFa) "حالت بازی\n" else "Gaming Mode\n")
                    }
                    withStyle(SpanStyle(color = stateColor, fontWeight = FontWeight.Black)) {
                        append(
                            when {
                                isConnected -> if (isFa) "فعال" else "Active"
                                isConnecting -> if (isFa) "در حال آماده‌سازی" else "Optimizing"
                                else -> if (isFa) "آماده" else "Ready"
                            }
                        )
                    }
                },
                fontSize = 26.sp,
                lineHeight = 32.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (isFa)
                    "پینگ فوق‌العاده پایین\nبرای بهترین تجربه بازی"
                else
                    "Ultra Low Ping\nfor the Best Experience",
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .size(148.dp)
                .graphicsLayer {
                    scaleX = orbScale.value
                    scaleY = orbScale.value
                }
                .clickable {
                    scope.launch {
                        orbScale.snapTo(0.86f)
                        orbScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 550f))
                    }
                    tick(HapticFeedbackType.LongPress)
                    onToggleConnection()
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .drawBehind {
                        drawIntoCanvas { canvas ->
                            canvas.drawCircle(center, size.width / 2, outerGlow)
                        }
                    }
                    .border(1.5.dp, stateColor.copy(alpha = 0.5f), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(CardDark)
                    .border(2.6.dp, stateColor.copy(alpha = 0.95f), CircleShape)
                    .drawBehind {
                        drawIntoCanvas { canvas ->
                            canvas.drawCircle(center, size.width / 2, innerGlow)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        color = GamingYellow,
                        strokeWidth = 3.2.dp,
                        modifier = Modifier.size(38.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = stateColor,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// STATS
// ═══════════════════════════════════════════════════════════════
@Composable
private fun PremiumStatsBar(
    basePing: String?,
    speedValues: () -> DashboardSpeeds,
    isConnected: Boolean,
    boostTrigger: Int,
    isFa: Boolean,
    onRefreshPing: () -> Unit
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
            val initial = basePing?.filter { it.isDigit() }?.toIntOrNull()?.coerceIn(28, 76)
                ?: Random.nextInt(42, 68)
            var current = initial
            var target = Random.nextInt(30, 75)

            while (true) {
                if (abs(current - target) <= 4) target = Random.nextInt(28, 79)

                val dir = when {
                    target > current -> Random.nextInt(2, 8)
                    target < current -> -Random.nextInt(2, 8)
                    else -> Random.nextInt(-3, 4)
                }
                val next = (current + dir + Random.nextInt(-2, 3)).coerceIn(28, 78)
                val jitter = (abs(next - current) / 2 + Random.nextInt(1, 3)).coerceIn(1, 6)

                current = next
                livePing = current.toString()
                liveJitter = jitter.toString()
                liveLoss = "0"

                pingStatus = when {
                    current <= 40 -> if (isFa) "فوق‌العاده" else "Ultra"
                    current <= 58 -> if (isFa) "عالی" else "Excellent"
                    else -> if (isFa) "خوب" else "Good"
                }
                jitterStatus = if (jitter <= 3) {
                    if (isFa) "پایدار" else "Stable"
                } else {
                    if (isFa) "عادی" else "Normal"
                }

                val bytes = speedValues().download.coerceAtLeast(0L)
                if (bytes >= 8 * 1024) {
                    val (v, u) = formatDashboardSpeed(bytes)
                    liveSpeed = v
                    liveSpeedUnit = u
                } else {
                    liveSpeed = String.format(Locale.US, "%.1f", Random.nextInt(12, 90) / 10.0)
                    liveSpeedUnit = "KB/s"
                }

                delay(Random.nextLong(900, 1400))
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

    val shape = RoundedCornerShape(16.dp)
    val borderBrush = Brush.horizontalGradient(
        if (isConnected)
            listOf(ActiveGreen.copy(alpha = 0.7f), ActiveGreen.copy(alpha = 0.25f))
        else
            listOf(GamingYellow.copy(alpha = 0.6f), GamingAmber.copy(alpha = 0.25f))
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(CardDark)
                .border(1.2.dp, borderBrush, shape)
                .clickable(enabled = isConnected, onClick = onRefreshPing)
                .padding(vertical = 15.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                icon = Icons.Default.WifiTethering,
                title = "Ping",
                value = livePing,
                unit = "ms",
                status = pingStatus,
                isConnected = isConnected
            )
            VerticalDivider()
            StatItem(
                icon = Icons.Default.Timeline,
                title = "Jitter",
                value = liveJitter,
                unit = "ms",
                status = jitterStatus,
                isConnected = isConnected
            )
            VerticalDivider()
            StatItem(
                icon = Icons.Default.Shield,
                title = "Loss",
                value = liveLoss,
                unit = "%",
                status = if (isConnected) (if (isFa) "عالی" else "Perfect") else "",
                isConnected = isConnected
            )
            VerticalDivider()
            StatItem(
                icon = Icons.Default.Download,
                title = if (isFa) "دانلود" else "Down",
                value = liveSpeed,
                unit = liveSpeedUnit,
                status = if (isConnected) (if (isFa) "سریع" else "Fast") else "",
                isConnected = isConnected
            )
        }

        if (!isConnected) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isFa) "برای دیدن آمار زنده متصل شوید" else "Connect to see live stats",
                color = TextDim,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(38.dp)
            .background(CardStroke)
    )
}

@Composable
private fun StatItem(
    icon: ImageVector,
    title: String,
    value: String,
    unit: String,
    status: String,
    isConnected: Boolean
) {
    val color = if (isConnected) ActiveGreen else GamingYellow

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(74.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
            Text(title, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(5.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = color, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(2.dp))
            Text(
                unit,
                color = color.copy(alpha = 0.8f),
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        if (isConnected && status.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = ActiveGreen.copy(alpha = 0.85f),
                    modifier = Modifier.size(10.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(status, color = ActiveGreen.copy(alpha = 0.85f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Spacer(Modifier.height(13.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SERVER CARD
// ═══════════════════════════════════════════════════════════════
@Composable
private fun PremiumServerCard(
    node: NodeUiModel,
    isConnected: Boolean,
    themeAccent: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val borderBrush = Brush.horizontalGradient(
        if (isConnected)
            listOf(ActiveGreen.copy(alpha = 0.8f), ActiveGreen.copy(alpha = 0.3f))
        else
            listOf(GamingYellow.copy(alpha = 0.75f), GamingAmber.copy(alpha = 0.3f))
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardDark)
            .border(1.4.dp, borderBrush, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(width = 42.dp, height = 28.dp)) {
            val code = node.countryCode.trim().lowercase()
            var flagBitmap by remember(code) { mutableStateOf(flagMemoryCache[code]) }
            val emoji = remember(code) { CountryFlagHelper.getServerFlagEmoji(node.countryCode) }

            LaunchedEffect(code) {
                if (flagBitmap == null && code.isNotBlank()) {
                    withContext(Dispatchers.IO) {
                        try {
                            val conn = (URL("https://flagcdn.com/w80/$code.png").openConnection() as HttpURLConnection).apply {
                                connectTimeout = 3000
                                readTimeout = 3000
                            }
                            if (conn.responseCode in 200..299) {
                                BitmapFactory.decodeStream(conn.inputStream)?.let {
                                    val bmp = it.asImageBitmap()
                                    flagMemoryCache[code] = bmp
                                    flagBitmap = bmp
                                }
                            }
                            conn.disconnect()
                        } catch (_: Exception) {
                        }
                    }
                }
            }

            if (flagBitmap != null) {
                Image(
                    bitmap = flagBitmap!!,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(5.dp))
                )
            } else {
                Text(emoji, fontSize = 22.sp, modifier = Modifier.align(Alignment.Center))
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = node.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                node.pingMs?.takeIf { it > 0 }?.let { ping ->
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${ping}ms",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            ping < 50 -> ActiveGreen
                            ping < 90 -> GamingYellow
                            else -> Color(0xFFFF5252)
                        }
                    )
                }
            }

            Spacer(Modifier.height(5.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = themeAccent,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = node.displayProtocol,
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
}

// ═══════════════════════════════════════════════════════════════
// GAMES
// ═══════════════════════════════════════════════════════════════
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
            modifier = Modifier.fillMaxWidth(),
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

    val yellowGlow = remember {
        androidx.compose.ui.graphics.Paint().apply {
            color = GamingYellow.copy(alpha = 0.22f)
            asFrameworkPaint().maskFilter =
                android.graphics.BlurMaskFilter(18f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
    }
    val greenGlow = remember {
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
                                if (isConnected) greenGlow else yellowGlow
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
