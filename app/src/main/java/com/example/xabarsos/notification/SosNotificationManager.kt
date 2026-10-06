package com.example.xabarsos.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.xabarsos.MainActivity
import com.example.xabarsos.R
import com.example.xabarsos.model.SosMessage

class SosNotificationManager(private val context: Context) {

    companion object {
        const val HEADS_UP_CHANNEL_ID = "xabar_sos_telegram_popup_v6"
        const val EMERGENCY_NOTIFICATION_ID = 9999
        const val ACTION_DISMISS_ALARM = "com.example.xabarsos.ACTION_DISMISS_ALARM"
    }

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createHighPriorityNotificationChannel()
    }

    private fun createHighPriorityNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                HEADS_UP_CHANNEL_ID,
                "🚨 Telegram Style SOS Popup Banners",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Telegram kabi ekran tepasidan ovoz bilan tushuvchi zudlikli SOS xabarnomasi"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 800)
                setSound(alarmUri, audioAttributes)
                enableLights(true)
                lightColor = Color.RED
                setBypassDnd(true) // Bypass Do Not Disturb mode
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showHeadsUpSosNotification(sosMessage: SosMessage) {
        // PendingIntent to launch app when notification clicked
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            sosMessage.id.hashCode(),
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // PendingIntent for Dismiss/Mute action button
        val dismissIntent = Intent(context, SosNotificationActionReceiver::class.java).apply {
            action = ACTION_DISMISS_ALARM
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            sosMessage.id.hashCode() + 1,
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, HEADS_UP_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🚨 SHOSHILINCH SOS: ${sosMessage.senderName}")
            .setContentText("${sosMessage.messageText} (Kimga: ${sosMessage.targetRecipient})")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Yuboruvchi: ${sosMessage.senderName}\nXabar: ${sosMessage.messageText}\nQabul qiluvchi: ${sosMessage.targetRecipient}")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX) // Telegram style High Priority Heads-Up Pop-up
            .setCategory(NotificationCompat.CATEGORY_MESSAGE) // CATEGORY_MESSAGE drops down top banner exactly like Telegram
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(contentPendingIntent, true) // Force Heads-Up Banner at TOP of screen!
            .setSound(alarmUri)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 800))
            .setOngoing(false) // Non-ongoing so Android System UI drops down top animated pop-up banner!
            .setAutoCancel(true)
            .addAction(
                R.mipmap.ic_launcher,
                "🛑 BEKOR QILISH",
                dismissPendingIntent
            )
            .addAction(
                R.mipmap.ic_launcher,
                "📱 ILOVANI OCHISH",
                contentPendingIntent
            )

        val notificationId = Math.abs(sosMessage.id.hashCode())
        notificationManager.notify(notificationId, builder.build())
    }

    fun cancelEmergencyNotification() {
        notificationManager.cancelAll()
    }
}
