package com.example.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.PointerEventPass
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.*
import com.example.util.PersianNumberHelper

// Haze (iOS blur integration)
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle

// CompositionLocal for HazeState across the entire app
val LocalHazeState = compositionLocalOf<HazeState?> { null }

// ==========================================
// 1. LIQUID GLASS BASE MODIFIERS & ROOT
// ==========================================

/**
 * شکست نور مایع (AGSL، اندروید ۱۳+): امواج آرام روی تصویر پس‌زمینه + لنز بزرگ‌نما زیر انگشت.
 * شیشه‌ها این تصویر را بلور می‌کنند و در نتیجه درون کارت‌ها «مایع» جاری دیده می‌شود.
 */
@RequiresApi(33)
private class LiquidRefraction {
    private val shader = android.graphics.RuntimeShader(
        """
        uniform shader content;
        uniform float2 touch;
        uniform float strength;
        uniform float radius;
        uniform float time;
        half4 main(float2 p) {
            float2 q = p;
            q.x += sin(p.y * 0.012 + time) * 7.0 + sin(p.y * 0.031 - time * 2.0) * 3.0;
            q.y += cos(p.x * 0.011 + time) * 7.0 + cos(p.x * 0.027 + time * 2.0) * 3.0;
            float2 d = q - touch;
            float r = length(d);
            if (strength > 0.001 && r < radius) {
                float t = r / radius;
                float k = 1.0 - strength * 0.5 * (1.0 - t * t);
                q = touch + d * k;
            }
            return content.eval(q);
        }
        """.trimIndent()
    )

    fun effect(tx: Float, ty: Float, strength: Float, radius: Float, time: Float): RenderEffect {
        shader.setFloatUniform("touch", tx, ty)
        shader.setFloatUniform("strength", strength)
        shader.setFloatUniform("radius", radius)
        shader.setFloatUniform("time", time)
        return android.graphics.RenderEffect
            .createRuntimeShaderEffect(shader, "content")
            .asComposeRenderEffect()
    }
}

// فاز زمانی مشترک برای حرکت آرام نور و جلوه‌های مایع (۰..۱ تکرارشونده)
val LocalGlassPhase = compositionLocalOf<State<Float>> { mutableStateOf(0f) }

/**
 * Root container: botanical backdrop (slow liquid drift), palette-tinted atmosphere,
 * and a touch-following "liquid lens" like the reference videos.
 */
