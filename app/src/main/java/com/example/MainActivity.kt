package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScreenDestination
import com.example.ui.StorySparkViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*

import androidx.activity.SystemBarStyle

class MainActivity : ComponentActivity() {
    private val viewModel: StorySparkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initial edge-to-edge configuration
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = themeMode == com.example.ui.AppThemeMode.DARK

            // Ensure status bar and navigation bar icons remain crisp and readable in both light and dark themes
            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            MyApplicationTheme(darkTheme = isDark) {
                val currentUser by viewModel.currentUser.collectAsState()
                if (currentUser == null) {
                    SignInScreen(viewModel = viewModel)
                } else {
                    StorySparkApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorySparkApp(viewModel: StorySparkViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val credits by viewModel.credits.collectAsState()

    val isSubScreen = currentScreen is ScreenDestination.StoryEditor || currentScreen is ScreenDestination.BookReader

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (!isSubScreen) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SparkPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoStories,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "StorySpark",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = " AI",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = SparkPrimary
                                    )
                                }
                                Text(
                                    text = "Create magical stories. Bring them to life.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        Surface(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            color = SparkSecondaryContainer,
                            onClick = { viewModel.navigateTo(ScreenDestination.Pricing) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Stars,
                                    contentDescription = "Credits",
                                    tint = SparkSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$credits cr",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = SparkOnSecondaryContainer
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        floatingActionButton = {
            if (currentScreen is ScreenDestination.Home) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.wizardStep.value = 1
                        viewModel.navigateTo(ScreenDestination.CreateStory)
                    },
                    containerColor = SparkPrimary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = "Create Story") },
                    text = { Text("Create Story", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("dashboard_create_fab")
                )
            }
        },
        bottomBar = {
            if (!isSubScreen) {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen is ScreenDestination.Home,
                        onClick = { viewModel.navigateTo(ScreenDestination.Home) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentScreen is ScreenDestination.MyBooks,
                        onClick = { viewModel.navigateTo(ScreenDestination.MyBooks) },
                        icon = { Icon(Icons.Default.AutoStories, contentDescription = "My Books") },
                        label = { Text("My Books", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentScreen is ScreenDestination.Templates,
                        onClick = { viewModel.navigateTo(ScreenDestination.Templates) },
                        icon = { Icon(Icons.Default.Category, contentDescription = "Templates") },
                        label = { Text("Templates", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentScreen is ScreenDestination.Pricing,
                        onClick = { viewModel.navigateTo(ScreenDestination.Pricing) },
                        icon = { Icon(Icons.Default.Payment, contentDescription = "Pricing") },
                        label = { Text("Pricing", fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = currentScreen is ScreenDestination.Profile,
                        onClick = { viewModel.navigateTo(ScreenDestination.Profile) },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile", fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is ScreenDestination.Home -> HomeScreen(viewModel = viewModel)
                is ScreenDestination.CreateStory -> CreateStoryScreen(viewModel = viewModel)
                is ScreenDestination.MyBooks -> MyBooksScreen(viewModel = viewModel)
                is ScreenDestination.Templates -> TemplatesScreen(viewModel = viewModel)
                is ScreenDestination.Pricing -> PricingScreen(viewModel = viewModel)
                is ScreenDestination.Profile -> ProfileScreen(viewModel = viewModel)
                is ScreenDestination.StoryEditor -> StoryEditorScreen(bookId = screen.bookId, viewModel = viewModel)
                is ScreenDestination.BookReader -> BookReaderScreen(bookId = screen.bookId, viewModel = viewModel)
            }
        }
    }
}
