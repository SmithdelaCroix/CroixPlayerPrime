/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Immutable
data class CroixResolvedPalette(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val glassSurface: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val textPrimary: Color,
    val textSecondary: Color,
)

val LocalCroixDesign = staticCompositionLocalOf { PrimePresets.PrimeVioletGlass }
val LocalCroixPalette = staticCompositionLocalOf {
    CroixResolvedPalette(
        primary = Color(0xFFA238FF),
        secondary = Color(0xFFFF2E93),
        background = Color(0xFF09080F),
        glassSurface = Color.White.copy(alpha = 0.14f),
        glassBorderTop = Color.White.copy(alpha = 0.26f),
        glassBorderBottom = Color.White.copy(alpha = 0.06f),
        textPrimary = Color.White,
        textSecondary = Color.White.copy(alpha = 0.68f),
    )
}

private val PrimeTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun CroixPrimeTheme(
    design: CroixDesignState,
    albumSeedPrimary: Color? = null,
    albumSeedSecondary: Color? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val safeDesign = design.clamped()

    val targetPrimary = when (safeDesign.colorMode) {
        PrimeColorMode.VIOLET_GLASS -> Color(0xFFA238FF)
        PrimeColorMode.NEON_CYAN -> Color(0xFF00E5FF)
        PrimeColorMode.ALBUM_MORPH -> albumSeedPrimary ?: Color(0xFFA238FF)
        PrimeColorMode.DYNAMIC_SYSTEM -> dynamicDarkColorScheme(context).primary
        PrimeColorMode.AMOLED_BLACK -> albumSeedPrimary ?: Color(0xFFA238FF)
        PrimeColorMode.CUSTOM_COLOR -> safeDesign.customAccentColor
    }
    val targetSecondary = when (safeDesign.colorMode) {
        PrimeColorMode.VIOLET_GLASS -> Color(0xFFFF2E93)
        PrimeColorMode.NEON_CYAN -> Color(0xFF7C4DFF)
        PrimeColorMode.ALBUM_MORPH -> albumSeedSecondary ?: Color(0xFF00E5FF)
        PrimeColorMode.DYNAMIC_SYSTEM -> dynamicDarkColorScheme(context).tertiary
        PrimeColorMode.AMOLED_BLACK -> Color(0xFF3A3A48)
        PrimeColorMode.CUSTOM_COLOR -> safeDesign.secondaryAccentColor
    }
    val targetBg = when (safeDesign.colorMode) {
        PrimeColorMode.AMOLED_BLACK -> Color.Black
        else -> Color(0xFF09080F)
    }

    val primary by animateColorAsState(targetPrimary, tween(450), label = "primary")
    val secondary by animateColorAsState(targetSecondary, tween(450), label = "secondary")
    val bg by animateColorAsState(targetBg, tween(450), label = "bg")

    val resolved = CroixResolvedPalette(
        primary = primary,
        secondary = secondary,
        background = bg,
        glassSurface = Color.White.copy(alpha = safeDesign.glassOpacity),
        glassBorderTop = Color.White.copy(alpha = safeDesign.borderGlowAlpha),
        glassBorderBottom = primary.copy(alpha = (safeDesign.borderGlowAlpha * 0.45f).coerceIn(0f, 0.35f)),
        textPrimary = Color.White,
        textSecondary = Color.White.copy(alpha = 0.68f),
    )

    val m3Scheme = darkColorScheme(
        primary = primary,
        secondary = secondary,
        background = bg,
        surface = Color(0xFF13111E),
        surfaceVariant = Color(0xFF1E1B2E),
        onPrimary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color.White.copy(alpha = 0.72f),
    )

    CompositionLocalProvider(
        LocalCroixDesign provides safeDesign,
        LocalCroixPalette provides resolved,
    ) {
        MaterialTheme(
            colorScheme = m3Scheme,
            typography = PrimeTypography,
            content = content,
        )
    }
}

/**
 * Full-screen living ambient backdrop (Deezer glow + Yuma depth) that fuels all translucent glass cards.
 */
@Composable
fun CroixAnimatedBackdrop(
    modifier: Modifier = Modifier,
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val glow = design.ambientGlowStrength

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        if (glow > 0.02f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (design.glassBlurDp > 0f) (design.glassBlurDp * 1.5f).dp else 0.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                palette.primary.copy(alpha = glow * 0.65f),
                                Color.Transparent,
                            ),
                            center = Offset(220f, 180f),
                            radius = 900f,
                        ),
                    )
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                palette.secondary.copy(alpha = glow * 0.48f),
                                Color.Transparent,
                            ),
                            center = Offset(900f, 1450f),
                            radius = 950f,
                        ),
                    ),
            )
        }
    }
}

/**
 * Core translucent Glassmorphism surface driven live by Croix Studio sliders + real Android 12+ Haze blur.
 */
