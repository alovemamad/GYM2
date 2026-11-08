package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * رنگ متن/آیکون خوانا روی یک پس‌زمینه: سفید یا مشکی، هرکدام کنتراست بیشتری دارد.
 */
fun onColorFor(background: Color): Color {
    val l = background.luminance()
    val whiteContrast = 1.05f / (l + 0.05f)
    val darkContrast = (l + 0.05f) / 0.0556f
    return if (whiteContrast >= darkContrast) Color.White else Color(0xFF111113)
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

    val surfaceSolid: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.62f)

    val surfaceElevated: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.06f)

    val backgroundNeutral: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) DarkBackground else LightBackground

    val dividerColor: Color
        @Composable
        @ReadOnlyComposable
        get() = Color.White.copy(alpha = 0.15f)

    val textPrimary: Color
        @Composable
        @ReadOnlyComposable
        get() = Color.White

    val textSecondary: Color
        @Composable
        @ReadOnlyComposable
        get() = Color.White.copy(alpha = 0.82f)
}

fun createLiquidColorScheme(palette: LiquidPalette, isDark: Boolean): ColorScheme {
    val accent = palette.getAccent(isDark)
    return if (isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.18f),
            onPrimaryContainer = accent,
            secondary = accent,
            onSecondary = Color.White,
            background = DarkBackground,
            onBackground = DarkTextPrimary,
            surface = DarkCard,
            onSurface = DarkTextPrimary,
            surfaceVariant = DarkCardElevated,
            onSurfaceVariant = DarkTextSecondary,
            outline = DarkDivider,
            error = SemanticError,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.14f),
            onPrimaryContainer = accent,
            secondary = accent,
            onSecondary = Color.White,
            background = LightBackground,
            onBackground = LightTextPrimary,
            surface = LightCard,
            onSurface = LightTextPrimary,
            surfaceVariant = LightCardElevated,
            onSurfaceVariant = LightTextSecondary,
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
            shapes = Shapes(extraSmall = RoundedCornerShape(16.dp)),
            content = content // 👈 اینجا اصلاح شد
        )
    }
}
