package com.example.xabarsos.audio

import kotlin.math.sin

enum class VoiceEffect(
    val displayName: String,
    val emoji: String,
    val pitchRatio: Float,
    val isRobot: Boolean = false
) {
    NORMAL("Oddiy", "👤", 1.0f),
    ROBOT("Robot", "🤖", 1.0f, isRobot = true),
    HORROR("Horror", "👹", 0.72f),
    BABY("Chaqa", "👶", 1.42f),
    OLD_MAN("Qariya", "👴", 0.82f),
    GIRL("Qiz bola", "👧", 1.22f)
}

object VoiceEffectProcessor {

    fun applyEffect(pcmShorts: ShortArray, effect: VoiceEffect, sampleRate: Int = 16000): ShortArray {
        if (effect == VoiceEffect.NORMAL) {
            return pcmShorts
        }

        var processed = pcmShorts

        // 1. Robot Ring Modulation Effect
        if (effect.isRobot) {
            processed = applyRobotEffect(processed, sampleRate)
        }

        // 2. Pitch Resampling Effect (Baby, Horror, Old Man, Girl)
        if (effect.pitchRatio != 1.0f) {
            processed = resamplePitch(processed, effect.pitchRatio)
        }

        return processed
    }

    private fun applyRobotEffect(pcmShorts: ShortArray, sampleRate: Int): ShortArray {
        val out = ShortArray(pcmShorts.size)
        val carrierFreq = 140.0 // 140Hz carrier tone for metallic robot sound
        val twoPi = 2.0 * Math.PI

        for (i in pcmShorts.indices) {
            val t = i.toDouble() / sampleRate.toDouble()
            val modulator = sin(twoPi * carrierFreq * t)
            val sample = pcmShorts[i].toDouble()
            val RobotSample = (sample * (0.6 + 0.4 * modulator)).toInt()
            out[i] = RobotSample.coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    private fun resamplePitch(pcmShorts: ShortArray, pitchRatio: Float): ShortArray {
        if (pcmShorts.isEmpty() || pitchRatio <= 0f) return pcmShorts

        val newLength = (pcmShorts.size / pitchRatio).toInt()
        if (newLength <= 0) return pcmShorts

        val output = ShortArray(newLength)

        for (i in 0 until newLength) {
            val srcIndex = i * pitchRatio
            val indexFloor = srcIndex.toInt()
            val indexCeil = (indexFloor + 1).coerceAtMost(pcmShorts.size - 1)
            val fraction = srcIndex - indexFloor

            val sampleFloor = pcmShorts.getOrElse(indexFloor) { 0 }.toFloat()
            val sampleCeil = pcmShorts.getOrElse(indexCeil) { 0 }.toFloat()

            val interpolated = sampleFloor + fraction * (sampleCeil - sampleFloor)
            output[i] = interpolated.toInt().coerceIn(-32768, 32767).toShort()
        }

        return output
    }
}
