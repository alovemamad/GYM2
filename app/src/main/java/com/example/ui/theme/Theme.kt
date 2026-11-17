package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlin.math.max
import kotlin.math.min

/**
 * رنگ متن/آیکون خوانا روی یک پس‌زمینه: سفید یا مشکی، هرکدام کنتراست بیشتری دارد.
 */
fun onColorFor(background: Color): Color {
    val l = background.luminance()
    val whiteContrast = 1.05f / (l + 0.05f)
    val darkContrast = (l + 0.05f) / 0.0556f
    return if (whiteContrast >= darkContrast) Color.White else Color(0xFF111113)
}

/**
 * نسبت کنتراست WCAG 2.1 بین رنگ رویی (با پشتیبانی از آلفا) و رنگ پس‌زمینه.
 */
fun wcagContrastRatio(foreground: Color, background: Color): Float {
    val opaqueBg = background.copy(alpha = 1f)
    val effectiveFg = if (foreground.alpha < 1f) foreground.compositeOver(opaqueBg) else foreground
    val l1 = effectiveFg.luminance()
    val l2 = opaqueBg.luminance()
    val brighter = max(l1, l2)
    val darker = min(l1, l2)
    return (brighter + 0.05f) / (darker + 0.05f)
}

fun liquidGlassSurfaceColor(palette: LiquidPalette, isDark: Boolean): Color {
    return if (isDark) {
        androidx.compose.ui.graphics.lerp(Color(0xFF0B0F16), palette.gradientEnd, 0.34f)
    } else {
        androidx.compose.ui.graphics.lerp(Color(0xFFF3F6FA), palette.gradientStart, 0.08f)
    }
}

fun liquidSurfaceSolidColor(palette: LiquidPalette, isDark: Boolean): Color {
    return if (isDark) {
        androidx.compose.ui.graphics.lerp(Color(0xFF141A22), palette.gradientEnd, 0.32f).copy(alpha = 0.84f)
    } else {
        androidx.compose.ui.graphics.lerp(Color(0xFFEEF3F8), palette.gradientStart, 0.10f).copy(alpha = 0.94f)
    }
}

fun liquidSurfaceElevatedColor(palette: LiquidPalette, isDark: Boolean): Color {
    return if (isDark) {
        androidx.compose.ui.graphics.lerp(Color(0xFF1E2530), palette.nightAccent, 0.18f).copy(alpha = 0.90f)
    } else {
        androidx.compose.ui.graphics.lerp(Color(0xFFE0E7F0), palette.gradientStart, 0.14f).copy(alpha = 0.94f)
    }
}

fun liquidBackgroundNeutralColor(palette: LiquidPalette, isDark: Boolean): Color {
    return if (isDark) {
        androidx.compose.ui.graphics.lerp(Color(0xFF06080B), palette.gradientEnd, 0.25f)
    } else {
        androidx.compose.ui.graphics.lerp(Color(0xFFE9EFF6), palette.gradientStart, 0.10f)
    }
}

fun liquidTextPrimaryColor(isDark: Boolean): Color {
    return if (isDark) DarkTextPrimary else Color(0xFF0F141C)
}

fun liquidTextSecondaryColor(palette: LiquidPalette, isDark: Boolean): Color {
    return if (isDark) {
        Color.White.copy(alpha = 0.80f)
    } else {
        androidx.compose.ui.graphics.lerp(Color(0xFF2D3644), palette.gradientStart, 0.08f)
    }
}

/** چرخاندن رنگ‌مایه (hue) برای ساخت طیف هم‌خانواده از رنگ اصلی پالت. */
fun Color.shiftHue(degrees: Float, satMul: Float = 1f, valMul: Float = 1f): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[0] = ((hsv[0] + degrees) % 360f + 360f) % 360f
    hsv[1] = (hsv[1] * satMul).coerceIn(0f, 1f)
    hsv[2] = (hsv[2] * valMul).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

val LocalLiquidPalette = compositionLocalOf { LiquidPalettes.SPORT_RED }
val LocalIsDarkTheme = compositionLocalOf { true }

object LiquidTheme {
    val palette: LiquidPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalLiquidPalette.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalIsDarkTheme.current

    val accentColor: Color
        @Composable
        @ReadOnlyComposable
        get() = palette.getAccent(isDark)

    // رنگ آیکون‌ها و برجسته‌سازی‌ها: همگی از پالت انتخابی کاربر ساخته می‌شوند
    val iconRed: Color
        @Composable
        @ReadOnlyComposable
        get() = accentColor
    val iconOrange: Color
        @Composable
        @ReadOnlyComposable
        get() = accentColor.shiftHue(24f)
    val iconGreen: Color
        @Composable
        @ReadOnlyComposable
        get() = accentColor.shiftHue(-24f, valMul = 1.05f)
    val iconBlue: Color
        @Composable
        @ReadOnlyComposable
        get() = accentColor.shiftHue(48f, 0.92f)
    val iconCyan: Color
        @Composable
        @ReadOnlyComposable
        get() = accentColor.shiftHue(-48f, 0.9f)
    val iconPurple: Color
        @Composable
        @ReadOnlyComposable
        get() = accentColor.shiftHue(72f, 0.85f)
    val iconIndigo: Color
        @Composable
        @ReadOnlyComposable
        get() = accentColor.shiftHue(-72f, 0.85f)

