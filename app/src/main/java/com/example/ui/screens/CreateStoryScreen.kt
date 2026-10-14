package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScreenDestination
import com.example.ui.StorySparkViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateStoryScreen(
    viewModel: StorySparkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler {
        viewModel.navigateTo(ScreenDestination.Home)
    }

    val isGenerating by viewModel.isGenerating.collectAsState()
    val progressText by viewModel.generationProgressText.collectAsState()
    val errorMsg by viewModel.generationErrorMessage.collectAsState()
    val credits by viewModel.credits.collectAsState()

    val storyIdea by viewModel.wizardStoryIdea.collectAsState()
    val mainCharName by viewModel.wizardMainCharName.collectAsState()
    val mainCharType by viewModel.wizardMainCharType.collectAsState()
    val targetAge by viewModel.wizardTargetAge.collectAsState()
    val pageCount by viewModel.wizardPageCount.collectAsState()
    val storyTone by viewModel.wizardStoryTone.collectAsState()
    val artStyle by viewModel.wizardArtStyle.collectAsState()
    val dynamicSuggestions by viewModel.dynamicSuggestions.collectAsState()
    val isFetchingSuggestions by viewModel.isFetchingSuggestions.collectAsState()

    var showOptionalSettings by remember { mutableStateOf(false) }

    // Quick Hero Suggestions
    val heroSuggestions = remember {
        listOf("Leo 🦁", "Maya 🌸", "Lumi 🦊", "Oliver 🐻", "Luna 🌙", "Pip 🐼")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("create_story_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // --- TOP HEADER ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.navigateTo(ScreenDestination.Home) },
                            modifier = Modifier.testTag("create_story_back_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Home",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Create a Story",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Story Spark Ai Creator",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Credits Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SparkSecondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Stars,
                                contentDescription = null,
                                tint = SparkSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$credits credits",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SparkOnSecondaryContainer
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- HERO WELCOME BANNER ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SparkPrimaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SparkPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoStories, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Instant Picture Book Creator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Type your idea below or tap a theme. AI will craft the story and make custom illustrations automatically!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // --- 1. SIMPLE CORE OPTIONS (MAIN DASHBOARD) ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Section Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = SparkPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "1. What is your story about?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            // Dynamic AI Surprise Me button
                            TextButton(
                                onClick = { viewModel.surpriseMeWithIdea() },
                                enabled = !isFetchingSuggestions,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("surprise_me_button")
                            ) {
                                if (isFetchingSuggestions) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = SparkPrimary)
                                } else {
                                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isFetchingSuggestions) "Generating..." else "Surprise Me (AI)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Story Prompt Input
                        OutlinedTextField(
                            value = storyIdea,
                            onValueChange = { viewModel.updateWizardStoryIdea(it) },
                            placeholder = { Text("e.g. A brave little astronaut exploring colorful moon rocks with a friendly robot dog...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("story_idea_input"),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2,
                            maxLines = 4
                        )

                        // Dynamic AI Story Sparks Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AI-Generated Story Sparks:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                TextButton(
                                    onClick = { viewModel.fetchFreshStorySuggestions() },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp), tint = SparkPrimary)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("New Ideas", fontSize = 10.sp, color = SparkPrimary, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (dynamicSuggestions.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    dynamicSuggestions.forEach { suggestion ->
                                        val isSelected = storyIdea == suggestion.prompt
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.applyDynamicSuggestion(suggestion) },
                                            label = {
                                                Text(
                                                    text = "${suggestion.category}: ${suggestion.title.take(18)}...",
                                                    fontSize = 11.sp
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SparkPrimaryContainer,
                                                selectedLabelColor = SparkOnPrimaryContainer
                                            )
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Tap 'Surprise Me (AI)' above for dynamic personalized story concepts.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Hero Name Section
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Face, contentDescription = null, tint = SparkPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "2. Hero / Child Name",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            OutlinedTextField(
                                value = mainCharName,
                                onValueChange = { viewModel.updateWizardMainCharName(it) },
                                placeholder = { Text("Enter name (e.g. Leo, Maya, Lumi)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("hero_name_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            // Name suggestions chips
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(heroSuggestions) { item ->
                                    val rawName = item.split(" ").first()
                                    SuggestionChip(
                                        onClick = { viewModel.updateWizardMainCharName(rawName) },
                                        label = { Text(item, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                // --- 2. OPTIONAL BUT SIMPLE SETTINGS CARD ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (showOptionalSettings) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (showOptionalSettings) 2.dp else 0.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showOptionalSettings = !showOptionalSettings },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(SparkSecondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = SparkSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Optional Story & Art Settings",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (showOptionalSettings) "Tap to collapse custom options" else "$targetAge • $artStyle • $pageCount Pages",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                if (showOptionalSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showOptionalSettings) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(
                            visible = showOptionalSettings,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // Age Group
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Reader Age Group", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("2–4 years", "5–7 years", "8–10 years").forEach { age ->
                                            val isSelected = targetAge == age
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                                        color = if (isSelected) SparkPrimary else MaterialTheme.colorScheme.outlineVariant,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { viewModel.updateWizardTargetAge(age) },
                                                color = if (isSelected) SparkPrimaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                                            ) {
                                                Text(
                                                    text = age,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Art Style Selection
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("AI Illustration Art Style", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            "Watercolor" to "🎨 Soft Watercolor",
                                            "3D Animated" to "🧸 3D Pixar Style",
                                            "Whimsical Crayon" to "🖍️ Whimsical Crayon",
                                            "Classic Picture Book" to "📖 Classic Storybook",
                                            "Dreamy Pastel" to "🌸 Dreamy Pastel"
                                        ).forEach { (id, label) ->
                                            FilterChip(
                                                selected = artStyle == id,
                                                onClick = { viewModel.updateWizardArtStyle(id) },
                                                label = { Text(label, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = SparkPrimaryContainer,
                                                    selectedLabelColor = SparkOnPrimaryContainer
                                                )
                                            )
                                        }
                                    }
                                }

                                // Story Tone
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Story Tone & Feeling", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            "Gentle & Calming" to "🌙 Gentle",
                                            "Adventurous" to "⚡ Adventure",
                                            "Playful & Funny" to "😂 Silly"
                                        ).forEach { (id, label) ->
                                            val isSelected = storyTone.contains(id.take(6)) || storyTone == id
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                                        color = if (isSelected) SparkPrimary else MaterialTheme.colorScheme.outlineVariant,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { viewModel.updateWizardStoryTone(id) },
                                                color = if (isSelected) SparkPrimaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Book Page Length
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Book Length", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            4 to "4 Pages (Quick)",
                                            6 to "6 Pages (Classic)",
                                            8 to "8 Pages (Full)"
                                        ).forEach { (pages, label) ->
                                            val isSelected = pageCount == pages
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                                        color = if (isSelected) SparkPrimary else MaterialTheme.colorScheme.outlineVariant,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { viewModel.updateWizardPageCount(pages) },
                                                color = if (isSelected) SparkPrimaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Hero Type
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Hero Character Type", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("Child", "Animal", "Robot").forEach { type ->
                                            val isSelected = mainCharType == type
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                                        color = if (isSelected) SparkPrimary else MaterialTheme.colorScheme.outlineVariant,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { viewModel.updateWizardMainCharType(type) },
                                                color = if (isSelected) SparkPrimaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                                            ) {
                                                Text(
                                                    text = type,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 3. AUTOMATIC AI ILLUSTRATIONS CALLOUT ---
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Automatic AI Illustrations Included",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF1E3A8A)
                            )
                            Text(
                                text = "Every page and cover will automatically receive vibrant custom illustrations crafted specifically to match your story characters and scenes.",
                                fontSize = 11.sp,
                                color = Color(0xFF1E40AF),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Error Notice if any
                if (errorMsg != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMsg ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // --- 4. BIG PRIMARY CREATE BUTTON ---
                Button(
                    onClick = {
                        val ideaText = storyIdea.ifBlank { "A magical adventure about friendship and kindness" }
                        if (storyIdea.isBlank()) {
                            viewModel.updateWizardStoryIdea(ideaText)
                        }
                        viewModel.startGeneratingBook(context = context) { newId ->
                            viewModel.navigateTo(ScreenDestination.BookReader(newId))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("generate_story_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary),
                    enabled = !isGenerating
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Create Story & Illustrations",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Includes full story text, custom AI page illustrations & audio read-aloud",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // --- FULL-SCREEN GENERATION PROGRESS OVERLAY ---
        if (isGenerating) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(54.dp),
                                color = SparkPrimary,
                                strokeWidth = 4.dp
                            )

                            Text(
                                text = "Creating Your Storybook",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = progressText.ifBlank { "Writing story & crafting AI illustrations..." },
                                fontSize = 13.sp,
                                color = SparkPrimary,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )

                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SparkPrimary,
                                trackColor = SparkPrimaryContainer
                            )

                            Text(
                                text = "Please wait a moment while the AI weaves the story and generates custom illustrations for every page...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
