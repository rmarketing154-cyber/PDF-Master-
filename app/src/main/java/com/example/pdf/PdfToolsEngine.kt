package com.example.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod

object PdfToolsEngine {
    private const val TAG = "PdfToolsEngine"
    private const val STANDARD_WIDTH = 595 // A4 width in points
    private const val STANDARD_HEIGHT = 842 // A4 height in points

    fun getPageCount(context: Context, file: File): Int {
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (e: Exception) {
            Log.e(TAG, "Error getting page count", e)
            0
        }
    }

    fun imageToPdf(context: Context, imageUris: List<Uri>, outputFileName: String): File {
        val pdfDocument = PdfDocument()
        val dir = context.filesDir
        val outputFile = File(dir, outputFileName)

        try {
            imageUris.forEachIndexed { index, uri ->
                val bitmap = decodeUriToBitmap(context, uri) ?: return@forEachIndexed
                
                // Scale bitmap to fit nicely on A4 or keep aspect ratio
                val scale = Math.min(STANDARD_WIDTH.toFloat() / bitmap.width, STANDARD_HEIGHT.toFloat() / bitmap.height)
                val targetWidth = (bitmap.width * scale).toInt()
                val targetHeight = (bitmap.height * scale).toInt()
                
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                if (scaledBitmap != bitmap) {
                    bitmap.recycle()
                }

                val pageInfo = PdfDocument.PageInfo.Builder(targetWidth, targetHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                
                canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
                scaledBitmap.recycle()
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
        }
        return outputFile
    }

    fun pdfToImages(context: Context, pdfFile: File, outputDirName: String): List<File> {
        val imageFiles = mutableListOf<File>()
        val outputDir = File(context.filesDir, outputDirName)
        if (!outputDir.exists()) outputDir.mkdirs()

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val count = renderer.pageCount

            for (i in 0 until count) {
                val page = renderer.openPage(i)
                
                // Limit size to avoid out of memory, max 1500px height
                val scale = if (page.height > 1500) 1500f / page.height else 1.0f
                val w = (page.width * scale).toInt()
                val h = (page.height * scale).toInt()

                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                // Draw white background
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                val imageFile = File(outputDir, "page_${i + 1}.jpg")
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                
                imageFiles.add(imageFile)
                bitmap.recycle()
                page.close()
            }
        } finally {
            renderer?.close()
            pfd?.close()
        }
        return imageFiles
    }

    fun mergePdfs(context: Context, pdfFiles: List<File>, outputFileName: String): File {
        val pdfDocument = PdfDocument()
        val outputFile = File(context.filesDir, outputFileName)
        var pageIndex = 1

        try {
            pdfFiles.forEach { file ->
                var pfd: ParcelFileDescriptor? = null
                var renderer: PdfRenderer? = null
                try {
                    pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    renderer = PdfRenderer(pfd)
                    val count = renderer.pageCount

                    for (i in 0 until count) {
                        val page = renderer.openPage(i)
                        
                        // Render page to bitmap
                        val scale = if (page.height > 1500) 1500f / page.height else 1.0f
                        val w = (page.width * scale).toInt()
                        val h = (page.height * scale).toInt()

                        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val canvasBg = Canvas(bitmap)
                        canvasBg.drawColor(Color.WHITE)

                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val pageInfo = PdfDocument.PageInfo.Builder(w, h, pageIndex++).create()
                        val newPage = pdfDocument.startPage(pageInfo)
                        newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        pdfDocument.finishPage(newPage)

                        bitmap.recycle()
                        page.close()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error merging file: ${file.name}", e)
                } finally {
                    renderer?.close()
                    pfd?.close()
                }
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
        }
        return outputFile
    }

    fun splitPdf(context: Context, pdfFile: File, splitPoints: List<Int>): List<File> {
        val splitFiles = mutableListOf<File>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount

            // Create splits
            // We group pages, e.g., if totalPages is 6 and splitPoints is [2, 4],
            // groups are: [0..1], [2..3], [4..5]
            val groups = mutableListOf<List<Int>>()
            var currentGroup = mutableListOf<Int>()
            for (i in 0 until totalPages) {
                if (splitPoints.contains(i) && currentGroup.isNotEmpty()) {
                    groups.add(currentGroup)
                    currentGroup = mutableListOf()
                }
                currentGroup.add(i)
            }
            if (currentGroup.isNotEmpty()) {
                groups.add(currentGroup)
            }

            groups.forEachIndexed { groupIdx, pageIndices ->
                val pdfDocument = PdfDocument()
                var newPageIndex = 1
                
                pageIndices.forEach { pageIdx ->
                    val page = renderer.openPage(pageIdx)
                    
                    val scale = if (page.height > 1500) 1500f / page.height else 1.0f
                    val w = (page.width * scale).toInt()
                    val h = (page.height * scale).toInt()

                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvasBg = Canvas(bitmap)
                    canvasBg.drawColor(Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val pageInfo = PdfDocument.PageInfo.Builder(w, h, newPageIndex++).create()
                    val newPage = pdfDocument.startPage(pageInfo)
                    newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(newPage)

                    bitmap.recycle()
                    page.close()
                }

                val splitName = "${pdfFile.nameWithoutExtension}_part_${groupIdx + 1}.pdf"
                val splitFile = File(context.filesDir, splitName)
                FileOutputStream(splitFile).use { out ->
                    pdfDocument.writeTo(out)
                }
                pdfDocument.close()
                splitFiles.add(splitFile)
            }
        } finally {
            renderer?.close()
            pfd?.close()
        }

        return splitFiles
    }

    fun compressPdf(context: Context, pdfFile: File, quality: Int, scaleFactor: Float, outputFileName: String): File {
        val pdfDocument = PdfDocument()
        val outputFile = File(context.filesDir, outputFileName)
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var pageIndex = 1

        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val count = renderer.pageCount

            for (i in 0 until count) {
                val page = renderer.openPage(i)
                
                // Downscale resolution
                val w = (page.width * scaleFactor).toInt()
                val h = (page.height * scaleFactor).toInt()

                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvasBg = Canvas(bitmap)
                canvasBg.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                // Compress using JPEG encoding
                val bos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
                val compressedBytes = bos.toByteArray()
                
                val compressedBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

                val pageInfo = PdfDocument.PageInfo.Builder(w, h, pageIndex++).create()
                val newPage = pdfDocument.startPage(pageInfo)
                newPage.canvas.drawBitmap(compressedBitmap, 0f, 0f, null)
                pdfDocument.finishPage(newPage)

                bitmap.recycle()
                compressedBitmap.recycle()
                page.close()
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            renderer?.close()
            pfd?.close()
            pdfDocument.close()
        }
        return outputFile
    }

    fun addWatermark(
        context: Context,
        pdfFile: File,
        text: String,
        color: Int,
        size: Float,
        alpha: Int,
        angle: Float,
        outputFileName: String
    ): File {
        val pdfDocument = PdfDocument()
        val outputFile = File(context.filesDir, outputFileName)
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var pageIndex = 1

        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val count = renderer.pageCount

            for (i in 0 until count) {
                val page = renderer.openPage(i)
                
                val scale = if (page.height > 1500) 1500f / page.height else 1.0f
                val w = (page.width * scale).toInt()
                val h = (page.height * scale).toInt()

                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvasBg = Canvas(bitmap)
                canvasBg.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val pageInfo = PdfDocument.PageInfo.Builder(w, h, pageIndex++).create()
                val newPage = pdfDocument.startPage(pageInfo)
                val canvas = newPage.canvas
                
                // Draw original page
                canvas.drawBitmap(bitmap, 0f, 0f, null)

                // Configure Watermark Paint
                val paint = Paint().apply {
                    this.color = color
                    this.textSize = size
                    this.alpha = alpha
                    this.isAntiAlias = true
                    this.textAlign = Paint.Align.CENTER
                }

                // Draw rotated text watermark
                canvas.save()
                canvas.rotate(angle, (w / 2).toFloat(), (h / 2).toFloat())
                canvas.drawText(text, (w / 2).toFloat(), (h / 2).toFloat(), paint)
                canvas.restore()

                pdfDocument.finishPage(newPage)
                bitmap.recycle()
                page.close()
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            renderer?.close()
            pfd?.close()
            pdfDocument.close()
        }
        return outputFile
    }

    fun rotatePdf(context: Context, pdfFile: File, pageRotationMap: Map<Int, Float>, outputFileName: String): File {
        val pdfDocument = PdfDocument()
        val outputFile = File(context.filesDir, outputFileName)
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var pageIndex = 1

        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val count = renderer.pageCount

            for (i in 0 until count) {
                val page = renderer.openPage(i)
                
                val scale = if (page.height > 1500) 1500f / page.height else 1.0f
                val w = (page.width * scale).toInt()
                val h = (page.height * scale).toInt()

                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvasBg = Canvas(bitmap)
                canvasBg.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val angle = pageRotationMap[i] ?: 0f
                val finalBitmap = if (angle != 0f) {
                    val matrix = Matrix().apply { postRotate(angle) }
                    val rot = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    if (rot != bitmap) bitmap.recycle()
                    rot
                } else {
                    bitmap
                }

                val pageInfo = PdfDocument.PageInfo.Builder(finalBitmap.width, finalBitmap.height, pageIndex++).create()
                val newPage = pdfDocument.startPage(pageInfo)
                newPage.canvas.drawBitmap(finalBitmap, 0f, 0f, null)
                pdfDocument.finishPage(newPage)

                finalBitmap.recycle()
                page.close()
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            renderer?.close()
            pfd?.close()
            pdfDocument.close()
        }
        return outputFile
    }

    fun deletePages(context: Context, pdfFile: File, pagesToDelete: Set<Int>, outputFileName: String): File {
        val pdfDocument = PdfDocument()
        val outputFile = File(context.filesDir, outputFileName)
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var pageIndex = 1

        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val count = renderer.pageCount

            for (i in 0 until count) {
                if (pagesToDelete.contains(i)) {
                    continue // Skip page
                }
                val page = renderer.openPage(i)
                
                val scale = if (page.height > 1500) 1500f / page.height else 1.0f
                val w = (page.width * scale).toInt()
                val h = (page.height * scale).toInt()

                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvasBg = Canvas(bitmap)
                canvasBg.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val pageInfo = PdfDocument.PageInfo.Builder(w, h, pageIndex++).create()
                val newPage = pdfDocument.startPage(pageInfo)
                newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(newPage)

                bitmap.recycle()
                page.close()
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            renderer?.close()
            pfd?.close()
            pdfDocument.close()
        }
        return outputFile
    }

    fun extractPages(context: Context, pdfFile: File, pagesToExtract: List<Int>, outputFileName: String): File {
        val pdfDocument = PdfDocument()
        val outputFile = File(context.filesDir, outputFileName)
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var pageIndex = 1

        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)

            pagesToExtract.forEach { i ->
                val page = renderer.openPage(i)
                
                val scale = if (page.height > 1500) 1500f / page.height else 1.0f
                val w = (page.width * scale).toInt()
                val h = (page.height * scale).toInt()

                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvasBg = Canvas(bitmap)
                canvasBg.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val pageInfo = PdfDocument.PageInfo.Builder(w, h, pageIndex++).create()
                val newPage = pdfDocument.startPage(pageInfo)
                newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(newPage)

                bitmap.recycle()
                page.close()
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            renderer?.close()
            pfd?.close()
            pdfDocument.close()
        }
        return outputFile
    }

    fun reorderPages(context: Context, pdfFile: File, newOrder: List<Int>, outputFileName: String): File {
        // reorderPages is identical in spirit to extractPages, but maps to a new order.
        return extractPages(context, pdfFile, newOrder, outputFileName)
    }

    // Helpers
    private fun decodeUriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding URI to Bitmap", e)
            null
        }
    }

