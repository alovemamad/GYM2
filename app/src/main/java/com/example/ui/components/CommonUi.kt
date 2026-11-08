package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import com.example.ui.theme.*
import com.example.util.JalaliCalendar
import com.example.util.PersianNumberHelper

// ==========================================
// 1. LIQUID GLASS CONTAINER & MODIFIER
// ==========================================

/** بلور واقعی فقط روی API 31+ کار می‌کند؛ پایین‌تر از آن لایه شیشه کدرتر می‌شود تا خوانایی حفظ شود. */
private val supportsBlur: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** گرادیان سفید روی لبه: نور از بالا-چپ می‌تابد و به سمت پایین-راست محو می‌شود (همان inset highlight در CSS). */
fun glassSpecularBrush(strength: Float = 0.5f): Brush = Brush.linearGradient(
    0f to Color.White.copy(alpha = strength.coerceIn(0f, 1f)),
    0.45f to Color.White.copy(alpha = 0.05f),
    1f to Color.White.copy(alpha = (strength * 0.35f).coerceIn(0f, 1f))
)

/** پس‌زمینه‌ی رنگی ملایم پشت صفحه؛ شیشه روی زمینه‌ی تخت خوب دیده نمی‌شود. */
@Composable
fun Modifier.liquidBackdrop(): Modifier {
    val isDark = LiquidTheme.isDark
    val base = LiquidTheme.backgroundNeutral
    val accent = LiquidTheme.accentColor
    val a1 = accent.copy(alpha = if (isDark) 0.28f else 0.22f)
    val a2 = LiquidTheme.palette.gradientEnd.copy(alpha = if (isDark) 0.30f else 0.12f)
    return this.drawBehind {
        drawRect(base)
        drawRect(
            Brush.radialGradient(
                colors = listOf(a1, Color.Transparent),
                center = Offset(size.width * 0.15f, size.height * 0.08f),
                radius = size.maxDimension * 0.6f
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(a2, Color.Transparent),
                center = Offset(size.width * 0.95f, size.height * 0.85f),
                radius = size.maxDimension * 0.55f
            )
        )
    }
}

@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(28.dp),
    tintAlpha: Float? = null,
    borderAlpha: Float = 0.25f,
    elevation: Dp = 6.dp,
    baseAlpha: Float? = null
): Modifier {
    val isDark = LiquidTheme.isDark
    val baseAlpha = baseAlpha ?: if (supportsBlur) 0.55f else 0.72f
    val baseBg = (if (isDark) GlassBaseDark else GlassBaseLight).copy(alpha = baseAlpha)
    val tint = Color.White.copy(alpha = tintAlpha ?: if (isDark) 0.08f else 0.65f)
    val tintBrush = Brush.verticalGradient(listOf(tint, tint.copy(alpha = tint.alpha * 0.55f)))
    val highlight = if (isDark) borderAlpha else (borderAlpha + 0.5f).coerceAtMost(0.95f)
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.10f)

    return this
        .shadow(elevation = elevation, shape = shape, spotColor = shadowColor, ambientColor = shadowColor)
        .clip(shape)
        .background(baseBg)
        .background(tintBrush)
        .border(BorderStroke(1.dp, glassSpecularBrush(highlight)), shape = shape)
}

@Composable
fun LiquidGlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    elevation: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlass(shape = shape, elevation = elevation),
        content = content
    )
}

