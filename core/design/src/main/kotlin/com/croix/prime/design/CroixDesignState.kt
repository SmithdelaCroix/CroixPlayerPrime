/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.design

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class PrimeColorMode {
    DEEZER_VIOLET_GLASS,
    YUMA_NEON_CYAN,
    ALBUM_ART_MORPH,
    MATERIAL_YOU,
    AMOLED_GLASS,
    CUSTOM_ACCENT,
}

@Serializable
enum class PrimePlayerStyle {
    DEEZER_TRANSLUCENT,
    YUMA_GLASS_HYBRID,
    FULLSCREEN_COVER,
    MINIMAL_CLEAN,
}

@Serializable
enum class PrimeProgressStyle {
    WAVEFORM,
    CAPSULE,
    THIN_LINE,
    SQUIGGLY,
}

@Serializable
enum class PrimeNavBarStyle {
    FLOATING_GLASS_PILL,
    DOCKED_TRANSLUCENT,
    COMPACT_ICONS,
}

@Serializable
enum class PrimeCardDensity {
    COMPACT,
    DEEZER_EDITORIAL,
    SPACIOUS,
}

@Serializable
enum class PrimeLyricsAlign {
    START,
    CENTER,
    END,
}

@Immutable
@Serializable
data class CroixDesignState(
    val presetName: String = "Deezer Violet Glass",
    // Glassmorphism translucency controls (applied across the ENTIRE app)
    val glassOpacity: Float = 0.16f,
    val glassBlurDp: Float = 22f,
    val borderGlowAlpha: Float = 0.24f,
    val borderWidthDp: Float = 1.0f,
    val cornerRadiusDp: Float = 22f,
    val ambientGlowStrength: Float = 0.42f,
    // Color Engine
    val colorMode: PrimeColorMode = PrimeColorMode.DEEZER_VIOLET_GLASS,
    val customAccentArgb: Long = 0xFFA238FF,
    val secondaryAccentArgb: Long = 0xFF00E5FF,
    // Home Screen (Deezer x Yuma customization)
    val showCroixFlowHero: Boolean = true,
    val showMoodChips: Boolean = true,
    val showRecentSection: Boolean = true,
    val cardDensity: PrimeCardDensity = PrimeCardDensity.DEEZER_EDITORIAL,
    // Now Playing Screen
    val playerStyle: PrimePlayerStyle = PrimePlayerStyle.DEEZER_TRANSLUCENT,
    val progressStyle: PrimeProgressStyle = PrimeProgressStyle.WAVEFORM,
    val artworkCornerDp: Float = 24f,
    val vinylSpinEnabled: Boolean = false,
    val playerControlDockOpacity: Float = 0.20f,
    // Navigation & Mini-Player
    val navBarStyle: PrimeNavBarStyle = PrimeNavBarStyle.FLOATING_GLASS_PILL,
    val showNavLabels: Boolean = true,
    val miniPlayerFloating: Boolean = true,
    val miniPlayerHeightDp: Float = 66f,
    // Lyrics
    val lyricsFontSizeSp: Float = 24f,
    val lyricsAlign: PrimeLyricsAlign = PrimeLyricsAlign.START,
    val lyricsBackdropBlurDp: Float = 26f,
    // SponsorBlock integrated settings
    val sponsorBlockEnabled: Boolean = true,
    val skipMusicOfftopic: Boolean = true,
    val skipSponsors: Boolean = true,
    val skipIntroOutro: Boolean = true,
) {
    fun clamped(): CroixDesignState = copy(
        glassOpacity = glassOpacity.coerceIn(0.05f, 0.75f),
        glassBlurDp = glassBlurDp.coerceIn(0f, 34f),
        borderGlowAlpha = borderGlowAlpha.coerceIn(0.0f, 0.65f),
        borderWidthDp = borderWidthDp.coerceIn(0.5f, 2.5f),
        cornerRadiusDp = cornerRadiusDp.coerceIn(8f, 32f),
        ambientGlowStrength = ambientGlowStrength.coerceIn(0f, 0.9f),
        artworkCornerDp = artworkCornerDp.coerceIn(0f, 36f),
        playerControlDockOpacity = playerControlDockOpacity.coerceIn(0.05f, 0.80f),
        miniPlayerHeightDp = miniPlayerHeightDp.coerceIn(56f, 84f),
        lyricsFontSizeSp = lyricsFontSizeSp.coerceIn(16f, 36f),
        lyricsBackdropBlurDp = lyricsBackdropBlurDp.coerceIn(0f, 34f),
    )

    val customAccentColor: Color get() = Color(customAccentArgb)
    val secondaryAccentColor: Color get() = Color(secondaryAccentArgb)

    fun enabledSponsorCategories(): Set<String> = buildSet {
        if (!sponsorBlockEnabled) return@buildSet
        if (skipMusicOfftopic) add("music_offtopic")
        if (skipSponsors) {
            add("sponsor")
            add("selfpromo")
            add("interaction")
        }
        if (skipIntroOutro) {
            add("intro")
            add("outro")
        }
    }
}

