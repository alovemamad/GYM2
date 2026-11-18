package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.R
import com.example.data.local.SoundPrefsCache
import com.example.data.local.UserSettings
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.AlertType
import com.example.util.AppUpdateHelper
import com.example.util.BackupHelper
import com.example.util.BackupSaveResult
import com.example.util.ImageStorageHelper
import com.example.util.InstallLaunchStatus
import com.example.util.NotificationHelper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.util.PersianNumberHelper
import com.example.util.ReleaseUpdateInfo
import com.example.util.ReminderScheduler
import com.example.util.UpdateCheckResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ProfileSettingsScreen(
    userSettings: UserSettings,
    onSelectTheme: (String) -> Unit,
    onSetAppearanceAndPalette: (appearanceMode: String, accentPalette: String) -> Unit,
    onToggleReminder: (type: String, enabled: Boolean, timeStr: String) -> Unit,
    onTestNotification: () -> Unit,
    onExportBackupJson: suspend () -> String,
    onRestoreBackupJson: suspend (String) -> Result<Unit>,
    onGetAiPrompt: suspend () -> String,
    onClearAllData: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onUpdateProfilePhoto: (String) -> Unit = {},
    onUpdateSoundSettings: (Boolean, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    onSetGithubRepo: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentSubScreen by rememberSaveable { mutableStateOf<String?>(null) } // null, "appearance", "reminders"

    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAiExportDialog by remember { mutableStateOf(false) }
    var aiPromptContent by remember { mutableStateOf("") }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showBackupSuccessDialog by remember { mutableStateOf(false) }
    var lastSavedBackupResult by remember { mutableStateOf<BackupSaveResult?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    // Permission launcher for Android 13+ notifications & background reliability checks
    var hasNotifPermission by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }
    var canExactAlarm by remember { mutableStateOf(ReminderScheduler.canScheduleExactAlarms(context)) }
    var isIgnoringBattery by remember { mutableStateOf(ReminderScheduler.isIgnoringBatteryOptimizations(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifPermission = NotificationHelper.hasNotificationPermission(context)
                canExactAlarm = ReminderScheduler.canScheduleExactAlarms(context)
                isIgnoringBattery = ReminderScheduler.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (granted) {
            Toast.makeText(context, "مجوز اعلان با موفقیت فعال شد.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "بدون مجوز اعلان، یادآورهای روزانه نمایش داده نخواهند شد.", Toast.LENGTH_LONG).show()
        }
    }

    val performBackupExport: () -> Unit = {
        coroutineScope.launch {
            try {
                val json = onExportBackupJson()
                val res = BackupHelper.saveBackupWithDetails(context, json)
                lastSavedBackupResult = res
                showBackupSuccessDialog = true
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در ایجاد فایل پشتیبان: ${e.message ?: "نامشخص"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Legacy storage permission launcher (Android 9 and below only)
    var pendingStorageAction by remember { mutableStateOf<String?>(null) } // "BACKUP" or "RESTORE"
    val legacyStoragePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        when (pendingStorageAction) {
            "BACKUP" -> {
                if (!allGranted) {
                    Toast.makeText(context, "مجوز حافظه داده نشد؛ فایل در پوشه اختصاصی برنامه ذخیره شد.", Toast.LENGTH_SHORT).show()
                }
                performBackupExport()
            }
            "RESTORE" -> showRestoreDialog = true
        }
        pendingStorageAction = null
    }

    // SAF OpenDocument launcher to restore any .json file without broad storage permissions
    val restoreDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val readRes = BackupHelper.readBackupFromUri(context, uri)
                if (readRes.isSuccess) {
                    val container = readRes.getOrNull()!!
                    val json = BackupHelper.createBackupJson(
                        settings = container.settings,
                        plans = container.plans,
                        exercises = container.exercises,
                        histories = container.histories,
                        skips = container.skips,
                        supplements = container.supplements,
                        supplementHistories = container.supplementHistories,
                        nutritionLogs = container.nutritionLogs,
                        measurements = container.measurements
                    )
                    val restoreRes = onRestoreBackupJson(json)
                    if (restoreRes.isSuccess) {
                        showRestoreDialog = false
                        Toast.makeText(context, "اطلاعات با موفقیت از فایل انتخاب‌شده بازیابی شد!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "خطا در بازیابی اطلاعات دیتابیس.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "فایل انتخاب‌شده یک پشتیبان معتبر GYM نیست.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // SAF CreateDocument launcher so user can export backup .json to any folder they choose
    val createBackupDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val json = onExportBackupJson()
                    val writeRes = BackupHelper.writeBackupToUri(context, uri, json)
                    if (writeRes.isSuccess) {
                        Toast.makeText(context, "فایل پشتیبان با موفقیت در مسیر انتخابی ذخیره شد.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "خطا در ذخیره فایل در مسیر انتخابی.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "خطا در ذخیره‌سازی: ${e.message ?: "نامشخص"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    var timePickerTarget by remember { mutableStateOf<String?>(null) } // "WORKOUT", "WATER", "SUPPLEMENT"

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val permanentPath = ImageStorageHelper.saveProfilePhotoPermanently(context, uri)
            onUpdateProfilePhoto(permanentPath)
            Toast.makeText(context, "عکس پروفایل با موفقیت بروز شد.", Toast.LENGTH_SHORT).show()
        }
    }

    // Intercept back button for sub-screens
    BackHandler(enabled = currentSubScreen != null) {
        currentSubScreen = null
    }

    when (currentSubScreen) {
        "appearance" -> {
            // ==========================================
            // زیرصفحه تم و ظاهر
            // ==========================================
            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    LiquidTopBar(
                        title = "شخصی‌سازی ظاهر",
                        navigationIcon = {
                            LiquidCircleButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                onClick = { currentSubScreen = null }
                            )
                        }
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Axis 1: Day / Night mode
                    item {
                        LiquidSectionHeader(title = "حالت نمایش سیستم")
                        SolidGroupedCard {
                            Column(modifier = Modifier.padding(14.dp)) {
                                val modes = listOf(
                                    "SYSTEM" to "سیستم",
                                    "LIGHT" to "روشن",
                                    "DARK" to "تاریک"
                                )
                                val selectedIndex = when (userSettings.appearanceMode) {
                                    "LIGHT" -> 1
                                    "DARK" -> 2
                                    else -> 0
                                }
                                LiquidSegmentedControl(
                                    items = modes.map { it.second },
                                    selectedIndex = selectedIndex,
                                    onSelectIndex = { idx ->
                                        val newMode = modes[idx].first
                                        onSetAppearanceAndPalette(newMode, userSettings.accentPalette)
                                    }
                                )
                            }
                        }
                    }

                    // Axis 2: 8 Accent Palettes
                    item {
                        LiquidSectionHeader(title = "پالت‌های رنگی Liquid Glass")
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val palettes = LiquidPalettes.all
                            for (i in palettes.indices step 2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val p1 = palettes[i]
                                    val p2 = palettes.getOrNull(i + 1)

                                    Box(modifier = Modifier.weight(1f)) {
                                        PaletteCard(
                                            palette = p1,
                                            isSelected = userSettings.accentPalette.equals(p1.id, ignoreCase = true),
                                            onClick = {
                                                onSetAppearanceAndPalette(userSettings.appearanceMode, p1.id)
                                            }
                                        )
                                    }

                                    if (p2 != null) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            PaletteCard(
                                                palette = p2,
                                                isSelected = userSettings.accentPalette.equals(p2.id, ignoreCase = true),
                                                onClick = {
                                                    onSetAppearanceAndPalette(userSettings.appearanceMode, p2.id)
                                                }
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        "reminders" -> {
            // ==========================================
            // زیرصفحه یادآورها و اعلان‌ها
            // ==========================================
            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    LiquidTopBar(
                        title = "تنظیم یادآورها",
                        navigationIcon = {
                            LiquidCircleButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                onClick = { currentSubScreen = null }
                            )
                        }
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Permission Banner if not granted
                    if (!hasNotifPermission) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = SemanticWarning.copy(alpha = 0.14f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = SemanticWarning)
                                        Text(
                                            "مجوز اعلان‌ها غیرفعال است",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SemanticWarning
                                        )
                                    }
                                    Text(
                                        "برای دریافت هشدارهای روزانه تمرین و آب، لطفاً دسترسی به اعلان را فعال کنید.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LiquidTheme.textPrimary
                                    )
                                    LiquidPrimaryButton(
                                        text = "فعال‌سازی مجوز اعلان",
                                        onClick = {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Exact Alarm Permission Banner (Android 12+) if revoked
                    if (!canExactAlarm) {
                        item {
                            SolidGroupedCard {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Filled.AlarmOff, contentDescription = null, tint = SemanticWarning)
                                        Text(
                                            "دسترسی زنگ دقیق غیرفعال است",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SemanticWarning
                                        )
                                    }
                                    Text(
                                        "برای اینکه اعلان‌ها حتی در زمان بسته بودن طولانی‌مدت برنامه سر ساعت دقیق ارسال شوند، دسترسی «آلارم و یادآورها» را فعال کنید.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LiquidTheme.textPrimary
                                    )
                                    LiquidPrimaryButton(
                                        text = "فعال‌سازی آلارم دقیق",
                                        onClick = { ReminderScheduler.openExactAlarmSettings(context) }
                                    )
                                }
                            }
                        }
                    }

                    // Battery Optimization Banner for closed-app delivery reliability
                    if (!isIgnoringBattery) {
                        item {
                            SolidGroupedCard {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Filled.BatteryAlert, contentDescription = null, tint = LiquidTheme.iconOrange)
                                        Text(
                                            "جلوگیری از توقف یادآور در پس‌زمینه",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = LiquidTheme.textPrimary
                                        )
                                    }
                                    Text(
                                        "در برخی گوشی‌ها (مثل سامسونگ و شیائومی)، بهینه‌ساز باتری باعث می‌شود یادآورها زمانی که برنامه بسته است با تاخیر یا بعد از باز کردن برنامه یکجا بیایند. در صفحه بازشده، GYM PRO را روی «نامحدود / Unrestricted» بگذارید.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LiquidTheme.textSecondary
                                    )
                                    LiquidSecondaryButton(
                                        text = "تنظیم باتری در حالت نامحدود",
                                        icon = Icons.Filled.Settings,
                                        onClick = { ReminderScheduler.openBatteryOptimizationSettings(context) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        LiquidSectionHeader(title = "هشدارهای روزانه")

                        SolidGroupedCard {
                            GroupedRow(
                                title = "یادآور تمرین ورزشی",
                                subtitle = "ساعت: ${PersianNumberHelper.toPersianDigits(userSettings.workoutReminderTime)} (برای تغییر ساعت لمس کنید)",
                                icon = Icons.Filled.FitnessCenter,
                                iconTint = LiquidTheme.iconRed,
                                showArrow = true,
                                showDivider = true,
                                onClick = { timePickerTarget = "WORKOUT" },
                                trailingContent = {
                                    LiquidSwitch(
                                        checked = userSettings.workoutReminderEnabled,
                                        onCheckedChange = { en ->
                                            if (en && !hasNotifPermission) {
                                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                            onToggleReminder("WORKOUT", en, userSettings.workoutReminderTime)
                                        }
                                    )
                                }
                            )

                            GroupedRow(
                                title = "یادآور نوشیدن آب",
                                subtitle = "ساعت: ${PersianNumberHelper.toPersianDigits(userSettings.waterReminderTime)} (برای تغییر ساعت لمس کنید)",
                                icon = Icons.Filled.WaterDrop,
                                iconTint = LiquidTheme.iconCyan,
                                showArrow = true,
                                showDivider = true,
                                onClick = { timePickerTarget = "WATER" },
                                trailingContent = {
                                    LiquidSwitch(
                                        checked = userSettings.waterReminderEnabled,
                                        onCheckedChange = { en ->
                                            if (en && !hasNotifPermission) {
                                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                            onToggleReminder("WATER", en, userSettings.waterReminderTime)
                                        }
                                    )
                                }
                            )

                            GroupedRow(
                                title = "یادآور مصرف مکمل‌ها",
                                subtitle = "ساعت: ${PersianNumberHelper.toPersianDigits(userSettings.supplementReminderTime)} (برای تغییر ساعت لمس کنید)",
                                icon = Icons.Filled.Medication,
                                iconTint = LiquidTheme.iconGreen,
                                showArrow = true,
                                showDivider = false,
                                onClick = { timePickerTarget = "SUPPLEMENT" },
                                trailingContent = {
                                    LiquidSwitch(
                                        checked = userSettings.supplementReminderEnabled,
                                        onCheckedChange = { en ->
                                            if (en && !hasNotifPermission) {
                                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                            onToggleReminder("SUPPLEMENT", en, userSettings.supplementReminderTime)
                                        }
                                    )
                                }
                            )
                        }
                    }

                    // In-app Sound Settings with individual sliders
                    item {
                        LiquidSectionHeader(title = "صداها و هشدارهای داخل برنامه")

                        SolidGroupedCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 1. Global in-app sounds toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(LiquidTheme.accentColor.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (userSettings.inAppSoundsEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                                                contentDescription = null,
                                                tint = LiquidTheme.accentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                "صداهای داخل برنامه",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = LiquidTheme.textPrimary
                                            )
                                            Text(
                                                "بوق تایمر استراحت و آلارم حرکات زمان‌دار",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = LiquidTheme.textSecondary
                                            )
                                        }
                                    }

                                    LiquidSwitch(
                                        checked = userSettings.inAppSoundsEnabled,
                                        onCheckedChange = { en ->
                                            onUpdateSoundSettings(
                                                en,
                                                userSettings.restTimerVolume,
                                                userSettings.timedAlarmVolume,
                                                userSettings.reminderVolume
                                            )
                                        }
                                    )
                                }

                                if (userSettings.inAppSoundsEnabled) {
                                    HorizontalDivider(color = LiquidTheme.dividerColor, thickness = 0.5.dp)

                                    var restVolDraft by remember(userSettings.restTimerVolume) {
                                        mutableFloatStateOf(userSettings.restTimerVolume)
                                    }
                                    var timedVolDraft by remember(userSettings.timedAlarmVolume) {
                                        mutableFloatStateOf(userSettings.timedAlarmVolume)
                                    }
                                    var reminderVolDraft by remember(userSettings.reminderVolume) {
                                        mutableFloatStateOf(userSettings.reminderVolume)
                                    }

                                    // 2. Rest timer beep volume slider
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("حجم بوق تایمر استراحت:", style = MaterialTheme.typography.bodyMedium, color = LiquidTheme.textPrimary)
                                            Text("${PersianNumberHelper.toPersianDigits((restVolDraft * 100).toInt())}٪", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LiquidTheme.accentColor)
                                        }
                                        Slider(
                                            value = restVolDraft,
                                            onValueChange = { newVol ->
                                                restVolDraft = newVol
                                                SoundPrefsCache.save(context, userSettings.inAppSoundsEnabled, newVol, timedVolDraft, reminderVolDraft)
                                            },
                                            onValueChangeFinished = {
                                                onUpdateSoundSettings(
                                                    userSettings.inAppSoundsEnabled,
                                                    restVolDraft,
                                                    timedVolDraft,
                                                    reminderVolDraft
                                                )
                                                NotificationHelper.playAlarmSoundAndVibrate(
                                                    context,
                                                    AlertType.REST_END,
                                                    overrideVolume = restVolDraft,
                                                    withVibration = false
                                                )
                                            },
                                            modifier = Modifier.testTag("slider_rest_volume"),
                                            colors = SliderDefaults.colors(
                                                thumbColor = LiquidTheme.accentColor,
                                                activeTrackColor = LiquidTheme.accentColor,
                                                inactiveTrackColor = LiquidTheme.surfaceElevated
                                            )
                                        )
                                    }

                                    // 3. Timed exercise alarm volume slider
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("حجم آلارم پایان حرکات زمان‌دار (پلانک):", style = MaterialTheme.typography.bodyMedium, color = LiquidTheme.textPrimary)
                                            Text("${PersianNumberHelper.toPersianDigits((timedVolDraft * 100).toInt())}٪", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LiquidTheme.accentColor)
                                        }
                                        Slider(
                                            value = timedVolDraft,
                                            onValueChange = { newVol ->
                                                timedVolDraft = newVol
                                                SoundPrefsCache.save(context, userSettings.inAppSoundsEnabled, restVolDraft, newVol, reminderVolDraft)
                                            },
                                            onValueChangeFinished = {
                                                onUpdateSoundSettings(
                                                    userSettings.inAppSoundsEnabled,
                                                    restVolDraft,
                                                    timedVolDraft,
                                                    reminderVolDraft
                                                )
                                                NotificationHelper.playAlarmSoundAndVibrate(
                                                    context,
                                                    AlertType.EXERCISE_END,
                                                    overrideVolume = timedVolDraft,
                                                    withVibration = false
                                                )
                                            },
                                            modifier = Modifier.testTag("slider_timed_volume"),
                                            colors = SliderDefaults.colors(
                                                thumbColor = LiquidTheme.accentColor,
                                                activeTrackColor = LiquidTheme.accentColor,
                                                inactiveTrackColor = LiquidTheme.surfaceElevated
                                            )
                                        )
                                    }

                                    // 4. Reminder notification volume slider
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("حجم صدای زنگ اعلان‌ها و یادآورها:", style = MaterialTheme.typography.bodyMedium, color = LiquidTheme.textPrimary)
                                            Text("${PersianNumberHelper.toPersianDigits((reminderVolDraft * 100).toInt())}٪", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LiquidTheme.accentColor)
                                        }
                                        Slider(
                                            value = reminderVolDraft,
                                            onValueChange = { newVol ->
                                                reminderVolDraft = newVol
                                                SoundPrefsCache.save(context, userSettings.inAppSoundsEnabled, restVolDraft, timedVolDraft, newVol)
                                            },
                                            onValueChangeFinished = {
                                                onUpdateSoundSettings(
                                                    userSettings.inAppSoundsEnabled,
                                                    restVolDraft,
                                                    timedVolDraft,
                                                    reminderVolDraft
                                                )
                                                NotificationHelper.playAlarmSoundAndVibrate(
                                                    context,
                                                    AlertType.WORKOUT,
                                                    overrideVolume = reminderVolDraft,
                                                    withVibration = false
                                                )
                                            },
                                            modifier = Modifier.testTag("slider_reminder_volume"),
                                            colors = SliderDefaults.colors(
                                                thumbColor = LiquidTheme.accentColor,
                                                activeTrackColor = LiquidTheme.accentColor,
                                                inactiveTrackColor = LiquidTheme.surfaceElevated
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        LiquidPrimaryButton(
                            text = "تست ارسال اعلان و آلارم صوتی",
                            icon = Icons.Filled.NotificationsActive,
                            onClick = {
                                if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                onTestNotification()
                                Toast.makeText(context, "اعلان و زنگ تست با موفقیت ارسال شد!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        else -> {
            // ==========================================
            // صفحه اصلی تنظیمات (Main Settings)
            // ==========================================
            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    LiquidTopBar(
                        title = "تنظیمات و پروفایل",
                        actions = {
                            LiquidCircleButton(
                                icon = Icons.Filled.Edit,
                                contentDescription = "ویرایش مشخصات",
                                onClick = onOpenOnboarding
                            )
                        }
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .testTag("profile_screen"),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Header Card
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SolidGroupedCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenOnboarding() }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(LiquidTheme.surfaceElevated)
                                            .clickable { avatarPickerLauncher.launch("image/*") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (userSettings.profilePhotoUri.isNotBlank()) {
                                            AsyncImage(
                                                model = userSettings.profilePhotoUri,
                                                contentDescription = "عکس پروفایل",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.Person,
                                                contentDescription = null,
                                                tint = LiquidTheme.accentColor,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }

                                        // Camera edit badge
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(LiquidTheme.accentColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CameraAlt,
                                                contentDescription = "تغییر عکس",
                                                tint = LiquidTheme.onAccent,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (userSettings.userName.isNotBlank()) userSettings.userName else "ورزشکار حرفه‌ای",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = LiquidTheme.textPrimary
                                        )
                                        Text(
                                            text = "هدف: ${
                                                when (userSettings.fitnessGoal) {
                                                    "BULK" -> "افزایش حجم عضلانی"
                                                    "CUT" -> "کاهش چربی و تفکیک"
                                                    else -> "حفظ تناسب و سلامت عمومی"
                                                }
                                            }",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LiquidTheme.textSecondary
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = LiquidTheme.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Group 1: Appearance & Reminders
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LiquidSectionHeader(title = "تنظیمات عمومی")

                                SolidGroupedCard {
                                    GroupedRow(
                                        title = "ظاهر و پالت‌های رنگی",
                                        subtitle = "حالت سیستم و ۸ پالت اختصاصی Liquid Glass",
                                        icon = Icons.Filled.Palette,
                                        iconTint = LiquidTheme.iconPurple,
                                        showDivider = true,
                                        onClick = { currentSubScreen = "appearance" }
                                    )

                                    GroupedRow(
                                        title = "یادآورها و هشدارها",
                                        subtitle = "تمرین، مصرف آب و مکمل‌ها",
                                        icon = Icons.Filled.Notifications,
                                        iconTint = LiquidTheme.iconOrange,
                                        showDivider = false,
                                        onClick = { currentSubScreen = "reminders" }
                                    )
                                }
                            }
                        }
                    }

                    // Group 2: Dedicated File Storage Backup, Restore & Updates
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LiquidSectionHeader(title = "پشتیبان‌گیری و بروزرسانی")

                                SolidGroupedCard {
                                    GroupedRow(
                                        title = "ایجاد فایل پشتیبان جدید در حافظه",
                                        subtitle = "ذخیره فایل JSON در پوشه دانلودها و حافظه برنامه",
                                        icon = Icons.Filled.Save,
                                        iconTint = LiquidTheme.iconBlue,
                                        showDivider = true,
                                        modifier = Modifier.testTag("create_backup_file_row"),
                                        onClick = {
                                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !BackupHelper.hasLegacyStoragePermission(context)) {
                                                pendingStorageAction = "BACKUP"
                                                legacyStoragePermissionLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                        Manifest.permission.READ_EXTERNAL_STORAGE
                                                    )
                                                )
                                            } else {
                                                performBackupExport()
                                            }
                                        }
                                    )

                                    GroupedRow(
                                        title = "ذخیره فایل پشتیبان در مسیر دلخواه",
                                        subtitle = "انتخاب پوشه دلخواه با مدیریت فایل سیستم",
                                        icon = Icons.Filled.CreateNewFolder,
                                        iconTint = LiquidTheme.iconPurple,
                                        showDivider = true,
                                        modifier = Modifier.testTag("save_backup_saf_row"),
                                        onClick = {
                                            try {
                                                createBackupDocumentLauncher.launch("gym_pro_backup.json")
                                            } catch (_: Exception) {
                                                Toast.makeText(context, "امکان باز کردن انتخابگر پوشه وجود ندارد.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )

                                    GroupedRow(
                                        title = "بازیابی اطلاعات از فایل پشتیبان",
                                        subtitle = "بازیابی از پوشه برنامه یا انتخاب فایل از گوشی",
                                        icon = Icons.Filled.Restore,
                                        iconTint = LiquidTheme.iconGreen,
                                        showDivider = true,
                                        modifier = Modifier.testTag("restore_backup_file_row"),
                                        onClick = {
                                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !BackupHelper.hasLegacyStoragePermission(context)) {
                                                pendingStorageAction = "RESTORE"
                                                legacyStoragePermissionLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.READ_EXTERNAL_STORAGE,
                                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                                    )
                                                )
                                            } else {
                                                showRestoreDialog = true
                                            }
                                        }
                                    )

                                    GroupedRow(
                                        title = "بروزرسانی برنامه",
                                        subtitle = "نسخه فعلی: ${PersianNumberHelper.toPersianDigits(BuildConfig.VERSION_NAME)} • بررسی و دانلود نسخه جدید",
                                        icon = Icons.Filled.SystemUpdate,
                                        iconTint = LiquidTheme.accentColor,
                                        showDivider = true,
                                        modifier = Modifier.testTag("check_update_row"),
                                        onClick = { showUpdateDialog = true }
                                    )

                                    GroupedRow(
                                        title = "خروجی هوشمند برای هوش مصنوعی",
                                        subtitle = "پرامپت آنالیز جامع تمرین و تغذیه",
                                        icon = Icons.Filled.AutoAwesome,
                                        iconTint = LiquidTheme.iconIndigo,
                                        showDivider = true,
                                        onClick = {
                                            coroutineScope.launch {
                                                aiPromptContent = onGetAiPrompt()
                                                showAiExportDialog = true
                                            }
                                        }
                                    )

                                    GroupedRow(
                                        title = "درباره GYM",
                                        subtitle = "انگیزشی • ارتباط با توسعه‌دهنده",
                                        icon = Icons.Filled.Info,
                                        iconTint = LiquidTheme.iconCyan,
                                        showDivider = false,
                                        onClick = { showAboutDialog = true }
                                    )
                                }
                            }
                        }
                    }

                    // Group 3: Destructive Group (Clear All Data in red)
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LiquidSectionHeader(title = "منطقه حساس")

                                SolidGroupedCard {
                                    GroupedRow(
                                        title = "حذف کامل تمامی اطلاعات",
                                        subtitle = "پاک کردن تمامی برنامه‌ها، سوابق و تنظیمات",
                                        icon = Icons.Filled.DeleteForever,
                                        iconTint = SemanticError,
                                        showArrow = false,
                                        showDivider = false,
                                        onClick = { showClearDataConfirm = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Backup Created Success Dialog
    if (showBackupSuccessDialog) {
        val result = lastSavedBackupResult
        val file = result?.appFile ?: BackupHelper.getAppBackupFile(context)
        val sizeKb = (file.length() / 1024).coerceAtLeast(1)
        val displayLoc = result?.displayLocation ?: "پوشه اختصاصی GymPro_Backups"
        LiquidAlertDialog(
            onDismissRequest = { showBackupSuccessDialog = false },
            title = "فایل پشتیبان ذخیره شد",
            message = if (result?.exportedToDownloads == true) {
                "فایل پشتیبان با موفقیت در پوشه دانلودها و حافظه اختصاصی برنامه ذخیره شد."
            } else {
                "فایل پشتیبان در پوشه اختصاصی برنامه ذخیره شد. همچنین می‌توانید آن را به صورت مستقیم به اشتراک بگذارید."
            },
            confirmText = "ارسال و اشتراک فایل",
            dismissText = "بستن",
            onConfirm = {
                try {
                    val shareUri = BackupHelper.getShareUri(context, file)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_SUBJECT, "فایل پشتیبان GYM PRO")
                        putExtra(Intent.EXTRA_STREAM, shareUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری فایل پشتیبان"))
                } catch (_: Exception) {
                    Toast.makeText(context, "فایل در مسیر $displayLoc ذخیره شده است.", Toast.LENGTH_SHORT).show()
                }
            },
            content = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = LiquidTheme.surfaceElevated
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("نام فایل: ${file.name}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = LiquidTheme.textPrimary)
                        Text("حجم فایل: ${PersianNumberHelper.toPersianDigits(sizeKb)} کیلوبایت", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                        Text("مسیر: $displayLoc", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                    }
                }
            }
        )
    }

    // Restore from File Dialog
    if (showRestoreDialog) {
        val backupFiles = remember(showRestoreDialog) {
            BackupHelper.listBackupFilesSafely(context)
        }

        LiquidAlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = "بازیابی از فایل پشتیبان",
            message = if (backupFiles.isNotEmpty()) "فایل موردنظر را از لیست زیر انتخاب کنید یا فایل JSON را از حافظه گوشی برگزینید:" else "فایل پشتیبان خود را از حافظه گوشی انتخاب کنید:",
            confirmText = "انتخاب فایل از حافظه",
            dismissText = "انصراف",
            onConfirm = {
                try {
                    restoreDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                } catch (_: Exception) {
                    Toast.makeText(context, "برنامه مدیریت فایلی برای انتخاب فایل در دسترس نیست.", Toast.LENGTH_LONG).show()
                }
            },
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (backupFiles.isNotEmpty()) {
                        Text(
                            text = "فایل‌های آماده در حافظه برنامه:",
                            style = MaterialTheme.typography.labelSmall,
                            color = LiquidTheme.textSecondary
                        )
                        backupFiles.take(5).forEach { file ->
                            val sizeKb = (file.length() / 1024).coerceAtLeast(1)
                            val modifiedDate = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.US).format(java.util.Date(file.lastModified()))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .liquidTapEffect {
                                        coroutineScope.launch {
                                            val readRes = BackupHelper.readBackupFromFile(file)
                                            if (readRes.isSuccess) {
                                                val c = readRes.getOrNull()!!
                                                val json = BackupHelper.createBackupJson(
                                                    settings = c.settings,
                                                    plans = c.plans,
                                                    exercises = c.exercises,
                                                    histories = c.histories,
                                                    skips = c.skips,
                                                    supplements = c.supplements,
                                                    supplementHistories = c.supplementHistories,
                                                    nutritionLogs = c.nutritionLogs,
                                                    measurements = c.measurements
                                                )
                                                val res = onRestoreBackupJson(json)
                                                if (res.isSuccess) {
                                                    showRestoreDialog = false
                                                    Toast.makeText(context, "اطلاعات با موفقیت از فایل «${file.name}» بازیابی شد!", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "خطا در واردسازی دیتابیس.", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "فایل پشتیبان نامعتبر است.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = LiquidTheme.surfaceElevated
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(LiquidTheme.accentColor.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.RestorePage,
                                            contentDescription = null,
                                            tint = LiquidTheme.accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = LiquidTheme.textPrimary
                                        )
                                        Text(
                                            text = "تاریخ: $modifiedDate • حجم: ${PersianNumberHelper.toPersianDigits(sizeKb)} KB",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LiquidTheme.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    // AI Prompt Export Dialog
    if (showAiExportDialog) {
        LiquidAlertDialog(
            onDismissRequest = { showAiExportDialog = false },
            title = "پرامپت هوشمند تحلیل ورزشی",
            message = "این متن شامل داده‌های بدنی و تمرینی شما برای ارائه به هوش مصنوعی است:",
            confirmText = "کپی در کلیپ‌بورد",
            dismissText = "بستن",
            onConfirm = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Gym AI Prompt", aiPromptContent))
                Toast.makeText(context, "در کلیپ‌بورد کپی شد!", Toast.LENGTH_SHORT).show()
            },
            content = {
                LiquidInputField(
                    value = aiPromptContent,
                    onValueChange = {},
                    label = "متن پرامپت",
                    singleLine = false,
                    modifier = Modifier.height(140.dp)
                )
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearDataConfirm) {
        LiquidAlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = "تأیید حذف کامل داده‌ها",
            message = "آیا مطمئن هستید؟ با این کار تمامی برنامه‌ها، سوابق جلسات، ثبت‌های تغذیه، اندازه‌های بدن و تنظیمات برای همیشه حذف خواهند شد.",
            confirmText = "بله، همه را حذف کن",
            dismissText = "انصراف",
            isDestructive = true,
            onConfirm = {
                onClearAllData()
                Toast.makeText(context, "تمامی داده‌ها با موفقیت پاک شدند.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // About Dialog with Motivational Bodybuilding Quote & Clickable Telegram / Instagram Links
    if (showAboutDialog) {
        LiquidAlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = "درباره GYM",
            message = "«بدن شما تنها خانه‌ای است که تا پایان عمر در آن زندگی می‌کنید. هر قطره عرق، هر ست سنگین و هر ثانیه‌ای که تسلیم نشدید، ساختن نسخه‌ای قوی‌تر، شجاع‌تر و متعهدتر از خودِ شماست. درد تمرین موقتی است، اما افتخار ساختن قهرمان درونتان تا ابد باقی می‌ماند!»",
            confirmText = "بستن",
            onConfirm = {},
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ارتباط با توسعه‌دهنده و سازنده:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = LiquidTheme.textPrimary
                    )

                    // Telegram Button Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidTapEffect {
                                try {
                                    val tgIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/momo1f"))
                                    context.startActivity(tgIntent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "خطا در باز کردن تلگرام", Toast.LENGTH_SHORT).show()
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = BrandTelegram.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_telegram),
                                contentDescription = "تلگرام توسعه‌دهنده",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "تلگرام توسعه‌دهنده",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = LiquidTheme.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = LiquidTheme.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Instagram Button Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidTapEffect {
                                try {
                                    val igIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/die3man"))
                                    context.startActivity(igIntent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "خطا در باز کردن اینستاگرام", Toast.LENGTH_SHORT).show()
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = BrandInstagram.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_instagram),
                                contentDescription = "اینستاگرام توسعه‌دهنده",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "اینستاگرام توسعه‌دهنده",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = LiquidTheme.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = LiquidTheme.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        )
    }

    if (timePickerTarget != null) {
        val currTarget = timePickerTarget!!
        val currTime = when (currTarget) {
            "WORKOUT" -> userSettings.workoutReminderTime
            "WATER" -> userSettings.waterReminderTime
            else -> userSettings.supplementReminderTime
        }
        val currTitle = when (currTarget) {
            "WORKOUT" -> "ساعت یادآور تمرین"
            "WATER" -> "ساعت یادآور آب"
            else -> "ساعت یادآور مکمل‌ها"
        }
        val isEnabled = when (currTarget) {
            "WORKOUT" -> userSettings.workoutReminderEnabled
            "WATER" -> userSettings.waterReminderEnabled
            else -> userSettings.supplementReminderEnabled
        }
        LiquidTimePickerDialog(
            initialTime = currTime,
            title = currTitle,
            onDismissRequest = { timePickerTarget = null },
            onConfirm = { newTime ->
                onToggleReminder(currTarget, isEnabled, newTime)
                timePickerTarget = null
                Toast.makeText(context, "ساعت اعلان با موفقیت تنظیم شد: ${PersianNumberHelper.toPersianDigits(newTime)}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showUpdateDialog) {
        AppUpdateDialog(
            onDismissRequest = { showUpdateDialog = false }
        )
    }
}

@Composable
private fun AppUpdateDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val targetRepo = remember { AppUpdateHelper.getEffectiveRepo() }

    var isChecking by remember { mutableStateOf(false) }
    var checkError by remember { mutableStateOf<String?>(null) }
    var upToDateVersion by remember { mutableStateOf<String?>(null) }
    var availableUpdate by remember { mutableStateOf<ReleaseUpdateInfo?>(null) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var downloadedBytes by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(0L) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    fun triggerCheck() {
        isChecking = true
        checkError = null
        upToDateVersion = null
        availableUpdate = null
        downloadedFile = null
        downloadError = null

        scope.launch {
            val res = AppUpdateHelper.checkForUpdate(targetRepo)
            isChecking = false
            res.fold(
                onSuccess = { outcome ->
                    when (outcome) {
                        is UpdateCheckResult.UpToDate -> {
                            upToDateVersion = outcome.latestVersion
                        }
                        is UpdateCheckResult.UpdateAvailable -> {
                            availableUpdate = outcome.info
                        }
                    }
                },
                onFailure = { err ->
                    checkError = err.message ?: "خطا در بررسی نسخه جدید."
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        triggerCheck()
    }

    DisposableEffect(Unit) {
        onDispose {
            downloadJob?.cancel()
        }
    }

    fun startDownloadAndInstall(info: ReleaseUpdateInfo) {
        if (isDownloading) return
        isDownloading = true
        downloadError = null
        downloadProgress = 0
        downloadedBytes = 0L
        totalBytes = info.apkSizeBytes
        downloadJob = scope.launch {
            val res = AppUpdateHelper.downloadApk(
                context = context,
                downloadUrl = info.apkDownloadUrl,
                fileName = info.apkName,
                onProgress = { pct, dl, total ->
                    downloadProgress = pct
                    downloadedBytes = dl
                    totalBytes = total
                }
            )
            isDownloading = false
            res.fold(
                onSuccess = { apk ->
                    downloadedFile = apk
                    when (AppUpdateHelper.installApk(context, apk)) {
                        InstallLaunchStatus.NEEDS_PERMISSION -> {
                            Toast.makeText(
                                context,
                                "دانلود کامل شد! مجوز نصب را فعال کنید و سپس روی «نصب نسخه دانلود شده» بزنید.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        InstallLaunchStatus.ERROR -> {
                            downloadError = "فایل دانلود شد اما اجرای نصب با خطا مواجه شد."
                        }
                        InstallLaunchStatus.LAUNCHED -> Unit
                    }
                },
                onFailure = { err ->
                    downloadError = err.message ?: "خطا در دانلود فایل بروزرسانی."
                }
            )
        }
    }

    fun handleInstallReadyApk(apk: File) {
        when (AppUpdateHelper.installApk(context, apk)) {
            InstallLaunchStatus.NEEDS_PERMISSION -> {
                Toast.makeText(
                    context,
                    "لطفاً مجوز نصب برنامه از منابع ناشناس را فعال کرده و سپس دکمه نصب را دوباره بزنید.",
                    Toast.LENGTH_LONG
                ).show()
            }
            InstallLaunchStatus.ERROR -> {
                Toast.makeText(context, "خطا در اجرای نصب‌کننده.", Toast.LENGTH_SHORT).show()
            }
            InstallLaunchStatus.LAUNCHED -> Unit
        }
    }

    val closeDialog: () -> Unit = {
        downloadJob?.cancel()
        onDismissRequest()
    }

    val actionLabel: String? = when {
        downloadedFile != null -> "نصب نسخه جدید"
        isDownloading -> null
        availableUpdate != null -> "دانلود و نصب"
        isChecking -> null
        checkError != null || upToDateVersion != null -> "بررسی مجدد"
        else -> "بررسی بروزرسانی"
    }

    Dialog(
        onDismissRequest = {
            if (!isDownloading) closeDialog()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(enabled = !isDownloading) { closeDialog() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .width(330.dp)
                    .clickable(enabled = false) {}
                    .liquidGlass(
                        hazeState = null,
                        shape = RoundedCornerShape(26.dp),
                        tintAlpha = if (LiquidTheme.isDark) 0.90f else 0.95f,
                        elevation = 16.dp
                    )
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "بروزرسانی GYM PRO",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = LiquidTheme.textPrimary
                        )
                        Text(
                            text = "نسخه فعلی نصب‌شده: ${PersianNumberHelper.toPersianDigits(BuildConfig.VERSION_NAME)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LiquidTheme.textSecondary,
                            textAlign = TextAlign.Center
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when {
                                isChecking -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        LiquidGlassSpinner(size = 54.dp)
                                        Text(
                                            text = "در حال بررسی آخرین نسخه...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LiquidTheme.textSecondary
                                        )
                                    }
                                }

                                availableUpdate != null -> {
                                    val info = availableUpdate!!
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = LiquidTheme.surfaceElevated
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "نسخه جدید موجود است!",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SemanticSuccess
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = LiquidTheme.accentColor.copy(alpha = 0.18f)
                                                ) {
                                                    Text(
                                                        text = "v${PersianNumberHelper.toPersianDigits(info.versionName)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = LiquidTheme.accentColor,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                            if (info.apkSizeBytes > 0L) {
                                                val sizeMb = String.format(java.util.Locale.US, "%.1f", info.apkSizeBytes / (1024.0 * 1024.0))
                                                Text(
                                                    text = "حجم فایل: ${PersianNumberHelper.toPersianDigits(sizeMb)} مگابایت",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = LiquidTheme.textSecondary
                                                )
                                            }
                                            if (info.changelog.isNotBlank()) {
                                                Text(
                                                    text = info.changelog,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = LiquidTheme.textPrimary
                                                )
                                            }
                                        }
                                    }

                                    if (isDownloading) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val dlMb = String.format(java.util.Locale.US, "%.1f", downloadedBytes / (1024.0 * 1024.0))
                                            val pctText = if (downloadProgress >= 0) {
                                                val totalText = if (totalBytes > 0L) {
                                                    " (${PersianNumberHelper.toPersianDigits(dlMb)} / ${PersianNumberHelper.toPersianDigits(String.format(java.util.Locale.US, "%.1f", totalBytes / (1024.0 * 1024.0)))} MB)"
                                                } else ""
                                                "${PersianNumberHelper.toPersianDigits(downloadProgress)}٪$totalText"
                                            } else {
                                                "${PersianNumberHelper.toPersianDigits(dlMb)} مگابایت"
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "در حال دانلود فایل APK...",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = LiquidTheme.textSecondary
                                                )
                                                Text(
                                                    pctText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LiquidTheme.accentColor
                                                )
                                            }
                                            if (downloadProgress >= 0) {
                                                LinearProgressIndicator(
                                                    progress = { (downloadProgress / 100f).coerceIn(0f, 1f) },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(8.dp)
                                                        .clip(CircleShape),
                                                    color = LiquidTheme.accentColor,
                                                    trackColor = LiquidTheme.surfaceElevated
                                                )
                                            } else {
                                                LinearProgressIndicator(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(8.dp)
                                                        .clip(CircleShape),
                                                    color = LiquidTheme.accentColor,
                                                    trackColor = LiquidTheme.surfaceElevated
                                                )
                                            }
                                        }
                                    } else if (downloadedFile != null) {
                                        LiquidPrimaryButton(
                                            text = "نصب نسخه دانلود شده",
                                            icon = Icons.Filled.InstallMobile,
                                            onClick = { handleInstallReadyApk(downloadedFile!!) },
                                            modifier = Modifier.testTag("btn_install_downloaded_apk")
                                        )
                                    } else {
                                        LiquidPrimaryButton(
                                            text = "دانلود و نصب مستقیم",
                                            icon = Icons.Filled.Download,
                                            onClick = { startDownloadAndInstall(info) },
                                            modifier = Modifier.testTag("btn_download_update_apk")
                                        )
                                    }

                                    if (!downloadError.isNullOrBlank()) {
                                        Text(
                                            text = downloadError!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SemanticError
                                        )
                                    }
                                }

                                upToDateVersion != null -> {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = SemanticSuccess.copy(alpha = 0.14f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SemanticSuccess)
                                            Text(
                                                text = "آخرین نسخه نصب است (نسخه ${PersianNumberHelper.toPersianDigits(upToDateVersion!!)})",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = LiquidTheme.textPrimary
                                            )
                                        }
                                    }
                                }

                                !checkError.isNullOrBlank() -> {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = SemanticError.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = checkError!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SemanticError,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(LiquidTheme.dividerColor)
                    )

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("btn_update_dialog_close")
                                .clickable { closeDialog() },
                            Alignment.Center
                        ) {
                            Text(
                                text = if (isDownloading) "انصراف از دانلود" else "بستن",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isDownloading) SemanticError else LiquidTheme.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (actionLabel != null) {
                            Box(
                                Modifier
                                    .width(0.5.dp)
                                    .fillMaxHeight()
                                    .background(LiquidTheme.dividerColor)
                            )
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .testTag("btn_update_dialog_action")
                                    .clickable {
                                        val readyApk = downloadedFile
                                        val update = availableUpdate
                                        when {
                                            readyApk != null -> handleInstallReadyApk(readyApk)
                                            update != null -> startDownloadAndInstall(update)
                                            else -> triggerCheck()
                                        }
                                    },
                                Alignment.Center
                            ) {
                                Text(
                                    text = actionLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LiquidTheme.accentColor
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
private fun PaletteCard(
    palette: LiquidPalette,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .liquidTapEffect { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.getCardGradient())
                .background(Color.Black.copy(alpha = 0.24f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = palette.persianName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = palette.dayAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