/**
 * [۱] کارت شیشه‌ای — معادل glass-card.
 * لایه‌ها: filter (بلور ۴dp روی API 31+) ← overlay ← specular ← content
 * توجه: Modifier.blur در Compose «محتوای خودِ همان نود» را بلور می‌کند، نه پشت آن را (برخلاف backdrop-filter).
 * برای همین بلور فقط روی لایه‌ی پس‌زمینه است و متن واضح می‌ماند.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 6.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .liquidTapEffect(enabled = onClick != null) { onClick?.invoke() }
            .liquidGlass(shape = shape, elevation = elevation)
    ) {
        // filter layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(if (supportsBlur) Modifier.blur(4.dp, BlurredEdgeTreatment(shape)) else Modifier)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)
                    )
                )
        )
        // content
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun Modifier.liquidTapEffect(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "liquid_tap_scale"
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
                    currentOnClick()
                }
            )
        }
}

// ==========================================
// 2. SOLID INSET GROUPED LIST & ROWS
// ==========================================

@Composable
fun SolidGroupedCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    // نام تابع برای سازگاری با صفحه‌ها حفظ شده؛ ظاهر آن اکنون شیشه‌ای است
    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = shape, elevation = 6.dp),
        content = content
    )
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
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple()
        ) { onClick() }
    } else Modifier

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(clickableModifier)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (icon != null) {
                    val bg = iconBackground ?: iconTint.copy(alpha = 0.16f)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .border(BorderStroke(1.dp, glassSpecularBrush(0.5f)), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = LiquidTheme.textPrimary
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
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
                        text = valueText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LiquidTheme.textSecondary
                    )
                }

                if (trailingContent != null) {
                    trailingContent()
                } else if (showArrow && onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, // در RTL به سمت چپ اشاره می‌کند
                        contentDescription = null,
                        tint = LiquidTheme.textSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (icon != null) 66.dp else 16.dp)
                    .height(0.6.dp)
                    .background(LiquidTheme.dividerColor)
            )
        }
    }
}

@Composable
fun LiquidSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            color = LiquidTheme.textSecondary,
            fontWeight = FontWeight.SemiBold
        )

        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                color = LiquidTheme.accentColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple()
                    ) { onActionClick() }
                    .padding(horizontal = 10.dp, vertical = 12.dp)
            )
        }
    }
}

// ==========================================
// 3. GRADIENT HERO CARDS & CONTENT ROWS
// ==========================================

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
            .shadow(8.dp, shape = shape, spotColor = Color.Black.copy(alpha = 0.35f)),
        shape = shape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                // لایه تیره ملایم تا متن سفید روی پالت‌های روشن (طلایی، سایبر، ...) خوانا بماند
                .background(Color.Black.copy(alpha = 0.32f))
                .border(
                    BorderStroke(
                        1.dp,
                        glassSpecularBrush(0.6f)
                    ),
                    shape = shape
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                if (content != null) {
                    Spacer(modifier = Modifier.height(4.dp))
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
            modifier = Modifier
                .size(60.dp)
                .liquidGlass(shape = CircleShape, elevation = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
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
            modifier = Modifier
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
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(gradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LiquidTheme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
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
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = LiquidTheme.accentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else if (trailing != null) {
                trailing()
            }
        }
    }
}

// ==========================================
// 4. BUTTONS & INPUT FIELDS
// ==========================================

@Composable
fun LiquidPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val accent = LiquidTheme.accentColor
    val shape = CircleShape
    val fill: Brush = if (enabled) {
        Brush.verticalGradient(listOf(accent.copy(alpha = 0.96f), accent.copy(alpha = 0.78f)))
    } else {
        SolidColor(LiquidTheme.textSecondary.copy(alpha = 0.3f))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .liquidTapEffect(enabled = enabled) { onClick() }
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = shape,
                spotColor = accent.copy(alpha = 0.55f),
                ambientColor = accent.copy(alpha = 0.25f)
            )
            .clip(shape)
            .background(fill)
            .border(BorderStroke(1.dp, glassSpecularBrush(if (enabled) 0.75f else 0.2f)), shape),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) LiquidTheme.onAccent else LiquidTheme.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) LiquidTheme.onAccent else LiquidTheme.textSecondary
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
    val accent = LiquidTheme.accentColor
    val shape = CircleShape
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .liquidTapEffect { onClick() }
            .clip(shape)
            .background(accent.copy(alpha = 0.14f))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)))
            .border(BorderStroke(1.dp, glassSpecularBrush(0.45f)), shape),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

/**
 * [۲] دکمه‌ی شیشه‌ای خنثی — معادل glass-button (گوشه ۱۲dp، انیمیشن اسپرینگی فشردن).
 */
