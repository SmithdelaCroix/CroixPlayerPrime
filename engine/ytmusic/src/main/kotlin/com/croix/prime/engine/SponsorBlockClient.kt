/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.engine

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap

class SponsorBlockClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient(OkHttp)
    private val cache = ConcurrentHashMap<String, List<SponsorSegment>>()

    suspend fun fetchSegments(
        videoId: String,
        categories: Set<SponsorCategory>,
    ): List<SponsorSegment> = withContext(Dispatchers.IO) {
        if (videoId.isBlank() || categories.isEmpty()) return@withContext emptyList()
        val cacheKey = "$videoId:${categories.map { it.apiKey }.sorted().joinToString(",")}"
        cache[cacheKey]?.let { return@withContext it }

        runCatching {
            val response = http.get("https://sponsor.ajay.app/api/skipSegments") {
                parameter("videoID", videoId)
                categories.forEach { cat -> parameter("category", cat.apiKey) }
            }
            if (!response.status.isSuccess()) return@withContext emptyList()
            val arr = json.parseToJsonElement(response.bodyAsText()).jsonArray
            arr.mapNotNull { item ->
                val obj = item.jsonObject
                val seg = obj["segment"]?.jsonArray ?: return@mapNotNull null
                val startSec = seg.getOrNull(0)?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                val endSec = seg.getOrNull(1)?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                if (endSec <= startSec) return@mapNotNull null
                SponsorSegment(
                    category = obj["category"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    startMs = (startSec * 1000.0).toLong(),
                    endMs = (endSec * 1000.0).toLong(),
                    uuid = obj["UUID"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                )
            }.sortedBy { it.startMs }.also { cache[cacheKey] = it }
        }.getOrElse { emptyList() }
    }
}

/**
 * Pure, loop-safe SponsorBlock segment skipper.
 */
object SponsorBlockSkipper {
    fun findTargetSkip(
        positionMs: Long,
        segments: List<SponsorSegment>,
        alreadySkippedUuids: Set<String>,
        enabledCategories: Set<String>,
    ): SponsorSegment? {
        return segments.firstOrNull { seg ->
            seg.category in enabledCategories &&
                seg.uuid !in alreadySkippedUuids &&
                positionMs >= seg.startMs &&
                positionMs < seg.endMs - 250L
        }
    }
}
