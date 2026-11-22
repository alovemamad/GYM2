package com.example.ui.glass

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LiquidPalette
import com.example.ui.theme.LiquidPalettes
import com.example.ui.theme.liquidBackgroundNeutralColor
import com.example.ui.theme.liquidGlassSurfaceColor
import com.example.ui.theme.liquidSurfaceElevatedColor
import com.example.ui.theme.liquidSurfaceSolidColor
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GlassMaterial(val id: String, val persianName: String) {
    CLEAR("CLEAR", "شفاف (شیشه‌ای)"),
    TINTED("TINTED", "مات و خوانا");

    companion object {
        fun fromId(id: String): GlassMaterial =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CLEAR
    }
}

enum class GlassWallpaper(val id: String, val persianName: String) {
    BOTANICAL("BOTANICAL", "جنگل استوایی"),
    MIST("MIST", "مه نقره‌ای"),
    LAGOON("LAGOON", "تالاب آبی"),
    DUSK("DUSK", "شفق گرگ‌ومیش"),
    GRAPHITE("GRAPHITE", "گرافیت مدرن"),
    PLAIN("PLAIN", "ساده و مینیمال");

    fun isDarkBackdrop(systemDark: Boolean): Boolean = when (this) {
        BOTANICAL -> true
        else -> systemDark
    }

    companion object {
        fun fromId(id: String): GlassWallpaper =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: BOTANICAL
    }
}

fun isGlassRenderEffectSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
fun isGlassLensSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

object GlassShapes {
    val cell = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    val module = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
    val capsule = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)
    val badge = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
}

@Immutable
data class GlassColors(
    val dark: Boolean,
    val darkBackdrop: Boolean,
    val tint: Color,
    val label: Color,
    val label2: Color,
    val label3: Color,
    val separator: Color,
    val fill: Color,
    val fill2: Color,
    val cell: Color,
    val solidCell: Color,
    val elevatedCell: Color,
    val chrome: Color,
    val chromeSolid: Color,
    val module: Color,
    val rimHi: Color,
    val rimMid: Color,
    val rimLo: Color,
    val highlight: Color,
    val platter: Color,
    val thumb: Color,
    val shadow: Color,
    val translucent: Boolean,
    val reduced: Boolean,
    val wallpaper: GlassWallpaper,
    val material: GlassMaterial,
)

