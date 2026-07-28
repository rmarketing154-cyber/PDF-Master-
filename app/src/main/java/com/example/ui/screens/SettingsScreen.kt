package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Vibrator
import android.os.VibrationEffect
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AdBanner
import com.example.ui.components.SoundHelper
import com.example.ui.viewmodel.PdfViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PdfViewModel,
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as Activity
    val prefs: SharedPreferences = remember {
        context.getSharedPreferences("pdf_master_settings", Context.MODE_PRIVATE)
    }

    // Shared States
    var selectedLanguage by remember { 
        mutableStateOf(prefs.getString("selected_lang", "English") ?: "English") 
    }
    var autoOpenEnabled by remember { 
        mutableStateOf(prefs.getBoolean("auto_open", true)) 
    }
    var reminderEnabled by remember { 
        mutableStateOf(prefs.getBoolean("reminder_enabled", true)) 
    }
    var vibrationEnabled by remember { 
        mutableStateOf(prefs.getBoolean("vibration_enabled", true)) 
    }
    var soundEnabled by remember { 
        mutableStateOf(prefs.getBoolean("sound_enabled", true)) 
    }

    // Sync sound setting with helper on load/change
    LaunchedEffect(soundEnabled) {
        SoundHelper.isSoundEnabled = soundEnabled
    }

    // Vibration helper
    val triggerVibration = {
        if (vibrationEnabled) {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= 26) {
                        vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(40)
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Dialog control states
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    
    var feedbackText by remember { mutableStateOf("") }
    var ratingStars by remember { mutableStateOf(0) }

    // Translations based on language selection
    val isBangla = selectedLanguage == "Bangla"
    val tSettings = if (isBangla) "সেটিংস" else "Settings"
    val tSub = if (isBangla) "আপনার পিডিএফ মাস্টার অ্যাপের কনফিগারেশন পরিবর্তন করুন" else "Configure your PDF MASTER suite preferences"
    val tGeneral = if (isBangla) "সাধারণ সেটিংস" else "General & Interface"
    val tLang = if (isBangla) "ভাষা" else "Language"
    val tStorage = if (isBangla) "স্টোরেজ ও মেমোরি" else "Storage & Cache"
    val tPermissions = if (isBangla) "অ্যাপ পারমিশন চেক" else "System Permissions"
    val tNotification = if (isBangla) "রিমাইন্ডার ও নোটিফিকেশন" else "Reminders & Alerts"
    val tDownloads = if (isBangla) "ডাউনলোড কনফিগারেশন" else "Download Configuration"
    val tShare = if (isBangla) "শেয়ার এবং ফিডব্যাক" else "Share & Rating"
    val tSecurity = if (isBangla) "নিরাপত্তা ও পলিসি" else "Security & Privacy"
    val tSupport = if (isBangla) "হেল্প ও সাপোর্ট" else "Help & Support"

    Scaffold(
        bottomBar = {
            AdBanner(modifier = Modifier.background(MaterialTheme.colorScheme.surface))
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = tSettings,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = tSub,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            // Group 1: General & Appearance
            item { SettingsGroupHeader(title = tGeneral) }
            item {
                SettingsCard {
                    SettingsToggleRow(
                        icon = Icons.Default.DarkMode,
                        title = if (isBangla) "ডার্ক থিম" else "Dark Theme Mode",
                        description = if (isBangla) "চোখের সুরক্ষার জন্য ডার্ক মোড অন করুন" else "Enable eye-soothing dark mode",
                        checked = isDarkMode,
                        onCheckedChange = {
                            triggerVibration()
                            onThemeChange(it)
                            SoundHelper.playClick(context)
                        }
                    )
                    Divider()
                    SettingsClickableRow(
                        icon = Icons.Default.Language,
                        title = if (isBangla) "ভাষা পরিবর্তন" else "App Language",
                        description = if (isBangla) "বর্তমান: বাংলা" else "Current: English",
                        onClick = {
                            triggerVibration()
                            SoundHelper.playClick(context)
                            showLanguageDialog = true
                        }
                    )
                }
            }

            // Group 2: Storage & Memory
            item { SettingsGroupHeader(title = tStorage) }
            item {
                SettingsCard {
                    SettingsClickableRow(
                        icon = Icons.Default.DeleteSweep,
                        title = if (isBangla) "টেম্পোরারি ফাইল ক্যাশ মুছুন" else "Clear Temp Cache",
                        description = if (isBangla) "মেমোরি ফ্রী করতে টেম্প ফাইল ডিলিট করুন" else "Delete generated intermediate splits & preview files",
                        onClick = {
                            triggerVibration()
                            viewModel.clearCache(context)
                            Toast.makeText(context, if (isBangla) "ক্যাশ পরিষ্কার করা হয়েছে!" else "Temp files cleared successfully!", Toast.LENGTH_SHORT).show()
                            SoundHelper.playSuccess(context)
                        }
                    )
                    Divider()
                    SettingsClickableRow(
                        icon = Icons.Default.History,
                        title = if (isBangla) "রিসেন্ট হিস্ট্রি মুছুন" else "Clear Activity History",
                        description = if (isBangla) "হোম স্ক্রিনের পূর্বের ফাইল তালিকা রিসেট করুন" else "Reset recent operations from the Home list",
                        onClick = {
                            triggerVibration()
                            Toast.makeText(context, if (isBangla) "হিস্ট্রি রিসেট সম্পন্ন হয়েছে!" else "Recent activity logs reset!", Toast.LENGTH_SHORT).show()
                            SoundHelper.playSuccess(context)
                        }
                    )
                }
            }

            // Group 3: System Permissions
            item { SettingsGroupHeader(title = tPermissions) }
            item {
                SettingsCard {
                    SettingsClickableRow(
                        icon = Icons.Default.Folder,
                        title = if (isBangla) "ফাইল পারমিশন" else "Files & Storage",
                        description = if (isBangla) "পিডিএফ কনভার্ট এবং রিড করার অনুমতি" else "Access and convert PDF files on device storage",
                        onClick = {
                            triggerVibration()
                            SoundHelper.playClick(context)
                            Toast.makeText(context, "Storage settings opened", Toast.LENGTH_SHORT).show()
                        }
                    )
                    Divider()
                    SettingsClickableRow(
                        icon = Icons.Default.CameraAlt,
                        title = if (isBangla) "ক্যামেরা পারমিশন" else "Camera Permissions",
                        description = if (isBangla) "কাগজপত্র স্ক্যান করে সরাসরি পিডিএফ বানাতে" else "Required for PDF scanning & document imaging",
                        onClick = {
                            triggerVibration()
                            SoundHelper.playClick(context)
                            Toast.makeText(context, "Camera permissions active", Toast.LENGTH_SHORT).show()
                        }
                    )
                    Divider()
                    SettingsClickableRow(
                        icon = Icons.Default.NotificationsActive,
                        title = if (isBangla) "নোটিফিকেশন পারমিশন" else "Notifications Permissions",
                        description = if (isBangla) "গুরুত্বপূর্ণ আপডেট এবং নোটিফিকেশন পেতে" else "Used to alert you on completed background operations",
                        onClick = {
                            triggerVibration()
                            SoundHelper.playClick(context)
                            Toast.makeText(context, "Notification permissions active", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Group 4: Reminders & Alerts
            item { SettingsGroupHeader(title = tNotification) }
            item {
                SettingsCard {
                    SettingsToggleRow(
                        icon = Icons.Default.Notifications,
                        title = if (isBangla) "৩ ঘণ্টার নোটিফিকেশন রিমাইন্ডার" else "Enable 3-Hour Reminders",
                        description = if (isBangla) "আপনাকে পিডিএফ পরিচালনার কথা মনে করিয়ে দেবে" else "Receive non-intrusive tips to manage files every 3 hrs",
                        checked = reminderEnabled,
                        onCheckedChange = {
                            triggerVibration()
                            reminderEnabled = it
                            prefs.edit().putBoolean("reminder_enabled", it).apply()
                            SoundHelper.playClick(context)
                        }
                    )
                    Divider()
                    SettingsToggleRow(
                        icon = Icons.Default.VolumeUp,
                        title = if (isBangla) "সাউন্ড সিস্টেম অন" else "Sound Effects",
                        description = if (isBangla) "সফল বা ব্যর্থ অপারেশনে অডিও প্লে হবে" else "Play tactile chirps and sound cues during operations",
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            prefs.edit().putBoolean("sound_enabled", it).apply()
                            SoundHelper.isSoundEnabled = it
                            triggerVibration()
                            SoundHelper.playClick(context)
                        }
                    )
                    Divider()
                    SettingsToggleRow(
                        icon = Icons.Default.Vibration,
                        title = if (isBangla) "ভাইব্রেশন ফিডব্যাক" else "Vibration / Haptics",
                        description = if (isBangla) "বাটনে ক্লিক বা সাউন্ডে মৃদু ভাইব্রেশন" else "Trigger subtle haptic responses for button actions",
                        checked = vibrationEnabled,
                        onCheckedChange = {
                            vibrationEnabled = it
                            prefs.edit().putBoolean("vibration_enabled", it).apply()
                            triggerVibration()
                            SoundHelper.playClick(context)
                        }
                    )
                }
            }

            // Group 5: Downloads Configuration
            item { SettingsGroupHeader(title = tDownloads) }
            item {
                SettingsCard {
                    SettingsClickableRow(
                        icon = Icons.Default.FolderSpecial,
                        title = if (isBangla) "ডিফল্ট সেভ ফোল্ডার" else "Default Save Path",
                        description = "Internal Storage → PDF MASTER",
                        onClick = {
                            triggerVibration()
                            SoundHelper.playClick(context)
                            Toast.makeText(context, "Saved automatically inside: Internal Storage/PDF MASTER", Toast.LENGTH_LONG).show()
                        }
                    )
                    Divider()
                    SettingsToggleRow(
                        icon = Icons.Default.OpenInNew,
                        title = if (isBangla) "কনভার্ট শেষে অটোমেটিক ওপেন" else "Auto-Open Converted PDF",
                        description = if (isBangla) "কাজ শেষ হওয়ার পর স্বয়ংক্রিয়ভাবে পিডিএফ দেখাবে" else "Launch document reader immediately after processing",
                        checked = autoOpenEnabled,
                        onCheckedChange = {
                            triggerVibration()
                            autoOpenEnabled = it
                            prefs.edit().putBoolean("auto_open", it).apply()
                            SoundHelper.playClick(context)
                        }
                    )
                }
            }

            // Group 6: Share & Rating
            item { SettingsGroupHeader(title = tShare) }
            item {
                SettingsCard {
                    SettingsClickableRow(
                        icon = Icons.Default.Share,
                        title = if (isBangla) "পিডিএফ মাস্টার শেয়ার করুন" else "Share PDF MASTER App",
                        description = if (isBangla) "বন্ধু ও সহকর্মীদের সাথে শেয়ার করুন" else "Invite colleagues & friends to manage PDFs",
                        onClick = {
                            triggerVibration()
                            SoundHelper.playClick(context)
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "PDF MASTER Utility")
                                    putExtra(Intent.EXTRA_TEXT, "Hey! Use PDF MASTER, the ultimate offline-first professional PDF companion on Android. Fast, clean, and highly effective: https://play.google.com/store/apps/details?id=com.pdfmaster.tools")
                                }
                                context.startActivity(Intent.createChooser(intent, "Share via"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error sharing", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    Divider()
                    SettingsClickableRow(
                        icon = Icons.Default.Star,
                        title = if (isBangla) "রেটিং দিয়ে আমাদের সাহায্য করুন" else "Rate PDF MASTER",
                        description = if (isBangla) "আপনার ৫ স্টার আমাদের টিমকে উৎসাহিত করবে" else "Support secure offline toolkit developments",
                        onClick = {
                            triggerVibration()
                            SoundHelper.playClick(context)
                            showRatingDialog = true
                        }
                    )
                }
            }

            // Group 7: Security & Policies
            item { SettingsGroupHeader(title = tSecurity) }
            item {
                SettingsCard {
                    SettingsClickableRow(
                        icon = Icons.Default.Gavel,
                        title = if (isBangla) "ব্যবহারের শর্তাবলী" else "Terms of Service",
                        description = "Licensing and user guidelines",
                        onClick = {
                            triggerVibration()
                            SoundHelper.playClick(context)
                            showTermsDialog = true
                        }
                    )
                    Divider()
                    SettingsClickableRow(
                        icon = Icons.Default.Security,
                        title = if (isBangla) "গোপনীয়তা নীতি" else "Privacy Policy",
                        description = if (isBangla) "আপনার ফাইল সম্পূর্ণ সুরক্ষিত এবং অফলাইন" else "Your documents are kept 100% secure and offline",
                        onClick = {
                            triggerVibration()
                            SoundHelper.playClick(context)
                            showPrivacyDialog = true
                        }
                    )
                    Divider()
                    SettingsClickableRow(
                        icon = Icons.Default.Info,
                        title = if (isBangla) "পিডিএফ মাস্টার সম্পর্কে" else "About PDF MASTER",
                        description = "Developer & build architecture info (v1.5.0)",
                        onClick = {
                            triggerVibration()
                            SoundHelper.playClick(context)
                            showAboutDialog = true
                        }
                    )
                }
            }

            // Group 8: Support & FAQ
            item { SettingsGroupHeader(title = tSupport) }
            item {
                SettingsCard {
                    SettingsClickableRow(
                        icon = Icons.Default.QuestionAnswer,
                        title = if (isBangla) "সচরাচর জিজ্ঞাসিত প্রশ্নাবলী (FAQ)" else "FAQ & Guides",
                        description = "Quick answers to common operations",
                        onClick = {
                            triggerVibration()
                            SoundHelper.playClick(context)
                            showFaqDialog = true
                        }
                    )
                    Divider()
                    SettingsClickableRow(
                        icon = Icons.Default.Feedback,
                        title = if (isBangla) "ফিডব্যাক পাঠান" else "Send Feedback",
                        description = "Help us improve with your suggestions",
                        onClick = {
                            triggerVibration()
                            SoundHelper.playClick(context)
                            showFeedbackDialog = true
                        }
                    )
                    Divider()
                    SettingsClickableRow(
                        icon = Icons.Default.Email,
                        title = if (isBangla) "যোগাযোগ করুন" else "Contact Support",
                        description = "Get 24/7 dedicated support for file recoveries",
                        onClick = {
                            triggerVibration()
                            SoundHelper.playClick(context)
                            showContactDialog = true
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Dialogs Setup
        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                shape = RoundedCornerShape(24.dp),
                title = { Text(if (isBangla) "ভাষা নির্বাচন করুন" else "Select Language", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("English", "Bangla").forEach { lang ->
                            val isSelected = selectedLanguage == lang
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        triggerVibration()
                                        selectedLanguage = lang
                                        prefs.edit().putString("selected_lang", lang).apply()
                                        Toast.makeText(context, if (lang == "Bangla") "ভাষা বাংলায় পরিবর্তন করা হয়েছে!" else "Language changed to English!", Toast.LENGTH_SHORT).show()
                                        SoundHelper.playSuccess(context)
                                        showLanguageDialog = false
                                    },
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (lang == "Bangla") "বাংলা (Bangla)" else "English (US)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                shape = RoundedCornerShape(24.dp),
                title = { Text("About PDF MASTER") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PDF MASTER v1.5.0",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "A world-class premium PDF utility built natively with Jetpack Compose. Offers high-performance merges, splits, encryption, watermarking, scanners, and local storage organization.\n\nAll tools work 100% offline. We respect your privacy and process all items on-device.\n\nPowered by Start.io premium ad services.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { showAboutDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                shape = RoundedCornerShape(24.dp),
                title = { Text("Privacy Policy") },
                text = {
                    Text(
                        text = "We take privacy very seriously.\n\n1. Offline Processing: No documents or image conversions are uploaded to any internet server. Everything resides in local sandbox files.\n2. Start.io Ads: Contextual banner/interstitial advertisements are requested anonymously using the Start.io SDK complying with safe-policy rules.\n3. Analytics: Zero diagnostic logs are recorded.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(onClick = { showPrivacyDialog = false }) {
                        Text("I Agree")
                    }
                }
            )
        }

        if (showTermsDialog) {
            AlertDialog(
                onDismissRequest = { showTermsDialog = false },
                shape = RoundedCornerShape(24.dp),
                title = { Text("Terms of Service") },
                text = {
                    Text(
                        text = "By using PDF MASTER, you agree that:\n\n1. All document files generated remain your proprietary items.\n2. The app is provided 'as is' without warranty of file recovery in case of system factory resets.\n3. Advertisements are displayed to support continued premium tool upgrades.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(onClick = { showTermsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showRatingDialog) {
            AlertDialog(
                onDismissRequest = { showRatingDialog = false },
                shape = RoundedCornerShape(24.dp),
                title = { Text(if (isBangla) "রেটিং দিন" else "Rate PDF MASTER", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isBangla) "পিডিএফ মাস্টার অ্যাপটি আপনার কেমন লাগছে? অনুগ্রহ করে রেটিং দিয়ে আমাদের সাহায্য করুন!" else "Enjoying PDF MASTER? Let us know with a quick 5-star rating!", 
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { star ->
                                IconButton(onClick = { ratingStars = star }) {
                                    Icon(
                                        imageVector = if (star <= ratingStars) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Star $star",
                                        tint = if (star <= ratingStars) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (ratingStars > 0) {
                                triggerVibration()
                                Toast.makeText(context, if (isBangla) "আপনার মূল্যবান মতামতের জন্য ধন্যবাদ!" else "Thank you for rating us $ratingStars stars!", Toast.LENGTH_LONG).show()
                                SoundHelper.playSuccess(context)
                            }
                            showRatingDialog = false
                            ratingStars = 0
                        }
                    ) {
                        Text(if (isBangla) "সাবমিট করুন" else "Submit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRatingDialog = false
                        ratingStars = 0
                    }) {
                        Text(if (isBangla) "বাতিল" else "Cancel")
                    }
                }
            )
        }

        if (showFeedbackDialog) {
            AlertDialog(
                onDismissRequest = { showFeedbackDialog = false },
                shape = RoundedCornerShape(24.dp),
                title = { Text("Send Feedback") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tell us how to improve our toolkits or request a feature:", fontSize = 13.sp)
                        OutlinedTextField(
                            value = feedbackText,
                            onValueChange = { feedbackText = it },
                            placeholder = { Text("Write your message here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (feedbackText.isNotBlank()) {
                                triggerVibration()
                                Toast.makeText(context, "Feedback sent! Thank you.", Toast.LENGTH_SHORT).show()
                                SoundHelper.playSuccess(context)
                                feedbackText = ""
                            }
                            showFeedbackDialog = false
                        }
                    ) {
                        Text("Send")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFeedbackDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showFaqDialog) {
            AlertDialog(
                onDismissRequest = { showFaqDialog = false },
                shape = RoundedCornerShape(24.dp),
                title = { Text("FAQ & Help") },
                text = {
                    LazyColumn(
                        modifier = Modifier.height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text("Q: Are files sent online during OCR summary?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("A: No, the OCR processes items safely inside local systems.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        item {
                            Text("Q: Where are files saved on download?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("A: In 'Downloads' folder under displays of 'PDF MASTER'.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        item {
                            Text("Q: How do I unlock premium features?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("A: Tap any premium tool (marked with crown) and watch a sponsored 5-sec video to get instant unlocks.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showFaqDialog = false }) {
                        Text("Dismiss")
                    }
                }
            )
        }

        if (showContactDialog) {
            AlertDialog(
                onDismissRequest = { showContactDialog = false },
                shape = RoundedCornerShape(24.dp),
                title = { Text("Contact Support") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Need direct assistance with converted files?", fontSize = 13.sp)
                        Text("Support Email:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("support@pdfmaster.tools", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("We reply within 12 hours.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            triggerVibration()
                            SoundHelper.playClick(context)
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:support@pdfmaster.tools")
                                    putExtra(Intent.EXTRA_SUBJECT, "PDF MASTER Support Request")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                            }
                            showContactDialog = false
                        }
                    ) {
                        Text("Email Support")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showContactDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFC4C6D0).copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

@Composable
fun Divider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "Go", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}