@Composable
fun LiquidGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .liquidTapEffect(enabled = enabled) { onClick() }
            .liquidGlass(shape = RoundedCornerShape(12.dp), elevation = 4.dp)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = LiquidTheme.textPrimary, modifier = Modifier.size(18.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = LiquidTheme.textPrimary
            )
        }
    }
}

@Composable
fun LiquidBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(72.dp)
            .liquidGlass(shape = RoundedCornerShape(36.dp), elevation = 10.dp, baseAlpha = 0.82f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            com.example.ui.navigation.Screen.bottomNavItems.forEach { screen ->
                val selected = currentRoute == screen.route
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            if (selected) {
                                if (LiquidTheme.isDark) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.85f)
                            } else Color.Transparent
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple()
                        ) { onNavigate(screen.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                            contentDescription = screen.title,
                            tint = if (selected) LiquidTheme.accentColor else LiquidTheme.textSecondary,
                            modifier = Modifier.size(23.dp)
                        )
                        Text(
                            text = screen.title,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) LiquidTheme.accentColor else LiquidTheme.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .liquidTapEffect { onClick() },
        shape = CircleShape,
        color = SemanticError.copy(alpha = 0.15f)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SemanticError
            )
        }
    }
}

/** [۷] نوار جستجوی شیشه‌ای — با فوکوس کمی بالا می‌آید (translateY -2dp) و سایه و لبه‌ی accent می‌گیرد. */
@Composable
fun LiquidSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    val lift by animateDpAsState(
        targetValue = if (focused) (-2).dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "search_lift"
    )
    val elevation by animateDpAsState(
        targetValue = if (focused) 10.dp else 4.dp,
        animationSpec = spring(stiffness = 500f),
        label = "search_elevation"
    )
    val focusBorderAlpha by animateFloatAsState(
        targetValue = if (focused) 0.9f else 0f,
        animationSpec = spring(stiffness = 500f),
        label = "search_border"
    )
    val shape = CircleShape

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .offset(y = lift)
            .liquidGlass(shape = shape, elevation = elevation)
            .border(BorderStroke(1.5.dp, LiquidTheme.accentColor.copy(alpha = focusBorderAlpha)), shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = if (focused) LiquidTheme.accentColor else LiquidTheme.textSecondary,
                modifier = Modifier.size(22.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LiquidTheme.textSecondary
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    cursorBrush = SolidColor(LiquidTheme.accentColor),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = LiquidTheme.textPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused }
                )
            }

            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "پاک کردن",
                        tint = LiquidTheme.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (trailingAction != null) {
                trailingAction()
            }
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
    var focused by remember { mutableStateOf(false) }
    val isDark = LiquidTheme.isDark
    val fieldBg by animateColorAsState(
        targetValue = when {
            focused -> if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.07f)
            else -> if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.045f)
        },
        animationSpec = tween(200),
        label = "input_bg"
    )
    val fieldBorder by animateColorAsState(
        targetValue = if (focused) LiquidTheme.accentColor else Color.White.copy(alpha = if (isDark) 0.12f else 0.5f),
        animationSpec = tween(200),
        label = "input_border"
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        color = fieldBg,
        border = BorderStroke(1.dp, fieldBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = LiquidTheme.accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = LiquidTheme.textSecondary
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LiquidTheme.textSecondary.copy(alpha = 0.5f)
                        )
                    }
                    val isNumeric = keyboardOptions.keyboardType == KeyboardType.Number ||
                        keyboardOptions.keyboardType == KeyboardType.Decimal
                    BasicTextField(
                        value = value,
                        // کیبورد فارسی ارقام فارسی تایپ می‌کند؛ برای پردازش به لاتین تبدیل می‌شود
                        onValueChange = { typed ->
                            onValueChange(
                                if (isNumeric) {
                                    PersianNumberHelper.toEnglishDigits(typed).replace('٫', '.').replace(',', '.')
                                } else typed
                            )
                        },
                        singleLine = singleLine,
                        keyboardOptions = keyboardOptions,
                        cursorBrush = SolidColor(LiquidTheme.accentColor),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = LiquidTheme.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focused = it.isFocused }
                    )
                }
            }

            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelMedium,
                    color = LiquidTheme.textSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ==========================================
