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
import kotlin.math.exp
import kotlin.math.sin

/**
 * Real-time Media3 PCM 16-bit Stereo AudioProcessor combining:
 * 1. Karaoke Vocal Remover DSP: Mid/Side center-channel vocal cancellation with a 190 Hz
 *    1st-order low-pass filter on the Mid channel so bass guitar & kick drums remain punchy
 *    while center-panned lead vocals are strongly reduced in real time.
 * 2. 8D Spatial Stereo Audio: Smooth equal-power sinusoidal L/R orbital panning.
 */
@OptIn(UnstableApi::class)
class Croix8dAudioProcessor : BaseAudioProcessor() {
    @Volatile
    var enabled: Boolean = false

    @Volatile
    var speed: Float = 0.45f // 0.1..1.0 -> 0.08..0.48 Hz rotation

    @Volatile
    var karaokeEnabled: Boolean = false

    @Volatile
    var karaokeStrength: Float = 0.88f // 0.3..1.0 vocal reduction depth

    private var phaseRad: Double = 0.0
    private var sampleRateHz: Int = 48_000
    private var channelCount: Int = 2
    private var bassLpfState: Double = 0.0
    private var bassLpfAlpha: Double = 0.025

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        sampleRateHz = inputAudioFormat.sampleRate.coerceAtLeast(8_000)
        channelCount = inputAudioFormat.channelCount
        // 1st-order low-pass cutoff at ~185 Hz to preserve center bass/kick in Karaoke mode
        bassLpfAlpha = 1.0 - exp(-2.0 * PI * 185.0 / sampleRateHz.toDouble())
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || channelCount != 2) {
            return AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining <= 0) return

        val out = replaceOutputBuffer(remaining)
        val do8d = enabled
        val doKaraoke = karaokeEnabled
        if ((!do8d && !doKaraoke) || channelCount != 2) {
            out.put(inputBuffer)
            out.flip()
            return
        }

        val freqHz = 0.08 + speed.coerceIn(0.1f, 1.0f) * 0.40
        val phaseStep = (2.0 * PI * freqHz) / sampleRateHz.toDouble()
        val vocalKeep = 1.0 - karaokeStrength.coerceIn(0.3f, 1.0f)
        val alpha = bassLpfAlpha
        val frames = remaining / 4

        for (f in 0 until frames) {
            var curL = inputBuffer.short.toDouble()
            var curR = inputBuffer.short.toDouble()

            // Stage 1: Real-time Karaoke Vocal Remover (preserves <185 Hz bass + boosts stereo accompaniment)
            if (doKaraoke) {
                val mid = (curL + curR) * 0.5
                val side = (curL - curR) * 0.5
                bassLpfState += alpha * (mid - bassLpfState)
                val midBass = bassLpfState
                val midVocalBand = mid - midBass
                val reducedMid = midBass * 0.96 + midVocalBand * vocalKeep
                val wideSide = side * 1.30
                curL = reducedMid + wideSide
                curR = reducedMid - wideSide
            }

            // Stage 2: 8D Spatial Stereo Orbital Panner
            if (do8d) {
                val pan01 = (sin(phaseRad) + 1.0) * 0.5
                val angle = pan01 * (PI * 0.5)
                val gainL = 0.24 + 0.76 * cos(angle)
                val gainR = 0.24 + 0.76 * sin(angle)
                val nextL = curL * gainL + curR * 0.12 * gainL
                val nextR = curR * gainR + curL * 0.12 * gainR
                curL = nextL
                curR = nextR
                phaseRad += phaseStep
                if (phaseRad >= 2.0 * PI) phaseRad -= 2.0 * PI
            }

            out.putShort(curL.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
            out.putShort(curR.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
        }

        while (inputBuffer.hasRemaining()) {
            out.put(inputBuffer.get())
        }
        out.flip()
    }

    override fun onFlush() {
        bassLpfState = 0.0
    }

    override fun onReset() {
        phaseRad = 0.0
        bassLpfState = 0.0
    }
}
