package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookEntity
import com.example.export.PdfExportService
import com.example.export.PdfPageSize
import com.example.ui.ScreenDestination
import com.example.ui.StorySparkViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ==========================================
// 1. WRITE PIPELINE DIALOG
// ==========================================
@Composable
fun WritePipelineDialog(
    viewModel: StorySparkViewModel,
    onDismiss: () -> Unit
) {
    val books by viewModel.allBooks.collectAsState()
    var storyIdea by remember { mutableStateOf(viewModel.wizardStoryIdea.value) }
    var selectedAge by remember { mutableStateOf(viewModel.wizardTargetAge.value) }
    var selectedType by remember { mutableStateOf(viewModel.wizardStoryType.value) }
    var charName by remember { mutableStateOf(viewModel.wizardMainCharName.value) }

    val ageOptions = listOf("2–4 years", "4–6 years", "6–8 years", "8–10 years", "10–12 years")
    val typeOptions = listOf("Bedtime", "Adventure", "Friendship", "Fantasy", "Animals", "Educational")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SparkPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = SparkPrimary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Story Writing Studio", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Pipeline Stage 1: Craft Story Narrative", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Story Premise / Idea:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    TextButton(
                        onClick = {
                            viewModel.surpriseMeWithIdea()
                            storyIdea = viewModel.wizardStoryIdea.value
                            selectedAge = viewModel.wizardTargetAge.value
                            selectedType = viewModel.wizardStoryType.value
                            charName = viewModel.wizardMainCharName.value
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(14.dp), tint = SparkPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Surprise Idea", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SparkPrimary)
                    }
                }

                OutlinedTextField(
                    value = storyIdea,
                    onValueChange = { storyIdea = it },
                    placeholder = { Text("What is your story about? (e.g. A shy hedgehog who discovers a magical rainbow stone)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Text("Target Reader Age:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ageOptions) { age ->
                        FilterChip(
                            selected = selectedAge == age,
                            onClick = { selectedAge = age },
                            label = { Text(age, fontSize = 11.sp) }
                        )
                    }
                }

                Text("Story Genre:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(typeOptions) { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = charName,
                    onValueChange = { charName = it },
                    label = { Text("Main Character Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                if (books.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Or Edit an Existing Script:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            books.take(3).forEach { book ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.navigateTo(ScreenDestination.StoryEditor(book.id))
                                            onDismiss()
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = book.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1
                                    )
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp), tint = SparkPrimary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.wizardStoryIdea.value = storyIdea
                    viewModel.wizardTargetAge.value = selectedAge
                    viewModel.wizardStoryType.value = selectedType
                    viewModel.wizardMainCharName.value = charName
                    viewModel.wizardStep.value = 1
                    viewModel.navigateTo(ScreenDestination.CreateStory)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary)
            ) {
                Text("Start Writing Flow", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ==========================================
// 2. ILLUSTRATE PIPELINE DIALOG
// ==========================================
@Composable
fun IllustratePipelineDialog(
    viewModel: StorySparkViewModel,
    onDismiss: () -> Unit
) {
    val books by viewModel.allBooks.collectAsState()
    var selectedArtStyle by remember { mutableStateOf(viewModel.wizardArtStyle.value) }
    var selectedBook by remember { mutableStateOf(books.firstOrNull()) }
    var sceneDescription by remember { mutableStateOf("Sitting under a glowing willow tree looking up at shooting stars") }

    val styles = listOf("Watercolor", "3D Animated", "Cartoon", "Storybook", "Colored Pencil", "Fantasy", "Anime")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SparkSecondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = SparkSecondary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Illustration Studio", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Pipeline Stage 2: Art Styles & Visual Prompts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("1. Choose Illustration Art Style:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(styles) { style ->
                        FilterChip(
                            selected = selectedArtStyle == style,
                            onClick = {
                                selectedArtStyle = style
                                viewModel.wizardArtStyle.value = style
                            },
                            label = { Text(style, fontSize = 11.sp) }
                        )
                    }
                }

                Text("2. Select Storybook to Illustrate:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (books.isEmpty()) {
                    Text("No books created yet. Create a story first!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            books.forEach { book ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedBook = book }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedBook?.id == book.id,
                                        onClick = { selectedBook = book }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(book.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text("${book.pageCount} pages • Art: ${book.artStyle}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                Text("3. Visual Scene Prompt Composer:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedTextField(
                    value = sceneDescription,
                    onValueChange = { sceneDescription = it },
                    label = { Text("Scene Action & Environment") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SparkPrimaryContainer.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "Synthesized Prompt: \"$sceneDescription in $selectedArtStyle children's picture book style, rich lighting, faithful character bible consistency, no text.\"",
                        fontSize = 11.sp,
                        color = SparkOnPrimaryContainer,
                        modifier = Modifier.padding(8.dp),
                        lineHeight = 15.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedBook != null) {
                        viewModel.loadBookDetails(selectedBook!!.id)
                        viewModel.navigateTo(ScreenDestination.StoryEditor(selectedBook!!.id))
                    } else {
                        viewModel.wizardArtStyle.value = selectedArtStyle
                        viewModel.wizardStep.value = 4
                        viewModel.navigateTo(ScreenDestination.CreateStory)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SparkSecondary)
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open Art Studio", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ==========================================
// 3. PUBLISH PIPELINE DIALOG
// ==========================================
@Composable
fun PublishPipelineDialog(
    viewModel: StorySparkViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val books by viewModel.allBooks.collectAsState()
    val activePages by viewModel.activePages.collectAsState()
    val activeChars by viewModel.activeCharacters.collectAsState()

    var selectedBook by remember { mutableStateOf(books.firstOrNull()) }
    var selectedSize by remember { mutableStateOf(PdfPageSize.SQUARE_8_5) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var generatedResult by remember { mutableStateOf<com.example.export.PdfExportResult?>(null) }

    // Load active pages and characters whenever selected book changes
    LaunchedEffect(selectedBook) {
        selectedBook?.let { viewModel.loadBookDetails(it.id) }
    }

    AlertDialog(
        onDismissRequest = { if (!isGeneratingPdf) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SparkTertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = SparkTertiary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Publishing & Export Hub", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Pipeline Stage 3: Print-Ready PDF & Distribution", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("1. Select Book to Publish:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (books.isEmpty()) {
                    Text("No books created yet. Please create a story first.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            books.forEach { book ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedBook = book }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedBook?.id == book.id,
                                        onClick = { selectedBook = book }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(book.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text("By ${book.authorName} • ${book.pageCount} pages", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                Text("2. Choose Trim Size Format:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                PdfPageSize.values().forEach { size ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSize = size }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSize == size,
                            onClick = { selectedSize = size }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(size.label, fontSize = 12.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFD1FAE5)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Includes Front Cover, Full Page Layouts, Moral Lesson & Author Bio on Back Cover.",
                            color = Color(0xFF065F46),
                            fontSize = 11.sp
                        )
                    }
                }

                if (isGeneratingPdf) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compiling print-ready PDF...", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (generatedResult != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val uri = generatedResult!!.fileProviderUri
                            PdfExportService.openPdf(context, uri)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open PDF", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val uri = generatedResult!!.fileProviderUri
                            PdfExportService.sharePdf(context, uri)
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }
            } else {
                Button(
                    onClick = {
                        val book = selectedBook
                        if (book != null) {
                            isGeneratingPdf = true
                            coroutineScope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        PdfExportService.generateBookPdf(
                                            context = context,
                                            book = book,
                                            pages = activePages,
                                            characters = activeChars,
                                            pageSize = selectedSize
                                        )
                                    }
                                    generatedResult = result
                                    isGeneratingPdf = false
                                    Toast.makeText(context, "PDF saved to Downloads folder!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    isGeneratingPdf = false
                                    Toast.makeText(context, "Publishing error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please create a storybook first.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = selectedBook != null && !isGeneratingPdf,
                    colors = ButtonDefaults.buttonColors(containerColor = SparkTertiary)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download & Publish PDF", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isGeneratingPdf) {
                Text("Cancel")
            }
        }
    )
}
