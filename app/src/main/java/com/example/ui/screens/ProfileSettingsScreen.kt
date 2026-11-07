package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.UserSettings
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.BackupHelper
import com.example.util.NotificationHelper
import com.example.util.PersianNumberHelper
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
    onOpenOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentSubScreen by remember { mutableStateOf<String?>(null) } // null, "appearance", "reminders", "goals"
    var searchQuery by remember { mutableStateOf("") }

    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAiExportDialog by remember { mutableStateOf(false) }
    var aiPromptContent by remember { mutableStateOf("") }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showBackupSuccessDialog by remember { mutableStateOf(false) }
    var lastSavedBackupFile by remember { mutableStateOf<File?>(null) }

    // Permission launcher for Android 13+
    var hasNotifPermission by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }
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

    // File picker launcher to restore any .json file
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
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
                    Toast.makeText(context, "فرمت فایل نامعتبر است.", Toast.LENGTH_LONG).show()
                }
            }
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
                containerColor = LiquidTheme.backgroundNeutral,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    LiquidTopBar(
                        title = "شخصی‌سازی ظاهر",
                        navigationIcon = {
                            LiquidCircleButton(
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
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
                containerColor = LiquidTheme.backgroundNeutral,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    LiquidTopBar(
                        title = "تنظیم یادآورها",
                        navigationIcon = {
                            LiquidCircleButton(
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
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

                    item {
                        LiquidSectionHeader(title = "هشدارهای روزانه")

                        SolidGroupedCard {
                            GroupedRow(
                                title = "یادآور تمرین ورزشی",
                                subtitle = "ساعت: ${PersianNumberHelper.toPersianDigits(userSettings.workoutReminderTime)}",
                                icon = Icons.Filled.FitnessCenter,
                                iconTint = SemanticIconRed,
                                showArrow = false,
                                showDivider = true,
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
                                subtitle = "ساعت: ${PersianNumberHelper.toPersianDigits(userSettings.waterReminderTime)}",
                                icon = Icons.Filled.WaterDrop,
                                iconTint = SemanticIconCyan,
                                showArrow = false,
                                showDivider = true,
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
                                subtitle = "ساعت: ${PersianNumberHelper.toPersianDigits(userSettings.supplementReminderTime)}",
                                icon = Icons.Filled.Medication,
                                iconTint = SemanticIconGreen,
                                showArrow = false,
                                showDivider = false,
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

                    item {
                        LiquidPrimaryButton(
                            text = "تست ارسال اعلان و آلارم صوتی",
                            icon = Icons.Filled.NotificationsActive,
                            onClick = {
                                if (!hasNotifPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                onTestNotification()
                                NotificationHelper.playAlarmSoundAndVibrate(context)
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
                containerColor = LiquidTheme.backgroundNeutral,
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
                    contentPadding = PaddingValues(bottom = 110.dp),
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
                                            .background(LiquidTheme.surfaceElevated),
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
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                                        iconTint = SemanticIconPurple,
                                        showDivider = true,
                                        onClick = { currentSubScreen = "appearance" }
                                    )

                                    GroupedRow(
                                        title = "یادآورها و هشدارها",
                                        subtitle = "تمرین، مصرف آب و مکمل‌ها",
                                        icon = Icons.Filled.Notifications,
                                        iconTint = SemanticIconOrange,
                                        showDivider = false,
                                        onClick = { currentSubScreen = "reminders" }
                                    )
                                }
                            }
                        }
                    }

                    // Group 2: Dedicated File Storage Backup & Restore
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                LiquidSectionHeader(title = "پشتیبان‌گیری در حافظه گوشی")

                                SolidGroupedCard {
                                    GroupedRow(
                                        title = "ایجاد فایل پشتیبان جدید در حافظه",
                                        subtitle = "ذخیره و جایگزینی فایل اختصاصی در حافظه گوشی",
                                        icon = Icons.Filled.Save,
                                        iconTint = SemanticIconBlue,
                                        showDivider = true,
                                        onClick = {
                                            coroutineScope.launch {
                                                val json = onExportBackupJson()
                                                val savedFile = BackupHelper.saveBackupToFile(context, json)
                                                lastSavedBackupFile = savedFile
                                                showBackupSuccessDialog = true
                                            }
                                        }
                                    )

                                    GroupedRow(
                                        title = "بازیابی اطلاعات از فایل پشتیبان",
                                        subtitle = "بازیابی از آخرین فایل ذخیره‌شده یا انتخاب فایل",
                                        icon = Icons.Filled.Restore,
                                        iconTint = SemanticIconGreen,
                                        showDivider = true,
                                        onClick = { showRestoreDialog = true }
                                    )

                                    GroupedRow(
                                        title = "خروجی هوشمند برای هوش مصنوعی",
                                        subtitle = "پرامپت آنالیز جامع برای Gemini",
                                        icon = Icons.Filled.AutoAwesome,
                                        iconTint = SemanticIconIndigo,
                                        showDivider = true,
                                        onClick = {
                                            coroutineScope.launch {
                                                aiPromptContent = onGetAiPrompt()
                                                showAiExportDialog = true
                                            }
                                        }
                                    )

                                    GroupedRow(
                                        title = "درباره GYM PRO",
                                        subtitle = "انگیزشی • ارتباط با توسعه‌دهنده",
                                        icon = Icons.Filled.Info,
                                        iconTint = SemanticIconCyan,
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
        val file = lastSavedBackupFile ?: BackupHelper.getAppBackupFile(context)
        val sizeKb = (file.length() / 1024).coerceAtLeast(1)
        LiquidAlertDialog(
            onDismissRequest = { showBackupSuccessDialog = false },
            title = "پشتیبان‌گیری در حافظه ذخیره شد",
            message = "فایل جدید در پوشه اختصاصی برنامه ذخیره شد و جایگزین نسخه قبلی گردید.",
            confirmText = "ارسال و اشتراک فایل",
            dismissText = "بستن",
            onConfirm = {
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "پشتیبان GYM PRO")
                        putExtra(Intent.EXTRA_TEXT, file.readText())
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری فایل پشتیبان"))
                } catch (_: Exception) {
                    Toast.makeText(context, "فایل در مسیر پوشه برنامه ذخیره است.", Toast.LENGTH_SHORT).show()
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
                        Text("مسیر: پوشه اختصاصی GymPro_Backups", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                    }
                }
            }
        )
    }

    // Restore from File Dialog
    if (showRestoreDialog) {
        val existingFile = BackupHelper.getExistingBackupFile(context)
        LiquidAlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = "بازیابی از فایل پشتیبان",
            message = "می‌توانید از آخرین فایل ذخیره‌شده در حافظه بازیابی کنید یا یک فایل دیگر را انتخاب نمایید:",
            confirmText = "انتخاب فایل JSON از حافظه",
            dismissText = "انصراف",
            onConfirm = {
                filePickerLauncher.launch("*/*")
            },
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (existingFile != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidTapEffect {
                                    coroutineScope.launch {
                                        val readRes = BackupHelper.readBackupFromFile(existingFile)
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
                                                Toast.makeText(context, "اطلاعات با موفقیت از آخرین نسخه ذخیره‌شده بازیابی شد!", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = LiquidTheme.accentColor.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.RestorePage, contentDescription = null, tint = LiquidTheme.accentColor)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("بازیابی سریع از آخرین فایل ذخیره‌شده", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LiquidTheme.accentColor)
                                    Text("نام فایل: ${existingFile.name} (${existingFile.length() / 1024} KB)", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
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
            title = "درباره GYM PRO",
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
                                    Toast.makeText(context, "تلگرام: @momo1f", Toast.LENGTH_SHORT).show()
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF29B6F6).copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_telegram),
                                contentDescription = "تلگرام",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تلگرام: @momo1f", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LiquidTheme.textPrimary)
                                Text("مشاهده کانال و ارتباط مستقیم", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = LiquidTheme.textSecondary, modifier = Modifier.size(16.dp))
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
                                    Toast.makeText(context, "اینستاگرام: @die3man", Toast.LENGTH_SHORT).show()
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFE1306C).copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_instagram),
                                contentDescription = "اینستاگرام",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("اینستاگرام: @die3man", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LiquidTheme.textPrimary)
                                Text("مشاهده صفحه و محتوای انگیزشی", style = MaterialTheme.typography.labelSmall, color = LiquidTheme.textSecondary)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = LiquidTheme.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        )
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
