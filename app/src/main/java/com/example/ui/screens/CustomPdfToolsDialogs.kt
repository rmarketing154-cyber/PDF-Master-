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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.SavedPdfFile
import com.example.ui.components.HapticHelper
import com.example.ui.viewmodel.PdfViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageExtractionAndSplitDialog(
    pdfFiles: List<SavedPdfFile>,
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity

    var selectedFile by remember { mutableStateOf<SavedPdfFile?>(pdfFiles.firstOrNull()) }
    var showDropdown by remember { mutableStateOf(false) }

    // Page selection state
    val totalPages = selectedFile?.pageCount ?: 0
    val selectedPages = remember(selectedFile) { mutableStateMapOf<Int, Boolean>() }
    
    var customRangeText by remember { mutableStateOf("") }
    var useRangeInput by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallSplit,
                        contentDescription = "Split/Extract Pages",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Split & Extract Pages",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // File selection dropdown
                    item {
                        Text(
                            text = "Select Document",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        ExposedDropdownMenuBox(
                            expanded = showDropdown,
                            onExpandedChange = { showDropdown = !showDropdown }
                        ) {
                            OutlinedTextField(
                                value = selectedFile?.fileName ?: "No PDF selected",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = showDropdown,
                                onDismissRequest = { showDropdown = false }
                            ) {
                                pdfFiles.forEach { file ->
                                    DropdownMenuItem(
                                        text = { Text(file.fileName) },
                                        onClick = {
                                            selectedFile = file
                                            showDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedFile != null) {
                        item {
                            Text(
                                text = "Document Info: ${selectedFile?.pageCount} total pages",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Toggle page mode
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Enter Custom Page Ranges (e.g. 1-3, 5)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Switch(
                                    checked = useRangeInput,
                                    onCheckedChange = { useRangeInput = it }
                                )
                            }
                        }

                        if (useRangeInput) {
                            item {
                                OutlinedTextField(
                                    value = customRangeText,
                                    onValueChange = { customRangeText = it },
                                    placeholder = { Text("e.g. 1-3, 5, 7") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                            }
                        } else {
                            // Checkbox Grid of pages
                            item {
                                Text(
                                    text = "Select Pages to Extract",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            items((0 until totalPages).toList()) { index ->
                                val isChecked = selectedPages[index] ?: false
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedPages[index] = !isChecked }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { selectedPages[index] = it }
                                    )
                                    Text(
                                        text = "Page ${index + 1}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Actions Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val targetFile = selectedFile ?: return@Button
                            HapticHelper.triggerClick(context)

                            val indices = mutableListOf<Int>()
                            if (useRangeInput) {
                                // Parse custom range
                                try {
                                    val parts = customRangeText.split(",")
                                    parts.forEach { part ->
                                        val clean = part.trim()
                                        if (clean.contains("-")) {
                                            val bounds = clean.split("-")
                                            val start = bounds[0].trim().toInt() - 1
                                            val end = bounds[1].trim().toInt() - 1
                                            for (p in start..end) {
                                                if (p in 0 until totalPages) indices.add(p)
                                            }
                                        } else if (clean.isNotEmpty()) {
                                            val p = clean.toInt() - 1
                                            if (p in 0 until totalPages) indices.add(p)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Invalid range format!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                            } else {
                                // Accumulate selected check boxes
                                selectedPages.forEach { (idx, checked) ->
                                    if (checked) indices.add(idx)
                                }
                            }

                            if (indices.isEmpty()) {
                                Toast.makeText(context, "Please select or input pages", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Trigger extraction
                            viewModel.performExtractPages(
                                context,
                                targetFile,
                                indices.sorted(),
                                "Extracted_${System.currentTimeMillis() / 1000}.pdf",
                                activity
                            ) { success ->
                                if (success) {
                                    Toast.makeText(context, "Pages extracted successfully!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedFile != null
                    ) {
                        Text("Extract")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedWatermarkDialog(
    pdfFiles: List<SavedPdfFile>,
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity

    var selectedFile by remember { mutableStateOf<SavedPdfFile?>(pdfFiles.firstOrNull()) }
    var showFileDropdown by remember { mutableStateOf(false) }

    // Type of watermark
    var isTextMode by remember { mutableStateOf(true) }

    // Text configuration
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var selectedColor by remember { mutableStateOf(Color.Red) }
    var sizeSliderValue by remember { mutableStateOf(48f) }

    // Image configuration
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageName by remember { mutableStateOf("") }
    
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            selectedImageName = uri.lastPathSegment ?: "Selected Image"
        }
    }

    // Common slider configurations
    var opacity by remember { mutableStateOf(0.4f) }
    var rotation by remember { mutableStateOf(-45f) }
    
    // Position dropdown
    val positions = listOf("Center", "Top Left", "Top Right", "Bottom Left", "Bottom Right")
    var selectedPosition by remember { mutableStateOf("Center") }
    var showPositionDropdown by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Watermark",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Overlay Custom Watermark",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // File Picker
                    item {
                        Text(
                            text = "Select Document",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        ExposedDropdownMenuBox(
                            expanded = showFileDropdown,
                            onExpandedChange = { showFileDropdown = !showFileDropdown }
                        ) {
                            OutlinedTextField(
                                value = selectedFile?.fileName ?: "No PDF selected",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFileDropdown) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = showFileDropdown,
                                onDismissRequest = { showFileDropdown = false }
                            ) {
                                pdfFiles.forEach { file ->
                                    DropdownMenuItem(
                                        text = { Text(file.fileName) },
                                        onClick = {
                                            selectedFile = file
                                            showFileDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Watermark Type Selector
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = isTextMode,
                                onClick = { isTextMode = true },
                                label = { Text("Text Watermark") },
                                leadingIcon = if (isTextMode) { { Icon(Icons.Default.TextFields, "Text") } } else null,
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = !isTextMode,
                                onClick = { isTextMode = false },
                                label = { Text("Image Watermark") },
                                leadingIcon = if (!isTextMode) { { Icon(Icons.Default.Image, "Image") } } else null,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (isTextMode) {
                        // Text Parameters
                        item {
                            OutlinedTextField(
                                value = watermarkText,
                                onValueChange = { watermarkText = it },
                                label = { Text("Watermark Text") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        item {
                            Text(
                                text = "Text Color",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val colors = listOf(Color.Red, Color.Blue, Color.Gray, Color.Green, Color.Black)
                                colors.forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                color,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedColor = color }
                                            .padding(2.dp)
                                    ) {
                                        if (selectedColor == color) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = if (color == Color.Black) Color.White else Color.White,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Text Size",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text("${sizeSliderValue.toInt()} sp")
                                }
                                Slider(
                                    value = sizeSliderValue,
                                    onValueChange = { sizeSliderValue = it },
                                    valueRange = 12f..120f
                                )
                            }
                        }
                    } else {
                        // Image parameter selection
                        item {
                            OutlinedButton(
                                onClick = { imageLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = "Upload Image")
                                    Text(selectedImageName.ifEmpty { "Select Watermark Image" })
                                }
                            }
                        }
                    }

                    // Shared Parameters: Opacity, Rotation, Position
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Opacity",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("${(opacity * 100).toInt()}%")
                            }
                            Slider(
                                value = opacity,
                                onValueChange = { opacity = it },
                                valueRange = 0.05f..1.0f
                            )
                        }
                    }

                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Rotation Angle",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("${rotation.toInt()}°")
                            }
                            Slider(
                                value = rotation,
                                onValueChange = { rotation = it },
                                valueRange = -180f..180f
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Position",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        ExposedDropdownMenuBox(
                            expanded = showPositionDropdown,
                            onExpandedChange = { showPositionDropdown = !showPositionDropdown }
                        ) {
                            OutlinedTextField(
                                value = selectedPosition,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPositionDropdown) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = showPositionDropdown,
                                onDismissRequest = { showPositionDropdown = false }
                            ) {
                                positions.forEach { pos ->
                                    DropdownMenuItem(
                                        text = { Text(pos) },
                                        onClick = {
                                            selectedPosition = pos
                                            showPositionDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Actions Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val targetFile = selectedFile ?: return@Button
                            HapticHelper.triggerClick(context)

                            if (!isTextMode && selectedImageUri == null) {
                                Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.performAdvancedWatermark(
                                context = context,
                                file = targetFile,
                                isText = isTextMode,
                                text = watermarkText,
                                textColor = selectedColor.toArgb(),
                                textSize = sizeSliderValue,
                                imageUri = selectedImageUri,
                                opacity = opacity,
                                rotation = rotation,
                                position = selectedPosition,
                                outName = "Watermarked_${System.currentTimeMillis() / 1000}.pdf",
                                activity = activity
                            ) { success ->
                                if (success) {
                                    Toast.makeText(context, "Watermark overlays applied!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedFile != null
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZipEncryptDecryptDialog(
    pdfFiles: List<SavedPdfFile>,
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity

    var isEncryptMode by remember { mutableStateOf(true) }

    // Filter file choices based on mode
    val selectableFiles = remember(pdfFiles, isEncryptMode) {
        if (isEncryptMode) {
            pdfFiles.filter { !it.hasPassword && !it.fileName.endsWith(".zip", ignoreCase = true) }
        } else {
            pdfFiles.filter { it.hasPassword || it.fileName.endsWith(".zip", ignoreCase = true) }
        }
    }

    var selectedFile by remember(selectableFiles) { mutableStateOf(selectableFiles.firstOrNull()) }
    var showFileDropdown by remember { mutableStateOf(false) }

    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = if (isEncryptMode) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Zip4j Crypt",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = if (isEncryptMode) "Lock with Zip4j AES" else "Decrypt Locked Zip",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tab choice
                    TabRow(
                        selectedTabIndex = if (isEncryptMode) 0 else 1,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = isEncryptMode,
                            onClick = { isEncryptMode = true },
                            text = { Text("Lock File") }
                        )
                        Tab(
                            selected = !isEncryptMode,
                            onClick = { isEncryptMode = false },
                            text = { Text("Unlock File") }
                        )
                    }

                    // Selected File dropdown
                    Text(
                        text = "Select Target File",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ExposedDropdownMenuBox(
                        expanded = showFileDropdown,
                        onExpandedChange = { showFileDropdown = !showFileDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedFile?.fileName ?: "No files available",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFileDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (selectableFiles.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = showFileDropdown,
                                onDismissRequest = { showFileDropdown = false }
                            ) {
                                selectableFiles.forEach { file ->
                                    DropdownMenuItem(
                                        text = { Text(file.fileName) },
                                        onClick = {
                                            selectedFile = file
                                            showFileDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Password Input
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Enter Encryption Password") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        }
                    )
                }

                // Actions Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val targetFile = selectedFile ?: return@Button
                            HapticHelper.triggerClick(context)

                            if (passwordInput.isEmpty()) {
                                Toast.makeText(context, "Password is required", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (isEncryptMode) {
                                viewModel.performZipEncrypt(
                                    context = context,
                                    file = targetFile,
                                    password = passwordInput,
                                    outName = "Secure_${targetFile.fileNameWithoutExtension}.zip",
                                    activity = activity
                                ) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Document Encrypted & Locked in ZIP!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                }
                            } else {
                                viewModel.performZipDecrypt(
                                    context = context,
                                    file = targetFile,
                                    password = passwordInput,
                                    outName = "Unlocked_${targetFile.fileNameWithoutExtension}.pdf",
                                    activity = activity
                                ) { success, resultFile ->
                                    if (success) {
                                        Toast.makeText(context, "Unzipped & Decrypted Successfully!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedFile != null
                    ) {
                        Text(if (isEncryptMode) "Lock" else "Unlock")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageRotationDialog(
    pdfFiles: List<SavedPdfFile>,
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity

    var selectedFile by remember { mutableStateOf<SavedPdfFile?>(pdfFiles.firstOrNull()) }
    var showDropdown by remember { mutableStateOf(false) }

    val totalPages = selectedFile?.pageCount ?: 0
    val pageRotations = remember(selectedFile) { mutableStateMapOf<Int, Float>() }

    var previewPage by remember { mutableStateOf(0) }
    var previewEnabled by remember { mutableStateOf(true) }

    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoadingPreview by remember { mutableStateOf(false) }

    // Reset preview page when selected file changes
    LaunchedEffect(selectedFile) {
        previewPage = 0
    }

    // Load preview bitmap asynchronously when file, page, or its rotation changes
    LaunchedEffect(selectedFile, previewPage, previewEnabled, pageRotations[previewPage]) {
        if (previewEnabled && selectedFile != null) {
            isLoadingPreview = true
            val file = File(selectedFile!!.filePath)
            val angle = pageRotations[previewPage] ?: 0f
            val bm = withContext(Dispatchers.IO) {
                com.example.pdf.PdfToolsEngine.renderPageToBitmap(context, file, previewPage, angle)
            }
            previewBitmap = bm
            isLoadingPreview = false
        } else {
            previewBitmap = null
            isLoadingPreview = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RotateRight,
                        contentDescription = "Rotate PDF Pages",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Rotate PDF Pages",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Document selector
                    item {
                        Text(
                            text = "Select Document",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        ExposedDropdownMenuBox(
                            expanded = showDropdown,
                            onExpandedChange = { showDropdown = !showDropdown }
                        ) {
                            OutlinedTextField(
                                value = selectedFile?.fileName ?: "No PDF selected",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = showDropdown,
                                onDismissRequest = { showDropdown = false }
                            ) {
                                pdfFiles.forEach { file ->
                                    DropdownMenuItem(
                                        text = { Text(file.fileName) },
                                        onClick = {
                                            selectedFile = file
                                            showDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedFile != null) {
                        item {
                            Text(
                                text = "Document Info: $totalPages total pages",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Bulk "Rotate All" tools
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Rotate All Pages",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(90f, 180f, 270f, 0f).forEach { angle ->
                                            val label = if (angle == 0f) "Reset" else "${angle.toInt()}°"
                                            Button(
                                                onClick = {
                                                    HapticHelper.triggerClick(context)
                                                    for (p in 0 until totalPages) {
                                                        pageRotations[p] = angle
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (angle == 0f) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = if (angle == 0f) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                                ),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Live Preview toggle
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Preview",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Live Preview of Pages",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = previewEnabled,
                                    onCheckedChange = { previewEnabled = it }
                                )
                            }
                        }

                        if (previewEnabled) {
                            // Active Page Preview layout
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Page selector row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilledIconButton(
                                            onClick = {
                                                HapticHelper.triggerClick(context)
                                                if (previewPage > 0) previewPage--
                                            },
                                            enabled = previewPage > 0,
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        ) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Previous Page")
                                        }

                                        Text(
                                            text = "Page ${previewPage + 1} of $totalPages",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        FilledIconButton(
                                            onClick = {
                                                HapticHelper.triggerClick(context)
                                                if (previewPage < totalPages - 1) previewPage++
                                            },
                                            enabled = previewPage < totalPages - 1,
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        ) {
                                            Icon(Icons.Default.ArrowForward, contentDescription = "Next Page")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Preview Frame
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(240.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoadingPreview) {
                                            CircularProgressIndicator()
                                        } else if (previewBitmap != null) {
                                            Image(
                                                bitmap = previewBitmap!!.asImageBitmap(),
                                                contentDescription = "Page Preview",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(12.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Text(
                                                text = "Failed to render preview",
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Page-specific rotation selector
                                    Text(
                                        text = "Configure Current Page Rotation",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(0f, 90f, 180f, 270f).forEach { angle ->
                                            val currentAngle = pageRotations[previewPage] ?: 0f
                                            val isSelected = currentAngle == angle
                                            val label = if (angle == 0f) "0° (Reset)" else "${angle.toInt()}°"

                                            ElevatedButton(
                                                onClick = {
                                                    HapticHelper.triggerClick(context)
                                                    pageRotations[previewPage] = angle
                                                },
                                                modifier = Modifier.weight(1.5f),
                                                colors = ButtonDefaults.elevatedButtonColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Detailed Bulk configuration list
                            item {
                                Text(
                                    text = "All Pages Configuration",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            items((0 until totalPages).toList()) { index ->
                                val currentAngle = pageRotations[index] ?: 0f
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Page ${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            listOf(0f, 90f, 180f, 270f).forEach { angle ->
                                                val isSelected = currentAngle == angle
                                                val label = if (angle == 0f) "0°" else "${angle.toInt()}°"

                                                SuggestionChip(
                                                    onClick = {
                                                        HapticHelper.triggerClick(context)
                                                        pageRotations[index] = angle
                                                    },
                                                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                        labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    border = if (isSelected) null else SuggestionChipDefaults.suggestionChipBorder(true)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Actions Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val targetFile = selectedFile ?: return@Button
                            HapticHelper.triggerClick(context)

                            // Construct rotations map
                            val rotationsMap = mutableMapOf<Int, Float>()
                            for (p in 0 until totalPages) {
                                val rot = pageRotations[p] ?: 0f
                                if (rot != 0f) {
                                    rotationsMap[p] = rot
                                }
                            }

                            if (rotationsMap.isEmpty()) {
                                Toast.makeText(context, "No rotation changes specified to apply!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val outName = "Rotated_${System.currentTimeMillis() / 1000}_${targetFile.fileName}"
                            viewModel.performRotate(
                                context = context,
                                file = targetFile,
                                pageRotations = rotationsMap,
                                outName = outName,
                                activity = activity
                            ) { success ->
                                if (success) {
                                    Toast.makeText(context, "Pages rotated successfully!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedFile != null
                    ) {
                        Text("Apply & Save")
                    }
                }
            }
        }
    }
}

private val SavedPdfFile.fileNameWithoutExtension: String
    get() = if (fileName.contains(".")) fileName.substringBeforeLast(".") else fileName
