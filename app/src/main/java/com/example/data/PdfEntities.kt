package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_pdfs")
data class SavedPdfFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val pageCount: Int,
    val isFavorite: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val lastOpenedTimestamp: Long = System.currentTimeMillis(),
    val hasPassword: Boolean = false,
    val passwordHint: String = ""
)

@Entity(tableName = "operation_logs")
data class PdfOperationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val operationType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0,
    val isSuccessful: Boolean = true
)
