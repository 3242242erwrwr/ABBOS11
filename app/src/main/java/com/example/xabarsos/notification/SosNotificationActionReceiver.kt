package com.example.xabarsos.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.xabarsos.audio.SosAlertManager
import com.example.xabarsos.data.SosRepository

class SosNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("SosNotificationReceiver", "Action received: ${intent?.action}")

        if (intent?.action == SosNotificationManager.ACTION_DISMISS_ALARM) {
            try {
                SosAlertManager.stopAllAlerts()

                val repository = SosRepository.getInstance(context.applicationContext)
                repository.dismissActiveAlert()

                val notificationManager = SosNotificationManager(context.applicationContext)
                notificationManager.cancelEmergencyNotification()
            } catch (e: Exception) {
                Log.e("SosNotificationReceiver", "Error stopping alarm: ${e.message}")
            }
        }
    }
}
