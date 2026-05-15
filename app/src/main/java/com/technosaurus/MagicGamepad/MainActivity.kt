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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
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
                        BtSelectScreen { device ->
                            navController.navigate("remote/$device")
                        }
                    }
                    composable("wifi_select") {
                        WifiSelectScreen(
                            onDeviceSelected = { ip ->
                                navController.navigate("remote/$ip")
                            }
                        )
                    }
//                    composable("bt_select") {
//                        BtSelectScreen(
//                            onDeviceSelected = { device ->
//                                val intent = Intent(context, RemoteActivity::class.java)
//                                intent.putExtra("selected_device", device)  // same key as before
//                                context.startActivity(intent)
//                            }
//                        )
//                    }
//                    composable("wifi_select") {
//                        WifiSelectScreen(
//                            onDeviceSelected = { ip ->
//                                val intent = Intent(context, RemoteActivity::class.java)
//                                intent.putExtra("key", ip)
//                                context.startActivity(intent)
//                            }
//                        )
//                    }
                    composable("remote/{device}") { /* RemoteScreen */ }
                }
            }
        }
    }
}

