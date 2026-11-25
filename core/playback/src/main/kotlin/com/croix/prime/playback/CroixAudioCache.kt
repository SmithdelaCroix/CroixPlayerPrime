/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Smart persistent LRU audio stream cache backed by Media3 SimpleCache.
 * Tracks are keyed by their stable videoId so replays start instantaneously with 0 KB data usage.
 */
@OptIn(UnstableApi::class)
object CroixAudioCache {
    @Volatile
    private var cache: SimpleCache? = null

    @Volatile
    private var currentLimitMb: Int = 512

    @Volatile
    var cachingEnabled: Boolean = true

    fun cacheDir(context: Context): File = File(context.applicationContext.cacheDir, "croix_audio_cache")

    fun getOrCreate(context: Context, limitMb: Int = currentLimitMb): SimpleCache {
        val existing = cache
        if (existing != null) return existing
        return synchronized(this) {
            cache ?: run {
                val safeMb = limitMb.coerceIn(64, 4096)
                currentLimitMb = safeMb
                val dir = cacheDir(context).apply { mkdirs() }
                val evictor = LeastRecentlyUsedCacheEvictor(safeMb.toLong() * 1024L * 1024L)
                val dbProvider = StandaloneDatabaseProvider(context.applicationContext)
                SimpleCache(dir, evictor, dbProvider).also { cache = it }
            }
        }
    }

    fun configureLimitMb(context: Context, limitMb: Int) {
        cachingEnabled = limitMb > 0
        if (limitMb <= 0) return
        if (limitMb != currentLimitMb) {
            currentLimitMb = limitMb.coerceIn(64, 4096)
            // Keep existing cache instance alive while service is running; if over new limit, trim oldest entries
            val active = cache ?: return
            val maxBytes = currentLimitMb.toLong() * 1024L * 1024L
            if (active.cacheSpace > maxBytes) {
                runCatching {
                    val keys = active.keys.toList()
                    for (k in keys) {
                        if (active.cacheSpace <= maxBytes) break
                        active.removeResource(k)
                    }
                }
            }
        }
    }

    fun isTrackCached(trackId: String): Boolean {
        if (!cachingEnabled || trackId.isBlank()) return false
        return runCatching {
            val c = cache ?: return false
            c.getCachedSpans(trackId).any { it.isCached && it.length > 32_768L }
        }.getOrDefault(false)
    }

    fun getCacheSizeBytes(context: Context): Long {
        val c = cache
        if (c != null) {
            return runCatching { c.cacheSpace }.getOrDefault(0L)
        }
        val dir = cacheDir(context)
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    suspend fun clearCache(context: Context): Long = withContext(Dispatchers.IO) {
        val c = cache
        if (c != null) {
            runCatching {
                c.keys.toList().forEach { key ->
                    runCatching { c.removeResource(key) }
                }
            }
        } else {
            runCatching { cacheDir(context).deleteRecursively() }
        }
        getCacheSizeBytes(context)
    }
}
