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
import androidx.compose.ui.graphics.Brush
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

// ایمپورت‌های مورد نیاز برای نمودار و رنگ‌ها
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.toArgb // 👈 این خط اضافه شد

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
        contentPadding = PaddingValues(bottom = 24.dp),
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
                                        iconTint = LiquidTheme.iconOrange,
                                        valueText = if (currentWeight != null) "${PersianNumberHelper.toPersianDigits(currentWeight)} ${userSettings.weightUnit}" else "وارد نشده",
                                        showDivider = true,
                                        showArrow = false
                                    )

                                    GroupedRow(
                                        title = "دور کمر",
                                        icon = Icons.Filled.Straighten,
                                        iconTint = LiquidTheme.iconCyan,
                                        valueText = if (latest?.waistCm != null) "${PersianNumberHelper.toPersianDigits(latest.waistCm)} cm" else "وارد نشده",
                                        showDivider = true,
                                        showArrow = false
                                    )

                                    GroupedRow(
                                        title = "دور بازو",
                                        icon = Icons.Filled.FitnessCenter,
                                        iconTint = LiquidTheme.iconRed,
                                        valueText = if (latest?.armsCm != null) "${PersianNumberHelper.toPersianDigits(latest.armsCm)} cm" else "وارد نشده",
                                        showDivider = true,
                                        showArrow = false
                                    )

                                    GroupedRow(
                                        title = "دور سینه",
                                        icon = Icons.Filled.Accessibility,
                                        iconTint = LiquidTheme.iconIndigo,
                                        valueText = if (latest?.chestCm != null) "${PersianNumberHelper.toPersianDigits(latest.chestCm)} cm" else "وارد نشده",
                                        showDivider = true,
                                        showArrow = false
                                    )

                                    GroupedRow(
                                        title = "دور ران",
                                        icon = Icons.Filled.DirectionsRun,
                                        iconTint = LiquidTheme.iconGreen,
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
                                            color = if (bmi in 18.5..24.9) SemanticSuccess else SemanticWarning,
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
                                        color = LiquidTheme.iconOrange
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
                                        color = LiquidTheme.iconGreen
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
                                        color = LiquidTheme.iconCyan
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
                                            Text("پروتئین", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.iconRed)
                                            Text(
                                                text = "${PersianNumberHelper.toPersianDigits(targetProtein)}g",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = LiquidTheme.textPrimary
                                            )
                                            Text("۲.۱g به ازای وزن", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("کربوهیدرات", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.iconOrange)
                                            Text(
                                                text = "${PersianNumberHelper.toPersianDigits(targetCarbs)}g",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = LiquidTheme.textPrimary
                                            )
                                            Text("منبع انرژی تمرین", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("چربی سالم", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.iconGreen)
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
                // Tab 2: Weight trend, BMI trend, custom chart (Y axis selectable)
                item {
                    val weightPoints = allMeasurements.reversed().mapNotNull { it.weightKg }
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        LiquidChartCard(
                            title = "روند وزن",
                            unit = userSettings.weightUnit,
                            values = weightPoints,
                            lineColor = LiquidTheme.accentColor,
                            emptyMessage = "حداقل ۲ بار ثبت وزن برای رسم نمودار نیاز است"
                        )
                    }
                }

                item {
                    val userH = userSettings.heightCm
                    val bmiPoints = if (userH != null && userH > 0) {
                        allMeasurements.reversed().mapNotNull { m ->
                            val w = m.weightKg ?: return@mapNotNull null
                            val hM = (m.heightCm ?: userH) / 100.0
                            w / (hM * hM)
                        }
                    } else emptyList()
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        LiquidChartCard(
                            title = "روند BMI",
                            unit = "BMI",
                            values = bmiPoints,
                            lineColor = SemanticSuccess,
                            emptyMessage = "برای نمایش روند BMI قد و وزن را ثبت کنید"
                        )
                    }
                }

                item {
                    var selectedYIndex by remember { mutableIntStateOf(0) }
                    val yLabels = listOf("وزن", "دور کمر", "دور بازو", "دور سینه")
                    val yUnits = listOf(userSettings.weightUnit, "cm", "cm", "cm")
                    val customPoints = allMeasurements.reversed().mapNotNull { m ->
                        when (selectedYIndex) {
                            1 -> m.waistCm
                            2 -> m.armsCm
                            3 -> m.chestCm
                            else -> m.weightKg
                        }
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidSegmentedControl(
                            items = yLabels,
                            selectedIndex = selectedYIndex,
                            onSelectIndex = { selectedYIndex = it }
                        )
                        LiquidChartCard(
                            title = "نمودار سفارشی: ${yLabels[selectedYIndex]}",
                            unit = yUnits[selectedYIndex],
                            values = customPoints,
                            lineColor = SemanticWarning,
                            emptyMessage = "داده کافی برای این متغیر ثبت نشده است"
                        )
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


@Composable
fun LiquidChartCard(
    title: String,
    unit: String,
    values: List<Double>,
    lineColor: Color,
    emptyMessage: String,
    modifier: Modifier = Modifier
) {
    SolidGroupedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = LiquidTheme.textSecondary
            )
            if (values.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = LiquidTheme.textSecondary
                    )
                }
            } else {
                val latestValue = values.last()
                val minV = values.minOrNull() ?: 0.0
                val maxV = values.maxOrNull() ?: 0.0
                Text(
                    text = "${PersianNumberHelper.formatDecimal(latestValue)} $unit",
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = lineColor
                )
                Text(
                    text = "کمترین: ${PersianNumberHelper.formatDecimal(minV)}  |  بیشترین: ${PersianNumberHelper.formatDecimal(maxV)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LiquidTheme.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                LiquidLineChart(
                    values = values,
                    lineColor = lineColor,
                    gridColor = LiquidTheme.dividerColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }
        }
    }
}

