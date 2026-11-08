package com.example.ui.components

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect

// ==========================================
// 1. LIQUID GLASS BASE MODIFIERS
// ==========================================

@Composable
fun Modifier.liquidGlass(
    hazeState: HazeState? = null,
    shape: Shape = RoundedCornerShape(24.dp),
    tintAlpha: Float = 0.85f, // شفافیت بالا برای خوانایی
    elevation: Dp = 8.dp
): Modifier {
    val isDark = LiquidTheme.isDark
    
    // رنگ پس‌زمینه کارت
    val bgColor = if (isDark) Color(0xFF1E1E1E).copy(alpha = tintAlpha) else Color.White.copy(alpha = tintAlpha)
    
    // حاشیه نوری (Specular) از بالا
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.2f else 0.8f),
            Color.Transparent
        )
    )

    return this
        .shadow(elevation = elevation, shape = shape, spotColor = Color.Black.copy(alpha = 0.3f))
        .clip(shape)
        .then(
            if (hazeState != null) {
                Modifier.hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = bgColor,
                        tints = emptyList(),
                        blurRadius = 20.dp,
                        noiseFactor = 0.1f
                    )
                )
            } else {
                Modifier.background(bgColor)
            }
        )
        .border(BorderStroke(1.dp, borderBrush), shape = shape)
}

@Composable
fun Modifier.liquidTapEffect(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "tap")
    return if (!enabled) this else this.scale(scale).pointerInput(Unit) {
        detectTapGestures(onPress = { isPressed = true; tryAwaitRelease(); isPressed = false }, onTap = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() })
    }
}

// ==========================================
// 2. BASE CARDS & LAYOUTS
// ==========================================

@Composable
fun SolidGroupedCard(
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier, 
    shape: Shape = RoundedCornerShape(24.dp), 
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxWidth().liquidGlass(hazeState = hazeState, shape = shape, elevation = 4.dp)) {
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun LiquidGlassBox(
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier, 
    shape: Shape = RoundedCornerShape(24.dp), 
    elevation: Dp = 8.dp, 
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.liquidGlass(hazeState = hazeState, shape = shape, elevation = elevation), content = content)
}

@Composable
fun LiquidSectionHeader(title: String, modifier: Modifier = Modifier, actionText: String? = null, onActionClick: (() -> Unit)? = null) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp), color = LiquidTheme.textSecondary, fontWeight = FontWeight.SemiBold)
        if (actionText != null && onActionClick != null) Text(actionText, style = MaterialTheme.typography.labelMedium, color = LiquidTheme.accentColor, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onActionClick() }.padding(4.dp))
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
    val isPressed by interactionSource.collectIsPressedAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().clickable(interactionSource, null, enabled = onClick != null) { onClick?.invoke() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(1f, fill = false)) {
                if (icon != null) Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(iconBackground ?: iconTint.copy(alpha = 0.2f)).border(0.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp)) }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = LiquidTheme.textPrimary)
                    if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LiquidTheme.textSecondary)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (valueText != null) Text(valueText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = LiquidTheme.textSecondary)
                if (trailingContent != null) trailingContent() else if (showArrow && onClick != null) Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = LiquidTheme.textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }
        if (showDivider) Box(Modifier.fillMaxWidth().padding(start = if (icon != null) 72.dp else 16.dp).height(0.5.dp).background(Color.White.copy(alpha = 0.1f)))
    }
}

@Composable
fun GradientHeroCard(title: String, modifier: Modifier = Modifier, subtitle: String? = null, gradient: Brush = LiquidTheme.cardGradient, badgeText: String? = null, content: (@Composable ColumnScope.() -> Unit)? = null) {
    val shape = RoundedCornerShape(28.dp)
    Surface(modifier = modifier.fillMaxWidth().shadow(8.dp, shape, spotColor = Color.Black.copy(alpha = 0.35f)), shape = shape, color = Color.Transparent) {
        Box(Modifier.fillMaxWidth().background(gradient).border(BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f)))), shape).padding(20.dp)) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (badgeText != null) Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.2f)) { Text(badgeText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(10.dp, 4.dp)) }
                Text(title, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp), fontWeight = FontWeight.ExtraBold, color = Color.White)
                if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                if (content != null) { Spacer(Modifier.height(4.dp)); content() }
            }
        }
    }
}