object GlassPalette {
    fun of(
        dark: Boolean,
        palette: LiquidPalette,
        material: GlassMaterial,
        wallpaper: GlassWallpaper,
        glassLists: Boolean,
        reduceTransparency: Boolean,
    ): GlassColors {
        val darkBackdrop = wallpaper.isDarkBackdrop(dark)
        val tinted = material == GlassMaterial.TINTED
        val isBotanical = wallpaper == GlassWallpaper.BOTANICAL
        val rawAccent = palette.getAccent(dark)
        val accent = if (darkBackdrop) {
            rawAccent
        } else {
            val lFg = (0.2126f * rawAccent.red + 0.7152f * rawAccent.green + 0.0722f * rawAccent.blue)
            val lBg = 0.90f // light frosted glass card luminance
            val contrast = (lBg + 0.05f) / (lFg + 0.05f)
            if (contrast >= 3.2f) {
                rawAccent
            } else {
                val baseStart = palette.gradientStart
                val startLum = 0.2126f * baseStart.red + 0.7152f * baseStart.green + 0.0722f * baseStart.blue
                if ((lBg + 0.05f) / (startLum + 0.05f) >= 3.4f) {
                    baseStart
                } else {
                    lerp(baseStart, Color(0xFF101520), 0.35f)
                }
            }
        }

        val baseGlass = liquidGlassSurfaceColor(palette, dark)
        val baseSolid = liquidSurfaceSolidColor(palette, dark)
        val baseElevated = liquidSurfaceElevatedColor(palette, dark)

        val solidCell = when {
            isBotanical -> baseSolid.copy(alpha = 0.94f)
            dark -> lerp(Color(0xFF1C1C1E), palette.gradientEnd, 0.14f)
            else -> Color.White
        }

        val module = when {
            reduceTransparency -> solidCell
            isBotanical && dark && tinted -> lerp(baseGlass, Color(0xFF0E131B), 0.2f).copy(alpha = 0.82f)
            isBotanical && dark -> baseGlass.copy(alpha = 0.40f)
            isBotanical && tinted -> baseGlass.copy(alpha = 0.80f)
            isBotanical -> baseGlass.copy(alpha = 0.42f)
            dark && tinted -> lerp(Color(0xFF28282C), palette.gradientEnd, 0.14f).copy(alpha = 0.86f)
            dark -> lerp(Color(0xFF2C2C30), palette.gradientEnd, 0.12f).copy(alpha = 0.42f)
            tinted -> lerp(Color.White, palette.gradientStart, 0.04f).copy(alpha = 0.86f)
            else -> lerp(Color.White, palette.gradientStart, 0.03f).copy(alpha = 0.54f)
        }

        val chromeSolid = when {
            isBotanical -> baseSolid.copy(alpha = 0.96f)
            dark -> Color(0xFF2C2C2E)
            else -> Color(0xFFF9F9FB)
        }

        val chrome = when {
            reduceTransparency -> chromeSolid
            isBotanical && dark && tinted -> baseGlass.copy(alpha = 0.84f)
            isBotanical && dark -> baseGlass.copy(alpha = 0.44f)
            isBotanical && tinted -> baseGlass.copy(alpha = 0.82f)
            isBotanical -> baseGlass.copy(alpha = 0.48f)
            dark && tinted -> Color(0xDB242426)
            dark -> Color(0x661E1E20)
            tinted -> Color(0xCCFFFFFF)
            else -> Color(0x68FFFFFF)
        }

        val label = if (darkBackdrop) Color.White else Color(0xFF0F131A)
        val label2 = when {
            isBotanical && !dark -> Color.White.copy(alpha = 0.88f)
            darkBackdrop -> Color(0xCCEBEBF5)
            else -> Color(0xB83C3C43)
        }
        val label3 = if (darkBackdrop) Color(0x5CEBEBF5) else Color(0x523C3C43)
        val separator = if (darkBackdrop) Color.White.copy(alpha = 0.16f) else Color(0x2E3C3C43)

        val elevated = when {
            isBotanical -> baseElevated
            dark -> Color(0xFF2C2C30)
            else -> Color(0xFFE5E5EA)
        }

        return GlassColors(
            dark = dark,
            darkBackdrop = darkBackdrop,
            tint = accent,
            label = label,
            label2 = label2,
            label3 = label3,
            separator = separator,
            fill = if (darkBackdrop) Color(0x5C787880) else Color(0x33787880),
            fill2 = if (darkBackdrop) Color(0x3D767680) else Color(0x1F767680),
            cell = if (glassLists) module else solidCell,
            solidCell = solidCell,
            elevatedCell = elevated,
            chrome = chrome,
            chromeSolid = chromeSolid,
            module = module,
            rimHi = if (darkBackdrop) Color(0xADFFFFFF) else Color.White,
            rimMid = if (darkBackdrop) Color(0x24FFFFFF) else Color(0x5CFFFFFF),
            rimLo = if (darkBackdrop) Color(0x47FFFFFF) else Color(0xCCFFFFFF),
            highlight = if (darkBackdrop) Color(0x42FFFFFF) else Color(0xF5FFFFFF),
            platter = if (darkBackdrop) Color(0x24FFFFFF) else Color(0x16000000),
            thumb = if (darkBackdrop) Color(0x3DFFFFFF) else Color(0xF5FFFFFF),
            shadow = if (darkBackdrop) Color(0x80000000) else Color(0x22000000),
            translucent = glassLists && !reduceTransparency,
            reduced = reduceTransparency,
            wallpaper = wallpaper,
            material = material,
        )
    }

    fun wallpaperTop(wallpaper: GlassWallpaper, palette: LiquidPalette, dark: Boolean): Color = when (wallpaper) {
        GlassWallpaper.BOTANICAL -> lerp(Color(0xFF0A140E), palette.gradientEnd, 0.35f)
        GlassWallpaper.MIST -> if (dark) lerp(Color(0xFF17191E), palette.gradientEnd, 0.15f) else Color(0xFFE4E8EF)
        GlassWallpaper.LAGOON -> if (dark) Color(0xFF031327) else Color(0xFFE2EFFF)
        GlassWallpaper.DUSK -> if (dark) Color(0xFF1A0712) else Color(0xFFFFF0E6)
        GlassWallpaper.GRAPHITE -> if (dark) Color(0xFF121317) else Color(0xFFE1E5EC)
        GlassWallpaper.PLAIN -> if (dark) lerp(Color.Black, palette.gradientEnd, 0.08f) else Color(0xFFF2F2F7)
    }
}