@Composable
fun LiquidGlassRoot(
    hazeState: HazeState = remember { HazeState() },
    content: @Composable () -> Unit
) {
    val isDark = LiquidTheme.isDark
    val accent = LiquidTheme.accentColor
    val palette = LiquidTheme.palette
    val bgTint = if (isDark) Color.Black.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.12f)

    val transition = rememberInfiniteTransition(label = "glass_phase")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "phase"
    )

    val refraction: Any? = remember {
        if (Build.VERSION.SDK_INT >= 33) {
            try { LiquidRefraction() } catch (_: Throwable) { null }
        } else null
    }

    // Touch lens state
    var touch by remember { mutableStateOf(Offset(0f, 0f)) }
    var touching by remember { mutableStateOf(false) }
    val lensPos by animateOffsetAsState(
        targetValue = touch,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f),
        label = "lens_pos"
    )
    val lensAlpha by animateFloatAsState(
        targetValue = if (touching) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 260f),
        label = "lens_alpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_liquid_glass_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val t = phase.value * 2f * PI.toFloat()
                        val sc = 1.14f
                        scaleX = sc
                        scaleY = sc
                        translationX = sin(t) * 18f
                        translationY = cos(t) * 12f
                        if (Build.VERSION.SDK_INT >= 33 && refraction != null) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val lx = (lensPos.x - cx) / sc + cx
                            val ly = (lensPos.y - cy) / sc + cy
                            renderEffect = (refraction as LiquidRefraction).effect(
                                lx, ly, lensAlpha,
                                52.dp.toPx() / sc,
                                phase.value * 6f * PI.toFloat()
                            )
                        }
                    }
            )
            Box(modifier = Modifier.fillMaxSize().background(bgTint))
            // رنگ پالت انتخابی روی کل فضای برنامه می‌نشیند
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accent.copy(alpha = if (isDark) 0.30f else 0.22f),
                                Color.Transparent,
                                palette.gradientEnd.copy(alpha = if (isDark) 0.30f else 0.14f)
                            )
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressed = event.changes.firstOrNull { it.pressed }
                            if (pressed != null) {
                                touch = pressed.position
                                touching = true
                            } else {
                                touching = false
                            }
                        }
                    }
                }
                .drawWithContent {
                    drawContent()
                    if (lensAlpha > 0.01f) {
                        val r = 52.dp.toPx() * (0.9f + 0.1f * lensAlpha)
                        // refraction-like body
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.02f * lensAlpha),
                                    Color.White.copy(alpha = 0.10f * lensAlpha),
                                    Color.White.copy(alpha = 0.26f * lensAlpha)
                                ),
                                center = lensPos,
                                radius = r
                            ),
                            radius = r,
                            center = lensPos
                        )
                        // specular rim
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.85f * lensAlpha),
                                    Color.White.copy(alpha = 0.12f * lensAlpha),
                                    Color.White.copy(alpha = 0.55f * lensAlpha),
                                    Color.White.copy(alpha = 0.12f * lensAlpha),
                                    Color.White.copy(alpha = 0.85f * lensAlpha)
                                ),
                                center = lensPos
                            ),
                            radius = r,
                            center = lensPos,
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                        // inner touch dot
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f * lensAlpha),
                            radius = 4.5.dp.toPx(),
                            center = lensPos
                        )
                    }
                }
        ) {
            CompositionLocalProvider(
                LocalHazeState provides hazeState,
                LocalGlassPhase provides phase
            ) {
                content()
            }
        }
    }
}

/** ورود نرم بدون شفافیت صفر (تا هیچ‌وقت فریم خالی دیده نشود): فقط کمی بالا آمدن و بزرگ شدن فنری. */
@Composable
fun Modifier.liquidAppear(delayMs: Int = 0): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        progress.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 320f))
    }
    return this.graphicsLayer {
        val p = progress.value
        translationY = (1f - p) * 18f
        val sc = 0.975f + 0.025f * p
        scaleX = sc
        scaleY = sc
    }
}

/**
 * Primary Liquid Glass modifier based on Snipzy CSS liquid glass design:
 * - Backdrop blur with Haze (16dp)
 * - Translucent overlay tint (--bg-color: rgba(255, 255, 255, 0.22) / rgba(0, 0, 0, 0.25))
 * - Specular highlight gradient border (inset 1px 1px 1px highlight: 0.85 alpha on top-left)
 * - Inset vertical reflection sheen
 * - Soft ambient shadow (box-shadow: 0 6px 24px rgba(0, 0, 0, 0.35))
 */
