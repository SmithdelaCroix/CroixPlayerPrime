/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.engine

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap

/**
 * Offline multi-script lyric romanizer supporting Korean (Hangul Revised Romanization),
 * Japanese (Hiragana & Katakana with digrams/gemination), and Cyrillic (Russian/Slavic).
 */
object LyricsRomanizer {
    private val HANGUL_INITIALS = arrayOf(
        "g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s",
        "ss", "", "j", "jj", "ch", "k", "t", "p", "h",
    )
    private val HANGUL_MEDIALS = arrayOf(
        "a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa",
        "wae", "oe", "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i",
    )
    private val HANGUL_FINALS = arrayOf(
        "", "k", "k", "k", "n", "n", "n", "t", "l", "l",
        "l", "l", "l", "l", "l", "l", "m", "p", "p", "t",
        "t", "ng", "t", "t", "k", "t", "p", "t",
    )

    private val CYRILLIC = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ё' to "yo",
        'ж' to "zh", 'з' to "z", 'и' to "i", 'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m",
        'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u",
        'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh", 'щ' to "shch",
        'ъ' to "", 'ы' to "y", 'ь' to "", 'э' to "e", 'ю' to "yu", 'я' to "ya",
        'є' to "ye", 'і' to "i", 'ї' to "yi", 'ґ' to "g",
    )

    private val KANA_DIGRAMS = mapOf(
        "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
        "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
        "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
        "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
        "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
        "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
        "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
        "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
        "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
        "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
        "ふぁ" to "fa", "ふぃ" to "fi", "ふぇ" to "fe", "ふぉ" to "fo",
        "てぃ" to "ti", "でぃ" to "di", "ヴぁ" to "va", "ヴぃ" to "vi", "ヴぇ" to "ve", "ヴぉ" to "vo",
    )

    private val HIRAGANA = mapOf(
        'あ' to "a", 'い' to "i", 'う' to "u", 'え' to "e", 'お' to "o",
        'か' to "ka", 'き' to "ki", 'く' to "ku", 'け' to "ke", 'こ' to "ko",
        'さ' to "sa", 'し' to "shi", 'す' to "su", 'せ' to "se", 'そ' to "so",
        'た' to "ta", 'ち' to "chi", 'つ' to "tsu", 'て' to "te", 'と' to "to",
        'な' to "na", 'に' to "ni", 'ぬ' to "nu", 'ね' to "ne", 'の' to "no",
        'は' to "ha", 'ひ' to "hi", 'ふ' to "fu", 'へ' to "he", 'ほ' to "ho",
        'ま' to "ma", 'み' to "mi", 'む' to "mu", 'め' to "me", 'も' to "mo",
        'や' to "ya", 'ゆ' to "yu", 'よ' to "yo",
        'ら' to "ra", 'り' to "ri", 'る' to "ru", 'れ' to "re", 'ろ' to "ro",
        'わ' to "wa", 'を' to "wo", 'ん' to "n",
        'が' to "ga", 'ぎ' to "gi", 'ぐ' to "gu", 'げ' to "ge", 'ご' to "go",
        'ざ' to "za", 'じ' to "ji", 'ず' to "zu", 'ぜ' to "ze", 'ぞ' to "zo",
        'だ' to "da", 'ぢ' to "ji", 'づ' to "zu", 'で' to "de", 'ど' to "do",
        'ば' to "ba", 'び' to "bi", 'ぶ' to "bu", 'べ' to "be", 'ぼ' to "bo",
        'ぱ' to "pa", 'ぴ' to "pi", 'ぷ' to "pu", 'ぺ' to "pe", 'ぽ' to "po",
        'ぁ' to "a", 'ぃ' to "i", 'ぅ' to "u", 'ぇ' to "e", 'ぉ' to "o",
        'ゃ' to "ya", 'ゅ' to "yu", 'ょ' to "yo", 'ゔ' to "vu",
    )

    fun needsRomanization(text: String): Boolean = text.any { ch ->
        val c = ch.code
        (c in 0xAC00..0xD7A3) || // Hangul
            (c in 0x3040..0x30FF) || // Hiragana & Katakana
            (c in 0x0400..0x04FF) // Cyrillic
    }

    fun romanize(text: String): String {
        if (!needsRomanization(text)) return ""
        // Convert Katakana to Hiragana first so a single table handles both cleanly
        val normalized = buildString(text.length) {
            for (ch in text) {
                val c = ch.code
                if (c in 0x30A1..0x30F6) append((c - 0x60).toChar()) else append(ch)
            }
        }
        val out = StringBuilder(normalized.length * 2)
        var i = 0
        var geminateNext = false
        while (i < normalized.length) {
            val ch = normalized[i]
            val code = ch.code
            when {
                // Small tsu (gemination)
                ch == 'っ' -> {
                    geminateNext = true
                    i++
                }
                // Long vowel mark
                ch == 'ー' -> {
                    val last = out.lastOrNull()
                    if (last != null && last in "aeiou") out.append(last)
                    i++
                }
                // Hangul syllable decomposition
                code in 0xAC00..0xD7A3 -> {
                    val s = code - 0xAC00
                    val initial = s / 588
                    val medial = (s % 588) / 28
                    val finalIdx = s % 28
                    val rom = HANGUL_INITIALS[initial] + HANGUL_MEDIALS[medial] + HANGUL_FINALS[finalIdx]
                    out.append(rom)
                    // Insert clean syllable readability separator if next is non-space Hangul? Keep word continuous
                    i++
                }
                // Japanese digram
                i + 1 < normalized.length && KANA_DIGRAMS.containsKey(normalized.substring(i, i + 2)) -> {
                    val rom = KANA_DIGRAMS[normalized.substring(i, i + 2)]!!
                    if (geminateNext && rom.isNotEmpty()) {
                        out.append(rom[0])
                        geminateNext = false
                    }
                    out.append(rom)
                    i += 2
                }
                // Japanese single kana
                HIRAGANA.containsKey(ch) -> {
                    val rom = HIRAGANA[ch]!!
                    if (geminateNext && rom.isNotEmpty()) {
                        out.append(rom[0])
                        geminateNext = false
                    }
                    out.append(rom)
                    i++
                }
                // Cyrillic
                code in 0x0400..0x04FF -> {
                    val lower = ch.lowercaseChar()
                    val rom = CYRILLIC[lower]
                    if (rom != null) {
                        out.append(if (ch.isUpperCase() && rom.isNotEmpty()) rom.replaceFirstChar { it.uppercase() } else rom)
                    } else {
                        out.append(ch)
                    }
                    i++
                }
                else -> {
                    geminateNext = false
                    out.append(ch)
                    i++
                }
            }
        }
        return out.toString().replace(Regex("\\s+"), " ").trim()
    }
}