@Immutable
class GlassLook(
    val colors: GlassColors,
    val material: GlassMaterial = GlassMaterial.CLEAR,
    val wallpaper: GlassWallpaper = GlassWallpaper.BOTANICAL,
    val glassLists: Boolean = true,
    val shrinkTabBar: Boolean = true,
    val motionHighlights: Boolean = false,
    val reduceTransparency: Boolean = false,
)

val LocalGlassLook = staticCompositionLocalOf {
    GlassLook(
        colors = GlassPalette.of(
            dark = true,
            palette = LiquidPalettes.SPORT_RED,
            material = GlassMaterial.CLEAR,
            wallpaper = GlassWallpaper.BOTANICAL,
            glassLists = true,
            reduceTransparency = false,
        )
    )
}

object Glass {
    val colors: GlassColors
        @Composable @ReadOnlyComposable get() = LocalGlassLook.current.colors

    val look: GlassLook
        @Composable @ReadOnlyComposable get() = LocalGlassLook.current
}

@Stable
class GlassChromeBackdrops(
    val wall: LayerBackdrop,
    val page: LayerBackdrop,
    val combined: Backdrop,
)

val LocalGlassChrome = staticCompositionLocalOf<GlassChromeBackdrops?> { null }
val LocalGlassTilt = staticCompositionLocalOf<State<Float>?> { null }

@Composable
fun rememberGlassChrome(): GlassChromeBackdrops {
    val wall = rememberLayerBackdrop()
    val page = rememberLayerBackdrop()
    val combined = rememberCombinedBackdrop(wall, page)
    return remember(wall, page, combined) { GlassChromeBackdrops(wall, page, combined) }
}

private object BotanicalWallpaperCache {
    @Volatile
    var cachedBitmap: ImageBitmap? = null
}

@Composable
fun GlassWallpaperLayer(
    backdrop: LayerBackdrop,
    palette: LiquidPalette,
    isDark: Boolean,
    wallpaper: GlassWallpaper,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val paletteStart by animateColorAsState(
        targetValue = palette.gradientStart,
        animationSpec = tween(durationMillis = 450),
        label = "bg_palette_start"
    )
    val paletteEnd by animateColorAsState(
        targetValue = palette.gradientEnd,
        animationSpec = tween(durationMillis = 450),
        label = "bg_palette_end"
    )
    val paletteAccent by animateColorAsState(
        targetValue = palette.getAccent(isDark),
        animationSpec = tween(durationMillis = 450),
        label = "bg_palette_accent"
    )
    val baseTint by animateColorAsState(
        targetValue = liquidBackgroundNeutralColor(palette, isDark),
        animationSpec = tween(durationMillis = 350),
        label = "bg_base_tint"
    )

    val botanicalBitmap by produceState<ImageBitmap?>(
        initialValue = BotanicalWallpaperCache.cachedBitmap,
        key1 = wallpaper == GlassWallpaper.BOTANICAL
    ) {
        if (wallpaper == GlassWallpaper.BOTANICAL && value == null) {
            value = withContext(Dispatchers.IO) {
                BotanicalWallpaperCache.cachedBitmap ?: run {
                    val opts = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    BitmapFactory.decodeResource(context.resources, R.drawable.img_liquid_glass_bg, opts)
                        ?.asImageBitmap()
                        ?.also { BotanicalWallpaperCache.cachedBitmap = it }
                }
            }
        }
    }

    val atmosphericBrush = remember(isDark, baseTint, paletteStart, paletteEnd, paletteAccent) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    paletteEnd.copy(alpha = 0.62f),
                    baseTint.copy(alpha = 0.44f),
                    paletteAccent.copy(alpha = 0.16f)
                )
            )
        } else {
            val topTint = lerp(paletteStart, Color(0xFF0D1722), 0.30f).copy(alpha = 0.26f)
            val midTint = lerp(paletteAccent, Color(0xFF0D1722), 0.25f).copy(alpha = 0.12f)
            val botTint = lerp(paletteEnd, Color(0xFF0D1722), 0.30f).copy(alpha = 0.28f)
            Brush.verticalGradient(colors = listOf(topTint, midTint, botTint))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .layerBackdrop(backdrop)
            .drawBehind {
                if (wallpaper != GlassWallpaper.BOTANICAL) {
                    drawGlassWallpaper(wallpaper, isDark, paletteAccent)
                }
            }
    ) {
        if (wallpaper == GlassWallpaper.BOTANICAL) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B1A10))
            )
            botanicalBitmap?.let { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(atmosphericBrush)
            )
        }
    }
}

