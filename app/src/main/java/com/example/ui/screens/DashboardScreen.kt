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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.JalaliCalendar
import com.example.util.PersianNumberHelper

@Composable
fun DashboardScreen(
    userSettings: UserSettings,
    allPlans: List<WorkoutPlan>,
    todayNutrition: NutritionLog?,
    todayHistories: List<WorkoutHistory>,
    allHistories: List<WorkoutHistory>,
    allSkips: List<WorkoutPlanSkip>,
    latestMeasurement: BodyMeasurement?,
    onStartWorkout: (WorkoutPlan) -> Unit,
    onNavigateToWorkouts: () -> Unit,
    onNavigateToNutrition: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onAddWater: (Int) -> Unit,
    onResetWater: () -> Unit,
    isRestTimerActive: Boolean,
    restRemainingSeconds: Int
) {
    val todayGregorian = remember { JalaliCalendar.getTodayGregorian() }
    val todayJalali = remember { JalaliCalendar.getTodayJalali() }
    val todayWeekday = remember { JalaliCalendar.getTodayWeekdayPersian() }

    val skippedPlanIdsForToday = allSkips.filter {
        (it.originalDate == todayGregorian && it.newDate == null) ||
        (it.originalDate == todayGregorian && it.newDate != todayGregorian)
    }.map { it.planId }

    val postponedToTodayPlans = allSkips.filter { it.newDate == todayGregorian }.map { it.planId }

    val todayScheduledPlan = allPlans.firstOrNull { plan ->
        (plan.scheduledDay.contains(todayWeekday) || postponedToTodayPlans.contains(plan.id)) &&
                !skippedPlanIdsForToday.contains(plan.id)
    }

    val isTodayWorkoutFinished = todayHistories.isNotEmpty()

    val currentWeight = latestMeasurement?.weightKg ?: userSettings.weightKg
    val currentHeight = latestMeasurement?.heightCm ?: userSettings.heightCm
    val bmi = if (currentWeight != null && currentHeight != null && currentHeight > 0) {
        val hM = currentHeight / 100.0
        currentWeight / (hM * hM)
    } else null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Inline Liquid Top Bar with Profile Button & Logo
        item {
            LiquidTopBar(
                title = "GYM",
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .liquidGlass(shape = CircleShape, elevation = 3.dp)
                            .liquidTapEffect { onNavigateToProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (userSettings.profilePhotoUri.isNotBlank()) {
                            AsyncImage(
                                model = userSettings.profilePhotoUri,
                                contentDescription = "پروفایل",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "پروفایل",
                                tint = LiquidTheme.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .liquidGlass(shape = CircleShape, elevation = 3.dp)
                            .liquidTapEffect { onNavigateToWorkouts() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = "برنامه‌ها",
                            tint = LiquidTheme.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }

        // 2. Greeting & Jalali Date (< 24sp)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (userSettings.userName.isNotBlank()) "سلام، ${userSettings.userName}" else "سلام، قهرمان",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 22.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = LiquidTheme.textPrimary
                )
                Text(
                    text = "$todayWeekday، ${todayJalali.toFormattedString(withMonthName = true)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LiquidTheme.textSecondary
                )
            }
        }

        // 3. Horizontal Carousel of Gradient Hero Cards
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Today's Workout
                item {
                    GradientHeroCard(
                        title = when {
                            isTodayWorkoutFinished -> "تمرین امروز انجام شد"
                            todayScheduledPlan != null -> todayScheduledPlan.name
                            allPlans.isEmpty() -> "ساخت اولین برنامه"
                            else -> "روز استراحت و ریکاوری"
                        },
                        subtitle = when {
                            isTodayWorkoutFinished -> "عالی بود! اطلاعات تمرین در سوابق ذخیره شد."
                            todayScheduledPlan != null -> "جلسه تمرینی برنامه‌ریزی شده برای $todayWeekday"
                            allPlans.isEmpty() -> "برای شروع ورزش، اولین برنامه اختصاصی‌تان را بسازید."
                            else -> "به عضلات خود فرصت بازسازی و رشد دهید."
                        },
                        badgeText = when {
                            isTodayWorkoutFinished -> "تکمیل شد"
                            todayScheduledPlan != null -> "آماده شروع"
                            allPlans.isEmpty() -> "شروع"
                            else -> "استراحت"
                        },
                        modifier = Modifier.width(310.dp)
                    ) {
                        if (todayScheduledPlan != null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .liquidTapEffect { onStartWorkout(todayScheduledPlan) },
                                shape = CircleShape,
                                color = Color.White
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isTodayWorkoutFinished) "تمرین مجدد" else "شروع تمرین",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        } else if (allPlans.isEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .liquidTapEffect { onNavigateToWorkouts() },
                                shape = CircleShape,
                                color = Color.White
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "ایجاد برنامه جدید",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 2: Water & Hydration
                item {
                    val currentWater = todayNutrition?.waterIntakeMl ?: 0
                    val goalWater = userSettings.waterGoalMl.coerceAtLeast(1000)
                    val percent = ((currentWater.toDouble() / goalWater.toDouble()) * 100).toInt().coerceIn(0, 100)

                    GradientHeroCard(
                        title = "${PersianNumberHelper.toPersianDigits(currentWater)} ml آب",
                        subtitle = "هدف: ${PersianNumberHelper.toPersianDigits(goalWater)} میلی‌لیتر (${PersianNumberHelper.toPersianDigits(percent)}٪)",
                        badgeText = "هیدراتاسیون",
                        gradient = LiquidPalettes.OCEAN.getCardGradient(),
                        modifier = Modifier.width(310.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .liquidTapEffect { onAddWater(250) },
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
                                    .liquidTapEffect { onAddWater(500) },
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("+۵۰۰ ml", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            IconButton(
                                onClick = onResetWater,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = "ریست آب", tint = Color.White)
                            }
                        }
                    }
                }

                // Card 3: Nutrition & Meals
                item {
                    GradientHeroCard(
                        title = "تغذیه و وعده‌های غذایی",
                        subtitle = "ثبت صبحانه، ناهار، شام، میان‌وعده‌ها و کنترل کالری روزانه",
                        badgeText = "رژیم غذایی",
                        gradient = LiquidPalettes.EMERALD.getCardGradient(),
                        modifier = Modifier.width(310.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .liquidTapEffect { onNavigateToNutrition() },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("مدیریت وعده‌ها و مکمل‌ها", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // 4. "هفته‌ی من" (My Week: 7 Circular Day Indicators)
        item {
            val weekDays = remember { JalaliCalendar.getCurrentWeekDays() }
            val completedDates = allHistories.map { it.date }.toSet()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidSectionHeader(title = "هفته‌ی من")

                SolidGroupedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        weekDays.forEach { (dateStr, weekday) ->
                            val isDone = completedDates.contains(dateStr)
                            val isToday = dateStr == todayGregorian

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = weekday.take(1),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isToday) LiquidTheme.accentColor else LiquidTheme.textSecondary
                                )

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isDone -> SemanticSuccess
                                                isToday -> LiquidTheme.accentColor.copy(alpha = 0.2f)
                                                else -> LiquidTheme.surfaceElevated
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDone) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "انجام شد",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isToday) LiquidTheme.accentColor else LiquidTheme.textSecondary.copy(alpha = 0.4f)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Circular Quick Action Shortcuts
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidSectionHeader(title = "دسترسی سریع")

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    item {
                        CircularShortcut(
                            icon = Icons.Filled.Straighten,
                            label = "ثبت سایز",
                            iconTint = SemanticIconOrange,
                            onClick = onNavigateToAnalysis
                        )
                    }
                    item {
                        CircularShortcut(
                            icon = Icons.Filled.Medication,
                            label = "مکمل‌ها",
                            iconTint = SemanticIconGreen,
                            onClick = onNavigateToNutrition
                        )
                    }
                    item {
                        CircularShortcut(
                            icon = Icons.Filled.Analytics,
                            label = "آنالیز",
                            iconTint = SemanticIconPurple,
                            onClick = onNavigateToAnalysis
                        )
                    }
                    item {
                        CircularShortcut(
                            icon = Icons.Filled.History,
                            label = "سوابق",
                            iconTint = SemanticIconBlue,
                            onClick = onNavigateToWorkouts
                        )
                    }
                }
            }
        }

        // 6. Inset Grouped Card: "وضعیت بدن" (Body Status)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidSectionHeader(
                    title = "وضعیت بدن",
                    actionText = "جزئیات",
                    onActionClick = onNavigateToAnalysis
                )

                SolidGroupedCard {
                    GroupedRow(
                        title = "وزن فعلی",
                        icon = Icons.Filled.FitnessCenter,
                        iconTint = SemanticIconOrange,
                        valueText = if (currentWeight != null) {
                            "${PersianNumberHelper.toPersianDigits(String.format("%.1f", currentWeight))} ${userSettings.weightUnit}"
                        } else "اطلاعات وارد نشده",
                        showDivider = true,
                        onClick = onNavigateToAnalysis
                    )

                    GroupedRow(
                        title = "شاخص توده بدنی (BMI)",
                        icon = Icons.Filled.MonitorWeight,
                        iconTint = SemanticIconCyan,
                        valueText = if (bmi != null) {
                            "${PersianNumberHelper.toPersianDigits(String.format("%.1f", bmi))} " + when {
                                bmi < 18.5 -> "(کمبود وزن)"
                                bmi < 25.0 -> "(نرمال)"
                                bmi < 30.0 -> "(اضافه وزن)"
                                else -> "(چاقی)"
                            }
                        } else "اطلاعات وارد نشده",
                        showDivider = true,
                        onClick = onNavigateToAnalysis
                    )

                    GroupedRow(
                        title = "هدف ورزشی",
                        icon = Icons.Filled.TrackChanges,
                        iconTint = SemanticIconGreen,
                        valueText = when (userSettings.fitnessGoal) {
                            "BULK" -> "افزایش حجم عضلانی"
                            "CUT" -> "کاهش چربی و کات"
                            else -> "تثبیت و تناسب اندام"
                        },
                        showDivider = false,
                        onClick = onNavigateToProfile
                    )
                }
            }
        }
    }
}