/**
 * Live Spanish lyric translator (batched + cached per trackId).
 */
class LyricsTranslator {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val http = HttpClient(OkHttp) { expectSuccess = false }
    private val cache = ConcurrentHashMap<String, List<String>>()

    suspend fun translateLinesToSpanish(trackId: String, lines: List<String>): List<String> = withContext(Dispatchers.IO) {
        if (lines.isEmpty()) return@withContext emptyList()
        val key = "$trackId:${lines.size}"
        cache[key]?.let { return@withContext it }

        val translated = MutableList(lines.size) { "" }
        // Translate in batches of 18 lines joined by newline so full tracks resolve in 1-3 requests
        lines.chunked(18).forEachIndexed { batchIdx, chunk ->
            val startIdx = batchIdx * 18
            val joined = chunk.joinToString("\n")
            val resultText = runCatching {
                val resp = http.get("https://translate.googleapis.com/translate_a/single") {
                    parameter("client", "gtx")
                    parameter("sl", "auto")
                    parameter("tl", "es")
                    parameter("dt", "t")
                    parameter("q", joined)
                    header("User-Agent", "Mozilla/5.0 (Android 14; Mobile) AppleWebKit/537.36")
                }
                if (!resp.status.isSuccess()) return@runCatching null
                val outer = json.parseToJsonElement(resp.bodyAsText()).jsonArray
                val segments = outer.firstOrNull()?.jsonArray ?: return@runCatching null
                buildString {
                    segments.forEach { seg ->
                        seg.jsonArray.firstOrNull()?.jsonPrimitive?.contentOrNull?.let(::append)
                    }
                }
            }.getOrNull()

            if (!resultText.isNullOrBlank()) {
                val split = resultText.split("\n")
                chunk.indices.forEach { i ->
                    val orig = chunk[i].trim()
                    val tr = split.getOrNull(i)?.trim().orEmpty()
                    // Skip redundant identical translations when song is already in Spanish
                    translated[startIdx + i] = if (tr.equals(orig, ignoreCase = true)) "" else tr
                }
            }
        }
        cache[key] = translated
        translated
    }
}
