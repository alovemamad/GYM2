package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.util.PersianNumberHelper
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle

// ==========================================
// 1. LIQUID GLASS BASE MODIFIERS
// ==========================================

@Composable
fun Modifier.liquidGlass(
    hazeState: HazeState,
    shape: Shape = RoundedCornerShape(24.dp),
    tintAlpha: Float? = null,
    borderAlpha: Float = 0.5f,
    elevation: Dp = 8.dp
): Modifier {
    val isDark = LiquidTheme.isDark
    
    // تنظیمات رنگ برای Haze
    val backgroundColor = if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.75f)
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.2f else 0.8f),
            Color.Transparent
        )
    )

    return this
        .shadow(elevation = elevation, shape = shape, spotColor = Color.Black.copy(alpha = 0.3f))
        .clip(shape)
        .hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = backgroundColor,
                tints = listOf(Color.Transparent), // بدون رنگ اضافه
                blurRadius = 20.dp,
                noiseFactor = 0.05f
            )
        )
        .border(BorderStroke(1.dp, borderBrush), shape = shape)
}

@Composable
fun Modifier.liquidTapEffect(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "tap_scale"
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
// 2. BASE CARDS & LAYOUTS
// ==========================================

@Composable
fun SolidGroupedCard(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(hazeState = hazeState, shape = shape, elevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun LiquidGlassBox(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
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
fun GroupedRow(
    hazeState: HazeState,
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
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val lightOffset by animateFloatAsState(
        targetValue = if (isPressed) 100f else -100f,
        animationSpec = tween(300),
        label = "light"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = onClick != null
                ) { onClick?.invoke() }
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = if (isPressed) 0.1f else 0.0f), Color.Transparent),
                        start = Offset(lightOffset, 0f),
                        end = Offset(lightOffset + 200f, 0f)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
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
                            .background(iconBackground ?: iconTint.copy(alpha = 0.2f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
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
                        fontWeight = FontWeight.SemiBold,
                        color = LiquidTheme.textSecondary
                    )
                }
                if (trailingContent != null) {
                    trailingContent()
                } else if (showArrow && onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = LiquidTheme.textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (icon != null) 72.dp else 16.dp)
                    .height(0.5.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )
        }
    }
}

// ==========================================
// 3. BUTTONS
// ==========================================

@Composable
fun LiquidPrimaryButton(
    hazeState: HazeState,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .scale(scale)
            .liquidGlass(hazeState = hazeState, shape = CircleShape, elevation = if (isPressed) 2.dp else 6.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, null, tint = LiquidTheme.accentColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LiquidTheme.accentColor
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
            .background(Color.White.copy(alpha = 0.05f), CircleShape)
            .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, null, tint = LiquidTheme.textPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = LiquidTheme.textPrimary
            )
        }
    }
}

@Composable
fun LiquidCircleButton(
    hazeState: HazeState,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    badgeCount: Int? = null
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .liquidGlass(hazeState = hazeState, shape = CircleShape, elevation = 4.dp)
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
                    .padding(8.dp)
                    .size(10.dp)
                    .background(SemanticError, CircleShape)
                    .border(1.dp, Color.White, CircleShape)
            )
        }
    }
}

// ==========================================
// 4. INPUTS & CONTROLS
// ==========================================

@Composable
fun LiquidSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(Color.White.copy(alpha = 0.1f), CircleShape)
            .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = LiquidTheme.textSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
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
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Close, null, tint = LiquidTheme.textSecondary, modifier = Modifier.size(18.dp))
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, tint = LiquidTheme.textSecondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = LiquidTheme.textSecondary
                )
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = LiquidTheme.textSecondary.copy(alpha = 0.4f))
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = singleLine,
                        keyboardOptions = keyboardOptions,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = LiquidTheme.textPrimary, fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (trailingText != null) {
                Text(trailingText, style = MaterialTheme.typography.bodyMedium, color = LiquidTheme.textSecondary)
            }
        }
    }
}

@Composable
fun LiquidSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 24f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "switch"
    )
    
    val trackColor = if (checked) LiquidTheme.accentColor.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.2f)
    val borderColor = if (checked) LiquidTheme.accentColor.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .size(width = 54.dp, height = 32.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.dp)
                .size(26.dp)
                .shadow(4.dp, CircleShape)
                .background(if (checked) Color.White else LiquidTheme.textSecondary, CircleShape)
        )
    }
}

// ==========================================
// 5. TOP BAR & BOTTOM NAVIGATION
// ==========================================

@Composable
fun LiquidBottomBar(
    hazeState: HazeState,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .height(72.dp)
            .liquidGlass(hazeState = hazeState, shape = RoundedCornerShape(36.dp), elevation = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            com.example.ui.navigation.Screen.bottomNavItems.forEach { screen ->
                val selected = currentRoute == screen.route
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            if (selected) Color.White.copy(alpha = 0.15f) else Color.Transparent
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigate(screen.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                            contentDescription = screen.title,
                            tint = if (selected) LiquidTheme.accentColor else LiquidTheme.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = screen.title,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) LiquidTheme.accentColor else LiquidTheme.textSecondary
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.wrapContentWidth().defaultMinSize(minWidth = 46.dp),
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
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    fontWeight = FontWeight.Bold,
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
                modifier = Modifier.wrapContentWidth().defaultMinSize(minWidth = 46.dp),
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
// 6. DIALOGS & MISC
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
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onDismissRequest() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .clickable(enabled = false) {}
                    .background(LiquidTheme.surfaceSolid, RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .shadow(16.dp, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LiquidTheme.textPrimary)
                        if (message.isNotBlank()) {
                            Text(message, style = MaterialTheme.typography.bodyMedium, color = LiquidTheme.textSecondary, textAlign = TextAlign.Center)
                        }
                        if (content != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            content()
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.1f)))
                    Row(modifier = Modifier.fillMaxWidth().height(54.dp)) {
                        if (dismissText != null) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onDismissRequest() }, contentAlignment = Alignment.Center) {
                                Text(dismissText, style = MaterialTheme.typography.titleMedium, color = LiquidTheme.textSecondary)
                            }
                            Box(modifier = Modifier.width(0.5.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.1f)))
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onConfirm(); onDismissRequest() }, contentAlignment = Alignment.Center) {
                            Text(confirmText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isDestructive) SemanticError else LiquidTheme.accentColor)
                        }
                    }
                }
            }
        }
    }
}

// (بقیه کامپوننت‌های خاص مثل EmptyStateCard و LiquidTimePickerDialog رو هم می‌تونی همون‌طور که قبلاً بود نگه داری یا از کدهای قبلی کپی کنی)
