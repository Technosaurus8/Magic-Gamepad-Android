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
import com.technosaurus.MagicGamepad.screens.BtSelectScreen
import com.technosaurus.MagicGamepad.screens.HomeScreen
import com.technosaurus.MagicGamepad.screens.WifiSelectScreen
import com.technosaurus.MagicGamepad.screens.SettingsScreen
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
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

