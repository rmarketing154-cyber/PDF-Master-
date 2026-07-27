package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ads.StartIoAdsManager
import com.example.data.PdfRepository
import com.example.data.SavedPdfFile
import com.example.pdf.PdfToolsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PdfViewModel(private val repository: PdfRepository) : ViewModel() {
    private val TAG = "PdfViewModel"

    // DB flows
    val allPdfs: StateFlow<List<SavedPdfFile>> = repository.allPdfs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritePdfs: StateFlow<List<SavedPdfFile>> = repository.favoritePdfs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentPdfs: StateFlow<List<SavedPdfFile>> = repository.recentPdfs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statistics flows
    val successfulOperationsCount: StateFlow<Int> = repository.successfulOperationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPdfsCount: StateFlow<Int> = repository.totalPdfsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalStorageUsed: StateFlow<Long> = repository.totalStorageUsed
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // UI Progress States
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingMessage = MutableStateFlow("")
    val processingMessage: StateFlow<String> = _processingMessage.asStateFlow()

    private val _processingProgress = MutableStateFlow<Float?>(null)
    val processingProgress: StateFlow<Float?> = _processingProgress.asStateFlow()

    // Premium states (Rewarded Ads)
    private val _isPremiumUnlocked = MutableStateFlow(false)
    val isPremiumUnlocked: StateFlow<Boolean> = _isPremiumUnlocked.asStateFlow()

    // New results and error states for success review and dialogs
    private val _lastProcessedFile = MutableStateFlow<SavedPdfFile?>(null)
    val lastProcessedFile: StateFlow<SavedPdfFile?> = _lastProcessedFile.asStateFlow()

    private val _operationError = MutableStateFlow<String?>(null)
    val operationError: StateFlow<String?> = _operationError.asStateFlow()

    fun clearLastProcessedFile() {
        _lastProcessedFile.value = null
    }

    fun clearOperationError() {
        _operationError.value = null
    }

    fun cancelCurrentOperation() {
        progressJob?.cancel()
        _isProcessing.value = false
        _processingProgress.value = null
        _processingMessage.value = ""
    }

    private var progressJob: kotlinx.coroutines.Job? = null

    private fun startProgressSimulation(initialMessage: String) {
        progressJob?.cancel()
        _isProcessing.value = true
        _processingMessage.value = initialMessage
        _processingProgress.value = 0.0f
        _lastProcessedFile.value = null
        _operationError.value = null
        
        progressJob = viewModelScope.launch {
            val stages = listOf(
                "Initializing processing engine...",
                "Reading source PDF structure...",
                "Analyzing document layers...",
                "Performing core transforms...",
                "Rebuilding page layouts...",
                "Compressing graphical assets...",
                "Optimizing for offline viewer...",
                "Saving final output to vault..."
            )
            var currentProgress = 0.0f
            while (currentProgress < 0.95f) {
                kotlinx.coroutines.delay(100)
                currentProgress += (0.01f + (Math.random() * 0.03f).toFloat())
                if (currentProgress > 0.95f) currentProgress = 0.95f
                _processingProgress.value = currentProgress
                
                val stageIndex = ((currentProgress * stages.size).toInt()).coerceIn(0, stages.size - 1)
                _processingMessage.value = "${stages[stageIndex]} (${(currentProgress * 100).toInt()}%)"
            }
        }
    }

    private fun completeProgressSimulation(successMessage: String, resultFile: SavedPdfFile? = null) {
        progressJob?.cancel()
        _processingProgress.value = 1.0f
        _processingMessage.value = successMessage
        if (resultFile != null) {
            _lastProcessedFile.value = resultFile
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            _isProcessing.value = false
            _processingProgress.value = null
        }
    }

    private fun failProgressSimulation(errorMessage: String) {
        progressJob?.cancel()
        _isProcessing.value = false
        _processingProgress.value = null
        _operationError.value = errorMessage
    }

    // Ad status helper
    fun isInterstitialLoaded(): Boolean = StartIoAdsManager.isInterstitialLoaded
    fun isRewardedLoaded(): Boolean = StartIoAdsManager.isRewardedLoaded

    fun unlockPremiumViaAd(activity: Activity, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        if (_isPremiumUnlocked.value) {
            onSuccess()
            return
        }
        StartIoAdsManager.showRewardedAd(
            activity,
            onRewarded = {
                _isPremiumUnlocked.value = true
                onSuccess()
            },
            onFailed = {
                onFailure("Could not load video ad. Please try again later.")
            }
        )
    }

    private fun triggerInterstitial(activity: Activity) {
        viewModelScope.launch {
            // Give brief delay so user can see completion first, then show ad
            withContext(Dispatchers.Main) {
                StartIoAdsManager.showInterstitialAd(activity) {
                    Log.d(TAG, "Interstitial ad dismissed/completed")
                }
            }
        }
    }

    // Core PDF Operations

    fun importPdfFile(context: Context, uri: Uri, nameOverride: String? = null, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            startProgressSimulation("Importing PDF from storage...")
            var success = false
            val startTime = System.currentTimeMillis()
            var savedFile: SavedPdfFile? = null
            try {
                withContext(Dispatchers.IO) {
                    val originalName = nameOverride ?: getFileNameFromUri(context, uri) ?: "imported_file.pdf"
                    val sanitizedName = if (originalName.endsWith(".pdf", ignoreCase = true)) originalName else "$originalName.pdf"
                    
                    // Copy to sandbox
                    val copiedFile = PdfToolsEngine.copyFileFromUri(context, uri, sanitizedName)
                    if (copiedFile != null && copiedFile.exists()) {
                        val pageCount = PdfToolsEngine.getPageCount(context, copiedFile)
                        val size = copiedFile.length()

                        val newFile = SavedPdfFile(
                            fileName = sanitizedName,
                            filePath = copiedFile.absolutePath,
                            fileSize = size,
                            pageCount = pageCount
                        )
                        val insertedId = repository.insertPdf(newFile)
                        savedFile = newFile.copy(id = insertedId.toInt())
                        repository.logOperation("Import", System.currentTimeMillis() - startTime, true)
                        success = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing file", e)
                repository.logOperation("Import", System.currentTimeMillis() - startTime, false)
            } finally {
                if (success && savedFile != null) {
                    com.example.ui.components.NotificationHelper.sendNotification(
                        context,
                        "PDF Imported Successfully",
                        "File \"${savedFile?.fileName}\" is added to your local library."
                    )
                    completeProgressSimulation("Import Completed!", savedFile)
                } else {
                    failProgressSimulation("Failed to import PDF file. Ensure the file is not corrupted.")
                }
                onComplete(success)
            }
        }
    }

    fun performMerge(context: Context, files: List<SavedPdfFile>, outName: String, activity: Activity, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            startProgressSimulation("Merging ${files.size} PDF files into $outName...")
            val startTime = System.currentTimeMillis()
            var success = false
            var savedFile: SavedPdfFile? = null
            try {
                withContext(Dispatchers.IO) {
                    val inputFiles = files.map { File(it.filePath) }.filter { it.exists() }
                    if (inputFiles.isNotEmpty()) {
                        val merged = PdfToolsEngine.mergePdfs(context, inputFiles, outName)
                        val pages = PdfToolsEngine.getPageCount(context, merged)
                        
                        val newFile = SavedPdfFile(
                            fileName = outName,
                            filePath = merged.absolutePath,
                            fileSize = merged.length(),
                            pageCount = pages
                        )
                        val insertedId = repository.insertPdf(newFile)
                        savedFile = newFile.copy(id = insertedId.toInt())
                        repository.logOperation("Merge", System.currentTimeMillis() - startTime, true)
                        success = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Merge error", e)
                repository.logOperation("Merge", System.currentTimeMillis() - startTime, false)
            } finally {
                if (success && savedFile != null) {
                    com.example.ui.components.NotificationHelper.sendNotification(
                        context,
                        "PDF Files Merged Successfully",
                        "Your merged PDF \"$outName\" is ready!"
                    )
                    completeProgressSimulation("Merge completed successfully!", savedFile)
                    onComplete(success)
                    triggerInterstitial(activity)
                } else {
                    failProgressSimulation("Failed to merge PDFs. Verify that files are valid and decrypted.")
                    onComplete(success)
                }
            }
        }
    }

    fun performSplit(context: Context, file: SavedPdfFile, splitPoints: List<Int>, activity: Activity, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            startProgressSimulation("Splitting document \"${file.fileName}\" into parts...")
            val startTime = System.currentTimeMillis()
            var success = false
            var lastCreatedFile: SavedPdfFile? = null
            try {
                withContext(Dispatchers.IO) {
                    val source = File(file.filePath)
                    if (source.exists()) {
                        val results = PdfToolsEngine.splitPdf(context, source, splitPoints)
                        results.forEachIndexed { idx, splitFile ->
                            val pCount = PdfToolsEngine.getPageCount(context, splitFile)
                            val newFile = SavedPdfFile(
                                fileName = splitFile.name,
                                filePath = splitFile.absolutePath,
                                fileSize = splitFile.length(),
                                pageCount = pCount
                            )
                            val insertedId = repository.insertPdf(newFile)
                            if (idx == results.size - 1 || lastCreatedFile == null) {
                                lastCreatedFile = newFile.copy(id = insertedId.toInt())
                            }
                        }
                        repository.logOperation("Split", System.currentTimeMillis() - startTime, true)
                        success = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Split error", e)
                repository.logOperation("Split", System.currentTimeMillis() - startTime, false)
            } finally {
                if (success && lastCreatedFile != null) {
                    com.example.ui.components.NotificationHelper.sendNotification(
                        context,
                        "PDF Splitting Successful",
                        "File has been split successfully. Part files are saved in your local vault."
                    )
                    completeProgressSimulation("Splitting finished successfully!", lastCreatedFile)
                    onComplete(success)
                    triggerInterstitial(activity)
                } else {
                    failProgressSimulation("Failed to split PDF document. Check if file is encrypted.")
                    onComplete(success)
                }
            }
        }
    }

    fun performCompress(
        context: Context,
        file: SavedPdfFile,
        outName: String,
        quality: Int,
        scale: Float,
        activity: Activity,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            startProgressSimulation("Compressing PDF images and assets...")
            val startTime = System.currentTimeMillis()
            var success = false
            var savedFile: SavedPdfFile? = null
            try {
                withContext(Dispatchers.IO) {
                    val source = File(file.filePath)
                    if (source.exists()) {
                        val compressed = PdfToolsEngine.compressPdf(context, source, quality, scale, outName)
                        val pCount = PdfToolsEngine.getPageCount(context, compressed)
                        val newFile = SavedPdfFile(
                            fileName = outName,
                            filePath = compressed.absolutePath,
                            fileSize = compressed.length(),
                            pageCount = pCount
                        )
                        val insertedId = repository.insertPdf(newFile)
                        savedFile = newFile.copy(id = insertedId.toInt())
                        repository.logOperation("Compress", System.currentTimeMillis() - startTime, true)
                        success = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Compress error", e)
                repository.logOperation("Compress", System.currentTimeMillis() - startTime, false)
            } finally {
                if (success && savedFile != null) {
                    com.example.ui.components.NotificationHelper.sendNotification(
                        context,
                        "PDF Compression Successful",
                        "Compressed file \"$outName\" created! Size reduced by up to ${100 - quality}%."
                    )
                    completeProgressSimulation("Compression finished!", savedFile)
                    onComplete(success)
                    triggerInterstitial(activity)
                } else {
                    failProgressSimulation("Failed to compress PDF file.")
                    onComplete(success)
                }
            }
        }
    }

    fun performImageToPdf(context: Context, uris: List<Uri>, outName: String, activity: Activity, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            startProgressSimulation("Converting chosen images to high-quality PDF...")
            val startTime = System.currentTimeMillis()
            var success = false
            var savedFile: SavedPdfFile? = null
            try {
                withContext(Dispatchers.IO) {
                    val pdf = PdfToolsEngine.imageToPdf(context, uris, outName)
                    val pCount = PdfToolsEngine.getPageCount(context, pdf)
                    val newFile = SavedPdfFile(
                        fileName = outName,
                        filePath = pdf.absolutePath,
                        fileSize = pdf.length(),
                        pageCount = pCount
                    )
                    val insertedId = repository.insertPdf(newFile)
                    savedFile = newFile.copy(id = insertedId.toInt())
                    repository.logOperation("ImageToPdf", System.currentTimeMillis() - startTime, true)
                    success = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "ImageToPdf error", e)
                repository.logOperation("ImageToPdf", System.currentTimeMillis() - startTime, false)
            } finally {
                if (success && savedFile != null) {
                    com.example.ui.components.NotificationHelper.sendNotification(
                        context,
                        "Images Converted to PDF",
                        "New PDF \"$outName\" created successfully from images!"
                    )
                    completeProgressSimulation("Images converted successfully!", savedFile)
                    onComplete(success)
                    triggerInterstitial(activity)
                } else {
                    failProgressSimulation("Failed to convert images to PDF.")
                    onComplete(success)
                }
            }
        }
    }

    fun performPdfToImages(context: Context, file: SavedPdfFile, activity: Activity, onComplete: (Boolean, List<File>?) -> Unit) {
        viewModelScope.launch {
            startProgressSimulation("Extracting all pages from \"${file.fileName}\" as JPEG images...")
            val startTime = System.currentTimeMillis()
            var resultList: List<File>? = null
            try {
                withContext(Dispatchers.IO) {
                    val source = File(file.filePath)
                    if (source.exists()) {
                        val dirName = "render_${file.id}_${System.currentTimeMillis()}"
                        val images = PdfToolsEngine.pdfToImages(context, source, dirName)
                        repository.logOperation("PdfToImage", System.currentTimeMillis() - startTime, true)
                        resultList = images
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "PdfToImage error", e)
                repository.logOperation("PdfToImage", System.currentTimeMillis() - startTime, false)
            } finally {
                if (resultList != null && resultList!!.isNotEmpty()) {
                    com.example.ui.components.NotificationHelper.sendNotification(
                        context,
                        "PDF Pages Extracted as Images",
                        "Successfully extracted ${resultList!!.size} pages from \"${file.fileName}\"."
                    )
                    // Set a dummy SavedPdfFile pointing to the parent folder/first image to show success preview
                    val dummyFile = SavedPdfFile(
                        fileName = "Extracted Pages (${resultList!!.size} Images)",
                        filePath = resultList!!.first().absolutePath,
                        fileSize = resultList!!.sumOf { it.length() },
                        pageCount = resultList!!.size
                    )
                    completeProgressSimulation("Pages extracted successfully!", dummyFile)
                    onComplete(true, resultList)
                    triggerInterstitial(activity)
                } else {
                    failProgressSimulation("Failed to render PDF pages as images.")
                    onComplete(false, null)
                }
            }
        }
    }

    fun performWatermark(
        context: Context,
        file: SavedPdfFile,
        text: String,
        color: Int,
        size: Float,
        alpha: Int,
        angle: Float,
        outName: String,
        activity: Activity,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            startProgressSimulation("Applying custom watermark overlay to all pages...")
            val startTime = System.currentTimeMillis()
            var success = false
            var savedFile: SavedPdfFile? = null
            try {
                withContext(Dispatchers.IO) {
                    val source = File(file.filePath)
                    if (source.exists()) {
                        val watermarkFile = PdfToolsEngine.addWatermark(context, source, text, color, size, alpha, angle, outName)
                        val pCount = PdfToolsEngine.getPageCount(context, watermarkFile)
                        val newFile = SavedPdfFile(
                            fileName = outName,
                            filePath = watermarkFile.absolutePath,
                            fileSize = watermarkFile.length(),
                            pageCount = pCount
                        )
                        val insertedId = repository.insertPdf(newFile)
                        savedFile = newFile.copy(id = insertedId.toInt())
                        repository.logOperation("Watermark", System.currentTimeMillis() - startTime, true)
                        success = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Watermark error", e)
                repository.logOperation("Watermark", System.currentTimeMillis() - startTime, false)
            } finally {
                if (success && savedFile != null) {
                    com.example.ui.components.NotificationHelper.sendNotification(
                        context,
                        "Watermark Applied Successfully",
                        "Watermarked file \"$outName\" has been generated!"
                    )
                    completeProgressSimulation("Watermark added successfully!", savedFile)
                    onComplete(success)
                    triggerInterstitial(activity)
                } else {
                    failProgressSimulation("Failed to apply watermark overlay.")
                    onComplete(success)
                }
            }
        }
    }

    fun performRotate(
        context: Context,
        file: SavedPdfFile,
        pageRotations: Map<Int, Float>,
        outName: String,
        activity: Activity,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            startProgressSimulation("Rotating specified pages in document...")
            val startTime = System.currentTimeMillis()
            var success = false
            var savedFile: SavedPdfFile? = null
            try {
                withContext(Dispatchers.IO) {
                    val source = File(file.filePath)
                    if (source.exists()) {
                        val rotated = PdfToolsEngine.rotatePdf(context, source, pageRotations, outName)
                        val pCount = PdfToolsEngine.getPageCount(context, rotated)
                        val newFile = SavedPdfFile(
                            fileName = outName,
                            filePath = rotated.absolutePath,
                            fileSize = rotated.length(),
                            pageCount = pCount
                        )
                        val insertedId = repository.insertPdf(newFile)
                        savedFile = newFile.copy(id = insertedId.toInt())
                        repository.logOperation("Rotate", System.currentTimeMillis() - startTime, true)
                        success = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Rotate error", e)
                repository.logOperation("Rotate", System.currentTimeMillis() - startTime, false)
            } finally {
                if (success && savedFile != null) {
                    com.example.ui.components.NotificationHelper.sendNotification(
                        context,
                        "PDF Pages Rotated Successfully",
                        "Modified PDF file has been saved to your local library as \"$outName\"."
                    )
                    completeProgressSimulation("Pages rotated successfully!", savedFile)
                    onComplete(success)
                    triggerInterstitial(activity)
                } else {
                    failProgressSimulation("Failed to rotate document pages.")
                    onComplete(success)
                }
            }
        }
    }

    fun performDeletePages(
        context: Context,
        file: SavedPdfFile,
        pagesToDelete: Set<Int>,
        outName: String,
        activity: Activity,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            startProgressSimulation("Removing selected pages from PDF document...")
            val startTime = System.currentTimeMillis()
            var success = false
            var savedFile: SavedPdfFile? = null
            try {
                withContext(Dispatchers.IO) {
                    val source = File(file.filePath)
                    if (source.exists()) {
                        val trimmed = PdfToolsEngine.deletePages(context, source, pagesToDelete, outName)
                        val pCount = PdfToolsEngine.getPageCount(context, trimmed)
                        val newFile = SavedPdfFile(
                            fileName = outName,
                            filePath = trimmed.absolutePath,
                            fileSize = trimmed.length(),
                            pageCount = pCount
                        )
                        val insertedId = repository.insertPdf(newFile)
                        savedFile = newFile.copy(id = insertedId.toInt())
                        repository.logOperation("DeletePages", System.currentTimeMillis() - startTime, true)
                        success = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "DeletePages error", e)
                repository.logOperation("DeletePages", System.currentTimeMillis() - startTime, false)
            } finally {
                if (success && savedFile != null) {
                    com.example.ui.components.NotificationHelper.sendNotification(
                        context,
                        "Pages Removed Successfully",
                        "Your output file \"$outName\" is ready in local storage."
                    )
                    completeProgressSimulation("Specified pages removed successfully!", savedFile)
                    onComplete(success)
                    triggerInterstitial(activity)
                } else {
                    failProgressSimulation("Failed to remove pages from PDF file.")
                    onComplete(success)
                }
            }
        }
    }

    fun performExtractPages(
        context: Context,
        file: SavedPdfFile,
        pagesToExtract: List<Int>,
        outName: String,
        activity: Activity,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            startProgressSimulation("Extracting specified pages into new PDF file...")
            val startTime = System.currentTimeMillis()
            var success = false
            var savedFile: SavedPdfFile? = null
            try {
                withContext(Dispatchers.IO) {
                    val source = File(file.filePath)
                    if (source.exists()) {
                        val extracted = PdfToolsEngine.extractPages(context, source, pagesToExtract, outName)
                        val pCount = PdfToolsEngine.getPageCount(context, extracted)
                        val newFile = SavedPdfFile(
                            fileName = outName,
                            filePath = extracted.absolutePath,
                            fileSize = extracted.length(),
                            pageCount = pCount
                        )
                        val insertedId = repository.insertPdf(newFile)
                        savedFile = newFile.copy(id = insertedId.toInt())
                        repository.logOperation("ExtractPages", System.currentTimeMillis() - startTime, true)
                        success = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "ExtractPages error", e)
                repository.logOperation("ExtractPages", System.currentTimeMillis() - startTime, false)
            } finally {
                if (success && savedFile != null) {
                    com.example.ui.components.NotificationHelper.sendNotification(
                        context,
                        "Pages Extracted Successfully",
                        "New file \"$outName\" containing chosen pages is generated!"
                    )
                    completeProgressSimulation("Pages extracted successfully!", savedFile)
                    onComplete(success)
                    triggerInterstitial(activity)
                } else {
                    failProgressSimulation("Failed to extract pages from PDF.")
                    onComplete(success)
                }
            }
        }
    }

    fun performReorderPages(
        context: Context,
        file: SavedPdfFile,
        newOrder: List<Int>,
        outName: String,
        activity: Activity,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            startProgressSimulation("Re-arranging document pages sequence...")
            val startTime = System.currentTimeMillis()
            var success = false
            var savedFile: SavedPdfFile? = null
            try {
                withContext(Dispatchers.IO) {
                    val source = File(file.filePath)
                    if (source.exists()) {
                        val reordered = PdfToolsEngine.reorderPages(context, source, newOrder, outName)
                        val pCount = PdfToolsEngine.getPageCount(context, reordered)
                        val newFile = SavedPdfFile(
                            fileName = outName,
                            filePath = reordered.absolutePath,
                            fileSize = reordered.length(),
                            pageCount = pCount
                        )
                        val insertedId = repository.insertPdf(newFile)
                        savedFile = newFile.copy(id = insertedId.toInt())
                        repository.logOperation("ReorderPages", System.currentTimeMillis() - startTime, true)
                        success = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reorder error", e)
                repository.logOperation("ReorderPages", System.currentTimeMillis() - startTime, false)
            } finally {
                if (success && savedFile != null) {
                    com.example.ui.components.NotificationHelper.sendNotification(
                        context,
                        "PDF Reordered Successfully",
                        "Reordered file \"$outName\" is saved locally!"
                    )
                    completeProgressSimulation("Pages re-sequenced successfully!", savedFile)
                    onComplete(success)
                    triggerInterstitial(activity)
                } else {
                    failProgressSimulation("Failed to reorder PDF pages.")
                    onComplete(success)
                }
            }
        }
    }

    fun protectPdfWithPassword(
        file: SavedPdfFile,
        passwordHint: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val updatedFile = file.copy(hasPassword = true, passwordHint = passwordHint)
                repository.updatePdf(updatedFile)
                _lastProcessedFile.value = updatedFile
                onComplete(true)
            } catch (e: Exception) {
                _operationError.value = "Failed to lock PDF."
                onComplete(false)
            }
        }
    }

    fun removePasswordFromPdf(
        file: SavedPdfFile,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val updatedFile = file.copy(hasPassword = false, passwordHint = "")
                repository.updatePdf(updatedFile)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    // Favorite/Metadata Operations
    fun toggleFavorite(file: SavedPdfFile) {
        viewModelScope.launch {
            repository.updateFavorite(file.id, !file.isFavorite)
        }
    }

    fun updateLastOpened(file: SavedPdfFile) {
        viewModelScope.launch {
            repository.updateLastOpened(file.id, System.currentTimeMillis())
        }
    }

    fun renamePdf(context: Context, file: SavedPdfFile, newName: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val cleanName = if (newName.endsWith(".pdf", ignoreCase = true)) newName else "$newName.pdf"
                val source = File(file.filePath)
                if (source.exists()) {
                    val dest = File(source.parentFile, cleanName)
                    if (source.renameTo(dest)) {
                        val updatedFile = file.copy(fileName = cleanName, filePath = dest.absolutePath)
                        repository.updatePdf(updatedFile)
                        onComplete(true)
                    } else {
                        onComplete(false)
                    }
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun deletePdf(file: SavedPdfFile) {
        viewModelScope.launch {
            try {
                val f = File(file.filePath)
                if (f.exists()) {
                    f.delete()
                }
                repository.deletePdf(file)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting file", e)
            }
        }
    }

    fun clearCache(context: Context) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Clearing Temporary Cache..."
            try {
                withContext(Dispatchers.IO) {
                    val filesDir = context.filesDir
                    // Let's delete subdirectories with prefix "render_" or "temp_"
                    filesDir.listFiles()?.forEach { file ->
                        if (file.isDirectory && (file.name.startsWith("render_") || file.name.startsWith("temp_"))) {
                            file.deleteRecursively()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing cache", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // URI name extraction helper
    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting filename from uri", e)
        }
        return name
    }

    fun performAdvancedWatermark(
        context: Context,
        file: SavedPdfFile,
        isText: Boolean,
        text: String,
        textColor: Int,
        textSize: Float,
        imageUri: Uri?,
        opacity: Float,
        rotation: Float,
        position: String,
        outName: String,
        activity: Activity,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            startProgressSimulation("Applying custom watermark overlay...")
            val startTime = System.currentTimeMillis()
            var success = false
            var savedFile: SavedPdfFile? = null
            try {
                withContext(Dispatchers.IO) {
                    val source = File(file.filePath)
                    if (source.exists()) {
                        val watermarked = PdfToolsEngine.addAdvancedWatermark(
                            context = context,
                            pdfFile = source,
                            isText = isText,
                            text = text,
                            textColor = textColor,
                            textSize = textSize,
                            imageUri = imageUri,
                            opacity = opacity,
                            rotation = rotation,
                            position = position,
                            outputFileName = outName
                        )
                        val pCount = PdfToolsEngine.getPageCount(context, watermarked)
                        val newFile = SavedPdfFile(
                            fileName = outName,
                            filePath = watermarked.absolutePath,
                            fileSize = watermarked.length(),
                            pageCount = pCount
                        )
                        val insertedId = repository.insertPdf(newFile)
                        savedFile = newFile.copy(id = insertedId.toInt())
                        repository.logOperation("AdvancedWatermark", System.currentTimeMillis() - startTime, true)
                        success = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Watermark error", e)
                repository.logOperation("AdvancedWatermark", System.currentTimeMillis() - startTime, false)
            } finally {
                if (success && savedFile != null) {
                    com.example.ui.components.NotificationHelper.sendNotification(
                        context,
                        "Watermark Overlay Applied",
                        "File \"$outName\" was watermarked and saved!"
                    )
                    completeProgressSimulation("Watermarked successfully!", savedFile)
                    onComplete(success)
                    triggerInterstitial(activity)
                } else {
                    failProgressSimulation("Failed to apply watermark overlay.")
                    onComplete(success)
                }
            }
        }
    }

    fun performZipEncrypt(
        context: Context,
        file: SavedPdfFile,
        password: String,
        outName: String,
        activity: Activity,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            startProgressSimulation("Encrypting and locking document with Zip4j AES-256...")
            val startTime = System.currentTimeMillis()
            var success = false
            var savedFile: SavedPdfFile? = null
            try {
                withContext(Dispatchers.IO) {
                    val source = File(file.filePath)
                    if (source.exists()) {
                        val encryptedZip = PdfToolsEngine.encryptPdfWithZip4j(
                            context = context,
                            pdfFile = source,
                            password = password,
                            outName = outName
                        )
                        val newFile = SavedPdfFile(
                            fileName = outName,
                            filePath = encryptedZip.absolutePath,
                            fileSize = encryptedZip.length(),
                            pageCount = file.pageCount,
                            hasPassword = true,
                            passwordHint = "AES-256 Encrypted ZIP"
                        )
                        val insertedId = repository.insertPdf(newFile)
                        savedFile = newFile.copy(id = insertedId.toInt())
                        repository.logOperation("ZipEncrypt", System.currentTimeMillis() - startTime, true)
                        success = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Zip encrypt error", e)
                repository.logOperation("ZipEncrypt", System.currentTimeMillis() - startTime, false)
            } finally {
                if (success && savedFile != null) {
                    com.example.ui.components.NotificationHelper.sendNotification(
                        context,
                        "Document Encrypted",
                        "ZIP archive \"$outName\" is safely encrypted!"
                    )
                    completeProgressSimulation("Locked with AES-256 successfully!", savedFile)
                    onComplete(success)
                    triggerInterstitial(activity)
                } else {
                    failProgressSimulation("Failed to encrypt document.")
                    onComplete(success)
                }
            }
        }
    }

    fun performZipDecrypt(
        context: Context,
        file: SavedPdfFile,
        password: String,
        outName: String,
        activity: Activity,
        onComplete: (Boolean, File?) -> Unit
    ) {
        viewModelScope.launch {
            startProgressSimulation("Extracting and decrypting Zip4j AES-256 secure archive...")
            val startTime = System.currentTimeMillis()
            var success = false
            var savedFile: SavedPdfFile? = null
            var decryptedFile: File? = null
            try {
                withContext(Dispatchers.IO) {
                    val source = File(file.filePath)
                    if (source.exists()) {
                        decryptedFile = PdfToolsEngine.decryptPdfWithZip4j(
                            context = context,
                            zipFile = source,
                            password = password,
                            outName = outName
                        )
                        val pCount = PdfToolsEngine.getPageCount(context, decryptedFile!!)
                        val newFile = SavedPdfFile(
                            fileName = outName,
                            filePath = decryptedFile!!.absolutePath,
                            fileSize = decryptedFile!!.length(),
                            pageCount = pCount
                        )
                        val insertedId = repository.insertPdf(newFile)
                        savedFile = newFile.copy(id = insertedId.toInt())
                        repository.logOperation("ZipDecrypt", System.currentTimeMillis() - startTime, true)
                        success = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Zip decrypt error", e)
                repository.logOperation("ZipDecrypt", System.currentTimeMillis() - startTime, false)
            } finally {
                if (success && savedFile != null) {
                    com.example.ui.components.NotificationHelper.sendNotification(
                        context,
                        "Archive Decrypted Successfully",
                        "Decrypted file \"$outName\" is saved!"
                    )
                    completeProgressSimulation("Archive decrypted successfully!", savedFile)
                    onComplete(success, decryptedFile)
                    triggerInterstitial(activity)
                } else {
                    failProgressSimulation("Incorrect password or corrupt zip.")
                    onComplete(success, null)
                }
            }
        }
    }
}

class PdfViewModelFactory(private val repository: PdfRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PdfViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PdfViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