// 5. CONTROLS (SWITCH, SEGMENT, STEPPER, PROGRESS)
// ==========================================

/** [۱۰] سوییچ شیشه‌ای — ریل شیشه‌ای/accent، دسته‌ی سفید با لبه‌ی نورانی و انیمیشن اسپرینگی. */
@Composable
fun LiquidSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val isDark = LiquidTheme.isDark
    val accent = LiquidTheme.accentColor

    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 28f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "switch_thumb"
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (pressed) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "switch_thumb_scale"
    )
    val trackAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(stiffness = 500f),
        label = "switch_track"
    )

    val trackShape = CircleShape
    val offTrack = if (isDark) Color.White.copy(alpha = 0.16f) else SwitchTrackOff.copy(alpha = 0.32f)

    Box(
        modifier = modifier
            .size(width = 60.dp, height = 32.dp)
            .clip(trackShape)
            .background(offTrack)
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.95f * trackAlpha), accent.copy(alpha = 0.78f * trackAlpha))
                )
            )
            .border(BorderStroke(1.dp, glassSpecularBrush(0.35f)), trackShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch
            ) { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.dp)
                .size(24.dp)
                .graphicsLayer { scaleX = thumbScale; scaleY = thumbScale }
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape)
                .border(BorderStroke(1.dp, glassSpecularBrush(0.9f)), CircleShape)
        )
    }
}

@Composable
fun LiquidSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (LiquidTheme.isDark) DarkCard.copy(alpha = 0.75f) else LightCardElevated.copy(alpha = 0.85f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected) {
                                if (LiquidTheme.isDark) DarkCardElevated else LightCard
                            } else Color.Transparent
                        )
                        .clickable { onSelectIndex(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) LiquidTheme.textPrimary else LiquidTheme.textSecondary,
                        maxLines = 1
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
    val cardBg = if (LiquidTheme.isDark) DarkCardElevated else LightCardElevated
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                val newVal = (value - step).coerceAtLeast(min)
                onValueChange(newVal)
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "کاهش", tint = LiquidTheme.textPrimary)
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .widthIn(min = 50.dp),
            contentAlignment = Alignment.Center
        ) {
            val text = if (step >= 1.0 && value % 1.0 == 0.0) {
                PersianNumberHelper.toPersianDigits(value.toInt())
            } else {
                PersianNumberHelper.toPersianDigits(String.format(formatString, value))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LiquidTheme.textPrimary
            )
        }

        IconButton(
            onClick = {
                val newVal = (value + step).coerceAtMost(max)
                onValueChange(newVal)
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "افزایش", tint = LiquidTheme.textPrimary)
        }
    }
}

@Composable
fun LiquidProgressRing(
    progressPercent: Int,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    ringColor: Color = LiquidTheme.accentColor,
    trackColor: Color = LiquidTheme.accentColor.copy(alpha = 0.16f),
    centerContent: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )

            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Sweep Progress
            val sweep = (progressPercent.coerceIn(0, 100) / 100f) * 360f
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        centerContent()
    }
}

// ==========================================
// 6. TOP BAR & BOTTOM BAR (LIQUID GLASS)
// ==========================================

@Composable
fun LiquidCircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    badgeCount: Int? = null
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .liquidGlass(shape = CircleShape, elevation = 3.dp)
            .liquidTapEffect { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = LiquidTheme.textPrimary,
            modifier = Modifier.size(22.dp)
        )
        if (badgeCount != null && badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(12.dp)
                    .background(SemanticError, CircleShape)
            )
        }
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
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(shape = RoundedCornerShape(24.dp), elevation = 4.dp, baseAlpha = 0.7f)
                .height(52.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.wrapContentWidth().defaultMinSize(minWidth = 44.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (navigationIcon != null) navigationIcon()
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = LiquidTheme.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = LiquidTheme.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier.wrapContentWidth().defaultMinSize(minWidth = 44.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (actions != null) actions()
                if (endAction != null) endAction()
            }
        }
    }
}

