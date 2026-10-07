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
        const val VOICE_NOTE_CHANNEL_ID = "xabar_sos_voice_note_popup_v1"
        const val EMERGENCY_NOTIFICATION_ID = 9999
        const val ACTION_DISMISS_ALARM = "com.example.xabarsos.ACTION_DISMISS_ALARM"
    }

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createHighPriorityNotificationChannels()
    }

    private fun createHighPriorityNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            // 1. SOS Emergency Text Channel (With Alarm Sound)
            val channelText = NotificationChannel(
                HEADS_UP_CHANNEL_ID,
                "🚨 Telegram Style SOS Popup Banners",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "SOS xabarnomasi"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 800)
                setSound(alarmUri, audioAttributes)
                enableLights(true)
                lightColor = Color.RED
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // 2. Voice Note / Galasavoy Channel (SILENT notification sound so ONLY voice note plays!)
            val channelVoice = NotificationChannel(
                VOICE_NOTE_CHANNEL_ID,
                "🎙️ Voice Note Galasavoy Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ovozli xabarnoma"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
                setSound(null, null) // SILENT so no background alarm music plays!
                enableLights(true)
                lightColor = Color.GREEN
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(channelText)
            notificationManager.createNotificationChannel(channelVoice)
        }
    }

    fun showHeadsUpSosNotification(sosMessage: SosMessage) {
        val isVoiceNote = !sosMessage.audioData.isNullOrBlank()

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            sosMessage.id.hashCode(),
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val dismissIntent = Intent(context, SosNotificationActionReceiver::class.java).apply {
            action = ACTION_DISMISS_ALARM
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            sosMessage.id.hashCode() + 1,
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = if (isVoiceNote) VOICE_NOTE_CHANNEL_ID else HEADS_UP_CHANNEL_ID
        val titleText = if (isVoiceNote) "🎙️ OVOZLI XABAR: ${sosMessage.senderName}" else "🚨 SHOSHILINCH SOS: ${sosMessage.senderName}"
        val bodyText = if (isVoiceNote) "▶️ Galasavoy ovozi yangramoqda..." else "${sosMessage.messageText} (Kimga: ${sosMessage.targetRecipient})"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Yuboruvchi: ${sosMessage.senderName}\nXabar: ${sosMessage.messageText}\nQabul qiluvchi: ${sosMessage.targetRecipient}")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(contentPendingIntent, true)
            .setVibrate(if (isVoiceNote) longArrayOf(0, 250, 100, 250) else longArrayOf(0, 500, 200, 500, 200, 800))
            .setOngoing(false)
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

        if (!isVoiceNote) {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(alarmUri)
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        } else {
            builder.setSound(null) // SILENT for voice notes so ONLY the voice is heard!
        }

        val notificationId = Math.abs(sosMessage.id.hashCode())
        notificationManager.notify(notificationId, builder.build())
    }

    fun cancelEmergencyNotification() {
        notificationManager.cancelAll()
    }
}
