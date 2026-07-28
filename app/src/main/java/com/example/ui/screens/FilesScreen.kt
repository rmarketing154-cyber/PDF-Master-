package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SavedPdfFile
import com.example.ui.components.AdBanner
import com.example.ui.components.FriendlyEmptyState
import com.example.ui.components.printPdfFile
import com.example.ui.components.sharePdfFile
import com.example.ui.viewmodel.PdfViewModel
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    viewModel: PdfViewModel,
    onOpenFile: (SavedPdfFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allFiles by viewModel.allPdfs.collectAsStateWithLifecycle()
    val recentFiles by viewModel.recentPdfs.collectAsStateWithLifecycle()
    val favoriteFiles by viewModel.favoritePdfs.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableStateOf(0) } // 0 = All, 1 = Recent, 2 = Favorites
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredFiles = remember(allFiles, recentFiles, favoriteFiles, selectedTab, searchQuery) {
        val activeList = when (selectedTab) {
            0 -> allFiles
            1 -> recentFiles
            2 -> favoriteFiles
            else -> allFiles
        }
        if (searchQuery.trim().isEmpty()) {
            activeList
        } else {
            activeList.filter { it.fileName.contains(searchQuery, ignoreCase = true) }
        }
    }

    var selectedFileForMenu by remember { mutableStateOf<SavedPdfFile?>(null) }
    var fileToRename by remember { mutableStateOf<SavedPdfFile?>(null) }
    var renameInputName by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            AdBanner(modifier = Modifier.background(MaterialTheme.colorScheme.surface))
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My PDF Vault",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Manage and view all your locally stored PDF files",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search PDF files...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0061A4),
                    unfocusedBorderColor = Color(0xFFDDE2EA),
                    focusedContainerColor = Color(0xFFEDF0F7),
                    unfocusedContainerColor = Color(0xFFEDF0F7),
                    focusedTextColor = Color(0xFF191C1E),
                    unfocusedTextColor = Color(0xFF191C1E),
                    focusedLeadingIconColor = Color(0xFF43474E),
                    unfocusedLeadingIconColor = Color(0xFF43474E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("file_search_input")
            )

            // Modern Material 3 TabRow
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All Files", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Recent", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Favorites", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Files list
            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (searchQuery.isNotEmpty()) {
                        FriendlyEmptyState(
                            icon = Icons.Default.SearchOff,
                            title = "No Match Found",
                            description = "We couldn't find any PDF matching \"$searchQuery\". Try checking the spelling or searching other keywords."
                        )
                    } else {
                        when (selectedTab) {
                            0 -> {
                                FriendlyEmptyState(
                                    icon = Icons.Default.FolderOpen,
                                    title = "Your Vault is Empty",
                                    description = "Import PDF files from your device storage or use our quick action tools to start building your offline library."
                                )
                            }
                            1 -> {
                                FriendlyEmptyState(
                                    icon = Icons.Default.History,
                                    title = "No Recent Activity",
                                    description = "Files you open, merge, compress, or edit will automatically appear in this list for quick, seamless access."
                                )
                            }
                            2 -> {
                                FriendlyEmptyState(
                                    icon = Icons.Default.Favorite,
                                    title = "No Favorites Yet",
                                    description = "Tap the heart icon on any PDF file inside your library to mark it as a favorite for instant one-tap access here."
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredFiles, key = { it.id }) { pdf ->
                        FileRowItem(
                            pdfFile = pdf,
                            onClick = { onOpenFile(pdf) },
                            onActionMenu = { selectedFileForMenu = pdf },
                            onFavoriteToggle = { viewModel.toggleFavorite(pdf) }
                        )
                    }
                }
            }
        }

        // Dropdown actions sheet / dialog when a row action is tapped
        selectedFileForMenu?.let { pdf ->
            AlertDialog(
                onDismissRequest = { selectedFileForMenu = null },
                title = { Text(pdf.fileName, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { selectedFileForMenu = null }) {
                        Text("Cancel")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ActionDialogRow(
                            icon = Icons.Default.Visibility,
                            text = "Open Reader",
                            onClick = {
                                selectedFileForMenu = null
                                onOpenFile(pdf)
                            }
                        )
                        ActionDialogRow(
                            icon = Icons.Default.Share,
                            text = "Share File",
                            onClick = {
                                selectedFileForMenu = null
                                sharePdfFile(context, pdf)
                            }
                        )
                        ActionDialogRow(
                            icon = Icons.Default.Print,
                            text = "Print PDF",
                            onClick = {
                                selectedFileForMenu = null
                                printPdfFile(context, pdf)
                            }
                        )
                        ActionDialogRow(
                            icon = Icons.Default.Edit,
                            text = "Rename PDF",
                            onClick = {
                                selectedFileForMenu = null
                                fileToRename = pdf
                                renameInputName = pdf.fileName.removeSuffix(".pdf")
                                showRenameDialog = true
                            }
                        )
                        ActionDialogRow(
                            icon = Icons.Default.Delete,
                            text = "Delete File",
                            textColor = MaterialTheme.colorScheme.error,
                            iconColor = MaterialTheme.colorScheme.error,
                            onClick = {
                                selectedFileForMenu = null
                                viewModel.deletePdf(pdf)
                                Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            )
        }

        // Rename Dialog
        if (showRenameDialog && fileToRename != null) {
            AlertDialog(
                onDismissRequest = {
                    showRenameDialog = false
                    fileToRename = null
                },
                title = { Text("Rename PDF File") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = renameInputName,
                            onValueChange = { renameInputName = it },
                            label = { Text("New file name") },
                            suffix = { Text(".pdf") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newName = renameInputName.trim()
                            if (newName.isNotEmpty()) {
                                viewModel.renamePdf(context, fileToRename!!, newName) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Renamed successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Rename failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            showRenameDialog = false
                            fileToRename = null
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showRenameDialog = false
                            fileToRename = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun FileRowItem(
    pdfFile: SavedPdfFile,
    onClick: () -> Unit,
    onActionMenu: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, Color(0xFFE0E2EC).copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (pdfFile.hasPassword) Color(0xFFFFDAD6) else Color(0xFFF9DEDC),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = if (pdfFile.hasPassword) Icons.Default.Lock else Icons.Default.PictureAsPdf,
                        contentDescription = "PDF Icon",
                        tint = if (pdfFile.hasPassword) Color(0xFFBA1A1A) else Color(0xFFB3261E),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pdfFile.fileName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${pdfFile.pageCount} pgs",
                            fontSize = 11.sp,
                            color = Color(0xFF43474E)
                        )
                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            color = Color(0xFF43474E).copy(alpha = 0.4f)
                        )
                        Text(
                            text = formatBytes(pdfFile.fileSize),
                            fontSize = 11.sp,
                            color = Color(0xFF43474E)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (pdfFile.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (pdfFile.isFavorite) Color(0xFFBA1A1A) else Color(0xFF43474E),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onActionMenu) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Actions Menu",
                        tint = Color(0xFF43474E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActionDialogRow(
    icon: ImageVector,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}
