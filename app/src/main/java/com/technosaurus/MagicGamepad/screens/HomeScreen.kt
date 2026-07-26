package com.technosaurus.MagicGamepad.screens

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.navigation.NavController
private const val HELP_URL = "https://technosaurus8.github.io/MagicGamepad/"

private fun openHelpPage(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, HELP_URL.toUri())
    try {
        context.startActivity(Intent.createChooser(intent, null))
    } catch (_: Exception) { }
}
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    var showBtDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07080F))
            .drawBehind {
                val step = 40.dp.toPx()
                val lineColor = Color(0xFF303C75)
                var x = 0f
                while (x <= size.width) {
                    drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
            }
    ) {
        Column(Modifier.fillMaxSize()
            .systemBarsPadding()) {
            val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            if (isLandscape) {
                // ── Landscape: two-column layout ──────────────────────────────
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: title
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Magic Gamepad", color = Color.White,
                            fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Transform your phone into a Gamepad",
                            color = Color.LightGray, fontSize = 14.sp)
                    }

                    // Right: buttons
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FeatureButton("Bluetooth Connect", Icons.Default.Bluetooth,
                            Color(0xFF2563EB)) {
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) showBtDialog = true
                            else navController.navigate("bt_select")
                        }
                        FeatureButton("Wi-Fi Connect", Icons.Default.Wifi,
                            Color(0xFF10B981)) { navController.navigate("wifi_select") }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SmallFeatureButton("Settings", Icons.Default.Settings,
                                Color(0xFF7C3AED)) { navController.navigate("settings") }
                            SmallFeatureButton("Help", Icons.AutoMirrored.Filled.Help,
                                Color(0xFF0EA5E9)) {openHelpPage(context) }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Magic Gamepad",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Transform your phone into a Gamepad",
                        color = Color.LightGray,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                    FeatureButton(
                        title = "Bluetooth Connect",
                        icon = Icons.Default.Bluetooth,
                        buttonColor = Color(0xFF2563EB)
                    ) {
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) showBtDialog = true
                        else navController.navigate("bt_select")
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    FeatureButton(
                        title = "Wi-Fi Connect",
                        icon = Icons.Default.Wifi,
                        buttonColor = Color(0xFF10B981)
                    ) { navController.navigate("wifi_select") }
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        SmallFeatureButton(
                            title = "Settings",
                            icon = Icons.Default.Settings,
                            buttonColor = Color(0xFF7C3AED)
                        ) { navController.navigate("settings") }
                        SmallFeatureButton(
                            title = "Help",
                            icon = Icons.AutoMirrored.Filled.Help,
                            buttonColor = Color(0xFF0EA5E9)
                        ) {openHelpPage(context) }
                    }
                }
            }
        }
    }
    // Add after the Box closes:
    if (showBtDialog) {
        BtModeDialog(
            onDismiss = { showBtDialog = false },
            onServerMode  = { showBtDialog = false; navController.navigate("bt_select") },
            onGenericMode = { showBtDialog = false; navController.navigate("bt_hid_select") }
        )
    }
}
@Composable
private fun BtModeDialog(
    onDismiss: () -> Unit,
    onServerMode: () -> Unit,
    onGenericMode: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0E1628))
                .border(1.dp, Color(0xFF1A2540), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text(
                text = "Bluetooth Connect",
                color = Color(0xFFE8F0FF),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Choose how to connect",
                color = Color(0xFF6B7FA8),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            BtDialogCard(
                isPro = false,
                icon = Icons.Default.Computer,
                title = "Magic Gamepad Server",
                description = "Requires the Magic Gamepad app running on your Windows PC.",
                buttonText = "Continue",
                onClick = onServerMode
            )
            Spacer(Modifier.height(8.dp))
            BtDialogCard(
                isPro = true,
                icon = Icons.Default.Bluetooth,
                title = "Generic BT controller",
                description = "No server app needed. Acts as a standard HID device works with most Bluetooth-enabled device.",
                buttonText = "Continue",
                onClick = onGenericMode
            )
        }
    }
}

@Composable
private fun BtDialogCard(
    isPro: Boolean,
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    val accentBlue = Color(0xFF3D8EFF)
    val accentCyan = Color(0xFF00D2FF)
    val accentGold = Color(0xFFC9A227)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF080D1A))
            .border(
                width = 1.5.dp,
                color = if (isPro) accentGold.copy(alpha = 0.55f) else accentBlue.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (isPro) accentGold.copy(alpha = 0.12f)
                            else accentBlue.copy(alpha = 0.12f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isPro) accentGold else accentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color(0xFFE8F0FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = description,
                        color = Color(0xFF6B7FA8),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPro) accentGold else accentBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = buttonText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
@Composable
fun FeatureButton(
    title: String,
    icon: ImageVector,
    buttonColor: Color,
    onClick: () -> Unit
) {

    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        label = ""
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .scale(scale)
            .clickable {
                pressed = true
                onClick()
                pressed = false
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = buttonColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 10.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )

            Spacer(modifier = Modifier.width(18.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SmallFeatureButton(
    title: String,
    icon: ImageVector,
    buttonColor: Color,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .width(150.dp)
            .height(120.dp)
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = buttonColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}