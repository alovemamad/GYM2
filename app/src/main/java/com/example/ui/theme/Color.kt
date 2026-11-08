package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// iOS Liquid Glass Design System Tokens
// ==========================================

// --- Neutral Colors (Day / Night) ---
val LightBackground = Color(0xFFF2F2F7)
val LightCard = Color(0xFFFFFFFF)
val LightCardElevated = Color(0xFFE5E5EA)
val LightDivider = Color(0x2E3C3C43) // 18% alpha
val LightTextPrimary = Color(0xFF000000)
val LightTextSecondary = Color(0xFF6C6C70)

val DarkBackground = Color(0xFF000000)
val DarkCard = Color(0xFF1C1C1E)
val DarkCardElevated = Color(0xFF2C2C2E)
val DarkDivider = Color(0xFF38383A)
val DarkTextPrimary = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFF8E8E93)

// --- Glass / Control Tokens ---
val GlassBaseDark = Color(0xFF141416)
val GlassBaseLight = Color(0xFFF9F9FC)
val SwitchTrackOff = Color(0xFF787880)

// --- Brand Colors (social icons) ---
val BrandTelegram = Color(0xFF29B6F6)
val BrandInstagram = Color(0xFFE1306C)

// --- Fixed Semantic Status Colors ---
val SemanticSuccess = Color(0xFF34C759)
val SemanticWarning = Color(0xFFFF9500)
val SemanticError = Color(0xFFFF3B30) // Destructive action

// --- Semantic Icon Colors (Inset Grouped Rows) ---
val SemanticIconPurple = Color(0xFFAF52DE)
val SemanticIconIndigo = Color(0xFF5856D6)
val SemanticIconCyan = Color(0xFF32ADE6)
val SemanticIconBlue = Color(0xFF007AFF)
val SemanticIconOrange = Color(0xFFFF9500)
val SemanticIconGreen = Color(0xFF34C759)
val SemanticIconRed = Color(0xFFFF3B30)
val SemanticIconGray = Color(0xFF8E8E93)

// --- 8 Accent Palettes Specification ---
data class LiquidPalette(
    val id: String,
    val persianName: String,
    val dayAccent: Color,
    val nightAccent: Color,
    val gradientStart: Color,
    val gradientEnd: Color
) {
    fun getAccent(isDark: Boolean): Color = if (isDark) nightAccent else dayAccent

    fun getCardGradient(): Brush = Brush.linearGradient(
        colors = listOf(gradientStart, gradientEnd)
    )
}

object LiquidPalettes {
    val SPORT_RED = LiquidPalette(
        id = "SPORT_RED",
        persianName = "اسپورت قرمز و مشکی",
        dayAccent = Color(0xFFD90D3C),
        nightAccent = Color(0xFFFF3355),
        gradientStart = Color(0xFFE5123F),
        gradientEnd = Color(0xFF14060A)
    )

    val GOLD_CARBON = LiquidPalette(
        id = "GOLD_CARBON",
        persianName = "طلایی و کربن",
        dayAccent = Color(0xFFE09100),
        nightAccent = Color(0xFFFFB020),
        gradientStart = Color(0xFFFFB020),
        gradientEnd = Color(0xFF1A1203)
    )

    val OCEAN = LiquidPalette(
        id = "OCEAN",
        persianName = "اقیانوس آبی",
        dayAccent = Color(0xFF0A74E0),
        nightAccent = Color(0xFF3B9BFF),
        gradientStart = Color(0xFF1E90FF),
        gradientEnd = Color(0xFF021B3A)
    )

    val EMERALD = LiquidPalette(
        id = "EMERALD",
        persianName = "زمردی ورزشی",
        dayAccent = Color(0xFF12A05A),
        nightAccent = Color(0xFF34D17F),
        gradientStart = Color(0xFF19C37D),
        gradientEnd = Color(0xFF03281A)
    )

    val SUNSET = LiquidPalette(
        id = "SUNSET",
        persianName = "غروب آتشین",
        dayAccent = Color(0xFFF25A0A),
        nightAccent = Color(0xFFFF7A33),
        gradientStart = Color(0xFFFF8A2B),
        gradientEnd = Color(0xFF8A1FA8)
    )

    val VIOLET = LiquidPalette(
        id = "VIOLET",
        persianName = "بنفش نئون",
        dayAccent = Color(0xFF7C3AED),
        nightAccent = Color(0xFFA78BFA),
        gradientStart = Color(0xFF8B5CF6),
        gradientEnd = Color(0xFF1E1250)
    )

    val ROSE = LiquidPalette(
        id = "ROSE",
        persianName = "رز پینک",
        dayAccent = Color(0xFFE0207A),
        nightAccent = Color(0xFFFF4D9D),
        gradientStart = Color(0xFFFF4D9D),
        gradientEnd = Color(0xFF3A0A2A)
    )

    val CYBER = LiquidPalette(
        id = "CYBER",
        persianName = "سایبرپانک",
        dayAccent = Color(0xFF00A9C7),
        nightAccent = Color(0xFF22E6FF),
        gradientStart = Color(0xFF00E5FF),
        gradientEnd = Color(0xFFFF2BD6)
    )

    val all = listOf(
        SPORT_RED,
        GOLD_CARBON,
        OCEAN,
        EMERALD,
        SUNSET,
        VIOLET,
        ROSE,
        CYBER
    )

    fun fromId(id: String): LiquidPalette {
        return all.firstOrNull { it.id.equals(id, ignoreCase = true) }
            ?: when (id.uppercase()) {
                "RED" -> SPORT_RED
                "DARK" -> SPORT_RED
                "LIGHT" -> SPORT_RED
                "SYSTEM" -> SPORT_RED
                "PINK" -> ROSE
                "PURPLE" -> VIOLET
                else -> SPORT_RED
            }
    }
}
