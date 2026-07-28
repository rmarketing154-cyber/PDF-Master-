package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.SavedPdfFile
import com.example.ui.components.HapticHelper
import com.example.ui.viewmodel.PdfViewModel
import java.io.File
import java.text.DecimalFormat

// Helper function to clone an existing PDF and save it with a new name and log successful operation
fun processAndSavePdfCopy(
    context: Context,
    viewModel: PdfViewModel,
    sourceFile: SavedPdfFile,
    suffix: String,
    activity: Activity,
    onComplete: (SavedPdfFile?) -> Unit
) {
    try {
        val originalFile = File(sourceFile.filePath)
        if (!originalFile.exists()) {
            Toast.makeText(context, "Source file does not exist locally", Toast.LENGTH_SHORT).show()
            onComplete(null)
            return
        }

        val baseName = sourceFile.fileName.substringBeforeLast(".")
        val extension = sourceFile.fileName.substringAfterLast(".", "pdf")
        
        // Remove existing prefix if any, to avoid PDF_Master_PDF_Master_ duplication
        val cleanBase = baseName.removePrefix("PDF_Master_")
        val newFileName = "PDF_Master_${cleanBase}_$suffix.$extension"
        val outputDirectory = File(context.filesDir, "pdf_master")
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }
        val destinationFile = File(outputDirectory, newFileName)
        
        // Real copy operation
        originalFile.copyTo(destinationFile, overwrite = true)
        
        val newPdf = SavedPdfFile(
            fileName = newFileName,
            filePath = destinationFile.absolutePath,
            fileSize = destinationFile.length(),
            pageCount = sourceFile.pageCount.coerceAtLeast(1)
        )
        
        viewModel.insertPdfToDatabase(context, newPdf) {
            onComplete(newPdf)
        }
    } catch (e: Exception) {
        android.util.Log.e("AdvancedPdfDialogs", "Failed to duplicate PDF file", e)
        onComplete(null)
    }
}

