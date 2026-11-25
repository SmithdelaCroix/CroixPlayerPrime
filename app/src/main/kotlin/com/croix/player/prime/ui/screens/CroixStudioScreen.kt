/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.player.prime.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.croix.prime.design.CroixAnimatedBackdrop
import com.croix.prime.design.CroixDesignState
import com.croix.prime.design.CroixGlassCard
import com.croix.prime.design.CroixThemeJson
import com.croix.prime.design.LocalCroixDesign
import com.croix.prime.design.LocalCroixPalette
import com.croix.prime.design.LocalCroixStrings
import com.croix.prime.design.PrimeCardDensity
import com.croix.prime.design.PrimeColorMode
import com.croix.prime.design.PrimeLanguageMode
import com.croix.prime.design.PrimeLyricsAlign
import com.croix.prime.design.PrimeNavBarStyle
import com.croix.prime.design.PrimePlayerStyle
import com.croix.prime.design.PrimePresets
import com.croix.prime.design.PrimeProgressStyle
import com.croix.prime.design.PrimeTransitionStyle
import com.croix.prime.design.PrimeVisualizerStyle
import com.croix.prime.playback.EqPreset
import com.croix.prime.playback.PlaybackUiState
import kotlin.math.roundToInt

private enum class SettingsPage(val title: String) {
    HOME("Settings"),
    STUDIO("Appearance & Studio"),
    AUDIO("Audio, EQ & Speed"),
    GESTURES("Gestures & Hardware"),
    SPONSORBLOCK("SponsorBlock"),
}

