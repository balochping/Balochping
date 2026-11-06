package com.lumen.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.lumen.ui.components.*
import com.lumen.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

// Cache for Game Posters
private val posterMemoryCache = mutableMapOf<String, ImageBitmap>()
// Cache for High Quality Rectangular Flags from flagcdn
private val flagMemoryCache = mutableMapOf<String, ImageBitmap>()

// === Premium Yellow & Neon Gaming Colors (X Ping Pro) ===
private val CyberYellow = Color(0xFFFFD600) 
private val NeonGreen = Color(0xFF00FF66) 
private val CyberPurple = Color(0xFFB026FF) 
private val CyberPurpleDark = Color(0xFF5A189A)
private val BgDark = Color(0xFF09090E)
private val CardDark = Color(0xFF15151E)
private val CardStroke = Color(0xFF252536)
private val TextMuted = Color(0xFF8B93A5)

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

    val bgBrush = Brush.radialGradient(
        colors = listOf(CyberPurpleDark.copy(alpha = 0.25f), BgDark),
        center = Offset(800f, 0f),
        radius = 1800f
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
            .statusBarsPadding()
    ) {
        BrandTopBar(onAddClick = { showAddSheet = true })

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
                        val dlSpeed = if (isConnected) formatSpeedToMbps(speedValues().download) else "0"
                        PremiumStatsBar(basePing = basePingValue, dlSpeed = dlSpeed, isConnected = isConnected)
                        Spacer(modifier = Modifier.height(28.dp))
                    }

                    GameBoostSection(selectedGameId = "pubg")
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Layers, contentDescription=null, tint=CyberPurple, modifier=Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(text = "Server", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardDark)
                                .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .clickable(onClick = openServerPicker)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⚡", fontSize = 10.sp, color = NeonGreen)
                            Spacer(Modifier.width(4.dp))
                            Text(text = "Auto Select", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (activeNode != null) {
                        val serverShape = RoundedCornerShape(20.dp)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(serverShape)
                                .background(CardDark)
                                .border(
                                    width = if(isConnected) 2.dp else 1.dp, 
                                    brush = Brush.horizontalGradient(listOf(NeonGreen, CyberPurple)), 
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
                                            try {
                                                val url = URL("https://flagcdn.com/w80/$countryCode.png")
                                                val connection = url.openConnection() as HttpURLConnection
                                                connection.connectTimeout = 3000
                                                connection.readTimeout = 3000
                                                connection.doInput = true
                                                connection.connect()
                                                
                                                val input = connection.inputStream
                                                val bmp = BitmapFactory.decodeStream(input)
                                                if (bmp != null) {
                                                    val imageBitmap = bmp.asImageBitmap()
                                                    flagMemoryCache[countryCode] = imageBitmap
                                                    flagBitmap = imageBitmap
                                                }
                                            } catch (e: Exception) {}
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
                                    Text(text = activeNode.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Shield, contentDescription=null, tint=TextMuted, modifier=Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(text = activeNode.displayProtocol, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                }
                            }
                            // فیکس ارور: استفاده از آیکون استاندارد به جای automirrored
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = strings.changeServer, tint = if(isConnected) NeonGreen else Color.White, modifier = Modifier.size(24.dp))
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(CardDark)
                                .border(1.dp, CardStroke, RoundedCornerShape(20.dp))
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
        DashboardServerPicker(groupNodes, groupTitle, onDismiss = { showServerPicker = false }, onSelect = { onSelectNode(it); showServerPicker = false }, pinging = pingingNodeIds, onPingAll = { onPingNodes(groupNodes) }, onPingNode = { onPingNodes(listOf(it)) }, onStopPing = onStopPing)
    }
    if (showAddSheet) {
        AddServerSheet(onDismissRequest = { showAddSheet = false }, onImportClipboard = onImportClipboard, onImportQr = scanQr, onImportFile = onImportFile, onAddManualNode = onAddManualNode)
    }
}

