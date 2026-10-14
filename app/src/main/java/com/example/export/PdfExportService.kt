package com.example.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.BookEntity
import com.example.data.CharacterEntity
import com.example.data.PageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class PdfPageSize(val label: String, val widthPt: Int, val heightPt: Int) {
    SQUARE_8_5("8.5 × 8.5 in (Square)", 612, 612),
    PORTRAIT_8_10("8 × 10 in (Portrait)", 576, 720),
    A4("A4 (8.3 × 11.7 in)", 595, 842),
    A5("A5 (5.8 × 8.3 in)", 420, 595)
}

data class PdfExportResult(
    val cacheFile: File,
    val downloadedUri: Uri?,
    val fileProviderUri: Uri,
    val displayPath: String,
    val fileName: String,
    val pageCount: Int
)

object PdfExportService {

    /**
     * Safely slices a string without throwing StringIndexOutOfBoundsException.
     * Always clamps start and end bounds within [0, length].
     */
    fun safeSubstring(str: String?, startIndex: Int, endIndex: Int): String {
        if (str.isNullOrEmpty()) return ""
        val safeStart = startIndex.coerceIn(0, str.length)
        val safeEnd = endIndex.coerceIn(safeStart, str.length)
        return str.substring(safeStart, safeEnd)
    }

    /**
     * Safely takes up to [n] characters from the start of [str].
     */
    fun safeTake(str: String?, n: Int): String {
        if (str.isNullOrEmpty() || n <= 0) return ""
        return if (str.length <= n) str else str.substring(0, n)
    }

    /**
     * Safely trims string and guarantees non-null result.
     */
    fun safeTrim(str: String?): String {
        return str?.trim().orEmpty()
    }

    suspend fun generateBookPdf(
        context: Context,
        book: BookEntity,
        pages: List<PageEntity>,
        characters: List<CharacterEntity>,
        pageSize: PdfPageSize = PdfPageSize.SQUARE_8_5,
        overrideTitle: String? = null,
        overrideMoral: String? = null,
        overridePageTexts: Map<Int, String>? = null,
        languageCode: String = book.language
    ): PdfExportResult = withContext(Dispatchers.IO) {
        val effectiveBook = book.copy(
            title = overrideTitle ?: book.title,
            moralLesson = overrideMoral ?: book.moralLesson,
            language = languageCode
        )
        // Ensure pages are loaded for the specific book
        val basePages = if (pages.isNotEmpty() && pages.all { it.bookId == book.id }) {
            pages
        } else {
            try {
                val db = com.example.data.AppDatabase.getDatabase(context)
                db.bookDao().getPagesForBook(book.id).first()
            } catch (_: Exception) {
                emptyList()
            }
        }
        val effectivePages = basePages.map { p ->
            val custom = overridePageTexts?.get(p.pageNumber)
            if (custom != null) p.copy(text = custom) else p
        }

        val effectiveCharacters = if (characters.isNotEmpty() && characters.all { it.bookId == book.id }) {
            characters
        } else {
            try {
                val db = com.example.data.AppDatabase.getDatabase(context)
                db.bookDao().getCharactersForBook(book.id).first()
            } catch (_: Exception) {
                emptyList()
            }
        }

        val document = PdfDocument()
        val pageWidth = pageSize.widthPt
        val pageHeight = pageSize.heightPt

        var pageIndex = 1

        // --- 1. FRONT COVER ---
        val coverPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex++).create()
        val coverPage = document.startPage(coverPageInfo)
        renderFrontCover(context, coverPage.canvas, book, effectiveCharacters.firstOrNull(), pageWidth, pageHeight)
        document.finishPage(coverPage)

        // --- 2. STORY PAGES (WITH AUTOMATIC MULTI-PAGE CONTINUATION FOR LONG TEXT) ---
        val pagesToRender = if (effectivePages.isEmpty()) {
            listOf(
                PageEntity(
                    bookId = book.id,
                    pageNumber = 1,
                    text = safeTrim(book.description).ifBlank { "Once upon a time in a world of wonder..." },
                    illustrationPrompt = "Warm children's book illustration",
                    imageUri = book.coverImageRes
                )
            )
        } else {
            effectivePages.sortedBy { it.pageNumber }
        }

