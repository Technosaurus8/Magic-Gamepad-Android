package com.technosaurus.MagicGamepad.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.technosaurus.MagicGamepad.connection.BluetoothHidManager
import com.technosaurus.MagicGamepad.connection.BluetoothProxyService
import com.technosaurus.MagicGamepad.connection.BtSocket
import kotlinx.coroutines.delay

// ── Colour palette ──────────────────────────────────────────────────────────
private val BgDeep     = Color(0xFF09070F)
private val BgCard     = Color(0xFF110E1C)
private val AccentBlue = Color(0xFF7C5CFC)
private val AccentCyan = Color(0xFFAA8FFF)
private val TextPrim   = Color(0xFFECE8FF)
private val TextSub    = Color(0xFF7A6FA8)
private val DivColor   = Color(0xFF1E1A30)
private val GlowBlue   = Color(0x227C5CFC)
private val GlowBlob   = Color(0x1A7C5CFC)
private val BtHeaderColors = listOf(Color(0xFF100D1F), Color(0xFF09070F))

// ── Screen states ────────────────────────────────────────────────────────────
@RequiresApi(api = Build.VERSION_CODES.Q)
@Composable
fun BtHidSelectScreen() {
    val context = LocalContext.current

    fun resolveState(): BtState = when {
        !hasBtPermissions(context)            -> BtState.NeedsPermission
        !BtSocket.isBluetoothAvailable() -> BtState.Disabled
        else                         -> BtState.Ready(BtSocket.getPairedDevicesList()?.toList() ?: emptyList())
    }

    var btState by remember { mutableStateOf(resolveState()) }
    val intent = remember {
        Intent(context, BluetoothProxyService::class.java).apply {
            val prefs = context.getSharedPreferences("com.technosaurus.MagicGamepad.preferences", Context.MODE_PRIVATE)
            val mode = BluetoothHidManager.DescriptorMode.entries
                .firstOrNull { it.name == prefs.getString("descriptor_mode_key", null) }
                ?: BluetoothHidManager.DescriptorMode.ANDROID_GAMEPAD
            putExtra("DESCRIPTOR_MODE", mode.name)
        }
    }
    // Permission launcher
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
//        results ->
//        if (results[Manifest.permission.POST_NOTIFICATIONS] == true) {
//            ContextCompat.startForegroundService(context, intent)
//        }
        // the above check is not required because the service can start without the permission.
        // The service will continue running, but the system will suppress the notification from appearing in the status bar.
        // You can still view it in the Foreground Services (FGS) Task Manager.
        results ->
        if (results.containsKey(Manifest.permission.POST_NOTIFICATIONS) && hasBtPermissions(context)) {// start the service even if the notification is not allowed.
            // the if is added for identifying weather the permission request was for notification permission.
            // the bluetooth permission will also trigger this callback so that the check is added.
            ContextCompat.startForegroundService(context, intent)
        }
        btState = resolveState()
    }

    fun requestPermissions() {
        val perms =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        else
            arrayOf(Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH)
        permLauncher.launch(perms)
    }
    // BT state broadcast receiver
    DisposableEffect(Unit) {
        if (hasBtPermissions(context)) {
            ContextCompat.startForegroundService(context, intent)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED,
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        btState = resolveState()  // re-fetches paired list automatically
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
            val activity = context as? Activity
            if (activity?.isChangingConfigurations != true) {
                context.stopService(intent)
            }
        } // won't dispose when navigating to remote activity. this is intended
    }

    // Auto-request on first composition if needed
    LaunchedEffect(Unit) {
        if (btState is BtState.NeedsPermission) requestPermissions()
    }


    // ── UI ───────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        // Ambient glow blob
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-60).dp, y = (-80).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(GlowBlob, Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            // ── Header ───────────────────────────────────────────────────────
            BtHeader(onRefresh = { btState = resolveState() })
            // ── Content area ─────────────────────────────────────────────────
            AnimatedContent(
                targetState = btState,
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 8 })
                        .togetherWith(fadeOut(tween(200)))
                },
                label = "bt_state"
            ) { state ->
                when (state) {
                    BtState.NeedsPermission -> PermissionPlaceholder(onGrant = ::requestPermissions)
                    BtState.Disabled -> DisabledPlaceholder()
                    is BtState.Ready        -> DeviceList(
                        devices  = state.devices,
                        onSelect = { device ->
                            val intent = Intent(context, RemoteActivity::class.java)
                            intent.putExtra("selected_device_generic", device)
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}
// ── Header ───────────────────────────────────────────────────────────────────
@Composable
private fun BtHeader(onRefresh: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f, label = "pulse_alpha",
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = BtHeaderColors,
                    start = Offset(0f, 0f), end = Offset(0f, Float.POSITIVE_INFINITY)
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated BT icon
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .alpha(pulse * 0.4f)
                            .background(AccentBlue.copy(alpha = 0.25f), CircleShape)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.BluetoothSearching,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Bluetooth Connect",
                        color = TextPrim,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = "Select a paired Bluetooth device",
                        color = TextSub,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh paired devices",
                    tint = AccentCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        // ── Info banner ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AccentBlue.copy(alpha = 0.08f))
                .border(1.dp, AccentBlue.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = AccentCyan.copy(alpha = 0.7f),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Make sure to keep the Bluetooth HID screen running while pairing the device. If the device is already paired unpair and pair the device again while the Bluetooth HID screen is running. if the device is already paired while the Bluetooth HID screen is open no need to re pair it.",
                    color = AccentCyan.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    letterSpacing = 0.2.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = DivColor)
    }
}

// ── Device list ───────────────────────────────────────────────────────────────
@Composable
private fun DeviceList(devices: List<String>, onSelect: (String) -> Unit) {
    if (devices.isEmpty()) {
        EmptyDevicesPlaceholder()
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        itemsIndexed(devices) { index, deviceName ->
            DeviceRow(name = deviceName, index = index, onClick = { onSelect(deviceName) })
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun DeviceRow(name: String, index: Int, onClick: () -> Unit) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        anim.animateTo(1f, tween(350))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = anim.value
                translationY = (1f - anim.value) * 24f
            }
            .clip(RoundedCornerShape(14.dp))
            .background(BgCard)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(AccentBlue.copy(alpha = 0.25f), Color.Transparent)
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Device icon badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(AccentBlue.copy(alpha = 0.2f), AccentCyan.copy(alpha = 0.1f))
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bluetooth,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = name,
                    color    = TextPrim,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Arrow indicator
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(GlowBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bluetooth,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ── Placeholder screens ───────────────────────────────────────────────────────
@Composable
private fun DisabledPlaceholder() {
    CenteredPlaceholder(
        icon = {
            Icon(
                imageVector = Icons.Rounded.BluetoothDisabled,
                contentDescription = null,
                tint = TextSub,
                modifier = Modifier.size(52.dp)
            )
        },
        title   = "Bluetooth is off",
        subtitle = "Enable Bluetooth on your device\nto see paired devices.",
        buttonText = null, onButton = {}
    )
}

@Composable
private fun EmptyDevicesPlaceholder() {
    CenteredPlaceholder(
        icon = {
            Icon(
                imageVector = Icons.Rounded.Bluetooth,
                contentDescription = null,
                tint = AccentBlue.copy(alpha = 0.5f),
                modifier = Modifier.size(52.dp)
            )
        },
        title    = "No paired devices",
        subtitle = "Pair a device in your phone's\nBluetooth settings, then return here.",
        buttonText = null, onButton = {}
    )
}

@Composable
private fun PermissionPlaceholder(onGrant: () -> Unit) {
    CenteredPlaceholder(
        icon = {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(52.dp)
            )
        },
        title      = "Permission needed",
        subtitle   = "Bluetooth access is required\nto discover paired devices.",
        buttonText = "Grant Permission",
        onButton   = onGrant
    )
}

@Composable
private fun CenteredPlaceholder(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    buttonText: String?,
    onButton: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_scale")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.94f, targetValue = 1.06f, label = "scale",
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse)
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
                .size(88.dp)
                .scale(scale)
                .background(BgCard, CircleShape)
                .border(1.dp, DivColor, CircleShape),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.height(24.dp))
        Text(
            text       = title,
            color      = TextPrim,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = subtitle,
            color     = TextSub,
            fontSize  = 13.sp,
            lineHeight = 20.sp,
            textAlign  = TextAlign.Center
        )
        if (buttonText != null) {
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onButton,
                colors  = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape   = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text     = buttonText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
