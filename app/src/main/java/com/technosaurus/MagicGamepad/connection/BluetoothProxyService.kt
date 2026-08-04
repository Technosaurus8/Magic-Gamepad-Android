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
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat.startForeground
import com.technosaurus.MagicGamepad.R

@RequiresApi(api = Build.VERSION_CODES.Q)
class BluetoothProxyService : Service() {
    private var initialized = false
    private var descriptorMode = BluetoothHidManager.DescriptorMode.ANDROID_GAMEPAD

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "bluetooth_proxy_service"
    }

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
        createNotificationChannel()
        startForeground(
            this,
            NOTIFICATION_ID,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )
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

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Magic Gamepad",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            /* hides the notification badge (the little dot or count number) on your app's home screen icon
             for any alerts sent through that specific notification channel*/
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Magic Gamepad")
            .setContentText("Bluetooth Proxy service running")
            .setSmallIcon(R.drawable.ic_stat_fgs)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}