object PrimePresets {
    val DeezerVioletGlass = CroixDesignState(
        presetName = "Deezer Violet Glass",
        glassOpacity = 0.16f,
        glassBlurDp = 22f,
        borderGlowAlpha = 0.26f,
        cornerRadiusDp = 22f,
        colorMode = PrimeColorMode.DEEZER_VIOLET_GLASS,
        customAccentArgb = 0xFFA238FF,
        secondaryAccentArgb = 0xFFFF2E93,
        playerStyle = PrimePlayerStyle.DEEZER_TRANSLUCENT,
        progressStyle = PrimeProgressStyle.WAVEFORM,
    )

    val YumaFrostedNeon = CroixDesignState(
        presetName = "Yuma Frosted Neon",
        glassOpacity = 0.22f,
        glassBlurDp = 28f,
        borderGlowAlpha = 0.34f,
        borderWidthDp = 1.2f,
        cornerRadiusDp = 26f,
        colorMode = PrimeColorMode.YUMA_NEON_CYAN,
        customAccentArgb = 0xFF00E5FF,
        secondaryAccentArgb = 0xFF7C4DFF,
        playerStyle = PrimePlayerStyle.YUMA_GLASS_HYBRID,
        progressStyle = PrimeProgressStyle.SQUIGGLY,
    )

    val AlbumMorphLive = CroixDesignState(
        presetName = "Album Morph Live",
        glassOpacity = 0.18f,
        glassBlurDp = 26f,
        borderGlowAlpha = 0.28f,
        ambientGlowStrength = 0.65f,
        colorMode = PrimeColorMode.ALBUM_ART_MORPH,
        playerStyle = PrimePlayerStyle.FULLSCREEN_COVER,
        progressStyle = PrimeProgressStyle.CAPSULE,
        lyricsAlign = PrimeLyricsAlign.CENTER,
    )

    val AmoledObsidian = CroixDesignState(
        presetName = "AMOLED Obsidian",
        glassOpacity = 0.08f,
        glassBlurDp = 10f,
        borderGlowAlpha = 0.14f,
        ambientGlowStrength = 0.12f,
        cornerRadiusDp = 16f,
        colorMode = PrimeColorMode.AMOLED_GLASS,
        playerStyle = PrimePlayerStyle.MINIMAL_CLEAN,
        progressStyle = PrimeProgressStyle.THIN_LINE,
        miniPlayerFloating = false,
    )

    val MaterialYouGlass = CroixDesignState(
        presetName = "Material You Glass",
        glassOpacity = 0.20f,
        glassBlurDp = 20f,
        borderGlowAlpha = 0.22f,
        colorMode = PrimeColorMode.MATERIAL_YOU,
        playerStyle = PrimePlayerStyle.DEEZER_TRANSLUCENT,
        progressStyle = PrimeProgressStyle.CAPSULE,
    )

    val All: List<CroixDesignState> = listOf(
        DeezerVioletGlass,
        YumaFrostedNeon,
        AlbumMorphLive,
        AmoledObsidian,
        MaterialYouGlass,
    )
}

object CroixThemeJson {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun export(state: CroixDesignState): String =
        json.encodeToString(CroixDesignState.serializer(), state.clamped())

    fun import(raw: String): Result<CroixDesignState> = runCatching {
        json.decodeFromString(CroixDesignState.serializer(), raw).clamped()
    }
}

private val Context.croixDesignStore: DataStore<Preferences> by preferencesDataStore(
    name = "croix_prime_design_store",
)

class CroixDesignRepository(private val context: Context) {
    private val key = stringPreferencesKey("croix_prime_design_json")

    val designFlow: Flow<CroixDesignState> = context.croixDesignStore.data.map { prefs ->
        val raw = prefs[key]
        if (raw.isNullOrBlank()) PrimePresets.DeezerVioletGlass
        else CroixThemeJson.import(raw).getOrDefault(PrimePresets.DeezerVioletGlass)
    }

    suspend fun update(transform: (CroixDesignState) -> CroixDesignState) {
        context.croixDesignStore.edit { prefs ->
            val current = prefs[key]?.let { CroixThemeJson.import(it).getOrNull() }
                ?: PrimePresets.DeezerVioletGlass
            prefs[key] = CroixThemeJson.export(transform(current).clamped())
        }
    }

    suspend fun applyPreset(preset: CroixDesignState) {
        context.croixDesignStore.edit { prefs ->
            prefs[key] = CroixThemeJson.export(preset.clamped())
        }
    }
}
