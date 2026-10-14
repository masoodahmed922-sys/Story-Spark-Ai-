package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ai.StoryTranslationService
import com.example.audio.StorySpeakerManager
import com.example.data.BookEntity
import com.example.data.BookTranslationData
import com.example.data.CharacterEntity
import com.example.data.PageEntity
import com.example.ui.ScreenDestination
import com.example.ui.StorySparkViewModel
import com.example.ui.components.CompactReaderBottomBar
import com.example.ui.components.PdfExportDialog
import com.example.ui.components.WomanVoiceSettingsDialog
import com.example.ui.theme.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(
    bookId: Long,
    viewModel: StorySparkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val book by viewModel.activeBook.collectAsState()
    val pages by viewModel.activePages.collectAsState()
    val characters by viewModel.activeCharacters.collectAsState()

    // Multi-Language & Translation States
    val readerLanguage by viewModel.readerLanguage.collectAsState()
    val activeTranslations by viewModel.activeTranslations.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val translationProgress by viewModel.translationProgress.collectAsState()
    val translationError by viewModel.translationError.collectAsState()
    val credits by viewModel.credits.collectAsState()

    // 0 = Front Cover, 1..N = Story Pages, N+1 = Back Cover
    var currentPageIndex by remember { mutableStateOf(0) }
    val totalSheets = pages.size + 2

    var isFullscreen by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showVoiceSettingsDialog by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showTranslatePicker by remember { mutableStateOf(false) }
    var showSaveChoiceDialog by remember { mutableStateOf(false) }
    var showReplaceConfirmDialog by remember { mutableStateOf(false) }
    var recentlyCompletedTranslation by remember { mutableStateOf<BookTranslationData?>(null) }

    // Text to Speech
    val speakerManager = remember { StorySpeakerManager(context) }
    val isSpeaking by speakerManager.isSpeaking.collectAsState()
    val voiceWarning by speakerManager.voiceWarning.collectAsState()
    var autoAdvance by remember { mutableStateOf(true) }

    // Active translation resolution
    val currentTranslation = activeTranslations[readerLanguage]
    val activeLangCode = if (readerLanguage != "original" && currentTranslation != null) {
        currentTranslation.langCode
    } else {
        book?.language ?: "en"
    }
    val isRtl = StoryTranslationService.isRtlLanguage(activeLangCode)

    val activeTitle = if (readerLanguage != "original" && currentTranslation != null) {
        currentTranslation.title
    } else {
        book?.title ?: "Storybook Reader"
    }

    val activeMoral = if (readerLanguage != "original" && currentTranslation != null) {
        currentTranslation.moral
    } else {
        book?.moralLesson.orEmpty()
    }

    val activePageTexts = remember(readerLanguage, activeTranslations, pages) {
        if (readerLanguage != "original" && currentTranslation != null) {
            currentTranslation.pages.associate { it.pageNumber to it.text }
        } else {
            emptyMap()
        }
    }

    DisposableEffect(context) {
        onDispose {
            speakerManager.shutdown()
        }
    }

    // Read handler
    fun readCurrentPage() {
        if (isSpeaking) {
            speakerManager.stop()
        } else {
            val textToRead = when (currentPageIndex) {
                0 -> "$activeTitle. ${book?.subtitle.orEmpty()}. Written by ${book?.authorName.orEmpty()}."
                in 1..pages.size -> {
                    val p = pages[currentPageIndex - 1]
                    activePageTexts[p.pageNumber] ?: p.text
                }
                else -> "The moral and lesson of our story: $activeMoral."
            }
            speakerManager.speak(textToRead, activeLangCode) {
                if (autoAdvance && currentPageIndex < totalSheets - 1) {
                    currentPageIndex++
                }
            }
        }
    }

    // Auto-advance reader handler
    LaunchedEffect(currentPageIndex, activeLangCode) {
        if (autoAdvance && isSpeaking) {
            val textToRead = when (currentPageIndex) {
                0 -> "$activeTitle. ${book?.subtitle.orEmpty()}. Written by ${book?.authorName.orEmpty()}."
                in 1..pages.size -> {
                    val p = pages[currentPageIndex - 1]
                    activePageTexts[p.pageNumber] ?: p.text
                }
                else -> "The moral and lesson of our story: $activeMoral."
            }
            speakerManager.speak(textToRead, activeLangCode) {
                if (autoAdvance && currentPageIndex < totalSheets - 1) {
                    currentPageIndex++
                }
            }
        }
    }

    // Auto-narrate on entry if requested
    LaunchedEffect(book) {
        if (book != null && viewModel.autoNarrateOnOpen.value) {
            viewModel.autoNarrateOnOpen.value = false
            readCurrentPage()
        }
    }

    BackHandler {
        speakerManager.stop()
        viewModel.navigateTo(ScreenDestination.MyBooks)
    }

    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                if (!isFullscreen) {
                    TopAppBar(
                        title = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeTitle,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SparkSecondary.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = StoryTranslationService.getLanguageNativeName(activeLangCode),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SparkSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = when (currentPageIndex) {
                                        0 -> "Front Cover"
                                        in 1..pages.size -> "Page $currentPageIndex of ${pages.size}"
                                        else -> "Back Cover • Moral & Dedication"
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                speakerManager.stop()
                                viewModel.navigateTo(ScreenDestination.MyBooks)
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            // Language switcher dropdown action
                            Box {
                                IconButton(onClick = { showLanguageMenu = true }) {
                                    Icon(
                                        Icons.Default.Translate,
                                        contentDescription = "Switch Language",
                                        tint = if (readerLanguage != "original") SparkSecondary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                DropdownMenu(
                                    expanded = showLanguageMenu,
                                    onDismissRequest = { showLanguageMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    "Original (${StoryTranslationService.getLanguageNativeName(book?.language ?: "en")})",
                                                    fontWeight = if (readerLanguage == "original") FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (readerLanguage == "original") {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = SparkPrimary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectReaderLanguage("original")
                                            showLanguageMenu = false
                                        }
                                    )

                                    if (activeTranslations.isNotEmpty()) {
                                        HorizontalDivider()
                                        activeTranslations.values.filter { it.status == "complete" }.forEach { trans ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            StoryTranslationService.getLanguageNativeName(trans.langCode),
                                                            fontWeight = if (readerLanguage == trans.langCode) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                        if (readerLanguage == trans.langCode) {
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Icon(Icons.Default.Check, contentDescription = null, tint = SparkPrimary, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.selectReaderLanguage(trans.langCode)
                                                    showLanguageMenu = false
                                                }
                                            )
                                        }
                                    }

                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = SparkPrimary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Translate Story to...", color = SparkPrimary, fontWeight = FontWeight.SemiBold)
                                            }
                                        },
                                        onClick = {
                                            showLanguageMenu = false
                                            showTranslatePicker = true
                                        }
                                    )
                                }
                            }

                            IconButton(onClick = { readCurrentPage() }) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Read Aloud",
                                    tint = if (isSpeaking) SparkSecondary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { isFullscreen = !isFullscreen }) {
                                Icon(Icons.Default.Fullscreen, contentDescription = "Toggle Fullscreen")
                            }
                            IconButton(onClick = { showExportDialog = true }) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = SparkSecondary)
                            }
                            IconButton(onClick = {
                                speakerManager.stop()
                                viewModel.navigateTo(ScreenDestination.StoryEditor(bookId))
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Book")
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(if (isFullscreen) PaddingValues(0.dp) else innerPadding)
                    .background(Color(0xFF0F172A))
                    .testTag("book_reader_screen"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Voice warning banner if TTS engine has missing voice data
                if (voiceWarning != null) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(voiceWarning!!, fontSize = 11.sp, color = Color(0xFF92400E))
                        }
                    }
                }

                // --- MAIN BOOK STAGE ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .widthIn(max = 560.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentPageIndex == 0 || currentPageIndex == totalSheets - 1) Color(0xFF1E1B4B) else Color(0xFFFDFBF7)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        when (currentPageIndex) {
                            0 -> {
                                FrontCoverView(book = book, displayTitle = activeTitle, isRtl = isRtl)
                            }
                            in 1..pages.size -> {
                                val page = pages[currentPageIndex - 1]
                                val currentText = activePageTexts[page.pageNumber] ?: page.text
                                StoryPageView(
                                    page = page,
                                    displayText = currentText,
                                    totalCount = pages.size,
                                    isRtl = isRtl
                                )
                            }
                            else -> {
                                BackCoverView(book = book, displayMoral = activeMoral, isRtl = isRtl)
                            }
                        }
                    }
                }

                // --- COMPACT BOTTOM AUDIO PLAY & NAVIGATION BAR ---
                CompactReaderBottomBar(
                    currentPage = currentPageIndex,
                    totalPages = totalSheets,
                    isSpeaking = isSpeaking,
                    onToggleSpeak = { readCurrentPage() },
                    onPrevious = {
                        if (currentPageIndex > 0) {
                            speakerManager.stop()
                            currentPageIndex--
                        }
                    },
                    onNext = {
                        if (currentPageIndex < totalSheets - 1) {
                            speakerManager.stop()
                            currentPageIndex++
                        }
                    },
                    onOpenVoiceSettings = { showVoiceSettingsDialog = true },
                    speakerManager = speakerManager
                )
            }
        }
    }

    // --- TRANSLATE PICKER SHEET ---
    if (showTranslatePicker) {
        val supportedCodes = listOf("en", "hi", "ur", "zh", "fr", "ar", "ja", "ru")
        AlertDialog(
            onDismissRequest = { showTranslatePicker = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = SparkPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Translate Story", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "Select target language. The entire story (title, pages, and moral) will be translated page-by-page. Costs ${viewModel.translationCreditCost} credit.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    supportedCodes.forEach { code ->
                        val nativeName = StoryTranslationService.getLanguageNativeName(code)
                        val engName = StoryTranslationService.getLanguageEnglishName(code)
                        val isCurrent = (readerLanguage == code) || (readerLanguage == "original" && (book?.language ?: "en") == code)
                        val hasExisting = activeTranslations[code]?.status == "complete"

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrent) SparkPrimaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showTranslatePicker = false
                                    if (hasExisting) {
                                        viewModel.selectReaderLanguage(code)
                                    } else {
                                        viewModel.translateActiveBook(code) { success ->
                                            if (success) {
                                                recentlyCompletedTranslation = viewModel.activeTranslations.value[code]
                                                showSaveChoiceDialog = true
                                            }
                                        }
                                    }
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(nativeName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(engName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (hasExisting) {
                                    Text("Saved", fontSize = 11.sp, color = SparkPrimary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTranslatePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- TRANSLATION IN-PROGRESS MODAL ---
    if (isTranslating) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss while active */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = SparkPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Translating Story...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        translationProgress?.statusMessage ?: "Translating storybook content...",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = {
                            val cur = translationProgress?.currentPage ?: 0
                            val tot = translationProgress?.totalPages ?: 1
                            if (tot > 0) (cur.toFloat() / tot.toFloat()).coerceIn(0f, 1f) else 0.5f
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = SparkPrimary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.cancelTranslation() }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // --- POST TRANSLATION ACTION CHOICE DIALOG ---
    if (showSaveChoiceDialog && recentlyCompletedTranslation != null) {
        val trans = recentlyCompletedTranslation!!
        AlertDialog(
            onDismissRequest = { showSaveChoiceDialog = false },
            title = {
                Text("Translation Ready!", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Your story is now completely translated into ${StoryTranslationService.getLanguageNativeName(trans.langCode)}. How would you like to save it?",
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveTranslationAsNewBook(trans)
                        showSaveChoiceDialog = false
                        Toast.makeText(context, "Saved as new book!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary)
                ) {
                    Text("Save as New Book")
                }
            },
            dismissButton = {
                Row {
                    OutlinedButton(
                        onClick = {
                            showSaveChoiceDialog = false
                            showReplaceConfirmDialog = true
                        }
                    ) {
                        Text("Replace Original")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { showSaveChoiceDialog = false }) {
                        Text("Keep in Reader")
                    }
                }
            }
        )
    }

    // --- CONFIRM REPLACE DIALOG ---
    if (showReplaceConfirmDialog && recentlyCompletedTranslation != null) {
        val trans = recentlyCompletedTranslation!!
        AlertDialog(
            onDismissRequest = { showReplaceConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Replace", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Are you sure you want to replace this book's original text with the ${StoryTranslationService.getLanguageNativeName(trans.langCode)} translation? This cannot be undone.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.replaceOriginalWithTranslation(trans)
                        showReplaceConfirmDialog = false
                        Toast.makeText(context, "Book text updated!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Replace")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Voice settings dialog
    if (showVoiceSettingsDialog) {
        WomanVoiceSettingsDialog(
            speakerManager = speakerManager,
            autoAdvance = autoAdvance,
            onToggleAutoAdvance = { autoAdvance = it },
            onDismiss = { showVoiceSettingsDialog = false }
        )
    }

    // PDF Export Dialog with language and translation overrides
    if (showExportDialog && book != null) {
        PdfExportDialog(
            book = book!!,
            pages = pages,
            characters = characters,
            overrideTitle = activeTitle,
            overrideMoral = activeMoral,
            overridePageTexts = activePageTexts,
            languageCode = activeLangCode,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun FrontCoverView(
    book: BookEntity?,
    displayTitle: String,
    isRtl: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0x33FBBF24)
                ) {
                    Text(
                        text = "✨ STORYSPARK ORIGINAL",
                        color = SparkSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = displayTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFEF3C7),
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Serif,
                    lineHeight = if (isRtl) 34.sp else 30.sp
                )

                if (!book?.subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = book?.subtitle.orEmpty(),
                        fontSize = 13.sp,
                        color = Color(0xFFC7D2FE),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Cover illustration artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF312E81))
            ) {
                StoryIllustrationImage(
                    imagePath = book?.coverImageRes,
                    contentDescription = "Cover Illustration",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Written by ${book?.authorName ?: "Elena Vance"}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFDE68A)
                )
                Text(
                    text = "${book?.artStyle ?: "Watercolor"} • ${book?.targetAge ?: "4-6 years"}",
                    fontSize = 11.sp,
                    color = Color(0xFFA5B4FC)
                )
            }
        }
    }
}

@Composable
fun StoryIllustrationImage(
    imagePath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = remember(imagePath) {
        if (!imagePath.isNullOrBlank()) File(imagePath) else null
    }
    if (file != null && file.exists()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(file)
                .size(720, 720)
                .scale(Scale.FILL)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.img_lumi_stars),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun StoryPageView(
    page: PageEntity,
    displayText: String,
    totalCount: Int,
    isRtl: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Large illustration top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E1B4B))
        ) {
            StoryIllustrationImage(
                imagePath = page.imageUri,
                contentDescription = "Illustration Page ${page.pageNumber}",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Readable large storybook text
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = displayText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Serif,
                lineHeight = if (isRtl) 28.sp else 25.sp,
                color = Color(0xFF1E293B),
                textAlign = if (isRtl) TextAlign.Right else TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        // Page number bottom
        Text(
            text = "— Page ${page.pageNumber} of $totalCount —",
            fontSize = 12.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun BackCoverView(
    book: BookEntity?,
    displayMoral: String,
    isRtl: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "✨",
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "THE MORAL OF THE STORY",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SparkSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF312E81))
            ) {
                Text(
                    text = "\"$displayMoral\"",
                    fontSize = 15.sp,
                    lineHeight = if (isRtl) 26.sp else 22.sp,
                    color = Color.White,
                    textAlign = if (isRtl) TextAlign.Right else TextAlign.Center,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (!book?.authorBio.isNullOrBlank()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "About the Author",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SparkSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = book?.authorBio.orEmpty(),
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "StorySpark AI Publishing",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Create magical stories. Bring them to life.",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