@Composable
fun CircularShortcut(icon: ImageVector, label: String, modifier: Modifier = Modifier, iconTint: Color = LiquidTheme.accentColor, onClick: () -> Unit) {
    Column(modifier = modifier.liquidTapEffect { onClick() }.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(60.dp).liquidGlass(shape = CircleShape, elevation = 4.dp), contentAlignment = Alignment.Center) { Icon(icon, label, tint = iconTint, modifier = Modifier.size(26.dp)) }
        Text(label, style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textPrimary, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ContentRow(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier, gradient: Brush = LiquidTheme.cardGradient, badgeText: String? = null, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    SolidGroupedCard(modifier = modifier.liquidTapEffect { onClick() }, shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(1f)) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(gradient), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = LiquidTheme.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LiquidTheme.accentColor, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (badgeText != null) Surface(shape = RoundedCornerShape(10.dp), color = LiquidTheme.accentColor.copy(alpha = 0.14f)) { Text(badgeText, style = MaterialTheme.typography.labelSmall, color = LiquidTheme.accentColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp, 4.dp)) } else if (trailing != null) trailing()
        }
    }
}

// ==========================================
// 3. BUTTONS & INPUTS
// ==========================================

@Composable
fun LiquidPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")
    Box(modifier = modifier.fillMaxWidth().height(54.dp).scale(scale).liquidGlass(shape = CircleShape, elevation = if (isPressed) 2.dp else 6.dp).clickable(interactionSource, null, enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) { Icon(icon, null, tint = LiquidTheme.accentColor, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = LiquidTheme.accentColor)
        }
    }
}

@Composable
fun LiquidSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Box(modifier = modifier.fillMaxWidth().height(54.dp).liquidTapEffect { onClick() }.background(Color.White.copy(alpha = 0.05f), CircleShape).border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) { Icon(icon, null, tint = LiquidTheme.textPrimary, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = LiquidTheme.textPrimary)
        }
    }
}

@Composable
fun LiquidCircleButton(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, contentDescription: String? = null, badgeCount: Int? = null) {
    Box(Modifier.size(46.dp).liquidGlass(shape = CircleShape, elevation = 4.dp).liquidTapEffect { onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription, tint = LiquidTheme.textPrimary, modifier = Modifier.size(22.dp))
        if (badgeCount != null && badgeCount > 0) Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(10.dp).background(SemanticError, CircleShape).border(1.dp, Color.White, CircleShape))
    }
}

@Composable
fun LiquidSegmentedControl(items: List<String>, selectedIndex: Int, onSelectIndex: (Int) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(44.dp).background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(22.dp)).padding(4.dp)) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(18.dp)).background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent).clickable { onSelectIndex(index) }, contentAlignment = Alignment.Center) {
                    Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) LiquidTheme.accentColor else LiquidTheme.textSecondary)
                }
            }
        }
    }
}

@Composable
fun LiquidStepper(value: Double, onValueChange: (Double) -> Unit, modifier: Modifier = Modifier, step: Double = 1.0, min: Double = 0.0, max: Double = 500.0, formatString: String = "%.1f") {
    Row(modifier = modifier.height(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.1f)), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onValueChange((value - step).coerceAtLeast(min)) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Filled.Remove, "کاهش", tint = LiquidTheme.textPrimary) }
        Box(Modifier.padding(horizontal = 8.dp).widthIn(min = 50.dp), contentAlignment = Alignment.Center) {
            Text(if (step >= 1.0 && value % 1.0 == 0.0) PersianNumberHelper.toPersianDigits(value.toInt()) else PersianNumberHelper.toPersianDigits(String.format(formatString, value)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = LiquidTheme.textPrimary)
        }
        IconButton(onClick = { onValueChange((value + step).coerceAtMost(max)) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Filled.Add, "افزایش", tint = LiquidTheme.textPrimary) }
    }
}

@Composable
fun LiquidSearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, trailingAction: (@Composable () -> Unit)? = null) {
    Box(modifier = modifier.fillMaxWidth().height(54.dp).background(Color.White.copy(alpha = 0.1f), CircleShape).border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Search, null, tint = LiquidTheme.textSecondary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = LiquidTheme.textSecondary.copy(alpha = 0.6f))
                BasicTextField(value, onValueChange, singleLine = true, textStyle = MaterialTheme.typography.bodyMedium.copy(color = LiquidTheme.textPrimary, fontWeight = FontWeight.Medium), modifier = Modifier.fillMaxWidth())
            }
            if (value.isNotEmpty()) { IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(24.dp)) { Icon(Icons.Filled.Close, null, tint = LiquidTheme.textSecondary, modifier = Modifier.size(18.dp)) } }
            if (trailingAction != null) trailingAction()
        }
    }
}

