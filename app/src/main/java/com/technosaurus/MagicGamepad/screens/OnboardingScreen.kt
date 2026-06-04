package com.technosaurus.MagicGamepad.screens

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.technosaurus.MagicGamepad.R
data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector? = null,
    val drawableIcon: Painter? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            title = "Transform Your Phone Into a Game Controller",
            description = "Magic Gamepad turns your Android device into a versatile game controller for PC.",
            drawableIcon = painterResource(id = R.drawable.logo),
        ),
        OnboardingPage(
            title = "Compatible with Windows Only",
            description = "Magic Gamepad currently supports Windows PCs only. Make sure the Magic Gamepad app is installed and updated to the latest version on your PC.",
            icon = Icons.Outlined.DesktopWindows
        ),
        OnboardingPage(
            title = "Customize Your Controls",
            description = "Visit Settings to customize your controller layout, reposition buttons, and tailor the gamepad to your preferences.",
            icon = Icons.Filled.Tune
        ),
        OnboardingPage(
            title = "Connect Up to 4 Players",
            description = "Enjoy seamless multiplayer with up to 4 players over Wi-Fi, Bluetooth or USB(via usb tethering)",
            icon = Icons.Filled.Devices
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPage(page = pages[page])
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Dot indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        label = "dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // Button row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip button (hidden on last page)
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(onClick = onFinished) {
                        Text("Skip", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                } else {
                    Spacer(modifier = Modifier.width(64.dp))
                }

                // Next / Get Started button
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinished()
                        }
                    }
                ) {
                    Text(
                        text = if (pagerState.currentPage == pages.size - 1) "Get Started" else "Next"
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(page: OnboardingPage) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            if (page.icon != null) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (page.drawableIcon != null) {
                Icon(
                    painter = page.drawableIcon,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = null
                )
            }
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = page.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = page.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    lineHeight = 20.sp
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp)
                .padding(top = 120.dp, bottom = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            if (page.icon != null) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (page.drawableIcon != null) {
                Icon(
                    painter = page.drawableIcon,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = null
                )
            }
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = page.title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = page.description,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                lineHeight = 24.sp
            )
        }
    }
}