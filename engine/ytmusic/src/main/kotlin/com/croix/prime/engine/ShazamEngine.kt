/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.engine

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import java.util.UUID
import java.util.zip.CRC32
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

@Serializable
data class ShazamMatch(
    val key: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val coverUrl: String = "",
    val genre: String = "",
    val shazamUrl: String = "",
    val youtubeVideoId: String? = null,
)

data class AcousticSignature(
    val uri: String,
    val sampleDurationMs: Long,
)

/**
 * Pure Kotlin Shazam acoustic landmark fingerprint generator (16 kHz mono PCM16).
 */
class ShazamSignatureGenerator(
    private val maxTimeSeconds: Double = 4.2,
    private val maxPeaks: Int = 255,
) {
    private val pending = IntBufferList()
    private var processedSamples = 0

    private val sampleRateHz = 16000
    private val fft = Radix2Fft(2048)
    private val window = DoubleArray(2048) { i ->
        val n = 2050
        val j = i + 1
        0.5 - 0.5 * cos(2.0 * PI * j / (n - 1))
    }
    private val realBuffer = DoubleArray(2048)
    private val imagBuffer = DoubleArray(2048)

    private val ringSamples = IntArray(2048)
    private var ringSamplePos = 0

    private val fftOutputs = Array(256) { DoubleArray(1025) }
    private var fftPos = 0
    private var fftWritten = 0

    private val spreadOutputs = Array(256) { DoubleArray(1025) }
    private var spreadPos = 0
    private var spreadWritten = 0
    private val originSpread = DoubleArray(1025)

    private var signatureNumberSamples = 0
    private val bandToPeaks = linkedMapOf<FreqBand, MutableList<FreqPeak>>()

    fun feedPcm16Mono(samples: ShortArray, count: Int = samples.size) {
        val limit = count.coerceAtMost(samples.size)
        for (i in 0 until limit) {
            pending.add(samples[i].toInt())
        }
    }

    fun reset() {
        pending.clear()
        processedSamples = 0
        ringSamples.fill(0)
        ringSamplePos = 0
        fftPos = 0
        fftWritten = 0
        spreadPos = 0
        spreadWritten = 0
        originSpread.fill(0.0)
        signatureNumberSamples = 0
        bandToPeaks.clear()
    }

    fun buildSignatureOrNull(): AcousticSignature? {
        if (pending.size - processedSamples < 128) return null

        while (pending.size - processedSamples >= 128 &&
            (signatureNumberSamples.toDouble() / sampleRateHz < maxTimeSeconds ||
                bandToPeaks.values.sumOf { it.size } < maxPeaks)
        ) {
            processBlock(pending, processedSamples, processedSamples + 128)
            processedSamples += 128
        }

        if (bandToPeaks.isEmpty()) return null

        val uri = encodeToUri(
            sampleRateHz = sampleRateHz,
            numberSamples = signatureNumberSamples,
            peaksByBand = bandToPeaks.toSortedMap(compareBy { it.value }).mapValues { it.value.toList() },
        )
        val durationMs = (signatureNumberSamples * 1000L) / sampleRateHz
        reset()
        return AcousticSignature(uri = uri, sampleDurationMs = durationMs)
    }

    private fun processBlock(samples: IntBufferList, start: Int, endExclusive: Int) {
        signatureNumberSamples += (endExclusive - start)
        var pos = start
        while (pos < endExclusive) {
            runFft(samples, pos, pos + 128)
            spreadPeaks()
            if (spreadWritten >= 46) recognizePeaks()
            pos += 128
        }
    }

    private fun runFft(samples: IntBufferList, start: Int, endExclusive: Int) {
        var i = start
        while (i < endExclusive) {
            ringSamples[ringSamplePos] = samples[i]
            ringSamplePos++
            if (ringSamplePos == ringSamples.size) ringSamplePos = 0
            i++
        }

        var p = ringSamplePos
        var idx = 0
        while (idx < 2048) {
            if (p == 2048) p = 0
            realBuffer[idx] = ringSamples[p].toDouble() * window[idx]
            imagBuffer[idx] = 0.0
            p++
            idx++
        }

        fft.transform(realBuffer, imagBuffer)

        val out = fftOutputs[fftPos]
        var k = 0
        while (k <= 1024) {
            val re = realBuffer[k]
            val im = imagBuffer[k]
            val v = (re * re + im * im) / 131072.0
            out[k] = if (v <= 1e-10) 1e-10 else v
            k++
        }

        fftPos++
        if (fftPos == fftOutputs.size) fftPos = 0
        fftWritten++
    }

    private fun spreadPeaks() {
        val lastFftIndex = (fftPos - 1).modFloor(fftOutputs.size)
        val origin = fftOutputs[lastFftIndex]

        var i = 0
        while (i <= 1021) {
            originSpread[i] = max(origin[i], max(origin[i + 1], origin[i + 2]))
            i++
        }
        originSpread[1022] = origin[1022]
        originSpread[1023] = origin[1023]
        originSpread[1024] = origin[1024]

        val s1 = spreadOutputs[(spreadPos - 1).modFloor(spreadOutputs.size)]
        val s2 = spreadOutputs[(spreadPos - 3).modFloor(spreadOutputs.size)]
        val s3 = spreadOutputs[(spreadPos - 6).modFloor(spreadOutputs.size)]

        var bin = 0
        while (bin < 1025) {
            val m1 = max(originSpread[bin], s1[bin])
            s1[bin] = m1
            val m2 = max(m1, s2[bin])
            s2[bin] = m2
            val m3 = max(m2, s3[bin])
            s3[bin] = m3
            bin++
        }

        System.arraycopy(originSpread, 0, spreadOutputs[spreadPos], 0, 1025)
        spreadPos++
        if (spreadPos == spreadOutputs.size) spreadPos = 0
        spreadWritten++
    }

    private fun recognizePeaks() {
        val fftMinus46 = fftOutputs[(fftPos - 46).modFloor(fftOutputs.size)]
        val spreadMinus49 = spreadOutputs[(spreadPos - 49).modFloor(spreadOutputs.size)]

        var bin = 10
        while (bin <= 1014) {
            val energy = fftMinus46[bin]
            if (energy >= (1.0 / 64.0) && energy >= spreadMinus49[bin - 1]) {
                var maxNeighbor = 0.0
                for (offset in FREQ_OFFSETS) {
                    maxNeighbor = max(maxNeighbor, spreadMinus49[bin + offset])
                }
                if (energy > maxNeighbor) {
                    var maxTimeNeighbor = maxNeighbor
                    for (tOffset in TIME_OFFSETS) {
                        val idx = (spreadPos + tOffset).modFloor(spreadOutputs.size)
                        maxTimeNeighbor = max(maxTimeNeighbor, spreadOutputs[idx][bin - 1])
                    }
                    if (energy > maxTimeNeighbor) {
                        val fftNum = spreadWritten - 46
                        val peakMag = ln(max(1.0 / 64.0, energy)) * 1477.3 + 6144.0
                        val magBefore = ln(max(1.0 / 64.0, fftMinus46[bin - 1])) * 1477.3 + 6144.0
                        val magAfter = ln(max(1.0 / 64.0, fftMinus46[bin + 1])) * 1477.3 + 6144.0
                        val var1 = peakMag * 2.0 - magBefore - magAfter
                        if (var1 > 0.0) {
                            val var2 = (magAfter - magBefore) * 32.0 / var1
                            val correctedBin = bin * 64.0 + var2
                            val freqHz = correctedBin * (sampleRateHz / 2.0 / 1024.0 / 64.0)
                            val band = when {
                                freqHz in 250.0..520.0 -> FreqBand.HZ_250_520
                                freqHz > 520.0 && freqHz <= 1450.0 -> FreqBand.HZ_520_1450
                                freqHz > 1450.0 && freqHz <= 3500.0 -> FreqBand.HZ_1450_3500
                                freqHz > 3500.0 && freqHz <= 5500.0 -> FreqBand.HZ_3500_5500
                                else -> null
                            }
                            if (band != null) {
                                bandToPeaks.getOrPut(band) { mutableListOf() }.add(
                                    FreqPeak(fftNum, peakMag.toInt(), correctedBin.toInt()),
                                )
                            }
                        }
                    }
                }
            }
            bin++
        }
    }

    private fun encodeToUri(
        sampleRateHz: Int,
        numberSamples: Int,
        peaksByBand: Map<FreqBand, List<FreqPeak>>,
    ): String {
        val contents = ByteArrayOutputStream()
        for ((band, peaks) in peaksByBand.entries.sortedBy { it.key.value }) {
            val peaksBytes = ByteArrayOutputStream()
            var lastFftPass = 0
            for (peak in peaks) {
                var delta = peak.fftPassNumber - lastFftPass
                if (delta >= 255) {
                    peaksBytes.write(0xFF)
                    peaksBytes.writeLeInt(peak.fftPassNumber)
                    lastFftPass = peak.fftPassNumber
                    delta = 0
                }
                peaksBytes.write(delta.coerceIn(0, 254))
                peaksBytes.writeLeShort(peak.peakMagnitude)
                peaksBytes.writeLeShort(peak.correctedBin)
                lastFftPass = peak.fftPassNumber
            }
            val peaksValue = peaksBytes.toByteArray()
            contents.writeLeInt(0x60030040 + band.value)
            contents.writeLeInt(peaksValue.size)
            contents.write(peaksValue)
            repeat((-peaksValue.size).modFloor(4)) { contents.write(0) }
        }

        val contentsValue = contents.toByteArray()
        val sizeMinusHeader = contentsValue.size + 8
        val header = ByteBuffer.allocate(48).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(0xCAFE2580.toInt())
            putInt(0)
            putInt(sizeMinusHeader)
            putInt(0x94119C00.toInt())
            putInt(0)
            putInt(0)
            putInt(0)
            putInt(3 shl 27) // 16 kHz sample rate id
            putInt(0)
            putInt(0)
            putInt((numberSamples + sampleRateHz * 0.24).toInt())
            putInt((15 shl 19) + 0x40000)
        }

        val full = ByteArrayOutputStream()
        full.write(header.array())
        full.writeLeInt(0x40000000)
        full.writeLeInt(sizeMinusHeader)
        full.write(contentsValue)

        val bytes = full.toByteArray()
        val crc = CRC32().apply { update(bytes, 8, bytes.size - 8) }.value.toInt()
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(4, crc)
        return "data:audio/vnd.shazam.sig;base64," + Base64.getEncoder().encodeToString(bytes)
    }

    private enum class FreqBand(val value: Int) {
        HZ_250_520(0),
        HZ_520_1450(1),
        HZ_1450_3500(2),
        HZ_3500_5500(3),
    }

    private data class FreqPeak(val fftPassNumber: Int, val peakMagnitude: Int, val correctedBin: Int)

    private companion object {
        val FREQ_OFFSETS = intArrayOf(-10, -7, -4, -3, 1, 2, 5, 8)
        val TIME_OFFSETS = intArrayOf(-53, -45, 165, 172, 179, 186, 193, 200, 214, 221, 228, 235, 242, 249)
    }
}

class ShazamRecognitionClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val http = HttpClient(OkHttp) { expectSuccess = false }
    private val userAgents = listOf(
        "Dalvik/2.1.0 (Linux; U; Android 13; Pixel 7 Build/TQ3A.230901.001)",
        "Dalvik/2.1.0 (Linux; U; Android 14; SM-S918B Build/UP1A.231005.007)",
        "Dalvik/2.1.0 (Linux; U; Android 12; Pixel 6 Build/SQ3A.220705.004)",
    )
    private val timezones = listOf("America/Mexico_City", "Europe/Madrid", "America/Bogota", "America/New_York")

    suspend fun recognize(signature: AcousticSignature): Result<ShazamMatch> = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp = System.currentTimeMillis() / 1000L
            val uuid1 = UUID.randomUUID().toString().uppercase()
            val uuid2 = UUID.randomUUID().toString()

            val payload = buildJsonObject {
                putJsonObject("geolocation") {
                    put("altitude", Random.nextDouble() * 400 + 100)
                    put("latitude", Random.nextDouble() * 140 - 70)
                    put("longitude", Random.nextDouble() * 320 - 160)
                }
                putJsonObject("signature") {
                    put("samplems", signature.sampleDurationMs)
                    put("timestamp", timestamp)
                    put("uri", signature.uri)
                }
                put("timestamp", timestamp)
                put("timezone", timezones.random())
            }

            val resp = http.post("https://amp.shazam.com/discovery/v5/es/ES/android/-/tag/$uuid1/$uuid2") {
                parameter("sync", "true")
                parameter("webv3", "true")
                parameter("sampling", "true")
                parameter("connected", "")
                parameter("shazamapiversion", "v3")
                parameter("sharehub", "true")
                parameter("video", "v3")
                header("User-Agent", userAgents.random())
                header("Content-Language", "es_ES")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }

            if (!resp.status.isSuccess()) {
                error("Servicio de reconocimiento respondió HTTP ${resp.status.value}")
            }
            val root = json.parseToJsonElement(resp.bodyAsText()).jsonObject
            val track = root["track"]?.jsonObject ?: error("No se identificó ninguna canción")
            val title = track["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val artist = track["subtitle"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (title.isBlank()) error("No se identificó ninguna canción")

            val images = track["images"]?.jsonObject
            val cover = images?.get("coverarthq")?.jsonPrimitive?.contentOrNull
                ?: images?.get("coverart")?.jsonPrimitive?.contentOrNull
                ?: ""
            val genre = track["genres"]?.jsonObject?.get("primary")?.jsonPrimitive?.contentOrNull.orEmpty()
            val sections = track["sections"]?.jsonArray.orEmpty()
            val album = sections.mapNotNull { it.jsonObject }
                .firstOrNull { it["type"]?.jsonPrimitive?.contentOrNull == "SONG" }
                ?.get("metadata")?.jsonArray
                ?.mapNotNull { it.jsonObject }
                ?.firstOrNull { it["title"]?.jsonPrimitive?.contentOrNull.equals("Album", true) }
                ?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty()

            ShazamMatch(
                key = track["key"]?.jsonPrimitive?.contentOrNull ?: uuid2,
                title = title,
                artist = artist.ifBlank { "Artista desconocido" },
                album = album,
                coverUrl = cover,
                genre = genre,
                shazamUrl = track["url"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            )
        }
    }
}