@Composable
fun LiquidInputField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, placeholder: String = "", keyboardOptions: KeyboardOptions = KeyboardOptions.Default, singleLine: Boolean = true, trailingText: String? = null, leadingIcon: ImageVector? = null) {
    Box(modifier = modifier.fillMaxWidth().height(60.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)).border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (leadingIcon != null) { Icon(leadingIcon, null, tint = LiquidTheme.textSecondary, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)) }
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = LiquidTheme.textSecondary.copy(alpha = 0.4f))
                    BasicTextField(value, onValueChange, singleLine = singleLine, keyboardOptions = keyboardOptions, textStyle = MaterialTheme.typography.bodyLarge.copy(color = LiquidTheme.textPrimary, fontWeight = FontWeight.Bold), modifier = Modifier.fillMaxWidth())
                }
            }
            if (trailingText != null) Text(trailingText, style = MaterialTheme.typography.bodyMedium, color = LiquidTheme.textSecondary)
        }
    }
}

@Composable
fun LiquidSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val thumbOffset by animateFloatAsState(if (checked) 24f else 0f, spring(dampingRatio = 0.7f, stiffness = 500f), label = "switch")
    val trackColor = if (checked) LiquidTheme.accentColor.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.2f)
    val borderColor = if (checked) LiquidTheme.accentColor.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.2f)
    Box(modifier = modifier.size(54.dp, 32.dp).clip(CircleShape).background(trackColor).border(1.dp, borderColor, CircleShape).clickable { onCheckedChange(!checked) }.padding(3.dp), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.offset(x = thumbOffset.dp).size(26.dp).shadow(4.dp, CircleShape).background(if (checked) Color.White else LiquidTheme.textSecondary, CircleShape))
    }
}

// ==========================================
// 4. NAVIGATION & DIALOGS
// ==========================================

@Composable
fun LiquidBottomBar(currentRoute: String, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 24.dp, vertical = 12.dp).height(72.dp).liquidGlass(shape = RoundedCornerShape(36.dp), elevation = 12.dp)) {
        Row(Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceAround) {
            com.example.ui.navigation.Screen.bottomNavItems.forEach { screen ->
                val selected = currentRoute == screen.route
                Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(28.dp)).background(if (selected) Color.White.copy(alpha = 0.15f) else Color.Transparent).clickable { onNavigate(screen.route) }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(if (selected) screen.selectedIcon else screen.unselectedIcon, screen.title, tint = if (selected) LiquidTheme.accentColor else LiquidTheme.textSecondary, modifier = Modifier.size(24.dp))
                        Text(screen.title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) LiquidTheme.accentColor else LiquidTheme.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidTopBar(title: String, modifier: Modifier = Modifier, navigationIcon: (@Composable () -> Unit)? = null, actions: (@Composable RowScope.() -> Unit)? = null, endAction: (@Composable () -> Unit)? = null, subtitle: String? = null) {
    Box(modifier = modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(16.dp, 8.dp)) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.wrapContentWidth().defaultMinSize(minWidth = 46.dp), contentAlignment = Alignment.CenterStart) { if (navigationIcon != null) navigationIcon() }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), fontWeight = FontWeight.Bold, color = LiquidTheme.textPrimary, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary, textAlign = TextAlign.Center, maxLines = 1)
            }
            Row(Modifier.wrapContentWidth().defaultMinSize(minWidth = 46.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) { if (actions != null) actions(); if (endAction != null) endAction() }
        }
    }
}