@Composable
fun CroixGlassCard(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    cornerRadius: Dp = LocalCroixDesign.current.cornerRadiusDp.dp,
    opacityOverride: Float? = null,
    accentTint: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val shape = RoundedCornerShape(cornerRadius)
    val effectiveAlpha = (opacityOverride ?: design.glassOpacity).coerceIn(0.04f, 0.85f)
    val baseFill = (accentTint ?: Color.White).copy(alpha = effectiveAlpha)

    val hazeMod = if (hazeState != null && design.glassBlurDp > 1f) {
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = palette.background.copy(alpha = 0.5f),
                tint = HazeTint(baseFill),
                blurRadius = design.glassBlurDp.dp,
                noiseFactor = 0.04f,
            ),
        )
    } else {
        Modifier.background(
            Brush.verticalGradient(
                listOf(
                    baseFill,
                    (accentTint ?: Color.White).copy(alpha = (effectiveAlpha * 0.68f).coerceAtLeast(0.03f)),
                ),
            ),
            shape,
        )
    }

    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .then(hazeMod)
            .border(
                width = design.borderWidthDp.dp,
                brush = Brush.verticalGradient(
                    listOf(palette.glassBorderTop, palette.glassBorderBottom),
                ),
                shape = shape,
            )
            .then(clickMod),
        content = content,
    )
}

data class CroixNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Floating translucent Yuma-style pill navigation bar (switches live between Floating / Docked / Compact).
 */
@Composable
fun CroixNavigationBar(
    destinations: List<CroixNavDestination>,
    currentRoute: String,
    hazeState: HazeState,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val isFloating = design.navBarStyle != PrimeNavBarStyle.DOCKED_TRANSLUCENT
    val showLabels = design.showNavLabels && design.navBarStyle != PrimeNavBarStyle.COMPACT_ICONS

    val outerPadding = if (isFloating) 16.dp else 0.dp
    val corner = if (isFloating) (design.cornerRadiusDp + 6f).coerceAtMost(36f).dp else 0.dp

    CroixGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = outerPadding, vertical = if (isFloating) 10.dp else 0.dp),
        hazeState = hazeState,
        cornerRadius = corner,
        opacityOverride = (design.glassOpacity + 0.08f).coerceAtMost(0.80f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { dest ->
                val selected = dest.route == currentRoute
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) palette.primary.copy(alpha = 0.22f) else Color.Transparent,
                        )
                        .clickable { onSelect(dest.route) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = dest.label,
                        tint = if (selected) palette.primary else palette.textSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                    if (showLabels) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = dest.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 11.sp,
                            color = if (selected) Color.White else palette.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive seekbar supporting 4 live styles from Croix Studio: Waveform, Capsule, Thin Line, Squiggly.
 */
@Composable
fun CroixSeekBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val design = LocalCroixDesign.current
    val palette = LocalCroixPalette.current
    val clamped = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (size.width > 0) onSeek((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    if (size.width > 0) onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerY = h / 2f
            val activeX = w * clamped
            val inactiveColor = Color.White.copy(alpha = 0.22f)

            when (design.progressStyle) {
                PrimeProgressStyle.WAVEFORM -> {
                    val bars = 46
                    val gap = w / bars
                    val barWidth = (gap * 0.52f).coerceAtLeast(3f)
                    for (i in 0 until bars) {
                        val frac = i.toFloat() / bars
                        val wave = 0.25f + 0.75f * abs(sin((i * 0.65 + 0.4)).toFloat())
                        val barH = (h * 0.82f) * wave
                        val x = i * gap + (gap - barWidth) / 2f
                        val color = if (frac <= clamped) palette.primary else inactiveColor
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, centerY - barH / 2f),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                        )
                    }
                }
                PrimeProgressStyle.CAPSULE -> {
                    val barH = 12f
                    drawRoundRect(
                        color = inactiveColor,
                        topLeft = Offset(0f, centerY - barH / 2f),
                        size = Size(w, barH),
                        cornerRadius = CornerRadius(barH / 2f, barH / 2f),
                    )
                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(palette.primary, palette.secondary)),
                        topLeft = Offset(0f, centerY - barH / 2f),
                        size = Size(activeX.coerceAtLeast(barH), barH),
                        cornerRadius = CornerRadius(barH / 2f, barH / 2f),
                    )
                }
                PrimeProgressStyle.THIN_LINE -> {
                    drawLine(inactiveColor, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 5f, cap = StrokeCap.Round)
                    drawLine(palette.primary, Offset(0f, centerY), Offset(activeX, centerY), strokeWidth = 5f, cap = StrokeCap.Round)
                    drawCircle(Color.White, radius = 9f, center = Offset(activeX, centerY))
                }
                PrimeProgressStyle.SQUIGGLY -> {
                    drawLine(inactiveColor, Offset(activeX, centerY), Offset(w, centerY), strokeWidth = 6f, cap = StrokeCap.Round)
                    val path = Path()
                    val step = 6f
                    var x = 0f
                    while (x <= activeX) {
                        val y = centerY + sin((x / 24f) * PI).toFloat() * 7f
                        if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                        x += step
                    }
                    drawPath(path, color = palette.primary, style = Stroke(width = 6f, cap = StrokeCap.Round))
                    drawCircle(Color.White, radius = 10f, center = Offset(activeX, centerY))
                }
            }
        }
    }
}
