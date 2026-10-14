package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.example.R
import com.example.data.BookEntity
import com.example.ui.ScreenDestination
import com.example.ui.StorySparkViewModel
import com.example.ui.components.PdfExportDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MyBooksScreen(
    viewModel: StorySparkViewModel,
    modifier: Modifier = Modifier
) {
    val books by viewModel.allBooks.collectAsState()
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isSyncing by viewModel.isSyncingWithFirestore.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    var bookToExport by remember { mutableStateOf<BookEntity?>(null) }
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }

    // Load and sync books from Cloud Firestore when screen is active & user signed in
    LaunchedEffect(isSignedIn, currentUser?.uid) {
        if (isSignedIn && currentUser?.uid?.isNotBlank() == true) {
            viewModel.syncBooksWithFirestore()
        }
    }

    val categories = listOf("All", "Bedtime", "Animals", "Adventure", "Fantasy", "Friendship", "Learning")

    val filteredBooks = books.filter { book ->
        val matchesSearch = book.title.contains(searchQuery, ignoreCase = true) ||
                book.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || book.storyType.equals(selectedCategory, ignoreCase = true)
        matchesSearch && matchesCategory
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("my_books_screen")
    ) {
        // --- HEADER ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "My Storybooks",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${books.size} books in library",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isSignedIn) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSyncing) SparkSecondaryContainer else Color(0xFFE6F4EA)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(10.dp),
                                                strokeWidth = 1.5.dp,
                                                color = SparkPrimary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Cloud Syncing...",
                                                fontSize = 10.sp,
                                                color = SparkPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.CloudDone,
                                                contentDescription = "Cloud Synced",
                                                modifier = Modifier.size(12.dp),
                                                tint = Color(0xFF137333)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Cloud Synced",
                                                fontSize = 10.sp,
                                                color = Color(0xFF137333),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSignedIn) {
                            IconButton(
                                onClick = { viewModel.syncBooksWithFirestore() },
                                enabled = !isSyncing,
                                modifier = Modifier.testTag("sync_firestore_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync with Cloud Firestore",
                                    tint = if (isSyncing) SparkPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Button(
                            onClick = {
                                viewModel.wizardStep.value = 1
                                viewModel.navigateTo(ScreenDestination.CreateStory)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary),
                            modifier = Modifier.testTag("my_books_create_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Book", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by title or topic...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Filter Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        // --- BOOK LIST / EMPTY STATE ---
        if (filteredBooks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(SparkPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = SparkPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "Your magical library is waiting.",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Create your first storybook to inspire sweet dreams, courage, and imaginative wonder.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.wizardStep.value = 1
                            viewModel.navigateTo(ScreenDestination.CreateStory)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Story", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredBooks, key = { it.id }) { book ->
                    BookCardItem(
                        book = book,
                        onOpen = { viewModel.navigateTo(ScreenDestination.BookReader(book.id)) },
                        onSpeak = {
                            viewModel.autoNarrateOnOpen.value = true
                            viewModel.navigateTo(ScreenDestination.BookReader(book.id))
                        },
                        onEdit = { viewModel.navigateTo(ScreenDestination.StoryEditor(book.id)) },
                        onDownload = {
                            viewModel.loadBookDetails(book.id)
                            bookToExport = book
                        },
                        onDuplicate = { viewModel.duplicateBook(book) },
                        onDelete = { bookToDelete = book }
                    )
                }
            }
        }
    }

    // PDF Export Dialog
    if (bookToExport != null) {
        val activePages by viewModel.activePages.collectAsState()
        val activeChars by viewModel.activeCharacters.collectAsState()
        PdfExportDialog(
            book = bookToExport!!,
            pages = activePages,
            characters = activeChars,
            onDismiss = { bookToExport = null }
        )
    }

    // Delete Confirmation Dialog
    if (bookToDelete != null) {
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Delete Storybook?") },
            text = { Text("Are you sure you want to delete \"${bookToDelete?.title}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        bookToDelete?.let { viewModel.deleteBook(it) }
                        bookToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BookCardItem(
    book: BookEntity,
    onOpen: () -> Unit,
    onSpeak: () -> Unit,
    onEdit: () -> Unit,
    onDownload: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Book Cover Thumbnail
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(115.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E1B4B))
                ) {
                    val context = LocalContext.current
                    val coverFile = remember(book.coverImageRes) {
                        if (book.coverImageRes.isNotBlank()) java.io.File(book.coverImageRes) else null
                    }
                    if (coverFile != null && coverFile.exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(coverFile)
                                .size(180, 230) // Request strictly 2x dp thumbnail resolution to preserve RAM
                                .scale(Scale.FILL)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Cover for ${book.title}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_lumi_stars),
                            contentDescription = "Cover for ${book.title}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Page count badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xCC000000)
                    ) {
                        Text(
                            text = "${book.pageCount}p",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Book Cover Thumbnail & Details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Cover Box with page badge in top-right corner with padding
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(124.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1B4B))
                        ) {
                            val context = LocalContext.current
                            val coverFile = remember(book.coverImageRes) {
                                if (book.coverImageRes.isNotBlank()) java.io.File(book.coverImageRes) else null
                            }
                            if (coverFile != null && coverFile.exists()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(coverFile)
                                        .size(192, 248)
                                        .scale(Scale.FILL)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Cover for ${book.title}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.img_lumi_stars),
                                    contentDescription = "Cover for ${book.title}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Page count badge moved to top-right with padding so it never overlaps artwork
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xDD0F172A)
                            ) {
                                Text(
                                    text = "${book.pageCount}p",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Metadata column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SparkPrimaryContainer
                                ) {
                                    Text(
                                        text = book.storyType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SparkOnPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "•  ${book.targetAge}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                .format(Date(book.createdAt))
                            Text(
                                text = "By ${book.authorName.ifBlank { "StorySpark Author" }} • $dateStr",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (book.subtitle.isNotBlank()) {
                                Text(
                                    text = book.subtitle,
                                    fontSize = 12.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Full title below the cover (never cut off or baked into the image)
                    Text(
                        text = book.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )

                    // Description: allow up to 3 lines with ellipsis only after that
                    if (book.description.isNotBlank()) {
                        Text(
                            text = book.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: equal size, icon + label, wraps cleanly on small screens
            OptInFlowRowButtons(
                onOpen = onOpen,
                onSpeak = onSpeak,
                onEdit = onEdit,
                onDownload = onDownload,
                onDuplicate = onDuplicate,
                onDelete = onDelete
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptInFlowRowButtons(
    onOpen: () -> Unit,
    onSpeak: () -> Unit,
    onEdit: () -> Unit,
    onDownload: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onOpen,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("Read", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        FilledTonalButton(
            onClick = onSpeak,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = SparkSecondaryContainer),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Listen", tint = SparkOnSecondaryContainer, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("Listen", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SparkOnSecondaryContainer)
        }

        OutlinedButton(
            onClick = onEdit,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("Edit", fontSize = 12.sp)
        }

        OutlinedButton(
            onClick = onDownload,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("PDF", fontSize = 12.sp)
        }

        OutlinedButton(
            onClick = onDuplicate,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text("Copy", fontSize = 12.sp)
        }

        OutlinedButton(
            onClick = onDelete,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete", fontSize = 12.sp, color = Color.Red)
        }
    }
}
