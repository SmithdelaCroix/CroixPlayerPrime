/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.data

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.croix.prime.engine.AcousticSignature
import com.croix.prime.engine.ShazamSignatureGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Captures 4.5 seconds of 16 kHz mono PCM16 audio via Android AudioRecord,
 * streams real-time normalized mic amplitude for the listening radar UI,
 * and compiles a Shazam-compatible acoustic fingerprint signature.
 */
object AudioFingerprintRecorder {
    @SuppressLint("MissingPermission")
    suspend fun recordSignature(
        durationMs: Long = 4_600L,
        onLevelUpdate: (amplitude01: Float, progress01: Float) -> Unit,
    ): Result<AcousticSignature> = withContext(Dispatchers.IO) {
        runCatching {
            val sampleRate = 16_000
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(4096)

            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2,
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                error("No se pudo inicializar el micrófono")
            }

            val generator = ShazamSignatureGenerator(maxTimeSeconds = (durationMs / 1000.0) - 0.3)
            val buffer = ShortArray(2048)
            val startTime = System.currentTimeMillis()
            try {
                recorder.startRecording()
                while (currentCoroutineContext().isActive) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed >= durationMs) break
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        generator.feedPcm16Mono(buffer, read)
                        var sumSq = 0.0
                        for (i in 0 until read) {
                            val v = buffer[i].toDouble()
                            sumSq += v * v
                        }
                        val rms = sqrt(sumSq / read) / 14_000.0
                        val level = abs(rms).toFloat().coerceIn(0f, 1f)
                        val progress = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        onLevelUpdate(level, progress)
                    }
                }
            } finally {
                runCatching { recorder.stop() }
                runCatching { recorder.release() }
            }

            generator.buildSignatureOrNull()
                ?: error("No se detectó suficiente señal de audio")
        }
    }
}
