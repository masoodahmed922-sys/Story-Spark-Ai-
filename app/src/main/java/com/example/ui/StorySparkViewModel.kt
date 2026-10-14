package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiToolType
import com.example.ai.DynamicStorySuggestion
import com.example.ai.StoryAiService
import com.example.ai.StoryDataBank
import com.example.ai.StoryDna
import com.example.ai.StoryGeneratorEngine
import com.example.ai.StoryIdeaItem
import com.example.ai.StoryTemplate
import com.example.data.AppDatabase
import com.example.data.BookEntity
import com.example.data.BookRepository
import com.example.data.CharacterEntity
import com.example.data.PageEntity
import com.example.data.PaymentMethodType
import com.example.data.SavedPaymentMethod
import com.example.data.SubscriptionDetails
import com.example.data.TransactionRecord
import com.example.ai.StoryTranslationService
import com.example.data.BookTranslationData
import com.example.data.BookTranslationEntity
import com.example.data.TranslationPageItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import kotlinx.coroutines.launch

sealed class ScreenDestination {
    object Home : ScreenDestination()
    object CreateStory : ScreenDestination()
    object MyBooks : ScreenDestination()
    object Templates : ScreenDestination()
    object Pricing : ScreenDestination()
    object Profile : ScreenDestination()
    data class StoryEditor(val bookId: Long) : ScreenDestination()
    data class BookReader(val bookId: Long) : ScreenDestination()
}

class StorySparkViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = BookRepository(database.bookDao())

    private val prefs = application.getSharedPreferences("storyspark_user_credits", android.content.Context.MODE_PRIVATE)
    private val firebaseAuth = FirebaseAuth.getInstance()

    companion object {
        const val DEFAULT_FREE_CREDITS = 18

        fun getInitials(name: String?): String {
            if (name.isNullOrBlank()) return "U"
            val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            return when {
                parts.size >= 2 -> "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
                parts.isNotEmpty() -> "${parts[0].first().uppercase()}"
                else -> "U"
            }
        }
    }

    private fun getStoredCreditsForUser(uid: String): Int {
        if (uid.isBlank()) return 0
        val key = "credits_$uid"
        return if (prefs.contains(key)) {
            prefs.getInt(key, DEFAULT_FREE_CREDITS)
        } else {
            prefs.edit().putInt(key, DEFAULT_FREE_CREDITS).apply()
            DEFAULT_FREE_CREDITS
        }
    }

    private fun updateStoredCredits(uid: String, credits: Int) {
        if (uid.isNotBlank()) {
            prefs.edit().putInt("credits_$uid", credits).apply()
        }
    }

    // User Authentication State
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allBooks: StateFlow<List<BookEntity>> = _currentUser
        .flatMapLatest { user ->
            if (user == null || user.uid.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.getBooksForUser(user.uid)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Screen
    private val _currentScreen = MutableStateFlow<ScreenDestination>(ScreenDestination.Home)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    // Active Book for Editor or Reader
    private val _activeBook = MutableStateFlow<BookEntity?>(null)
    val activeBook: StateFlow<BookEntity?> = _activeBook.asStateFlow()

    private val _activePages = MutableStateFlow<List<PageEntity>>(emptyList())
    val activePages: StateFlow<List<PageEntity>> = _activePages.asStateFlow()

    private val _activeCharacters = MutableStateFlow<List<CharacterEntity>>(emptyList())
    val activeCharacters: StateFlow<List<CharacterEntity>> = _activeCharacters.asStateFlow()

    // Multi-Language & Translation State
    val translationCreditCost: Int = 1
    private val _readerLanguage = MutableStateFlow("original")
    val readerLanguage: StateFlow<String> = _readerLanguage.asStateFlow()

    private val _activeTranslations = MutableStateFlow<Map<String, BookTranslationData>>(emptyMap())
    val activeTranslations: StateFlow<Map<String, BookTranslationData>> = _activeTranslations.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _translationProgress = MutableStateFlow<StoryTranslationService.TranslationProgress?>(null)
    val translationProgress: StateFlow<StoryTranslationService.TranslationProgress?> = _translationProgress.asStateFlow()

    private val _translationError = MutableStateFlow<String?>(null)
    val translationError: StateFlow<String?> = _translationError.asStateFlow()

    private var translationJob: Job? = null
    @Volatile
    private var isTranslationCancelled = false

    // User SaaS state
    private val _credits = MutableStateFlow(0)
    val credits: StateFlow<Int> = _credits.asStateFlow()

    private val _userPlan = MutableStateFlow("Free Plan")
    val userPlan: StateFlow<String> = _userPlan.asStateFlow()

    private val _authorName = MutableStateFlow("")
    val authorName: StateFlow<String> = _authorName.asStateFlow()

    private val _authorBio = MutableStateFlow("Children's storyteller weaving nighttime wonders and inspiring curiosity.")
    val authorBio: StateFlow<String> = _authorBio.asStateFlow()

    // Cloud Firestore Synchronization State
    private val _isSyncingWithFirestore = MutableStateFlow(false)
    val isSyncingWithFirestore: StateFlow<Boolean> = _isSyncingWithFirestore.asStateFlow()

    private val _firestoreSyncMessage = MutableStateFlow<String?>(null)
    val firestoreSyncMessage: StateFlow<String?> = _firestoreSyncMessage.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        if (user != null) {
            val initials = getInitials(user.displayName)
            val isGoogle = user.providerData.any { it.providerId == "google.com" } || user.providerId == "google.com"
            val providerLabel = if (isGoogle) "Google Sign-In" else (user.providerId.ifBlank { "Firebase" })
            val joined = try {
                val creationTime = user.metadata?.creationTimestamp ?: System.currentTimeMillis()
                SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(creationTime))
            } catch (e: Exception) {
                "Recent"
            }
            _currentUser.value = UserProfile(
                uid = user.uid,
                name = user.displayName?.ifBlank { "Creator" } ?: "Creator",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString(),
                avatarInitials = initials,
                authProvider = providerLabel,
                joinedDate = "Member since $joined",
                isEmailVerified = user.isEmailVerified
            )
            _isSignedIn.value = true
            _authorName.value = user.displayName ?: "Creator"
            _credits.value = getStoredCreditsForUser(user.uid)
            syncBooksWithFirestore()
        } else {
            _currentUser.value = null
            _isSignedIn.value = false
            _credits.value = 0
            _authorName.value = ""
            _activeBook.value = null
            _activePages.value = emptyList()
            _activeCharacters.value = emptyList()
            _transactions.value = emptyList()
            _savedPaymentMethods.value = emptyList()
        }
    }

    init {
        firebaseAuth.addAuthStateListener(authStateListener)
        val initialUser = firebaseAuth.currentUser
        if (initialUser != null) {
            val initials = getInitials(initialUser.displayName)
            val isGoogle = initialUser.providerData.any { it.providerId == "google.com" } || initialUser.providerId == "google.com"
            val providerLabel = if (isGoogle) "Google Sign-In" else (initialUser.providerId.ifBlank { "Firebase" })
            _currentUser.value = UserProfile(
                uid = initialUser.uid,
                name = initialUser.displayName?.ifBlank { "Creator" } ?: "Creator",
                email = initialUser.email ?: "",
                photoUrl = initialUser.photoUrl?.toString(),
                avatarInitials = initials,
                authProvider = providerLabel,
                joinedDate = "Member since " + SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(initialUser.metadata?.creationTimestamp ?: System.currentTimeMillis())),
                isEmailVerified = initialUser.isEmailVerified
            )
            _isSignedIn.value = true
            _authorName.value = initialUser.displayName ?: "Creator"
            _credits.value = getStoredCreditsForUser(initialUser.uid)
            syncBooksWithFirestore()
        }
    }

    override fun onCleared() {
        super.onCleared()
        firebaseAuth.removeAuthStateListener(authStateListener)
    }

    fun syncBooksWithFirestore() {
        val uid = _currentUser.value?.uid
        if (!_isSignedIn.value || uid.isNullOrBlank()) return

        viewModelScope.launch {
            _isSyncingWithFirestore.value = true
            try {
                val count = repository.syncBooksFromFirestore(uid)
                Log.d("StorySparkVM", "Synced $count books from Cloud Firestore for user $uid")
                _firestoreSyncMessage.value = "Synced with Cloud ($count books)"
            } catch (e: Exception) {
                Log.e("StorySparkVM", "Failed to sync books from Firestore: ${e.message}", e)
                _firestoreSyncMessage.value = "Using local offline cache"
            } finally {
                _isSyncingWithFirestore.value = false
            }
        }
    }

    fun signOut(context: android.content.Context) {
        viewModelScope.launch {
            try {
                com.example.auth.GoogleAuthService.signOut(context)
            } catch (e: Exception) {
                Log.e("StorySparkVM", "Sign out error: ${e.message}", e)
            }
            _currentUser.value = null
            _isSignedIn.value = false
            _credits.value = 0
            _authorName.value = ""
            _authorBio.value = ""
            _activeBook.value = null
            _activePages.value = emptyList()
            _activeCharacters.value = emptyList()
            _userPlan.value = "Free Plan"
            _transactions.value = emptyList()
            _savedPaymentMethods.value = emptyList()
            _currentScreen.value = ScreenDestination.Home
        }
    }

    fun deleteAccountPermanently(context: android.content.Context) {
        viewModelScope.launch {
            val uid = _currentUser.value?.uid
            if (!uid.isNullOrBlank()) {
                allBooks.value.forEach { book ->
                    repository.deleteBook(book)
                    repository.deleteBookFromFirestore(uid, book.id)
                }
                prefs.edit().remove("credits_$uid").apply()
            }
            try {
                firebaseAuth.currentUser?.delete()?.await()
            } catch (e: Exception) {
                Log.e("StorySparkVM", "Failed to delete Firebase user: ${e.message}", e)
            }
            signOut(context)
        }
    }

    // App Appearance Theme State (Bright vs Dark)
    private val _themeMode = MutableStateFlow(AppThemeMode.BRIGHT)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    fun toggleTheme() {
        _themeMode.value = if (_themeMode.value == AppThemeMode.BRIGHT) AppThemeMode.DARK else AppThemeMode.BRIGHT
    }

    // Payment & Subscription State
    private val _transactions = MutableStateFlow<List<TransactionRecord>>(emptyList())
    val transactions: StateFlow<List<TransactionRecord>> = _transactions.asStateFlow()

    private val _savedPaymentMethods = MutableStateFlow<List<SavedPaymentMethod>>(emptyList())
    val savedPaymentMethods: StateFlow<List<SavedPaymentMethod>> = _savedPaymentMethods.asStateFlow()

    private val _subscriptionDetails = MutableStateFlow(
        SubscriptionDetails(
            planName = "Free Plan",
            price = 0.00,
            billingCycle = "Monthly",
            nextRenewalDate = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
            autoRenew = false,
            status = "Active"
        )
    )
    val subscriptionDetails: StateFlow<SubscriptionDetails> = _subscriptionDetails.asStateFlow()

    // Story Speak (Woman Voice) state
    val autoNarrateOnOpen = MutableStateFlow(false)

    // Admin Dashboard settings
    val isAdminMode = MutableStateFlow(false)
    val proMonthlyPrice = MutableStateFlow(9.99)
    val creatorMonthlyPrice = MutableStateFlow(19.99)
    val creditBundlePrice = MutableStateFlow(4.99)
    val commercialTerms = MutableStateFlow("Commercial-use rights depend on your plan and the underlying AI/image providers. Check the current license terms before selling generated content.")

    // --- CREATE STORY WIZARD STATE ---
    var wizardStep = MutableStateFlow(1) // 1 to 5
    var wizardStoryIdea = MutableStateFlow("A little orange fox discovers a fallen baby star and helps it find its way back to the sky.")
    var wizardChildName = MutableStateFlow("")
    var wizardChildFavAnimal = MutableStateFlow("")
    var wizardChildFavColor = MutableStateFlow("")
    var wizardChildSpecialMessage = MutableStateFlow("")

    var wizardMainCharName = MutableStateFlow("Lumi")
    var wizardMainCharType = MutableStateFlow("Animal")
    var wizardMainCharAge = MutableStateFlow("5")
    var wizardMainCharPersonality = MutableStateFlow("Curious, gentle, and kind-hearted")
    var wizardMainCharFavThing = MutableStateFlow("Watching fireflies and peppermint tea")
    var wizardMainCharSpecialAbility = MutableStateFlow("Can understand animal whispers")
    var wizardMainCharAppearance = MutableStateFlow("Small orange fox with silky ginger fur, golden eyes, and a sky-blue knitted scarf")

    var wizardSecondaryCharacters = MutableStateFlow<List<CharacterEntity>>(emptyList())

    var wizardTargetAge = MutableStateFlow("4–6 years")
    var wizardPageCount = MutableStateFlow(8)
    var wizardStoryType = MutableStateFlow("Bedtime")
    var wizardStoryTone = MutableStateFlow("Gentle")
    var wizardReadingLevel = MutableStateFlow("Simple")
    var wizardArtStyle = MutableStateFlow("Watercolor")
    var wizardPageLayout = MutableStateFlow("Image Above Text")

    // Active Story DNA for current story creation
    val wizardStoryDna = MutableStateFlow<StoryDna?>(null)

    // Dynamic AI Suggestions for "Surprise Me"
    private val _dynamicSuggestions = MutableStateFlow<List<DynamicStorySuggestion>>(emptyList())
    val dynamicSuggestions: StateFlow<List<DynamicStorySuggestion>> = _dynamicSuggestions.asStateFlow()
    val isFetchingSuggestions = MutableStateFlow(false)

    var isGenerating = MutableStateFlow(false)
    var generationProgressText = MutableStateFlow("")
    var generationErrorMessage = MutableStateFlow<String?>(null)

    fun updateWizardStoryIdea(idea: String) {
        wizardStoryIdea.value = idea
    }

    fun updateWizardMainCharName(name: String) {
        wizardMainCharName.value = name
    }

    fun updateWizardMainCharType(type: String) {
        wizardMainCharType.value = type
    }

    fun updateWizardTargetAge(age: String) {
        wizardTargetAge.value = age
    }

    fun updateWizardPageCount(count: Int) {
        wizardPageCount.value = count
    }

    fun updateWizardStoryTone(tone: String) {
        wizardStoryTone.value = tone
    }

    fun updateWizardArtStyle(style: String) {
        wizardArtStyle.value = style
    }

    fun navigateTo(destination: ScreenDestination) {
        _currentScreen.value = destination
        if (destination is ScreenDestination.StoryEditor) {
            loadBookDetails(destination.bookId)
        } else if (destination is ScreenDestination.BookReader) {
            loadBookDetails(destination.bookId)
        }
    }

    fun loadBookDetails(bookId: Long) {
        viewModelScope.launch {
            repository.getBookById(bookId).collect { book ->
                _activeBook.value = book
            }
        }
        viewModelScope.launch {
            repository.getPagesForBook(bookId).collect { pages ->
                _activePages.value = pages
            }
        }
        viewModelScope.launch {
            repository.getCharactersForBook(bookId).collect { chars ->
                _activeCharacters.value = chars
            }
        }
        viewModelScope.launch {
            repository.getTranslationsForBook(bookId).collect { list ->
                val map = mutableMapOf<String, BookTranslationData>()
                list.forEach { entity ->
                    try {
                        val obj = org.json.JSONObject().apply {
                            put("langCode", entity.langCode)
                            put("title", entity.title)
                            put("moral", entity.moral)
                            put("status", entity.status)
                            put("model", entity.model)
                            put("updatedAt", entity.updatedAt)
                            put("pages", org.json.JSONArray(entity.pagesJson))
                        }
                        BookTranslationData.fromJson(obj.toString())?.let {
                            map[entity.langCode] = it
                        }
                    } catch (_: Exception) {}
                }
                _activeTranslations.value = map
            }
        }
        _readerLanguage.value = "original"
    }

    init {
        // Pre-fetch dynamic AI story concepts on launch so "Surprise Me" is always instant and fresh
        fetchFreshStorySuggestions()
    }

    fun fetchFreshStorySuggestions() {
        viewModelScope.launch {
            isFetchingSuggestions.value = true
            try {
                val list = StoryGeneratorEngine.generateDynamicSuggestions(
                    count = 4,
                    targetAge = wizardTargetAge.value,
                    preferredTone = wizardStoryTone.value,
                    artStyle = wizardArtStyle.value
                )
                if (list.isNotEmpty()) {
                    _dynamicSuggestions.value = list
                }
            } catch (e: Exception) {
                // Keep existing suggestions
            } finally {
                isFetchingSuggestions.value = false
            }
        }
    }

    fun surpriseMeWithIdea() {
        viewModelScope.launch {
            isFetchingSuggestions.value = true
            try {
                // If existing suggestions are present, pick one or generate fresh
                val current = _dynamicSuggestions.value
                val chosen = if (current.isNotEmpty()) {
                    val pick = current.random()
                    // Rotate and fetch new in background to ensure infinite freshness
                    fetchFreshStorySuggestions()
                    pick
                } else {
                    val list = StoryGeneratorEngine.generateDynamicSuggestions(
                        count = 4,
                        targetAge = wizardTargetAge.value,
                        preferredTone = wizardStoryTone.value,
                        artStyle = wizardArtStyle.value
                    )
                    _dynamicSuggestions.value = list
                    list.firstOrNull()
                }

                if (chosen != null) {
                    applyDynamicSuggestion(chosen)
                } else {
                    val fallbackDna = StoryGeneratorEngine.createStoryDna(
                        ageGroup = wizardTargetAge.value,
                        artStyle = wizardArtStyle.value,
                        preferredTone = wizardStoryTone.value
                    )
                    wizardStoryIdea.value = fallbackDna.centralGoal
                    wizardStoryDna.value = fallbackDna
                }
            } catch (e: Exception) {
                val fallbackDna = StoryGeneratorEngine.createStoryDna(
                    ageGroup = wizardTargetAge.value,
                    artStyle = wizardArtStyle.value,
                    preferredTone = wizardStoryTone.value
                )
                wizardStoryIdea.value = fallbackDna.centralGoal
                wizardStoryDna.value = fallbackDna
            } finally {
                isFetchingSuggestions.value = false
            }
        }
    }

    fun applyDynamicSuggestion(suggestion: DynamicStorySuggestion) {
        wizardStoryIdea.value = suggestion.prompt
        wizardTargetAge.value = suggestion.recommendedAge
        wizardStoryTone.value = suggestion.recommendedTone
        wizardStoryType.value = suggestion.category
        wizardMainCharName.value = suggestion.characterName
        wizardMainCharType.value = suggestion.characterType
        wizardMainCharAppearance.value = suggestion.characterAppearance
        wizardStoryDna.value = suggestion.dna
    }

    fun applyStoryIdea(idea: StoryIdeaItem) {
        val dna = StoryGeneratorEngine.createStoryDna(
            customIdea = idea.prompt,
            ageGroup = idea.recommendedAge,
            artStyle = wizardArtStyle.value,
            preferredTone = idea.recommendedTone
        )
        wizardStoryIdea.value = idea.prompt
        wizardTargetAge.value = idea.recommendedAge
        wizardStoryTone.value = idea.recommendedTone
        wizardStoryType.value = idea.category
        wizardMainCharName.value = idea.defaultCharName
        wizardMainCharType.value = idea.defaultCharType
        wizardMainCharAppearance.value = idea.defaultCharAppearance
        wizardStoryDna.value = dna
    }

    fun applyTemplate(template: StoryTemplate) {
        wizardStoryIdea.value = template.defaultPrompt
        wizardStoryType.value = template.category
        wizardStoryTone.value = template.recommendedTone
        wizardTargetAge.value = template.recommendedAge
        wizardArtStyle.value = template.artStyle
        wizardPageCount.value = template.pageCount
        wizardStep.value = 1
        navigateTo(ScreenDestination.CreateStory)
    }

    fun addSecondaryCharacter(character: CharacterEntity) {
        if (wizardSecondaryCharacters.value.size < 5) {
            wizardSecondaryCharacters.value = wizardSecondaryCharacters.value + character
        }
    }

    fun removeSecondaryCharacter(index: Int) {
        val list = wizardSecondaryCharacters.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            wizardSecondaryCharacters.value = list
        }
    }

    fun generateAiCharacterDescription() {
        viewModelScope.launch {
            val desc = StoryAiService.generateCharacterDescription(
                name = wizardMainCharName.value,
                type = wizardMainCharType.value,
                age = wizardMainCharAge.value,
                personality = wizardMainCharPersonality.value
            )
            wizardMainCharAppearance.value = desc
        }
    }

    fun startGeneratingBook(context: android.content.Context? = null, onSuccess: (Long) -> Unit) {
        val requiredCredits = 15
        if (_credits.value < 1 && _userPlan.value == "Free Plan") {
            generationErrorMessage.value = "You have run out of credits. Please upgrade or purchase credits!"
            return
        }

        isGenerating.value = true
        generationErrorMessage.value = null
        generationProgressText.value = "Initializing AI Story Engine..."

        val mainChar = CharacterEntity(
            bookId = 0,
            name = wizardMainCharName.value.ifBlank { "Lumi" },
            type = wizardMainCharType.value,
            age = wizardMainCharAge.value,
            personality = wizardMainCharPersonality.value,
            favoriteThing = wizardMainCharFavThing.value,
            specialAbility = wizardMainCharSpecialAbility.value,
            appearance = wizardMainCharAppearance.value.ifBlank { "Small orange fox with a blue scarf" },
            role = "Main Character",
            isMainCharacter = true
        )

        val targetContext = context ?: getApplication<Application>()

        viewModelScope.launch {
            try {
                // Step 1: Generate Story Text & Narrative Outline
                val dnaForGeneration = wizardStoryDna.value ?: StoryGeneratorEngine.createStoryDna(
                    customIdea = wizardStoryIdea.value,
                    ageGroup = wizardTargetAge.value,
                    artStyle = wizardArtStyle.value,
                    preferredTone = wizardStoryTone.value
                )

                val storyResult = StoryAiService.generateStoryBook(
                    storyIdea = wizardStoryIdea.value,
                    targetAge = wizardTargetAge.value,
                    pageCount = wizardPageCount.value,
                    storyType = wizardStoryType.value,
                    storyTone = wizardStoryTone.value,
                    readingLevel = wizardReadingLevel.value,
                    artStyle = wizardArtStyle.value,
                    mainCharacter = mainChar,
                    secondaryCharacters = wizardSecondaryCharacters.value,
                    childName = wizardChildName.value,
                    childFavorite = wizardChildFavAnimal.value,
                    authorName = _authorName.value,
                    storyDna = dnaForGeneration,
                    onProgress = { progress ->
                        generationProgressText.value = progress
                    }
                )

                // Step 2: Automatically generate AI Illustrations for book cover & every page
                generationProgressText.value = "Generating AI cover illustration..."
                val coverImagePath = StoryAiService.generateIllustrationForCover(
                    context = targetContext,
                    bookTitle = storyResult.title,
                    subtitle = storyResult.subtitle,
                    artStyle = wizardArtStyle.value,
                    mainCharName = mainChar.name,
                    mainCharAppearance = mainChar.appearance,
                    storyType = wizardStoryType.value
                )

                val totalPages = storyResult.pages.size
                val pageEntities = storyResult.pages.mapIndexed { idx, p ->
                    generationProgressText.value = "Generating AI illustration for Page ${p.pageNumber} of $totalPages..."
                    val pageImagePath = StoryAiService.generateIllustrationForPage(
                        context = targetContext,
                        bookTitle = storyResult.title,
                        pageNumber = p.pageNumber,
                        text = p.text,
                        prompt = p.illustrationPrompt,
                        artStyle = wizardArtStyle.value,
                        mainCharName = mainChar.name,
                        mainCharAppearance = mainChar.appearance,
                        storyType = wizardStoryType.value,
                        mood = p.mood,
                        location = p.location
                    )

                    PageEntity(
                        bookId = 0,
                        pageNumber = p.pageNumber,
                        text = p.text,
                        illustrationPrompt = p.illustrationPrompt,
                        sceneDescription = p.sceneDescription,
                        mood = p.mood,
                        location = p.location,
                        importantObjects = p.importantObjects,
                        imageUri = pageImagePath
                    )
                }

                // Deduct credits
                val newCredits = (_credits.value - 2).coerceAtLeast(0)
                _credits.value = newCredits
                val currentUid = _currentUser.value?.uid ?: ""
                if (currentUid.isNotBlank()) updateStoredCredits(currentUid, newCredits)

                // Serialize Story DNA to JSON for persistent reproduction
                val savedDna = storyResult.storyDna ?: dnaForGeneration
                val dnaJson = org.json.JSONObject().apply {
                    put("storySeed", savedDna.storySeed)
                    put("characterProfile", savedDna.characterProfile)
                    put("setting", savedDna.setting)
                    put("centralGoal", savedDna.centralGoal)
                    put("conflict", savedDna.conflict)
                    put("supportingCharacters", savedDna.supportingCharacters)
                    put("uniqueEvent", savedDna.uniqueEvent)
                    put("emotionalArc", savedDna.emotionalArc)
                    put("storyStructure", savedDna.storyStructure)
                    put("visualStyle", savedDna.visualStyle)
                    put("colorPalette", savedDna.colorPalette)
                    put("moral", savedDna.moral)
                    put("randomizationSeed", savedDna.randomizationSeed)
                }.toString()

                // Save to Room DB
                val bookEntity = BookEntity(
                    userId = currentUid,
                    title = storyResult.title,
                    subtitle = storyResult.subtitle,
                    description = storyResult.description,
                    authorName = _authorName.value,
                    authorBio = _authorBio.value,
                    coverImageRes = coverImagePath,
                    coverStyle = "Modern Whimsical",
                    titlePosition = "Top",
                    targetAge = wizardTargetAge.value,
                    storyType = wizardStoryType.value,
                    storyTone = wizardStoryTone.value,
                    readingLevel = wizardReadingLevel.value,
                    artStyle = wizardArtStyle.value,
                    pageLayout = wizardPageLayout.value,
                    moralLesson = storyResult.moralLesson,
                    storyDnaJson = dnaJson,
                    createdAt = System.currentTimeMillis(),
                    pageCount = storyResult.pages.size,
                    isFavorite = false
                )

                val allChars = listOf(mainChar) + wizardSecondaryCharacters.value

                val newId = repository.insertBookWithDetails(bookEntity, pageEntities, allChars)
                val uid = _currentUser.value?.uid
                if (_isSignedIn.value && !uid.isNullOrBlank()) {
                    repository.saveBookToFirestore(uid, newId)
                }
                isGenerating.value = false
                wizardStep.value = 1
                onSuccess(newId)
            } catch (e: Exception) {
                isGenerating.value = false
                generationErrorMessage.value = "Generation failed: ${e.message}. No credits were charged."
            }
        }
    }

    fun regeneratePageIllustration(context: android.content.Context, page: PageEntity, book: BookEntity) {
        viewModelScope.launch {
            try {
                val newImagePath = StoryAiService.generateIllustrationForPage(
                    context = context,
                    bookTitle = book.title,
                    pageNumber = page.pageNumber,
                    text = page.text,
                    prompt = page.illustrationPrompt,
                    artStyle = book.artStyle,
                    mainCharName = book.authorName,
                    mainCharAppearance = "",
                    storyType = book.storyType,
                    mood = page.mood,
                    location = page.location
                )
                repository.updatePage(page.copy(imageUri = newImagePath))
                val uid = _currentUser.value?.uid
                if (_isSignedIn.value && !uid.isNullOrBlank()) {
                    repository.saveBookToFirestore(uid, book.id)
                }
            } catch (e: Exception) {
                android.util.Log.e("StorySparkVM", "Failed to regenerate illustration: ${e.message}")
            }
        }
    }

    // Editor Actions
    fun updatePageText(page: PageEntity, newText: String) {
        viewModelScope.launch {
            repository.updatePage(page.copy(text = newText))
            val uid = _currentUser.value?.uid
            if (_isSignedIn.value && !uid.isNullOrBlank()) {
                repository.saveBookToFirestore(uid, page.bookId)
            }
        }
    }

    fun applyAiToolToPage(page: PageEntity, tool: AiToolType) {
        viewModelScope.launch {
            val book = _activeBook.value
            val context = book?.description ?: "Children's story"
            val improved = StoryAiService.applyAiWritingTool(tool, page.text, context)
            repository.updatePage(page.copy(text = improved))
            val uid = _currentUser.value?.uid
            if (_isSignedIn.value && !uid.isNullOrBlank()) {
                repository.saveBookToFirestore(uid, page.bookId)
            }
        }
    }

    fun selectReaderLanguage(langCode: String) {
        _readerLanguage.value = langCode
    }

    fun cancelTranslation() {
        isTranslationCancelled = true
        translationJob?.cancel()
        _isTranslating.value = false
        _translationProgress.value = null
    }

    fun translateActiveBook(targetLangCode: String, onFinished: (Boolean) -> Unit = {}) {
        val book = _activeBook.value ?: return
        val pages = _activePages.value
        val characters = _activeCharacters.value
        if (pages.isEmpty()) return

        // 1. If translation already exists, simply switch to it for free!
        val existing = _activeTranslations.value[targetLangCode]
        if (existing != null && existing.status == "complete") {
            _readerLanguage.value = targetLangCode
            onFinished(true)
            return
        }

        // 2. Check credits
        if (_credits.value < translationCreditCost) {
            _translationError.value = "You need at least $translationCreditCost credit to translate this story."
            onFinished(false)
            return
        }

        isTranslationCancelled = false
        _isTranslating.value = true
        _translationError.value = null
        _translationProgress.value = StoryTranslationService.TranslationProgress(
            currentPage = 0,
            totalPages = pages.size,
            statusMessage = "Starting translation to ${StoryTranslationService.getLanguageNativeName(targetLangCode)}..."
        )

        translationJob = viewModelScope.launch {
            try {
                val translationResult = StoryTranslationService.translateFullBook(
                    book = book,
                    pages = pages,
                    characters = characters,
                    targetLangCode = targetLangCode,
                    onProgress = { progress ->
                        _translationProgress.value = progress
                    },
                    isCancelled = { isTranslationCancelled }
                )

                // Save translation separately
                repository.saveTranslation(book.id, translationResult)

                // Deduct credits ONLY after full translation passes validation
                val newCredits = (_credits.value - translationCreditCost).coerceAtLeast(0)
                _credits.value = newCredits
                val currentUid = _currentUser.value?.uid ?: ""
                if (currentUid.isNotBlank()) updateStoredCredits(currentUid, newCredits)
                val transRecord = TransactionRecord(
                    id = "TXN_TRANS_${System.currentTimeMillis()}",
                    title = "Story Translation (${StoryTranslationService.getLanguageNativeName(targetLangCode)})",
                    amount = 0.0,
                    currency = "CREDITS",
                    timestamp = System.currentTimeMillis(),
                    paymentMethod = PaymentMethodType.GOOGLE_PLAY_BILLING,
                    status = "Completed",
                    receiptNumber = "REC-TRANS-${System.currentTimeMillis() % 1000000}",
                    description = "Used $translationCreditCost Credits for translation"
                )
                _transactions.value = listOf(transRecord) + _transactions.value

                _readerLanguage.value = targetLangCode
                _isTranslating.value = false
                _translationProgress.value = null
                onFinished(true)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    _translationError.value = "Translation was cancelled."
                } else {
                    _translationError.value = e.message ?: "Translation failed. Please try again."
                }
                _isTranslating.value = false
                _translationProgress.value = null
                onFinished(false)
            }
        }
    }

    fun saveTranslationAsNewBook(translation: BookTranslationData) {
        val originalBook = _activeBook.value ?: return
        val originalPages = _activePages.value
        val characters = _activeCharacters.value

        viewModelScope.launch {
            val newBook = originalBook.copy(
                id = 0,
                title = translation.title,
                moralLesson = translation.moral,
                language = translation.langCode,
                originalBookId = originalBook.id,
                translatedFrom = originalBook.language,
                createdAt = System.currentTimeMillis()
            )

            val newPages = originalPages.map { p ->
                val translatedText = translation.pages.find { it.pageNumber == p.pageNumber }?.text ?: p.text
                p.copy(id = 0, text = translatedText)
            }

            val newBookId = repository.insertBookWithDetails(newBook, newPages, characters)
            loadBookDetails(newBookId)
            _readerLanguage.value = "original"
        }
    }

    fun replaceOriginalWithTranslation(translation: BookTranslationData) {
        val originalBook = _activeBook.value ?: return
        val originalPages = _activePages.value

        viewModelScope.launch {
            val updatedBook = originalBook.copy(
                title = translation.title,
                moralLesson = translation.moral,
                language = translation.langCode
            )
            repository.updateBook(updatedBook)

            originalPages.forEach { p ->
                val translatedText = translation.pages.find { it.pageNumber == p.pageNumber }?.text
                if (translatedText != null) {
                    repository.updatePage(p.copy(text = translatedText))
                }
            }

            // Remove the translation from book_translations since it is now the main book
            repository.deleteTranslation(originalBook.id, translation.langCode)
            _readerLanguage.value = "original"
            loadBookDetails(originalBook.id)

            val uid = _currentUser.value?.uid
            if (_isSignedIn.value && !uid.isNullOrBlank()) {
                repository.saveBookToFirestore(uid, originalBook.id)
            }
        }
    }

    fun updateAuthorProfile(name: String, bio: String) {
        _authorName.value = name
        _authorBio.value = bio
    }

    fun updateBook(book: BookEntity) {
        viewModelScope.launch {
            repository.updateBook(book)
            _activeBook.value = book
            val uid = _currentUser.value?.uid
            if (_isSignedIn.value && !uid.isNullOrBlank()) {
                repository.saveBookToFirestore(uid, book.id)
            }
        }
    }

    fun duplicateBook(book: BookEntity) {
        viewModelScope.launch {
            val pages = _activePages.value
            val chars = _activeCharacters.value
            val newBookId = repository.duplicateBook(book, pages, chars)
            val uid = _currentUser.value?.uid
            if (_isSignedIn.value && !uid.isNullOrBlank()) {
                repository.saveBookToFirestore(uid, newBookId)
            }
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            val bookId = book.id
            repository.deleteBook(book)
            if (_activeBook.value?.id == bookId) {
                _activeBook.value = null
            }
            val uid = _currentUser.value?.uid
            if (_isSignedIn.value && !uid.isNullOrBlank()) {
                repository.deleteBookFromFirestore(uid, bookId)
            }
            navigateTo(ScreenDestination.MyBooks)
        }
    }

    fun movePageUp(index: Int) {
        val list = _activePages.value.toMutableList()
        if (index > 0 && index < list.size) {
            val temp = list[index]
            list[index] = list[index - 1]
            list[index - 1] = temp
            viewModelScope.launch {
                repository.reorderPages(list)
                val bookId = _activeBook.value?.id ?: list.firstOrNull()?.bookId
                val uid = _currentUser.value?.uid
                if (_isSignedIn.value && !uid.isNullOrBlank() && bookId != null) {
                    repository.saveBookToFirestore(uid, bookId)
                }
            }
        }
    }

    fun movePageDown(index: Int) {
        val list = _activePages.value.toMutableList()
        if (index >= 0 && index < list.size - 1) {
            val temp = list[index]
            list[index] = list[index + 1]
            list[index + 1] = temp
            viewModelScope.launch {
                repository.reorderPages(list)
                val bookId = _activeBook.value?.id ?: list.firstOrNull()?.bookId
                val uid = _currentUser.value?.uid
                if (_isSignedIn.value && !uid.isNullOrBlank() && bookId != null) {
                    repository.saveBookToFirestore(uid, bookId)
                }
            }
        }
    }

    fun deletePage(page: PageEntity) {
        viewModelScope.launch {
            repository.deletePage(page)
            val updated = _activePages.value.filter { it.id != page.id }
            repository.reorderPages(updated)
            val uid = _currentUser.value?.uid
            if (_isSignedIn.value && !uid.isNullOrBlank()) {
                repository.saveBookToFirestore(uid, page.bookId)
            }
        }
    }

    fun addNewPage(bookId: Long) {
        viewModelScope.launch {
            val currentPages = _activePages.value
            val newPageNum = currentPages.size + 1
            val newPage = PageEntity(
                bookId = bookId,
                pageNumber = newPageNum,
                text = "And then, a new magical surprise danced into view...",
                illustrationPrompt = "A whimsical storybook scene with sparkling starlight and friendly woodland friends",
                sceneDescription = "New scene $newPageNum",
                mood = "Wonder",
                location = "Enchanted Meadow"
            )
            repository.insertPage(newPage)
            val uid = _currentUser.value?.uid
            if (_isSignedIn.value && !uid.isNullOrBlank()) {
                repository.saveBookToFirestore(uid, bookId)
            }
        }
    }

    fun purchaseCredits(amount: Int) {
        _credits.value += amount
        val uid = _currentUser.value?.uid
        if (!uid.isNullOrBlank()) updateStoredCredits(uid, _credits.value)
    }

    fun upgradePlan(planName: String) {
        _userPlan.value = planName
        if (planName == "StorySpark Pro") _credits.value += 100
        if (planName == "Creator Plan") _credits.value += 250
        val uid = _currentUser.value?.uid
        if (!uid.isNullOrBlank()) updateStoredCredits(uid, _credits.value)
    }

    fun processPayment(
        itemTitle: String,
        amount: Double,
        method: PaymentMethodType,
        creditsToAdd: Int = 0,
        newPlan: String? = null
    ): String {
        val receiptNum = "REC-SPARK-${System.currentTimeMillis() % 1000000}"
        val record = TransactionRecord(
            id = "TXN_${System.currentTimeMillis()}",
            title = itemTitle,
            amount = amount,
            currency = "USD",
            timestamp = System.currentTimeMillis(),
            paymentMethod = method,
            status = "Completed",
            receiptNumber = receiptNum,
            description = if (newPlan != null) "Upgraded subscription to $newPlan" else "Purchased $creditsToAdd Generation Credits"
        )
        _transactions.value = listOf(record) + _transactions.value

        if (creditsToAdd > 0) {
            _credits.value += creditsToAdd
        }
        val uid = _currentUser.value?.uid
        if (!uid.isNullOrBlank()) updateStoredCredits(uid, _credits.value)
        if (newPlan != null) {
            _userPlan.value = newPlan
            _subscriptionDetails.value = SubscriptionDetails(
                planName = newPlan,
                price = amount,
                billingCycle = "Monthly",
                nextRenewalDate = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
                autoRenew = true,
                status = "Active"
            )
        }
        return receiptNum
    }

    fun redeemVoucher(code: String): Pair<Boolean, String> {
        val cleanCode = code.trim().uppercase()
        return when (cleanCode) {
            "TEACHER2026" -> {
                val receipt = processPayment("Educator Grant (TEACHER2026)", 0.00, PaymentMethodType.EDUCATOR_VOUCHER, 100, "StorySpark Pro")
                Pair(true, "Voucher Applied! +100 Credits & Pro Plan granted (Receipt #$receipt)")
            }
            "HOMESCHOOL" -> {
                val receipt = processPayment("Homeschooler Grant (HOMESCHOOL)", 0.00, PaymentMethodType.EDUCATOR_VOUCHER, 50, null)
                Pair(true, "Voucher Applied! +50 Credits granted (Receipt #$receipt)")
            }
            "SPARKGIFT" -> {
                val receipt = processPayment("Gift Voucher (SPARKGIFT)", 0.00, PaymentMethodType.EDUCATOR_VOUCHER, 25, null)
                Pair(true, "Voucher Applied! +25 Credits granted (Receipt #$receipt)")
            }
            "KDPCREATOR" -> {
                val receipt = processPayment("KDP Author Grant (KDPCREATOR)", 0.00, PaymentMethodType.EDUCATOR_VOUCHER, 250, "Creator Plan")
                Pair(true, "Voucher Applied! +250 Credits & Creator Plan granted (Receipt #$receipt)")
            }
            else -> {
                Pair(false, "Invalid voucher code. Try 'TEACHER2026' or 'HOMESCHOOL'")
            }
        }
    }

    fun cancelSubscription() {
        val curr = _subscriptionDetails.value
        _subscriptionDetails.value = curr.copy(
            autoRenew = false,
            status = "Cancels at period end"
        )
    }

    fun savePaymentMethod(method: SavedPaymentMethod) {
        _savedPaymentMethods.value = _savedPaymentMethods.value + method
    }
}

data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String? = null,
    val avatarInitials: String = "",
    val authProvider: String = "",
    val joinedDate: String = "",
    val isEmailVerified: Boolean = false
)

enum class AppThemeMode {
    BRIGHT,
    DARK
}