    val onAccent: Color
        @Composable
        @ReadOnlyComposable
        get() = onColorFor(accentColor)

    val cardGradient: Brush
        @Composable
        @ReadOnlyComposable
        get() = palette.getCardGradient()

    /**
     * رنگ پایه کپسول‌ها و سطوح شیشه‌ای بدون نور سفید خیره‌کننده در روز
     * و با عمق رنگی هماهنگ با پالت در شب.
     */
    val glassSurfaceColor: Color
        @Composable
        @ReadOnlyComposable
        get() = liquidGlassSurfaceColor(palette, isDark)

    val surfaceSolid: Color
        @Composable
        @ReadOnlyComposable
        get() = liquidSurfaceSolidColor(palette, isDark)

    val surfaceElevated: Color
        @Composable
        @ReadOnlyComposable
        get() = liquidSurfaceElevatedColor(palette, isDark)

    val backgroundNeutral: Color
        @Composable
        @ReadOnlyComposable
        get() = liquidBackgroundNeutralColor(palette, isDark)

    val dividerColor: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) {
            Color.White.copy(alpha = 0.14f)
        } else {
            Color(0xFF1C2430).copy(alpha = 0.16f)
        }

    val textPrimary: Color
        @Composable
        @ReadOnlyComposable
        get() = liquidTextPrimaryColor(isDark)

    val textSecondary: Color
        @Composable
        @ReadOnlyComposable
        get() = liquidTextSecondaryColor(palette, isDark)
}

fun createLiquidColorScheme(palette: LiquidPalette, isDark: Boolean): ColorScheme {
    val accent = palette.getAccent(isDark)
    val onAccent = onColorFor(accent)
    val bg = liquidBackgroundNeutralColor(palette, isDark)
    return if (isDark) {
        val card = androidx.compose.ui.graphics.lerp(Color(0xFF14181F), palette.gradientEnd, 0.26f)
        val elevated = androidx.compose.ui.graphics.lerp(Color(0xFF1E242E), palette.gradientEnd, 0.30f)
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accent.copy(alpha = 0.20f),
            onPrimaryContainer = accent,
            secondary = accent,
            onSecondary = onAccent,
            background = bg,
            onBackground = DarkTextPrimary,
            surface = card,
            onSurface = DarkTextPrimary,
            surfaceVariant = elevated,
            onSurfaceVariant = DarkTextSecondary,
            outline = DarkDivider,
            error = SemanticError,
            onError = Color.White
        )
    } else {
        val card = liquidGlassSurfaceColor(palette, isDark = false)
        val elevated = androidx.compose.ui.graphics.lerp(Color(0xFFDCE4EE), palette.gradientStart, 0.16f)
        val inkPrimary = liquidTextPrimaryColor(isDark = false)
        val inkSecondary = liquidTextSecondaryColor(palette, isDark = false)
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accent.copy(alpha = 0.16f),
            onPrimaryContainer = accent,
            secondary = accent,
            onSecondary = onAccent,
            background = bg,
            onBackground = inkPrimary,
            surface = card,
            onSurface = inkPrimary,
            surfaceVariant = elevated,
            onSurfaceVariant = inkSecondary,
            outline = LightDivider,
            error = SemanticError,
            onError = Color.White
        )
    }
}

// Backward compatibility helper
fun getThemeColorScheme(themeName: String, systemIsDark: Boolean): ColorScheme {
    val isDark = when (themeName.uppercase()) {
        "LIGHT" -> false
        "DARK" -> true
        "SYSTEM" -> systemIsDark
        else -> systemIsDark
    }
    val palette = LiquidPalettes.fromId(themeName)
    return createLiquidColorScheme(palette, isDark)
}

@Composable
fun GymTheme(
    appearanceMode: String = "SYSTEM",
    accentPalette: String = "SPORT_RED",
    content: @Composable () -> Unit
) {
    val systemIsDark = isSystemInDarkTheme()
    val isDark = when (appearanceMode.uppercase()) {
        "LIGHT" -> false
        "DARK" -> true
        else -> systemIsDark
    }

    val palette = LiquidPalettes.fromId(accentPalette)
    val colorScheme = createLiquidColorScheme(palette, isDark)
    val defaultInk = liquidTextPrimaryColor(isDark)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalLiquidPalette provides palette,
        LocalIsDarkTheme provides isDark
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = Shapes(extraSmall = RoundedCornerShape(16.dp))
        ) {
            CompositionLocalProvider(LocalContentColor provides defaultInk) {
                content()
            }
        }
    }
}
