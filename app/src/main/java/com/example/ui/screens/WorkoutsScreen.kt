package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.example.data.local.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.JalaliCalendar
import com.example.util.PersianNumberHelper
import com.example.util.PlanShareHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WorkoutsScreen(
    allPlans: List<WorkoutPlan>,
    allHistories: List<WorkoutHistory>,
    allSkips: List<WorkoutPlanSkip>,
    onStartWorkout: (WorkoutPlan) -> Unit,
    onEditPlan: (WorkoutPlan?) -> Unit,
    onDuplicatePlan: (Long) -> Unit,
    onDeletePlan: (Long) -> Unit,
    onReschedulePlan: (Long, String, String?) -> Unit,
    onUndoSkip: (Long, String) -> Unit,
    onDeleteHistory: (Long) -> Unit,
    onImportPlanJson: (String) -> Unit,
    onGetPlanExercises: suspend (Long) -> List<WorkoutExercise>,
    onDuplicateAllPlans: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: برنامه‌ها, 1: تاریخچه
    var searchQuery by remember { mutableStateOf("") }
    var planToReschedule by remember { mutableStateOf<WorkoutPlan?>(null) }
    var planToDelete by remember { mutableStateOf<WorkoutPlan?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDuplicateAllDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val todayGregorian = remember { JalaliCalendar.getTodayGregorian() }
    val todayWeekday = remember { JalaliCalendar.getTodayWeekdayPersian() }

    val filteredPlans = remember(allPlans, searchQuery) {
        if (searchQuery.isBlank()) allPlans
        else allPlans.filter { it.name.contains(searchQuery, ignoreCase = true) || it.notes.contains(searchQuery, ignoreCase = true) }
    }

    val (todayPlans, otherPlans) = remember(filteredPlans, todayWeekday, allSkips, todayGregorian) {
        val postponedToToday = allSkips.filter { it.newDate == todayGregorian }.map { it.planId }
        val skippedToday = allSkips.filter { it.originalDate == todayGregorian && it.newDate == null }.map { it.planId }

        filteredPlans.partition { plan ->
            (plan.scheduledDay.contains(todayWeekday) || postponedToToday.contains(plan.id)) && !skippedToday.contains(plan.id)
        }
    }

    val groupedHistories = remember(allHistories, searchQuery) {
        val list = if (searchQuery.isBlank()) allHistories
        else allHistories.filter { it.planName.contains(searchQuery, ignoreCase = true) || it.exerciseName.contains(searchQuery, ignoreCase = true) }
        list.groupBy { it.date }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("workouts_screen"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Inline Liquid Top Bar with 44dp Circular Glass Add Button
        item {
            LiquidTopBar(
                title = "برنامه‌های تمرینی",
                navigationIcon = {
                    LiquidCircleButton(
                        icon = Icons.Filled.FileDownload,
                        contentDescription = "ایمپورت برنامه",
                        onClick = { showImportDialog = true }
                    )
                },
                actions = {
                    if (allPlans.isNotEmpty()) {
                        LiquidCircleButton(
                            icon = Icons.Filled.ContentCopy,
                            contentDescription = "کپی متن تمامی برنامه‌ها",
                            onClick = {
                                coroutineScope.launch {
                                    val fullText = StringBuilder()
                                    fullText.append("📋 برنامه‌های تمرینی GYM PRO\n\n")
                                    allPlans.forEach { plan ->
                                        val exercises = onGetPlanExercises(plan.id)
                                        fullText.append(PlanShareHelper.formatPlanAsShareableText(plan, exercises))
                                        fullText.append("\n-------------------\n\n")
                                    }
                                    PlanShareHelper.copyToClipboard(context, "برنامه‌های تمرینی", fullText.toString())
                                    Toast.makeText(context, "متن تمامی برنامه‌ها در حافظه کپی شد (آماده ارسال به دیگران).", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    LiquidCircleButton(
                        icon = Icons.Filled.Add,
                        contentDescription = "برنامه جدید",
                        onClick = { onEditPlan(null) }
                    )
                }
            )
        }

        // 2. Pill Search Field
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                LiquidSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = if (selectedTab == 0) "جستجو در نام یا روزهای برنامه‌ها..." else "جستجو در سوابق تمرینات..."
                )
            }
        }

        // 3. Segmented Control: «برنامه‌ها / تاریخچه»
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                LiquidSegmentedControl(
                    items = listOf("برنامه‌های تمرینی", "سوابق و تاریخچه"),
                    selectedIndex = selectedTab,
                    onSelectIndex = { selectedTab = it }
                )
            }
        }

        if (selectedTab == 0) {
            // Plans Tab
            if (filteredPlans.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.FitnessCenter,
                        title = if (searchQuery.isNotBlank()) "برنامه‌ای با این مشخصات یافت نشد" else "هنوز برنامه‌ای تعریف نشده است",
                        description = if (searchQuery.isNotBlank()) "عبارت جستجو را تغییر دهید" else "یک برنامه اختصاصی جدید بسازید یا برنامه دوستانتان را ایمپورت کنید.",
                        actionButtonText = if (searchQuery.isBlank()) "ساخت برنامه جدید" else null,
                        onActionClick = { onEditPlan(null) }
                    )
                }
            } else {
                // Section: Today's plans
                if (todayPlans.isNotEmpty()) {
                    item {
                        LiquidSectionHeader(title = "نوبت تمرین امروز ($todayWeekday)")
                    }

                    items(todayPlans, key = { "today_${it.id}" }) { plan ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SwipeablePlanCard(
                                onDelete = { planToDelete = plan },
                                onEdit = { onEditPlan(plan) },
                                onShare = {
                                    coroutineScope.launch {
                                        val exercises = onGetPlanExercises(plan.id)
                                        PlanShareHelper.sharePlan(context, plan, exercises)
                                    }
                                },
                                onReschedule = { planToReschedule = plan },
                                onCopyText = {
                                    coroutineScope.launch {
                                        val exercises = onGetPlanExercises(plan.id)
                                        val text = PlanShareHelper.formatPlanAsShareableText(plan, exercises)
                                        PlanShareHelper.copyToClipboard(context, "برنامه ${plan.name}", text)
                                        Toast.makeText(context, "متن برنامه «${plan.name}» کپی شد (آماده ارسال به دیگران).", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                ContentRow(
                                    title = plan.name,
                                    subtitle = "برنامه امروز • ${plan.scheduledDay}",
                                    icon = Icons.Filled.PlayArrow,
                                    badgeText = "شروع نوبت",
                                    gradient = LiquidTheme.cardGradient,
                                    onClick = { onStartWorkout(plan) }
                                
                                )
                            }
                        }
                    }
                }

                // Section: Other plans
                if (otherPlans.isNotEmpty()) {
                    item {
                        LiquidSectionHeader(title = if (todayPlans.isNotEmpty()) "سایر برنامه‌ها" else "تمامی برنامه‌ها")
                    }

                    items(otherPlans, key = { "other_${it.id}" }) { plan ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SwipeablePlanCard(
                                onDelete = { planToDelete = plan },
                                onEdit = { onEditPlan(plan) },
                                onShare = {
                                    coroutineScope.launch {
                                        val exercises = onGetPlanExercises(plan.id)
                                        PlanShareHelper.sharePlan(context, plan, exercises)
                                    }
                                },
                                onCopyText = {
                                    coroutineScope.launch {
                                        val exercises = onGetPlanExercises(plan.id)
                                        val text = PlanShareHelper.formatPlanAsShareableText(plan, exercises)
                                        PlanShareHelper.copyToClipboard(context, "برنامه ${plan.name}", text)
                                        Toast.makeText(context, "متن برنامه «${plan.name}» کپی شد (آماده ارسال به دیگران).", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                ContentRow(
                                    title = plan.name,
                                    subtitle = "روزهای اجرا: ${plan.scheduledDay}",
                                    icon = Icons.Filled.FitnessCenter,
                                    gradient = Brush.linearGradient(
                                        listOf(LiquidTheme.surfaceElevated, LiquidTheme.surfaceSolid)
                                    ),
                                    onClick = { onStartWorkout(plan) }
                                
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // History Tab
            if (groupedHistories.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.History,
                        title = "هنوز سابقه‌ای ثبت نشده است",
                        description = "پس از به پایان رساندن جلسات تمرینی، خلاصه ست‌ها و زمان در این بخش ثبت می‌شود."
                    )
                }
            } else {
                groupedHistories.forEach { (dateStr, histories) ->
                    val jDate = JalaliCalendar.formatGregorianToPersian(dateStr)

                    item(key = "hdr_$dateStr") {
                        LiquidSectionHeader(title = jDate)
                    }

                    item(key = "card_$dateStr") {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SolidGroupedCard {
                                histories.forEachIndexed { index, history ->
                                    val subtitle = "وزن: ${PersianNumberHelper.toPersianDigits(history.weight)} کیلوگرم • ${PersianNumberHelper.toPersianDigits(history.sets)} ست × ${PersianNumberHelper.toPersianDigits(history.reps)} تکرار"

                                    GroupedRow(
                                        title = history.exerciseName,
                                        subtitle = subtitle,
                                        icon = Icons.Filled.CheckCircle,
                                        iconTint = SemanticSuccess,
                                        showArrow = false,
                                        showDivider = index < histories.size - 1,
                                        trailingContent = {
                                            IconButton(
                                                onClick = { onDeleteHistory(history.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.DeleteOutline,
                                                    contentDescription = "حذف رکورد",
                                                    tint = LiquidTheme.textSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Reschedule Confirmation Dialog
    if (planToReschedule != null) {
        val next14Days = remember { JalaliCalendar.getNextDaysGregorian(14) }
        var selectedNewDate by remember { mutableStateOf<String?>(next14Days.getOrNull(1)) }

        LiquidAlertDialog(
            onDismissRequest = { planToReschedule = null },
            title = "مدیریت نوبت تمرین امروز",
            message = "می‌توانید نوبت امروز را به روز دیگری موکول کرده یا کاملاً رد کنید.",
            confirmText = "انتقال نوبت",
            dismissText = "انصراف",
            onConfirm = {
                planToReschedule?.let { p ->
                    onReschedulePlan(p.id, todayGregorian, selectedNewDate)
                }
            },
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "انتخاب روز جدید:",
                        style = MaterialTheme.typography.labelSmall,
                        color = LiquidTheme.textSecondary
                    )
                    next14Days.take(5).drop(1).forEach { gDate ->
                        val jStr = JalaliCalendar.formatGregorianToPersian(gDate)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedNewDate = gDate }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedNewDate == gDate,
                                onClick = { selectedNewDate = gDate }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(jStr, style = MaterialTheme.typography.bodySmall, color = LiquidTheme.textPrimary)
                        }
                    }
                }
            }
        )
    }

    // Delete Plan Confirmation Dialog
    if (planToDelete != null) {
        LiquidAlertDialog(
            onDismissRequest = { planToDelete = null },
            title = "حذف برنامه تمرینی",
            message = "آیا از حذف برنامه «${planToDelete?.name}» اطمینان دارید؟ تمامی حرکات این برنامه حذف خواهند شد.",
            confirmText = "حذف برنامه",
            dismissText = "انصراف",
            isDestructive = true,
            onConfirm = {
                planToDelete?.let { p ->
                    onDeletePlan(p.id)
                }
            }
        )
    }

    // Import Plan Dialog
    if (showImportDialog) {
        LiquidAlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = "ایمپورت برنامه تمرینی",
            message = "کد یا متن JSON برنامه‌ی اشتراک‌گذاری شده را وارد کنید:",
            confirmText = "ایمپورت و ذخیره",
            dismissText = "انصراف",
            onConfirm = {
                if (importJsonText.isNotBlank()) {
                    onImportPlanJson(importJsonText)
                    importJsonText = ""
                }
            },
            content = {
                LiquidInputField(
                    value = importJsonText,
                    onValueChange = { importJsonText = it },
                    label = "متن JSON برنامه",
                    placeholder = "...",
                    singleLine = false,
                    modifier = Modifier.height(100.dp)
                )
            }
        )
    }

    // Duplicate All Plans Confirmation Dialog
    if (showDuplicateAllDialog) {
        LiquidAlertDialog(
            onDismissRequest = { showDuplicateAllDialog = false },
            title = "کپی کردن تمامی برنامه‌ها",
            message = "آیا می‌خواهید از تمام ${PersianNumberHelper.toPersianDigits(allPlans.size)} برنامه‌ی موجود یک نسخه کپی ایجاد کنید؟",
            confirmText = "بله، همه را کپی کن",
            dismissText = "انصراف",
            onConfirm = {
                onDuplicateAllPlans()
                Toast.makeText(context, "تمام برنامه‌ها با موفقیت کپی شدند.", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun SwipeAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    bg: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(58.dp)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = tint, maxLines = 1)
    }
}

@Composable
fun SwipeablePlanCard(
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onCopyText: () -> Unit,
    onShare: (() -> Unit)? = null,
    onReschedule: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val actionCount = 3 + (if (onShare != null) 1 else 0) + (if (onReschedule != null) 1 else 0)
    val revealWidth = (58 * actionCount + 8).dp
    val revealPx = with(density) { revealWidth.toPx() }
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    fun close() { coroutineScope.launch { offsetX.animateTo(0f, androidx.compose.animation.core.spring(0.7f, 400f)) } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(LiquidTheme.surfaceElevated)
                .padding(start = 6.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SwipeAction("ویرایش", Icons.Filled.Edit, LiquidTheme.accentColor, LiquidTheme.accentColor.copy(alpha = 0.18f)) { close(); onEdit() }
            SwipeAction("حذف", Icons.Filled.Delete, SemanticError, SemanticError.copy(alpha = 0.18f)) { close(); onDelete() }
            SwipeAction("کپی متن", Icons.Filled.ContentCopy, LiquidTheme.textPrimary, LiquidTheme.surfaceSolid) { close(); onCopyText() }
            if (onShare != null) SwipeAction("اشتراک", Icons.Filled.Share, LiquidTheme.accentColor, LiquidTheme.accentColor.copy(alpha = 0.18f)) { close(); onShare() }
            if (onReschedule != null) SwipeAction("تعویق", Icons.Filled.EventBusy, SemanticWarning, SemanticWarning.copy(alpha = 0.18f)) { close(); onReschedule() }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(revealPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value > revealPx / 3) offsetX.animateTo(revealPx, androidx.compose.animation.core.spring(0.7f, 400f))
                                else offsetX.animateTo(0f, androidx.compose.animation.core.spring(0.7f, 400f))
                            }
                        },
                        onDragCancel = { close() },
                        onHorizontalDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                offsetX.snapTo((offsetX.value + dragAmount).coerceIn(0f, revealPx))
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
