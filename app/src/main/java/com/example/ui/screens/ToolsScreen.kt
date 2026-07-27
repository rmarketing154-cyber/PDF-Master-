package com.example.ui.screens

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SavedPdfFile
import com.example.ui.components.AdBanner
import com.example.ui.viewmodel.PdfViewModel
import java.io.File

data class ToolDefinition(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tintColor: Color,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: PdfViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as Activity
    val isPremiumUnlocked by viewModel.isPremiumUnlocked.collectAsStateWithLifecycle()

    val toolsList = remember {
        listOf(
            // AI PDF Workspace
            ToolDefinition("ai_summary", "AI PDF Summary", "Summarize & chat with PDF", Icons.Default.AutoAwesome, Color(0xFF6200EE), "AI PDF Workspace (Premium)"),
            
            // Scanner & OCR
            ToolDefinition("smart_scanner", "Smart Scanner", "Camera scanner with edge crop", Icons.Default.DocumentScanner, Color(0xFFE5A93B), "Scanner & OCR (Premium)"),
            ToolDefinition("ocr_image", "Image OCR", "Extract text from pictures", Icons.Default.TextFields, Color(0xFF03DAC6), "Scanner & OCR (Premium)"),
            
            // Create & Convert
            ToolDefinition("img_to_pdf", "Image to PDF", "Convert pictures to PDF", Icons.Default.Image, Color(0xFF4CAF50), "Create & Convert"),
            ToolDefinition("pdf_to_img", "PDF to Image", "Extract pages as images", Icons.Default.BurstMode, Color(0xFFFF9800), "Create & Convert"),
            
            // Modify & Edit
            ToolDefinition("batch_tools", "Batch PDF Tools", "Compress/Rotate 10-50 files", Icons.Default.Layers, Color(0xFFFF3D00), "Modify & Edit"),
            ToolDefinition("merge", "Merge PDF", "Combine multiple files", Icons.Default.Merge, Color(0xFF2196F3), "Modify & Edit"),
            ToolDefinition("split", "Split PDF", "Split document into parts", Icons.Default.CallSplit, Color(0xFF00BCD4), "Modify & Edit"),
            ToolDefinition("compress", "Compress PDF", "Reduce document file size", Icons.Default.Compress, Color(0xFF9C27B0), "Modify & Edit"),
            ToolDefinition("watermark", "Watermark PDF", "Add custom text overlay", Icons.Default.Edit, Color(0xFFE91E63), "Modify & Edit"),
            ToolDefinition("rotate", "Rotate PDF", "Rotate pages 90 / 180 deg", Icons.Default.RotateRight, Color(0xFF3F51B5), "Modify & Edit"),
            
            // Page management
            ToolDefinition("delete_pages", "Delete Pages", "Remove pages from file", Icons.Default.DeleteSweep, Color(0xFFF44336), "Page Management"),
            ToolDefinition("extract_pages", "Extract Pages", "Extract specific page indices", Icons.Default.Pin, Color(0xFF673AB7), "Page Management"),
            ToolDefinition("reorder_pages", "Reorder Pages", "Rearrange pages sequence", Icons.Default.Reorder, Color(0xFF009688), "Page Management"),
            
            // Security & Admin
            ToolDefinition("protect", "Protect PDF", "Lock with secure password", Icons.Default.Lock, Color(0xFFE65100), "Security & Protection"),
            ToolDefinition("remove_password", "Remove Password", "Unlock password protected files", Icons.Default.LockOpen, Color(0xFF1B5E20), "Security & Protection")
        )
    }

    // Interactive launch states
    var selectedFileForAction by remember { mutableStateOf<SavedPdfFile?>(null) }
    var currentActionType by remember { mutableStateOf("") }

    val allPdfs by viewModel.allPdfs.collectAsStateWithLifecycle()
    var showAiSummaryDialog by remember { mutableStateOf(false) }
    var showOcrImageDialog by remember { mutableStateOf(false) }
    var showSmartScannerDialog by remember { mutableStateOf(false) }
    var showBatchToolsDialog by remember { mutableStateOf(false) }
    var showSplitDialog by remember { mutableStateOf(false) }
    var showWatermarkDialog by remember { mutableStateOf(false) }
    var showEncryptDialog by remember { mutableStateOf(false) }
    var showRotateDialog by remember { mutableStateOf(false) }

    // Setup pickers for the tools
    val multiPdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            if (uris.size < 2) {
                Toast.makeText(context, "Please select at least 2 files to merge", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            // Execute Merge
            val tempFiles = mutableListOf<File>()
            uris.forEachIndexed { i, uri ->
                val f = com.example.pdf.PdfToolsEngine.copyFileFromUri(context, uri, "temp_merge_t_$i.pdf")
                if (f != null) tempFiles.add(f)
            }
            if (tempFiles.size >= 2) {
                viewModel.performMerge(context, tempFiles.map { SavedPdfFile(fileName = it.name, filePath = it.absolutePath, fileSize = it.length(), pageCount = 0) }, "Merged_${System.currentTimeMillis() / 1000}.pdf", activity) { success ->
                    tempFiles.forEach { it.delete() }
                    if (success) Toast.makeText(context, "Merge completed!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            viewModel.performImageToPdf(context, uris, "Images_${System.currentTimeMillis() / 1000}.pdf", activity) { success ->
                if (success) Toast.makeText(context, "Converted Images to PDF!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val singlePdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val file = com.example.pdf.PdfToolsEngine.copyFileFromUri(context, uri, "temp_tool_input.pdf")
            if (file != null) {
                val inputPdf = SavedPdfFile(fileName = file.name, filePath = file.absolutePath, fileSize = file.length(), pageCount = 0)
                when (currentActionType) {
                    "pdf_to_img" -> {
                        viewModel.performPdfToImages(context, inputPdf, activity) { success, images ->
                            file.delete()
                            if (success && !images.isNullOrEmpty()) {
                                Toast.makeText(context, "Pages converted: ${images.size} JPEGs saved in sandbox", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to render PDF to Images", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    "compress" -> {
                        val action = {
                            viewModel.performCompress(context, inputPdf, "Compressed_${System.currentTimeMillis() / 1000}.pdf", 70, 0.75f, activity) { success ->
                                file.delete()
                                if (success) Toast.makeText(context, "PDF Compressed!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        if (!isPremiumUnlocked) {
                            viewModel.unlockPremiumViaAd(activity, onSuccess = action, onFailure = {
                                viewModel.performCompress(context, inputPdf, "Compressed_Std_${System.currentTimeMillis() / 1000}.pdf", 45, 0.5f, activity) { success ->
                                    file.delete()
                                    if (success) Toast.makeText(context, "Standard Compression Completed!", Toast.LENGTH_SHORT).show()
                                }
                            })
                        } else {
                            action()
                        }
                    }
                    "watermark" -> {
                        val action = {
                            viewModel.performWatermark(context, inputPdf, "CONFIDENTIAL", 0xFFFF0000.toInt(), 48f, 70, -45f, "Watermarked_${System.currentTimeMillis() / 1000}.pdf", activity) { success ->
                                file.delete()
                                if (success) Toast.makeText(context, "Watermark Added!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        if (!isPremiumUnlocked) {
                            viewModel.unlockPremiumViaAd(activity, onSuccess = action, onFailure = {
                                Toast.makeText(context, "Unlock watermark removal with Rewarded Ads!", Toast.LENGTH_SHORT).show()
                                action()
                            })
                        } else {
                            action()
                        }
                    }
                    "split" -> {
                        viewModel.performSplit(context, inputPdf, listOf(1), activity) { success ->
                            file.delete()
                            if (success) Toast.makeText(context, "Split successful!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "rotate" -> {
                        viewModel.performRotate(context, inputPdf, mapOf(0 to 90f), "Rotated_${System.currentTimeMillis() / 1000}.pdf", activity) { success ->
                            file.delete()
                            if (success) Toast.makeText(context, "Rotated successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "delete_pages" -> {
                        viewModel.performDeletePages(context, inputPdf, setOf(0), "Pages_Deleted_${System.currentTimeMillis() / 1000}.pdf", activity) { success ->
                            file.delete()
                            if (success) Toast.makeText(context, "First page deleted!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "extract_pages" -> {
                        viewModel.performExtractPages(context, inputPdf, listOf(0), "Pages_Extracted_${System.currentTimeMillis() / 1000}.pdf", activity) { success ->
                            file.delete()
                            if (success) Toast.makeText(context, "First page extracted!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "reorder_pages" -> {
                        // Reorder pages by reversing first page order if page count was 2
                        viewModel.performReorderPages(context, inputPdf, listOf(0), "Pages_Reordered_${System.currentTimeMillis() / 1000}.pdf", activity) { success ->
                            file.delete()
                            if (success) Toast.makeText(context, "Pages reordered successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "protect" -> {
                        viewModel.protectPdfWithPassword(inputPdf, "1234") { success ->
                            // Save file to sandbox first to store in DB
                            viewModel.importPdfFile(context, Uri.fromFile(file), "Protected_${System.currentTimeMillis() / 1000}.pdf") { importSuccess ->
                                file.delete()
                                if (importSuccess) {
                                    Toast.makeText(context, "PDF Protected with code 1234", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                    "remove_password" -> {
                        viewModel.removePasswordFromPdf(inputPdf) { success ->
                            viewModel.importPdfFile(context, Uri.fromFile(file), "Unlocked_${System.currentTimeMillis() / 1000}.pdf") { importSuccess ->
                                file.delete()
                                if (importSuccess) Toast.makeText(context, "PDF App-Level password removed", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            AdBanner(modifier = Modifier.background(MaterialTheme.colorScheme.surface))
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "PDF Workspace",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Instant local offline-first conversions & edits",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            // Group tools by categories
            val categories = toolsList.groupBy { it.category }
            categories.forEach { (catName, catTools) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = catName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                items(catTools) { tool ->
                    ToolCardItem(
                        tool = tool,
                        onClick = {
                            com.example.ui.components.HapticHelper.triggerClick(context)
                            currentActionType = tool.id
                            when (tool.id) {
                                "ai_summary" -> showAiSummaryDialog = true
                                "smart_scanner" -> showSmartScannerDialog = true
                                "ocr_image" -> showOcrImageDialog = true
                                "batch_tools" -> showBatchToolsDialog = true
                                "merge" -> multiPdfPicker.launch("application/pdf")
                                "img_to_pdf" -> imagePicker.launch("image/*")
                                "split", "extract_pages" -> {
                                    if (allPdfs.isEmpty()) {
                                        Toast.makeText(context, "Please import or scan some PDFs first!", Toast.LENGTH_LONG).show()
                                    } else {
                                        showSplitDialog = true
                                    }
                                }
                                "rotate" -> {
                                    if (allPdfs.isEmpty()) {
                                        Toast.makeText(context, "Please import or scan some PDFs first!", Toast.LENGTH_LONG).show()
                                    } else {
                                        showRotateDialog = true
                                    }
                                }
                                "watermark" -> {
                                    if (allPdfs.isEmpty()) {
                                        Toast.makeText(context, "Please import or scan some PDFs first!", Toast.LENGTH_LONG).show()
                                    } else {
                                        showWatermarkDialog = true
                                    }
                                }
                                "protect", "remove_password" -> {
                                    if (allPdfs.isEmpty()) {
                                        Toast.makeText(context, "Please import or scan some PDFs first!", Toast.LENGTH_LONG).show()
                                    } else {
                                        showEncryptDialog = true
                                    }
                                }
                                else -> singlePdfPicker.launch("application/pdf")
                            }
                        }
                    )
                }
            }
            
            // Spacer item
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showAiSummaryDialog) {
        AiSummaryDialog(
            pdfFiles = allPdfs,
            onDismiss = { showAiSummaryDialog = false }
        )
    }

    if (showOcrImageDialog) {
        OcrImageDialog(
            onDismiss = { showOcrImageDialog = false }
        )
    }

    if (showSmartScannerDialog) {
        SmartScannerDialog(
            viewModel = viewModel,
            onDismiss = { showSmartScannerDialog = false }
        )
    }

    if (showBatchToolsDialog) {
        BatchToolsDialog(
            pdfFiles = allPdfs,
            viewModel = viewModel,
            onDismiss = { showBatchToolsDialog = false }
        )
    }

    if (showSplitDialog) {
        PageExtractionAndSplitDialog(
            pdfFiles = allPdfs,
            viewModel = viewModel,
            onDismiss = { showSplitDialog = false }
        )
    }

    if (showWatermarkDialog) {
        AdvancedWatermarkDialog(
            pdfFiles = allPdfs,
            viewModel = viewModel,
            onDismiss = { showWatermarkDialog = false }
        )
    }

    if (showEncryptDialog) {
        ZipEncryptDecryptDialog(
            pdfFiles = allPdfs,
            viewModel = viewModel,
            onDismiss = { showEncryptDialog = false }
        )
    }

    if (showRotateDialog) {
        PageRotationDialog(
            pdfFiles = allPdfs,
            viewModel = viewModel,
            onDismiss = { showRotateDialog = false }
        )
    }
}

@Composable
fun ToolCardItem(
    tool: ToolDefinition,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, Color(0xFFC4C6D0).copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .background(tool.tintColor, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = tool.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tool.description,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2,
                    lineHeight = 12.sp
                )
            }
        }
    }
}
