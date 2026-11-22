package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.glass.*
import com.example.ui.theme.*
import com.example.util.PersianNumberHelper
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.roundToInt

// ==========================================
// 1. LIQUID GLASS BASE MODIFIERS & ROOT
// ==========================================

val LocalLiquidBottomClearance = staticCompositionLocalOf { 0.dp }

/**
 * Root container providing the selected organic or studio Liquid Glass wallpaper
 * and Kyant Backdrop layer hosts.
 */
@Composable
fun LiquidGlassRoot(
    chrome: GlassChromeBackdrops = rememberGlassChrome(),
    motionHighlights: Boolean = Glass.look.motionHighlights,
    content: @Composable () -> Unit
) {
    val c = Glass.colors
    val look = Glass.look
    val tilt = rememberGlassTilt(enabled = motionHighlights && !c.reduced)

    Box(modifier = Modifier.fillMaxSize()) {
        GlassWallpaperLayer(
            backdrop = chrome.wall,
            palette = LiquidTheme.palette,
            isDark = LiquidTheme.isDark,
            wallpaper = look.wallpaper
        )

        Box(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(
                LocalGlassChrome provides chrome,
                LocalGlassTilt provides tilt
            ) {
                content()
            }
        }
    }
}

/**
 * Primary Liquid Glass modifier powered by Kyant Backdrop + ZedSecure crystal highlights:
 * - Real-time Backdrop blur + vibrancy (+ optional lens refraction on API 33+)
 * - Adaptive Clear / Tinted material translucency & Reduce Transparency fallback
 * - Crisp top inner highlight & diagonal specular crystal rim
 */
@Composable
fun Modifier.liquidGlass(
    useBackdrop: Boolean = true,
    backdrop: Backdrop? = LocalGlassChrome.current?.wall,
    shape: Shape = RoundedCornerShape(22.dp),
    tintAlpha: Float = if (LiquidTheme.isDark) 0.38f else 0.40f,
    elevation: Dp = 8.dp,
    borderAlpha: Float = if (LiquidTheme.isDark) 0.68f else 0.78f,
    refraction: Boolean = false
): Modifier {
    val c = Glass.colors
    val look = Glass.look
    val tilt = LocalGlassTilt.current
    val isDark = LiquidTheme.isDark
    val accent = LiquidTheme.accentColor

    val targetAlpha = when {
        c.reduced -> 0.96f
        c.material == GlassMaterial.TINTED -> maxOf(tintAlpha, c.module.alpha)
        c.wallpaper != GlassWallpaper.BOTANICAL -> c.module.alpha
        else -> tintAlpha
    }
    val surfaceColor = remember(c.cell, targetAlpha) {
        c.cell.copy(alpha = targetAlpha)
    }

    val renderLiveBlur = useBackdrop && backdrop != null && !c.reduced && look.glassLists && isGlassRenderEffectSupported()

    if (renderLiveBlur && backdrop != null) {
        return this.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(14.dp.toPx())
                if (refraction && isGlassLensSupported()) {
                    lens(12.dp.toPx(), 22.dp.toPx())
                }
            },
            highlight = { glassHighlightOf(tilt) },
            shadow = if (elevation > 0.dp) {
                {
                    Shadow(
                        radius = elevation * 2f,
                        color = if (c.darkBackdrop) Color.Black.copy(alpha = 0.42f) else c.shadow
                    )
                }
            } else {
                null
            },
            onDrawSurface = {
                drawRect(surfaceColor)
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = if (c.darkBackdrop) 0.12f else 0.24f),
                        0.45f to accent.copy(alpha = if (c.darkBackdrop) 0.04f else 0.03f),
                        1f to Color.Transparent
                    )
                )
            }
        )
    }

    val fallbackColor = when {
        c.reduced || !look.glassLists -> c.solidCell
        c.wallpaper != GlassWallpaper.BOTANICAL -> lerp(c.solidCell, surfaceColor, 0.45f).copy(alpha = maxOf(surfaceColor.alpha, 0.86f))
        else -> surfaceColor.copy(alpha = (targetAlpha + if (isDark) 0.30f else 0.28f).coerceAtMost(0.94f))
    }

    val borderBrush = remember(c.rimHi, c.rimMid, c.rimLo, accent, borderAlpha, c.darkBackdrop) {
        Brush.linearGradient(
            colors = listOf(
                c.rimHi.copy(alpha = borderAlpha.coerceIn(0f, 1f)),
                accent.copy(alpha = if (c.darkBackdrop) 0.34f else 0.22f),
                c.rimMid,
                c.rimLo
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    val innerSpecularBrush = remember(c.darkBackdrop, accent) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (c.darkBackdrop) 0.14f else 0.26f),
                accent.copy(alpha = if (c.darkBackdrop) 0.04f else 0.03f),
                Color.Transparent,
                Color.White.copy(alpha = if (c.darkBackdrop) 0.04f else 0.08f)
            )
        )
    }

    return this
        .then(
            if (elevation > 0.dp) {
                Modifier.shadow(
                    elevation = elevation,
                    shape = shape,
                    spotColor = if (c.darkBackdrop) accent.copy(alpha = 0.24f) else c.shadow,
                    ambientColor = c.shadow
                )
            } else {
                Modifier
            }
        )
        .clip(shape)
        .background(fallbackColor)
        .then(
            if (!c.reduced) {
                Modifier
                    .background(innerSpecularBrush)
                    .glassHighlight(shape, c.highlight)
                    .border(BorderStroke(1.dp, borderBrush), shape = shape)
            } else {
                Modifier.border(0.75.dp, c.separator, shape = shape)
            }
        )
}

