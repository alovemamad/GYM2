package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ActiveWorkoutViewModel
import com.example.util.PersianNumberHelper

@Composable
fun ActiveWorkoutScreen(
    viewModel: ActiveWorkoutViewModel,
    onTemporaryExit: () -> Unit,
    onWorkoutFinished: () -> Unit,
    weightUnit: String = "kg"
) {
    val planName by viewModel.currentPlanName.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val exercisesState by viewModel.exercisesState.collectAsState()
    val isRestTimerActive by viewModel.isRestTimerActive.collectAsState()
    val restSecondsRemaining by viewModel.restSecondsRemaining.collectAsState()
    val totalRestUsed by viewModel.totalRestSecondsUsed.collectAsState()
    val isGlobalCustomMetrics by viewModel.isGlobalCustomMetrics.collectAsState()

    var showFinishConfirmDialog by remember { mutableStateOf(false) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var showWorkoutSettingsDialog by remember { mutableStateOf(false) }
    var selectedDefaultRest by remember { mutableIntStateOf(60) }

    // Intercept back button to allow temporary exit without losing progress
    BackHandler {
        onTemporaryExit()
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LiquidTopBar(
                title = planName,
                navigationIcon = {
                    LiquidCircleButton(
                        icon = Icons.Filled.Close,
                        contentDescription = "خروج موقت",
                        onClick = onTemporaryExit
                    )
                },
                actions = {
                    LiquidCircleButton(
                        icon = Icons.Filled.Settings,
                        contentDescription = "تنظیمات تمرین",
                        onClick = { showWorkoutSettingsDialog = true }
                    )
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Fixed Floating Rest Timer Dock Capsule
                LiquidRestTimerDock(
                    remainingSeconds = restSecondsRemaining,
                    isActive = isRestTimerActive,
                    onAdd15Seconds = { viewModel.addRestTime(15) },
                    onCancel = { viewModel.cancelRestTimer() },
                    onClickDock = {}
                )

                LiquidPrimaryButton(
                    text = "پایان و ذخیره تمرین",
                    icon = Icons.Filled.Check,
                    onClick = { showFinishConfirmDialog = true },
                    modifier = Modifier.testTag("workout_finish_btn")
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val minutes = elapsedSeconds / 60
                    val seconds = elapsedSeconds % 60
                    val timeStr = "${String.format("%02d", minutes)}:${String.format("%02d", seconds)}"

                    // Elapsed Workout Time Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .liquidGlass(shape = RoundedCornerShape(18.dp), elevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(LiquidTheme.iconOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Timer, contentDescription = null, tint = LiquidTheme.iconOrange, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("مدت تمرین", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                                Text(
                                    PersianNumberHelper.toPersianDigits(timeStr),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LiquidTheme.textPrimary
                                )
                            }
                        }
                    }

                    // Completed Sets Ratio Card
                    val totalSets = exercisesState.sumOf { it.sets.size }
                    val completedSets = exercisesState.sumOf { it.sets.count { s -> s.completed } }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .liquidGlass(shape = RoundedCornerShape(18.dp), elevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(LiquidTheme.iconGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = LiquidTheme.iconGreen, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("ست‌های انجام‌شده", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                                Text(
                                    "${PersianNumberHelper.toPersianDigits(completedSets)} / ${PersianNumberHelper.toPersianDigits(totalSets)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LiquidTheme.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Exercise cards
            itemsIndexed(exercisesState, key = { index, _ -> "ex_$index" }) { exIndex, exState ->
                val allCompleted = exState.sets.isNotEmpty() && exState.sets.all { it.completed }
                val isTimed = exState.exercise.isTimedExercise

                SolidGroupedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Exercise Header
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
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (allCompleted) SemanticSuccess else LiquidTheme.accentColor.copy(alpha = 0.14f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (allCompleted) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    } else {
                                        Text(
                                            text = PersianNumberHelper.toPersianDigits(exIndex + 1),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = LiquidTheme.accentColor
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = exState.exercise.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = LiquidTheme.textPrimary
                                    )

                                    if (isTimed) {
                                        Text(
                                            text = "حرکت زمان‌دار • تایمر معکوس و آلارم هوشمند",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LiquidTheme.iconOrange,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else if (exState.exercise.repsMax != null && exState.exercise.repsMax > exState.exercise.reps) {
                                        Text(
                                            text = "دامنه تکرار هدف: ${PersianNumberHelper.toPersianDigits(exState.exercise.reps)} الی ${PersianNumberHelper.toPersianDigits(exState.exercise.repsMax)} تکرار",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LiquidTheme.accentColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            if (exState.exercise.notes.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = LiquidTheme.surfaceElevated
                                ) {
                                    Text(
                                        text = exState.exercise.notes,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LiquidTheme.textSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // Sets Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ست", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                            if (isGlobalCustomMetrics) {
                                Text("شاخص‌های جلسه سفارشی", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary, modifier = Modifier.weight(2f), textAlign = TextAlign.Center)
                            } else if (isTimed) {
                                Text("مدت زمان", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("تایمر معکوس", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            } else {
                                Text("وزنه ($weightUnit)", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("تکرار", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            }
                            Text("ثبت ست", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
                        }

                        // Set rows
                        exState.sets.forEachIndexed { setIndex, setLog ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (setLog.completed) SemanticSuccess.copy(alpha = 0.12f) else LiquidTheme.surfaceElevated)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Set Number
                                Text(
                                    text = PersianNumberHelper.toPersianDigits(setLog.setNumber),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LiquidTheme.textPrimary,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = TextAlign.Center
                                )

                                if (isGlobalCustomMetrics) {
                                    // Custom metrics (weight, reps, time, ...) editable per set
                                    Column(
                                        modifier = Modifier
                                            .weight(2f)
                                            .padding(horizontal = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        setLog.customValues.forEach { (metricKey, metricVal) ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = metricKey,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = LiquidTheme.textSecondary,
                                                    modifier = Modifier.width(72.dp)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(LiquidTheme.surfaceSolid),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    BasicTextField(
                                                        value = metricVal,
                                                        onValueChange = { newVal ->
                                                            viewModel.updateCustomMetricValue(
                                                                exIndex, setIndex, metricKey,
                                                                PersianNumberHelper.toEnglishDigits(newVal).replace('٫', '.')
                                                            )
                                                        },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                            textAlign = TextAlign.Center,
                                                            fontWeight = FontWeight.Bold,
                                                            color = LiquidTheme.textPrimary
                                                        ),
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else if (isTimed) {
                                    // Timed duration label & adjuster
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "${PersianNumberHelper.toPersianDigits(setLog.timerSecondsRemaining)} ثانیه",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (setLog.isTimerActive) LiquidTheme.iconOrange else LiquidTheme.textPrimary
                                        )
                                    }

                                    // Play / Pause Timer Button with Pulse
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .liquidTapEffect {
                                                    viewModel.toggleExerciseTimer(exIndex, setIndex)
                                                },
                                            color = if (setLog.isTimerActive) LiquidTheme.iconOrange else LiquidTheme.surfaceSolid
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (setLog.isTimerActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                    contentDescription = "شروع تایمر",
                                                    tint = if (setLog.isTimerActive) onColorFor(LiquidTheme.iconOrange) else LiquidTheme.accentColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = if (setLog.isTimerActive) "توقف" else "شروع",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (setLog.isTimerActive) onColorFor(LiquidTheme.iconOrange) else LiquidTheme.accentColor
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Weight Input
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 6.dp)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(LiquidTheme.surfaceSolid),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // متن محلی تا تایپ اعشار (مثل «۲۲.») قطع نشود
                                        var weightText by remember(exIndex, setIndex, setLog.weight) {
                                            mutableStateOf(
                                                if (setLog.weight == 0.0) ""
                                                else if (setLog.weight % 1.0 == 0.0) setLog.weight.toLong().toString()
                                                else setLog.weight.toString()
                                            )
                                        }
                                        BasicTextField(
                                            value = weightText,
                                            onValueChange = { str ->
                                                val clean = PersianNumberHelper.toEnglishDigits(str)
                                                    .replace('٫', '.').replace(',', '.')
                                                weightText = clean
                                                val w = clean.toDoubleOrNull() ?: 0.0
                                                viewModel.updateSetValues(exIndex, setIndex, weight = w, reps = setLog.reps)
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                textAlign = TextAlign.Center,
                                                fontWeight = FontWeight.Bold,
                                                color = LiquidTheme.textPrimary
                                            ),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    // Reps Input
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 6.dp)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(LiquidTheme.surfaceSolid),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BasicTextField(
                                            value = if (setLog.reps == 0) "" else setLog.reps.toString(),
                                            onValueChange = { str ->
                                                val r = PersianNumberHelper.toEnglishDigits(str).toIntOrNull() ?: 0
                                                viewModel.updateSetValues(exIndex, setIndex, weight = setLog.weight, reps = r)
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                textAlign = TextAlign.Center,
                                                fontWeight = FontWeight.Bold,
                                                color = LiquidTheme.textPrimary
                                            ),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // Big Tick Button for Set Completion
                                Box(
                                    modifier = Modifier.width(56.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (setLog.completed) SemanticSuccess else LiquidTheme.surfaceSolid
                                            )
                                            .clickable {
                                                viewModel.completeSet(exIndex, setIndex, defaultRestSeconds = selectedDefaultRest)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "ثبت ست",
                                            tint = if (setLog.completed) Color.White else LiquidTheme.textSecondary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Full-Width Add Set Button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .liquidTapEffect { viewModel.addExtraSet(exIndex) },
                            shape = RoundedCornerShape(12.dp),
                            color = LiquidTheme.accentColor.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = LiquidTheme.accentColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "افزودن ست جدید",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LiquidTheme.accentColor
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "ریست و لغو این جلسه",
                        style = MaterialTheme.typography.bodySmall,
                        color = SemanticError,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { showCancelConfirmDialog = true }
                            .padding(8.dp)
                    )
                }
            }
        }
    }

    // Workout Settings & Rest Duration Dialog
    if (showWorkoutSettingsDialog) {
        LiquidAlertDialog(
            onDismissRequest = { showWorkoutSettingsDialog = false },
            title = "تنظیمات جلسه تمرین",
            message = "مدت زمان پیش‌فرض استراحت بین ست‌ها:",
            confirmText = "تأیید و ذخیره",
            dismissText = "بستن",
            onConfirm = {},
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "حالت شاخص‌های دلخواه",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = LiquidTheme.textPrimary
                            )
                            Text(
                                text = "ویرایش وزنه، تکرار و زمان هر ست به‌صورت فیلدهای جداگانه",
                                style = MaterialTheme.typography.labelSmall,
                                color = LiquidTheme.textSecondary
                            )
                        }
                        LiquidSwitch(
                            checked = isGlobalCustomMetrics,
                            onCheckedChange = { viewModel.toggleGlobalCustomMetrics() },
                            modifier = Modifier.testTag("toggle_custom_metrics_btn")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(30, 45, 60, 90, 120).forEach { sec ->
                            val isSel = selectedDefaultRest == sec
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .liquidTapEffect { selectedDefaultRest = sec },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) LiquidTheme.accentColor else LiquidTheme.surfaceElevated
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${PersianNumberHelper.toPersianDigits(sec)}ث",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) LiquidTheme.onAccent else LiquidTheme.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "لغو کامل تمرین امروز",
                        style = MaterialTheme.typography.bodySmall,
                        color = SemanticError,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable {
                                showWorkoutSettingsDialog = false
                                showCancelConfirmDialog = true
                            }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        )
    }

    // Finish Workout Confirmation Dialog
    if (showFinishConfirmDialog) {
        LiquidAlertDialog(
            onDismissRequest = { showFinishConfirmDialog = false },
            title = "پایان جلسه تمرینی",
            message = "آیا تمرین امروز را به پایان می‌رسانید؟ اطلاعات ست‌های انجام‌شده به همراه مدت زمان کل در تاریخچه ذخیره خواهد شد.",
            confirmText = "ثبت و پایان",
            dismissText = "ادامه تمرین",
            onConfirm = {
                viewModel.finishWorkout {
                    onWorkoutFinished()
                }
            }
        )
    }

    // Cancel Workout Confirmation Dialog
    if (showCancelConfirmDialog) {
        LiquidAlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = "ریست کامل تمرین",
            message = "آیا مطمئن هستید؟ تمامی ست‌های ثبت‌شده در این جلسه پاک شده و لغو می‌گردند.",
            confirmText = "ریست و خروج",
            dismissText = "انصراف",
            isDestructive = true,
            onConfirm = {
                viewModel.cancelAndResetWorkout()
                onTemporaryExit()
            }
        )
    }
}