@Composable
fun LiquidLineChart(
    values: List<Double>,
    lineColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier
) {
    // در صفحه RTL، زمان از راست به چپ جلو می‌رود (جدیدترین مقدار سمت چپ)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val labelColor = LiquidTheme.textSecondary
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)

    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas

        val dataMin = values.minOrNull() ?: 0.0
        val dataMax = values.maxOrNull() ?: 1.0
        // حداقل بازه نمایش: نوسان‌های خیلی کوچک (مثلاً ۰٫۲ کیلو) بزرگ‌نمایی نشود
        val minSpan = maxOf(kotlin.math.abs(dataMax) * 0.06, 0.5)
        val span = maxOf(dataMax - dataMin, minSpan)
        val center = (dataMax + dataMin) / 2.0
        val minVal = center - span / 2.0 - span * 0.08
        val maxVal = center + span / 2.0 + span * 0.08
        val range = (maxVal - minVal).coerceAtLeast(0.0001)

        val padY = 12.dp.toPx()
        val chartH = size.height - padY * 2
        val stepX = size.width / (values.size - 1)

        fun xAt(index: Int): Float = if (isRtl) size.width - index * stepX else index * stepX

        // خطوط راهنما + برچسب عددی محور Y (بالا، وسط، پایین)
        for (i in 0..2) {
            val gy = padY + chartH * i / 2f
            drawLine(
                color = gridColor,
                start = Offset(0f, gy),
                end = Offset(size.width, gy),
                strokeWidth = 1.dp.toPx()
            )
            val labelValue = maxVal - (maxVal - minVal) * i / 2.0
            val layout = textMeasurer.measure(
                text = PersianNumberHelper.formatDecimal(labelValue),
                style = labelStyle
            )
            val lx = if (isRtl) size.width - layout.size.width else 0f
            // استفاده از drawIntoCanvas و nativeCanvas برای رندر متن روی Canvas
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    PersianNumberHelper.formatDecimal(labelValue),
                    lx,
                    (gy - layout.size.height - 2.dp.toPx()).coerceAtLeast(0f),
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = labelStyle.fontSize.toPx()
                    }
                )
            }
        }

        val linePath = Path()
        val points = values.mapIndexed { index, v ->
            val y = padY + chartH - (((v - minVal) / range) * chartH).toFloat()
            Offset(xAt(index), y)
        }
        points.forEachIndexed { index, pt ->
            if (index == 0) linePath.moveTo(pt.x, pt.y) else linePath.lineTo(pt.x, pt.y)
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.22f), Color.Transparent)
            )
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        points.forEachIndexed { index, pt ->
            val isLast = index == points.lastIndex
            drawCircle(
                color = lineColor,
                radius = if (isLast) 6.dp.toPx() else 3.5.dp.toPx(),
                center = pt
            )
        }
    }
}
