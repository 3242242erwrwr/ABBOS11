package com.example.xabarsos.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.xabarsos.MainActivity
import com.example.xabarsos.R
import com.example.xabarsos.data.SosRepository

class SosForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "xabar_sos_background_channel"
        const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, SosForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private var partialWakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var repository: SosRepository? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("SosForegroundService", "SosForegroundService Created")

        // 1. Acquire Partial WakeLock to keep CPU awake when screen is off
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            partialWakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "XABARSOS::BackgroundCpuWakeLock"
            ).apply {
                acquire()
            }
        } catch (e: Exception) {
            Log.e("SosForegroundService", "Error acquiring WakeLock: ${e.message}")
        }

        // 2. Acquire WifiLock to keep Wi-Fi active when screen is dark
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wifiLock = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "XABARSOS::BackgroundWifiLock"
            ).apply {
                acquire()
            }
        } catch (e: Exception) {
            Log.e("SosForegroundService", "Error acquiring WifiLock: ${e.message}")
        }

        // 3. Create Notification Channel and Start Foreground Service
        createNotificationChannel()
        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        // 4. Instantiate Repository to keep WebSocket & Bluetooth connected 24/7
        repository = SosRepository.getInstance(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Return START_STICKY so Android automatically restarts service if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SosForegroundService", "SosForegroundService Destroyed")
        try {
            if (partialWakeLock?.isHeld == true) {
                partialWakeLock?.release()
            }
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (e: Exception) {
            Log.e("SosForegroundService", "Error releasing locks: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "XABAR SOS Fon Hizmati",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SOS xabarlarini ekran qorong'i bo'lganda ham uzluksiz qabul qilish xizmati"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚨 XABAR SOS Tizimi Aktiv")
            .setContentText("Ekran qorong'i bo'lganda ham SOS xabarlari uzluksiz qabul qilinadi")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