fun DrawScope.drawGlassWallpaper(
    kind: GlassWallpaper,
    dark: Boolean,
    paletteTint: Color = Color(0xFFFF3355),
) {
    when (kind) {
        GlassWallpaper.BOTANICAL -> {
            drawRect(
                Brush.verticalGradient(
                    0f to Color(0xFF0D2818),
                    0.5f to Color(0xFF153D24),
                    1f to Color(0xFF091A10)
                )
            )
            ellipseGlow(0.75f, 0.2f, 0.7f, 0.38f, Color(0x552EA058))
            ellipseGlow(0.22f, 0.75f, 0.7f, 0.42f, paletteTint.copy(alpha = 0.20f))
        }
        GlassWallpaper.MIST -> {
            drawRect(
                Brush.verticalGradient(
                    0f to (if (dark) lerp(Color(0xFF17191E), paletteTint, 0.12f) else lerp(Color(0xFFE4E8EF), paletteTint, 0.05f)),
                    0.52f to (if (dark) Color(0xFF0B0C0F) else Color(0xFFECEEF3)),
                    1f to (if (dark) Color.Black else Color(0xFFF2F2F7)),
                )
            )
            ellipseGlow(0.82f, 0.12f, 0.75f, 0.35f, paletteTint.copy(alpha = if (dark) 0.22f else 0.10f))
        }
        GlassWallpaper.LAGOON -> {
            drawRect(
                Brush.verticalGradient(
                    listOf(
                        if (dark) Color(0xFF031327) else Color(0xFFE2EFFF),
                        if (dark) Color(0xFF010812) else Color(0xFFEEF5FA),
                    )
                )
            )
            ellipseGlow(0.82f, 0.06f, 0.85f, 0.42f, if (dark) Color(0x730091FF) else Color(0x5C0088FF))
            ellipseGlow(0.08f, 0.58f, 0.80f, 0.40f, if (dark) Color(0x4700D2E0) else Color(0x4700C3D0))
        }
        GlassWallpaper.DUSK -> {
            drawRect(
                Brush.verticalGradient(
                    listOf(
                        if (dark) Color(0xFF1A0712) else Color(0xFFFFF0E6),
                        if (dark) Color(0xFF0A0410) else Color(0xFFF6EDFB),
                    )
                )
            )
            ellipseGlow(0.20f, 0.06f, 0.85f, 0.42f, if (dark) Color(0x61FF9230) else Color(0x5CFF8D28))
            ellipseGlow(0.90f, 0.60f, 0.80f, 0.40f, if (dark) Color(0x52DB34F2) else Color(0x3DCB30E0))
        }
        GlassWallpaper.GRAPHITE -> {
            drawRect(
                Brush.verticalGradient(
                    listOf(
                        if (dark) Color(0xFF121317) else Color(0xFFE1E5EC),
                        if (dark) Color(0xFF050506) else Color(0xFFECEEF2),
                    )
                )
            )
            ellipseGlow(0.80f, 0.08f, 0.85f, 0.42f, if (dark) Color(0x3D98989D) else Color(0xE6FFFFFF))
        }
        GlassWallpaper.PLAIN -> drawRect(if (dark) Color.Black else Color(0xFFF2F2F7))
    }
}

private fun DrawScope.ellipseGlow(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) {
    val radiusX = size.width * rx
    val radiusY = size.height * ry
    if (radiusX <= 0f || radiusY <= 0f) return
    val center = Offset(size.width * cx, size.height * cy)
    scale(scaleX = radiusX / radiusY, scaleY = 1f, pivot = center) {
        drawCircle(
            Brush.radialGradient(
                0f to color,
                0.72f to color.copy(alpha = 0f),
                center = center,
                radius = radiusY,
            ),
            radius = radiusY,
            center = center,
        )
    }
}