// ==========================================
// 7. DIALOGS & BOTTOM SHEETS (LIQUID GLASS)
// ==========================================

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
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismissRequest() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(310.dp)
                    .clickable(enabled = false) {}
                    .liquidGlass(shape = RoundedCornerShape(28.dp), elevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                            fontWeight = FontWeight.Bold,
                            color = LiquidTheme.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        if (message.isNotBlank()) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                color = LiquidTheme.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (content != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            content()
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.6.dp)
                            .background(LiquidTheme.dividerColor)
                    )

                    // Action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (dismissText != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { onDismissRequest() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dismissText,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                    fontWeight = FontWeight.Medium,
                                    color = LiquidTheme.textSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(0.6.dp)
                                    .fillMaxHeight()
                                    .background(LiquidTheme.dividerColor)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    onConfirm()
                                    onDismissRequest()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = confirmText,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
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

@Composable
fun LiquidRestTimerDock(
    remainingSeconds: Int,
    isActive: Boolean,
    onAdd15Seconds: () -> Unit,
    onCancel: () -> Unit,
    onClickDock: () -> Unit,
    modifier: Modifier = Modifier
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
            shape = RoundedCornerShape(26.dp),
            elevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LiquidProgressRing(
                        progressPercent = ((remainingSeconds % 60) * 100) / 60,
                        modifier = Modifier.size(38.dp),
                        strokeWidth = 4.dp
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = LiquidTheme.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "استراحت ست فعال",
                            style = MaterialTheme.typography.labelSmall,
                            color = LiquidTheme.textSecondary
                        )
                        Text(
                            text = PersianNumberHelper.formatSecondsToTime(remainingSeconds.toLong()),
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
                            .height(44.dp)
                            .liquidTapEffect { onAdd15Seconds() },
                        shape = CircleShape,
                        color = LiquidTheme.accentColor.copy(alpha = 0.16f)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+۱۵ث",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = LiquidTheme.accentColor
                            )
                        }
                    }

                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "انصراف",
                            tint = LiquidTheme.textSecondary
                        )
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
    SolidGroupedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(LiquidTheme.accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LiquidTheme.accentColor,
                    modifier = Modifier.size(30.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LiquidTheme.textPrimary
            )

            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = LiquidTheme.textSecondary,
                    textAlign = TextAlign.Center
                )
            }

            if (actionButtonText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                LiquidPrimaryButton(
                    text = actionButtonText,
                    onClick = onActionClick,
                    modifier = Modifier.widthIn(max = 220.dp)
                )
            }
        }
    }
}

