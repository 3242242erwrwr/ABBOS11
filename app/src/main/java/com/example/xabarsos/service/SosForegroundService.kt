package com.example.xabarsos.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.xabarsos.MainActivity
import com.example.xabarsos.R
import com.example.xabarsos.data.SosRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var connectivityReceiver: BroadcastReceiver? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val mainHandler = Handler(Looper.getMainLooper())

    private val backgroundHeartbeatRunnable = object : Runnable {
        override fun run() {
            try {
                repository?.webSocketManager?.connect()
            } catch (e: Exception) {
                Log.e("SosForegroundService", "Error in heartbeat: ${e.message}")
            } finally {
                mainHandler.postDelayed(this, 15000) // Repeat every 20 seconds (efficient & battery friendly)
            }
        }
    }

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

        // 5. Register System Network Callback & BroadcastReceiver to immediately reconnect on 4G LTE or Wi-Fi toggle
        registerNetworkCallback()
        registerConnectivityReceiver()

        // 6. Start Doze-Proof CPU WakeLock Heartbeat Loop
        mainHandler.post(backgroundHeartbeatRunnable)
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Log.d("SosForegroundService", "4G / Wi-Fi Network Available. Restoring connection...")
                    handleNetworkRestored()
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                        Log.d("SosForegroundService", "4G / Wi-Fi Internet Validated. Syncing...")
                        handleNetworkRestored()
                    }
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    Log.d("SosForegroundService", "Network connection lost")
                }
            }
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.e("SosForegroundService", "Error registering network callback: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    private fun registerConnectivityReceiver() {
        try {
            connectivityReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    Log.d("SosForegroundService", "CONNECTIVITY_ACTION Broadcast: 4G/Wi-Fi toggled!")
                    handleNetworkRestored()
                }
            }
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(connectivityReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(connectivityReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e("SosForegroundService", "Error registering connectivity receiver: ${e.message}")
        }
    }

    private fun handleNetworkRestored() {
        serviceScope.launch {
            try {
                if (wifiLock?.isHeld == false) {
                    wifiLock?.acquire()
                }
            } catch (e: Exception) {}

            repository?.webSocketManager?.reconnect()
            triggerImmediateSync()

            delay(300)
            repository?.webSocketManager?.reconnect()
            triggerImmediateSync()

            delay(1000)
            repository?.webSocketManager?.reconnect()
            triggerImmediateSync()

            delay(2000)
            repository?.webSocketManager?.reconnect()
            triggerImmediateSync()
        }
    }

    private fun triggerImmediateSync() {
        serviceScope.launch {
            try {
                val currentRepo = repository ?: return@launch
                currentRepo.webSocketManager.fetchRecentSosMessagesHttp(0) { newMsgs ->
                    newMsgs.forEach { msg ->
                        currentRepo.processIncomingSosMessage(msg)
                    }
                }
            } catch (e: Exception) {
                Log.e("SosForegroundService", "Error in immediate sync: ${e.message}")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Return START_STICKY so Android automatically restarts service if killed
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("SosForegroundService", "onTaskRemoved: App swiped away. Auto-restarting background service...")
        try {
            val restartServiceIntent = Intent(applicationContext, SosForegroundService::class.java)
            val restartServicePendingIntent = PendingIntent.getService(
                applicationContext, 1, restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent
            )
        } catch (e: Exception) {
            Log.e("SosForegroundService", "Error auto-restarting service: ${e.message}")
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SosForegroundService", "SosForegroundService Destroyed")
        try {
            mainHandler.removeCallbacks(backgroundHeartbeatRunnable)
            if (partialWakeLock?.isHeld == true) {
                partialWakeLock?.release()
            }
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
            networkCallback?.let {
                val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.unregisterNetworkCallback(it)
            }
            connectivityReceiver?.let {
                unregisterReceiver(it)
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
            .setContentTitle("🚨 XABAR SOS Tizimi Aktiv (24/7)")
            .setContentText("Ekran qorong'i bo'lganda ham SOS xabarlari uzluksiz va kafolatlangan holda keladi")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
