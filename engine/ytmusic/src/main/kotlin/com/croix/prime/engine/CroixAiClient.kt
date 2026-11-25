/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.engine

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

@Serializable
enum class AiProviderMode(val label: String, val defaultModel: String, val defaultBaseUrl: String) {
    GEMINI("Google Gemini", "gemini-2.0-flash", "https://generativelanguage.googleapis.com"),
    OPENAI_COMPAT("OpenAI / OpenRouter / Groq", "gpt-4o-mini", "https://api.openai.com/v1"),
}

@Serializable
data class AiApiConfig(
    val enabled: Boolean = false,
    val provider: AiProviderMode = AiProviderMode.GEMINI,
    val apiKey: String = "",
    val model: String = "gemini-2.0-flash",
    val customBaseUrl: String = "",
) {
    val isReady: Boolean get() = enabled && apiKey.trim().isNotBlank()
    val effectiveModel: String get() = model.trim().ifBlank { provider.defaultModel }
    val effectiveBaseUrl: String get() = customBaseUrl.trim().trimEnd('/').ifBlank { provider.defaultBaseUrl }
}

@Serializable
data class AiTrackRecommendation(
    val title: String,
    val artist: String,
    val reasonEs: String = "",
)

/**
 * Configurable AI integration client (Google Gemini & OpenAI-compatible APIs) for:
 * 1. Natural, poetic 1:1 Spanish lyric translation preserving rhythm and slang.
 * 2. Personalized music curation & recommendations based on listening history + custom mood prompt.
 */
class CroixAiClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val http = HttpClient(OkHttp) { expectSuccess = false }

    suspend fun testConnection(config: AiApiConfig): Result<String> = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("Ingresa tu API Key primero"))
        runCatching {
            val reply = completeText(
                config = config,
                systemPrompt = "Responde únicamente con: OK",
                userPrompt = "Ping",
                temperature = 0.0,
            )
            if (reply.isBlank()) error("Respuesta vacía de la IA")
            "Conectado (${config.provider.label} • ${config.effectiveModel})"
        }
    }

    suspend fun translateLyricsNaturalEs(
        config: AiApiConfig,
        title: String,
        artist: String,
        lines: List<String>,
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        if (!config.isReady || lines.isEmpty()) return@withContext Result.failure(IllegalStateException("IA no configurada"))
        runCatching {
            val out = MutableList(lines.size) { "" }
            // Process in chunks of 35 lines so timing stays fast and 1:1 line indices remain rock-solid
            lines.chunked(35).forEachIndexed { chunkIdx, chunk ->
                val baseIdx = chunkIdx * 35
                val numbered = chunk.mapIndexed { i, text -> "${i + 1}|$text" }.joinToString("\n")
                val raw = completeText(
                    config = config,
                    systemPrompt = """
                        Eres un traductor musical experto al español neutro natural.
                        Traduce cada línea de la canción "$title" de "$artist" conservando la emoción, modismos y sentido poético.
                        Devuelve EXACTAMENTE el mismo número de líneas con el formato: numero|traduccion
                        Si una línea ya está en español, déjala vacía después de "|".
                        NO agregues explicaciones ni markdown.
                    """.trimIndent(),
                    userPrompt = numbered,
                    temperature = 0.25,
                )
                val parsed = parseNumberedTranslations(raw, chunk.size)
                parsed.forEachIndexed { idx, translated ->
                    val orig = chunk[idx].trim()
                    out[baseIdx + idx] = if (translated.equals(orig, ignoreCase = true)) "" else translated
                }
            }
            out
        }
    }

    suspend fun recommendTracksFromHistory(
        config: AiApiConfig,
        topArtists: List<String>,
        topTracks: List<String>,
        recentTracks: List<String>,
        blockedArtists: List<String>,
        customPrompt: String = "",
        count: Int = 10,
    ): Result<List<AiTrackRecommendation>> = withContext(Dispatchers.IO) {
        if (!config.isReady) return@withContext Result.failure(IllegalStateException("Configura tu API Key de IA en Ajustes"))
        runCatching {
            val prompt = buildString {
                appendLine("Recomienda exactamente $count canciones reales y excelentes para este oyente.")
                if (topArtists.isNotEmpty()) appendLine("Artistas favoritos: ${topArtists.take(10).joinToString(", ")}")
                if (topTracks.isNotEmpty()) appendLine("Canciones más escuchadas: ${topTracks.take(10).joinToString(" | ")}")
                if (recentTracks.isNotEmpty()) appendLine("Escuchado recientemente: ${recentTracks.take(10).joinToString(" | ")}")
                if (blockedArtists.isNotEmpty()) appendLine("NUNCA incluyas a estos artistas bloqueados: ${blockedArtists.joinToString(", ")}")
                if (customPrompt.isNotBlank()) appendLine("Petición especial del usuario: $customPrompt")
                appendLine("Incluye una mezcla de joyas ocultas y canciones afines que NO estén repetidas en la lista anterior.")
                appendLine("""Responde SOLO con un JSON Array válido con objetos {"title":"...","artist":"...","reasonEs":"frase corta en español de por qué le gustará"}.""")
            }

            val raw = completeText(
                config = config,
                systemPrompt = "Eres un curador musical profesional en español. Responde exclusivamente JSON válido sin bloques de código ni texto extra.",
                userPrompt = prompt,
                temperature = 0.75,
            )
            val parsed = parseRecommendationsJson(raw)
            if (parsed.isEmpty()) error("La IA no devolvió recomendaciones válidas")
            parsed
        }
    }

    private suspend fun completeText(
        config: AiApiConfig,
        systemPrompt: String,
        userPrompt: String,
        temperature: Double,
    ): String {
        val key = config.apiKey.trim()
        return when (config.provider) {
            AiProviderMode.GEMINI -> {
                val url = "${config.effectiveBaseUrl}/v1beta/models/${config.effectiveModel}:generateContent?key=$key"
                val payload = buildJsonObject {
                    putJsonObject("systemInstruction") {
                        putJsonArray("parts") {
                            add(buildJsonObject { put("text", systemPrompt) })
                        }
                    }
                    putJsonArray("contents") {
                        add(
                            buildJsonObject {
                                put("role", "user")
                                putJsonArray("parts") {
                                    add(buildJsonObject { put("text", userPrompt) })
                                }
                            },
                        )
                    }
                    putJsonObject("generationConfig") {
                        put("temperature", temperature)
                    }
                }
                val resp = http.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(payload.toString())
                }
                val body = resp.bodyAsText()
                if (!resp.status.isSuccess()) {
                    val err = runCatching {
                        json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                    }.getOrNull()
                    error(err ?: "Gemini HTTP ${resp.status.value}")
                }
                val root = json.parseToJsonElement(body).jsonObject
                root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("content")?.jsonObject
                    ?.get("parts")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty().trim()
            }
            AiProviderMode.OPENAI_COMPAT -> {
                val base = config.effectiveBaseUrl
                val endpoint = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
                val payload = buildJsonObject {
                    put("model", config.effectiveModel)
                    put("temperature", temperature)
                    put(
                        "messages",
                        buildJsonArray {
                            add(buildJsonObject { put("role", "system"); put("content", systemPrompt) })
                            add(buildJsonObject { put("role", "user"); put("content", userPrompt) })
                        },
                    )
                }
                val resp = http.post(endpoint) {
                    header("Authorization", "Bearer $key")
                    contentType(ContentType.Application.Json)
                    setBody(payload.toString())
                }
                val body = resp.bodyAsText()
                if (!resp.status.isSuccess()) {
                    val err = runCatching {
                        json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                    }.getOrNull()
                    error(err ?: "AI API HTTP ${resp.status.value}")
                }
                val root = json.parseToJsonElement(body).jsonObject
                root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("message")?.jsonObject
                    ?.get("content")?.jsonPrimitive?.contentOrNull.orEmpty().trim()
            }
        }
    }

    internal fun parseNumberedTranslations(raw: String, expectedSize: Int): List<String> {
        val out = MutableList(expectedSize) { "" }
        val clean = raw.replace("```", "").trim()
        val lines = clean.lines().map { it.trim() }.filter { it.isNotBlank() }
        var fallbackIdx = 0
        for (line in lines) {
            val pipe = line.indexOf('|')
            if (pipe > 0) {
                val num = line.substring(0, pipe).filter { it.isDigit() }.toIntOrNull()
                val text = line.substring(pipe + 1).trim()
                if (num != null && num in 1..expectedSize) {
                    out[num - 1] = text
                    continue
                }
            }
            if (fallbackIdx < expectedSize) {
                out[fallbackIdx++] = line
            }
        }
        return out
    }

    internal fun parseRecommendationsJson(raw: String): List<AiTrackRecommendation> {
        val cleaned = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = cleaned.indexOf('[')
        val end = cleaned.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        val slice = cleaned.substring(start, end + 1)
        return runCatching {
            val arr: JsonArray = json.parseToJsonElement(slice).jsonArray
            arr.mapNotNull { elem ->
                val o: JsonObject = elem as? JsonObject ?: return@mapNotNull null
                val title = o["title"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val artist = o["artist"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val reason = o["reasonEs"]?.jsonPrimitive?.contentOrNull?.trim()
                    ?: o["reason"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                if (title.isBlank() || artist.isBlank()) null
                else AiTrackRecommendation(title = title, artist = artist, reasonEs = reason)
            }
        }.getOrElse { emptyList() }
    }
}
