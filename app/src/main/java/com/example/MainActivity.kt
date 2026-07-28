package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.content.Context
import android.content.Intent
import com.example.ads.StartIoAdsManager
import com.example.data.PdfDatabase
import com.example.data.PdfRepository
import com.example.data.SavedPdfFile
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Camera
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.components.PdfReaderScreen
import com.example.ui.components.printPdfFile
import com.example.ui.components.sharePdfFile
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PdfViewModel
import com.example.ui.viewmodel.PdfViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room Database, DAO and Repository
        val database = PdfDatabase.getDatabase(applicationContext)
        val repository = PdfRepository(database.pdfDao())
        
        // Initialize Start.io Ads SDK using App ID 206743399
        StartIoAdsManager.initialize(applicationContext)

        // Schedule 3-hour friendly reminder notification
        scheduleThreeHourReminder(applicationContext)

        // Instantiate PDF ViewModel
        val viewModel: PdfViewModel by viewModels { PdfViewModelFactory(repository) }

        enableEdgeToEdge()

        setContent {
            // Dark Mode preferences
            val systemTheme = isSystemInDarkTheme()
            var isDarkMode by rememberSaveable { mutableStateOf(systemTheme) }

            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppMainLayout(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode,
                        onThemeChange = { isDarkMode = it }
                    )
                }
            }
        }
    }

    private fun scheduleThreeHourReminder(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
            if (alarmManager != null) {
                val intent = Intent(context, ReminderReceiver::class.java)
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    100,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0
                )
                val intervalMillis = 3 * 60 * 60 * 1000L // 3 Hours
                val triggerAtMillis = System.currentTimeMillis() + intervalMillis
                alarmManager.setInexactRepeating(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    intervalMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to schedule repeating reminder", e)
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Tools : Screen("tools", "Tools", Icons.Default.Build)
    object Files : Screen("files", "Files", Icons.Default.Folder)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppMainLayout(
    viewModel: PdfViewModel,
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Permissions check on entering
    val permissionsList = remember {
        buildList {
            add(android.Manifest.permission.CAMERA)
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                add(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
    val permissionsState = rememberMultiplePermissionsState(permissionsList)
    var showPermissionDialog by remember { mutableStateOf(!permissionsState.allPermissionsGranted) }

    // Active full screen PDF reading state
    var activePdfForReading by remember { mutableStateOf<SavedPdfFile?>(null) }
    
    // Global processing indicator overlay
    val isProcessing by viewModel.isProcessing.collectAsState()
    val processingMessage by viewModel.processingMessage.collectAsState()
    val processingProgress by viewModel.processingProgress.collectAsState()

    val screens = listOf(
        Screen.Home,
        Screen.Tools,
        Screen.Files,
        Screen.Settings
    )

    if (activePdfForReading != null) {
        // Full screen PDF Viewer Screen
        PdfReaderScreen(
            pdfFile = activePdfForReading!!,
            onBack = { activePdfForReading = null },
            onPrint = { printPdfFile(context, activePdfForReading!!) },
            onShare = { sharePdfFile(context, activePdfForReading!!) }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("app_bottom_nav"),
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ) {
                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, fontSize = 11.sp) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = viewModel,
                            onOpenFile = { pdf ->
                                viewModel.updateLastOpened(pdf)
                                activePdfForReading = pdf
                            }
                        )
                    }
                    composable(Screen.Tools.route) {
                        ToolsScreen(
                            viewModel = viewModel
                        )
                    }
                    composable(Screen.Files.route) {
                        FilesScreen(
                            viewModel = viewModel,
                            onOpenFile = { pdf ->
                                viewModel.updateLastOpened(pdf)
                                activePdfForReading = pdf
                            }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = viewModel,
                            isDarkMode = isDarkMode,
                            onThemeChange = onThemeChange
                        )
                    }
                }

                // Global high-fidelity Premium Processing Overlay Dialog
                if (isProcessing) {
                    com.example.ui.components.PremiumProcessingScreen(
                        message = processingMessage,
                        progress = processingProgress,
                        onCancel = {
                            viewModel.cancelCurrentOperation()
                        }
                    )
                }

                // Global high-fidelity Success Review Overlay Dialog
                val lastProcessedFile by viewModel.lastProcessedFile.collectAsState()
                val processedFile = lastProcessedFile
                if (processedFile != null) {
                    com.example.ui.components.SuccessReviewOverlay(
                        pdfFile = processedFile,
                        onOpen = {
                            viewModel.clearLastProcessedFile()
                            viewModel.updateLastOpened(processedFile)
                            activePdfForReading = processedFile
                        },
                        onShare = {
                            sharePdfFile(context, processedFile)
                        },
                        onPrint = {
                            printPdfFile(context, processedFile)
                        },
                        onDismiss = {
                            viewModel.clearLastProcessedFile()
                        }
                    )
                }

                // Startup Permissions Dialog overlay
                if (showPermissionDialog) {
                    StartupPermissionsDialog(
                        permissionsState = permissionsState,
                        onDismiss = { showPermissionDialog = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StartupPermissionsDialog(
    permissionsState: com.google.accompanist.permissions.MultiplePermissionsState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "Permissions Required",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Permissions Required",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "To unlock and use all features of this app, please grant the following essential permissions:",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Storage explanation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Storage / Files",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "To access, convert, and save PDFs on your device.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Notifications explanation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Notifications",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "To alert you when background tasks (OCR, merges) finish.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Camera explanation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Camera",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "To scan physical pages and convert them directly into PDFs.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    permissionsState.launchMultiplePermissionRequest()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Grant Permissions", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Not Now", color = MaterialTheme.colorScheme.outline)
            }
        }
    )
}
