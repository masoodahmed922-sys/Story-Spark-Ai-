package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookEntity
import com.example.data.CharacterEntity
import com.example.data.PageEntity
import com.example.export.PdfExportResult
import com.example.export.PdfExportService
import com.example.export.PdfPageSize
import com.example.ui.theme.SparkPrimary
import com.example.ui.theme.SparkPrimaryContainer
import com.example.ui.theme.SparkSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PdfExportDialog(
    book: BookEntity,
    pages: List<PageEntity>,
    characters: List<CharacterEntity>,
    overrideTitle: String? = null,
    overrideMoral: String? = null,
    overridePageTexts: Map<Int, String>? = null,
    languageCode: String = book.language,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedSize by remember { mutableStateOf(PdfPageSize.SQUARE_8_5) }
    var isExporting by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<PdfExportResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPreviewMode by remember { mutableStateOf(false) }
    var previewPageIndex by remember { mutableStateOf(0) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var totalPreviewPages by remember { mutableStateOf(0) }

    // Safely recycle bitmap when dialog is dismissed
    DisposableEffect(Unit) {
        onDispose {
            previewBitmap?.recycle()
            previewBitmap = null
        }
    }

    // SAF custom file saver launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        if (destUri != null && exportResult != null) {
            val success = PdfExportService.savePdfToCustomUri(context, exportResult!!.cacheFile, destUri)
            if (success) {
                Toast.makeText(context, "Saved successfully to your chosen folder!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to write file to selected folder.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load preview page bitmap when preview mode or page index changes
    LaunchedEffect(isPreviewMode, previewPageIndex, exportResult) {
        if (isPreviewMode && exportResult != null) {
            withContext(Dispatchers.IO) {
                val oldBmp = previewBitmap
                val bmp = PdfExportService.renderPdfPage(exportResult!!.cacheFile, previewPageIndex, 600)
                val count = PdfExportService.getPdfPageCount(exportResult!!.cacheFile)
                withContext(Dispatchers.Main) {
                    oldBmp?.recycle()
                    previewBitmap = bmp
                    totalPreviewPages = count
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SparkPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        if (isPreviewMode) "Preview Storybook PDF" else "Download Storybook PDF",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Print-ready children's picture book", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Book Info Header
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = book.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Author: ${book.authorName.ifBlank { "StorySpark Author" }} • Age: ${book.targetAge}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // In-App PDF Preview View
                if (isPreviewMode && exportResult != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E1B4B)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val bmp = previewBitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "PDF Page Preview",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                            }
                        }
                    }

                    // Page Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (previewPageIndex > 0) previewPageIndex-- },
                            enabled = previewPageIndex > 0
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Page")
                        }

                        Text(
                            text = "Page ${previewPageIndex + 1} of ${totalPreviewPages.coerceAtLeast(1)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )

                        IconButton(
                            onClick = { if (previewPageIndex < totalPreviewPages - 1) previewPageIndex++ },
                            enabled = previewPageIndex < totalPreviewPages - 1
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Page")
                        }
                    }
                }

                if (exportResult == null && !isPreviewMode) {
                    Text(
                        text = "Select Book Format & Trim Size:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    PdfPageSize.values().forEach { size ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (selectedSize == size) 1.5.dp else 0.5.dp,
                                    color = if (selectedSize == size) SparkPrimary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedSize = size },
                            color = if (selectedSize == size) SparkPrimaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedSize == size,
                                    onClick = { selectedSize = size },
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(size.label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = if (size == PdfPageSize.SQUARE_8_5) "Ideal for picture books & bedtime stories" else "Standard print format",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Download Destination Note
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = SparkPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Auto Destination: Device Downloads folder",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // In-progress state
                if (isExporting) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp, color = SparkPrimary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Rendering & saving PDF to Downloads...", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Compiling covers, typography & high-res illustrations", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Error State
                if (errorMessage != null && !isExporting) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "Download failed",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Completed state
                if (exportResult != null && !isExporting && !isPreviewMode) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF166534), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PDF Ready & Saved!",
                                    color = Color(0xFF166534),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "File: ${exportResult!!.fileName}",
                                fontSize = 11.sp,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Path: ${exportResult!!.displayPath}",
                                fontSize = 10.sp,
                                color = Color(0xFF166534).copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Action buttons grid for downloaded file
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // In-App Preview Button
                            OutlinedButton(
                                onClick = { isPreviewMode = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview Pages", fontSize = 12.sp)
                            }

                            // Save As... (SAF)
                            Button(
                                onClick = {
                                    createDocumentLauncher.launch(exportResult!!.fileName)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save As...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Open PDF in external app
                            FilledTonalButton(
                                onClick = {
                                    PdfExportService.openPdf(context, exportResult!!.fileProviderUri)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open in App", fontSize = 12.sp)
                            }

                            // Share PDF Button
                            OutlinedButton(
                                onClick = {
                                    PdfExportService.sharePdf(context, exportResult!!.fileProviderUri)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isPreviewMode) {
                Button(
                    onClick = { isPreviewMode = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary)
                ) {
                    Text("Back to Export")
                }
            } else if (exportResult != null) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary)
                ) {
                    Text("Done")
                }
            } else {
                Button(
                    onClick = {
                        isExporting = true
                        errorMessage = null
                        coroutineScope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    PdfExportService.generateBookPdf(
                                        context = context,
                                        book = book,
                                        pages = pages,
                                        characters = characters,
                                        pageSize = selectedSize,
                                        overrideTitle = overrideTitle,
                                        overrideMoral = overrideMoral,
                                        overridePageTexts = overridePageTexts,
                                        languageCode = languageCode
                                    )
                                }
                                exportResult = result
                                isExporting = false
                                Toast.makeText(context, "PDF saved to Downloads folder!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                isExporting = false
                                errorMessage = "Download failed: ${e.message}"
                                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary),
                    modifier = Modifier.testTag("download_pdf_confirm_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (errorMessage != null) "Retry Download" else "Download PDF", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isPreviewMode) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isExporting
                ) {
                    Text(if (exportResult != null) "Close" else "Cancel")
                }
            }
        }
    )
}