        var totalStoryPagesCount = 0
        pagesToRender.forEach { pageEntity ->
            val countForThisPage = renderStoryPagesWithContinuation(
                context = context,
                document = document,
                book = book,
                page = pageEntity,
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                startIndex = pageIndex
            )
            pageIndex += countForThisPage
            totalStoryPagesCount += countForThisPage
        }

        // --- 3. BACK COVER ---
        val backPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex++).create()
        val backPage = document.startPage(backPageInfo)
        renderBackCover(context, backPage.canvas, book, pageWidth, pageHeight)
        document.finishPage(backPage)

        // Save PDF to cache directory first with safe file name construction
        val rawTitle = safeTrim(book.title).ifBlank { "StoryBook" }
        val sanitizedTitle = rawTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val safePrefix = safeSubstring(sanitizedTitle, 0, 24).ifBlank { "StoryBook" }
        val fileName = "StorySpark_${safePrefix}_${System.currentTimeMillis()}.pdf"
        val outputDir = File(context.cacheDir, "story_exports")
        if (!outputDir.exists()) outputDir.mkdirs()
        val cachePdfFile = File(outputDir, fileName)

        FileOutputStream(cachePdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        // Automatically download & copy into public Downloads folder
        val (downloadUri, displayPath) = savePdfToDownloads(context, cachePdfFile, fileName)

        val fileProviderUri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cachePdfFile)
        } catch (_: Exception) {
            downloadUri ?: Uri.EMPTY
        }

        PdfExportResult(
            cacheFile = cachePdfFile,
            downloadedUri = downloadUri ?: fileProviderUri,
            fileProviderUri = fileProviderUri,
            displayPath = displayPath,
            fileName = fileName,
            pageCount = totalStoryPagesCount + 2
        )
    }

    /**
     * Renders a story page and automatically splits across multiple PDF pages if text is long.
     * Preserves paragraph structure, text wrapping, and never truncates text.
     */
    private fun renderStoryPagesWithContinuation(
        context: Context,
        document: PdfDocument,
        book: BookEntity,
        page: PageEntity,
        pageWidth: Int,
        pageHeight: Int,
        startIndex: Int
    ): Int {
        val textSafe = safeTrim(page.text).ifBlank { "..." }

        val textPaint = TextPaint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 15f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val textWidth = (pageWidth - 96).coerceAtLeast(100)

        // Illustration bounds on the primary story page
        val artWidth = (pageWidth - 80).toFloat()
        val artHeight = (pageHeight * 0.46f)
        val artTop = 40f
        val textTopFirstPage = artTop + artHeight + 24f
        val textBottomFirstPage = pageHeight - 56f
        val maxTextHeightFirstPage = (textBottomFirstPage - textTopFirstPage).coerceAtLeast(60f)

        // Build static layout of the full text to test fit
        val initialLayout = StaticLayout.Builder.obtain(
            textSafe,
            0,
            textSafe.length,
            textPaint,
            textWidth
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

        var pagesRendered = 0

        if (initialLayout.height <= maxTextHeightFirstPage) {
            // Entire text fits on a single page with illustration
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, startIndex).create()
            val pdfPage = document.startPage(pageInfo)
            renderSingleStoryPage(
                context = context,
                canvas = pdfPage.canvas,
                book = book,
                page = page,
                pageText = textSafe,
                showIllustration = true,
                displayPageNumber = page.pageNumber,
                continuationNote = null,
                width = pageWidth,
                height = pageHeight
            )
            document.finishPage(pdfPage)
            pagesRendered++
        } else {
            // Text overflows available space: paginate cleanly across continuation pages
            var remainingText = textSafe
            var isFirstScenePage = true

            while (remainingText.isNotEmpty()) {
                val isFirst = isFirstScenePage
                val maxAllowedHeight = if (isFirst) maxTextHeightFirstPage else (pageHeight - 110f).coerceAtLeast(100f)

                val layout = StaticLayout.Builder.obtain(
                    remainingText,
                    0,
                    remainingText.length,
                    textPaint,
                    textWidth
                ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

                val textToDraw: String
                if (layout.height <= maxAllowedHeight) {
                    textToDraw = remainingText
                    remainingText = ""
                } else {
                    // Find highest line index that fits within maxAllowedHeight
                    var lastFittingLine = 0
                    for (line in 0 until layout.lineCount) {
                        if (layout.getLineBottom(line) <= maxAllowedHeight) {
                            lastFittingLine = line
                        } else {
                            break
                        }
                    }

                    val rawBreakOffset = layout.getLineEnd(lastFittingLine).coerceIn(0, remainingText.length)
                    if (rawBreakOffset <= 0 || rawBreakOffset >= remainingText.length) {
                        // Safe fallback boundary avoiding infinite loop
                        val fallbackBreak = safeTake(remainingText, minOf(remainingText.length, 300))
                        textToDraw = fallbackBreak
                        remainingText = safeSubstring(remainingText, fallbackBreak.length, remainingText.length).trimStart()
                    } else {
                        textToDraw = safeSubstring(remainingText, 0, rawBreakOffset).trimEnd()
                        remainingText = safeSubstring(remainingText, rawBreakOffset, remainingText.length).trimStart()
                    }
                }

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, startIndex + pagesRendered).create()
                val pdfPage = document.startPage(pageInfo)
                renderSingleStoryPage(
                    context = context,
                    canvas = pdfPage.canvas,
                    book = book,
                    page = page,
                    pageText = textToDraw,
                    showIllustration = isFirst,
                    displayPageNumber = page.pageNumber,
                    continuationNote = if (!isFirst) "Page ${page.pageNumber} • Continued" else null,
                    width = pageWidth,
                    height = pageHeight
                )
                document.finishPage(pdfPage)
                pagesRendered++
                isFirstScenePage = false
            }
        }

        return pagesRendered
    }

    private fun renderSingleStoryPage(
        context: Context,
        canvas: Canvas,
        book: BookEntity,
        page: PageEntity,
        pageText: String,
        showIllustration: Boolean,
        displayPageNumber: Int,
        continuationNote: String? = null,
        width: Int,
        height: Int
    ) {
        val bgPaint = Paint().apply {
            color = Color.parseColor("#FDFBF7")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(24f, 24f, width - 24f, height - 24f, borderPaint)

        val textTop: Float
        if (showIllustration) {
            // Illustration Area
            val artWidth = (width - 80).toFloat()
            val artHeight = (height * 0.46f)
            val artLeft = 40f
            val artTop = 40f

            val artPaint = Paint().apply {
                color = Color.parseColor("#EEF2FF")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(artLeft, artTop, artLeft + artWidth, artTop + artHeight), 12f, 12f, artPaint)

            val bmp = loadSafeBitmap(context, page.imageUri.ifBlank { book.coverImageRes }, artWidth.toInt(), artHeight.toInt())
            if (bmp != null) {
                val destRect = RectF(artLeft, artTop, artLeft + artWidth, artTop + artHeight)
                val srcRect = Rect(0, 0, bmp.width, bmp.height)
                canvas.drawBitmap(bmp, srcRect, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                bmp.recycle()
            }

            textTop = artTop + artHeight + 24f
        } else {
            // Subtle continuation header for non-illustrated follow-up pages
            if (!continuationNote.isNullOrBlank()) {
                val notePaint = TextPaint().apply {
                    color = Color.parseColor("#6366F1")
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val safeNote = safeSubstring(continuationNote, 0, continuationNote.length)
                val noteLayout = StaticLayout.Builder.obtain(
                    safeNote,
                    0,
                    safeNote.length,
                    notePaint,
                    (width - 96).coerceAtLeast(100)
                ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

                canvas.save()
                canvas.translate(48f, 44f)
                noteLayout.draw(canvas)
                canvas.restore()
            }
            textTop = 68f
        }

        // Story Text Area
        val textPaint = TextPaint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 15f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val pageTextSafe = safeTrim(pageText).ifBlank { "..." }
        val isRtl = book.language in listOf("ur", "ar")
        val alignment = if (isRtl) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
        val spacingMult = if (book.language in listOf("ur", "ar", "hi")) 1.35f else 1.0f
        val textLayout = StaticLayout.Builder.obtain(
            pageTextSafe,
            0,
            pageTextSafe.length,
            textPaint,
            (width - 96).coerceAtLeast(100)
        ).setAlignment(alignment)
         .setLineSpacing(0f, spacingMult)
         .build()

        canvas.save()
        canvas.translate(48f, textTop)
        textLayout.draw(canvas)
        canvas.restore()

        // Page Number at bottom
        val pageNumPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val pageLabel = if (continuationNote != null) "• $displayPageNumber (cont.) •" else "• $displayPageNumber •"
        canvas.drawText(pageLabel, width / 2f, height - 36f, pageNumPaint)
    }

    private fun renderFrontCover(
        context: Context,
        canvas: Canvas,
        book: BookEntity,
        mainChar: CharacterEntity?,
        width: Int,
        height: Int
    ) {
        // Deep indigo / warm twilight background
        val bgPaint = Paint().apply {
            color = Color.parseColor("#1E1B4B")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Decorative borders
        val borderPaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(RectF(24f, 24f, width - 24f, height - 24f), 16f, 16f, borderPaint)

        val innerBorder = Paint().apply {
            color = Color.parseColor("#6366F1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(RectF(32f, 32f, width - 32f, height - 32f), 12f, 12f, innerBorder)

        // Title text
        val titlePaint = TextPaint().apply {
            color = Color.parseColor("#FEF3C7")
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val rawTitle = safeTrim(book.title).ifBlank { "Untitled Story" }
        val titleSafe = safeSubstring(rawTitle, 0, rawTitle.length)
        val titleLayout = StaticLayout.Builder.obtain(
            titleSafe,
            0,
            titleSafe.length,
            titlePaint,
            (width - 80).coerceAtLeast(100)
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

        canvas.save()
        canvas.translate(40f, 54f)
        titleLayout.draw(canvas)
        canvas.restore()

        // Subtitle text (if present)
        val rawSubtitle = safeTrim(book.subtitle)
        var titleTotalHeight = titleLayout.height.toFloat()
        if (rawSubtitle.isNotBlank()) {
            val subPaint = TextPaint().apply {
                color = Color.parseColor("#C7D2FE")
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                isAntiAlias = true
            }
            val subSafe = safeSubstring(rawSubtitle, 0, rawSubtitle.length)
            val subLayout = StaticLayout.Builder.obtain(
                subSafe,
                0,
                subSafe.length,
                subPaint,
                (width - 80).coerceAtLeast(100)
            ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

            canvas.save()
            canvas.translate(40f, 54f + titleLayout.height + 6f)
            subLayout.draw(canvas)
            canvas.restore()
            titleTotalHeight += subLayout.height + 6f
        }

        // Cover Illustration dynamically positioned below title/subtitle
        val artTop = (54f + titleTotalHeight + 14f).coerceAtLeast(height * 0.22f)
        val artBottomAvailable = height - 90f
        val artHeight = (artBottomAvailable - artTop).coerceIn(120f, height * 0.48f)
        val artWidth = (width - 100).toFloat()
        val artLeft = 50f

        val coverBmp = loadSafeBitmap(context, book.coverImageRes, artWidth.toInt(), artHeight.toInt())

        if (coverBmp != null) {
            val destRect = RectF(artLeft, artTop, artLeft + artWidth, artTop + artHeight)
            canvas.drawRoundRect(destRect, 16f, 16f, Paint().apply { isAntiAlias = true })
            val srcRect = Rect(0, 0, coverBmp.width, coverBmp.height)
            canvas.drawBitmap(coverBmp, srcRect, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
            coverBmp.recycle()
        } else {
            val artBg = Paint().apply {
                color = Color.parseColor("#312E81")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(RectF(artLeft, artTop, artLeft + artWidth, artTop + artHeight), 16f, 16f, artBg)
        }

        // Author Name at bottom
        val authorPaint = TextPaint().apply {
            color = Color.parseColor("#F59E0B")
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val rawAuthor = safeTrim(book.authorName).ifBlank { "StorySpark Author" }
        val authorText = "Written by $rawAuthor"
        val authorSafe = safeSubstring(authorText, 0, authorText.length)
        val authorLayout = StaticLayout.Builder.obtain(
            authorSafe,
            0,
            authorSafe.length,
            authorPaint,
            (width - 80).coerceAtLeast(100)
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

        canvas.save()
        canvas.translate(40f, height - 70f)
        authorLayout.draw(canvas)
        canvas.restore()
    }

    private fun renderBackCover(
        context: Context,
        canvas: Canvas,
        book: BookEntity,
        width: Int,
        height: Int
    ) {
        val bgPaint = Paint().apply {
            color = Color.parseColor("#1E1B4B")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(RectF(24f, 24f, width - 24f, height - 24f), 16f, 16f, borderPaint)

        // Moral Lesson Header
        val moralHeaderPaint = TextPaint().apply {
            color = Color.parseColor("#F59E0B")
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val moralHeaderText = "🌟 THE MORAL OF THE STORY"
        val moralHeaderSafe = safeSubstring(moralHeaderText, 0, moralHeaderText.length)
        val moralHeaderLayout = StaticLayout.Builder.obtain(
            moralHeaderSafe,
            0,
            moralHeaderSafe.length,
            moralHeaderPaint,
            (width - 80).coerceAtLeast(100)
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

        canvas.save()
        canvas.translate(40f, 90f)
        moralHeaderLayout.draw(canvas)
        canvas.restore()

        val moralPaint = TextPaint().apply {
            color = Color.parseColor("#FEF3C7")
            textSize = 15f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }
        val rawMoral = safeTrim(book.moralLesson).ifBlank { "Kindness and wonder brighten the world." }
        val moralSafe = "\"$rawMoral\""
        val safeMoralString = safeSubstring(moralSafe, 0, moralSafe.length)
        val moralLayout = StaticLayout.Builder.obtain(
            safeMoralString,
            0,
            safeMoralString.length,
            moralPaint,
            (width - 100).coerceAtLeast(100)
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

        canvas.save()
        canvas.translate(50f, 125f)
        moralLayout.draw(canvas)
        canvas.restore()

        // Author Biography (if present)
        val rawBio = safeTrim(book.authorBio)
        if (rawBio.isNotBlank()) {
            val bioHeaderPaint = TextPaint().apply {
                color = Color.parseColor("#A5B4FC")
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val bioHeaderText = "ABOUT THE AUTHOR"
            val bioHeaderSafe = safeSubstring(bioHeaderText, 0, bioHeaderText.length)
            val bioHeaderLayout = StaticLayout.Builder.obtain(
                bioHeaderSafe,
                0,
                bioHeaderSafe.length,
                bioHeaderPaint,
                (width - 80).coerceAtLeast(100)
            ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

            canvas.save()
            canvas.translate(40f, 260f)
            bioHeaderLayout.draw(canvas)
            canvas.restore()

            val bioPaint = TextPaint().apply {
                color = Color.parseColor("#E0E7FF")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }
            val safeBioString = safeSubstring(rawBio, 0, rawBio.length)
            val bioLayout = StaticLayout.Builder.obtain(
                safeBioString,
                0,
                safeBioString.length,
                bioPaint,
                (width - 120).coerceAtLeast(100)
            ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

            canvas.save()
            canvas.translate(60f, 290f)
            bioLayout.draw(canvas)
            canvas.restore()
        }

        // Branding
        val brandPaint = TextPaint().apply {
            color = Color.parseColor("#C7D2FE")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val brandText = "✨ Created with Story Spark Ai • Printable Children's Edition"
        val brandSafe = safeSubstring(brandText, 0, brandText.length)
        val brandLayout = StaticLayout.Builder.obtain(
            brandSafe,
            0,
            brandSafe.length,
            brandPaint,
            (width - 80).coerceAtLeast(100)
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

        canvas.save()
        canvas.translate(40f, height - 70f)
        brandLayout.draw(canvas)
        canvas.restore()
    }

    /**
     * Saves the PDF file directly to device's public Downloads directory.
     * Compatible with Android 10+ (API 29+) MediaStore and legacy Android.
     */
    fun savePdfToDownloads(context: Context, pdfFile: File, fileName: String): Pair<Uri?, String> {
        var resultUri: Uri? = null
        var displayPath = "Downloads/StorySpark/$fileName"

        // 1. Try MediaStore (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/StorySpark")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                var uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri == null) {
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    displayPath = "Downloads/$fileName"
                }

                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outStream ->
                        pdfFile.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                    resultUri = uri
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Try copying to public Downloads directory
        try {
            val pubDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val storySparkDir = File(pubDownloads, "StorySpark").apply { if (!exists()) mkdirs() }
            val targetFile = File(storySparkDir, fileName)
            pdfFile.copyTo(targetFile, overwrite = true)
            MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf("application/pdf"), null)
            if (resultUri == null) {
                resultUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
                displayPath = targetFile.absolutePath
            }
        } catch (_: Throwable) {}

        // 3. Fallback: app external files Downloads
        if (resultUri == null) {
            try {
                val extDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                if (extDir != null) {
                    val fallbackFile = File(extDir, fileName)
                    pdfFile.copyTo(fallbackFile, overwrite = true)
                    MediaScannerConnection.scanFile(context, arrayOf(fallbackFile.absolutePath), arrayOf("application/pdf"), null)
                    resultUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fallbackFile)
                    displayPath = fallbackFile.absolutePath
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        // 4. Final fallback: cache file provider uri
        if (resultUri == null) {
            try {
                resultUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                displayPath = pdfFile.name
            } catch (_: Exception) {}
        }

        return Pair(resultUri, displayPath)
    }

    /**
     * Writes PDF bytes directly to a user-selected SAF Uri (e.g. from CreateDocument picker).
     */
    fun savePdfToCustomUri(context: Context, sourceFile: File, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { outStream ->
                sourceFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Renders a specific page of a PDF file to a high quality Bitmap using Android's native PdfRenderer.
     */
    fun renderPdfPage(pdfFile: File, pageIndex: Int, targetWidth: Int = 800): Bitmap? {
        return try {
            val pfd = android.os.ParcelFileDescriptor.open(pdfFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = android.graphics.pdf.PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                pfd.close()
                return null
            }
            val page = renderer.openPage(pageIndex)
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            val targetHeight = (targetWidth * aspectRatio).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    fun getPdfPageCount(pdfFile: File): Int {
        return try {
            val pfd = android.os.ParcelFileDescriptor.open(pdfFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = android.graphics.pdf.PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (_: Throwable) {
            0
        }
    }

    /**
     * Opens the PDF in the user's PDF viewer.
     */
    fun openPdf(context: Context, uri: Uri) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(viewIntent)
        } catch (_: Exception) {
            try {
                val chooser = Intent.createChooser(viewIntent, "Open PDF with...").apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (_: Exception) {
                Toast.makeText(context, "No PDF viewer app found on device.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openPdf(context: Context, file: File) {
        val uri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            Uri.EMPTY
        }
        if (uri != Uri.EMPTY) {
            openPdf(context, uri)
        } else {
            Toast.makeText(context, "Unable to access PDF file.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares the PDF via standard Android Share sheet.
     */
    fun sharePdf(context: Context, uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val chooser = Intent.createChooser(shareIntent, "Share Storybook PDF").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, "Cannot open share sheet.", Toast.LENGTH_SHORT).show()
        }
    }

    fun sharePdf(context: Context, file: File) {
        val uri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            Uri.EMPTY
        }
        if (uri != Uri.EMPTY) {
            sharePdf(context, uri)
        } else {
            Toast.makeText(context, "Unable to access PDF for sharing.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun loadSafeBitmap(context: Context, imageResOrPath: String? = null, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val path = imageResOrPath?.trim().orEmpty()
            if (path.isNotEmpty()) {
                // 1. Try file path if it starts with / or file:
                val file = if (path.startsWith("file://")) File(Uri.parse(path).path ?: "") else File(path)
                if (file.exists() && file.isFile && file.length() > 0) {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, options)
                    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                    options.inJustDecodeBounds = false
                    options.inPreferredConfig = Bitmap.Config.RGB_565
                    val bmp = BitmapFactory.decodeFile(file.absolutePath, options)
                    if (bmp != null) return bmp
                }

                // 2. Try content URI
                if (path.startsWith("content://")) {
                    context.contentResolver.openInputStream(Uri.parse(path))?.use { inputStream ->
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeStream(inputStream, null, options)
                    }
                    context.contentResolver.openInputStream(Uri.parse(path))?.use { inputStream ->
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
                            inJustDecodeBounds = false
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        val bmp = BitmapFactory.decodeStream(inputStream, null, options)
                        if (bmp != null) return bmp
                    }
                }

                // 3. Try resource identifier if it was a drawable resource name
                val resId = context.resources.getIdentifier(path, "drawable", context.packageName)
                if (resId != 0) {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeResource(context.resources, resId, options)
                    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                    options.inJustDecodeBounds = false
                    options.inPreferredConfig = Bitmap.Config.RGB_565
                    return BitmapFactory.decodeResource(context.resources, resId, options)
                }
            }

            // Fallback default drawable
            val defaultResId = R.drawable.img_lumi_stars
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, defaultResId, options)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565
            BitmapFactory.decodeResource(context.resources, defaultResId, options)
        } catch (_: Throwable) {
            null
        }
    }
}