@Composable
fun LiquidAlertDialog(onDismissRequest: () -> Unit, title: String, message: String, confirmText: String, onConfirm: () -> Unit, dismissText: String? = null, isDestructive: Boolean = false, content: (@Composable () -> Unit)? = null) {
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable { onDismissRequest() }, contentAlignment = Alignment.Center) { // پس‌زمینه تیره‌تر
            Box(Modifier.width(320.dp).clickable(enabled = false) {}.background(LiquidTheme.surfaceSolid.copy(alpha = 1f), RoundedCornerShape(24.dp)).border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)).shadow(16.dp, RoundedCornerShape(24.dp))) { // دیالوگ کاملاً مات
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LiquidTheme.textPrimary)
                        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodyMedium, color = LiquidTheme.textSecondary, textAlign = TextAlign.Center)
                        if (content != null) { Spacer(Modifier.height(4.dp)); content() }
                    }
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.1f)))
                    Row(Modifier.fillMaxWidth().height(54.dp)) {
                        if (dismissText != null) { Box(Modifier.weight(1f).fillMaxHeight().clickable { onDismissRequest() }, Alignment.Center) { Text(dismissText, style = MaterialTheme.typography.titleMedium, color = LiquidTheme.textSecondary) }; Box(Modifier.width(0.5.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.1f))) }
                        Box(Modifier.weight(1f).fillMaxHeight().clickable { onConfirm(); onDismissRequest() }, Alignment.Center) { Text(confirmText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isDestructive) SemanticError else LiquidTheme.accentColor) }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. EXTRA COMPONENTS
// ==========================================

@Composable
fun EmptyStateCard(icon: ImageVector, title: String, modifier: Modifier = Modifier, description: String? = null, actionButtonText: String? = null, onActionClick: (() -> Unit)? = null) {
    SolidGroupedCard(modifier = modifier.fillMaxWidth().padding(16.dp, 8.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(LiquidTheme.accentColor.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = LiquidTheme.accentColor, modifier = Modifier.size(30.dp)) }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = LiquidTheme.textPrimary)
            if (!description.isNullOrBlank()) Text(description, style = MaterialTheme.typography.bodySmall, color = LiquidTheme.textSecondary, textAlign = TextAlign.Center)
            if (actionButtonText != null && onActionClick != null) { Spacer(Modifier.height(4.dp)); LiquidPrimaryButton(text = actionButtonText, onClick = onActionClick, modifier = Modifier.widthIn(max = 220.dp)) }
        }
    }
}

@Composable
fun LiquidRestTimerDock(remainingSeconds: Int, isActive: Boolean, onAdd15Seconds: () -> Unit, onCancel: () -> Unit, onClickDock: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = isActive, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(), modifier = modifier) {
        LiquidGlassBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable { onClickDock() }, shape = RoundedCornerShape(26.dp), elevation = 8.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Timer, null, tint = LiquidTheme.accentColor, modifier = Modifier.size(24.dp))
                    Column {
                        Text("استراحت ست فعال", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                        Text(PersianNumberHelper.formatSecondsToTime(remainingSeconds.toLong()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = LiquidTheme.accentColor)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.height(34.dp).liquidTapEffect { onAdd15Seconds() }, shape = CircleShape, color = LiquidTheme.accentColor.copy(alpha = 0.16f)) { Box(Modifier.padding(horizontal = 12.dp), Alignment.Center) { Text("+۱۵ث", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LiquidTheme.accentColor) } }
                    IconButton(onClick = onCancel, modifier = Modifier.size(34.dp)) { Icon(Icons.Filled.Close, "انصراف", tint = LiquidTheme.textSecondary) }
                }
            }
        }
    }
}

@Composable
fun LiquidTimePickerDialog(initialTime: String, title: String = "انتخاب ساعت", onDismissRequest: () -> Unit, onConfirm: (String) -> Unit) {
    LiquidAlertDialog(onDismissRequest = onDismissRequest, title = title, message = "برای تنظیم ساعت، فعلاً از پیش‌فرض استفاده کنید.", confirmText = "تایید", onConfirm = { onConfirm(initialTime) }, dismissText = "انصراف")
}

@Composable
fun LiquidGlassRoot(content: @Composable () -> Unit) {
    // یک پس‌زمینه گرادیانتی ملایم برای اینکه افکت شیشه دیده بشه
    val bgBrush = Brush.linearGradient(
        colors = if (LiquidTheme.isDark) {
            listOf(Color(0xFF0F0F13), Color(0xFF1A1A24), Color(0xFF0F0F13))
        } else {
            listOf(Color(0xFFF5F5F7), Color(0xFFE8E8ED), Color(0xFFF5F5F7))
        }
    )
    Box(modifier = Modifier.fillMaxSize().background(bgBrush)) { content() }
}