@Composable
fun GlassTopEdge(
    backdrop: Backdrop?,
    palette: LiquidPalette,
    modifier: Modifier = Modifier,
) {
    val c = Glass.colors
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    if (top <= 0.dp) return
    val height = top + 14.dp
    val tone = GlassPalette.wallpaperTop(c.wallpaper, palette, c.dark)

    if (backdrop == null || c.reduced || !isGlassRenderEffectSupported()) {
        Box(
            modifier
                .fillMaxWidth()
                .height(height)
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            0f to tone.copy(alpha = 0.88f),
                            0.55f to tone.copy(alpha = 0.64f),
                            1f to tone.copy(alpha = 0f),
                        ),
                    )
                },
        )
        return
    }

    val fade = Brush.verticalGradient(0f to Color.Black, 0.55f to Color.Black, 1f to Color.Transparent)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = { blur(10.dp.toPx()) },
                highlight = null,
                shadow = null,
                onDrawSurface = { drawRect(tone.copy(alpha = 0.34f)) },
                onDrawFront = { drawRect(fade, blendMode = BlendMode.DstIn) },
            ),
    )
}

fun glassRimBrush(c: GlassColors): Brush = Brush.linearGradient(
    0f to c.rimHi,
    0.28f to c.rimMid,
    0.48f to Color.Transparent,
    0.64f to Color.Transparent,
    1f to c.rimLo,
)

fun Modifier.glassRim(shape: Shape, c: GlassColors, width: Dp = 1.dp): Modifier =
    border(width, glassRimBrush(c), shape)

fun Modifier.glassHighlight(shape: Shape, color: Color, thickness: Dp = 1.dp): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val inner = Path().apply { addOutline(outline) }
    val shifted = Path().apply {
        addOutline(outline)
        translate(Offset(0f, thickness.toPx()))
    }
    onDrawWithContent {
        clipPath(inner) {
            clipPath(shifted, ClipOp.Difference) { drawRect(color) }
        }
        drawContent()
    }
}

fun glassHighlightOf(tilt: State<Float>?): Highlight =
    if (tilt == null) {
        Highlight.Default
    } else {
        Highlight(style = HighlightStyle.Default(angle = tilt.value))
    }

fun Modifier.glassChrome(
    c: GlassColors,
    shape: Shape,
    backdrop: Backdrop? = null,
    surface: Color = c.chrome,
    blurRadius: Dp = 12.dp,
    refraction: Boolean = true,
    elevated: Boolean = true,
    tilt: State<Float>? = null,
    useTranslucent: Boolean = true,
): Modifier {
    val renderBlur = backdrop != null && !c.reduced && useTranslucent && isGlassRenderEffectSupported()
    if (!renderBlur) {
        val fill = when {
            c.reduced || !useTranslucent -> c.chromeSolid
            else -> lerp(c.chromeSolid, surface, 0.45f).copy(alpha = maxOf(surface.alpha, 0.78f))
        }
        return this
            .then(
                if (elevated) {
                    Modifier.shadow(8.dp, shape, clip = false, ambientColor = c.shadow, spotColor = c.shadow)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(fill)
            .then(
                if (!c.reduced) {
                    Modifier
                        .glassHighlight(shape, c.highlight)
                        .glassRim(shape, c)
                } else {
                    Modifier.border(0.6.dp, c.separator, shape)
                }
            )
    }

    return this.drawBackdrop(
        backdrop = backdrop!!,
        shape = { shape },
        effects = {
            vibrancy()
            blur(blurRadius.toPx())
            if (refraction && isGlassLensSupported()) {
                lens(12.dp.toPx(), 22.dp.toPx())
            }
        },
        highlight = { glassHighlightOf(tilt) },
        shadow = if (elevated) ({ Shadow(radius = 20.dp, color = c.shadow) }) else null,
        onDrawSurface = { drawRect(surface) },
    )
}

private class GlassPressIndication(private val color: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        GlassPressNode(interactionSource, color)

    override fun equals(other: Any?): Boolean = other is GlassPressIndication && other.color == color
    override fun hashCode(): Int = color.hashCode()
}

private class GlassPressNode(
    private val source: InteractionSource,
    private val color: Color,
) : Modifier.Node(), DrawModifierNode {
    private var pressed = false

    override fun onAttach() {
        coroutineScope.launch {
            var count = 0
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> count++
                    is PressInteraction.Release, is PressInteraction.Cancel -> count = (count - 1).coerceAtLeast(0)
                }
                val now = count > 0
                if (now != pressed) {
                    pressed = now
                    invalidateDraw()
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        if (pressed) drawRect(color)
        drawContent()
    }
}

@Composable
fun glassRowIndication(): Indication {
    val c = Glass.colors
    return remember(c.fill2) { GlassPressIndication(c.fill2) }
}

@Composable
fun Modifier.glassPress(source: InteractionSource, scale: Float = 1.06f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "glass-press",
    )
    return graphicsLayer {
        scaleX = s
        scaleY = s
    }
}
