package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable // 👈 اضافه شد
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.WorkoutExercise
import com.example.data.local.WorkoutPlan
import com.example.ui.components.LiquidBottomBar
import com.example.ui.components.LiquidRestTimerDock
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.GymTheme
import com.example.ui.theme.LiquidTheme
import com.example.ui.viewmodel.ActiveWorkoutViewModel
import com.example.ui.viewmodel.GymViewModel
import com.example.util.JalaliCalendar
import com.example.util.NotificationHelper
import com.example.util.PlanShareHelper
import com.example.util.PersianNumberHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val gymViewModel: GymViewModel by viewModels()
    private val activeWorkoutViewModel: ActiveWorkoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channels
        NotificationHelper.createNotificationChannels(this)

        setContent {
            val userSettings by gymViewModel.userSettings.collectAsStateWithLifecycle()
            val allPlans by gymViewModel.allPlans.collectAsStateWithLifecycle()
            val allHistories by gymViewModel.allHistories.collectAsStateWithLifecycle()
            val allSupplements by gymViewModel.allSupplements.collectAsStateWithLifecycle()
            val allMeasurements by gymViewModel.allMeasurements.collectAsStateWithLifecycle()
            val latestMeasurement by gymViewModel.latestMeasurement.collectAsStateWithLifecycle()
            val allSkips by gymViewModel.allSkips.collectAsStateWithLifecycle()
            val datesWithNutrition by gymViewModel.datesWithNutrition.collectAsStateWithLifecycle()

            val selectedNutritionDate by gymViewModel.selectedNutritionDate.collectAsStateWithLifecycle()
            val selectedNutritionLog by gymViewModel.selectedNutritionLog.collectAsStateWithLifecycle()
            val selectedSupplementIntakes by gymViewModel.selectedDateSupplementIntakes.collectAsStateWithLifecycle()
            val allSupplementIntakes by gymViewModel.allSupplementIntakeHistories.collectAsStateWithLifecycle()
            val todayNutritionLog by gymViewModel.todayNutritionLog.collectAsStateWithLifecycle()

            // Active workout state
            val isWorkoutActive by activeWorkoutViewModel.isWorkoutActive.collectAsStateWithLifecycle()
            val isRestTimerActive by activeWorkoutViewModel.isRestTimerActive.collectAsStateWithLifecycle()
            val restRemainingSeconds by activeWorkoutViewModel.restSecondsRemaining.collectAsStateWithLifecycle()

            var currentScreen by rememberSaveable { mutableStateOf<String>(Screen.Dashboard.route) }
            var planBeingEdited by remember { mutableStateOf<WorkoutPlan?>(null) }
            var planExercisesBeingEdited by remember { mutableStateOf<List<WorkoutExercise>>(emptyList()) }
            var isEditingPlan by remember { mutableStateOf(false) }
            var isShowingOnboarding by rememberSaveable { mutableStateOf(false) }

            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current

            val notifPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* permission callback */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!NotificationHelper.hasNotificationPermission(context)) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            // اگر مسیر ذخیره‌شده معتبر نباشد (مثلاً بعد از بسته شدن پروسه) به پیشخوان برمی‌گردیم
            val screenRoute = if (Screen.bottomNavItems.any { it.route == currentScreen }) {
                currentScreen
            } else {
                Screen.Dashboard.route
            }

            // Check if onboarding needs to be shown initially
            val showOnboardingFirstTime = !userSettings.onboardingCompleted && !isShowingOnboarding

            GymTheme(
                appearanceMode = userSettings.appearanceMode,
                accentPalette = userSettings.accentPalette
            ) {
                // Strictly enforce RTL for Persian language
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    if (showOnboardingFirstTime || isShowingOnboarding) {
                        BackHandler(enabled = isShowingOnboarding) {
                            isShowingOnboarding = false
                        }
                        OnboardingScreen(
                            currentSettings = userSettings,
                            onSaveAndContinue = { name, photo, gender, age, height, weight, activity, goal, meals, water, unit ->
                                gymViewModel.updateProfile(
                                    name = name,
                                    photoUri = photo,
                                    gender = gender,
                                    age = age,
                                    heightCm = height,
                                    weightKg = weight,
                                    activity = activity,
                                    goal = goal,
                                    mealsCount = meals,
                                    waterGoal = water,
                                    weightUnit = unit
                                )
                                isShowingOnboarding = false
                            },
                            onSkip = {
                                gymViewModel.setOnboardingCompleted(true)
                                isShowingOnboarding = false
                            },
                            onRestoreBackupClick = {
                                currentScreen = Screen.Profile.route
                                isShowingOnboarding = false
                                gymViewModel.setOnboardingCompleted(true)
                            }
                        )
                    } else if (isEditingPlan) {
                        PlanEditorScreen(
                            initialPlan = planBeingEdited,
                            initialExercises = planExercisesBeingEdited,
                            onSave = { plan, exercises ->
                                coroutineScope.launch {
                                    gymViewModel.repository.savePlanWithExercises(plan, exercises)
                                    isEditingPlan = false
                                    planBeingEdited = null
                                    planExercisesBeingEdited = emptyList()
                                }
                            },
                            onBack = {
                                isEditingPlan = false
                                planBeingEdited = null
                                planExercisesBeingEdited = emptyList()
                            }
                        )
                    } else if (isWorkoutActive && currentScreen == "active_workout") {
                        ActiveWorkoutScreen(
                            viewModel = activeWorkoutViewModel,
                            weightUnit = userSettings.weightUnit,
                            onTemporaryExit = {
                                currentScreen = Screen.Dashboard.route
                            },
                            onWorkoutFinished = {
                                currentScreen = Screen.Workouts.route
                                Toast.makeText(context, "جلسه تمرین با موفقیت در تاریخچه ذخیره شد!", Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        // دکمه برگشت: از هر تب به پیشخوان، و از پیشخوان خروج از برنامه
                        BackHandler(enabled = screenRoute != Screen.Dashboard.route) {
                            currentScreen = Screen.Dashboard.route
                        }
                        Scaffold(
                            containerColor = LiquidTheme.backgroundNeutral,
                            // هر صفحه نوار بالای خودش را با پدینگ status bar دارد؛ پدینگ دوبل نشود
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            bottomBar = {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Persistent Rest Timer Dock
                                    LiquidRestTimerDock(
                                        remainingSeconds = restRemainingSeconds,
                                        isActive = isRestTimerActive,
                                        onAdd15Seconds = { activeWorkoutViewModel.addRestTime(15) },
                                        onCancel = { activeWorkoutViewModel.cancelRestTimer() },
                                        onClickDock = {
                                            if (isWorkoutActive) {
                                                currentScreen = "active_workout"
                                            }
                                        }
                                    )

                                    // Floating Liquid Glass Navigation Bar
                                    LiquidBottomBar(
                                        currentRoute = screenRoute,
                                        onNavigate = { currentScreen = it }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                Crossfade(
                                    targetState = screenRoute,
                                    animationSpec = tween(durationMillis = 180),
                                    label = "tab_crossfade"
                                ) { route ->
                                when (route) {
                                    Screen.Dashboard.route -> {
                                        val todayGregorian = remember { JalaliCalendar.getTodayGregorian() }
                                        val todayHistories = allHistories.filter { it.date == todayGregorian }

                                        DashboardScreen(
                                            userSettings = userSettings,
                                            allPlans = allPlans,
                                            todayNutrition = todayNutritionLog,
                                            todayHistories = todayHistories,
                                            allHistories = allHistories,
                                            allSkips = allSkips,
                                            latestMeasurement = latestMeasurement,
                                            onStartWorkout = { plan ->
                                                coroutineScope.launch {
                                                    val exercises = gymViewModel.repository.getExercisesForPlanSync(plan.id)
                                                    activeWorkoutViewModel.startWorkout(plan, exercises)
                                                    currentScreen = "active_workout"
                                                }
                                            },
                                            onNavigateToWorkouts = { currentScreen = Screen.Workouts.route },
                                            onNavigateToNutrition = { currentScreen = Screen.Nutrition.route },
                                            onNavigateToAnalysis = { currentScreen = Screen.Analysis.route },
                                            onNavigateToProfile = { currentScreen = Screen.Profile.route },
                                            onAddWater = { amount -> gymViewModel.addWater(amount) },
                                            onResetWater = { gymViewModel.resetWater() },
                                            isRestTimerActive = isRestTimerActive,
                                            restRemainingSeconds = restRemainingSeconds
                                        )
                                    }

                                    Screen.Workouts.route -> {
                                        WorkoutsScreen(
                                            allPlans = allPlans,
                                            allHistories = allHistories,
                                            allSkips = allSkips,
                                            onStartWorkout = { plan ->
                                                coroutineScope.launch {
                                                    val exercises = gymViewModel.repository.getExercisesForPlanSync(plan.id)
                                                    activeWorkoutViewModel.startWorkout(plan, exercises)
                                                    currentScreen = "active_workout"
                                                }
                                            },
                                            onEditPlan = { plan ->
                                                coroutineScope.launch {
                                                    planBeingEdited = plan
                                                    planExercisesBeingEdited = if (plan != null) {
                                                        gymViewModel.repository.getExercisesForPlanSync(plan.id)
                                                    } else {
                                                        emptyList()
                                                    }
                                                    isEditingPlan = true
                                                }
                                            },
                                            onDuplicatePlan = { planId -> gymViewModel.duplicatePlan(planId) },
                                            onDuplicateAllPlans = { gymViewModel.duplicateAllPlans() },
                                            onDeletePlan = { planId -> gymViewModel.deletePlan(planId) },
                                            onReschedulePlan = { planId, origDate, newDate ->
                                                gymViewModel.reschedulePlan(planId, origDate, newDate)
                                            },
                                            onUndoSkip = { planId, origDate -> gymViewModel.undoSkip(planId, origDate) },
                                            onDeleteHistory = { hId -> gymViewModel.deleteHistory(hId) },
                                            onImportPlanJson = { json ->
                                                val parseRes = PlanShareHelper.parsePlansFromJson(json)
                                                if (parseRes.isSuccess && !parseRes.getOrNull().isNullOrEmpty()) {
                                                    val plansList = parseRes.getOrNull()!!
                                                    coroutineScope.launch {
                                                        plansList.forEach { data ->
                                                            val p = WorkoutPlan(
                                                                name = data.name,
                                                                notes = data.notes,
                                                                scheduledDay = data.scheduledDay
                                                            )
                                                            val exList = data.exercises.map { e ->
                                                                WorkoutExercise(
                                                                    planId = 0,
                                                                    name = e.name,
                                                                    sets = e.sets,
                                                                    reps = e.reps,
                                                                    repsMax = e.repsMax,
                                                                    weight = e.weight,
                                                                    notes = e.notes,
                                                                    supersetGroup = e.supersetGroup,
                                                                    isTimedExercise = e.isTimedExercise,
                                                                    customMetrics = e.customMetrics
                                                                )
                                                            }
                                                            gymViewModel.repository.savePlanWithExercises(p, exList)
                                                        }
                                                        val countStr = PersianNumberHelper.toPersianDigits(plansList.size)
                                                        Toast.makeText(context, "$countStr برنامه تمرینی با موفقیت اضافه شد!", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "فرمت فایل یا کد JSON نامعتبر است.", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            onGetPlanExercises = { planId ->
                                                gymViewModel.repository.getExercisesForPlanSync(planId)
                                            }
                                        )
                                    }

                                    Screen.Nutrition.route -> {
                                        NutritionScreen(
                                            userSettings = userSettings,
                                            selectedDateGregorian = selectedNutritionDate,
                                            onSelectDate = { date -> gymViewModel.setSelectedNutritionDate(date) },
                                            datesWithNutrition = datesWithNutrition,
                                            currentNutritionLog = selectedNutritionLog,
                                            allSupplements = allSupplements,
                                            selectedDateSupplementIntakes = selectedSupplementIntakes,
                                            allSupplementIntakes = allSupplementIntakes,
                                            onSaveMealsLog = { date, b, l, d, s, n ->
                                                gymViewModel.updateMealsLog(date, b, l, d, s, n)
                                                Toast.makeText(context, "وعده‌ها با موفقیت ذخیره شدند.", Toast.LENGTH_SHORT).show()
                                            },
                                            onAddWater = { amount, date -> gymViewModel.addWater(amount, date) },
                                            onResetWater = { date -> gymViewModel.resetWater(date) },
                                            onSaveSupplement = { sup -> gymViewModel.saveSupplement(sup) },
                                            onDeleteSupplement = { sup -> gymViewModel.deleteSupplement(sup) },
                                            onToggleSupplementActive = { sup -> gymViewModel.toggleSupplementActive(sup) },
                                            onToggleSupplementIntake = { sup, isTaken, date ->
                                                gymViewModel.toggleSupplementIntake(sup, isTaken, date)
                                            }
                                        )
                                    }

                                    Screen.Analysis.route -> {
                                        AnalysisScreen(
                                            userSettings = userSettings,
                                            allMeasurements = allMeasurements,
                                            allHistories = allHistories,
                                            onSaveMeasurement = { m ->
                                                gymViewModel.saveMeasurement(m)
                                                Toast.makeText(context, "سایز جدید با موفقیت ثبت شد.", Toast.LENGTH_SHORT).show()
                                            },
                                            onDeleteMeasurement = { date -> gymViewModel.deleteMeasurement(date) }
                                        )
                                    }

                                    Screen.Profile.route -> {
                                        ProfileSettingsScreen(
                                            userSettings = userSettings,
                                            onSelectTheme = { themeKey -> gymViewModel.setTheme(themeKey) },
                                            onSetAppearanceAndPalette = { mode, pal ->
                                                gymViewModel.setAppearanceAndPalette(mode, pal)
                                            },
                                            onToggleReminder = { type, enabled, time ->
                                                gymViewModel.toggleReminder(type, enabled, time)
                                            },
                                            onTestNotification = { gymViewModel.triggerTestNotification() },
                                            onExportBackupJson = { gymViewModel.exportDataJson() },
                                            onRestoreBackupJson = { json -> gymViewModel.restoreDataJson(json) },
                                            onGetAiPrompt = { gymViewModel.getAiExportPrompt() },
                                            onClearAllData = { gymViewModel.clearAllData() },
                                            onOpenOnboarding = { isShowingOnboarding = true },
                                            onUpdateProfilePhoto = { photo -> gymViewModel.updateProfilePhoto(photo) },
                                            onUpdateSoundSettings = { en, rVol, tVol, remVol ->
                                                gymViewModel.updateSoundSettings(en, rVol, tVol, remVol)
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
        }
    }
}