@Composable
fun SettingsAndStudioOverlay(
    playback: PlaybackUiState,
    onClose: () -> Unit,
    onUpdateDesign: ((CroixDesignState) -> CroixDesignState) -> Unit,
    onApplyPreset: (CroixDesignState) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetPitch: (Float) -> Unit,
    onSetSkipSilence: (Boolean) -> Unit,
    onSetEqEnabled: (Boolean) -> Unit,
    onApplyEqPreset: (EqPreset) -> Unit,
    onSetEqBand: (Int, Float) -> Unit,
    onSetBassBoost: (Float) -> Unit,
    onSetLoudness: (Int) -> Unit,
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val strings = LocalCroixStrings.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val touchBlocker = remember { MutableInteractionSource() }

    var page by remember { mutableStateOf(SettingsPage.HOME) }
    var studioTab by remember { mutableIntStateOf(0) }
    var showImport by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }
    var importErr by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = page != SettingsPage.HOME) {
        page = SettingsPage.HOME
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = touchBlocker, indication = null) {},
    ) {
        CroixAnimatedBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CroixGlassCard(
                    cornerRadius = 50.dp,
                    onClick = { if (page == SettingsPage.HOME) onClose() else page = SettingsPage.HOME },
                ) {
                    IconButton(
                        onClick = { if (page == SettingsPage.HOME) onClose() else page = SettingsPage.HOME },
                        modifier = Modifier.size(42.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
                Column {
                    val pageTitle = when (page) {
                        SettingsPage.HOME -> strings.settingsTitle
                        SettingsPage.STUDIO -> strings.settingsAppearance
                        SettingsPage.AUDIO -> strings.settingsAudio
                        SettingsPage.GESTURES -> strings.settingsGestures
                        SettingsPage.SPONSORBLOCK -> strings.settingsSponsorBlock
                    }
                    Text(pageTitle, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    if (page == SettingsPage.HOME) {
                        Text(
                            "Croix Player Prime • ARMv8 / ARMv9 • Android 12+",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val enteringSubmenu = targetState != SettingsPage.HOME
                    val dir = if (enteringSubmenu) 1 else -1
                    val dur = (260f / design.transitionSpeed).toInt().coerceIn(90, 500)
                    when (design.transitionStyle) {
                        PrimeTransitionStyle.NONE -> fadeIn(tween(1)).togetherWith(fadeOut(tween(1)))
                        PrimeTransitionStyle.GENTLE_FADE -> fadeIn(tween(dur)).togetherWith(fadeOut(tween(dur / 2)))
                        PrimeTransitionStyle.SNAPPY_ZOOM, PrimeTransitionStyle.SMOOTH_SLIDE ->
                            (fadeIn(tween(dur, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(tween(dur, easing = FastOutSlowInEasing)) { dir * (it / 5) })
                                .togetherWith(
                                    fadeOut(tween((dur * 0.7f).toInt(), easing = FastOutSlowInEasing)) +
                                        slideOutHorizontally(tween((dur * 0.8f).toInt(), easing = FastOutSlowInEasing)) { -dir * (it / 6) },
                                )
                    }
                },
                label = "settings_submenu_transition",
                modifier = Modifier.weight(1f),
            ) { activePage ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (activePage) {
                SettingsPage.HOME -> {
                    item {
                        SettingsMenuRow(
                            icon = Icons.Rounded.Palette,
                            title = strings.settingsAppearance,
                            subtitle = strings.settingsAppearanceSub,
                            accent = palette.primary,
                            onClick = { page = SettingsPage.STUDIO },
                        )
                    }
                    item {
                        SettingsMenuRow(
                            icon = Icons.Rounded.Equalizer,
                            title = strings.settingsAudio,
                            subtitle = "${"%.2f".format(playback.playbackSpeed)}x • Crossfade: ${if (design.crossfadeSeconds == 0) strings.crossfadeOff else "${design.crossfadeSeconds}s"} • 5-Band EQ",
                            accent = palette.secondary,
                            onClick = { page = SettingsPage.AUDIO },
                        )
                    }
                    item {
                        SettingsMenuRow(
                            icon = Icons.Rounded.TouchApp,
                            title = strings.settingsGestures,
                            subtitle = strings.settingsGesturesSub,
                            accent = palette.primary,
                            onClick = { page = SettingsPage.GESTURES },
                        )
                    }
                    item {
                        SettingsMenuRow(
                            icon = Icons.Rounded.Shield,
                            title = strings.settingsSponsorBlock,
                            subtitle = if (design.sponsorBlockEnabled) {
                                if (strings.isSpanish) "Saltando intros y secciones no musicales" else "Auto-skipping non-music & sponsors"
                            } else {
                                if (strings.isSpanish) "Desactivado" else "Disabled"
                            },
                            accent = palette.secondary,
                            onClick = { page = SettingsPage.SPONSORBLOCK },
                        )
                    }
                    item {
                        CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StudioEnumRow(
                                    title = strings.settingsLanguage,
                                    entries = PrimeLanguageMode.entries,
                                    selected = design.languageMode,
                                    labelOf = { if (strings.isSpanish) it.labelEs else it.labelEn },
                                ) { lang ->
                                    onUpdateDesign { it.copy(languageMode = lang) }
                                }
                            }
                        }
                    }
                    item {
                        CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(Icons.Rounded.Info, null, tint = palette.textSecondary)
                                Column {
                                    Text("Croix Player Prime v2.4.0", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("120fps Glass Engine • arm64-v8a • GPL-3.0", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                SettingsPage.STUDIO -> {
                    val tabs = listOf("Glass", "Colors", "Home & Nav", "Player", "Presets")
                    item {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            tabs.forEachIndexed { idx, label ->
                                CroixGlassCard(
                                    cornerRadius = 50.dp,
                                    accentTint = if (studioTab == idx) palette.primary else null,
                                    onClick = { studioTab = idx },
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    )
                                }
                            }
                        }
                    }
                    item {
                        CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                when (studioTab) {
                                    0 -> {
                                        StudioSlider("Glass Translucency", design.glassOpacity, 0.05f..0.75f, "${(design.glassOpacity * 100).roundToInt()}%") { v ->
                                            onUpdateDesign { it.copy(glassOpacity = v) }
                                        }
                                        StudioSlider("Floating Bar Blur", design.glassBlurDp, 0f..34f, "${design.glassBlurDp.roundToInt()} dp") { v ->
                                            onUpdateDesign { it.copy(glassBlurDp = v) }
                                        }
                                        StudioSlider("Border Glow", design.borderGlowAlpha, 0f..0.65f, "${(design.borderGlowAlpha * 100).roundToInt()}%") { v ->
                                            onUpdateDesign { it.copy(borderGlowAlpha = v) }
                                        }
                                        StudioSlider("Border Thickness", design.borderWidthDp, 0.5f..2.5f, "${"%.1f".format(design.borderWidthDp)} dp") { v ->
                                            onUpdateDesign { it.copy(borderWidthDp = v) }
                                        }
                                        StudioSlider("Corner Roundness", design.cornerRadiusDp, 8f..32f, "${design.cornerRadiusDp.roundToInt()} dp") { v ->
                                            onUpdateDesign { it.copy(cornerRadiusDp = v) }
                                        }
                                        StudioSlider("Ambient Glow", design.ambientGlowStrength, 0f..0.9f, "${(design.ambientGlowStrength * 100).roundToInt()}%") { v ->
                                            onUpdateDesign { it.copy(ambientGlowStrength = v) }
                                        }
                                        StudioSwitch("Reactive Animated Backdrop", design.backdropMotionEnabled) { v ->
                                            onUpdateDesign { it.copy(backdropMotionEnabled = v) }
                                        }
                                        if (design.backdropMotionEnabled) {
                                            StudioSlider("Animation Speed", design.backdropMotionSpeed, 0.2f..2.5f, "${"%.1f".format(design.backdropMotionSpeed)}x") { v ->
                                                onUpdateDesign { it.copy(backdropMotionSpeed = v) }
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(strings.transitionsTitle, color = palette.primary, fontWeight = FontWeight.Bold)
                                        StudioEnumRow(
                                            title = strings.transitionStyleLabel,
                                            entries = PrimeTransitionStyle.entries,
                                            selected = design.transitionStyle,
                                            labelOf = { if (strings.isSpanish) it.labelEs else it.labelEn },
                                        ) { ts ->
                                            onUpdateDesign { it.copy(transitionStyle = ts) }
                                        }
                                        if (design.transitionStyle != PrimeTransitionStyle.NONE) {
                                            StudioSlider(strings.transitionSpeedLabel, design.transitionSpeed, 0.5f..2.0f, "${"%.1f".format(design.transitionSpeed)}x") { v ->
                                                onUpdateDesign { it.copy(transitionSpeed = v) }
                                            }
                                        }
                                    }
                                    1 -> {
                                        // Gradient vs Solid Color Toggle!
                                        Text("Color Fill Mode", color = Color.White, fontWeight = FontWeight.Bold)
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                            CroixGlassCard(
                                                modifier = Modifier.weight(1f),
                                                cornerRadius = 14.dp,
                                                accentTint = if (design.useGradientStyle) palette.primary else null,
                                                onClick = { onUpdateDesign { it.copy(useGradientStyle = true) } },
                                            ) {
                                                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                                    Text("Gradient", color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            CroixGlassCard(
                                                modifier = Modifier.weight(1f),
                                                cornerRadius = 14.dp,
                                                accentTint = if (!design.useGradientStyle) palette.primary else null,
                                                onClick = { onUpdateDesign { it.copy(useGradientStyle = false) } },
                                            ) {
                                                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                                    Text("Solid Color", color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        StudioEnumRow("Color Palette", PrimeColorMode.entries, design.colorMode, { it.label }) { m ->
                                            onUpdateDesign { it.copy(colorMode = m) }
                                        }

                                        Text(
                                            if (design.useGradientStyle) "Custom Gradient Preview" else "Custom Solid Color Preview",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (design.useGradientStyle) {
                                                        Brush.horizontalGradient(listOf(design.customPrimaryFromHue, design.customSecondaryFromHue))
                                                    } else {
                                                        Brush.horizontalGradient(listOf(design.customPrimaryFromHue, design.customPrimaryFromHue))
                                                    },
                                                ),
                                        )
                                        StudioSlider("Primary Hue", design.primaryHue, 0f..360f, "${design.primaryHue.roundToInt()}°") { h ->
                                            onUpdateDesign { it.copy(colorMode = PrimeColorMode.CUSTOM_GRADIENT, primaryHue = h) }
                                        }
                                        if (design.useGradientStyle) {
                                            StudioSlider("Secondary Hue", design.secondaryHue, 0f..360f, "${design.secondaryHue.roundToInt()}°") { h ->
                                                onUpdateDesign { it.copy(colorMode = PrimeColorMode.CUSTOM_GRADIENT, secondaryHue = h) }
                                            }
                                        }
                                    }
                                    2 -> {
                                        StudioSwitch("Show Flow Hero Wheel", design.showCroixFlowHero) { v ->
                                            onUpdateDesign { it.copy(showCroixFlowHero = v) }
                                        }
                                        StudioSwitch("Show Moods Row", design.showMoodChips) { v ->
                                            onUpdateDesign { it.copy(showMoodChips = v) }
                                        }
                                        StudioSwitch("Show Recently Played Grid", design.showQuickResumeGrid) { v ->
                                            onUpdateDesign { it.copy(showQuickResumeGrid = v) }
                                        }
                                        StudioEnumRow("Card Density", PrimeCardDensity.entries, design.cardDensity, { it.label }) { d ->
                                            onUpdateDesign { it.copy(cardDensity = d) }
                                        }
                                        StudioEnumRow("Nav Bar Style", PrimeNavBarStyle.entries, design.navBarStyle, { it.label }) { n ->
                                            onUpdateDesign { it.copy(navBarStyle = n) }
                                        }
                                        StudioSwitch("Show Nav Bar Labels", design.showNavLabels) { v ->
                                            onUpdateDesign { it.copy(showNavLabels = v) }
                                        }
                                        StudioSwitch("Floating Glass Mini-Player", design.miniPlayerFloating) { v ->
                                            onUpdateDesign { it.copy(miniPlayerFloating = v) }
                                        }
                                        StudioSlider("Mini-Player Height", design.miniPlayerHeightDp, 56f..84f, "${design.miniPlayerHeightDp.roundToInt()} dp") { v ->
                                            onUpdateDesign { it.copy(miniPlayerHeightDp = v) }
                                        }
                                    }
                                    3 -> {
                                        StudioEnumRow("Player Layout", PrimePlayerStyle.entries, design.playerStyle, { it.label }) { s ->
                                            onUpdateDesign { it.copy(playerStyle = s) }
                                        }
                                        StudioEnumRow("Progress Bar", PrimeProgressStyle.entries, design.progressStyle, { it.label }) { p ->
                                            onUpdateDesign { it.copy(progressStyle = p) }
                                        }
                                        StudioEnumRow("Live Audio Visualizer", PrimeVisualizerStyle.entries, design.visualizerStyle, { it.label }) { v ->
                                            onUpdateDesign { it.copy(visualizerStyle = v) }
                                        }
                                        StudioSlider("Artwork Corner Radius", design.artworkCornerDp, 0f..36f, "${design.artworkCornerDp.roundToInt()} dp") { v ->
                                            onUpdateDesign { it.copy(artworkCornerDp = v) }
                                        }
                                        StudioSwitch("Vinyl Spin Artwork", design.vinylSpinEnabled) { v ->
                                            onUpdateDesign { it.copy(vinylSpinEnabled = v) }
                                        }
                                        StudioSlider("Control Dock Opacity", design.playerControlDockOpacity, 0.05f..0.80f, "${(design.playerControlDockOpacity * 100).roundToInt()}%") { v ->
                                            onUpdateDesign { it.copy(playerControlDockOpacity = v) }
                                        }
                                        StudioEnumRow("Lyrics Alignment", PrimeLyricsAlign.entries, design.lyricsAlign, { it.label }) { a ->
                                            onUpdateDesign { it.copy(lyricsAlign = a) }
                                        }
                                        StudioSlider("Lyrics Font Size", design.lyricsFontSizeSp, 16f..36f, "${design.lyricsFontSizeSp.roundToInt()} sp") { v ->
                                            onUpdateDesign { it.copy(lyricsFontSizeSp = v) }
                                        }
                                    }
                                    4 -> {
                                        Text("Theme Presets", color = palette.primary, fontWeight = FontWeight.Bold)
                                        PrimePresets.All.forEach { preset ->
                                            CroixGlassCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                cornerRadius = 14.dp,
                                                accentTint = if (design.presetName == preset.presetName) palette.primary else null,
                                                onClick = { onApplyPreset(preset) },
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Text(preset.presetName, color = Color.White, fontWeight = FontWeight.SemiBold)
                                                    if (design.presetName == preset.presetName) {
                                                        Text("Active", color = palette.primary, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            Button(
                                                onClick = {
                                                    val exported = CroixThemeJson.export(design)
                                                    clipboard.setText(AnnotatedString(exported))
                                                    val send = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_SUBJECT, "Croix Player Prime Theme")
                                                        putExtra(Intent.EXTRA_TEXT, exported)
                                                    }
                                                    context.startActivity(Intent.createChooser(send, "Share .croixtheme.json"))
                                                },
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Share JSON")
                                            }
                                            OutlinedButton(
                                                onClick = { importJson = ""; importErr = null; showImport = true },
                                                modifier = Modifier.weight(1f),
                                            ) { Text("Import JSON") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SettingsPage.AUDIO -> {
                    item {
                        CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Speed, Pitch & Silence", color = palette.primary, fontWeight = FontWeight.Bold)
                                StudioSlider("Playback Speed", playback.playbackSpeed, 0.5f..2.0f, "${"%.2f".format(playback.playbackSpeed)}x", onSetSpeed)
                                StudioSlider("Playback Pitch", playback.playbackPitch, 0.5f..2.0f, "${"%.2f".format(playback.playbackPitch)}x", onSetPitch)
                                StudioSwitch("Skip Silence", playback.skipSilenceEnabled, onSetSkipSilence)
                                StudioSlider(
                                    label = strings.crossfadeLabel,
                                    value = design.crossfadeSeconds.toFloat(),
                                    range = 0f..10f,
                                    valueLabel = if (design.crossfadeSeconds == 0) strings.crossfadeOff else "${design.crossfadeSeconds}s",
                                ) { v ->
                                    onUpdateDesign { it.copy(crossfadeSeconds = v.roundToInt()) }
                                }

                                Spacer(Modifier.height(4.dp))
                                Text("5-Band Hardware Equalizer", color = palette.primary, fontWeight = FontWeight.Bold)
                                StudioSwitch("Enable Equalizer", playback.eqEnabled, onSetEqEnabled)
                                StudioEnumRow("Preset", EqPreset.entries.filter { it != EqPreset.CUSTOM }, playback.eqPreset, { it.label }, onApplyEqPreset)
                                val bands = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
                                playback.eqBandsDb.forEachIndexed { idx, db ->
                                    StudioSlider(bands.getOrElse(idx) { "Band ${idx + 1}" }, db, -12f..12f, "${db.roundToInt()} dB") { v ->
                                        onSetEqBand(idx, v)
                                    }
                                }
                                StudioSlider("Bass Boost", playback.bassBoostLevel, 0f..1f, "${(playback.bassBoostLevel * 100).roundToInt()}%", onSetBassBoost)
                                StudioSlider("Loudness Boost", playback.loudnessGainMb.toFloat(), 0f..1200f, "+${playback.loudnessGainMb / 100} dB") { v ->
                                    onSetLoudness(v.roundToInt())
                                }
                            }
                        }
                    }
                }

                SettingsPage.GESTURES -> {
                    item {
                        CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                StudioSwitch("Swipe Artwork to Skip Track", design.swipeArtworkToSkip) { v ->
                                    onUpdateDesign { it.copy(swipeArtworkToSkip = v) }
                                }
                                StudioSlider("Double-Tap Seek Step", design.doubleTapSeekSeconds.toFloat(), 5f..30f, "${design.doubleTapSeekSeconds}s") { v ->
                                    onUpdateDesign { it.copy(doubleTapSeekSeconds = v.roundToInt()) }
                                }
                                StudioSwitch("Shake Device to Skip", design.shakeToSkipEnabled) { v ->
                                    onUpdateDesign { it.copy(shakeToSkipEnabled = v) }
                                }
                                if (design.shakeToSkipEnabled) {
                                    StudioSlider("Shake Sensitivity", design.shakeSensitivity, 0.1f..1.0f, "${(design.shakeSensitivity * 100).roundToInt()}%") { v ->
                                        onUpdateDesign { it.copy(shakeSensitivity = v) }
                                    }
                                }
                            }
                        }
                    }
                }

                SettingsPage.SPONSORBLOCK -> {
                    item {
                        CroixGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                StudioSwitch("Enable SponsorBlock Auto-Skip", design.sponsorBlockEnabled) { v ->
                                    onUpdateDesign { it.copy(sponsorBlockEnabled = v) }
                                }
                                StudioSwitch("Skip Non-Music Sections", design.skipMusicOfftopic) { v ->
                                    onUpdateDesign { it.copy(skipMusicOfftopic = v) }
                                }
                                StudioSwitch("Skip Sponsors & Self-Promo", design.skipSponsors) { v ->
                                    onUpdateDesign { it.copy(skipSponsors = v) }
                                }
                                StudioSwitch("Skip Intros & Outros", design.skipIntroOutro) { v ->
                                    onUpdateDesign { it.copy(skipIntroOutro = v) }
                                }
                            }
                        }
                    }
                }
            }
                }
            }
        }
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("Import .croixtheme.json") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = importJson,
                        onValueChange = { importJson = it },
                        label = { Text("Paste Croix Theme JSON") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (importErr != null) Text(importErr!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(onClick = {
                    CroixThemeJson.import(importJson)
                        .onSuccess { onApplyPreset(it); showImport = false }
                        .onFailure { importErr = it.message ?: "Invalid JSON" }
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showImport = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingsMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    val palette = LocalCroixPalette.current
    CroixGlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.24f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = palette.textSecondary)
        }
    }
}

@Composable
private fun StudioSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onChange: (Float) -> Unit,
) {
    val palette = LocalCroixPalette.current
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            Text(valueLabel, color = palette.primary, style = MaterialTheme.typography.labelMedium)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun StudioSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T : Enum<T>> StudioEnumRow(
    title: String,
    entries: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val palette = LocalCroixPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            entries.forEach { item ->
                CroixGlassCard(
                    cornerRadius = 12.dp,
                    accentTint = if (item == selected) palette.primary else null,
                    onClick = { onSelect(item) },
                ) {
                    Text(
                        text = labelOf(item),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
