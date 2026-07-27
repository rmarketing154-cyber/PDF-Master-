package com.example.data

import kotlinx.coroutines.flow.Flow

class PdfRepository(private val pdfDao: PdfDao) {
    val allPdfs: Flow<List<SavedPdfFile>> = pdfDao.getAllPdfs()
    val favoritePdfs: Flow<List<SavedPdfFile>> = pdfDao.getFavoritePdfs()
    val recentPdfs: Flow<List<SavedPdfFile>> = pdfDao.getRecentPdfs()

    val successfulOperationsCount: Flow<Int> = pdfDao.getSuccessfulOperationsCount()
    val totalPdfsCount: Flow<Int> = pdfDao.getTotalPdfsCount()
    val totalStorageUsed: Flow<Long?> = pdfDao.getTotalStorageUsed()

    suspend fun getPdfById(id: Int): SavedPdfFile? = pdfDao.getPdfById(id)

    suspend fun insertPdf(pdf: SavedPdfFile): Long = pdfDao.insertPdf(pdf)

    suspend fun updatePdf(pdf: SavedPdfFile) = pdfDao.updatePdf(pdf)

    suspend fun deletePdf(pdf: SavedPdfFile) = pdfDao.deletePdf(pdf)

    suspend fun deletePdfById(id: Int) = pdfDao.deletePdfById(id)

    suspend fun updateFavorite(id: Int, isFav: Boolean) = pdfDao.updateFavorite(id, isFav)

    suspend fun updateLastOpened(id: Int, timestamp: Long) = pdfDao.updateLastOpened(id, timestamp)

    suspend fun renamePdf(id: Int, newName: String) = pdfDao.renamePdf(id, newName)

    suspend fun logOperation(operationType: String, durationMs: Long, isSuccessful: Boolean) {
        pdfDao.insertOperationLog(
            PdfOperationLog(
                operationType = operationType,
                durationMs = durationMs,
                isSuccessful = isSuccessful
            )
        )
    }
}