/**
 * Liquid tap animation: scales down to 0.95 with a responsive spring and haptic feedback.
 */
@Composable
fun Modifier.liquidTapEffect(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "liquid_tap"
    )

    return if (!enabled) this else this
        .scale(scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
        }
}

// ==========================================
// 2. BASE CARDS & CONTAINERS
// ==========================================

@Composable
fun SolidGroupedCard(
    modifier: Modifier = Modifier,
    useBackdrop: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(useBackdrop = useBackdrop, shape = shape, elevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun LiquidGlassBox(
    modifier: Modifier = Modifier,
    useBackdrop: Boolean = true,
    backdrop: Backdrop? = LocalGlassChrome.current?.wall,
    refraction: Boolean = false,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlass(
            useBackdrop = useBackdrop,
            backdrop = backdrop,
            refraction = refraction,
            shape = shape,
            elevation = elevation
        ),
        content = content
    )
}

@Composable
fun LiquidSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val darkBackdrop = Glass.colors.darkBackdrop
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 15.sp,
                shadow = if (darkBackdrop) {
                    androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.55f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 4f
                    )
                } else {
                    null
                }
            ),
            color = LiquidTheme.textSecondary,
            fontWeight = FontWeight.SemiBold
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium,
                color = LiquidTheme.accentColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onActionClick() }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun GroupedRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = LiquidTheme.accentColor,
    iconBackground: Color? = null,
    valueText: String? = null,
    showArrow: Boolean = true,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val badgeShape = RoundedCornerShape(12.dp)
    val badgeBrush = remember(iconTint, iconBackground) {
        val base = iconBackground ?: iconTint
        Brush.verticalGradient(
            0f to lerp(base, Color.White, 0.24f),
            0.62f to base,
            1f to lerp(base, Color.Black, 0.10f)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = glassRowIndication(),
                    enabled = onClick != null
                ) { onClick?.invoke() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(1.5.dp, badgeShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.28f), spotColor = Color.Black.copy(alpha = 0.28f))
                            .clip(badgeShape)
                            .background(badgeBrush)
                            .glassHighlight(badgeShape, Color.White.copy(alpha = 0.45f), 0.75.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = LiquidTheme.textPrimary
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = LiquidTheme.textSecondary
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (valueText != null) {
                    Text(
                        valueText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LiquidTheme.textSecondary
                    )
                }
                if (trailingContent != null) {
                    trailingContent()
                } else if (showArrow && onClick != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        null,
                        tint = LiquidTheme.textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        if (showDivider) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = if (icon != null) 72.dp else 16.dp)
                    .height(0.5.dp)
                    .background(LiquidTheme.dividerColor)
            )
        }
    }
}

@Composable
fun GradientHeroCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    gradient: Brush = LiquidTheme.cardGradient,
    badgeText: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val shape = RoundedCornerShape(28.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, spotColor = Color.Black.copy(alpha = 0.35f)),
        shape = shape,
        color = Color.Transparent
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(gradient)
                .background(Color.Black.copy(alpha = 0.28f))
                .border(
                    BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.5f),
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    ),
                    shape
                )
                .padding(20.dp)
        ) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(10.dp, 4.dp)
                        )
                    }
                }
                Text(
                    title,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                if (content != null) {
                    Spacer(Modifier.height(4.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun CircularShortcut(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    iconTint: Color = LiquidTheme.accentColor,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .liquidTapEffect { onClick() }
            .width(76.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(60.dp)
                .liquidGlass(shape = CircleShape, elevation = 4.dp, refraction = true),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = iconTint, modifier = Modifier.size(26.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                shadow = if (Glass.colors.darkBackdrop) {
                    androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.55f),
                        offset = Offset(0f, 1f),
                        blurRadius = 3f
                    )
                } else {
                    null
                }
            ),
            color = LiquidTheme.textPrimary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ContentRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    gradient: Brush = LiquidTheme.cardGradient,
    badgeText: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    SolidGroupedCard(
        modifier = modifier.liquidTapEffect { onClick() },
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(gradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LiquidTheme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = LiquidTheme.accentColor,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LiquidTheme.accentColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = LiquidTheme.accentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp, 4.dp)
                    )
                }
            } else if (trailing != null) {
                trailing()
            }
        }
    }
}

