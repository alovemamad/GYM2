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
import androidx.compose.runtime.saveable.rememberSaveable
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

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection

@Composable
fun AnalysisScreen(
    userSettings: UserSettings,
    allMeasurements: List<BodyMeasurement>,
    allHistories: List<WorkoutHistory>,
    onSaveMeasurement: (BodyMeasurement) -> Unit,
    onDeleteMeasurement: (String) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: سایز و وزن, 1: ماشین‌حساب وزن و BMR, 2: نمودارها
    var showAddMeasurementDialog by remember { mutableStateOf(false) }

    val latest = remember(allMeasurements) {
        allMeasurements.maxWithOrNull(compareBy<BodyMeasurement> { it.date }.thenBy { it.timestamp })
    }
    val orderedMeasurements = remember(allMeasurements) {
        allMeasurements.sortedWith(compareBy<BodyMeasurement> { it.date }.thenBy { it.timestamp })
    }
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
                                    allMeasurements
                                        .sortedWith(compareByDescending<BodyMeasurement> { it.date }.thenByDescending { it.timestamp })
                                        .forEachIndexed { index, m ->
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

                    if (w == null || h == null || w <= 0.0 || h <= 0.0) {
                        EmptyStateCard(
                            icon = Icons.Filled.Calculate,
                            title = "اطلاعات قد و وزن وارد نشده است",
                            description = "برای محاسبه خودکار وزن ایده‌آل، BMR و کالری مصرفی، ابتدا قد و وزن خود را در پروفایل یا ثبت سایز وارد کنید.",
                            actionButtonText = "ثبت قد و وزن",
                            onActionClick = { showAddMeasurementDialog = true }
                        )
                    } else {
                        val weightInKg = if (userSettings.weightUnit == "lb") w * 0.45359237 else w
                        val idealWeightMin = 18.5 * (h / 100.0) * (h / 100.0)
                        val idealWeightMax = 24.9 * (h / 100.0) * (h / 100.0)
                        val bmi = weightInKg / ((h / 100.0) * (h / 100.0))

                        val bmiStatus = when {
                            bmi < 18.5 -> "کمبود وزن (لاغر)"
                            bmi < 25.0 -> "وزن نرمال و ایده‌آل"
                            bmi < 30.0 -> "دارای اضافه وزن"
                            else -> "چاقی درجه ۱ یا بالاتر"
                        }

                        // Mifflin-St Jeor formula (Standard gold standard)
                        val bmr = (10.0 * weightInKg) + (6.25 * h) - (5.0 * age) + if (gender == "FEMALE") -161.0 else 5.0

                        // Physical Activity Level multiplier
                        val activityMultiplier = when (userSettings.activityLevel) {
                            "SEDENTARY" -> 1.2
                            "LIGHT", "MODERATE" -> 1.375
                            "ACTIVE" -> 1.55
                            "ATHLETE" -> 1.725
                            "VERY_ACTIVE" -> 1.9
                            else -> 1.375
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
                        val deurenbergFallback = ((1.20 * bmi) + (0.23 * age) - (10.8 * if (gender == "FEMALE") 0.0 else 1.0) - 5.4).coerceIn(7.0, 45.0)
                        val bodyFat = if (waist != null && waist > neck + 1.0) {
                            val rawNavy = if (gender == "FEMALE") {
                                val hip = latest?.thighCm?.takeIf { it > 0.0 }?.let { it * 1.5 } ?: (waist * 1.15)
                                val circ = waist + hip - neck
                                if (circ > 1.0) {
                                    val denom = 1.29579 - 0.35004 * kotlin.math.log10(circ) + 0.22100 * kotlin.math.log10(h)
                                    if (denom > 0.1) (495.0 / denom) - 450.0 else Double.NaN
                                } else Double.NaN
                            } else {
                                val diff = waist - neck
                                if (diff > 1.0) {
                                    val denom = 1.0324 - 0.19077 * kotlin.math.log10(diff) + 0.15456 * kotlin.math.log10(h)
                                    if (denom > 0.1) (495.0 / denom) - 450.0 else Double.NaN
                                } else Double.NaN
                            }
                            if (rawNavy.isFinite()) {
                                rawNavy.coerceIn(if (gender == "FEMALE") 12.0 else 6.0, 48.0)
                            } else {
                                deurenbergFallback
                            }
                        } else {
                            deurenbergFallback
                        }

                        // FFMI (Fat-Free Mass Index) - The bodybuilder gold standard
                        val leanMassKg = weightInKg * (1.0 - (bodyFat / 100.0))
                        val ffmi = (leanMassKg / ((h / 100.0) * (h / 100.0))) + 6.1 * (1.8 - (h / 100.0))
                        val ffmiStatus = when {
                            ffmi < 18.0 -> "پایین / غیرورزشکار"
                            ffmi < 20.0 -> "متوسط و طبیعی"
                            ffmi < 22.0 -> "عضلانی و ورزشکار"
                            ffmi < 24.5 -> "ورزشکار پیشرفته / نخبه"
                            else -> "حداکثر پتانسیل ژنتیکی طبیعی"
                        }

                        // Daily Macronutrients
                        val targetProtein = (weightInKg * 2.1).toInt()
                        val targetFat = (weightInKg * 0.9).toInt()
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
                // Tab 2: Weight trend, BMI trend, custom chart (chronological across all recorded days)
                item {
                    val weightPoints = remember(orderedMeasurements) {
                        orderedMeasurements.mapNotNull { m ->
                            m.weightKg?.let { w -> ChartDataBuilder.toChartPoint(m.date, w) }
                        }
                    }
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        LiquidChartCard(
                            title = "روند وزن",
                            unit = userSettings.weightUnit,
                            points = weightPoints,
                            lineColor = LiquidTheme.accentColor,
                            emptyMessage = "حداقل ۲ روز ثبت وزن برای رسم نمودار نیاز است"
                        )
                    }
                }

                item {
                    val userH = userSettings.heightCm
                    val bmiPoints = remember(orderedMeasurements, userH, userSettings.weightUnit) {
                        orderedMeasurements.mapNotNull { m ->
                            val rawW = m.weightKg ?: return@mapNotNull null
                            val wKg = if (userSettings.weightUnit == "lb") rawW * 0.45359237 else rawW
                            val hVal = m.heightCm ?: userH ?: return@mapNotNull null
                            if (wKg <= 0.0 || hVal <= 0.0) return@mapNotNull null
                            val hM = hVal / 100.0
                            ChartDataBuilder.toChartPoint(m.date, wKg / (hM * hM))
                        }
                    }
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        LiquidChartCard(
                            title = "روند BMI",
                            unit = "BMI",
                            points = bmiPoints,
                            lineColor = SemanticSuccess,
                            emptyMessage = "برای نمایش روند BMI قد و وزن را ثبت کنید"
                        )
                    }
                }

                item {
                    var selectedYIndex by remember { mutableIntStateOf(0) }
                    val yLabels = listOf("وزن", "دور کمر", "دور بازو", "دور سینه")
                    val yUnits = listOf(userSettings.weightUnit, "cm", "cm", "cm")
                    val customPoints = remember(orderedMeasurements, selectedYIndex) {
                        orderedMeasurements.mapNotNull { m ->
                            val rawVal = when (selectedYIndex) {
                                1 -> m.waistCm
                                2 -> m.armsCm
                                3 -> m.chestCm
                                else -> m.weightKg
                            }
                            rawVal?.let { v -> ChartDataBuilder.toChartPoint(m.date, v) }
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
                            points = customPoints,
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
        val todayStr = remember { JalaliCalendar.getTodayGregorian() }
        val pastDays = remember {
            val cal = java.util.Calendar.getInstance()
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            (0..13).map { offset ->
                val c = (cal.clone() as java.util.Calendar).apply {
                    add(java.util.Calendar.DAY_OF_YEAR, -offset)
                }
                sdf.format(c.time)
            }
        }
        var date by remember { mutableStateOf(todayStr) }
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
            message = "تاریخ و ابعاد بدنی خود را وارد کنید:",
            confirmText = "ذخیره در سوابق",
            dismissText = "انصراف",
            onConfirm = {
                val parsedWeight = weightStr.toDoubleOrNull()?.takeIf { it > 0.0 }
                val parsedHeight = heightStr.toDoubleOrNull()?.takeIf { it > 0.0 }
                val parsedNeck = neckStr.toDoubleOrNull()?.takeIf { it > 0.0 }
                val parsedWaist = waistStr.toDoubleOrNull()?.takeIf { it > 0.0 }
                val parsedChest = chestStr.toDoubleOrNull()?.takeIf { it > 0.0 }
                val parsedArms = armsStr.toDoubleOrNull()?.takeIf { it > 0.0 }
                val parsedThigh = thighStr.toDoubleOrNull()?.takeIf { it > 0.0 }
                if (listOf(parsedWeight, parsedHeight, parsedNeck, parsedWaist, parsedChest, parsedArms, parsedThigh).any { it != null }) {
                    val m = BodyMeasurement(
                        date = date,
                        heightCm = parsedHeight,
                        weightKg = parsedWeight,
                        neckCm = parsedNeck,
                        waistCm = parsedWaist,
                        chestCm = parsedChest,
                        armsCm = parsedArms,
                        thighCm = parsedThigh,
                        timestamp = System.currentTimeMillis()
                    )
                    onSaveMeasurement(m)
                }
            },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "تاریخ ثبت: ${JalaliCalendar.formatGregorianToPersian(date)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = LiquidTheme.accentColor
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(pastDays) { gDate ->
                            val isSel = date == gDate
                            val jDate = JalaliCalendar.gregorianToJalali(gDate)
                            val label = if (gDate == todayStr) "امروز" else PersianNumberHelper.toPersianDigits("${jDate.month}/${jDate.day}")
                            Surface(
                                modifier = Modifier
                                    .height(32.dp)
                                    .liquidTapEffect { date = gDate },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) LiquidTheme.accentColor else LiquidTheme.surfaceElevated
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) LiquidTheme.onAccent else LiquidTheme.textPrimary
                                    )
                                }
                            }
                        }
                    }

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


