package com.technosaurus.MagicGamepad.connection
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.technosaurus.MagicGamepad.R

@RequiresApi(api = Build.VERSION_CODES.P)
class BluetoothProxyService : Service() {
    private var initialized = false
    private var descriptorMode = BluetoothHidManager.DescriptorMode.ANDROID_GAMEPAD

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                BluetoothAdapter.STATE_ON -> {
                    Log.d("BluetoothProxyService", "BT turned on — registering HID")
                    BluetoothHidManager.register(descriptorMode)
                }
                BluetoothAdapter.STATE_TURNING_OFF -> {
                    // Profile proxy is about to die anyway; clean up explicitly
                    BluetoothHidManager.unregister()
                }
            }
        }
    }
    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
        registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!initialized) {
            descriptorMode = BluetoothHidManager.DescriptorMode.valueOf(
                intent?.getStringExtra("DESCRIPTOR_MODE") ?: BluetoothHidManager.DescriptorMode.ANDROID_GAMEPAD.name
            )
            BluetoothHidManager.register(descriptorMode)
            initialized = true
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(bluetoothStateReceiver)
        BluetoothHidManager.unregister()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "bluetooth_proxy_service"

        val channel = NotificationChannel(
            channelId,
            "Magic Gamepad Pro",
            NotificationManager.IMPORTANCE_LOW
        )

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Magic Gamepad Pro")
            .setContentText("Bluetooth Proxy service running")
            .setSmallIcon(R.drawable.logo)
            .build()
    }
}