// ==========================================
// 3. BUTTONS & INPUTS
// ==========================================

/**
 * Snipzy Liquid Glass Primary Button:
 * - Active scale: 0.95
 * - Highlight border gradient
 * - Accent luster
 */
@Composable
fun LiquidPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "btn_scale"
    )
    val accent = LiquidTheme.accentColor
    val paletteStart = LiquidTheme.palette.gradientStart
    val midColor = androidx.compose.ui.graphics.lerp(paletteStart, accent, 0.5f)
    val contentColor = onColorFor(midColor)

    val buttonBrush = Brush.linearGradient(
        colors = listOf(
            paletteStart.copy(alpha = if (enabled) 0.94f else 0.45f),
            accent.copy(alpha = if (enabled) 0.90f else 0.40f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .glassPress(interactionSource, scale = 1.03f)
            .scale(scale)
            .shadow(if (isPressed) 2.dp else 10.dp, CircleShape, spotColor = accent.copy(alpha = 0.40f))
            .clip(CircleShape)
            .background(buttonBrush, CircleShape)
            .background(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (enabled) 0.26f else 0.10f),
                    0.52f to Color.Transparent
                )
            )
            .glassHighlight(CircleShape, Color.White.copy(alpha = 0.55f))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun LiquidSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .liquidTapEffect { onClick() }
            .liquidGlass(
                shape = CircleShape,
                elevation = 4.dp,
                tintAlpha = if (LiquidTheme.isDark) 0.34f else 0.42f
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, null, tint = LiquidTheme.textPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = LiquidTheme.textPrimary
            )
        }
    }
}

@Composable
fun LiquidDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .liquidTapEffect { onClick() }
            .liquidGlass(
                shape = CircleShape,
                elevation = 4.dp,
                tintAlpha = 0.25f,
                borderAlpha = 0.35f
            )
            .background(SemanticError.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SemanticError
        )
    }
}

