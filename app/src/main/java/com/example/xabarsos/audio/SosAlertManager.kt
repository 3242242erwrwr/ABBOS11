package com.example.xabarsos.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.util.Log

enum class SosSoundType(val displayName: String, val ringtoneType: Int) {
    ALARM("🚨 Sirena / Budilnik (Baland)", RingtoneManager.TYPE_ALARM),
    RINGTONE("📞 Qo'ng'iroq Ovozi (Ringtone)", RingtoneManager.TYPE_RINGTONE),
    NOTIFICATION("🔔 Bildirishnoma (Notification)", RingtoneManager.TYPE_NOTIFICATION)
}

class SosAlertManager(private val context: Context) {

    companion object {
        @Volatile
        private var globalMediaPlayer: MediaPlayer? = null

        @Volatile
        private var globalVibrator: Vibrator? = null

        @Volatile
        private var isRinging = false

        fun stopAllAlerts() {
            isRinging = false

            try {
                globalMediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.stop()
                    }
                    player.release()
                }
            } catch (e: Exception) {
                Log.e("SosAlertManager", "Error stopping global media player: ${e.message}")
            } finally {
                globalMediaPlayer = null
            }

            try {
                globalVibrator?.cancel()
            } catch (e: Exception) {
                Log.e("SosAlertManager", "Error stopping global vibrator: ${e.message}")
            } finally {
                globalVibrator = null
            }
        }
    }

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    @Suppress("DEPRECATION")
    private fun getVibratorInstance(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun playAlertSoundAndVibrate(soundType: SosSoundType = SosSoundType.ALARM) {
        stopAllAlerts()
        isRinging = true

        // 1. WAKE UP SCREEN IF DARK/LOCKED AND MAXIMIZE VOLUME TO 100% FOR EMERGENCY SOS
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val screenWakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "XABARSOS::WakeUpScreenEmergency"
            )
            screenWakeLock.acquire(10000) // Wake screen up for 10 seconds
        } catch (e: Exception) {
            Log.e("SosAlertManager", "Error waking up screen: ${e.message}")
        }

        try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

            val maxMusicVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusicVolume, 0)
        } catch (e: Exception) {
            Log.e("SosAlertManager", "Error maximizing volume: ${e.message}")
        }

        try {
            // Get selected sound URI
            val alertUri = RingtoneManager.getDefaultUri(soundType.ringtoneType)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val newPlayer = MediaPlayer().apply {
                setDataSource(context, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            globalMediaPlayer = newPlayer
        } catch (e: Exception) {
            Log.e("SosAlertManager", "Error playing alert sound: ${e.message}")
        }

        try {
            // Emergency Vibration pattern: [delay, vibrate, pause, vibrate, ...]
            val pattern = longArrayOf(0, 500, 200, 500, 200, 800)
            val vibrator = getVibratorInstance()
            globalVibrator = vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, 0)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("SosAlertManager", "Error starting vibration: ${e.message}")
        }
    }

    fun testSound(soundType: SosSoundType) {
        stopAllAlerts()
        playAlertSoundAndVibrate(soundType)
    }

    fun stopAlertSoundAndVibrate() {
        stopAllAlerts()
    }

    fun isAlertActive(): Boolean = isRinging
}
