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
    VIOLET_GLASS("Cristal Violeta"),
    NEON_CYAN("Neón Cian"),
    ALBUM_MORPH("Carátula Viva"),
    DYNAMIC_SYSTEM("Material You"),
    AMOLED_BLACK("AMOLED Negro"),
    CUSTOM_GRADIENT("Color Libre"),
}

@Serializable
enum class PrimePlayerStyle(val label: String) {
    TRANSLUCENT_CARD("Tarjeta Cristal"),
    FROSTED_GLASS("Cristal Escarchado"),
    FULLSCREEN_COVER("Carátula Grande"),
    MINIMAL_CLEAN("Minimalista"),
}

@Serializable
enum class PrimeProgressStyle(val label: String) {
    WAVEFORM("Onda"),
    CAPSULE("Cápsula"),
    THIN_LINE("Línea Fina"),
    SQUIGGLY("Ondulada"),
}

@Serializable
enum class PrimeVisualizerStyle(val label: String) {
    OFF("Apagado"),
    SPECTRUM_BARS("Barras Espectro"),
    PULSE_WAVE("Onda Pulso"),
    ORBIT_RINGS("Anillos Órbita"),
}

@Serializable
enum class PrimeNavBarStyle(val label: String) {
    FLOATING_GLASS_PILL("Píldora Flotante"),
    DOCKED_TRANSLUCENT("Barra Acoplada"),
    COMPACT_ICONS("Iconos Compactos"),
}

@Serializable
enum class PrimeCardDensity(val label: String) {
    COMPACT("Compacto"),
    EDITORIAL("Editorial"),
    SPACIOUS("Amplio"),
}

@Serializable
enum class PrimeLyricsAlign(val label: String) {
    START("Izquierda"),
    CENTER("Centro"),
    END("Derecha"),
}

@Serializable
enum class PrimeTransitionStyle(val labelEn: String, val labelEs: String) {
    SMOOTH_SLIDE("Smooth Slide", "Deslizamiento Suave"),
    GENTLE_FADE("Gentle Fade", "Fundido Suave"),
    SNAPPY_ZOOM("Snappy Zoom", "Zoom Fluido"),
    NONE("Instant", "Sin Animación"),
}

@Serializable
enum class PrimeLanguageMode(val labelEn: String, val labelEs: String) {
    SPANISH("Español", "Español"),
    SYSTEM("System Auto", "Automático del Sistema"),
    ENGLISH("English", "Inglés"),
}

@Serializable
enum class PrimeAudioQuality(val labelEs: String, val labelEn: String) {
    MAX_QUALITY("Máxima (Opus/AAC)", "Maximum (Opus/AAC)"),
    BALANCED("Balanceada (128 kbps)", "Balanced (128 kbps)"),
    DATA_SAVER("Ahorro de Datos", "Data Saver"),
}

