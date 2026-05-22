package com.technosaurus.MagicGamepad.screens
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.technosaurus.MagicGamepad.ui.AdBanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import com.technosaurus.MagicGamepad.R

// ── Palette ───────────────────────────────────────────────────────────────────
private val BgDeep      = Color(0xFF060E0C)
private val BgCard      = Color(0xFF0C1A16)
private val BgField     = Color(0xFF0A1510)
private val AccentGreen = Color(0xFF00E5A0)
private val AccentTeal  = Color(0xFF00B4D8)
private val AccentWarn  = Color(0xFFFFB347)
private val TextPrim    = Color(0xFFDEF5EE)
private val TextSub     = Color(0xFF4A7A6A)
private val DivColor    = Color(0xFF112218)
private val GlowGreen   = Color(0x1800E5A0)

// ── Mock data model ───────────────────────────────────────────────────────────
data class WifiDevice(
    val ip: String,
)

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun WifiSelectScreen() {
    val context = LocalContext.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    // UI state
    var isScanning by remember { mutableStateOf(false) }
    var showManualEntry by remember { mutableStateOf(false) }
    var manualIp by remember { mutableStateOf("") }
    var manualIpError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val discoveredDevices = remember { mutableStateListOf<WifiDevice>() }
    val prefs = remember {
        context.getSharedPreferences(
            "com.technosaurus.MagicGamepad.preferences",
            android.content.Context.MODE_PRIVATE
        )
    }
    val scanPort = remember {
        prefs.getString("wifi_scan_port_key", "8765")?.toIntOrNull() ?: 8765
    }
    val onDeviceSelected = { ip: String ->
        val intent = Intent(context, RemoteActivity::class.java)
        intent.putExtra("selected_device_ip", ip)
        context.startActivity(intent)
    }
    fun getLocalIpAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.hostAddress?.contains(':') == false) {
                        return address.hostAddress
                    }
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getBaseIpAddress(ipAddress: String?): String? {
        if (ipAddress == null) return null
        val octets = ipAddress.split(".")
        return octets.dropLast(1).joinToString(".")
    }


    suspend fun startScan() {
        isScanning = true
        discoveredDevices.clear()
        val localIp = getLocalIpAddress() ?: run {
            isScanning = false
            return
        }
        val baseIp = getBaseIpAddress(localIp) ?: run {
            isScanning = false
            return
        }

        // Launch 255 concurrent socket probes
        coroutineScope {
            (1..255).map { i ->
                async(Dispatchers.IO) {
                    val host = "$baseIp.$i"
                    try {
                        Socket().use { socket ->
                            socket.connect(InetSocketAddress(host, scanPort), 400)
                            // Connection succeeded — add to list on main thread
                            withContext(Dispatchers.Main) {
                                discoveredDevices.add(WifiDevice("$host:$scanPort"))
                            }
                        }
                    } catch (_: IOException) {
                        // Host not reachable, skip
                    }
                }
            }.awaitAll()
        }

        isScanning = false
    }

    fun validateAndConnect() {
        val trimmed = manualIp.trim()
        val ipPortRegex = Regex(
            """^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}""" +
                    """(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?):""" +
                    """(6553[0-5]|655[0-2][0-9]|65[0-4][0-9][0-9]|""" +
                    """6[0-4][0-9][0-9][0-9]|[1-5][0-9]{4}|[1-9][0-9]{0,3})$"""
        )
        if (trimmed.matches(ipPortRegex)) {
            manualIpError = false
            onDeviceSelected(trimmed)
        } else {
            manualIpError = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        // Corner glow
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = 120.dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x1200E5A0), Color.Transparent)),
                    CircleShape
                )
        )

        Column(Modifier.fillMaxSize().systemBarsPadding()) {

            // ── Header ────────────────────────────────────────────────────────
            WifiHeader(isScanning = isScanning)

            // ── Scan button + manual toggle ───────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Scan button
                Button(
                    onClick = { if (!isScanning) { scope.launch { startScan() } } },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen.copy(alpha = if (isScanning) 0.4f else 1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    val scanAnim = rememberInfiniteTransition(label = "spin")
                    val rotation by scanAnim.animateFloat(
                        initialValue = 0f, targetValue = 360f, label = "rot",
                        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing))
                    )
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = BgDeep,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { if (isScanning) rotationZ = rotation }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isScanning) "Scanning…" else "Scan",
                        color = BgDeep,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Manual entry toggle
                Button(
                    onClick = { showManualEntry = !showManualEntry },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showManualEntry) AccentTeal.copy(alpha = 0.2f) else BgCard
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .border(
                            1.dp,
                            if (showManualEntry) AccentTeal else DivColor,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = if (showManualEntry) AccentTeal else TextSub,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Manual IP",
                        color = if (showManualEntry) AccentTeal else TextSub,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            // ── Manual IP entry panel ─────────────────────────────────────────
            AnimatedVisibility(
                visible = showManualEntry,
                enter = expandVertically(tween(300, easing = EaseOutCubic)) + fadeIn(tween(250)),
                exit  = shrinkVertically(tween(250, easing = EaseInOut))    + fadeOut(tween(200))
            ) {
                ManualIpPanel(
                    ip          = manualIp,
                    onIpChange  = { manualIp = it; manualIpError = false },
                    isError     = manualIpError,
                    onConnect   = { focusManager.clearFocus(); validateAndConnect() },
                    onDismiss   = { showManualEntry = false; manualIp = ""; manualIpError = false }
                )
            }

            // ── Section label ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DISCOVERED DEVICES",
                    color = TextSub,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(DivColor)
                )
                if (discoveredDevices.isNotEmpty()) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "${discoveredDevices.size} found",
                        color = AccentGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // ── Device list or empty state ────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                if (discoveredDevices.isEmpty() && !isScanning) {
                    EmptyState()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Spacer(Modifier.height(2.dp)) }
                        if (isScanning && discoveredDevices.isEmpty()) {
                            item { ScanningPlaceholderRow() }
                            item { ScanningPlaceholderRow(alpha = 0.5f) }
                        }
                        itemsIndexed(discoveredDevices) { index, device ->
                            WifiDeviceRow(
                                device = device,
                                index = index,
                                onClick = { onDeviceSelected(device.ip) })
                        }
                        if (isScanning) {
                            item { ScanningPlaceholderRow(alpha = 0.3f) }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
            if (!imeVisible) {
                AdBanner(stringResource(R.string.ad_settings))
            }
        }
        // Trigger scan on first composition
        LaunchedEffect(Unit) { startScan() }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────
