package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM saved_pdfs ORDER BY addedTimestamp DESC")
    fun getAllPdfs(): Flow<List<SavedPdfFile>>

    @Query("SELECT * FROM saved_pdfs WHERE isFavorite = 1 ORDER BY addedTimestamp DESC")
    fun getFavoritePdfs(): Flow<List<SavedPdfFile>>

    @Query("SELECT * FROM saved_pdfs ORDER BY lastOpenedTimestamp DESC LIMIT 5")
    fun getRecentPdfs(): Flow<List<SavedPdfFile>>

    @Query("SELECT * FROM saved_pdfs WHERE id = :id")
    suspend fun getPdfById(id: Int): SavedPdfFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdf(pdf: SavedPdfFile): Long

    @Update
    suspend fun updatePdf(pdf: SavedPdfFile)

    @Delete
    suspend fun deletePdf(pdf: SavedPdfFile)

    @Query("DELETE FROM saved_pdfs WHERE id = :id")
    suspend fun deletePdfById(id: Int)

    @Query("UPDATE saved_pdfs SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFav: Boolean)

    @Query("UPDATE saved_pdfs SET lastOpenedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastOpened(id: Int, timestamp: Long)

    @Query("UPDATE saved_pdfs SET fileName = :newName WHERE id = :id")
    suspend fun renamePdf(id: Int, newName: String)

    // Statistics queries
    @Insert
    suspend fun insertOperationLog(log: PdfOperationLog)

    @Query("SELECT COUNT(*) FROM operation_logs WHERE isSuccessful = 1")
    fun getSuccessfulOperationsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM saved_pdfs")
    fun getTotalPdfsCount(): Flow<Int>

    @Query("SELECT SUM(fileSize) FROM saved_pdfs")
    fun getTotalStorageUsed(): Flow<Long?>
}