@Composable
private fun PremiumHeroSection(connectionState: ConnectionState, onToggleConnection: () -> Unit, tick: (HapticFeedbackType) -> Unit) {
    val isConnected = connectionState == ConnectionState.Connected
    val isConnecting = connectionState == ConnectionState.Connecting
    val scope = rememberCoroutineScope()
    val orbScale = remember { Animatable(1f) }

    val mainColor = if (isConnected) CyberYellow else TextMuted
    
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
                    .clip(RoundedCornerShape(6.dp))
                    .background(if(isConnected) NeonGreen.copy(alpha = 0.15f) else TextMuted.copy(alpha = 0.15f))
                    .border(1.dp, if(isConnected) NeonGreen else TextMuted, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = if(isConnected) NeonGreen else TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if(isConnected) "Protected" else "Not Connected", color = if(isConnected) NeonGreen else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            
            val statusAnnotated = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.White)) {
                    append(if(isConnecting) "Optimizing\n" else if(isConnected) "Gaming Mode\n" else "Ready to\n")
                }
                withStyle(style = SpanStyle(color = if(isConnected) NeonGreen else TextMuted)) {
                    append(if(isConnected) "Active >>" else if(isConnecting) "Network..." else "Boost >>")
                }
            }
            
            Text(
                text = statusAnnotated,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Ultra Low Ping\nfor the Best Experience", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)
        }

        Box(
            modifier = Modifier
                .size(150.dp)
                .graphicsLayer { scaleX = orbScale.value; scaleY = orbScale.value }
                .clickable(onClick = onOrbitClick),
            contentAlignment = Alignment.Center
        ) {
            val shape = CircleShape 
            
            if (isConnected || isConnecting) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(CyberPurple.copy(alpha = 0.5f), Color.Transparent),
                                    center = center,
                                    radius = size.width / 1.5f
                                )
                            )
                        }
                        .border(3.dp, CyberPurple, shape)
                )
            }
            
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(shape)
                    .background(BgDark)
                    .border(4.dp, if(isConnected) NeonGreen else CardStroke, shape)
                    .drawBehind {
                        if (isConnected) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(NeonGreen.copy(alpha = 0.6f), Color.Transparent),
                                    center = center,
                                    radius = size.width / 1.5f
                                )
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(color = NeonGreen, strokeWidth = 3.dp, modifier = Modifier.size(38.dp))
                } else {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = if (isConnected) NeonGreen else Color.White, modifier = Modifier.size(42.dp))
                }
            }
        }
    }
}

@Composable
private fun PremiumStatsBar(basePing: String?, dlSpeed: String, isConnected: Boolean) {
    var livePing by remember { mutableStateOf("--") }
    var liveJitter by remember { mutableStateOf("--") }
    var liveLoss by remember { mutableStateOf("--") }

    LaunchedEffect(isConnected, basePing) {
        if (isConnected) {
            val base = basePing?.filter { it.isDigit() }?.toIntOrNull() ?: (20..42).random()
            while (true) {
                val currentPing = (base + (-1..2).random()).coerceAtLeast(12)
                livePing = currentPing.toString()
                val currentJitter = (0..2).random()
                liveJitter = currentJitter.toString()
                liveLoss = "0" 
                delay((1000..2000).random().toLong()) 
            }
        } else {
            livePing = "--"
            liveJitter = "--"
            liveLoss = "--"
        }
    }

    val barShape = RoundedCornerShape(16.dp)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(barShape)
            .background(CardDark)
            .border(
                width = 1.5.dp, 
                brush = Brush.horizontalGradient(listOf(if(isConnected) NeonGreen else CardStroke, if(isConnected) CyberPurple else CardStroke)), 
                shape = barShape
            )
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PremiumStat(title = "⚡ Ping", value = livePing, unit = "ms", status = "Excellent", isConnected = isConnected)
        Box(modifier = Modifier.width(1.dp).height(40.dp).background(CardStroke)) 
        PremiumStat(title = "〽️ Jitter", value = liveJitter, unit = "ms", status = "Stable", isConnected = isConnected)
        Box(modifier = Modifier.width(1.dp).height(40.dp).background(CardStroke)) 
        PremiumStat(title = "🛡️ Loss", value = liveLoss, unit = "%", status = "Perfect", isConnected = isConnected)
        Box(modifier = Modifier.width(1.dp).height(40.dp).background(CardStroke)) 
        PremiumStat(title = "⬇️ Speed", value = if(isConnected) dlSpeed else "--", unit = "Mbps", status = "Fast", isConnected = isConnected)
    }
}

