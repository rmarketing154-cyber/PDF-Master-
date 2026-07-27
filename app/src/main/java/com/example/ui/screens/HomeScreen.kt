package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
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
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PdfViewModel,
    onOpenFile: (SavedPdfFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as Activity

    val recentPdfs by viewModel.recentPdfs.collectAsStateWithLifecycle()
    val totalPdfsCount by viewModel.totalPdfsCount.collectAsStateWithLifecycle()
    val successfulOps by viewModel.successfulOperationsCount.collectAsStateWithLifecycle()
    val totalStorageUsed by viewModel.totalStorageUsed.collectAsStateWithLifecycle()
    val isPremiumUnlocked by viewModel.isPremiumUnlocked.collectAsStateWithLifecycle()

    var showMergeDialog by remember { mutableStateOf(false) }
    var showCompressDialog by remember { mutableStateOf(false) }
    var showWatermarkDialog by remember { mutableStateOf(false) }

    // System File Pickers
    val singlePdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importPdfFile(context, uri) { success ->
                if (success) {
                    Toast.makeText(context, "PDF Imported Successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to Import PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val multiplePdfPickerForMerge = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            if (uris.size < 2) {
                Toast.makeText(context, "Please select at least 2 PDFs to merge", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            // Trigger merge operation directly in background
            val tempFiles = mutableListOf<File>()
            uris.forEachIndexed { i, uri ->
                val f = com.example.pdf.PdfToolsEngine.copyFileFromUri(context, uri, "temp_merge_$i.pdf")
                if (f != null) tempFiles.add(f)
            }
            if (tempFiles.size >= 2) {
                viewModel.performMerge(context, tempFiles.map { SavedPdfFile(fileName = it.name, filePath = it.absolutePath, fileSize = it.length(), pageCount = 0) }, "Merged_Document_${System.currentTimeMillis() / 1000}.pdf", activity) { success ->
                    if (success) Toast.makeText(context, "PDFs Merged Successfully!", Toast.LENGTH_SHORT).show()
                    // Delete temporary files
                    tempFiles.forEach { it.delete() }
                }
            }
        }
    }

    val imagePickerForPdf = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            viewModel.performImageToPdf(context, uris, "Image_to_PDF_${System.currentTimeMillis() / 1000}.pdf", activity) { success ->
                if (success) {
                    Toast.makeText(context, "Images Converted to PDF Successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to convert images", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val singlePdfPickerForCompress = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val file = com.example.pdf.PdfToolsEngine.copyFileFromUri(context, uri, "temp_compress.pdf")
            if (file != null) {
                // If high quality is chosen, but premium is locked, prompt rewarded ad
                val action = {
                    viewModel.performCompress(
                        context,
                        SavedPdfFile(fileName = file.name, filePath = file.absolutePath, fileSize = file.length(), pageCount = 0),
                        "Compressed_${System.currentTimeMillis() / 1000}.pdf",
                        70, // 70% quality compression
                        0.75f, // 0.75x scaling factor
                        activity
                    ) { success ->
                        file.delete()
                        if (success) Toast.makeText(context, "PDF Compressed Successfully!", Toast.LENGTH_SHORT).show()
                    }
                }

                // Unlocks High Quality with rewarded ad
                if (!isPremiumUnlocked) {
                    viewModel.unlockPremiumViaAd(activity, onSuccess = action, onFailure = {
                        // Fallback to lower compression quality
                        viewModel.performCompress(
                            context,
                            SavedPdfFile(fileName = file.name, filePath = file.absolutePath, fileSize = file.length(), pageCount = 0),
                            "Compressed_Low_${System.currentTimeMillis() / 1000}.pdf",
                            45, // 45% quality
                            0.5f, // 0.5x scaling
                            activity
                        ) { success ->
                            file.delete()
                            if (success) Toast.makeText(context, "PDF Compressed successfully using Standard Quality!", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    action()
                }
            }
        }
    }

    val singlePdfPickerForWatermark = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val file = com.example.pdf.PdfToolsEngine.copyFileFromUri(context, uri, "temp_watermark.pdf")
            if (file != null) {
                viewModel.performWatermark(
                    context,
                    SavedPdfFile(fileName = file.name, filePath = file.absolutePath, fileSize = file.length(), pageCount = 0),
                    "CONFIDENTIAL",
                    0xFFFF0000.toInt(),
                    48f,
                    80, // alpha
                    -45f,
                    "Watermarked_${System.currentTimeMillis() / 1000}.pdf",
                    activity
                ) { success ->
                    file.delete()
                    if (success) Toast.makeText(context, "Watermark Added Successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { singlePdfPicker.launch("application/pdf") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("import_pdf_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import PDF")
            }
        },
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
            // Welcome & Header Card
            item {
                HeaderSection(isPremiumUnlocked)
            }

            // Stats Cards
            item {
                StatsSection(totalPdfsCount, successfulOps, totalStorageUsed)
            }

            // Quick Tools Grid Section
            item {
                Text(
                    text = "Quick Tools",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ToolGridItem(
                        title = "Merge PDF",
                        description = "Combine multiple PDFs",
                        icon = Icons.Default.Merge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { multiplePdfPickerForMerge.launch("application/pdf") }
                    )
                    ToolGridItem(
                        title = "Image to PDF",
                        description = "Convert photos to PDF",
                        icon = Icons.Default.Image,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = { imagePickerForPdf.launch("image/*") }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ToolGridItem(
                        title = "Compress PDF",
                        description = "Reduce document size",
                        icon = Icons.Default.Compress,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        onClick = { singlePdfPickerForCompress.launch("application/pdf") }
                    )
                    ToolGridItem(
                        title = "Watermark",
                        description = "Add secure overlay text",
                        icon = Icons.Default.Edit,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        onClick = { singlePdfPickerForWatermark.launch("application/pdf") }
                    )
                }
            }

            // Recent Files List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Files",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (recentPdfs.isEmpty()) {
                item {
                    EmptyRecentState()
                }
            } else {
                items(recentPdfs, key = { it.id }) { pdf ->
                    RecentPdfItem(
                        pdfFile = pdf,
                        onClick = { onOpenFile(pdf) },
                        onFavorite = { viewModel.toggleFavorite(pdf) },
                        onShare = { sharePdfFile(context, pdf) },
                        onPrint = { printPdfFile(context, pdf) }
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderSection(isPremiumUnlocked: Boolean) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "PDF Master",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "All-In-One Offline PDF Utility Suite",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (isPremiumUnlocked) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Premium", tint = Color.Yellow, modifier = Modifier.size(16.dp))
                        Text(
                            text = "PREMIUM SESSION ACTIVE",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "Unlock high-quality tools by viewing a rewarded ad!",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun StatsSection(totalFiles: Int, operationsCount: Int, storageBytes: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Total PDFs
        Box(
            modifier = Modifier
                .weight(1f)
                .height(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(com.example.ui.theme.GeoAccentBlue)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$totalFiles",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF001C38)
                )
                Text(
                    text = "PDFs Managed",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF001C38).copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }

        // Card 2: Operations Saved
        Box(
            modifier = Modifier
                .weight(1f)
                .height(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(com.example.ui.theme.GeoAccentPurple)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$operationsCount",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF21005D)
                )
                Text(
                    text = "Space Saved",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF21005D).copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }

        // Card 3: Storage Used
        Box(
            modifier = Modifier
                .weight(1f)
                .height(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(com.example.ui.theme.GeoAccentGreen)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatBytes(storageBytes),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D2811)
                )
                Text(
                    text = "Local Storage",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0D2811).copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ToolGridItem(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    OutlinedCard(
        onClick = {
            com.example.ui.components.HapticHelper.triggerClick(context)
            onClick()
        },
        modifier = modifier
            .height(115.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, Color(0xFFC4C6D0).copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .background(color, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun EmptyRecentState() {
    FriendlyEmptyState(
        icon = Icons.Default.FolderOpen,
        title = "No Recent Files Found",
        description = "Your recently processed and viewed PDF files will show up here for quick and convenient offline access."
    )
}

@Composable
fun RecentPdfItem(
    pdfFile: SavedPdfFile,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit
) {
    val context = LocalContext.current
    OutlinedCard(
        onClick = {
            com.example.ui.components.HapticHelper.triggerClick(context)
            onClick()
        },
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
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = {
                    com.example.ui.components.HapticHelper.triggerClick(context)
                    onFavorite()
                }) {
                    Icon(
                        imageVector = if (pdfFile.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (pdfFile.isFavorite) Color(0xFFBA1A1A) else Color(0xFF43474E),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = {
                    com.example.ui.components.HapticHelper.triggerClick(context)
                    onShare()
                }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF43474E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