private fun Int.modFloor(mod: Int): Int {
    val r = this % mod
    return if (r < 0) r + mod else r
}

private class IntBufferList(initialCapacity: Int = 8192) {
    private var arr = IntArray(initialCapacity)
    var size: Int = 0
        private set

    operator fun get(idx: Int): Int = arr[idx]

    fun add(v: Int) {
        if (size == arr.size) arr = arr.copyOf(arr.size * 2)
        arr[size++] = v
    }

    fun clear() {
        size = 0
    }
}

private fun ByteArrayOutputStream.writeLeInt(v: Int) {
    write(v and 0xFF)
    write((v ushr 8) and 0xFF)
    write((v ushr 16) and 0xFF)
    write((v ushr 24) and 0xFF)
}

private fun ByteArrayOutputStream.writeLeShort(v: Int) {
    write(v and 0xFF)
    write((v ushr 8) and 0xFF)
}

private class Radix2Fft(private val n: Int) {
    private val cosTable = DoubleArray(n / 2) { i -> cos(2.0 * PI * i / n) }
    private val sinTable = DoubleArray(n / 2) { i -> sin(2.0 * PI * i / n) }
    private val bitRev = IntArray(n).also { out ->
        val bits = Integer.numberOfTrailingZeros(n)
        for (i in 0 until n) out[i] = i.reverseBits(bits)
    }

    fun transform(real: DoubleArray, imag: DoubleArray) {
        for (i in 0 until n) {
            val j = bitRev[i]
            if (j > i) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val half = len / 2
            val step = n / len
            var i = 0
            while (i < n) {
                var j = 0
                var k = 0
                while (j < half) {
                    val c = cosTable[k]
                    val s = sinTable[k]
                    val i1 = i + j
                    val i2 = i1 + half
                    val r2 = real[i2]
                    val im2 = imag[i2]
                    val tpre = r2 * c + im2 * s
                    val tpim = -r2 * s + im2 * c
                    real[i2] = real[i1] - tpre
                    imag[i2] = imag[i1] - tpim
                    real[i1] += tpre
                    imag[i1] += tpim
                    j++
                    k += step
                }
                i += len
            }
            len = len shl 1
        }
    }

    private fun Int.reverseBits(bits: Int): Int {
        var x = this
        var y = 0
        repeat(bits) {
            y = (y shl 1) or (x and 1)
            x = x ushr 1
        }
        return y
    }
}