@Composable
fun LiquidTimePickerDialog(
    initialTime: String,
    title: String = "انتخاب ساعت اعلان",
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initialTime.split(":").mapNotNull { it.trim().toIntOrNull() }
    var selectedHour by remember { mutableIntStateOf(parts.getOrElse(0) { 18 }.coerceIn(0, 23)) }
    var selectedMinute by remember { mutableIntStateOf(parts.getOrElse(1) { 0 }.coerceIn(0, 59)) }
    val haptic = LocalHapticFeedback.current

    var hourDragAccumulator by remember { mutableFloatStateOf(0f) }
    var minuteDragAccumulator by remember { mutableFloatStateOf(0f) }

    LiquidAlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        message = "",
        confirmText = "تأیید و ذخیره",
        dismissText = "انصراف",
        onConfirm = {
            val formatted = String.format(java.util.Locale.US, "%02d:%02d", selectedHour, selectedMinute)
            onConfirm(formatted)
        },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "برای تغییر دانه‌دانه، انگشت خود را روی عدد به بالا یا پایین بکشید",
                    style = MaterialTheme.typography.labelSmall,
                    color = LiquidTheme.textSecondary
                )

                // Big Display with vertical drag gesture
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = LiquidTheme.surfaceElevated
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour column
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(LiquidTheme.surfaceSolid.copy(alpha = 0.5f))
                                .padding(vertical = 8.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragEnd = { hourDragAccumulator = 0f },
                                        onDragCancel = { hourDragAccumulator = 0f },
                                        onVerticalDrag = { _, dragAmount ->
                                            hourDragAccumulator += dragAmount
                                            val threshold = 28f
                                            if (hourDragAccumulator <= -threshold) {
                                                // Swipe UP increases hour by 1
                                                selectedHour = (selectedHour + 1) % 24
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                hourDragAccumulator = 0f
                                            } else if (hourDragAccumulator >= threshold) {
                                                // Swipe DOWN decreases hour by 1
                                                selectedHour = if (selectedHour == 0) 23 else selectedHour - 1
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                hourDragAccumulator = 0f
                                            }
                                        }
                                    )
                                }
                        ) {
                            IconButton(
                                onClick = {
                                    selectedHour = (selectedHour + 1) % 24
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "افزایش ۱ ساعت", tint = LiquidTheme.accentColor)
                            }
                            Text(
                                text = PersianNumberHelper.toPersianDigits(String.format(java.util.Locale.US, "%02d", selectedHour)),
                                style = MaterialTheme.typography.displayMedium.copy(fontSize = 38.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = LiquidTheme.accentColor
                            )
                            IconButton(
                                onClick = {
                                    selectedHour = if (selectedHour == 0) 23 else selectedHour - 1
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "کاهش ۱ ساعت", tint = LiquidTheme.accentColor)
                            }
                            Text("ساعت (بکشید)", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                        }

                        Text(
                            text = ":",
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 38.sp),
                            fontWeight = FontWeight.Bold,
                            color = LiquidTheme.textSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        // Minute column
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(LiquidTheme.surfaceSolid.copy(alpha = 0.5f))
                                .padding(vertical = 8.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragEnd = { minuteDragAccumulator = 0f },
                                        onDragCancel = { minuteDragAccumulator = 0f },
                                        onVerticalDrag = { _, dragAmount ->
                                            minuteDragAccumulator += dragAmount
                                            val threshold = 22f
                                            if (minuteDragAccumulator <= -threshold) {
                                                // Swipe UP increases minute by 1
                                                selectedMinute = (selectedMinute + 1) % 60
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                minuteDragAccumulator = 0f
                                            } else if (minuteDragAccumulator >= threshold) {
                                                // Swipe DOWN decreases minute by 1
                                                selectedMinute = if (selectedMinute == 0) 59 else selectedMinute - 1
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                minuteDragAccumulator = 0f
                                            }
                                        }
                                    )
                                }
                        ) {
                            IconButton(
                                onClick = {
                                    selectedMinute = (selectedMinute + 1) % 60
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "افزایش ۱ دقیقه", tint = LiquidTheme.accentColor)
                            }
                            Text(
                                text = PersianNumberHelper.toPersianDigits(String.format(java.util.Locale.US, "%02d", selectedMinute)),
                                style = MaterialTheme.typography.displayMedium.copy(fontSize = 38.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = LiquidTheme.textPrimary
                            )
                            IconButton(
                                onClick = {
                                    selectedMinute = if (selectedMinute == 0) 59 else selectedMinute - 1
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "کاهش ۱ دقیقه", tint = LiquidTheme.accentColor)
                            }
                            Text("دقیقه (بکشید)", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                        }
                    }
                }

                // Quick presets
                Text(
                    text = "ساعت‌های پیشنهادی سریع:",
                    style = MaterialTheme.typography.labelSmall,
                    color = LiquidTheme.textSecondary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "صبح" to (8 to 0),
                        "ظهر" to (13 to 0),
                        "عصر" to (17 to 30),
                        "شب" to (21 to 0)
                    )
                    presets.forEach { (label, time) ->
                        val isCurr = selectedHour == time.first && selectedMinute == time.second
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .liquidTapEffect {
                                    selectedHour = time.first
                                    selectedMinute = time.second
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurr) LiquidTheme.accentColor else LiquidTheme.surfaceElevated
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isCurr) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurr) LiquidTheme.onAccent else LiquidTheme.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