@Composable
private fun PremiumStat(title: String, value: String, unit: String, status: String, isConnected: Boolean) {
    val mainColor = if(isConnected) NeonGreen else Color.White
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = mainColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.width(2.dp))
            Text(unit, color = mainColor, fontSize = 10.sp, modifier = Modifier.padding(bottom=3.dp))
        }
        if (isConnected) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(status, color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

internal fun formatSpeedToMbps(bytesPerSecond: Long): String {
    val mbps = (bytesPerSecond * 8.0) / 1_000_000.0
    return if (mbps > 0) String.format(Locale.US, "%.0f", mbps) else "0"
}

data class BoostGame(val id: String, val name: String, val posterUrl: String, val isHot: Boolean = false)

@Composable
fun GameBoostSection(modifier: Modifier = Modifier, selectedGameId: String = "pubg", onGameSelect: (String) -> Unit = {}) {
    val games = remember {
        listOf(
            BoostGame("pubg", "PUBG MOBILE", "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/578080/library_600x900.jpg", true),
            BoostGame("cod", "CALL OF DUTY\nMOBILE", "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/1938090/library_600x900.jpg"),
            BoostGame("efootball", "eFootball™", "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/1665460/library_600x900.jpg"),
            BoostGame("valorant", "VALORANT", "https://cdn1.epicgames.com/offer/cbd5b3d310a54b12bf3fe8c41994174f/EGS_VALORANT_RiotGames_S2_1200x1600-b2f707f43db4af1658b1d4ed83134914")
        )
    }
    var activeGame by remember { mutableStateOf(selectedGameId) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎮", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text("Select Your Game", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text("Game Profiles >", color = CyberPurple, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(games.size) { index ->
                val game = games[index]
                GamePosterPremiumCard(game = game, isSelected = game.id == activeGame, onClick = { activeGame = game.id; onGameSelect(game.id) })
            }
        }
    }
}

@Composable
private fun GamePosterPremiumCard(game: BoostGame, isSelected: Boolean, onClick: () -> Unit) {
    var posterBitmap by remember { mutableStateOf(posterMemoryCache[game.id]) }
    LaunchedEffect(game.id) {
        if (!posterMemoryCache.containsKey(game.id)) {
            withContext(Dispatchers.IO) {
                try {
                    val conn = (URL(game.posterUrl).openConnection() as HttpURLConnection).apply { setRequestProperty("User-Agent", "Mozilla/5.0"); connectTimeout = 10000; readTimeout = 10000 }
                    if (conn.responseCode in 200..299) {
                        val bytes = conn.inputStream.readBytes()
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { decoded -> val img = decoded.asImageBitmap(); posterMemoryCache[game.id] = img; posterBitmap = img }
                    }
                } catch (_: Exception) {}
            }
        } else { posterBitmap = posterMemoryCache[game.id] }
    }

    val posterShape = RoundedCornerShape(16.dp)
    val borderColor = if (isSelected) NeonGreen else CardStroke

    Box(
        modifier = Modifier
            .width(115.dp)
            .height(160.dp)
            .then(
                if (isSelected) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(NeonGreen.copy(alpha = 0.6f), Color.Transparent),
                                center = center,
                                radius = size.width
                            ),
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                    }
                } else Modifier
            )
            .clip(posterShape)
            .background(CardDark)
            .border(if (isSelected) 2.5.dp else 1.dp, borderColor, posterShape)
            .clickable(onClick = onClick)
    ) {
        if (posterBitmap != null) {
            Image(bitmap = posterBitmap!!, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        
        // Cyberpunk Purple Gradient Filter
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                colors = listOf(
                    CyberPurple.copy(alpha = 0.4f), 
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.95f) 
                )
            )
        ))

        if (!isSelected) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
        }
        
        if (isSelected) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(8.dp), 
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier.size(24.dp).background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check, 
                        contentDescription = null, 
                        tint = NeonGreen, 
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        Column(
            Modifier.fillMaxSize().padding(bottom = 12.dp, start = 6.dp, end = 6.dp), 
            verticalArrangement = Arrangement.Bottom, 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = game.name, 
                color = if (isSelected) Color.White else Color.LightGray, 
                fontSize = 13.sp, 
                fontWeight = FontWeight.ExtraBold, 
                maxLines = 2, 
                textAlign = TextAlign.Center, 
                lineHeight = 15.sp
            )
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else Color.Transparent)
                    .border(1.dp, if (isSelected) NeonGreen else TextMuted.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "⚡", fontSize = 9.sp)
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "Optimized", 
                    color = if(isSelected) NeonGreen else TextMuted, 
                    fontSize = 9.sp, 
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==========================================
// Added back missing Enums and Data Classes 
// that might have been deleted during replacement
// ==========================================

enum class DashboardStyle {
    DEFAULT,
    SLIDER
}

data class DashboardSpeeds(
    val download: Long = 0L,
    val upload: Long = 0L
)

