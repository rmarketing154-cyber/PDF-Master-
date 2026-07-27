package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProcessingState {
    object Idle : ProcessingState()
    data class Processing(val progress: Float, val status: String) : ProcessingState()
    data class Success(
        val fileName: String,
        val filePath: String,
        val fileSize: Long,
        val pageCount: Int
    ) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}

class PdfProcessingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val uiState: StateFlow<ProcessingState> = _uiState.asStateFlow()

    private var simulationJob: Job? = null

    /**
     * Start a real or simulated processing operation with progress updates.
     */
    fun startProcessing(status: String) {
        simulationJob?.cancel()
        _uiState.value = ProcessingState.Processing(0.0f, status)
    }

    /**
     * Update the progress and status dynamically.
     */
    fun updateProgress(progress: Float, status: String) {
        if (_uiState.value is ProcessingState.Processing) {
            _uiState.value = ProcessingState.Processing(progress.coerceIn(0.0f, 1.0f), status)
        }
    }

    /**
     * Transition smoothly to the success state.
     */
    fun completeProcessing(fileName: String, filePath: String, fileSize: Long, pageCount: Int) {
        simulationJob?.cancel()
        viewModelScope.launch {
            // Guarantee we hit 100% progress for visual completeness before transitioning
            _uiState.value = ProcessingState.Processing(1.0f, "Finalizing output and saving...")
            delay(500)
            _uiState.value = ProcessingState.Success(fileName, filePath, fileSize, pageCount)
        }
    }

    /**
     * Transition to the error state.
     */
    fun failProcessing(errorMessage: String) {
        simulationJob?.cancel()
        _uiState.value = ProcessingState.Error(errorMessage)
    }

    /**
     * Cancel the active processing job and reset to idle.
     */
    fun cancelProcessing() {
        simulationJob?.cancel()
        _uiState.value = ProcessingState.Idle
    }

    /**
     * Reset the view model to Idle.
     */
    fun reset() {
        simulationJob?.cancel()
        _uiState.value = ProcessingState.Idle
    }

    /**
     * Runs a high-fidelity simulation of an operation for demo/offline purposes.
     */
    fun runSimulatedOperation(
        operationName: String,
        outputFileName: String,
        outputFilePath: String,
        fileSize: Long,
        pageCount: Int,
        onComplete: () -> Unit = {}
    ) {
        startProcessing("Initializing $operationName engine...")
        
        simulationJob = viewModelScope.launch {
            val stages = when (operationName.lowercase()) {
                "compress" -> listOf(
                    "Analyzing page structural layers...",
                    "Downsampling high-resolution images...",
                    "Optimizing embedded font assets...",
                    "Compressing metadata catalogs...",
                    "Assembling compressed document stream..."
                )
                "merge" -> listOf(
                    "Parsing multiple PDF inputs...",
                    "Merging page tree structures...",
                    "Consolidating shared font dictionaries...",
                    "Relocating internal outline targets...",
                    "Building merged document package..."
                )
                "split" -> listOf(
                    "Loading primary document structure...",
                    "Extracting selected page indices...",
                    "Decoupling cross-reference tables...",
                    "Re-mapping internal links & markers...",
                    "Saving extracted file chunks..."
                )
                "ocr" -> listOf(
                    "Running optical text analysis...",
                    "Performing perspective deskewing...",
                    "Segmenting image layers to text blocks...",
                    "Recognizing letters with deep OCR...",
                    "Embedding invisible search layer..."
                )
                else -> listOf(
                    "Reading source PDF stream...",
                    "Performing core page transforms...",
                    "Rebuilding layout matrices...",
                    "Optimizing stream resources...",
                    "Saving finalized offline output..."
                )
            }

            var currentProgress = 0.0f
            while (currentProgress < 1.0f) {
                delay(120)
                // Random elegant progress steps
                val increment = 0.02f + (Math.random() * 0.05f).toFloat()
                currentProgress = (currentProgress + increment).coerceAtMost(1.0f)
                
                // Determine stage text
                val stageIndex = ((currentProgress * stages.size).toInt()).coerceIn(0, stages.size - 1)
                val stageText = "${stages[stageIndex]} (${(currentProgress * 100).toInt()}%)"
                
                if (currentProgress < 1.0f) {
                    updateProgress(currentProgress, stageText)
                }
            }
            
            completeProcessing(outputFileName, outputFilePath, fileSize, pageCount)
            onComplete()
        }
    }
}