@Immutable
@Serializable
data class CroixDesignState(
    val presetName: String = "Cristal Violeta",
    // Glassmorphism translucency controls (applied across the ENTIRE app)
    val glassOpacity: Float = 0.16f,
    val glassBlurDp: Float = 22f,
    val borderGlowAlpha: Float = 0.24f,
    val borderWidthDp: Float = 1.0f,
    val cornerRadiusDp: Float = 22f,
    val ambientGlowStrength: Float = 0.50f,
    val backdropMotionEnabled: Boolean = true,
    val backdropMotionSpeed: Float = 1.0f,
    val fontScale: Float = 1.0f,
    // Deep Dynamic Color & Gradient Engine
    val colorMode: PrimeColorMode = PrimeColorMode.VIOLET_GLASS,
    val useGradientStyle: Boolean = true,
    val customAccentArgb: Long = 0xFFA238FF,
    val secondaryAccentArgb: Long = 0xFFFF2E93,
    val primaryHue: Float = 272f,
    val secondaryHue: Float = 330f,
    val primarySaturation: Float = 0.80f,
    val secondarySaturation: Float = 0.84f,
    val pureBlackBackground: Boolean = false,
    // Home Screen Editorial Customization
    val showCroixFlowHero: Boolean = true,
    val showMoodChips: Boolean = true,
    val showQuickResumeGrid: Boolean = true,
    val showDailyMixes: Boolean = true,
    val showQuickPicks: Boolean = true,
    val showFavoriteArtistsRow: Boolean = true,
    val cardDensity: PrimeCardDensity = PrimeCardDensity.EDITORIAL,
    // Now Playing & Visualizer
    val playerStyle: PrimePlayerStyle = PrimePlayerStyle.TRANSLUCENT_CARD,
    val progressStyle: PrimeProgressStyle = PrimeProgressStyle.WAVEFORM,
    val visualizerStyle: PrimeVisualizerStyle = PrimeVisualizerStyle.SPECTRUM_BARS,
    val artworkCornerDp: Float = 24f,
    val vinylSpinEnabled: Boolean = false,
    val playerControlDockOpacity: Float = 0.22f,
    // Playback & Audio Quality
    val audioQuality: PrimeAudioQuality = PrimeAudioQuality.MAX_QUALITY,
    val normalizeAudio: Boolean = true,
    val autoPlayRelated: Boolean = true,
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
    // Transitions, Crossfade & Language (Default 100% Spanish!)
    val transitionStyle: PrimeTransitionStyle = PrimeTransitionStyle.SMOOTH_SLIDE,
    val transitionSpeed: Float = 1.0f,
    val crossfadeSeconds: Int = 0,
    val languageMode: PrimeLanguageMode = PrimeLanguageMode.SPANISH,
) {
    fun clamped(): CroixDesignState = copy(
        glassOpacity = glassOpacity.coerceIn(0.05f, 0.75f),
        glassBlurDp = glassBlurDp.coerceIn(0f, 34f),
        borderGlowAlpha = borderGlowAlpha.coerceIn(0.0f, 0.65f),
        borderWidthDp = borderWidthDp.coerceIn(0.5f, 2.5f),
        cornerRadiusDp = cornerRadiusDp.coerceIn(8f, 32f),
        ambientGlowStrength = ambientGlowStrength.coerceIn(0f, 0.9f),
        backdropMotionSpeed = backdropMotionSpeed.coerceIn(0.2f, 2.5f),
        fontScale = fontScale.coerceIn(0.85f, 1.20f),
        primaryHue = primaryHue.coerceIn(0f, 360f),
        secondaryHue = secondaryHue.coerceIn(0f, 360f),
        primarySaturation = primarySaturation.coerceIn(0.25f, 1.0f),
        secondarySaturation = secondarySaturation.coerceIn(0.25f, 1.0f),
        artworkCornerDp = artworkCornerDp.coerceIn(0f, 36f),
        playerControlDockOpacity = playerControlDockOpacity.coerceIn(0.05f, 0.80f),
        doubleTapSeekSeconds = doubleTapSeekSeconds.coerceIn(5, 30),
        shakeSensitivity = shakeSensitivity.coerceIn(0.1f, 1.0f),
        miniPlayerHeightDp = miniPlayerHeightDp.coerceIn(56f, 84f),
        lyricsFontSizeSp = lyricsFontSizeSp.coerceIn(16f, 36f),
        lyricsBackdropBlurDp = lyricsBackdropBlurDp.coerceIn(0f, 34f),
        transitionSpeed = transitionSpeed.coerceIn(0.5f, 2.0f),
        crossfadeSeconds = crossfadeSeconds.coerceIn(0, 10),
    )

    val customPrimaryFromHue: Color
        get() = Color.hsv(primaryHue.coerceIn(0f, 360f), primarySaturation.coerceIn(0.25f, 1f), 1.0f)
    val customSecondaryFromHue: Color
        get() = Color.hsv(secondaryHue.coerceIn(0f, 360f), secondarySaturation.coerceIn(0.25f, 1f), 1.0f)

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
        presetName = "Cristal Violeta",
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
        presetName = "Neón Cian",
        glassOpacity = 0.20f,
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
        presetName = "Carátula Viva",
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

    val SunsetCoral = CroixDesignState(
        presetName = "Atardecer Coral",
        glassOpacity = 0.18f,
        glassBlurDp = 24f,
        borderGlowAlpha = 0.30f,
        ambientGlowStrength = 0.58f,
        colorMode = PrimeColorMode.CUSTOM_GRADIENT,
        primaryHue = 16f,
        secondaryHue = 338f,
        primarySaturation = 0.88f,
        secondarySaturation = 0.85f,
        playerStyle = PrimePlayerStyle.TRANSLUCENT_CARD,
        progressStyle = PrimeProgressStyle.WAVEFORM,
    )

    val DeepEmerald = CroixDesignState(
        presetName = "Esmeralda Profunda",
        glassOpacity = 0.17f,
        glassBlurDp = 22f,
        borderGlowAlpha = 0.28f,
        ambientGlowStrength = 0.54f,
        colorMode = PrimeColorMode.CUSTOM_GRADIENT,
        primaryHue = 148f,
        secondaryHue = 192f,
        primarySaturation = 0.85f,
        secondarySaturation = 0.82f,
        playerStyle = PrimePlayerStyle.FROSTED_GLASS,
        progressStyle = PrimeProgressStyle.CAPSULE,
    )

    val MidnightGold = CroixDesignState(
        presetName = "Oro Nocturno",
        glassOpacity = 0.14f,
        glassBlurDp = 18f,
        borderGlowAlpha = 0.32f,
        ambientGlowStrength = 0.44f,
        pureBlackBackground = true,
        colorMode = PrimeColorMode.CUSTOM_GRADIENT,
        primaryHue = 42f,
        secondaryHue = 22f,
        primarySaturation = 0.90f,
        secondarySaturation = 0.82f,
        playerStyle = PrimePlayerStyle.MINIMAL_CLEAN,
        progressStyle = PrimeProgressStyle.THIN_LINE,
    )

    val AmoledObsidian = CroixDesignState(
        presetName = "Obsidiana AMOLED",
        glassOpacity = 0.08f,
        glassBlurDp = 10f,
        borderGlowAlpha = 0.16f,
        ambientGlowStrength = 0.12f,
        backdropMotionEnabled = false,
        pureBlackBackground = true,
        cornerRadiusDp = 16f,
        colorMode = PrimeColorMode.AMOLED_BLACK,
        playerStyle = PrimePlayerStyle.MINIMAL_CLEAN,
        progressStyle = PrimeProgressStyle.THIN_LINE,
        visualizerStyle = PrimeVisualizerStyle.OFF,
        miniPlayerFloating = false,
    )

    val DynamicGlass = CroixDesignState(
        presetName = "Cristal Dinámico",
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
        SunsetCoral,
        DeepEmerald,
        MidnightGold,
        AmoledObsidian,
        DynamicGlass,
    )
}

