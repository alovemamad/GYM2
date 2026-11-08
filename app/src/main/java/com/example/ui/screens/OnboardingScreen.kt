package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable // 👈 اضافه شد
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.UserSettings
import com.example.ui.components.*
import com.example.ui.theme.LiquidTheme
import com.example.util.ImageStorageHelper
import com.example.util.PersianNumberHelper

@Composable
fun OnboardingScreen(
    currentSettings: UserSettings,
    onSaveAndContinue: (name: String, photoUri: String, gender: String, age: Int?, height: Double?, weight: Double?, activity: String, goal: String, meals: Int, water: Int, unit: String) -> Unit,
    onSkip: () -> Unit,
    onRestoreBackupClick: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(currentSettings.userName) }
    var photoUri by rememberSaveable { mutableStateOf(currentSettings.profilePhotoUri) }
    var gender by rememberSaveable { mutableStateOf(currentSettings.gender.ifBlank { "MALE" }) }
    var ageText by rememberSaveable { mutableStateOf(currentSettings.age?.toString() ?: "") }
    var heightText by rememberSaveable { mutableStateOf(currentSettings.heightCm?.toString() ?: "") }
    var weightText by rememberSaveable { mutableStateOf(currentSettings.weightKg?.toString() ?: "") }
    var activityLevel by rememberSaveable { mutableStateOf(currentSettings.activityLevel) }
    var fitnessGoal by rememberSaveable { mutableStateOf(currentSettings.fitnessGoal) }
    var waterGoalText by rememberSaveable { mutableStateOf(currentSettings.waterGoalMl.toString()) }
    var weightUnit by rememberSaveable { mutableStateOf(currentSettings.weightUnit) }
    var mealsText by rememberSaveable { mutableStateOf(currentSettings.mealsCount.toString()) }
    var step by rememberSaveable { mutableStateOf(0) }
    val lastStep = 2
    BackHandler(enabled = step > 0) { step -= 1 }

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = ImageStorageHelper.saveProfilePhotoPermanently(context, uri)
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            LiquidTopBar(
                title = "راه‌اندازی اولیه",
                endAction = {
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text(
                            "رد کردن",
                            color = LiquidTheme.accentColor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // نشانگر مرحله
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..lastStep) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (i <= step) LiquidTheme.accentColor
                                else LiquidTheme.accentColor.copy(alpha = 0.18f)
                            )
                    )
                }
            }
            Text(
                text = "مرحله ${PersianNumberHelper.toPersianDigits(step + 1)} از ${PersianNumberHelper.toPersianDigits(lastStep + 1)}",
                style = MaterialTheme.typography.labelMedium,
                color = LiquidTheme.textSecondary
            )

            if (step == 0) {
            // Hero Photo Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(LiquidTheme.surfaceElevated)
                        .border(2.dp, LiquidTheme.accentColor, CircleShape)
                        .clickable { photoPickerLauncher.launch("image/*") }
                        .testTag("onboarding_photo_picker"),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri.isNotBlank()) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "عکس پروفایل",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AddAPhoto,
                            contentDescription = "افزودن عکس",
                            tint = LiquidTheme.accentColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "عکس پروفایل (اختیاری)",
                    style = MaterialTheme.typography.bodySmall,
                    color = LiquidTheme.textSecondary
                )
            }

            // Section 1: Basic Info
            LiquidSectionHeader(title = "مشخصات فردی")
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
                        label = "نام یا لقب ورزشی",
                        leadingIcon = Icons.Filled.Person,
                        modifier = Modifier.testTag("onboarding_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidInputField(
                            value = ageText,
                            onValueChange = { ageText = it },
                            label = "سن (سال)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_age_input")
                        )
                        LiquidInputField(
                            value = heightText,
                            onValueChange = { heightText = it },
                            label = "قد (سانتی‌متر)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_height_input")
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "جنسیت",
                            style = MaterialTheme.typography.bodySmall,
                            color = LiquidTheme.textSecondary
                        )
                        LiquidSegmentedControl(
                            items = listOf("آقایان", "بانوان"),
                            selectedIndex = if (gender == "FEMALE") 1 else 0,
                            onSelectIndex = { index -> gender = if (index == 0) "MALE" else "FEMALE" }
                        )
                    }
                }
            }

            // Section 2: Weight & Unit
            LiquidSectionHeader(title = "وزن و مقیاس")
            SolidGroupedCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidInputField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = "وزن فعلی ($weightUnit)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_weight_input")
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            LiquidSegmentedControl(
                                items = listOf("کیلوگرم", "پوند"),
                                selectedIndex = if (weightUnit == "lb") 1 else 0,
                                onSelectIndex = { index -> weightUnit = if (index == 0) "kg" else "lb" }
                            )
                        }
                    }
                }
            }

            }

            if (step == 1) {
            // Section 3: Goal
            LiquidSectionHeader(title = "هدف اصلی تمرینی")
            SolidGroupedCard {
                val goals = listOf(
                    "BULK" to ("افزایش حجم عضلانی" to "Bulking - عضله‌سازی و افزایش وزن هدفمند"),
                    "CUT" to ("کاهش چربی و تفکیک" to "Cutting - حفظ عضله و کاهش درصد چربی"),
                    "MAINTAIN" to ("حفظ تناسب و آمادگی" to "Maintenance - تثبیت و سلامت عمومی")
                )
                goals.forEachIndexed { index, (key, info) ->
                    val isSelected = fitnessGoal == key
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { fitnessGoal = key }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                info.first,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) LiquidTheme.accentColor else LiquidTheme.textPrimary
                            )
                            Text(
                                info.second,
                                style = MaterialTheme.typography.labelSmall,
                                color = LiquidTheme.textSecondary
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { fitnessGoal = key },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = LiquidTheme.accentColor,
                                unselectedColor = LiquidTheme.textSecondary.copy(alpha = 0.5f)
                            )
                        )
                    }
                    if (index < goals.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = LiquidTheme.dividerColor,
                            thickness = 0.5.dp
                        )
                    }
                }
            }

            // Section 4: Activity Level
            LiquidSectionHeader(title = "سطح فعالیت روزانه")
            SolidGroupedCard {
                val activities = listOf(
                    "SEDENTARY" to ("کم‌تحرک" to "پشت‌میزنشین، کمترین فعالیت فیزیکی"),
                    "MODERATE" to ("متوسط" to "۱ تا ۳ روز ورزش هوازی یا تمرین سبک"),
                    "ACTIVE" to ("پرتحرک" to "۴ تا ۵ روز تمرین ورزشی منظم"),
                    "ATHLETE" to ("ورزشکار حرفه‌ای" to "تمرین سنگین روزانه یا دو نوبت")
                )
                activities.forEachIndexed { index, (key, info) ->
                    val isSelected = activityLevel == key
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activityLevel = key }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                info.first,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) LiquidTheme.accentColor else LiquidTheme.textPrimary
                            )
                            Text(
                                info.second,
                                style = MaterialTheme.typography.labelSmall,
                                color = LiquidTheme.textSecondary
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { activityLevel = key },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = LiquidTheme.accentColor,
                                unselectedColor = LiquidTheme.textSecondary.copy(alpha = 0.5f)
                            )
                        )
                    }
                    if (index < activities.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = LiquidTheme.dividerColor,
                            thickness = 0.5.dp
                        )
                    }
                }
            }

            }

            if (step == 2) {
            // Section 5: Hydration & Nutrition Targets
            LiquidSectionHeader(title = "اهداف تغذیه و آب")
            SolidGroupedCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidInputField(
                            value = waterGoalText,
                            onValueChange = { waterGoalText = it },
                            label = "هدف آب (میلی‌لیتر)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        LiquidInputField(
                            value = mealsText,
                            onValueChange = { mealsText = it },
                            label = "تعداد وعده‌ها",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            }

            Spacer(modifier = Modifier.height(12.dp))

            if (step < lastStep) {
                LiquidPrimaryButton(
                    text = "بعدی",
                    onClick = { step += 1 },
                    modifier = Modifier.testTag("onboarding_next_button")
                )
                if (step > 0) {
                    LiquidSecondaryButton(
                        text = "قبلی",
                        onClick = { step -= 1 },
                        modifier = Modifier.testTag("onboarding_back_button")
                    )
                }
            } else {
            LiquidPrimaryButton(
                text = "شروع و ذخیره اطلاعات",
                onClick = {
                    val age = PersianNumberHelper.toEnglishDigits(ageText).toIntOrNull()
                    val height = PersianNumberHelper.toEnglishDigits(heightText).toDoubleOrNull()
                    val weight = PersianNumberHelper.toEnglishDigits(weightText).toDoubleOrNull()
                    val water = PersianNumberHelper.toEnglishDigits(waterGoalText).toIntOrNull() ?: 2500
                    val meals = (PersianNumberHelper.toEnglishDigits(mealsText).toIntOrNull() ?: 4).coerceIn(1, 10)
                    onSaveAndContinue(
                        name,
                        photoUri,
                        gender,
                        age,
                        height,
                        weight,
                        activityLevel,
                        fitnessGoal,
                        meals,
                        water,
                        weightUnit
                    )
                },
                modifier = Modifier.testTag("onboarding_save_button")
            )

            LiquidSecondaryButton(
                text = "بازیابی از فایل پشتیبان",
                icon = Icons.Filled.Restore,
                onClick = onRestoreBackupClick,
                modifier = Modifier.testTag("onboarding_restore_button")
            )

            LiquidSecondaryButton(
                text = "قبلی",
                onClick = { step -= 1 },
                modifier = Modifier.testTag("onboarding_back_button")
            )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
