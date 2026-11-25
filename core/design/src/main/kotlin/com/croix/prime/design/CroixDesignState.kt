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
enum class PrimeColorMode(val label: String) {
    VIOLET_GLASS("Violet Glass"),
    NEON_CYAN("Neon Cyan"),
    ALBUM_MORPH("Album Morph"),
    DYNAMIC_SYSTEM("Material You"),
    AMOLED_BLACK("AMOLED Black"),
    CUSTOM_GRADIENT("Custom Gradient"),
}

@Serializable
enum class PrimePlayerStyle(val label: String) {
    TRANSLUCENT_CARD("Translucent Card"),
    FROSTED_GLASS("Frosted Glass"),
    FULLSCREEN_COVER("Fullscreen Cover"),
    MINIMAL_CLEAN("Minimal Clean"),
}

@Serializable
enum class PrimeProgressStyle(val label: String) {
    WAVEFORM("Waveform"),
    CAPSULE("Capsule"),
    THIN_LINE("Thin Line"),
    SQUIGGLY("Squiggly"),
}

@Serializable
enum class PrimeVisualizerStyle(val label: String) {
    OFF("Off"),
    SPECTRUM_BARS("Spectrum Bars"),
    PULSE_WAVE("Pulse Wave"),
    ORBIT_RINGS("Orbit Rings"),
}

@Serializable
enum class PrimeNavBarStyle(val label: String) {
    FLOATING_GLASS_PILL("Floating Pill"),
    DOCKED_TRANSLUCENT("Docked Glass"),
    COMPACT_ICONS("Compact Icons"),
}

@Serializable
enum class PrimeCardDensity(val label: String) {
    COMPACT("Compact"),
    EDITORIAL("Editorial"),
    SPACIOUS("Spacious"),
}

@Serializable
enum class PrimeLyricsAlign(val label: String) {
    START("Left"),
    CENTER("Center"),
    END("Right"),
}

