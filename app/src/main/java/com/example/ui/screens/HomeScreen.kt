package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ai.DynamicStorySuggestion
import com.example.ai.StoryDataBank
import com.example.ai.StoryIdeaItem
import com.example.ui.ScreenDestination
import com.example.ui.StorySparkViewModel
import com.example.ui.components.IllustratePipelineDialog
import com.example.ui.components.PublishPipelineDialog
import com.example.ui.components.WritePipelineDialog
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: StorySparkViewModel,
    modifier: Modifier = Modifier
) {
    val books by viewModel.allBooks.collectAsState()
    val credits by viewModel.credits.collectAsState()
    val dynamicSuggestions by viewModel.dynamicSuggestions.collectAsState()
    val isFetchingSuggestions by viewModel.isFetchingSuggestions.collectAsState()

    var showWriteDialog by remember { mutableStateOf(false) }
    var showIllustrateDialog by remember { mutableStateOf(false) }
    var showPublishDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_content"),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- 1. HERO BANNER ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("hero_card"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_home_hero),
                            contentDescription = "StorySpark magical book hero banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xCC0F172A))
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xE61E1B4B))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = SparkSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$credits Credits",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SparkPrimaryContainer
                        ) {
                            Text(
                                text = "✨ AI Children's Storybook Creator",
                                color = SparkOnPrimaryContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Turn Your Ideas Into Magical Storybooks",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 30.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Create personalized children's stories with beautiful illustrations in minutes.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.wizardStep.value = 1
                                    viewModel.navigateTo(ScreenDestination.CreateStory)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("hero_create_story_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SparkPrimary
                                )
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create a Story", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { viewModel.navigateTo(ScreenDestination.MyBooks) },
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(48.dp)
                                    .testTag("hero_my_books_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AutoStories, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("My Books", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // --- 2. THREE FEATURE CARDS ---
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "End-to-End Publishing Pipeline",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureCardItem(
                        title = "Write",
                        description = "Turn your idea into an engaging children's story.",
                        icon = Icons.Default.EditNote,
                        iconBg = SparkPrimaryContainer,
                        iconTint = SparkPrimary,
                        actionLabel = "Start",
                        modifier = Modifier.weight(1f),
                        onClick = { showWriteDialog = true }
                    )
                    FeatureCardItem(
                        title = "Illustrate",
                        description = "Create beautiful illustrations for every page.",
                        icon = Icons.Default.Palette,
                        iconBg = SparkSecondaryContainer,
                        iconTint = SparkSecondary,
                        actionLabel = "Design",
                        modifier = Modifier.weight(1f),
                        onClick = { showIllustrateDialog = true }
                    )
                    FeatureCardItem(
                        title = "Publish",
                        description = "Export your finished storybook as a printable PDF.",
                        icon = Icons.Default.PictureAsPdf,
                        iconBg = SparkTertiaryContainer,
                        iconTint = SparkTertiary,
                        actionLabel = "Export",
                        modifier = Modifier.weight(1f),
                        onClick = { showPublishDialog = true }
                    )
                }
            }
        }

        // --- 3. HOW IT WORKS ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = SparkSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "How It Works",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    HowItWorksStep(number = "1", title = "Enter your idea", subtitle = "Describe characters, setting, or use random prompts")
                    Spacer(modifier = Modifier.height(10.dp))
                    HowItWorksStep(number = "2", title = "Customize your story", subtitle = "Pick age group, reading level, tone, and character bible")
                    Spacer(modifier = Modifier.height(10.dp))
                    HowItWorksStep(number = "3", title = "Generate your book", subtitle = "AI creates complete pages, illustration prompts & art")
                    Spacer(modifier = Modifier.height(10.dp))
                    HowItWorksStep(number = "4", title = "Download and share", subtitle = "Read interactively or export high-res printable PDF")
                }
            }
        }

        // --- 4. MADE FOR EVERY KIND OF STORY ---
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Made for Every Kind of Story",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Choose a genre to launch a customized story template",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                val categories = listOf(
                    CategoryChipData("Adventure", Icons.Default.Explore, Color(0xFF3B82F6)),
                    CategoryChipData("Friendship", Icons.Default.Favorite, Color(0xFFEC4899)),
                    CategoryChipData("Animals", Icons.Default.Pets, Color(0xFF10B981)),
                    CategoryChipData("Bedtime", Icons.Default.NightsStay, Color(0xFF6366F1)),
                    CategoryChipData("Fantasy", Icons.Default.AutoAwesome, Color(0xFF8B5CF6)),
                    CategoryChipData("Learning", Icons.Default.School, Color(0xFFF59E0B)),
                    CategoryChipData("Family", Icons.Default.Groups, Color(0xFF14B8A6)),
                    CategoryChipData("Funny Stories", Icons.Default.SentimentVerySatisfied, Color(0xFFF97316))
                )

                // 2-row category grid
                for (rowIndex in 0..3) {
                    val cat1 = categories[rowIndex * 2]
                    val cat2 = categories[rowIndex * 2 + 1]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategoryCard(
                            data = cat1,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.wizardStoryType.value = cat1.name
                                viewModel.navigateTo(ScreenDestination.CreateStory)
                            }
                        )
                        CategoryCard(
                            data = cat2,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.wizardStoryType.value = cat2.name
                                viewModel.navigateTo(ScreenDestination.CreateStory)
                            }
                        )
                    }
                }
            }
        }

        // --- 5. STORY IDEAS INSPIRATION CAROUSEL ---
        item {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Story Sparks & Ideas",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "AI-generated original concepts • Tap to use instantly",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isFetchingSuggestions) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SparkPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        IconButton(onClick = { viewModel.surpriseMeWithIdea() }) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Surprise idea", tint = SparkPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (dynamicSuggestions.isNotEmpty()) {
                        items(dynamicSuggestions) { suggestion ->
                            DynamicStoryIdeaCard(
                                suggestion = suggestion,
                                onUseIdea = {
                                    viewModel.applyDynamicSuggestion(suggestion)
                                    viewModel.navigateTo(ScreenDestination.CreateStory)
                                }
                            )
                        }
                    } else {
                        items(StoryDataBank.STORY_IDEAS) { idea ->
                            StoryIdeaCard(
                                idea = idea,
                                onUseIdea = {
                                    viewModel.applyStoryIdea(idea)
                                    viewModel.navigateTo(ScreenDestination.CreateStory)
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- 6. FAQ SECTION ---
        item {
            FaqSection()
        }
    }

    if (showWriteDialog) {
        WritePipelineDialog(viewModel = viewModel, onDismiss = { showWriteDialog = false })
    }
    if (showIllustrateDialog) {
        IllustratePipelineDialog(viewModel = viewModel, onDismiss = { showIllustrateDialog = false })
    }
    if (showPublishDialog) {
        PublishPipelineDialog(viewModel = viewModel, onDismiss = { showPublishDialog = false })
    }
}

@Composable
fun FeatureCardItem(
    title: String,
    description: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    actionLabel: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(165.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = iconBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(actionLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = iconTint)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = iconTint, modifier = Modifier.size(10.dp))
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun HowItWorksStep(number: String, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(SparkPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class CategoryChipData(val name: String, val icon: ImageVector, val color: Color)

@Composable
fun CategoryCard(
    data: CategoryChipData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(52.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(data.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = data.icon,
                    contentDescription = null,
                    tint = data.color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = data.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DynamicStoryIdeaCard(
    suggestion: DynamicStorySuggestion,
    onUseIdea: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SparkPrimaryContainer
                    ) {
                        Text(
                            text = suggestion.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SparkOnPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = suggestion.recommendedAge,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = suggestion.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = suggestion.prompt,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    maxLines = 3
                )
            }

            Button(
                onClick = onUseIdea,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary)
            ) {
                Text("Use This Idea", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun StoryIdeaCard(
    idea: StoryIdeaItem,
    onUseIdea: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SparkPrimaryContainer
                    ) {
                        Text(
                            text = idea.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SparkOnPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = idea.recommendedAge,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = idea.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = idea.prompt,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    maxLines = 3
                )
            }

            Button(
                onClick = onUseIdea,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SparkPrimary)
            ) {
                Text("Use This Idea", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun FaqSection() {
    val faqList = listOf(
        "What is StorySpark AI?" to "StorySpark AI is a professional children's book creation studio that turns simple story concepts into illustrated, print-ready digital books.",
        "Can I create personalized stories?" to "Yes! You can enter a child's name, favorite animal, favorite color, and special dedication message into any story.",
        "Can I create illustrations?" to "Yes! Every page includes rich illustration prompts with consistent Character Bibles and multiple art styles (Watercolor, 3D, Cartoon, etc.).",
        "Can I download my book as PDF?" to "Yes! StorySpark includes a native PDF exporter with multiple book dimensions (8.5x8.5, 8x10, A4, A5) ready for home printing or Amazon KDP.",
        "Can I use the books commercially?" to "Yes! Commercial-use rights are available on our Pro and Creator plans subject to AI provider terms.",
        "How many pages can I create?" to "You can create storybooks ranging from 8 up to 20 pages with full beginning, middle, and ending narrative arcs.",
        "Can I edit the generated story?" to "Absolutely! The Story Editor gives you complete control over text, page reordering, adding pages, and running AI writing tools.",
        "What age groups are supported?" to "StorySpark supports 5 tailored age groups: 2–4, 4–6, 6–8, 8–10, and 10–12 years old."
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Frequently Asked Questions",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))

        faqList.forEach { (question, answer) ->
            FaqItem(question = question, answer = answer)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun FaqItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = answer,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