data class ChartDataPoint(
    val dateGregorian: String,
    val shortDateLabel: String,
    val fullDateLabel: String,
    val value: Double
)

object ChartDataBuilder {
    fun toChartPoint(dateGregorian: String, value: Double): ChartDataPoint {
        val jDate = JalaliCalendar.gregorianToJalali(dateGregorian)
        return ChartDataPoint(
            dateGregorian = dateGregorian,
            shortDateLabel = PersianNumberHelper.toPersianDigits("${jDate.month}/${jDate.day}"),
            fullDateLabel = jDate.toFormattedString(withMonthName = true),
            value = value
        )
    }
}

@Composable
fun LiquidChartCard(
    title: String,
    unit: String,
    points: List<ChartDataPoint>,
    lineColor: Color,
    emptyMessage: String,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }

    SolidGroupedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = LiquidTheme.textSecondary
                )
                if (points.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = lineColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "${PersianNumberHelper.toPersianDigits(points.size)} روز ثبت‌شده",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = lineColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (points.size < 2) {
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
                val activePoint = selectedIndex?.let { points.getOrNull(it) } ?: points.last()
                val minV = points.minOf { it.value }
                val maxV = points.maxOf { it.value }
                val diff = points.last().value - points.first().value
                val diffPrefix = if (diff > 0) "+" else ""

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "${PersianNumberHelper.formatDecimal(activePoint.value)} $unit",
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 34.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = lineColor
                        )
                        Text(
                            text = if (selectedIndex != null) {
                                "ثبت شده در ${activePoint.fullDateLabel}"
                            } else {
                                "آخرین ثبت (${activePoint.fullDateLabel})"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = LiquidTheme.textSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LiquidTheme.surfaceElevated
                    ) {
                        Text(
                            text = "تغییر کل: $diffPrefix${PersianNumberHelper.formatDecimal(diff)} $unit",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = LiquidTheme.textPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Text(
                    text = "کمترین: ${PersianNumberHelper.formatDecimal(minV)}  |  بیشترین: ${PersianNumberHelper.formatDecimal(maxV)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LiquidTheme.textSecondary
                )

                LiquidLineChart(
                    points = points,
                    selectedIndex = selectedIndex,
                    onSelectPoint = { idx ->
                        selectedIndex = if (selectedIndex == idx) null else idx
                    },
                    lineColor = lineColor,
                    gridColor = LiquidTheme.dividerColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(195.dp)
                )

                // ردیف تمام روزهای ثبت‌شده تا کاربر هر چند روز که ثبت کرده را مستقیماً ببیند و انتخاب کند
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(points.indices.toList().reversed()) { idx ->
                        val p = points[idx]
                        val isSel = selectedIndex == idx || (selectedIndex == null && idx == points.lastIndex)
                        Surface(
                            modifier = Modifier
                                .height(36.dp)
                                .liquidTapEffect {
                                    selectedIndex = if (selectedIndex == idx) null else idx
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) lineColor.copy(alpha = 0.20f) else LiquidTheme.surfaceElevated
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${p.shortDateLabel}: ${PersianNumberHelper.formatDecimal(p.value)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) lineColor else LiquidTheme.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidLineChart(
    points: List<ChartDataPoint>,
    selectedIndex: Int?,
    onSelectPoint: (Int) -> Unit,
    lineColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier
) {
    // در صفحه RTL، زمان از راست به چپ جلو می‌رود (جدیدترین مقدار سمت چپ)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val primaryTextColor = LiquidTheme.textPrimary
    val secondaryTextColor = LiquidTheme.textSecondary
    val textMeasurer = rememberTextMeasurer()
    val valueLabelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
    )
    val dateLabelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )

    val scrollState = rememberScrollState()
    LaunchedEffect(points.size, scrollState.maxValue) {
        if (points.size > 6 && scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val minPointSpacing = 58.dp
        val contentWidth = maxOf(maxWidth, minPointSpacing * points.size.coerceAtLeast(2))
        val density = LocalDensity.current
        val contentWidthPx = with(density) { contentWidth.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width(contentWidth)
                    .fillMaxHeight()
                    .pointerInput(points, isRtl, contentWidthPx) {
                        detectTapGestures { tapOffset ->
                            if (points.size < 2) return@detectTapGestures
                            val sidePad = 28.dp.toPx()
                            val usableW = (size.width - sidePad * 2).coerceAtLeast(1f)
                            val stepX = usableW / (points.size - 1)
                            var bestIdx = 0
                            var bestDist = Float.MAX_VALUE
                            points.indices.forEach { idx ->
                                val px = if (isRtl) size.width - sidePad - idx * stepX else sidePad + idx * stepX
                                val dist = kotlin.math.abs(tapOffset.x - px)
                                if (dist < bestDist) {
                                    bestDist = dist
                                    bestIdx = idx
                                }
                            }
                            if (bestDist <= 42.dp.toPx()) {
                                onSelectPoint(bestIdx)
                            }
                        }
                    }
            ) {
                if (points.size < 2) return@Canvas

                val dataMin = points.minOf { it.value }
                val dataMax = points.maxOf { it.value }
                val minSpan = maxOf(kotlin.math.abs(dataMax) * 0.06, 0.5)
                val span = maxOf(dataMax - dataMin, minSpan)
                val center = (dataMax + dataMin) / 2.0
                val minVal = center - span / 2.0 - span * 0.10
                val maxVal = center + span / 2.0 + span * 0.10
                val range = (maxVal - minVal).coerceAtLeast(0.0001)

                val topPad = 26.dp.toPx()
                val bottomPad = 28.dp.toPx()
                val sidePad = 28.dp.toPx()
                val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)
                val usableW = (size.width - sidePad * 2).coerceAtLeast(1f)
                val stepX = usableW / (points.size - 1)

                fun xAt(index: Int): Float = if (isRtl) size.width - sidePad - index * stepX else sidePad + index * stepX

                // خطوط راهنمای افقی (بالا، وسط، پایین)
                for (i in 0..2) {
                    val gy = topPad + chartH * i / 2f
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, gy),
                        end = Offset(size.width, gy),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val coords = points.mapIndexed { index, pt ->
                    val y = topPad + chartH - (((pt.value - minVal) / range) * chartH).toFloat()
                    Offset(xAt(index), y)
                }

                val linePath = Path()
                coords.forEachIndexed { index, pt ->
                    if (index == 0) linePath.moveTo(pt.x, pt.y) else linePath.lineTo(pt.x, pt.y)
                }

                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(coords.last().x, topPad + chartH)
                    lineTo(coords.first().x, topPad + chartH)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.24f), Color.Transparent),
                        startY = topPad,
                        endY = topPad + chartH
                    )
                )
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // خط عمودی نشانگر برای نقطه انتخاب‌شده
                val highlightedIdx = selectedIndex ?: points.lastIndex
                val hiPt = coords[highlightedIdx]
                drawLine(
                    color = lineColor.copy(alpha = 0.35f),
                    start = Offset(hiPt.x, topPad),
                    end = Offset(hiPt.x, topPad + chartH),
                    strokeWidth = 1.5.dp.toPx()
                )

                // رسم نقاط، برچسب عددی هر روز و تاریخ جلالی محور افقی
                coords.forEachIndexed { index, pt ->
                    val isHighlighted = index == highlightedIdx
                    if (isHighlighted) {
                        drawCircle(
                            color = lineColor.copy(alpha = 0.24f),
                            radius = 10.dp.toPx(),
                            center = pt
                        )
                    }
                    drawCircle(
                        color = lineColor,
                        radius = if (isHighlighted) 5.5.dp.toPx() else 4.dp.toPx(),
                        center = pt
                    )

                    // مقدار عددی دقیق بالای (یا پایینِ) هر نقطه از تمام روزها
                    val valText = PersianNumberHelper.formatDecimal(points[index].value)
                    val valLayout = textMeasurer.measure(
                        text = valText,
                        style = valueLabelStyle.copy(
                            color = if (isHighlighted) lineColor else primaryTextColor
                        )
                    )
                    val preferredAboveY = pt.y - valLayout.size.height - 6.dp.toPx()
                    val valY = if (preferredAboveY >= 2.dp.toPx()) {
                        preferredAboveY
                    } else {
                        (pt.y + 8.dp.toPx()).coerceAtMost(topPad + chartH - valLayout.size.height)
                    }
                    val valX = (pt.x - valLayout.size.width / 2f).coerceIn(2f, (size.width - valLayout.size.width - 2f).coerceAtLeast(2f))
                    drawText(
                        textLayoutResult = valLayout,
                        topLeft = Offset(valX, valY)
                    )

                    // تاریخ هر روز روی محور افقی پایین نمودار
                    val dateLayout = textMeasurer.measure(
                        text = points[index].shortDateLabel,
                        style = dateLabelStyle.copy(
                            color = if (isHighlighted) lineColor else secondaryTextColor
                        )
                    )
                    val dateX = (pt.x - dateLayout.size.width / 2f).coerceIn(2f, (size.width - dateLayout.size.width - 2f).coerceAtLeast(2f))
                    val dateY = topPad + chartH + 6.dp.toPx()
                    drawText(
                        textLayoutResult = dateLayout,
                        topLeft = Offset(dateX, dateY)
                    )
                }
            }
        }
    }
}