@Composable
fun Modifier.liquidGlass(
    hazeState: HazeState? = LocalHazeState.current,
    shape: Shape = RoundedCornerShape(22.dp),
    tintAlpha: Float = if (LiquidTheme.isDark) 0.18f else 0.24f,
    elevation: Dp = 8.dp,
    borderAlpha: Float = if (LiquidTheme.isDark) 0.80f else 0.90f,
    accentTintAlpha: Float = 0.09f
): Modifier {
    val isDark = LiquidTheme.isDark
    val accent = LiquidTheme.accentColor
    val phase = LocalGlassPhase.current
    val bgColor = if (isDark) Color(0xFF0A120D).copy(alpha = tintAlpha) else Color.White.copy(alpha = tintAlpha)

    // Specular border gradient: bright white top-left highlight fading to subtle translucent edge
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = borderAlpha),
            Color.White.copy(alpha = borderAlpha * 0.35f),
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.10f),
            Color.White.copy(alpha = borderAlpha * 0.38f)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // Inner glossy reflection sheen
    val innerSpecularBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.03f),
            Color.Transparent
        ),
        startY = 0f,
        endY = 100f
    )

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            spotColor = Color.Black.copy(alpha = 0.35f),
            ambientColor = Color.Black.copy(alpha = 0.20f)
        )
        .clip(shape)
        .then(
            if (hazeState != null) {
                Modifier.hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = bgColor,
                        tints = emptyList(),
                        blurRadius = 16.dp,
                        noiseFactor = 0.04f
                    )
                )
            } else {
                Modifier.background(bgColor)
            }
        )
        .drawBehind {
            // رنگ پالت روی شیشه
            drawRect(accent.copy(alpha = accentTintAlpha))
            // برق نوری که آرام روی شیشه می‌لغزد
            val w = size.width
            val h = size.height
            val x = (phase.value * 2.4f - 0.7f) * (w + h)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.11f), Color.Transparent),
                    start = Offset(x - h, 0f),
                    end = Offset(x, h)
                )
            )
        }
        .background(innerSpecularBrush)
        .border(BorderStroke(1.2.dp, borderBrush), shape = shape)
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
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 420f),
        label = "liquid_tap"
    )

    return if (!enabled) this else this
        .graphicsLayer {
            // حالت ژله‌ای: موقع لمس کمی پهن‌تر و کوتاه‌تر می‌شود
            scaleX = 1f + (1f - scale) * 0.6f
            scaleY = scale
        }
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
    hazeState: HazeState? = LocalHazeState.current,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .liquidAppear()
            .fillMaxWidth()
            .liquidGlass(hazeState = hazeState, shape = shape, elevation = 6.dp)
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
    hazeState: HazeState? = LocalHazeState.current,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlass(hazeState = hazeState, shape = shape, elevation = elevation),
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

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    enabled = onClick != null
                ) { onClick?.invoke() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBackground ?: iconTint.copy(alpha = 0.16f))
                            .border(
                                0.5.dp,
                                Color.White.copy(alpha = 0.25f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
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
                    .background(Color.White.copy(alpha = if (LiquidTheme.isDark) 0.08f else 0.15f))
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
                .liquidGlass(shape = CircleShape, elevation = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = iconTint, modifier = Modifier.size(26.dp))
        }
        Text(
            label,
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .liquidGlass(
                shape = CircleShape,
                elevation = if (isPressed) 2.dp else 8.dp,
                tintAlpha = 0.20f,
                borderAlpha = 0.90f,
                accentTintAlpha = 0.0f
            )
            .background(
                Brush.verticalGradient(
                    listOf(
                        LiquidTheme.accentColor.copy(alpha = if (enabled) 0.88f else 0.35f),
                        LiquidTheme.accentColor.copy(alpha = if (enabled) 0.58f else 0.2f)
                    )
                ),
                CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
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
                    tint = LiquidTheme.onAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LiquidTheme.onAccent
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
                tintAlpha = if (LiquidTheme.isDark) 0.45f else 0.55f
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
    badgeCount: Int? = null
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .liquidGlass(shape = CircleShape, elevation = 4.dp)
            .liquidTapEffect { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription,
            tint = LiquidTheme.textPrimary,
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .liquidGlass(shape = RoundedCornerShape(23.dp), elevation = 4.dp)
            .padding(3.dp)
    ) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) {
                                if (LiquidTheme.isDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.85f)
                            } else Color.Transparent
                        )
                        .border(
                            if (isSelected) 0.5.dp else 0.dp,
                            if (isSelected) Color.White.copy(alpha = 0.4f) else Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onSelectIndex(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) LiquidTheme.accentColor else LiquidTheme.textSecondary
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
                    PersianNumberHelper.toPersianDigits(String.format(formatString, value))
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
            .height(60.dp)
            .liquidGlass(
                shape = RoundedCornerShape(18.dp),
                elevation = if (isFocused) 6.dp else 3.dp,
                borderAlpha = if (isFocused) 0.8f else 0.35f
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
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
 * Snipzy Liquid Glass Toggle Switch:
 * - 54dp x 32dp capsule track with specular border
 * - 26dp sliding thumb with spring animation
 * - Glowing accent on check
 */
@Composable
fun LiquidSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 22f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "switch_thumb"
    )

    val trackColor = if (checked) {
        Color.White.copy(alpha = 0.38f)
    } else {
        Color.Black.copy(alpha = 0.25f)
    }

    val borderColor = if (checked) {
        Color.White.copy(alpha = 0.85f)
    } else {
        Color.White.copy(alpha = 0.45f)
    }

    Box(
        modifier = modifier
            .size(54.dp, 32.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(1.2.dp, borderColor, CircleShape)
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset.dp)
                .size(26.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.White.copy(alpha = 0.95f), CircleShape)
                .border(0.5.dp, Color.White, CircleShape)
        )
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

    Box(
        modifier = modifier
            .size(size)
            .liquidGlass(shape = CircleShape, elevation = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.15f)) {
            val strokeWidth = 3.dp.toPx()
            val canvasSize = this.size.minDimension
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        ringColor.copy(alpha = 0.1f),
                        ringColor.copy(alpha = 0.4f),
                        ringColor,
                        Color.White
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
                        listOf(Color.White, ringColor.copy(alpha = 0.7f))
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
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(72.dp)
            .liquidGlass(
                shape = RoundedCornerShape(36.dp),
                elevation = 12.dp,
                tintAlpha = 0.28f,
                borderAlpha = 0.85f
            )
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            com.example.ui.navigation.Screen.bottomNavItems.forEach { screen ->
                val selected = currentRoute == screen.route
                val selBg by animateColorAsState(
                    if (selected) LiquidTheme.accentColor.copy(alpha = 0.42f) else Color.Transparent,
                    spring(stiffness = 300f), label = "nav_bg"
                )
                val selBorder by animateColorAsState(
                    if (selected) Color.White.copy(alpha = 0.6f) else Color.Transparent,
                    spring(stiffness = 300f), label = "nav_border"
                )
                val iconScale by animateFloatAsState(
                    if (selected) 1.18f else 1f,
                    spring(dampingRatio = 0.45f, stiffness = 380f), label = "nav_scale"
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp))
                        .background(selBg)
                        .border(1.dp, selBorder, RoundedCornerShape(28.dp))
                        .clickable { onNavigate(screen.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (selected) screen.selectedIcon else screen.unselectedIcon,
                            screen.title,
                            tint = if (selected) Color.White else Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(24.dp).scale(iconScale)
                        )
                        Text(
                            screen.title,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }
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
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .liquidGlass(
                    shape = RoundedCornerShape(28.dp),
                    elevation = 6.dp,
                    tintAlpha = 0.24f,
                    borderAlpha = 0.75f
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
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
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
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable { onDismissRequest() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .width(320.dp)
                    .clickable(enabled = false) {}
                    .liquidGlass(shape = RoundedCornerShape(26.dp), elevation = 16.dp)
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
                            .background(Color.White.copy(alpha = 0.15f))
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
                                    .background(Color.White.copy(alpha = 0.15f))
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
    val parts = initialTime.split(":")
    val initialH = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialM = parts.getOrNull(1)?.toIntOrNull() ?: 0

    var hour by remember { mutableIntStateOf(initialH) }
    var minute by remember { mutableIntStateOf(initialM) }
    val haptic = LocalHapticFeedback.current

    LiquidAlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        message = "با کشیدن انگشت به بالا/پایین یا دکمه‌ها، ساعت را تنظیم کنید",
        confirmText = "تایید",
        onConfirm = {
            val formatted = String.format("%02d:%02d", hour, minute)
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
                        PersianNumberHelper.toPersianDigits(String.format("%02d", minute)),
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
                        PersianNumberHelper.toPersianDigits(String.format("%02d", hour)),
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
