package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BodyMeasurement
import com.example.data.local.UserSettings
import com.example.data.local.WorkoutHistory
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.JalaliCalendar
import com.example.util.PersianNumberHelper

@Composable
fun AnalysisScreen(
    userSettings: UserSettings,
    allMeasurements: List<BodyMeasurement>,
    allHistories: List<WorkoutHistory>,
    onSaveMeasurement: (BodyMeasurement) -> Unit,
    onDeleteMeasurement: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: سایز و وزن, 1: ماشین‌حساب وزن و BMR, 2: نمودارها
    var showAddMeasurementDialog by remember { mutableStateOf(false) }

    val latest = allMeasurements.maxByOrNull { it.timestamp }
    val currentWeight = latest?.weightKg ?: userSettings.weightKg
    val currentHeight = latest?.heightCm ?: userSettings.heightCm

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analysis_screen"),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Inline Top Bar
        item {
            LiquidTopBar(
                title = "آنالیز بدن و بیومتریک",
                actions = {
                    if (selectedTab == 0) {
                        LiquidCircleButton(
                            icon = Icons.Filled.Add,
                            contentDescription = "ثبت سایز جدید",
                            onClick = { showAddMeasurementDialog = true }
                        )
                    }
                }
            )
        }

        // 2. Segmented Control
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                LiquidSegmentedControl(
                    items = listOf("سایز و ابعاد", "وزن ایده‌آل و BMR", "روند و نمودارها"),
                    selectedIndex = selectedTab,
                    onSelectIndex = { selectedTab = it }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // Tab 0: Size & Measurements Records
                if (allMeasurements.isEmpty() && currentWeight == null) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.Straighten,
                            title = "هنوز ابعاد یا وزنی ثبت نشده است",
                            description = "اولین ثبت ابعاد بدنی (وزن، دور کمر، بازو، سینه و ران) را انجام دهید تا پیشرفت تغییرات فیزیکی شما رهگیری شود.",
                            actionButtonText = "ثبت ابعاد جدید",
                            onActionClick = { showAddMeasurementDialog = true }
                        )
                    }
                } else {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LiquidSectionHeader(title = "آخرین سایزهای ثبت‌شده")

                                SolidGroupedCard {
                                    GroupedRow(
                                        title = "وزن کل بدن",
                                        icon = Icons.Filled.FitnessCenter,
                                        iconTint = SemanticIconOrange,
                                        valueText = if (currentWeight != null) "${PersianNumberHelper.toPersianDigits(currentWeight)} ${userSettings.weightUnit}" else "وارد نشده",
                                        showDivider = true,
                                        showArrow = false
                                    )

                                    GroupedRow(
                                        title = "دور کمر",
                                        icon = Icons.Filled.Straighten,
                                        iconTint = SemanticIconCyan,
                                        valueText = if (latest?.waistCm != null) "${PersianNumberHelper.toPersianDigits(latest.waistCm)} cm" else "وارد نشده",
                                        showDivider = true,
                                        showArrow = false
                                    )

                                    GroupedRow(
                                        title = "دور بازو",
                                        icon = Icons.Filled.FitnessCenter,
                                        iconTint = SemanticIconRed,
                                        valueText = if (latest?.armsCm != null) "${PersianNumberHelper.toPersianDigits(latest.armsCm)} cm" else "وارد نشده",
                                        showDivider = true,
                                        showArrow = false
                                    )

                                    GroupedRow(
                                        title = "دور سینه",
                                        icon = Icons.Filled.Accessibility,
                                        iconTint = SemanticIconIndigo,
                                        valueText = if (latest?.chestCm != null) "${PersianNumberHelper.toPersianDigits(latest.chestCm)} cm" else "وارد نشده",
                                        showDivider = true,
                                        showArrow = false
                                    )

                                    GroupedRow(
                                        title = "دور ران",
                                        icon = Icons.Filled.DirectionsRun,
                                        iconTint = SemanticIconGreen,
                                        valueText = if (latest?.thighCm != null) "${PersianNumberHelper.toPersianDigits(latest.thighCm)} cm" else "وارد نشده",
                                        showDivider = false,
                                        showArrow = false
                                    )
                                }
                            }
                        }
                    }

                    // Historical Measurements List
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LiquidSectionHeader(title = "سوابق ثبت‌های قبلی")

                                SolidGroupedCard {
                                    allMeasurements.sortedByDescending { it.timestamp }.forEachIndexed { index, m ->
                                        val jDate = JalaliCalendar.formatGregorianToPersian(m.date)
                                        val sub = buildString {
                                            if (m.weightKg != null) append("وزن: ${PersianNumberHelper.toPersianDigits(m.weightKg)} ")
                                            if (m.waistCm != null) append("• کمر: ${PersianNumberHelper.toPersianDigits(m.waistCm)} ")
                                            if (m.armsCm != null) append("• بازو: ${PersianNumberHelper.toPersianDigits(m.armsCm)}")
                                        }

                                        GroupedRow(
                                            title = jDate,
                                            subtitle = sub,
                                            icon = Icons.Filled.CalendarToday,
                                            iconTint = LiquidTheme.accentColor,
                                            showDivider = index < allMeasurements.size - 1,
                                            showArrow = false,
                                            trailingContent = {
                                                IconButton(
                                                    onClick = { onDeleteMeasurement(m.date) },
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

            1 -> {
                // Tab 1: Ideal Body Weight & BMR/TDEE Calculator with Large Numbers
                item {
                    val w = currentWeight
                    val h = currentHeight
                    val age = userSettings.age ?: 25
                    val gender = userSettings.gender

                    if (w == null || h == null) {
                        EmptyStateCard(
                            icon = Icons.Filled.Calculate,
                            title = "اطلاعات قد و وزن وارد نشده است",
                            description = "برای محاسبه خودکار وزن ایده‌آل، BMR و کالری مصرفی، ابتدا قد و وزن خود را در پروفایل یا ثبت سایز وارد کنید.",
                            actionButtonText = "ثبت قد و وزن",
                            onActionClick = { showAddMeasurementDialog = true }
                        )
                    } else {
                        val idealWeightMin = 18.5 * (h / 100.0) * (h / 100.0)
                        val idealWeightMax = 24.9 * (h / 100.0) * (h / 100.0)
                        val bmi = w / ((h / 100.0) * (h / 100.0))

                        val bmiStatus = when {
                            bmi < 18.5 -> "کمبود وزن (لاغر)"
                            bmi < 25.0 -> "وزن نرمال و ایده‌آل"
                            bmi < 30.0 -> "دارای اضافه وزن"
                            else -> "چاقی درجه ۱ یا بالاتر"
                        }

                        // Mifflin-St Jeor formula (Standard gold standard)
                        val bmr = (10.0 * w) + (6.25 * h) - (5.0 * age) + if (gender == "FEMALE") -161.0 else 5.0

                        // Physical Activity Level multiplier
                        val activityMultiplier = when (userSettings.activityLevel) {
                            "SEDENTARY" -> 1.2
                            "LIGHT" -> 1.375
                            "MODERATE" -> 1.55
                            "ACTIVE" -> 1.725
                            "VERY_ACTIVE", "ATHLETE" -> 1.9
                            else -> 1.4
                        }
                        val tdee = bmr * activityMultiplier

                        // Target Calories by Goal
                        val targetCalories = when (userSettings.fitnessGoal) {
                            "BULK" -> tdee + 400.0
                            "CUT" -> (tdee - 500.0).coerceAtLeast(bmr)
                            else -> tdee
                        }

                        // Estimated Body Fat % (US Navy formula if waist available, otherwise Deurenberg BMI)
                        val waist = latest?.waistCm
                        val neck = latest?.neckCm ?: (if (gender == "FEMALE") 33.0 else 38.0)
                        val bodyFat = if (waist != null && waist > neck) {
                            if (gender == "FEMALE") {
                                val hip = latest?.thighCm?.let { it * 1.5 } ?: (waist * 1.15)
                                (495.0 / (1.29579 - 0.35004 * kotlin.math.log10(waist + hip - neck) + 0.22100 * kotlin.math.log10(h)) - 450.0).coerceIn(12.0, 48.0)
                            } else {
                                (495.0 / (1.0324 - 0.19077 * kotlin.math.log10(waist - neck) + 0.15456 * kotlin.math.log10(h)) - 450.0).coerceIn(6.0, 45.0)
                            }
                        } else {
                            val genderFactor = if (gender == "FEMALE") 0.0 else 1.0
                            ((1.20 * bmi) + (0.23 * age) - (10.8 * genderFactor) - 5.4).coerceIn(7.0, 45.0)
                        }

                        // FFMI (Fat-Free Mass Index) - The bodybuilder gold standard
                        val leanMassKg = w * (1.0 - (bodyFat / 100.0))
                        val ffmi = (leanMassKg / ((h / 100.0) * (h / 100.0))) + 6.1 * (1.8 - (h / 100.0))
                        val ffmiStatus = when {
                            ffmi < 18.0 -> "پایین / غیرورزشکار"
                            ffmi < 20.0 -> "متوسط و طبیعی"
                            ffmi < 22.0 -> "عضلانی و ورزشکار"
                            ffmi < 24.5 -> "ورزشکار پیشرفته / نخبه"
                            else -> "حداکثر پتانسیل ژنتیکی طبیعی"
                        }

                        // Daily Macronutrients
                        val targetProtein = (w * 2.1).toInt()
                        val targetFat = (w * 0.9).toInt()
                        val targetCarbs = ((targetCalories - (targetProtein * 4 + targetFat * 9)) / 4.0).toInt().coerceAtLeast(80)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Card 1: BMI & Ideal Weight
                            SolidGroupedCard {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("شاخص توده بدنی (BMI) و وضعیت وزن", style = MaterialTheme.typography.titleMedium, color = LiquidTheme.textSecondary)
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = PersianNumberHelper.toPersianDigits(String.format(java.util.Locale.US, "%.1f", bmi)),
                                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = LiquidTheme.accentColor
                                        )
                                        Text(
                                            text = "• $bmiStatus",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (bmi in 18.5..24.9) SemanticIconGreen else SemanticIconOrange,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "محدوده وزن ایده‌آل سلامت: ${PersianNumberHelper.toPersianDigits(String.format(java.util.Locale.US, "%.1f", idealWeightMin))} تا ${PersianNumberHelper.toPersianDigits(String.format(java.util.Locale.US, "%.1f", idealWeightMax))} کیلوگرم",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LiquidTheme.textSecondary
                                    )
                                }
                            }

                            // Card 2: BMR & TDEE
                            SolidGroupedCard {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("سوخت‌وساز پایه روزانه (BMR میفلین-سنت جور)", style = MaterialTheme.typography.titleMedium, color = LiquidTheme.textSecondary)
                                    Text(
                                        text = "${PersianNumberHelper.toPersianDigits(bmr.toInt())} kcal",
                                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SemanticIconOrange
                                    )
                                    Text(
                                        text = "کالری مورد نیاز بیولوژیک برای حفظ حیات بدون احتساب فعالیت فیزیکی",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LiquidTheme.textSecondary
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(0.6.dp).background(LiquidTheme.dividerColor))
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text("کالری مصرفی کل با ضریب فعالیت (TDEE)", style = MaterialTheme.typography.titleMedium, color = LiquidTheme.textSecondary)
                                    Text(
                                        text = "${PersianNumberHelper.toPersianDigits(tdee.toInt())} kcal",
                                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SemanticIconGreen
                                    )
                                    Text(
                                        text = "کالری هدف روزانه برای هدف شما (${if (userSettings.fitnessGoal == "BULK") "افزایش حجم" else if (userSettings.fitnessGoal == "CUT") "کات و چربی‌سوزی" else "تثبیت"}): ${PersianNumberHelper.toPersianDigits(targetCalories.toInt())} کیلوکالری",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = LiquidTheme.accentColor
                                    )
                                }
                            }

                            // Card 3: Body Fat & FFMI
                            SolidGroupedCard {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("ترکیب بدنی و آنالیز عضلانی (Body Fat & FFMI)", style = MaterialTheme.typography.titleMedium, color = LiquidTheme.textSecondary)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("درصد چربی تقریبی", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                                            Text(
                                                text = "${PersianNumberHelper.toPersianDigits(String.format(java.util.Locale.US, "%.1f", bodyFat))}٪",
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = LiquidTheme.textPrimary
                                            )
                                        }

                                        Column {
                                            Text("شاخص توده بدون چربی (FFMI)", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                                            Text(
                                                text = PersianNumberHelper.toPersianDigits(String.format(java.util.Locale.US, "%.1f", ffmi)),
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = LiquidTheme.accentColor
                                            )
                                        }
                                    }

                                    Text(
                                        text = "وضعیت عضلانی FFMI: $ffmiStatus",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SemanticIconCyan
                                    )
                                }
                            }

                            // Card 4: Daily Macros Breakdown
                            SolidGroupedCard {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("درشت‌مغذی‌های پیشنهادی روزانه", style = MaterialTheme.typography.titleMedium, color = LiquidTheme.textSecondary)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("پروتئین", style = MaterialTheme.typography.labelSmall, color = SemanticIconRed)
                                            Text(
                                                text = "${PersianNumberHelper.toPersianDigits(targetProtein)}g",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = LiquidTheme.textPrimary
                                            )
                                            Text("۲.۱g به ازای وزن", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("کربوهیدرات", style = MaterialTheme.typography.labelSmall, color = SemanticIconOrange)
                                            Text(
                                                text = "${PersianNumberHelper.toPersianDigits(targetCarbs)}g",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = LiquidTheme.textPrimary
                                            )
                                            Text("منبع انرژی تمرین", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("چربی سالم", style = MaterialTheme.typography.labelSmall, color = SemanticIconGreen)
                                            Text(
                                                text = "${PersianNumberHelper.toPersianDigits(targetFat)}g",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = LiquidTheme.textPrimary
                                            )
                                            Text("تنظیم هورمونی", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Tab 2: Charts inside Solid Card with big key number and accent line
                item {
                    val weights = allMeasurements.mapNotNull { it.weightKg }
                    val currentVal = weights.lastOrNull() ?: currentWeight ?: 0.0

                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SolidGroupedCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("روند تغییرات وزن", style = MaterialTheme.typography.titleMedium, color = LiquidTheme.textSecondary)
                                Text(
                                    text = "${PersianNumberHelper.toPersianDigits(String.format("%.1f", currentVal))} kg",
                                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = LiquidTheme.accentColor
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Interactive Line Canvas Chart
                                val accent = LiquidTheme.accentColor
                                val divider = LiquidTheme.dividerColor

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        if (weights.size >= 2) {
                                            val minW = weights.minOrNull() ?: 0.0
                                            val maxW = weights.maxOrNull() ?: 100.0
                                            val range = (maxW - minW).coerceAtLeast(1.0)

                                            val stepX = size.width / (weights.size - 1)
                                            val path = Path()

                                            weights.forEachIndexed { i, w ->
                                                val x = i * stepX
                                                val y = size.height - (((w - minW) / range) * (size.height - 20.dp.toPx())).toFloat() - 10.dp.toPx()
                                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)

                                                // Point dot
                                                drawCircle(color = accent, radius = 4.dp.toPx(), center = Offset(x, y))
                                            }

                                            drawPath(
                                                path = path,
                                                color = accent,
                                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                        } else {
                                            // Guideline
                                            drawLine(
                                                color = divider,
                                                start = Offset(0f, size.height / 2),
                                                end = Offset(size.width, size.height / 2),
                                                strokeWidth = 2.dp.toPx()
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = if (weights.size < 2) "حداقل ۲ بار ثبت وزن برای رسم نمودار خطی لازم است." else "نمودار بر اساس تاریخ‌های ثبت سوابق",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LiquidTheme.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Measurement Dialog
    if (showAddMeasurementDialog) {
        var date by remember { mutableStateOf(JalaliCalendar.getTodayGregorian()) }
        var weightStr by remember { mutableStateOf(userSettings.weightKg?.toString() ?: "") }
        var heightStr by remember { mutableStateOf(userSettings.heightCm?.toString() ?: "") }
        var waistStr by remember { mutableStateOf("") }
        var armsStr by remember { mutableStateOf("") }
        var chestStr by remember { mutableStateOf("") }
        var thighStr by remember { mutableStateOf("") }
        var neckStr by remember { mutableStateOf("") }

        LiquidAlertDialog(
            onDismissRequest = { showAddMeasurementDialog = false },
            title = "ثبت سایز و وزن جدید",
            message = "ابعاد و وزن جدید خود را وارد کنید:",
            confirmText = "ذخیره در سوابق",
            dismissText = "انصراف",
            onConfirm = {
                val m = BodyMeasurement(
                    date = date,
                    heightCm = heightStr.toDoubleOrNull(),
                    weightKg = weightStr.toDoubleOrNull(),
                    neckCm = neckStr.toDoubleOrNull(),
                    waistCm = waistStr.toDoubleOrNull(),
                    chestCm = chestStr.toDoubleOrNull(),
                    armsCm = armsStr.toDoubleOrNull(),
                    thighCm = thighStr.toDoubleOrNull(),
                    timestamp = System.currentTimeMillis()
                )
                onSaveMeasurement(m)
            },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiquidInputField(
                            value = weightStr,
                            onValueChange = { weightStr = it },
                            label = "وزن (kg)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        LiquidInputField(
                            value = heightStr,
                            onValueChange = { heightStr = it },
                            label = "قد (cm)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiquidInputField(
                            value = waistStr,
                            onValueChange = { waistStr = it },
                            label = "دور کمر (cm)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        LiquidInputField(
                            value = armsStr,
                            onValueChange = { armsStr = it },
                            label = "دور بازو (cm)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiquidInputField(
                            value = chestStr,
                            onValueChange = { chestStr = it },
                            label = "دور سینه (cm)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        LiquidInputField(
                            value = thighStr,
                            onValueChange = { thighStr = it },
                            label = "دور ران (cm)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        )
    }
}
