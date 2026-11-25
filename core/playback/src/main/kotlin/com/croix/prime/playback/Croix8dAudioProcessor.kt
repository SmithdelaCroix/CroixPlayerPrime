/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Real-time Media3 PCM AudioProcessor implementing smooth 8D Spatial Stereo Audio rotation.
 * Uses equal-power sinusoidal L/R panning with a gentle warmth floor so stereo soundstages
 * orbit cleanly around the listener's head without hollowing vocals.
 */
@OptIn(UnstableApi::class)
class Croix8dAudioProcessor : BaseAudioProcessor() {
    @Volatile
    var enabled: Boolean = false

    @Volatile
    var speed: Float = 0.45f // 0.1..1.0 -> maps to 0.08..0.48 Hz rotation

    private var phaseRad: Double = 0.0
    private var sampleRateHz: Int = 48_000
    private var channelCount: Int = 2

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        sampleRateHz = inputAudioFormat.sampleRate.coerceAtLeast(8_000)
        channelCount = inputAudioFormat.channelCount
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || channelCount != 2) {
            return AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining <= 0) return

        val out = replaceOutputBuffer(remaining)
        if (!enabled || channelCount != 2) {
            out.put(inputBuffer)
            out.flip()
            return
        }

        val freqHz = 0.08 + speed.coerceIn(0.1f, 1.0f) * 0.40
        val phaseStep = (2.0 * PI * freqHz) / sampleRateHz.toDouble()
        val frames = remaining / 4 // 2 channels * 2 bytes (PCM 16-bit)

        for (f in 0 until frames) {
            val rawL = inputBuffer.short.toInt()
            val rawR = inputBuffer.short.toInt()

            // Map sin(phase) [-1..1] -> pan angle [0..PI/2] with subtle 0.24 ambient floor
            val pan01 = (sin(phaseRad) + 1.0) * 0.5
            val angle = pan01 * (PI * 0.5)
            val gainL = 0.24 + 0.76 * cos(angle)
            val gainR = 0.24 + 0.76 * sin(angle)

            // Gentle crossfeed for natural 8D spaciousness
            val procL = (rawL * gainL + rawR * 0.12 * gainL).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            val procR = (rawR * gainR + rawL * 0.12 * gainR).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

            out.putShort(procL.toShort())
            out.putShort(procR.toShort())

            phaseRad += phaseStep
            if (phaseRad >= 2.0 * PI) phaseRad -= 2.0 * PI
        }

        // Copy any trailing odd bytes safely
        while (inputBuffer.hasRemaining()) {
            out.put(inputBuffer.get())
        }
        out.flip()
    }

    override fun onFlush() {
        // Keep phase smooth across micro-seeks
    }

    override fun onReset() {
        phaseRad = 0.0
    }
}