@Composable
fun LiquidCircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    badgeCount: Int? = null,
    size: Dp = 48.dp,
    tint: Color = LiquidTheme.textPrimary,
    backdrop: Backdrop? = LocalGlassChrome.current?.wall,
) {
    val c = Glass.colors
    val tilt = LocalGlassTilt.current
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .glassPress(source)
            .size(size)
            .glassChrome(
                c = c,
                shape = CircleShape,
                backdrop = backdrop,
                refraction = true,
                tilt = tilt,
                useTranslucent = Glass.look.glassLists
            )
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
            }
            .clickable(source, null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        if (badgeCount != null && badgeCount > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(10.dp)
                    .background(SemanticError, CircleShape)
                    .border(1.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
fun LiquidSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val c = Glass.colors
    val accent = LiquidTheme.accentColor
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .liquidGlass(shape = CircleShape, elevation = 4.dp, refraction = false)
            .padding(3.dp)
    ) {
        val slot = maxWidth / items.size
        val activeIdx = selectedIndex.coerceIn(0, items.lastIndex)
        val x by animateDpAsState(
            targetValue = slot * activeIdx,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
            label = "glass_segment_pill"
        )
        val thumbShape = CircleShape
        val thumbFill = if (c.darkBackdrop) {
            accent.copy(alpha = 0.28f)
        } else {
            lerp(c.thumb, accent, 0.14f)
        }
        Box(
            Modifier
                .offset(x = x)
                .width(slot)
                .fillMaxHeight()
                .shadow(3.dp, thumbShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.18f))
                .clip(thumbShape)
                .background(thumbFill)
                .glassHighlight(thumbShape, c.highlight)
                .border(0.8.dp, accent.copy(alpha = if (c.darkBackdrop) 0.72f else 0.45f), thumbShape)
        )
        Row(
            Modifier
                .fillMaxSize()
                .selectableGroup()
        ) {
            items.forEachIndexed { index, title ->
                val isSelected = index == activeIdx
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .selectable(
                            selected = isSelected,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelectIndex(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) (if (c.darkBackdrop) accent else c.label) else LiquidTheme.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun LiquidStepper(
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    step: Double = 1.0,
    min: Double = 0.0,
    max: Double = 500.0,
    formatString: String = "%.1f"
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .liquidGlass(shape = RoundedCornerShape(14.dp), elevation = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onValueChange((value - step).coerceAtLeast(min)) },
            modifier = Modifier.size(42.dp)
        ) {
            Icon(Icons.Filled.Remove, "کاهش", tint = LiquidTheme.textPrimary)
        }
        Box(
            Modifier
                .padding(horizontal = 8.dp)
                .widthIn(min = 52.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (step >= 1.0 && value % 1.0 == 0.0) {
                    PersianNumberHelper.toPersianDigits(value.toInt())
                } else {
                    PersianNumberHelper.toPersianDigits(String.format(java.util.Locale.US, formatString, value))
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LiquidTheme.textPrimary
            )
        }
        IconButton(
            onClick = { onValueChange((value + step).coerceAtMost(max)) },
            modifier = Modifier.size(42.dp)
        ) {
            Icon(Icons.Filled.Add, "افزایش", tint = LiquidTheme.textPrimary)
        }
    }
}

@Composable
fun LiquidSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .liquidGlass(
                shape = CircleShape,
                elevation = if (isFocused) 6.dp else 4.dp,
                borderAlpha = if (isFocused) 0.85f else 0.45f
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Filled.Search,
                null,
                tint = if (isFocused) LiquidTheme.accentColor else LiquidTheme.textSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LiquidTheme.textSecondary.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = LiquidTheme.textPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        null,
                        tint = LiquidTheme.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (trailingAction != null) trailingAction()
        }
    }
}

@Composable
fun LiquidInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    trailingText: String? = null,
    leadingIcon: ImageVector? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val isNumeric = keyboardOptions.keyboardType == KeyboardType.Number ||
            keyboardOptions.keyboardType == KeyboardType.Decimal

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .liquidGlass(
                shape = RoundedCornerShape(18.dp),
                elevation = if (isFocused) 6.dp else 3.dp,
                borderAlpha = if (isFocused) 0.8f else 0.35f
            )
            .padding(horizontal = 16.dp, vertical = if (singleLine) 0.dp else 10.dp),
        contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
    ) {
        Row(
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    null,
                    tint = if (isFocused) LiquidTheme.accentColor else LiquidTheme.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFocused) LiquidTheme.accentColor else LiquidTheme.textSecondary
                )
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LiquidTheme.textSecondary.copy(alpha = 0.4f)
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = { typed ->
                            onValueChange(
                                if (isNumeric) {
                                    PersianNumberHelper.toEnglishDigits(typed)
                                        .replace('٫', '.')
                                        .replace(',', '.')
                                } else typed
                            )
                        },
                        singleLine = singleLine,
                        keyboardOptions = keyboardOptions,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = LiquidTheme.textPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                    )
                }
            }
            if (trailingText != null) {
                Text(
                    trailingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LiquidTheme.textSecondary
                )
            }
        }
    }
}

/**
 * ZedSecure iOS Liquid Glass Toggle Switch:
 * - 63dp x 28dp capsule track with 48dp accessible hit area
 * - Squash-and-stretch 37dp x 24dp glowing capsule thumb
 */
@Composable
fun LiquidSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val c = Glass.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val accent = LiquidTheme.accentColor
    val track by animateColorAsState(
        targetValue = if (checked) accent else c.fill,
        label = "glass_switch_track"
    )
    val thumbX by animateDpAsState(
        targetValue = if (checked) 24.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
        label = "glass_switch_knob"
    )
    val squash by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "glass_switch_press"
    )

    Box(
        modifier = modifier
            .sizeIn(minWidth = 63.dp, minHeight = 48.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .toggleable(
                value = checked,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 63.dp, height = 28.dp)
                .clip(CircleShape)
                .background(track)
                .border(0.8.dp, if (checked) accent else c.separator, CircleShape)
        ) {
            Box(
                Modifier
                    .offset(x = thumbX, y = 2.dp)
                    .size(width = 37.dp, height = 24.dp)
                    .graphicsLayer {
                        scaleX = 1f + 0.22f * squash
                        scaleY = 1f + 0.12f * squash
                    }
                    .shadow(3.dp, CircleShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.22f), spotColor = Color.Black.copy(alpha = 0.22f))
                    .clip(CircleShape)
                    .background(lerp(Color.White, Color.White.copy(alpha = 0.58f), squash))
                    .then(if (squash > 0.01f) Modifier.glassHighlight(CircleShape, Color.White, 1.dp) else Modifier)
            )
        }
    }
}