@Immutable
@Serializable
data class CroixDesignState(
    val presetName: String = "Prime Violet Glass",
    // Glassmorphism translucency controls (applied across the ENTIRE app)
    val glassOpacity: Float = 0.16f,
    val glassBlurDp: Float = 22f,
    val borderGlowAlpha: Float = 0.24f,
    val borderWidthDp: Float = 1.0f,
    val cornerRadiusDp: Float = 22f,
    val ambientGlowStrength: Float = 0.48f,
    val backdropMotionEnabled: Boolean = true,
    val backdropMotionSpeed: Float = 1.0f,
    // Free Gradient & Color Engine
    val colorMode: PrimeColorMode = PrimeColorMode.VIOLET_GLASS,
    val useGradientStyle: Boolean = true,
    val customAccentArgb: Long = 0xFFA238FF,
    val secondaryAccentArgb: Long = 0xFFFF2E93,
    val primaryHue: Float = 272f,
    val secondaryHue: Float = 330f,
    // Home Screen customization
    val showCroixFlowHero: Boolean = true,
    val showMoodChips: Boolean = true,
    val showQuickResumeGrid: Boolean = true,
    val cardDensity: PrimeCardDensity = PrimeCardDensity.EDITORIAL,
    // Now Playing & Visualizer
    val playerStyle: PrimePlayerStyle = PrimePlayerStyle.TRANSLUCENT_CARD,
    val progressStyle: PrimeProgressStyle = PrimeProgressStyle.WAVEFORM,
    val visualizerStyle: PrimeVisualizerStyle = PrimeVisualizerStyle.SPECTRUM_BARS,
    val artworkCornerDp: Float = 24f,
    val vinylSpinEnabled: Boolean = false,
    val playerControlDockOpacity: Float = 0.22f,
    // Gestures & Hardware
    val swipeArtworkToSkip: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    val shakeToSkipEnabled: Boolean = false,
    val shakeSensitivity: Float = 0.55f,
    // Navigation & Mini-Player
    val navBarStyle: PrimeNavBarStyle = PrimeNavBarStyle.FLOATING_GLASS_PILL,
    val showNavLabels: Boolean = true,
    val miniPlayerFloating: Boolean = true,
    val miniPlayerHeightDp: Float = 66f,
    // Lyrics
    val lyricsFontSizeSp: Float = 24f,
    val lyricsAlign: PrimeLyricsAlign = PrimeLyricsAlign.START,
    val lyricsBackdropBlurDp: Float = 26f,
    // SponsorBlock
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
        backdropMotionSpeed = backdropMotionSpeed.coerceIn(0.2f, 2.5f),
        primaryHue = primaryHue.coerceIn(0f, 360f),
        secondaryHue = secondaryHue.coerceIn(0f, 360f),
        artworkCornerDp = artworkCornerDp.coerceIn(0f, 36f),
        playerControlDockOpacity = playerControlDockOpacity.coerceIn(0.05f, 0.80f),
        doubleTapSeekSeconds = doubleTapSeekSeconds.coerceIn(5, 30),
        shakeSensitivity = shakeSensitivity.coerceIn(0.1f, 1.0f),
        miniPlayerHeightDp = miniPlayerHeightDp.coerceIn(56f, 84f),
        lyricsFontSizeSp = lyricsFontSizeSp.coerceIn(16f, 36f),
        lyricsBackdropBlurDp = lyricsBackdropBlurDp.coerceIn(0f, 34f),
    )

    val customPrimaryFromHue: Color get() = Color.hsv(primaryHue.coerceIn(0f, 360f), 0.78f, 1.0f)
    val customSecondaryFromHue: Color get() = Color.hsv(secondaryHue.coerceIn(0f, 360f), 0.82f, 1.0f)

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
    val PrimeVioletGlass = CroixDesignState(
        presetName = "Prime Violet Glass",
        glassOpacity = 0.16f,
        glassBlurDp = 22f,
        borderGlowAlpha = 0.26f,
        cornerRadiusDp = 22f,
        colorMode = PrimeColorMode.VIOLET_GLASS,
        primaryHue = 272f,
        secondaryHue = 330f,
        playerStyle = PrimePlayerStyle.TRANSLUCENT_CARD,
        progressStyle = PrimeProgressStyle.WAVEFORM,
        visualizerStyle = PrimeVisualizerStyle.SPECTRUM_BARS,
    )

    val FrostedNeonCyan = CroixDesignState(
        presetName = "Frosted Neon Cyan",
        glassOpacity = 0.22f,
        glassBlurDp = 28f,
        borderGlowAlpha = 0.34f,
        borderWidthDp = 1.2f,
        cornerRadiusDp = 26f,
        colorMode = PrimeColorMode.NEON_CYAN,
        primaryHue = 186f,
        secondaryHue = 262f,
        playerStyle = PrimePlayerStyle.FROSTED_GLASS,
        progressStyle = PrimeProgressStyle.SQUIGGLY,
        visualizerStyle = PrimeVisualizerStyle.PULSE_WAVE,
    )

    val AlbumMorphLive = CroixDesignState(
        presetName = "Album Morph Live",
        glassOpacity = 0.18f,
        glassBlurDp = 26f,
        borderGlowAlpha = 0.28f,
        ambientGlowStrength = 0.65f,
        colorMode = PrimeColorMode.ALBUM_MORPH,
        playerStyle = PrimePlayerStyle.FULLSCREEN_COVER,
        progressStyle = PrimeProgressStyle.CAPSULE,
        visualizerStyle = PrimeVisualizerStyle.ORBIT_RINGS,
        lyricsAlign = PrimeLyricsAlign.CENTER,
    )

    val AmoledObsidian = CroixDesignState(
        presetName = "AMOLED Obsidian",
        glassOpacity = 0.08f,
        glassBlurDp = 10f,
        borderGlowAlpha = 0.14f,
        ambientGlowStrength = 0.12f,
        backdropMotionEnabled = false,
        cornerRadiusDp = 16f,
        colorMode = PrimeColorMode.AMOLED_BLACK,
        playerStyle = PrimePlayerStyle.MINIMAL_CLEAN,
        progressStyle = PrimeProgressStyle.THIN_LINE,
        visualizerStyle = PrimeVisualizerStyle.OFF,
        miniPlayerFloating = false,
    )

    val DynamicGlass = CroixDesignState(
        presetName = "Dynamic Glass",
        glassOpacity = 0.20f,
        glassBlurDp = 20f,
        borderGlowAlpha = 0.22f,
        colorMode = PrimeColorMode.DYNAMIC_SYSTEM,
        playerStyle = PrimePlayerStyle.TRANSLUCENT_CARD,
        progressStyle = PrimeProgressStyle.CAPSULE,
    )

    val All: List<CroixDesignState> = listOf(
        PrimeVioletGlass,
        FrostedNeonCyan,
        AlbumMorphLive,
        AmoledObsidian,
        DynamicGlass,
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
        if (raw.isNullOrBlank()) PrimePresets.PrimeVioletGlass
        else CroixThemeJson.import(raw).getOrDefault(PrimePresets.PrimeVioletGlass)
    }

    suspend fun update(transform: (CroixDesignState) -> CroixDesignState) {
        context.croixDesignStore.edit { prefs ->
            val current = prefs[key]?.let { CroixThemeJson.import(it).getOrNull() }
                ?: PrimePresets.PrimeVioletGlass
            prefs[key] = CroixThemeJson.export(transform(current).clamped())
        }
    }

    suspend fun applyPreset(preset: CroixDesignState) {
        context.croixDesignStore.edit { prefs ->
            prefs[key] = CroixThemeJson.export(preset.clamped())
        }
    }
}
