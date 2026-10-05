package com.example.xabarsos.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.CombinedVibration
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.util.Log

class SosAlertManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var isRinging = false

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun playAlertSoundAndVibrate() {
        if (isRinging) return
        isRinging = true

        try {
            // Alarm / Ringtone Sound
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
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
        } catch (e: Exception) {
            Log.e("SosAlertManager", "Error playing alert sound: ${e.message}")
        }

        try {
            // Emergency Vibration pattern: [delay, vibrate, pause, vibrate, ...]
            val pattern = longArrayOf(0, 500, 200, 500, 200, 800)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, 0) // 0 means repeat indefinitely until stop
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("SosAlertManager", "Error starting vibration: ${e.message}")
        }
    }

    fun stopAlertSoundAndVibrate() {
        isRinging = false
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("SosAlertManager", "Error stopping alert sound: ${e.message}")
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("SosAlertManager", "Error stopping vibration: ${e.message}")
        }
    }

    fun isAlertActive(): Boolean = isRinging
}