/**
 * Snipzy Expandable Dropdown / Card:
 * - Glass header with rotating arrow
 * - Animated expanding content
 */
@Composable
fun LiquidExpandableCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "dropdown_arrow"
    )

    SolidGroupedCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (icon != null) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .liquidGlass(shape = RoundedCornerShape(10.dp), elevation = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = LiquidTheme.accentColor, modifier = Modifier.size(20.dp))
                    }
                }
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LiquidTheme.textPrimary
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = LiquidTheme.textSecondary
                        )
                    }
                }
            }

            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = LiquidTheme.textSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(arrowRotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                content = content
            )
        }
    }
}

/**
 * Snipzy Spinner:
 * - Spinning outer ring
 * - Pulsing core with scale & alpha
 */
@Composable
fun LiquidGlassSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    ringColor: Color = LiquidTheme.accentColor
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val isDark = LiquidTheme.isDark
    val coreHighlight = if (isDark) Color.White else LiquidTheme.glassSurfaceColor

    Box(
        modifier = modifier
            .size(size)
            .liquidGlass(shape = CircleShape, elevation = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.15f)) {
            val strokeWidth = 3.dp.toPx()
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        ringColor.copy(alpha = 0.12f),
                        ringColor.copy(alpha = 0.48f),
                        ringColor,
                        if (isDark) Color.White else ringColor
                    )
                ),
                startAngle = rotation,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Box(
            modifier = Modifier
                .size(size * 0.32f)
                .scale(corePulse)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(coreHighlight, ringColor.copy(alpha = 0.72f))
                    )
                )
        )
    }
}

// ==========================================
// 4. NAVIGATION, TOP BAR & DOCK
// ==========================================

