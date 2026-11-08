package com.example.ui.screens

import androidx.compose.animation.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.WorkoutExercise
import com.example.data.local.WorkoutPlan
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.JalaliCalendar
import com.example.util.PersianNumberHelper

@Composable
fun PlanEditorScreen(
    initialPlan: WorkoutPlan?,
    initialExercises: List<WorkoutExercise>,
    onSave: (WorkoutPlan, List<WorkoutExercise>) -> Unit,
    onBack: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialPlan?.name ?: "") }
    var notes by rememberSaveable { mutableStateOf(initialPlan?.notes ?: "") }
    val initialDays = remember {
        initialPlan?.scheduledDay?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    }
    var selectedDays by remember { mutableStateOf(initialDays) }

    var exercises by remember {
        mutableStateOf(
            if (initialExercises.isNotEmpty()) initialExercises else listOf(
                WorkoutExercise(planId = 0, name = "", sets = 3, reps = 10, weight = 20.0)
            )
        )
    }

    val weekDays = JalaliCalendar.WEEKDAY_NAMES

    // خروج بدون ذخیره: اگر چیزی تغییر کرده باشد قبل از خروج تأیید گرفته می‌شود
    var showDiscardDialog by remember { mutableStateOf(false) }
    val hasUnsavedChanges = name != (initialPlan?.name ?: "") ||
        notes != (initialPlan?.notes ?: "") ||
        selectedDays != initialDays ||
        (initialExercises.isNotEmpty() && exercises != initialExercises) ||
        (initialExercises.isEmpty() && exercises.any { it.name.isNotBlank() })
    val requestBack: () -> Unit = {
        if (hasUnsavedChanges) showDiscardDialog = true else onBack()
    }
    BackHandler(onBack = requestBack)

    if (showDiscardDialog) {
        LiquidAlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = "خروج بدون ذخیره؟",
            message = "تغییرات این برنامه ذخیره نشده و از بین می‌رود.",
            confirmText = "خروج",
            dismissText = "ادامه ویرایش",
            isDestructive = true,
            onConfirm = onBack
        )
    }

    Scaffold(
        containerColor = LiquidTheme.backgroundNeutral,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LiquidTopBar(
                title = if (initialPlan == null) "برنامه جدید" else "ویرایش برنامه",
                navigationIcon = {
                    LiquidCircleButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت",
                        onClick = requestBack
                    )
                },
                actions = {
                    Text(
                        text = "ذخیره",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (name.isNotBlank()) LiquidTheme.accentColor else LiquidTheme.textSecondary,
                        modifier = Modifier
                            .clickable(enabled = name.isNotBlank()) {
                                val plan = (initialPlan ?: WorkoutPlan(name = name)).copy(
                                    name = name,
                                    notes = notes,
                                    scheduledDay = selectedDays.joinToString(",")
                                )
                                onSave(plan, exercises.filter { it.name.isNotBlank() })
                            }
                            .padding(8.dp)
                            .testTag("plan_editor_save_btn")
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group 1: General Info
            item {
                LiquidSectionHeader(title = "مشخصات کلی")
                SolidGroupedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LiquidInputField(
                            value = name,
                            onValueChange = { name = it },
                            label = "نام برنامه (ضروری)",
                            placeholder = "مثلاً سینه و بازو، PPL، فول‌بادی"
                        )

                        LiquidInputField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = "توضیحات و نکات تمرینی (اختیاری)",
                            placeholder = "مثلاً استراحت ۹۰ ثانیه بین ست‌ها"
                        )
                    }
                }
            }

            // Group 2: Scheduled Days
            item {
                LiquidSectionHeader(title = "روزهای اجرا در هفته")
                SolidGroupedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            weekDays.take(4).forEach { day ->
                                val isSelected = selectedDays.contains(day)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .liquidTapEffect {
                                            selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) LiquidTheme.accentColor else LiquidTheme.surfaceElevated
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = day,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) LiquidTheme.onAccent else LiquidTheme.textPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            weekDays.drop(4).forEach { day ->
                                val isSelected = selectedDays.contains(day)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .liquidTapEffect {
                                            selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) LiquidTheme.accentColor else LiquidTheme.surfaceElevated
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = day,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) LiquidTheme.onAccent else LiquidTheme.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Group 3: Exercises
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidSectionHeader(title = "حرکات تمرینی (${PersianNumberHelper.toPersianDigits(exercises.size)})")
                    Text(
                        text = "+ حرکت جدید",
                        style = MaterialTheme.typography.labelLarge,
                        color = LiquidTheme.accentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                exercises = exercises + WorkoutExercise(
                                    planId = initialPlan?.id ?: 0,
                                    name = "",
                                    sets = 3,
                                    reps = 10,
                                    weight = 0.0
                                )
                            }
                            .padding(end = 16.dp)
                    )
                }
            }

            itemsIndexed(exercises) { index, exercise ->
                var expanded by remember { mutableStateOf(true) }

                SolidGroupedCard(
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header row with exercise name & collapsible chevron
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(LiquidTheme.accentColor.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = PersianNumberHelper.toPersianDigits(index + 1),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = LiquidTheme.accentColor
                                    )
                                }

                                Text(
                                    text = if (exercise.name.isNotBlank()) exercise.name else "حرکت شماره ${PersianNumberHelper.toPersianDigits(index + 1)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LiquidTheme.textPrimary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Move Up
                                if (index > 0) {
                                    IconButton(
                                        onClick = {
                                            val list = exercises.toMutableList()
                                            val temp = list[index]
                                            list[index] = list[index - 1]
                                            list[index - 1] = temp
                                            exercises = list
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.ArrowUpward, contentDescription = "بالا", tint = LiquidTheme.textSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                // Move Down
                                if (index < exercises.size - 1) {
                                    IconButton(
                                        onClick = {
                                            val list = exercises.toMutableList()
                                            val temp = list[index]
                                            list[index] = list[index + 1]
                                            list[index + 1] = temp
                                            exercises = list
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.ArrowDownward, contentDescription = "پایین", tint = LiquidTheme.textSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                // Duplicate / Clone
                                IconButton(
                                    onClick = {
                                        val list = exercises.toMutableList()
                                        list.add(index + 1, exercise.copy(name = if (exercise.name.isNotBlank()) "${exercise.name} (کپی)" else ""))
                                        exercises = list
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "تکثیر حرکت", tint = LiquidTheme.accentColor, modifier = Modifier.size(17.dp))
                                }

                                // Delete
                                IconButton(
                                    onClick = {
                                        exercises = exercises.filterIndexed { i, _ -> i != index }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = SemanticError, modifier = Modifier.size(18.dp))
                                }

                                // Expand/Collapse
                                IconButton(
                                    onClick = { expanded = !expanded },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = null,
                                        tint = LiquidTheme.textSecondary
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = expanded) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LiquidInputField(
                                    value = exercise.name,
                                    onValueChange = { newName ->
                                        exercises = exercises.mapIndexed { i, ex ->
                                            if (i == index) ex.copy(name = newName) else ex
                                        }
                                    },
                                    label = "نام حرکت",
                                    placeholder = "مثلاً پرس سینه با هالتر"
                                )

                                // 1. Sets count with visual chips
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("تعداد ست‌ها:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = LiquidTheme.textPrimary)
                                        LiquidStepper(
                                            value = exercise.sets.toDouble(),
                                            onValueChange = { newSets ->
                                                exercises = exercises.mapIndexed { i, ex ->
                                                    if (i == index) ex.copy(sets = newSets.toInt()) else ex
                                                }
                                            },
                                            step = 1.0,
                                            min = 1.0,
                                            max = 20.0
                                        )
                                    }

                                    // Visual set badges
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        (1..exercise.sets).take(7).forEach { setNum ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(LiquidTheme.surfaceElevated)
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "ست ${PersianNumberHelper.toPersianDigits(setNum)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = LiquidTheme.textSecondary
                                                )
                                            }
                                        }
                                        if (exercise.sets > 7) {
                                            Text("...", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                                        }
                                    }
                                }

                                HorizontalDivider(color = LiquidTheme.dividerColor, thickness = 0.5.dp)

                                // 2. Timed exercise toggle (Plank, Stretches, Isometric)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "حرکت زمان‌دار / تایمری (مثل پلانک)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = LiquidTheme.textPrimary
                                        )
                                        Text(
                                            text = "در زمان تمرین، تایمر معکوس با آلارم صوتی فعال می‌شود",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LiquidTheme.textSecondary
                                        )
                                    }
                                    LiquidSwitch(
                                        checked = exercise.isTimedExercise,
                                        onCheckedChange = { isTimed ->
                                            exercises = exercises.mapIndexed { i, ex ->
                                                if (i == index) ex.copy(
                                                    isTimedExercise = isTimed,
                                                    reps = if (isTimed && ex.reps < 10) 60 else ex.reps
                                                ) else ex
                                            }
                                        }
                                    )
                                }

                                if (exercise.isTimedExercise) {
                                    // Timed exercise duration selector
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "مدت زمان نگه داشتن (ثانیه):",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = LiquidTheme.textSecondary
                                            )
                                            LiquidStepper(
                                                value = exercise.reps.toDouble(),
                                                onValueChange = { newSec ->
                                                    exercises = exercises.mapIndexed { i, ex ->
                                                        if (i == index) ex.copy(reps = newSec.toInt()) else ex
                                                    }
                                                },
                                                step = 5.0,
                                                min = 5.0,
                                                max = 600.0
                                            )
                                        }

                                        // Preset quick chips
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(30, 45, 60, 90, 120).forEach { sec ->
                                                val isSelected = exercise.reps == sec
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(40.dp)
                                                        .liquidTapEffect {
                                                            exercises = exercises.mapIndexed { i, ex ->
                                                                if (i == index) ex.copy(reps = sec) else ex
                                                            }
                                                        },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) LiquidTheme.accentColor else LiquidTheme.surfaceElevated
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = "${PersianNumberHelper.toPersianDigits(sec)}ث",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) LiquidTheme.onAccent else LiquidTheme.textPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // 3. Regular Reps & Rep Range (e.g. 15 to 20 reps)
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "حداقل تکرار:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = LiquidTheme.textSecondary
                                            )
                                            LiquidStepper(
                                                value = exercise.reps.toDouble(),
                                                onValueChange = { newReps ->
                                                    exercises = exercises.mapIndexed { i, ex ->
                                                        if (i == index) ex.copy(reps = newReps.toInt()) else ex
                                                    }
                                                },
                                                step = 1.0,
                                                min = 1.0,
                                                max = 100.0
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    "حداکثر تکرار (بازه تکرار):",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = LiquidTheme.textSecondary
                                                )
                                                if (exercise.repsMax != null && exercise.repsMax > exercise.reps) {
                                                    Text(
                                                        "دامنه: ${PersianNumberHelper.toPersianDigits(exercise.reps)} الی ${PersianNumberHelper.toPersianDigits(exercise.repsMax)} تکرار",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = LiquidTheme.accentColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            LiquidStepper(
                                                value = (exercise.repsMax ?: exercise.reps).toDouble(),
                                                onValueChange = { newMax ->
                                                    exercises = exercises.mapIndexed { i, ex ->
                                                        if (i == index) ex.copy(repsMax = if (newMax.toInt() > ex.reps) newMax.toInt() else null) else ex
                                                    }
                                                },
                                                step = 1.0,
                                                min = exercise.reps.toDouble(),
                                                max = 100.0
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = LiquidTheme.dividerColor, thickness = 0.5.dp)

                                // 4. Weight
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("وزنه پیشنهادی (kg):", style = MaterialTheme.typography.bodyMedium, color = LiquidTheme.textSecondary)
                                    LiquidStepper(
                                        value = exercise.weight,
                                        onValueChange = { newWeight ->
                                            exercises = exercises.mapIndexed { i, ex ->
                                                if (i == index) ex.copy(weight = newWeight) else ex
                                            }
                                        },
                                        step = 2.5,
                                        min = 0.0,
                                        max = 500.0
                                    )
                                }

                                LiquidInputField(
                                    value = exercise.notes,
                                    onValueChange = { newNotes ->
                                        exercises = exercises.mapIndexed { i, ex ->
                                            if (i == index) ex.copy(notes = newNotes) else ex
                                        }
                                    },
                                    label = "نکات حرکت (اختیاری)",
                                    placeholder = "مثلاً تمرکز روی بخش منفی، مکث در اوج"
                                )
                            }
                        }
                    }
                }
            }

            // Quick Exercise Suggestions
            item {
                var selectedMuscleGroup by remember { mutableStateOf("سینه") }
                val presetsMap = remember {
                    mapOf(
                        "سینه" to listOf("پرس سینه با هالتر", "بالاسینه با دمبل", "قفسه سینه دمبل", "شنا سوئدی", "پارالل سینه"),
                        "زیربغل" to listOf("زیربغل سیم‌کش از جلو", "بارفیکس", "زیربغل هالتر خم", "قایقی سیم‌کش", "ددلیفت"),
                        "سرشانه" to listOf("پرس سرشانه دمبل", "نشر از جانب", "نشر خم سیم‌کش", "فیس پول", "کول هالتر"),
                        "بازو" to listOf("جلو بازو هالتر", "جلو بازو دمبل", "پشت بازو سیم‌کش طناب", "پشت بازو دیپ", "جلو بازو چکشی"),
                        "پا" to listOf("اسکات با هالتر", "پرس پا دستگاه", "جلو پا ماشین", "پشت پا خوابیده", "ساق پا ایستاده"),
                        "شکم" to listOf("پلانک شکم", "کرانچ روی تشک", "زیرشکم خلبانی", "چرخش روسی")
                    )
                }

                SolidGroupedCard(shape = RoundedCornerShape(18.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "💡 افزودن سریع حرکات پیشنهادی",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = LiquidTheme.textPrimary
                            )
                            Text(
                                text = "برای افزودن لمس کنید",
                                style = MaterialTheme.typography.labelSmall,
                                color = LiquidTheme.textSecondary
                            )
                        }

                        // Category tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presetsMap.keys.forEach { muscle ->
                                val isSelected = selectedMuscleGroup == muscle
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .liquidTapEffect { selectedMuscleGroup = muscle },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) LiquidTheme.accentColor else LiquidTheme.surfaceElevated
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = muscle,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) LiquidTheme.onAccent else LiquidTheme.textPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Exercise chips for selected category
                        val currentList = presetsMap[selectedMuscleGroup] ?: emptyList()
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentList) { exName ->
                                Surface(
                                    modifier = Modifier
                                        .liquidTapEffect {
                                            val isTimed = exName.contains("پلانک")
                                            exercises = exercises + WorkoutExercise(
                                                planId = initialPlan?.id ?: 0,
                                                name = exName,
                                                sets = if (isTimed) 3 else 4,
                                                reps = if (isTimed) 60 else 10,
                                                weight = if (isTimed) 0.0 else 20.0,
                                                isTimedExercise = isTimed
                                            )
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = LiquidTheme.surfaceElevated,
                                    border = BorderStroke(0.5.dp, LiquidTheme.dividerColor)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Add,
                                            contentDescription = null,
                                            tint = LiquidTheme.accentColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = exName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = LiquidTheme.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Prominent Full-Width Add Exercise Button
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidTapEffect {
                            exercises = exercises + WorkoutExercise(
                                planId = initialPlan?.id ?: 0,
                                name = "",
                                sets = 3,
                                reps = 10,
                                weight = 0.0
                            )
                        },
                    shape = RoundedCornerShape(18.dp),
                    color = LiquidTheme.accentColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.5.dp, LiquidTheme.accentColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = null,
                            tint = LiquidTheme.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "+ افزودن حرکت جدید به این برنامه (حرکت ${PersianNumberHelper.toPersianDigits(exercises.size + 1)})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LiquidTheme.accentColor
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                LiquidPrimaryButton(
                    text = "ذخیره تغییرات برنامه",
                    onClick = {
                        if (name.isNotBlank()) {
                            val plan = (initialPlan ?: WorkoutPlan(name = name)).copy(
                                name = name,
                                notes = notes,
                                scheduledDay = selectedDays.joinToString(",")
                            )
                            onSave(plan, exercises.filter { it.name.isNotBlank() })
                        }
                    },
                    enabled = name.isNotBlank()
                )
            }
        }
    }
}