    fun copyFileFromUri(context: Context, uri: Uri, outputFileName: String): File? {
        return try {
            val dir = context.filesDir
            val outputFile = File(dir, outputFileName)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            FileOutputStream(outputFile).use { out ->
                inputStream.copyTo(out)
            }
            inputStream.close()
            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error copying file from Uri", e)
            null
        }
    }

    fun addAdvancedWatermark(
        context: Context,
        pdfFile: File,
        isText: Boolean,
        text: String,
        textColor: Int,
        textSize: Float,
        imageUri: Uri?,
        opacity: Float,
        rotation: Float,
        position: String,
        outputFileName: String
    ): File {
        val pdfDocument = PdfDocument()
        val outputFile = File(context.filesDir, outputFileName)
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var pageIndex = 1

        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val count = renderer.pageCount

            val watermarkBitmap = if (!isText && imageUri != null) {
                decodeUriToBitmap(context, imageUri)
            } else {
                null
            }

            for (i in 0 until count) {
                val page = renderer.openPage(i)
                val scale = if (page.height > 1500) 1500f / page.height else 1.0f
                val w = (page.width * scale).toInt()
                val h = (page.height * scale).toInt()

                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvasBg = Canvas(bitmap)
                canvasBg.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                val pageInfo = PdfDocument.PageInfo.Builder(w, h, pageIndex++).create()
                val newPage = pdfDocument.startPage(pageInfo)
                val canvas = newPage.canvas
                canvas.drawBitmap(bitmap, 0f, 0f, null)

                val alpha = (opacity * 255).toInt().coerceIn(0, 255)

                if (isText) {
                    val paint = Paint().apply {
                        this.color = textColor
                        this.textSize = textSize
                        this.alpha = alpha
                        this.isAntiAlias = true
                        this.textAlign = Paint.Align.CENTER
                    }
                    canvas.save()
                    val (x, y) = getCoordinates(position, w, h, paint.measureText(text), paint.textSize)
                    canvas.rotate(rotation, x, y)
                    canvas.drawText(text, x, y, paint)
                    canvas.restore()
                } else if (watermarkBitmap != null) {
                    val paint = Paint().apply {
                        this.alpha = alpha
                        this.isAntiAlias = true
                    }
                    canvas.save()
                    val maxW = w * 0.35f
                    val scaleW = if (watermarkBitmap.width > maxW) maxW / watermarkBitmap.width else 1.0f
                    val wmWidth = watermarkBitmap.width * scaleW
                    val wmHeight = watermarkBitmap.height * scaleW

                    val (x, y) = getCoordinates(position, w, h, wmWidth, wmHeight)
                    canvas.rotate(rotation, x + wmWidth/2f, y + wmHeight/2f)
                    val destRect = RectF(x, y, x + wmWidth, y + wmHeight)
                    canvas.drawBitmap(watermarkBitmap, null, destRect, paint)
                    canvas.restore()
                }

                pdfDocument.finishPage(newPage)
                bitmap.recycle()
                page.close()
            }

            watermarkBitmap?.recycle()

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            renderer?.close()
            pfd?.close()
            pdfDocument.close()
        }
        return outputFile
    }

    private fun getCoordinates(position: String, pageW: Int, pageH: Int, elementW: Float, elementH: Float): Pair<Float, Float> {
        return when (position) {
            "Top Left" -> Pair(elementW / 2f + 40f, elementH + 40f)
            "Top Right" -> Pair(pageW - elementW / 2f - 40f, elementH + 40f)
            "Bottom Left" -> Pair(elementW / 2f + 40f, pageH - 40f)
            "Bottom Right" -> Pair(pageW - elementW / 2f - 40f, pageH - 40f)
            else -> Pair(pageW / 2f, pageH / 2f) // Center
        }
    }

    fun encryptPdfWithZip4j(context: Context, pdfFile: File, password: String, outName: String): File {
        val outputFile = File(context.filesDir, outName)
        if (outputFile.exists()) outputFile.delete()

        val zipFile = ZipFile(outputFile, password.toCharArray())
        val parameters = ZipParameters().apply {
            this.isEncryptFiles = true
            this.encryptionMethod = EncryptionMethod.AES
            this.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
        }

        zipFile.addFile(pdfFile, parameters)
        return outputFile
    }

    fun decryptPdfWithZip4j(context: Context, zipFile: File, password: String, outName: String): File {
        val zf = ZipFile(zipFile, password.toCharArray())
        val tempDir = File(context.cacheDir, "temp_zip_extract_${System.currentTimeMillis()}")
        if (!tempDir.exists()) tempDir.mkdirs()

        zf.extractAll(tempDir.absolutePath)
        
        val files = tempDir.listFiles()
        val pdfFile = files?.firstOrNull { it.name.endsWith(".pdf", ignoreCase = true) }
            ?: throw Exception("No PDF found in secure archive")

        val targetFile = File(context.filesDir, outName)
        if (targetFile.exists()) targetFile.delete()
        
        pdfFile.copyTo(targetFile, overwrite = true)
        tempDir.deleteRecursively()
        
        return targetFile
    }

    fun renderPageToBitmap(context: Context, pdfFile: File, pageIndex: Int, rotationAngle: Float): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (pageIndex in 0 until renderer.pageCount) {
                val page = renderer.openPage(pageIndex)
                val scale = if (page.height > 800) 800f / page.height else 1.0f
                val w = (page.width * scale).toInt()
                val h = (page.height * scale).toInt()
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                if (rotationAngle != 0f) {
                    val matrix = Matrix().apply { postRotate(rotationAngle) }
                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    if (rotatedBitmap != bitmap) {
                        bitmap.recycle()
                    }
                    return rotatedBitmap
                }
                return bitmap
            }
        } catch (e: Exception) {
            Log.e("PdfToolsEngine", "Error rendering preview", e)
        } finally {
            renderer?.close()
            pfd?.close()
        }
        return null
    }
}