object CroixThemeJson {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun export(state: CroixDesignState): String = json.encodeToString(CroixDesignState.serializer(), state.clamped())

    fun import(raw: String): Result<CroixDesignState> = runCatching {
        json.decodeFromString(CroixDesignState.serializer(), raw.trim()).clamped()
    }
}

private val Context.croixDesignDataStore: DataStore<Preferences> by preferencesDataStore(name = "croix_prime_design")

class CroixDesignRepository(private val context: Context) {
    private val key = stringPreferencesKey("design_state_v1")

    val designFlow: Flow<CroixDesignState> = context.croixDesignDataStore.data.map { prefs ->
        val raw = prefs[key] ?: return@map PrimePresets.PrimeVioletGlass
        CroixThemeJson.import(raw).getOrElse { PrimePresets.PrimeVioletGlass }
    }

    suspend fun update(transform: (CroixDesignState) -> CroixDesignState) {
        context.croixDesignDataStore.edit { prefs ->
            val current = prefs[key]?.let { CroixThemeJson.import(it).getOrNull() } ?: PrimePresets.PrimeVioletGlass
            val updated = transform(current).clamped()
            prefs[key] = CroixThemeJson.export(updated)
        }
    }

    suspend fun applyPreset(preset: CroixDesignState) {
        context.croixDesignDataStore.edit { prefs ->
            prefs[key] = CroixThemeJson.export(preset)
        }
    }
}
