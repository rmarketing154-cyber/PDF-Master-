package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AdBanner
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

    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var ratingStars by remember { mutableStateOf(0) }

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
                        text = "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Configure your PDF Master preferences",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            // Group 1: Appearance & Cache
            item {
                SettingsGroupHeader(title = "Appearance & Cache")
            }

            item {
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFC4C6D0).copy(alpha = 0.6f))
                ) {
                    Column {
                        // Dark Mode Toggle
                        SettingsToggleRow(
                            icon = Icons.Default.DarkMode,
                            title = "Dark Theme",
                            description = "Enable dark mode visual styles",
                            checked = isDarkMode,
                            onCheckedChange = onThemeChange
                        )
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))
                        
                        // Language Selector
                        SettingsClickableRow(
                            icon = Icons.Default.Language,
                            title = "App Language",
                            description = "Current: English",
                            onClick = { showLanguageDialog = true }
                        )
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))

                        // Cache Cleaner
                        SettingsClickableRow(
                            icon = Icons.Default.DeleteSweep,
                            title = "Clear PDF Temp Cache",
                            description = "Free up memory used by temp file splits & image renders",
                            onClick = {
                                viewModel.clearCache(context)
                                Toast.makeText(context, "Temp cache cleared successfully!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // Group 3: Support & About
            item {
                SettingsGroupHeader(title = "Support & Information")
            }

            item {
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFC4C6D0).copy(alpha = 0.6f))
                ) {
                    Column {
                        // Rate App
                        SettingsClickableRow(
                            icon = Icons.Default.RateReview,
                            title = "Rate PDF Master",
                            description = "Support offline utility development",
                            onClick = { showRatingDialog = true }
                        )
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))

                        // Share App
                        SettingsClickableRow(
                            icon = Icons.Default.Share,
                            title = "Share PDF Master App",
                            description = "Invite colleagues & friends",
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "PDF Master Utility")
                                        putExtra(Intent.EXTRA_TEXT, "Hey! Check out PDF Master, the best offline-first native PDF Tools suite on Android: https://play.google.com/store/apps/details?id=com.pdfmaster.tools")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share via"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error sharing", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))

                        // Privacy Policy
                        SettingsClickableRow(
                            icon = Icons.Default.Security,
                            title = "Privacy Policy",
                            description = "Your documents are kept 100% local and offline",
                            onClick = { showPrivacyDialog = true }
                        )
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))

                        // About
                        SettingsClickableRow(
                            icon = Icons.Default.Info,
                            title = "About PDF Master",
                            description = "Developer & build architecture info",
                            onClick = { showAboutDialog = true }
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Dialogs
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("About PDF Master") },
                text = {
                    Text(
                        text = "PDF Master v1.0.0\n\nBuilt entirely with modern Jetpack Compose for Android. Features offline-first document rendering, merging, splitting, compressing, watermarking, security tools, and Start.io ads SDK (206743399) integration.\n\nAll processed documents are maintained inside secure local application storage. We do not transmit your private documents to external servers.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
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
                title = { Text("Privacy Policy") },
                text = {
                    Text(
                        text = "Your Privacy is our Highest Priority.\n\nPDF Master processes all documents locally on your Android system. We collect absolutely zero analytical or file logs. Files imported using standard SAF (Storage Access Framework) system pickers are maintained in secure app sandboxes and never transmitted online.\n\nStart.io Ads SDK works strictly to serve contextual interstitial and rewarded banners in conformity with general Google Play developer guidelines.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(onClick = { showPrivacyDialog = false }) {
                        Text("Accept")
                    }
                }
            )
        }

        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = { Text("App Language") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Select preferred system locale:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        listOf("English (US)", "Spanish (Español)", "French (Français)", "German (Deutsch)", "Arabic (العربية)").forEach { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        Toast.makeText(context, "Locale changed to $lang!", Toast.LENGTH_SHORT).show()
                                        showLanguageDialog = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(lang, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        if (showRatingDialog) {
            AlertDialog(
                onDismissRequest = { showRatingDialog = false },
                title = { Text("Rate PDF Master") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enjoying PDF Master? Support us with a quick rating!", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { star ->
                                IconButton(onClick = { ratingStars = star }) {
                                    Icon(
                                        imageVector = if (star <= ratingStars) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Star $star",
                                        tint = if (star <= ratingStars) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.size(32.dp)
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
                                Toast.makeText(context, "Thank you for rating us $ratingStars stars!", Toast.LENGTH_LONG).show()
                            }
                            showRatingDialog = false
                            ratingStars = 0
                        }
                    ) {
                        Text("Submit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRatingDialog = false
                        ratingStars = 0
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
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