@Composable
fun LiquidBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalGlassChrome.current?.combined,
    minimized: Boolean = false,
    onExpand: () -> Unit = {}
) {
    val tabs = com.example.ui.navigation.Screen.bottomNavItems
    val currentScreen = tabs.firstOrNull { it.route == currentRoute } ?: tabs.first()
    val tilt = LocalGlassTilt.current
    val effectiveMinimized = minimized && Glass.look.shrinkTabBar

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(66.dp)
    ) {
        AnimatedContent(
            targetState = effectiveMinimized,
            transitionSpec = {
                (fadeIn(tween(180)) + scaleIn(initialScale = 0.9f)) togetherWith
                    (fadeOut(tween(150)) + scaleOut(targetScale = 0.9f))
            },
            label = "glass_bottom_bar_mode"
        ) { isMini ->
            if (isMini) {
                val c = Glass.colors
                val source = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Row(
                        modifier = Modifier
                            .height(58.dp)
                            .glassPress(source)
                            .glassChrome(
                                c = c,
                                shape = GlassShapes.capsule,
                                backdrop = backdrop,
                                refraction = true,
                                tilt = tilt
                            )
                            .testTag("bottom_bar_expand_pill")
                            .clickable(
                                interactionSource = source,
                                indication = null,
                                role = Role.Button,
                                onClick = onExpand
                            )
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            currentScreen.selectedIcon,
                            contentDescription = "باز کردن منو",
                            tint = LiquidTheme.accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            currentScreen.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = c.label,
                            maxLines = 1
                        )
                        Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = null,
                            tint = c.label2,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                GlassBottomTabPill(
                    tabs = tabs,
                    currentRoute = currentRoute,
                    onNavigate = onNavigate,
                    backdrop = backdrop,
                    tilt = tilt,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun GlassBottomTabPill(
    tabs: List<com.example.ui.navigation.Screen>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    backdrop: Backdrop?,
    tilt: State<Float>?,
    modifier: Modifier = Modifier
) {
    if (tabs.isEmpty()) return
    val c = Glass.colors
    val accent = LiquidTheme.accentColor
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val haptics = LocalHapticFeedback.current
    val exported = rememberLayerBackdrop()
    val navigate by rememberUpdatedState(onNavigate)

    var holding by remember { mutableStateOf(false) }
    var holdX by remember { mutableFloatStateOf(0f) }
    val currentIndex = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    BoxWithConstraints(modifier = modifier) {
        val pad = 4.dp
        val tabWidth = (maxWidth - pad * 2) / tabs.size
        val tabPx = with(density) { tabWidth.toPx() }.coerceAtLeast(1f)
        val padPx = with(density) { pad.toPx() }
        val pillWidth = maxWidth
        val widthPx = with(density) { pillWidth.toPx() }.coerceAtLeast(1f)
        val pillHeight = maxHeight

        fun indexAt(x: Float): Int {
            val slot = ((x - padPx) / tabPx).toInt().coerceIn(0, tabs.lastIndex)
            return if (rtl) tabs.lastIndex - slot else slot
        }

        val hoverIndex = if (holding) indexAt(holdX) else -1
        val barScale by animateFloatAsState(
            targetValue = if (holding) 1.03f else 1f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
            label = "glass_bar_scale"
        )
        val platterX by animateDpAsState(
            targetValue = tabWidth * currentIndex,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
            label = "glass_bar_platter"
        )
        val platterAlpha by animateFloatAsState(
            targetValue = if (holding) 0f else 1f,
            label = "glass_bar_platter_alpha"
        )
        val shape = GlassShapes.capsule
        val canRenderLive = backdrop != null && !c.reduced && isGlassRenderEffectSupported()
        val surface = if (canRenderLive) c.chrome else c.chromeSolid.copy(alpha = 0.94f)

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = barScale
                    scaleY = barScale
                }
                .then(
                    if (canRenderLive && backdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { shape },
                            effects = {
                                vibrancy()
                                blur(12.dp.toPx())
                                if (isGlassLensSupported()) {
                                    lens(14.dp.toPx(), 26.dp.toPx())
                                }
                            },
                            highlight = { glassHighlightOf(tilt) },
                            shadow = { Shadow(radius = 22.dp, color = c.shadow) },
                            exportedBackdrop = exported,
                            onDrawSurface = { drawRect(surface) }
                        )
                    } else {
                        Modifier.glassChrome(
                            c = c,
                            shape = shape,
                            backdrop = null,
                            refraction = false,
                            tilt = tilt
                        )
                    }
                )
                .pointerInput(tabs, rtl, tabPx, padPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var cancelled = false
                        val up = withTimeoutOrNull(180L) {
                            waitForUpOrCancellation().also { if (it == null) cancelled = true }
                        }
                        if (cancelled) return@awaitEachGesture
                        if (up != null) {
                            up.consume()
                            navigate(tabs[indexAt(up.position.x)].route)
                            return@awaitEachGesture
                        }
                        holdX = down.position.x
                        holding = true
                        var lastHovered = indexAt(holdX)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    navigate(tabs[indexAt(change.position.x)].route)
                                    break
                                }
                                holdX = change.position.x
                                val newHovered = indexAt(holdX)
                                if (newHovered != lastHovered) {
                                    lastHovered = newHovered
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                change.consume()
                            }
                        } finally {
                            holding = false
                        }
                    }
                }
        ) {
            Box(
                Modifier
                    .offset(x = pad + platterX, y = pad)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .padding(bottom = pad * 2)
                    .graphicsLayer { alpha = platterAlpha }
                    .clip(shape)
                    .background(lerp(c.platter, accent.copy(alpha = if (c.darkBackdrop) 0.24f else 0.14f), 0.45f))
                    .glassHighlight(shape, c.highlight.copy(alpha = 0.65f), 0.75.dp)
            )
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
            ) {
                tabs.forEachIndexed { index, screen ->
                    val on = screen.route == currentRoute
                    val color = when {
                        holding -> if (index == hoverIndex) accent else c.label
                        on -> accent
                        else -> c.label2
                    }
                    LiquidTabCell(
                        screen = screen,
                        selected = on,
                        color = color,
                        modifier = Modifier
                            .testTag("nav_${screen.route}")
                            .semantics {
                                role = Role.Tab
                                selected = on
                                contentDescription = screen.title
                                onClick {
                                    onNavigate(screen.route)
                                    true
                                }
                            }
                    )
                }
            }
        }

        if (holding) {
            val bubbleWidth = tabWidth + 14.dp
            val bubblePx = with(density) { bubbleWidth.toPx() }
            val lift = with(density) { 5.dp.toPx() }
            val left = (holdX - bubblePx / 2f).coerceIn(
                -8f * density.density,
                widthPx - bubblePx + 8f * density.density
            )
            val bubbleShape = GlassShapes.capsule
            val lensBackdrop = if (canRenderLive && backdrop != null) {
                rememberCombinedBackdrop(backdrop, exported)
            } else {
                null
            }
            val bubble = Modifier
                .align(AbsoluteAlignment.TopLeft)
                .absoluteOffset { IntOffset(left.roundToInt(), -lift.roundToInt()) }
                .size(bubbleWidth, pillHeight + 10.dp)

            Box(
                bubble.then(
                    if (lensBackdrop != null && isGlassLensSupported()) {
                        Modifier.drawBackdrop(
                            backdrop = lensBackdrop,
                            shape = { bubbleShape },
                            effects = { lens(12.dp.toPx(), 24.dp.toPx(), chromaticAberration = true) },
                            highlight = { Highlight(width = 1.dp, alpha = 1f) },
                            shadow = { Shadow(radius = 22.dp, offset = DpOffset(0.dp, 8.dp), color = Color.Black.copy(alpha = 0.2f)) },
                            onDrawBackdrop = { drawIt -> scale(1.18f, 1.18f, center) { drawIt() } },
                            onDrawSurface = { drawRect(Color.White.copy(alpha = if (c.darkBackdrop) 0.05f else 0.14f)) }
                        )
                    } else {
                        Modifier.glassChrome(c, bubbleShape, backdrop = null, surface = c.thumb)
                    }
                )
            )
            Box(bubble.clip(bubbleShape)) {
                Row(
                    Modifier
                        .wrapContentSize(AbsoluteAlignment.TopLeft, unbounded = true)
                        .absoluteOffset { IntOffset((-left).roundToInt(), lift.roundToInt()) }
                        .size(pillWidth, pillHeight)
                        .graphicsLayer {
                            scaleX = 1.18f
                            scaleY = 1.18f
                            transformOrigin = TransformOrigin((holdX / widthPx).coerceIn(0f, 1f), 0.5f)
                        }
                        .padding(pad)
                ) {
                    tabs.forEachIndexed { index, screen ->
                        LiquidTabCell(
                            screen = screen,
                            selected = index == hoverIndex,
                            color = if (index == hoverIndex) accent else c.label
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.LiquidTabCell(
    screen: com.example.ui.navigation.Screen,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .weight(1f)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
    ) {
        Icon(
            if (selected) screen.selectedIcon else screen.unselectedIcon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(23.dp)
        )
        Text(
            screen.title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LiquidTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    endAction: (@Composable () -> Unit)? = null,
    subtitle: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .liquidGlass(
                    shape = RoundedCornerShape(28.dp),
                    elevation = 6.dp,
                    tintAlpha = if (LiquidTheme.isDark) 0.38f else 0.48f,
                    borderAlpha = if (LiquidTheme.isDark) 0.75f else 0.85f,
                    refraction = true
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier
                    .wrapContentWidth()
                    .defaultMinSize(minWidth = 44.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (navigationIcon != null) navigationIcon()
            }

            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                    fontWeight = FontWeight.Bold,
                    color = LiquidTheme.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = LiquidTheme.accentColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            Row(
                Modifier
                    .wrapContentWidth()
                    .defaultMinSize(minWidth = 44.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (actions != null) actions()
                if (endAction != null) endAction()
            }
        }
    }
}

@Composable
fun LiquidAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = null,
    isDestructive: Boolean = false,
    content: (@Composable () -> Unit)? = null
) {
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable { onDismissRequest() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .width(320.dp)
                    .clickable(enabled = false) {}
                    .liquidGlass(
                        useBackdrop = false,
                        shape = RoundedCornerShape(26.dp),
                        tintAlpha = if (LiquidTheme.isDark) 0.92f else 0.96f,
                        elevation = 16.dp
                    )
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = LiquidTheme.textPrimary
                        )
                        if (message.isNotBlank()) {
                            Text(
                                message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = LiquidTheme.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (content != null) {
                            Spacer(Modifier.height(4.dp))
                            content()
                        }
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(LiquidTheme.dividerColor)
                    )

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        if (dismissText != null) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { onDismissRequest() },
                                Alignment.Center
                            ) {
                                Text(
                                    dismissText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = LiquidTheme.textSecondary
                                )
                            }
                            Box(
                                Modifier
                                    .width(0.5.dp)
                                    .fillMaxHeight()
                                    .background(LiquidTheme.dividerColor)
                            )
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    onConfirm()
                                    onDismissRequest()
                                },
                            Alignment.Center
                        ) {
                            Text(
                                confirmText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDestructive) SemanticError else LiquidTheme.accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Liquid Time Picker Dialog:
 * - 1-by-1 change with vertical finger drag gesture and haptic feedback
 * - Stepper buttons (+1 / -1)
 * - Beautiful Liquid Glass styling
 */
@Composable
fun LiquidTimePickerDialog(
    initialTime: String,
    title: String = "تنظیم ساعت",
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = PersianNumberHelper.toEnglishDigits(initialTime.trim()).split(":")
    val initialH = (parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 8).coerceIn(0, 23)
    val initialM = (parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0).coerceIn(0, 59)

    var hour by remember { mutableIntStateOf(initialH) }
    var minute by remember { mutableIntStateOf(initialM) }
    val haptic = LocalHapticFeedback.current

    LiquidAlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        message = "با کشیدن انگشت به بالا/پایین یا دکمه‌ها، ساعت را تنظیم کنید",
        confirmText = "تایید",
        onConfirm = {
            val formatted = String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
            onConfirm(formatted)
        },
        dismissText = "انصراف"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minute column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = {
                        minute = (minute + 1) % 60
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, "افزایش دقیقه", tint = LiquidTheme.accentColor)
                }

                var dragAccumulatorM by remember { mutableFloatStateOf(0f) }
                Box(
                    modifier = Modifier
                        .size(width = 68.dp, height = 62.dp)
                        .liquidGlass(shape = RoundedCornerShape(16.dp), elevation = 4.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = { dragAccumulatorM = 0f },
                                onDragCancel = { dragAccumulatorM = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    dragAccumulatorM += dragAmount
                                    if (dragAccumulatorM <= -24f) {
                                        minute = (minute + 1) % 60
                                        dragAccumulatorM = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else if (dragAccumulatorM >= 24f) {
                                        minute = if (minute == 0) 59 else minute - 1
                                        dragAccumulatorM = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        PersianNumberHelper.toPersianDigits(String.format(java.util.Locale.US, "%02d", minute)),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = LiquidTheme.textPrimary
                    )
                }

                IconButton(
                    onClick = {
                        minute = if (minute == 0) 59 else minute - 1
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, "کاهش دقیقه", tint = LiquidTheme.accentColor)
                }
                Text("دقیقه", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
            }

            Text(
                ":",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = LiquidTheme.accentColor,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            )

            // Hour column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = {
                        hour = (hour + 1) % 24
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, "افزایش ساعت", tint = LiquidTheme.accentColor)
                }

                var dragAccumulatorH by remember { mutableFloatStateOf(0f) }
                Box(
                    modifier = Modifier
                        .size(width = 68.dp, height = 62.dp)
                        .liquidGlass(shape = RoundedCornerShape(16.dp), elevation = 4.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = { dragAccumulatorH = 0f },
                                onDragCancel = { dragAccumulatorH = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    dragAccumulatorH += dragAmount
                                    if (dragAccumulatorH <= -24f) {
                                        hour = (hour + 1) % 24
                                        dragAccumulatorH = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else if (dragAccumulatorH >= 24f) {
                                        hour = if (hour == 0) 23 else hour - 1
                                        dragAccumulatorH = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        PersianNumberHelper.toPersianDigits(String.format(java.util.Locale.US, "%02d", hour)),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = LiquidTheme.textPrimary
                    )
                }

                IconButton(
                    onClick = {
                        hour = if (hour == 0) 23 else hour - 1
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, "کاهش ساعت", tint = LiquidTheme.accentColor)
                }
                Text("ساعت", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
            }
        }
    }
}

// ==========================================
// 5. REST TIMER DOCK & EMPTY STATES
// ==========================================

@Composable
fun LiquidRestTimerDock(
    remainingSeconds: Int,
    isActive: Boolean,
    onAdd15Seconds: () -> Unit,
    onCancel: () -> Unit,
    onClickDock: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalGlassChrome.current?.combined
) {
    AnimatedVisibility(
        visible = isActive,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        LiquidGlassBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable { onClickDock() },
            backdrop = backdrop,
            refraction = true,
            shape = RoundedCornerShape(26.dp),
            elevation = 10.dp
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Timer,
                        null,
                        tint = LiquidTheme.accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            "استراحت ست فعال",
                            style = MaterialTheme.typography.labelSmall,
                            color = LiquidTheme.textSecondary
                        )
                        Text(
                            PersianNumberHelper.formatSecondsToTime(remainingSeconds.toLong()),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LiquidTheme.accentColor
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .height(34.dp)
                            .liquidTapEffect { onAdd15Seconds() },
                        shape = CircleShape,
                        color = LiquidTheme.accentColor.copy(alpha = 0.16f)
                    ) {
                        Box(
                            Modifier.padding(horizontal = 12.dp),
                            Alignment.Center
                        ) {
                            Text(
                                "+۱۵ث",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = LiquidTheme.accentColor
                            )
                        }
                    }
                    IconButton(onClick = onCancel, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.Close, "انصراف", tint = LiquidTheme.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    SolidGroupedCard(modifier = modifier.fillMaxWidth().padding(16.dp, 8.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(LiquidTheme.accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = LiquidTheme.accentColor,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LiquidTheme.textPrimary
            )
            if (!description.isNullOrBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = LiquidTheme.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
            if (actionButtonText != null && onActionClick != null) {
                Spacer(Modifier.height(4.dp))
                LiquidPrimaryButton(
                    text = actionButtonText,
                    onClick = onActionClick,
                    modifier = Modifier.widthIn(max = 220.dp)
                )
            }
        }
    }
}