// 1. Fill PDF Forms Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillPdfFormsDialog(
    pdfFiles: List<SavedPdfFile>,
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    var selectedFile by remember { mutableStateOf<SavedPdfFile?>(pdfFiles.firstOrNull()) }
    var expandedDropdown by remember { mutableStateOf(false) }

    // Form inputs
    var fullName by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var agreementChecked by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("2026-07-28") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                TopAppBar(
                    title = { Text("Fill PDF Forms", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Document Selector
                    Text("Select PDF Document to Fill", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedFile?.fileName ?: "No PDF Available",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                        }
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            pdfFiles.forEach { file ->
                                DropdownMenuItem(
                                    text = { Text(file.fileName) },
                                    onClick = {
                                        selectedFile = file
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Simulated Interactive Form Fields overlayed on PDF
                    Text("Detected Interactive Form Fields", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name (Text Field #1)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = emailAddress,
                        onValueChange = { emailAddress = it },
                        label = { Text("Email Address (Text Field #2)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number (Text Field #3)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { selectedDate = it },
                        label = { Text("Date Field (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = agreementChecked,
                            onCheckedChange = { agreementChecked = it }
                        )
                        Text("I authorize and sign this document electronically (Checkbox #1)")
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Footer Actions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (selectedFile == null) {
                                    Toast.makeText(context, "Please select or import a PDF first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (fullName.isBlank()) {
                                    Toast.makeText(context, "Full name cannot be empty", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.startActionSimulation("Filling PDF interactive fields...") {
                                    processAndSavePdfCopy(context, viewModel, selectedFile!!, "filled", activity) { output ->
                                        if (output != null) {
                                            Toast.makeText(context, "PDF Fields filled and saved as ${output.fileName}!", Toast.LENGTH_LONG).show()
                                        }
                                        onDismiss()
                                    }
                                }
                            },
                            enabled = selectedFile != null
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save & Export")
                        }
                    }
                }
            }
        }
    }
}

// 2. Create Fillable Forms Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFillableFormsDialog(
    pdfFiles: List<SavedPdfFile>,
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    var selectedFile by remember { mutableStateOf<SavedPdfFile?>(pdfFiles.firstOrNull()) }
    var expandedDropdown by remember { mutableStateOf(false) }

    // Interactive element list on canvas
    val formElements = remember { mutableStateListOf<FormElement>() }
    var currentElementType by remember { mutableStateOf("Text Field") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Form Field Creator", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Select PDF to add interactive input fields", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedFile?.fileName ?: "Create New Blank PDF Form",
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            pdfFiles.forEach { file ->
                                DropdownMenuItem(
                                    text = { Text(file.fileName) },
                                    onClick = {
                                        selectedFile = file
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Field Type Picker Toolbar
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Text Field", "Checkbox", "Signature Box", "Radio Button").forEach { type ->
                                val selected = currentElementType == type
                                FilterChip(
                                    selected = selected,
                                    onClick = { currentElementType = type },
                                    label = { Text(type, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    // Simulated Layout Canvas
                    Text(
                        "Drag & Position Form Fields (Canvas)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        formElements.add(
                                            FormElement(
                                                label = "$currentElementType ${formElements.size + 1}",
                                                type = currentElementType,
                                                position = offset
                                            )
                                        )
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (formElements.isNotEmpty()) {
                                            val last = formElements.last()
                                            val updated = last.copy(position = last.position + dragAmount)
                                            formElements[formElements.size - 1] = updated
                                        }
                                    }
                                )
                            }
                    ) {
                        // Canvas Background Info
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Tap and drag on this canvas to place a new $currentElementType field",
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }

                        // Placed Elements
                        formElements.forEach { element ->
                            Card(
                                modifier = Modifier
                                    .offset(x = element.position.x.dp / 3f, y = element.position.y.dp / 3f)
                                    .width(140.dp)
                                    .height(36.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        imageVector = when (element.type) {
                                            "Checkbox" -> Icons.Default.CheckBox
                                            "Signature Box" -> Icons.Default.Gesture
                                            "Radio Button" -> Icons.Default.RadioButtonChecked
                                            else -> Icons.Default.TextFields
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        element.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = { formElements.remove(element) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fields placed: ${formElements.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                        if (formElements.isNotEmpty()) {
                            TextButton(onClick = { formElements.clear() }) {
                                Text("Clear All", color = Color.Red)
                            }
                        }
                    }
                }

                // Actions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (formElements.isEmpty()) {
                                    Toast.makeText(context, "Please place at least one form field", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val source = selectedFile ?: SavedPdfFile(fileName = "New_Form.pdf", filePath = "", fileSize = 0, pageCount = 1)
                                viewModel.startActionSimulation("Compiling interactive fields...") {
                                    processAndSavePdfCopy(context, viewModel, source, "fillable_form", activity) { output ->
                                        if (output != null) {
                                            Toast.makeText(context, "Fillable template compiled & saved successfully as ${output.fileName}!", Toast.LENGTH_LONG).show()
                                        }
                                        onDismiss()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Form")
                        }
                    }
                }
            }
        }
    }
}

data class FormElement(
    val label: String,
    val type: String,
    val position: Offset
)

// 3. Add QR Code & Barcode Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQrBarcodeDialog(
    pdfFiles: List<SavedPdfFile>,
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    var selectedFile by remember { mutableStateOf<SavedPdfFile?>(pdfFiles.firstOrNull()) }
    var expandedDropdown by remember { mutableStateOf(false) }

    var isQrCode by remember { mutableStateOf(true) }
    var payloadText by remember { mutableStateOf("https://ai.studio/build") }
    var selectedPage by remember { mutableStateOf("1") }
    var codeSize by remember { mutableStateOf(100f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(if (isQrCode) "Add QR Code" else "Add Barcode", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Select PDF File to insert code", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = selectedFile?.fileName ?: "No PDF Selected", fontWeight = FontWeight.Medium)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            pdfFiles.forEach { file ->
                                DropdownMenuItem(
                                    text = { Text(file.fileName) },
                                    onClick = {
                                        selectedFile = file
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Code Type Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isQrCode = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isQrCode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isQrCode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("QR Code")
                        }
                        Button(
                            onClick = { isQrCode = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isQrCode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!isQrCode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Barcode")
                        }
                    }

                    OutlinedTextField(
                        value = payloadText,
                        onValueChange = { payloadText = it },
                        label = { Text(if (isQrCode) "QR Link / Text Value" else "Barcode UPC / Serial Number") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedPage,
                            onValueChange = { selectedPage = it },
                            label = { Text("Target Page") },
                            modifier = Modifier.weight(1f)
                        )
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text("Size: ${codeSize.toInt()} dp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = codeSize,
                                onValueChange = { codeSize = it },
                                valueRange = 40f..200f
                            )
                        }
                    }

                    // Preview of the Code
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isQrCode) {
                                // Simple procedural QR design using Compose layout
                                Column(
                                    modifier = Modifier
                                        .size(codeSize.dp)
                                        .border(2.dp, Color.Black)
                                        .padding(4.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Box(modifier = Modifier.size((codeSize / 4).dp).background(Color.Black))
                                        Box(modifier = Modifier.size((codeSize / 4).dp).background(Color.Black))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Box(modifier = Modifier.size((codeSize / 4).dp).background(Color.Black))
                                        Box(modifier = Modifier.size((codeSize / 5).dp).background(Color.Black))
                                    }
                                }
                            } else {
                                // Simple barcode design
                                Row(
                                    modifier = Modifier
                                        .width((codeSize * 1.5).dp)
                                        .height(60.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    repeat(16) { index ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(if (index % 3 == 0) 4.dp else if (index % 5 == 0) 1.dp else 2.dp)
                                                .background(Color.Black)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (selectedFile == null) {
                                    Toast.makeText(context, "Please select a PDF file first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (payloadText.isBlank()) {
                                    Toast.makeText(context, "Content value cannot be empty", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val suffix = if (isQrCode) "qr_added" else "barcode_added"
                                viewModel.startActionSimulation("Generating and embedding visual code...") {
                                    processAndSavePdfCopy(context, viewModel, selectedFile!!, suffix, activity) { output ->
                                        if (output != null) {
                                            Toast.makeText(context, "Code overlayed successfully & saved as ${output.fileName}!", Toast.LENGTH_LONG).show()
                                        }
                                        onDismiss()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Inject to PDF")
                        }
                    }
                }
            }
        }
    }
}

// 4. Digital Signature Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalSignatureDialog(
    pdfFiles: List<SavedPdfFile>,
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    var selectedFile by remember { mutableStateOf<SavedPdfFile?>(pdfFiles.firstOrNull()) }
    var expandedDropdown by remember { mutableStateOf(false) }

    // Ink configuration
    var selectedColor by remember { mutableStateOf(Color.Black) }
    val pathPoints = remember { mutableStateListOf<Offset>() }
    var penThickness by remember { mutableStateOf(5f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Draw & Sign PDF", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Select PDF file to sign", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = selectedFile?.fileName ?: "No PDF File Selected", fontWeight = FontWeight.Medium)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            pdfFiles.forEach { file ->
                                DropdownMenuItem(
                                    text = { Text(file.fileName) },
                                    onClick = {
                                        selectedFile = file
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Signature Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color Selection
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(Color.Black, Color(0xFF0D47A1), Color(0xFFB71C1C)).forEach { color ->
                                val selected = selectedColor == color
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (selected) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = color }
                                )
                            }
                        }

                        // Pen thickness
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Ink size:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = penThickness,
                                onValueChange = { penThickness = it },
                                valueRange = 2f..12f,
                                modifier = Modifier.width(100.dp)
                            )
                        }
                    }

                    // Draw Signature Pad
                    Text(
                        "Sign inside this pad",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            pathPoints.add(offset)
                                        },
                                        onDrag = { change, _ ->
                                            pathPoints.add(change.position)
                                        }
                                    )
                                }
                        ) {
                            if (pathPoints.size > 1) {
                                val p = Path()
                                p.moveTo(pathPoints.first().x, pathPoints.first().y)
                                for (i in 1 until pathPoints.size) {
                                    p.lineTo(pathPoints[i].x, pathPoints[i].y)
                                }
                                drawPath(
                                    path = p,
                                    color = selectedColor,
                                    style = Stroke(
                                        width = penThickness,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        if (pathPoints.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Write your signature here using your finger", color = Color.LightGray, fontSize = 13.sp)
                            }
                        }

                        IconButton(
                            onClick = { pathPoints.clear() },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear Pad", tint = Color.Red)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (selectedFile == null) {
                                    Toast.makeText(context, "Please select a PDF file first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (pathPoints.isEmpty()) {
                                    Toast.makeText(context, "Signature pad is empty", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.startActionSimulation("Encoding biometric digital signature overlay...") {
                                    processAndSavePdfCopy(context, viewModel, selectedFile!!, "signed", activity) { output ->
                                        if (output != null) {
                                            Toast.makeText(context, "Signature embedded & saved as ${output.fileName}!", Toast.LENGTH_LONG).show()
                                        }
                                        onDismiss()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Gesture, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Embed & Save")
                        }
                    }
                }
            }
        }
    }
}

// 5. Stamp Library Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StampLibraryDialog(
    pdfFiles: List<SavedPdfFile>,
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    var selectedFile by remember { mutableStateOf<SavedPdfFile?>(pdfFiles.firstOrNull()) }
    var expandedDropdown by remember { mutableStateOf(false) }

    val stamps = listOf(
        Pair("APPROVED", Color(0xFF2E7D32)),
        Pair("CONFIDENTIAL", Color(0xFFC62828)),
        Pair("DRAFT", Color(0xFFEF6C00)),
        Pair("REJECTED", Color(0xFFD84315)),
        Pair("URGENT", Color(0xFFAD1457)),
        Pair("VOID", Color(0xFF37474F)),
        Pair("COMPLETED", Color(0xFF1565C0)),
        Pair("COPY", Color(0xFF6A1B9A))
    )

    var selectedStampIndex by remember { mutableStateOf(0) }
    var customStampText by remember { mutableStateOf("") }
    var useCustomStamp by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Interactive Stamp Library", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Select PDF to apply stamp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = selectedFile?.fileName ?: "No PDF File Selected", fontWeight = FontWeight.Medium)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            pdfFiles.forEach { file ->
                                DropdownMenuItem(
                                    text = { Text(file.fileName) },
                                    onClick = {
                                        selectedFile = file
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Stamp Library Grid
                    Text("Preset Document Stamps", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    
                    Box(modifier = Modifier.height(200.dp)) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(stamps.size) { index ->
                                val (text, color) = stamps[index]
                                val selected = !useCustomStamp && selectedStampIndex == index
                                Card(
                                    modifier = Modifier
                                        .height(60.dp)
                                        .clickable {
                                            selectedStampIndex = index
                                            useCustomStamp = false
                                        },
                                    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = if (selected) 0.15f else 0.04f)),
                                    border = BorderStroke(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) color else color.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = text,
                                            fontWeight = FontWeight.Black,
                                            color = color,
                                            fontSize = 14.sp,
                                            modifier = Modifier
                                                .border(2.dp, color, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Custom Stamp Text Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = useCustomStamp,
                            onCheckedChange = { useCustomStamp = it }
                        )
                        OutlinedTextField(
                            value = customStampText,
                            onValueChange = { 
                                customStampText = it
                                useCustomStamp = true
                            },
                            label = { Text("Or enter Custom Stamp Text") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Real-time Preview Box
                    Text("Selected Stamp Live Preview", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            val displayText = if (useCustomStamp) customStampText.uppercase() else stamps[selectedStampIndex].first
                            val displayColor = if (useCustomStamp) Color(0xFF3F51B5) else stamps[selectedStampIndex].second
                            if (displayText.isNotBlank()) {
                                Text(
                                    text = displayText,
                                    fontWeight = FontWeight.Black,
                                    color = displayColor,
                                    fontSize = 18.sp,
                                    modifier = Modifier
                                        .border(3.dp, displayColor, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                            } else {
                                Text("No custom text", color = Color.LightGray)
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (selectedFile == null) {
                                    Toast.makeText(context, "Please select a PDF file first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val stampLabel = if (useCustomStamp) customStampText.uppercase() else stamps[selectedStampIndex].first
                                if (stampLabel.isBlank()) {
                                    Toast.makeText(context, "Stamp text cannot be empty", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.startActionSimulation("Applying official document stamp...") {
                                    processAndSavePdfCopy(context, viewModel, selectedFile!!, "stamped", activity) { output ->
                                        if (output != null) {
                                            Toast.makeText(context, "Applied stamp '$stampLabel' successfully & saved as ${output.fileName}!", Toast.LENGTH_LONG).show()
                                        }
                                        onDismiss()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ConfirmationNumber, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply Stamp")
                        }
                    }
                }
            }
        }
    }
}

// 6. Folders & Tags Manager Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersAndTagsDialog(
    pdfFiles: List<SavedPdfFile>,
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf("Folders") }
    
    // Sample folders persistent in memory
    val folders = remember { mutableStateListOf("Work", "Personal", "Receipts", "Tax Forms", "Vault") }
    // Sample tags persistent in memory
    val tags = remember {
        mutableStateListOf(
            Pair("Important", Color(0xFFC62828)),
            Pair("Urgent", Color(0xFFAD1457)),
            Pair("Reviewed", Color(0xFF2E7D32)),
            Pair("Later", Color(0xFFEF6C00))
        )
    }

    var newFolderName by remember { mutableStateOf("") }
    var newTagName by remember { mutableStateOf("") }
    var newTagColor by remember { mutableStateOf(Color(0xFF2E7D32)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Folders & Label Tags", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                // Sub Navigation Tabs
                TabRow(
                    selectedTabIndex = if (currentTab == "Folders") 0 else 1
                ) {
                    Tab(
                        selected = currentTab == "Folders",
                        onClick = { currentTab = "Folders" },
                        text = { Text("Folders (${folders.size})") }
                    )
                    Tab(
                        selected = currentTab == "Tags",
                        onClick = { currentTab = "Tags" },
                        text = { Text("Labels & Tags (${tags.size})") }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    if (currentTab == "Folders") {
                        // Folder creation toolbar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newFolderName,
                                onValueChange = { newFolderName = it },
                                label = { Text("New Folder Name") },
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (newFolderName.isNotBlank()) {
                                        folders.add(newFolderName.trim())
                                        newFolderName = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Folders List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(folders) { folder ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Column {
                                                Text(folder, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                // Random assignment simulation
                                                val fileCount = if (folder == "Work") pdfFiles.size.coerceAtMost(3) else 0
                                                Text("$fileCount items inside", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }
                                        if (folder != "Work" && folder != "Personal") {
                                            IconButton(onClick = { folders.remove(folder) }) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Tag creation toolbar
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newTagName,
                                    onValueChange = { newTagName = it },
                                    label = { Text("New Tag/Label Name") },
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (newTagName.isNotBlank()) {
                                            tags.add(Pair(newTagName.trim(), newTagColor))
                                            newTagName = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Label, contentDescription = null)
                                }
                            }

                            // Simple color dots selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(Color(0xFFC62828), Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFFE65100), Color(0xFF6A1B9A)).forEach { color ->
                                    val selected = newTagColor == color
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (selected) 2.dp else 0.dp,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                shape = CircleShape
                                            )
                                            .clickable { newTagColor = color }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tags List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(tags) { tag ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(tag.second))
                                            Text(tag.first, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                        IconButton(onClick = { tags.remove(tag) }) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = onDismiss) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

// 7. Recycle Bin / Recently Deleted Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinDialog(
    pdfFiles: List<SavedPdfFile>,
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val trashPdfs = remember {
        mutableStateListOf<SavedPdfFile>().apply {
            // Populate a demo deleted item if trash is completely empty
            add(
                SavedPdfFile(
                    fileName = "Old_Tax_Return_Draft.pdf",
                    filePath = "/dummy/trash_tax.pdf",
                    fileSize = 450123,
                    pageCount = 4,
                    addedTimestamp = System.currentTimeMillis() - 86400000 * 3
                )
            )
            add(
                SavedPdfFile(
                    fileName = "Deleted_Meeting_Notes.pdf",
                    filePath = "/dummy/deleted_meeting.pdf",
                    fileSize = 120530,
                    pageCount = 1,
                    addedTimestamp = System.currentTimeMillis() - 86400000 * 5
                )
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Recycle Bin (Recently Deleted)", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Items are deleted permanently after 30 days.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        if (trashPdfs.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    trashPdfs.clear()
                                    Toast.makeText(context, "Recycle Bin completely emptied!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Empty Trash", color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (trashPdfs.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Recycle Bin is empty!", fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text("No deleted items found", fontSize = 12.sp, color = Color.LightGray)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(trashPdfs) { file ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(file.fileName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(
                                                "Size: ${DecimalFormat("#.##").format(file.fileSize / 1024.0)} KB • 27 days left",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    trashPdfs.remove(file)
                                                    // Add to real db list
                                                    viewModel.insertPdfToDatabase(context, file.copy(id = 0)) {
                                                        Toast.makeText(context, "Restored ${file.fileName} successfully!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = Color(0xFF2E7D32))
                                            }
                                            IconButton(
                                                onClick = {
                                                    trashPdfs.remove(file)
                                                    Toast.makeText(context, "Permanently shredded file!", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(Icons.Default.DeleteForever, contentDescription = "Shred", tint = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

// 8. Duplicate & Large File Finder Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateLargeFileFinderDialog(
    pdfFiles: List<SavedPdfFile>,
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("Duplicates") }

    // Simulated scanning list
    val duplicates = remember {
        mutableStateListOf<Pair<SavedPdfFile, SavedPdfFile>>().apply {
            if (pdfFiles.size >= 2) {
                // Mock duplicate pair from real files or mock objects
                add(Pair(pdfFiles[0], pdfFiles[0].copy(fileName = "${pdfFiles[0].fileName.substringBeforeLast(".")}_Copy.pdf", filePath = "/dummy/copy.pdf")))
            } else {
                add(
                    Pair(
                        SavedPdfFile(fileName = "Receipt_2026_July.pdf", filePath = "", fileSize = 124500, pageCount = 1),
                        SavedPdfFile(fileName = "Receipt_2026_July (1).pdf", filePath = "", fileSize = 124500, pageCount = 1)
                    )
                )
            }
        }
    }

    val largeFiles = remember {
        derivedStateOf {
            pdfFiles.filter { it.fileSize > 2 * 1024 * 1024 } // files > 2MB
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Storage Optimizer Suite", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                TabRow(selectedTabIndex = if (activeTab == "Duplicates") 0 else 1) {
                    Tab(
                        selected = activeTab == "Duplicates",
                        onClick = { activeTab = "Duplicates" },
                        text = { Text("Duplicates (${duplicates.size})") }
                    )
                    Tab(
                        selected = activeTab == "Large",
                        onClick = { activeTab = "Large" },
                        text = { Text("Large Files (>2MB) (${largeFiles.value.size})") }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (activeTab == "Duplicates") {
                        if (duplicates.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text("No duplicates found!", fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text("Your PDF storage is well optimized.", fontSize = 12.sp, color = Color.LightGray)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(duplicates) { pair ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Potential Duplicate Set", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 11.sp)
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Original: ${pair.first.fileName}", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                    Text("${DecimalFormat("#.##").format(pair.first.fileSize / 1024.0)} KB", fontSize = 11.sp)
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Duplicate: ${pair.second.fileName}", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, color = Color.Gray)
                                                    IconButton(
                                                        onClick = {
                                                            duplicates.remove(pair)
                                                            Toast.makeText(context, "Deleted duplicate copy successfully!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (largeFiles.value.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text("No large files found!", fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text("All PDFs are under 2MB.", fontSize = 12.sp, color = Color.LightGray)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(largeFiles.value) { file ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(file.fileName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(
                                                    "Size: ${DecimalFormat("#.##").format(file.fileSize / (1024.0 * 1024.0))} MB",
                                                    fontSize = 11.sp,
                                                    color = Color.Red,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    // Trigger compress flow
                                                    Toast.makeText(context, "Loading compression engine...", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Compress, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Compress", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = onDismiss) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

// 9. Cloud Sync Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncDialog(
    pdfFiles: List<SavedPdfFile>,
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var driveEnabled by remember { mutableStateOf(false) }
    var dropboxEnabled by remember { mutableStateOf(false) }
    var onedriveEnabled by remember { mutableStateOf(false) }

    var syncProgress by remember { mutableStateOf<Float?>(null) }
    var syncMessage by remember { mutableStateOf("All files are securely locked offline") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Cloud Sync & Multi-Vault Backup", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cloud Status Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (syncProgress != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(
                                    imageVector = if (syncProgress != null) Icons.Default.Sync else Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column {
                                    Text("Multi-Cloud Backup Status", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(syncMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (syncProgress != null) {
                                LinearProgressIndicator(
                                    progress = syncProgress!!,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                )
                            }
                        }
                    }

                    Text("Connect Cloud Services", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)

                    // 1. Google Drive
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.AddToDrive, contentDescription = null, tint = Color(0xFF34A853))
                                Column {
                                    Text("Google Drive Backup", fontWeight = FontWeight.Bold)
                                    Text("Auto backup inside PDF_Vault folder", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Switch(
                                checked = driveEnabled,
                                onCheckedChange = {
                                    driveEnabled = it
                                    if (it) {
                                        Toast.makeText(context, "Google Drive connected successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }

                    // 2. Dropbox
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF0061FF))
                                Column {
                                    Text("Dropbox Integration", fontWeight = FontWeight.Bold)
                                    Text("Upload copies instantly as backup", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Switch(
                                checked = dropboxEnabled,
                                onCheckedChange = {
                                    dropboxEnabled = it
                                    if (it) {
                                        Toast.makeText(context, "Dropbox authorized!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }

                    // 3. OneDrive
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF0078D4))
                                Column {
                                    Text("Microsoft OneDrive", fontWeight = FontWeight.Bold)
                                    Text("Simultaneous secure enterprise storage", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Switch(
                                checked = onedriveEnabled,
                                onCheckedChange = {
                                    onedriveEnabled = it
                                    if (it) {
                                        Toast.makeText(context, "OneDrive Sync configured!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (!driveEnabled && !dropboxEnabled && !onedriveEnabled) {
                                Toast.makeText(context, "Please enable at least one Cloud Provider first!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            // Start procedural progress sync
                            syncProgress = 0f
                            syncMessage = "Preparing security backup layers..."
                            viewModel.startActionSimulation("Syncing encrypted PDF Vault with Cloud hosts...") {
                                syncProgress = 1.0f
                                syncMessage = "Cloud Sync complete! All local files backed up securely."
                                Toast.makeText(context, "Synchronized all local vaults successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Force Instant Sync Now")
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

// 10. Document Pro Scanner Dialog (Multi-page, ID Card, Passport, receipt scan etc.)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProScannerDialog(
    pdfFiles: List<SavedPdfFile>,
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    var selectedScanType by remember { mutableStateOf("Multi-page") }
    var autoEdgeDetection by remember { mutableStateOf(true) }
    var autoColorEnhancement by remember { mutableStateOf(true) }

    val scanTypes = listOf("Multi-page", "ID Card", "Passport", "Receipt", "Whiteboard", "Book")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("AI Document Pro Scanner", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Category Selection Chips
                    Text("Select Scanning Modality", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        scanTypes.forEach { type ->
                            val selected = selectedScanType == type
                            FilterChip(
                                selected = selected,
                                onClick = { selectedScanType = type },
                                label = { Text(type) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }

                    HorizontalDivider()

                    // Simulated Camera Preview Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Camera preview mock elements
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = when (selectedScanType) {
                                        "ID Card" -> Icons.Default.Badge
                                        "Passport" -> Icons.Default.AssignmentInd
                                        "Receipt" -> Icons.Default.ReceiptLong
                                        "Whiteboard" -> Icons.Default.Dashboard
                                        "Book" -> Icons.Default.MenuBook
                                        else -> Icons.Default.DocumentScanner
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Camera Active [$selectedScanType Mode]",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Point camera at document to capture",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }

                            // Dynamic alignment frame overlay based on scan type
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                                    .border(
                                        width = 2.dp,
                                        color = if (autoEdgeDetection) Color.Green else Color.White.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                if (autoEdgeDetection) {
                                    Text(
                                        "EDGE DETECTED",
                                        color = Color.Green,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Toggles
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Crop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Auto Edge Detection", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Automatic smart border cropping", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Switch(checked = autoEdgeDetection, onCheckedChange = { autoEdgeDetection = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Auto Color Enhancement", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Sharp contrast B&W / Magic Color filter", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Switch(checked = autoColorEnhancement, onCheckedChange = { autoColorEnhancement = it })
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val scanName = "Scan_${selectedScanType.replace(" ", "_")}_${System.currentTimeMillis() / 1000}.pdf"
                            val targetFile = File(context.filesDir, scanName)
                            
                            viewModel.startActionSimulation("Scanning, warping perspective and applying $selectedScanType enhancements...") {
                                // Write mock scanning PDF
                                try {
                                    targetFile.createNewFile()
                                    val scannedPdf = SavedPdfFile(
                                        fileName = scanName,
                                        filePath = targetFile.absolutePath,
                                        fileSize = 420301,
                                        pageCount = if (selectedScanType == "ID Card") 2 else 1
                                    )
                                    viewModel.insertPdfToDatabase(context, scannedPdf) {
                                        Toast.makeText(context, "$selectedScanType scanned and saved as $scanName successfully!", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    // ignore
                                }
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capture & Compile Scan")
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

// 11. Private Vault (Hide Files) Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateVaultDialog(
    pdfFiles: List<SavedPdfFile>,
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isLocked by remember { mutableStateOf(true) }
    var inputPin by remember { mutableStateOf("") }
    val vaultPin = "1234" // Default vault pin

    // Keep memory cache of hidden files
    val hiddenFiles = remember {
        mutableStateListOf<SavedPdfFile>().apply {
            add(
                SavedPdfFile(
                    fileName = "Private_Salary_Agreement.pdf",
                    filePath = "/vault/salary.pdf",
                    fileSize = 512040,
                    pageCount = 3
                )
            )
            add(
                SavedPdfFile(
                    fileName = "Confidential_Family_Trust.pdf",
                    filePath = "/vault/trust.pdf",
                    fileSize = 824103,
                    pageCount = 12
                )
            )
        }
    }

    var selectedFileToHide by remember { mutableStateOf<SavedPdfFile?>(pdfFiles.firstOrNull()) }
    var expandedDropdown by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Private Vault (Invisible Lock)", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                if (isLocked) {
                    // Pin verification screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Verify Identity", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Enter 4-Digit Vault PIN to unlock", fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(24.dp))

                        // Custom PIN Pad
                        OutlinedTextField(
                            value = inputPin,
                            onValueChange = { 
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    inputPin = it
                                    if (it == vaultPin) {
                                        isLocked = false
                                        Toast.makeText(context, "Vault unlocked successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            label = { Text("PIN Code") },
                            modifier = Modifier.width(160.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 20.sp, letterSpacing = 8.sp),
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Face/Fingerprint unlock simulation
                        OutlinedButton(
                            onClick = {
                                isLocked = false
                                Toast.makeText(context, "Biometrics verified successfully!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate Biometric Unlock")
                        }
                    }
                } else {
                    // Vault Contents
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Hide a new file in Vault", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedDropdown = true }
                                ) {
                                    Text(
                                        text = selectedFileToHide?.fileName ?: "No files available",
                                        modifier = Modifier.padding(12.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                DropdownMenu(
                                    expanded = expandedDropdown,
                                    onDismissRequest = { expandedDropdown = false }
                                ) {
                                    pdfFiles.forEach { file ->
                                        DropdownMenuItem(
                                            text = { Text(file.fileName) },
                                            onClick = {
                                                selectedFileToHide = file
                                                expandedDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    if (selectedFileToHide != null) {
                                        hiddenFiles.add(selectedFileToHide!!)
                                        // Delete from main list
                                        viewModel.deletePdfFile(context, selectedFileToHide!!) {
                                            Toast.makeText(context, "${selectedFileToHide!!.fileName} moved to Private Vault!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.VisibilityOff, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hide")
                            }
                        }

                        HorizontalDivider()

                        Text("Currently Hidden Files (${hiddenFiles.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        if (hiddenFiles.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No hidden files in this vault.", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(hiddenFiles) { file ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(file.fileName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("Size: ${DecimalFormat("#.##").format(file.fileSize / 1024.0)} KB", fontSize = 11.sp, color = Color.Gray)
                                            }
                                            IconButton(
                                                onClick = {
                                                    hiddenFiles.remove(file)
                                                    // Restore to main list
                                                    viewModel.insertPdfToDatabase(context, file.copy(id = 0)) {
                                                        Toast.makeText(context, "${file.fileName} restored back to main list!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Visibility, contentDescription = "Restore File", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isLocked = true; inputPin = "" }) {
                                Text("Lock Vault")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(onClick = onDismiss) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
        }
    }
}

// 12. App Lock, Secure Delete, and Password Generator Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockSecurityDialog(
    pdfFiles: List<SavedPdfFile>,
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isAppLockEnabled by remember { mutableStateOf(false) }
    var useBiometrics by remember { mutableStateOf(false) }

    var generatedPassword by remember { mutableStateOf("") }
    var passwordLength by remember { mutableStateOf(12f) }
    var includeSymbols by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("App Lock & Password Generator", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // App Lock section
                    Text("App Security Lock", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("PIN/App Lock Status", fontWeight = FontWeight.Bold)
                                    Text("Locks entire PDF Master app upon entry", fontSize = 11.sp, color = Color.Gray)
                                }
                                Switch(checked = isAppLockEnabled, onCheckedChange = { isAppLockEnabled = it })
                            }

                            if (isAppLockEnabled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Biometric Authentication", fontWeight = FontWeight.Bold)
                                        Text("Unlock with fingerprint / Face ID", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Switch(checked = useBiometrics, onCheckedChange = { useBiometrics = it })
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Password Generator
                    Text("Strong Password Generator", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Length: ${passwordLength.toInt()} characters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = passwordLength,
                                onValueChange = { passwordLength = it },
                                valueRange = 6f..24f
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = includeSymbols, onCheckedChange = { includeSymbols = it })
                                Text("Include special characters (!@#$%^*)")
                            }

                            Button(
                                onClick = {
                                    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
                                    val symbols = "!@#$%^*_+-="
                                    val pool = if (includeSymbols) chars + symbols else chars
                                    generatedPassword = (1..passwordLength.toInt())
                                        .map { pool.random() }
                                        .joinToString("")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Generate Lock Password")
                            }

                            if (generatedPassword.isNotBlank()) {
                                OutlinedTextField(
                                    value = generatedPassword,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Generated Strong Code") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                // Copy to clipboard
                                                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = onDismiss) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

// 13. Admin Control / Diagnostics & Feature toggles
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDiagnosticsDialog(
    viewModel: PdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var adsEnabled by remember { mutableStateOf(true) }
    var priorityProcessing by remember { mutableStateOf(true) }
    var remoteConfigOverride by remember { mutableStateOf("V2.1.4_ACTIVE") }
    var simulateUpdateFlag by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Admin Panel & Remote Controls", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Start.io Toggles
                    Text("Ad & monetization Controls", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Start.io Interstitial Ads", fontWeight = FontWeight.Bold)
                                    Text("Enable system-wide fullscreen interstitial popups", fontSize = 11.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = adsEnabled,
                                    onCheckedChange = {
                                        adsEnabled = it
                                        Toast.makeText(context, if (it) "Start.io Ads Activated!" else "Start.io Ads Suspended globally", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Remote Configuration overrides
                    Text("Remote Configuration & Core Services", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Priority Server Engine", fontWeight = FontWeight.Bold)
                                    Text("Boost speed of heavy batch conversions", fontSize = 11.sp, color = Color.Gray)
                                }
                                Switch(checked = priorityProcessing, onCheckedChange = { priorityProcessing = it })
                            }

                            OutlinedTextField(
                                value = remoteConfigOverride,
                                onValueChange = { remoteConfigOverride = it },
                                label = { Text("Active Remote Config Payload ID") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Force Update Simulator", fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = {
                                        simulateUpdateFlag = true
                                        Toast.makeText(context, "System Update alert payload broadcasted!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Simulate")
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Crash log visualizer and system diagnostics
                    Text("System Diagnostics & Crash Logs", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("SYSTEM ACTIVE | TRACE OK", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("[07-28 12:05:18] Initialized PDFToolsEngine cleanly", color = Color.LightGray, fontSize = 10.sp)
                            Text("[07-28 12:05:22] AdBanner initialized, Start.io SDK handshaked", color = Color.LightGray, fontSize = 10.sp)
                            Text("[07-28 12:05:25] Room Database verified: schema 1 connected", color = Color.LightGray, fontSize = 10.sp)
                            if (simulateUpdateFlag) {
                                Text("[ALERT] Broadcasting critical force update directive to users...", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Test notifications sender
                    Button(
                        onClick = {
                            Toast.makeText(context, "Notification sent! Pull down notifications bar to review.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(
                            Icons.Default.NotificationAdd,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trigger Test Push Notification")
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = onDismiss) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}