@Composable
private fun WifiHeader(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    // Radar ring animation
    val ring1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f, label = "r1",
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing))
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f, label = "r2",
        animationSpec = infiniteRepeatable(tween(2000, 700, easing = LinearEasing))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0A1A14), BgDeep))
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Radar icon with rings
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isScanning) {
                    // Animated radar rings
                    listOf(ring1 to 0.6f, ring2 to 0.6f).forEach { (progress, maxAlpha) ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(0.4f + progress * 0.9f)
                                .alpha((1f - progress) * maxAlpha)
                                .drawBehind {
                                    drawCircle(
                                        color = AccentGreen,
                                        radius = size.minDimension / 2,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.WifiFind,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "Wi-Fi Connect",
                    color = TextPrim,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
                Text(
                    text = "Scan or enter server IP",
                    color = TextSub,
                    fontSize = 12.sp,
                    letterSpacing = 0.4.sp
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(AccentGreen.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )
    }
}

// ── Manual IP Panel ───────────────────────────────────────────────────────────
@Composable
private fun ManualIpPanel(
    ip: String,
    onIpChange: (String) -> Unit,
    isError: Boolean,
    onConnect: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(
                1.dp,
                Brush.linearGradient(listOf(AccentTeal.copy(alpha = 0.4f), AccentGreen.copy(alpha = 0.15f))),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ENTER SERVER IP",
                    color = AccentTeal,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSub, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = ip,
                onValueChange = onIpChange,
                placeholder = {
                    Text(
                        "192.168.x.x:port",
                        color = TextSub,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = if (isError) AccentWarn else AccentTeal, modifier = Modifier.size(18.dp))
                },
                trailingIcon = if (ip.isNotEmpty()) ({
                    IconButton(onClick = { onIpChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = TextSub, modifier = Modifier.size(16.dp))
                    }
                }) else null,
                isError = isError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onConnect() }),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = AccentTeal,
                    unfocusedBorderColor = DivColor,
                    errorBorderColor     = AccentWarn,
                    focusedTextColor     = TextPrim,
                    unfocusedTextColor   = TextPrim,
                    cursorColor          = AccentGreen,
                    focusedContainerColor   = BgField,
                    unfocusedContainerColor = BgField,
                    errorContainerColor     = Color(0xFF1A100A)
                ),
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = TextPrim
                )
            )
            if (isError) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Invalid IP address format",
                    color = AccentWarn,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onConnect,
                enabled = ip.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentTeal,
                    disabledContainerColor = AccentTeal.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = BgDeep, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Connect", color = BgDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ── Device row ────────────────────────────────────────────────────────────────
@Composable
private fun WifiDeviceRow(device: WifiDevice, index: Int, onClick: () -> Unit) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 80L)
        anim.animateTo(1f, tween(380, easing = EaseOutCubic))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = anim.value; translationY = (1f - anim.value) * 20f }
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(AccentGreen.copy(alpha = 0.18f), Color.Transparent)
                ),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Signal icon badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(GlowGreen, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = device.ip,
                    color = TextPrim,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.6.sp
                )
            }
        }
    }
}

// ── Scanning placeholder row ──────────────────────────────────────────────────
@Composable
private fun ScanningPlaceholderRow(alpha: Float = 1f) {
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val offset by shimmer.animateFloat(
        initialValue = -1f, targetValue = 1f, label = "sh_offset",
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing))
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(1.dp, DivColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DivColor, BgCard, DivColor),
                            start  = Offset(offset * 400f, 0f),
                            end    = Offset(offset * 400f + 300f, 0f)
                        )
                    )
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.55f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(DivColor, BgCard, DivColor),
                                start  = Offset(offset * 400f, 0f),
                                end    = Offset(offset * 400f + 300f, 0f)
                            )
                        )
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.38f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(DivColor, BgCard, DivColor),
                                start  = Offset(offset * 400f, 0f),
                                end    = Offset(offset * 400f + 300f, 0f)
                            )
                        )
                )
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyState() {
    val pulse = rememberInfiniteTransition(label = "ep")
    val sc by pulse.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f, label = "ep_sc",
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse)
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(sc)
                .background(BgCard, CircleShape)
                .border(1.dp, DivColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.WifiFind,
                contentDescription = null,
                tint = TextSub,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("No devices found", color = TextPrim, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap Scan to search your local network,\nor enter an IP address manually.",
            color = TextSub,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}