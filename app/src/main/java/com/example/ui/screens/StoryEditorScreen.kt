package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ai.AiToolType
import com.example.audio.StorySpeakerManager
import com.example.data.BookEntity
import com.example.data.PageEntity
import com.example.ui.ScreenDestination
import com.example.ui.StorySparkViewModel
import com.example.ui.components.PdfExportDialog
import com.example.ui.theme.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryEditorScreen(
    bookId: Long,
    viewModel: StorySparkViewModel,
    modifier: Modifier = Modifier
) {
    val book by viewModel.activeBook.collectAsState()
    val pages by viewModel.activePages.collectAsState()
    val characters by viewModel.activeCharacters.collectAsState()

    var editingPage by remember { mutableStateOf<PageEntity?>(null) }
    var pageForAiTools by remember { mutableStateOf<PageEntity?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showTranslateDialog by remember { mutableStateOf(false) }
    var showCoverEditDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val speakerManager = remember { StorySpeakerManager(context) }
    val isSpeaking by speakerManager.isSpeaking.collectAsState()
    var speakingPageId by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            speakerManager.shutdown()
        }
    }

    // Intercept Back press to return to My Books
    BackHandler {
        speakerManager.stop()
        viewModel.navigateTo(ScreenDestination.MyBooks)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = book?.title ?: "Story Editor",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${pages.size} Pages • ${book?.artStyle ?: "Watercolor"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(ScreenDestination.MyBooks) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showTranslateDialog = true }) {
                        Icon(Icons.Default.Translate, contentDescription = "Translate")
                    }
                    IconButton(onClick = { showCoverEditDialog = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Cover settings")
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = SparkSecondary)
                    }
                    Button(
                        onClick = { viewModel.navigateTo(ScreenDestination.BookReader(bookId)) },
                        colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.addNewPage(bookId) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Page") },
                containerColor = SparkPrimary,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("story_editor_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Book metadata summary card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SparkPrimaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✨ ${book?.storyType} Storybook",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SparkOnPrimaryContainer
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SparkSecondaryContainer
                            ) {
                                Text(
                                    text = book?.targetAge ?: "4-6 yrs",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SparkOnSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (!book?.moralLesson.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Moral: \"${book?.moralLesson}\"",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (characters.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Characters: " + characters.joinToString(", ") { "${it.name} (${it.role})" },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Pages List
            itemsIndexed(pages, key = { _, p -> p.id }) { index, page ->
                EditorPageCard(
                    page = page,
                    index = index,
                    totalCount = pages.size,
                    isSpeakingThisPage = isSpeaking && speakingPageId == page.id,
                    onSpeakClick = {
                        if (isSpeaking && speakingPageId == page.id) {
                            speakerManager.stop()
                            speakingPageId = null
                        } else {
                            speakingPageId = page.id
                            speakerManager.speak(page.text) {
                                speakingPageId = null
                            }
                        }
                    },
                    onEditClick = { editingPage = page },
                    onAiToolsClick = { pageForAiTools = page },
                    onMoveUp = { viewModel.movePageUp(index) },
                    onMoveDown = { viewModel.movePageDown(index) },
                    onDelete = { viewModel.deletePage(page) }
                )
            }

            // Bottom space for FAB
            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    // Edit Page Dialog
    if (editingPage != null) {
        var newText by remember { mutableStateOf(editingPage!!.text) }
        AlertDialog(
            onDismissRequest = { editingPage = null },
            modifier = Modifier.imePadding(),
            title = { Text("Edit Page ${editingPage!!.pageNumber}") },
            text = {
                Column(modifier = Modifier.imePadding()) {
                    Text(
                        "Edit story text directly:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newText,
                        onValueChange = { newText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updatePageText(editingPage!!, newText)
                        editingPage = null
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPage = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // AI Tools Bottom Sheet / Dialog
    if (pageForAiTools != null) {
        AlertDialog(
            onDismissRequest = { pageForAiTools = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SparkPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Writing Tools for Page ${pageForAiTools!!.pageNumber}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Select an enhancement to rewrite or elevate this page:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AiToolType.values().forEach { tool ->
                        OutlinedButton(
                            onClick = {
                                viewModel.applyAiToolToPage(pageForAiTools!!, tool)
                                pageForAiTools = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tool.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pageForAiTools = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Translation Dialog
    if (showTranslateDialog) {
        val supportedCodes = listOf("en", "hi", "ur", "zh", "fr", "ar", "ja", "ru")
        AlertDialog(
            onDismissRequest = { showTranslateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = SparkPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Translate Storybook")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Choose target language for full-book translation (${viewModel.translationCreditCost} credit):", fontSize = 12.sp)
                    supportedCodes.forEach { code ->
                        val nativeName = com.example.ai.StoryTranslationService.getLanguageNativeName(code)
                        val engName = com.example.ai.StoryTranslationService.getLanguageEnglishName(code)
                        OutlinedButton(
                            onClick = {
                                showTranslateDialog = false
                                viewModel.translateActiveBook(code)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$nativeName ($engName)")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTranslateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val isTranslating by viewModel.isTranslating.collectAsState()
    val translationProgress by viewModel.translationProgress.collectAsState()
    if (isTranslating) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Translating Story...", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(translationProgress?.statusMessage ?: "Translating page...", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = SparkPrimary)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.cancelTranslation() }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // Cover Customizer Dialog
    if (showCoverEditDialog && book != null) {
        var title by remember { mutableStateOf(book!!.title) }
        var subtitle by remember { mutableStateOf(book!!.subtitle) }
        var author by remember { mutableStateOf(book!!.authorName) }
        AlertDialog(
            onDismissRequest = { showCoverEditDialog = false },
            modifier = Modifier.imePadding(),
            title = { Text("Book Cover & Author Settings") },
            text = {
                Column(
                    modifier = Modifier.imePadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Book Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = { Text("Subtitle") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("Author Name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateBook(
                        book!!.copy(
                            title = title,
                            subtitle = subtitle,
                            authorName = author
                        )
                    )
                    showCoverEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCoverEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // PDF Export Dialog
    if (showExportDialog && book != null) {
        PdfExportDialog(
            book = book!!,
            pages = pages,
            characters = characters,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun EditorPageCard(
    page: PageEntity,
    index: Int,
    totalCount: Int,
    isSpeakingThisPage: Boolean,
    onSpeakClick: () -> Unit,
    onEditClick: () -> Unit,
    onAiToolsClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Page Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SparkPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${page.pageNumber}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PAGE ${page.pageNumber}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    if (index > 0) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                        }
                    }
                    if (index < totalCount - 1) {
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Illustration Preview Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1B4B))
            ) {
                val file = remember(page.imageUri) {
                    if (page.imageUri.isNotBlank()) File(page.imageUri) else null
                }
                if (file != null && file.exists()) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(file)
                            .size(600, 340) // Limit decoded resolution to 600x340 for editor card previews
                            .scale(Scale.FILL)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Illustration for Page ${page.pageNumber}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_lumi_stars),
                        contentDescription = "Illustration for Page ${page.pageNumber}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Prompt badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = Color(0xCC000000)
                ) {
                    Text(
                        text = "🎨 ${page.sceneDescription.ifBlank { "Scene: ${page.mood}" }}",
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Page Story Text
            Text(
                text = page.text,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Actions: Edit Text, Speak (Woman Voice), AI Tools
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Edit", fontSize = 11.sp)
                }

                FilledTonalButton(
                    onClick = onSpeakClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = if (isSpeakingThisPage) {
                        ButtonDefaults.filledTonalButtonColors(containerColor = SparkPrimaryContainer)
                    } else {
                        ButtonDefaults.filledTonalButtonColors(containerColor = SparkSecondaryContainer)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(
                        imageVector = if (isSpeakingThisPage) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isSpeakingThisPage) "Pause Narration" else "Play in Woman Voice",
                        tint = if (isSpeakingThisPage) SparkPrimary else SparkOnSecondaryContainer,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSpeakingThisPage) "Pause" else "Play (Woman)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSpeakingThisPage) SparkPrimary else SparkOnSecondaryContainer
                    )
                }

                Button(
                    onClick = onAiToolsClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp), tint = SparkSecondary)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("AI Tools", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
