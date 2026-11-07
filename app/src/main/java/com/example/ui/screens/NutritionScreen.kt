package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.JalaliCalendar
import com.example.util.PersianNumberHelper

@Composable
fun NutritionScreen(
    userSettings: UserSettings,
    selectedDateGregorian: String,
    onSelectDate: (String) -> Unit,
    datesWithNutrition: List<String>,
    currentNutritionLog: NutritionLog?,
    allSupplements: List<Supplement>,
    selectedDateSupplementIntakes: List<SupplementIntakeHistory>,
    allSupplementIntakes: List<SupplementIntakeHistory>,
    onSaveMealsLog: (String, String, String, String, String, String) -> Unit,
    onAddWater: (Int, String) -> Unit,
    onResetWater: (String) -> Unit,
    onSaveSupplement: (Supplement) -> Unit,
    onDeleteSupplement: (Supplement) -> Unit,
    onToggleSupplementActive: (Supplement) -> Unit,
    onToggleSupplementIntake: (Supplement, Boolean, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: وعده‌ها و آب, 1: مکمل‌ها, 2: رژیم هوشمند

    var breakfast by remember(currentNutritionLog) { mutableStateOf(currentNutritionLog?.breakfast ?: "") }
    var lunch by remember(currentNutritionLog) { mutableStateOf(currentNutritionLog?.lunch ?: "") }
    var dinner by remember(currentNutritionLog) { mutableStateOf(currentNutritionLog?.dinner ?: "") }
    var snacks by remember(currentNutritionLog) { mutableStateOf(currentNutritionLog?.snacks ?: "") }
    var dailyNotes by remember(currentNutritionLog) { mutableStateOf(currentNutritionLog?.dailyNotes ?: "") }

    var supplementToEdit by remember { mutableStateOf<Supplement?>(null) }
    var showSupplementDialog by remember { mutableStateOf(false) }
    var showIntakeHistoryDialog by remember { mutableStateOf(false) }
    var supplementToDelete by remember { mutableStateOf<Supplement?>(null) }

    val todayGregorian = remember { JalaliCalendar.getTodayGregorian() }

    // Smart diet calculation
    val weight = userSettings.weightKg ?: 75.0
    val height = userSettings.heightCm ?: 175.0
    val age = userSettings.age ?: 25
    val bmr = (10 * weight) + (6.25 * height) - (5 * age) + if (userSettings.gender == "FEMALE") -161 else 5
    val multiplier = when (userSettings.activityLevel) {
        "SEDENTARY" -> 1.2
        "ACTIVE" -> 1.55
        "ATHLETE" -> 1.725
        else -> 1.375
    }
    val tdee = bmr * multiplier
    val targetCalories = when (userSettings.fitnessGoal) {
        "BULK" -> tdee + 450
        "CUT" -> (tdee - 450).coerceAtLeast(1200.0)
        else -> tdee
    }
    val targetProteinGrams = when (userSettings.fitnessGoal) {
        "BULK" -> weight * 2.1
        "CUT" -> weight * 2.3
        else -> weight * 1.9
    }
    val targetFatGrams = (targetCalories * 0.25) / 9.0
    val targetCarbsGrams = (targetCalories - (targetProteinGrams * 4 + targetFatGrams * 9)).coerceAtLeast(0.0) / 4.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Inline Liquid Top Bar
        item {
            LiquidTopBar(
                title = "تغذیه، آب و مکمل‌ها",
                navigationIcon = {
                    if (selectedTab == 1) {
                        LiquidCircleButton(
                            icon = Icons.Filled.History,
                            contentDescription = "سوابق مکمل‌ها",
                            onClick = { showIntakeHistoryDialog = true }
                        )
                    }
                },
                actions = {
                    if (selectedTab == 1) {
                        LiquidCircleButton(
                            icon = Icons.Filled.Add,
                            contentDescription = "افزودن مکمل",
                            onClick = {
                                supplementToEdit = null
                                showSupplementDialog = true
                            }
                        )
                    }
                }
            )
        }

        // 2. Jalali Date Chips Selector
        item {
            val daysList = remember {
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, -5)
                val list = mutableListOf<String>()
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                for (i in 0..10) {
                    list.add(sdf.format(cal.time))
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
                list
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(daysList) { gDate ->
                    val isSelected = gDate == selectedDateGregorian
                    val isToday = gDate == todayGregorian
                    val hasLog = datesWithNutrition.contains(gDate)
                    val jDate = JalaliCalendar.gregorianToJalali(gDate)

                    Surface(
                        modifier = Modifier
                            .height(58.dp)
                            .width(52.dp)
                            .liquidTapEffect { onSelectDate(gDate) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) LiquidTheme.accentColor else LiquidTheme.surfaceSolid
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isToday) "امروز" else JalaliCalendar.formatWeekdayShort(gDate),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White.copy(alpha = 0.85f) else LiquidTheme.textSecondary
                            )
                            Text(
                                text = PersianNumberHelper.toPersianDigits(jDate.day),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else LiquidTheme.textPrimary
                            )
                            if (hasLog && !isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(LiquidTheme.accentColor)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Segmented Control
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                LiquidSegmentedControl(
                    items = listOf("وعده‌ها و آب", "مکمل‌ها", "رژیم و ماکروها"),
                    selectedIndex = selectedTab,
                    onSelectIndex = { selectedTab = it }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                // Tab 0: Meals & Water
                item {
                    val currentWater = currentNutritionLog?.waterIntakeMl ?: 0
                    val goalWater = userSettings.waterGoalMl.coerceAtLeast(1000)
                    val percent = ((currentWater.toDouble() / goalWater.toDouble()) * 100).toInt().coerceIn(0, 100)

                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        GradientHeroCard(
                            title = "${PersianNumberHelper.toPersianDigits(currentWater)} از ${PersianNumberHelper.toPersianDigits(goalWater)} ml آب",
                            subtitle = "${PersianNumberHelper.toPersianDigits(percent)}٪ از سهمیه روزانه مصرف شده",
                            gradient = LiquidPalettes.OCEAN.getCardGradient(),
                            badgeText = "هیدراتاسیون امروز"
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .liquidTapEffect { onAddWater(250, selectedDateGregorian) },
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("+۲۵۰ ml", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .liquidTapEffect { onAddWater(500, selectedDateGregorian) },
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("+۵۰۰ ml", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                IconButton(
                                    onClick = { onResetWater(selectedDateGregorian) },
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "ریست", tint = Color.White)
                                }
                            }
                        }
                    }
                }

                // Meals Inset Grouped Form
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LiquidSectionHeader(title = "ثبت وعده‌های غذایی")

                            SolidGroupedCard {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    LiquidInputField(
                                        value = breakfast,
                                        onValueChange = { breakfast = it },
                                        label = "صبحانه",
                                        placeholder = "مثلاً ۴ سفیده تخم‌مرغ + جو دوسر و کره بادام"
                                    )

                                    LiquidInputField(
                                        value = lunch,
                                        onValueChange = { lunch = it },
                                        label = "ناهار",
                                        placeholder = "مثلاً ۲۰۰ گرم فیله مرغ + کته و سالاد"
                                    )

                                    LiquidInputField(
                                        value = dinner,
                                        onValueChange = { dinner = it },
                                        label = "شام",
                                        placeholder = "مثلاً ماهی یا گوشت گریل + بروکلی و سیب‌زمینی"
                                    )

                                    LiquidInputField(
                                        value = snacks,
                                        onValueChange = { snacks = it },
                                        label = "میان‌وعده‌ها",
                                        placeholder = "مثلاً شیک پروتئین وی، موز و گردو"
                                    )

                                    LiquidInputField(
                                        value = dailyNotes,
                                        onValueChange = { dailyNotes = it },
                                        label = "یادداشت و وضعیت انرژی",
                                        placeholder = "مثلاً انرژی بالا در تمرین عصر"
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    LiquidPrimaryButton(
                                        text = "ذخیره وعده‌های این روز",
                                        onClick = {
                                            onSaveMealsLog(
                                                selectedDateGregorian,
                                                breakfast,
                                                lunch,
                                                dinner,
                                                snacks,
                                                dailyNotes
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Tab 1: Supplements
                if (allSupplements.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.Medication,
                            title = "هیچ مکملی ثبت نشده است",
                            description = "مکمل‌های مصرفی خود مانند پروتئین وی، کراتین یا ویتامین‌ها را اضافه کنید تا یادآوری روزانه فعال شود.",
                            actionButtonText = "افزودن اولین مکمل",
                            onActionClick = {
                                supplementToEdit = null
                                showSupplementDialog = true
                            }
                        )
                    }
                } else {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LiquidSectionHeader(title = "لیست مکمل‌ها و ثبت مصرف")

                                SolidGroupedCard {
                                    allSupplements.forEachIndexed { index, sup ->
                                        val isTaken = selectedDateSupplementIntakes.any { it.supplementId == sup.id }

                                        GroupedRow(
                                            title = sup.name,
                                            subtitle = "دوز: ${sup.dosage} • ساعت یادآور: ${PersianNumberHelper.toPersianDigits(sup.reminderTime)}",
                                            icon = Icons.Filled.Medication,
                                            iconTint = if (sup.isActive) SemanticIconGreen else LiquidTheme.textSecondary,
                                            showArrow = false,
                                            showDivider = index < allSupplements.size - 1,
                                            onClick = {
                                                supplementToEdit = sup
                                                showSupplementDialog = true
                                            },
                                            trailingContent = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    // Checkmark for taken today
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isTaken) SemanticSuccess else LiquidTheme.surfaceElevated)
                                                            .clickable {
                                                                onToggleSupplementIntake(sup, !isTaken, selectedDateGregorian)
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Check,
                                                            contentDescription = "ثبت مصرف",
                                                            tint = if (isTaken) Color.White else LiquidTheme.textSecondary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }

                                                    // Active Toggle
                                                    LiquidSwitch(
                                                        checked = sup.isActive,
                                                        onCheckedChange = { onToggleSupplementActive(sup) }
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

            2 -> {
                // Tab 2: Smart Diet & Macros
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        GradientHeroCard(
                            title = "${PersianNumberHelper.toPersianDigits(targetCalories.toInt())} کیلوکالری در روز",
                            subtitle = when (userSettings.fitnessGoal) {
                                "BULK" -> "هدف حجم عضلانی (۴۵۰+ کالری مازاد)"
                                "CUT" -> "هدف کاهش چربی (۴۵۰- کالری کسری)"
                                else -> "هدف تثبیت وزن و نگهداری"
                            },
                            badgeText = "محاسبه هوشمند ماکروها",
                            gradient = LiquidPalettes.EMERALD.getCardGradient()
                        )
                    }
                }

                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LiquidSectionHeader(title = "سهمیه تفکیکی ماکرونوترینت‌ها")

                            SolidGroupedCard {
                                GroupedRow(
                                    title = "پروتئین روزانه",
                                    icon = Icons.Filled.FitnessCenter,
                                    iconTint = SemanticIconRed,
                                    valueText = "${PersianNumberHelper.toPersianDigits(targetProteinGrams.toInt())} گرم",
                                    subtitle = "۲ تا ۲.۳ گرم به ازای هر کیلوگرم وزن بدن",
                                    showDivider = true,
                                    showArrow = false
                                )

                                GroupedRow(
                                    title = "کربوهیدرات پیچیده",
                                    icon = Icons.Filled.Grain,
                                    iconTint = SemanticIconOrange,
                                    valueText = "${PersianNumberHelper.toPersianDigits(targetCarbsGrams.toInt())} گرم",
                                    subtitle = "سوخت اصلی تمرینات قدرتی",
                                    showDivider = true,
                                    showArrow = false
                                )

                                GroupedRow(
                                    title = "چربی‌های مفید",
                                    icon = Icons.Filled.Opacity,
                                    iconTint = SemanticIconCyan,
                                    valueText = "${PersianNumberHelper.toPersianDigits(targetFatGrams.toInt())} گرم",
                                    subtitle = "تنظیم هورمون‌ها و سلامت مفاصل",
                                    showDivider = false,
                                    showArrow = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Supplement Dialog
    if (showSupplementDialog) {
        var sName by remember { mutableStateOf(supplementToEdit?.name ?: "") }
        var sDosage by remember { mutableStateOf(supplementToEdit?.dosage ?: "") }
        var sTime by remember { mutableStateOf(supplementToEdit?.reminderTime ?: "09:00") }
        var sNotes by remember { mutableStateOf(supplementToEdit?.notes ?: "") }
        var sActive by remember { mutableStateOf(supplementToEdit?.isActive ?: true) }

        LiquidAlertDialog(
            onDismissRequest = { showSupplementDialog = false },
            title = if (supplementToEdit == null) "افزودن مکمل جدید" else "ویرایش مکمل",
            message = "",
            confirmText = "ذخیره مکمل",
            dismissText = "انصراف",
            onConfirm = {
                if (sName.isNotBlank()) {
                    val sup = (supplementToEdit ?: Supplement(name = sName, dosage = sDosage)).copy(
                        name = sName,
                        dosage = sDosage,
                        reminderTime = sTime,
                        notes = sNotes,
                        isActive = sActive
                    )
                    onSaveSupplement(sup)
                }
            },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidInputField(
                        value = sName,
                        onValueChange = { sName = it },
                        label = "نام مکمل (مثلاً کراتین، وی)"
                    )
                    LiquidInputField(
                        value = sDosage,
                        onValueChange = { sDosage = it },
                        label = "دوز مصرفی (مثلاً ۵ گرم، ۱ پیمانه)"
                    )
                    LiquidInputField(
                        value = sTime,
                        onValueChange = { sTime = it },
                        label = "ساعت یادآوری (HH:mm)"
                    )
                    LiquidInputField(
                        value = sNotes,
                        onValueChange = { sNotes = it },
                        label = "یادداشت و نحوه مصرف"
                    )
                }
            }
        )
    }

    // Supplement Intake History Dialog
    if (showIntakeHistoryDialog) {
        LiquidAlertDialog(
            onDismissRequest = { showIntakeHistoryDialog = false },
            title = "سوابق مصرف مکمل‌ها",
            message = if (allSupplementIntakes.isEmpty()) "هنوز سابقه‌ای ثبت نشده است." else "",
            confirmText = "بستن",
            onConfirm = {},
            content = {
                if (allSupplementIntakes.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.height(240.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allSupplementIntakes) { h ->
                            val jDate = JalaliCalendar.formatGregorianToPersian(h.date)
                            GroupedRow(
                                title = h.supplementName,
                                subtitle = "$jDate ساعت ${PersianNumberHelper.toPersianDigits(h.time)}",
                                icon = Icons.Filled.CheckCircle,
                                iconTint = SemanticSuccess,
                                showArrow = false,
                                showDivider = true
                            )
                        }
                    }
                }
            }
        )
    }
}
