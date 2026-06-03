package com.technosaurus.MagicGamepad
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.technosaurus.MagicGamepad.screens.BtSelectScreen
import com.technosaurus.MagicGamepad.screens.HomeScreen
import com.technosaurus.MagicGamepad.screens.WifiSelectScreen
import com.technosaurus.MagicGamepad.screens.SettingsScreen
import androidx.core.content.edit
import com.technosaurus.MagicGamepad.screens.OnboardingScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val navController = rememberNavController()
                val context = LocalContext.current
                val prefs = remember {
                    context.getSharedPreferences("com.technosaurus.MagicGamepad.preferences", MODE_PRIVATE)
                }
                var showOnboarding by remember {
                    mutableStateOf(!prefs.getBoolean("onboarding_done", false))
                }
                if (showOnboarding) {
                    OnboardingScreen(
                        onFinished = {
                            prefs.edit { putBoolean("onboarding_done", false) }// change to true on release
                            showOnboarding = false// will recompose and navigate to the home screen.
                        }
                    )
                }
                else {
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(navController)
                        }
                        composable("bt_select") {
                            BtSelectScreen()
                        }
                        composable("wifi_select") {
                            WifiSelectScreen()
                        }
                        composable("settings") {
                            SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}