// ==========================================
// 9. NEW GLASS COMPONENTS (DROPDOWN, SPINNER, SIDEBAR, ICON)
// ==========================================

/**
 * [۳] دراپ‌داون شیشه‌ای — معادل glass-dropdown (باز/بسته شدن با max-height در CSS ← expandVertically).
 *
 * LiquidDropdown(title = "گروه عضلانی") {
 *     LiquidDropdownItem("سینه", selected = sel == 0) { sel = 0 }
 *     LiquidDropdownItem("پشت", selected = sel == 1) { sel = 1 }
 * }
 */
@Composable
fun LiquidDropdown(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    leadingIcon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "dropdown_arrow"
    )
    val shape = RoundedCornerShape(16.dp)

    Column(modifier = modifier.fillMaxWidth().liquidGlass(shape = shape, elevation = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    role = Role.DropdownList
                ) { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = LiquidTheme.accentColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = LiquidTheme.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = LiquidTheme.textSecondary,
                modifier = Modifier.rotate(arrowRotation)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(stiffness = 400f)) + fadeIn(),
            exit = shrinkVertically(spring(stiffness = 500f)) + fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), content = content)
        }
    }
}

@Composable
fun LiquidDropdownItem(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) LiquidTheme.accentColor.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) LiquidTheme.accentColor else LiquidTheme.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = LiquidTheme.accentColor, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * [۹] اسپینر شیشه‌ای — حلقه‌ی چرخان (spin، ۱ ثانیه) + هسته‌ی ضربان‌دار (pulse) روی دایره‌ی شیشه‌ای.
 */
@Composable
fun LiquidSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = Color.Unspecified
) {
    val tint = if (color == Color.Unspecified) LiquidTheme.accentColor else color
    val transition = rememberInfiniteTransition(label = "liquid_spinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "spin"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .size(size)
            .liquidGlass(shape = CircleShape, elevation = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.14f)) {
            val sw = this.size.minDimension * 0.09f
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            val topLeft = Offset(sw / 2f, sw / 2f)
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = (this.size.minDimension - sw) / 2f,
                style = Stroke(width = sw)
            )
            rotate(rotation) {
                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = sw, cap = StrokeCap.Round)
                )
            }
        }
        Box(
            modifier = Modifier
                .size(size * 0.22f)
                .graphicsLayer { scaleX = pulse; scaleY = pulse; alpha = pulse }
                .background(tint, CircleShape)
        )
    }
}

/** [۸] سایدبار شیشه‌ای — آیتم فعال با پس‌زمینه‌ی سفید ۱۰٪ و انتقال نرم ۴۰۰ms. */
data class LiquidSidebarItem(val title: String, val icon: ImageVector)

@Composable
fun LiquidGlassSidebar(
    title: String,
    items: List<LiquidSidebarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .liquidGlass(shape = RoundedCornerShape(20.dp))
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = LiquidTheme.textPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(
                targetValue = if (selected) Color.White.copy(alpha = if (LiquidTheme.isDark) 0.10f else 0.7f) else Color.Transparent,
                animationSpec = tween(400),
                label = "sidebar_item_bg"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple()
                    ) { onSelect(index) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = if (selected) LiquidTheme.accentColor else LiquidTheme.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) LiquidTheme.textPrimary else LiquidTheme.textSecondary
                )
            }
        }
    }
}

/** [۵] آیکون شیشه‌ای ۶۴dp با گوشه‌ی ۱۶dp — معادل glass-icon. اگر onClick داده شود، فشردن اسپرینگی دارد. */
@Composable
fun LiquidGlassIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    tint: Color = Color.Unspecified,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .liquidTapEffect(enabled = onClick != null) { onClick?.invoke() }
            .liquidGlass(shape = RoundedCornerShape(size * 0.25f), elevation = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (tint == Color.Unspecified) LiquidTheme.accentColor else tint,
            modifier = Modifier.size(size * 0.45f)
        )
    }
}
