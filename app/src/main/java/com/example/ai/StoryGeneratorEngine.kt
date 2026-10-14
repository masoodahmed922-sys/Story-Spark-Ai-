package com.example.ai

import com.example.BuildConfig
import com.example.data.CharacterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Rich DNA blueprint describing the creative genetics of every children's storybook.
 * Stored internally so the exact concepts, character bible, and color styling remain consistent.
 */
data class StoryDna(
    val storySeed: Long = Random.nextLong(),
    val characterProfile: String = "",
    val setting: String = "",
    val centralGoal: String = "",
    val conflict: String = "",
    val supportingCharacters: String = "",
    val uniqueEvent: String = "",
    val emotionalArc: String = "",
    val storyStructure: String = "",
    val visualStyle: String = "Watercolor",
    val colorPalette: String = "Warm golden pastel, amber, soft sky-blue",
    val moral: String = "",
    val vocabularyLevel: String = "Simple",
    val humorLevel: String = "Gentle smiles",
    val imaginationLevel: String = "High magical wonder",
    val randomizationSeed: String = Random.nextInt(100000, 999999).toString()
)

/**
 * Creative prompt suggestion presented to users for "Surprise Me".
 */
data class DynamicStorySuggestion(
    val id: String,
    val title: String,
    val category: String,
    val prompt: String,
    val recommendedAge: String,
    val recommendedTone: String,
    val characterName: String,
    val characterType: String,
    val characterAppearance: String,
    val dna: StoryDna
)

object StoryGeneratorEngine {
    private const val TAG = "StoryGeneratorEngine"
    private const val GEMINI_MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent"

    // Keep memory of recent story titles & concepts to strictly enforce freshness and prevent repetition
    private val recentConceptsHistory = mutableListOf<String>()
    private const val MAX_HISTORY_TRACK = 16

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isBlank() || key == "MY_GEMINI_API_KEY") "" else key
        } catch (e: Throwable) {
            ""
        }
    }

    fun recordUsedConcept(titleOrPrompt: String) {
        synchronized(recentConceptsHistory) {
            if (titleOrPrompt.isNotBlank()) {
                recentConceptsHistory.add(titleOrPrompt.lowercase().take(80))
                if (recentConceptsHistory.size > MAX_HISTORY_TRACK) {
                    recentConceptsHistory.removeAt(0)
                }
            }
        }
    }

    private fun isTooSimilarToRecent(concept: String): Boolean {
        val lower = concept.lowercase()
        synchronized(recentConceptsHistory) {
            return recentConceptsHistory.any { recent ->
                val wordsA = recent.split(" ").filter { it.length > 3 }.toSet()
                val wordsB = lower.split(" ").filter { it.length > 3 }.toSet()
                if (wordsA.isEmpty() || wordsB.isEmpty()) false
                else {
                    val overlap = wordsA.intersect(wordsB).size
                    val similarity = overlap.toFloat() / maxOf(wordsA.size, wordsB.size).coerceAtLeast(1)
                    similarity > 0.45f
                }
            }
        }
    }

    // --- Dynamic Elements Pool (Diverse Combinatorial Seed Engine) ---
    private val CHARACTER_TYPES = listOf(
        "Gentle little bear cub", "Inquisitive red panda", "Curious young astronaut",
        "Baby sea turtle", "Playful cloud nymph", "Miniature clockwork robot",
        "Clever woodland badger", "Little girl inventor", "Daydreaming otter",
        "Tiny winged baby dragon", "Cheerful hedgehog baker", "Star-gazing puppy",
        "Wandering origami crane", "Spirited pony with silver hooves", "Brave little firefly",
        "Imaginative farm boy", "Shy mountain chamois", "Friendly humming-beetle"
    )

    private val PERSONALITIES = listOf(
        "Kind-hearted, slightly clumsy, always cheerful",
        "Quietly observant, deeply loyal, loves mysteries",
        "Bold, enthusiastically curious, quick to help others",
        "Timid at first, remarkably brave when a friend is in need",
        "Playful, creative tinkerer with endless silly jokes",
        "Gentle, patient, loves humming warm melodies",
        "Determined, quick-witted, fascinated by nature's secrets"
    )

    private val SETTINGS = listOf(
        "A whispering whispering pine forest where mushrooms glow periwinkle at dusk",
        "A peaceful floating cloud bakery high above the rainbow ridge",
        "A sunken coral kingdom with shimmering lavender anemones and singing sea-shells",
        "A sunlit treehouse laboratory filled with gentle windchimes and magnifying glasses",
        "The sleepy crater hills of the moon where stardust gathers like morning dew",
        "An ancient cobbled village surrounded by giant golden sunflowers",
        "A misty emerald valley where waterfalls flow with sparkling apple-cider mist",
        "A cozy secret library nestled inside the trunk of a thousand-year-old oak tree",
        "A crystal cavern filled with harmonic stalactites that play lullabies",
        "A quiet twilight harbor where paper boats carry gentle dreams to sea"
    )

    private val CONFLICTS = listOf(
        "The village chime tree has gone silent because its silver bell slipped into the moss",
        "A lost baby constellation needs to find its mother star before the dawn arrives",
        "The seasonal rainbow lost its purple stripe during a gust of giggling wind",
        "A friendly traveling snail forgot which path leads to the annual honey-bun picnic",
        "The garden's friendly shadows became separated from their playful daytime objects",
        "The clockwork town clock stopped ticking because a tiny squirrel hid an acorn in the gear",
        "The twilight lanterns won't glow until someone finds three words of true kindness"
    )

    private val GOALS = listOf(
        "Deliver a warm batch of star-berry muffins across the enchanted river",
        "Help a nervous little owl overcome fear of the bright sunshine",
        "Build a patchwork kite sturdy enough to carry a secret wish to the mountaintop",
        "Solve the mystery of why the meadow flowers only bloom when someone giggles",
        "Create the most heartwarming birthday surprise for the lonely lighthouse keeper",
        "Craft a cozy winter nest with woodland friends before the first snowfall"
    )

    private val VISUAL_STYLES = listOf(
        "Watercolor", "3D Animated", "Whimsical Crayon", "Classic Picture Book", "Dreamy Pastel"
    )

    private val COLOR_PALETTES = listOf(
        "Lavender, warm amber, dusty rose, and midnight navy",
        "Mint green, daffodil yellow, soft terracotta, and cream",
        "Celestial azure, shimmering gold, blush pink, and pearl white",
        "Moss green, acorn brown, misty peach, and soft copper",
        "Ocean teal, coral pink, sandy pearl, and sunbeam yellow"
    )

    private val MORALS = listOf(
        "True strength is discovered through gentle patience and a listening heart.",
        "Sharing our humble gifts makes the whole world feel like home.",
        "Mistakes are just delightful stepping stones toward new wonders.",
        "Kindness whispers louder than fear in every shadowed forest.",
        "Being yourself is the greatest magic you will ever possess."
    )

    /**
     * Builds a comprehensive unique Story DNA object.
     */
    fun createStoryDna(
        customIdea: String = "",
        ageGroup: String = "4–6 years",
        artStyle: String = "Watercolor",
        preferredTone: String = "Gentle"
    ): StoryDna {
        val rand = java.util.Random(System.nanoTime())
        val charType = CHARACTER_TYPES[rand.nextInt(CHARACTER_TYPES.size)]
        val personality = PERSONALITIES[rand.nextInt(PERSONALITIES.size)]
        val setting = SETTINGS[rand.nextInt(SETTINGS.size)]
        val conflict = CONFLICTS[rand.nextInt(CONFLICTS.size)]
        val goal = GOALS[rand.nextInt(GOALS.size)]
        val palette = COLOR_PALETTES[rand.nextInt(COLOR_PALETTES.size)]
        val moral = MORALS[rand.nextInt(MORALS.size)]

        return StoryDna(
            storySeed = rand.nextLong(),
            characterProfile = "$charType who is $personality",
            setting = setting,
            centralGoal = goal,
            conflict = conflict,
            supportingCharacters = "A loyal companion and a wise local elder",
            uniqueEvent = "A sudden gentle surprise changes everyone's perspective",
            emotionalArc = "Curiosity → Surprising challenge → Heartfelt teamwork → Cozy fulfillment",
            storyStructure = "Three-act children's picture book cadence with rhythmic refrain",
            visualStyle = artStyle,
            colorPalette = palette,
            moral = moral,
            vocabularyLevel = when {
                ageGroup.contains("2–4") -> "Simple, sensory, rhythmic"
                ageGroup.contains("8–10") -> "Rich descriptive vocabulary with playful dialogue"
                else -> "Accessible, expressive, warm picture book prose"
            },
            humorLevel = if (preferredTone.contains("Funny", ignoreCase = true)) "Playful slapstick and silly rhymes" else "Warm gentle smiles",
            imaginationLevel = "High, delightful wonder"
        )
    }

    /**
     * Generates 3-4 completely dynamic, non-repetitive story suggestions for "Surprise Me".
     * If Gemini API is configured, requests AI generation with explicit anti-repetition rules.
     * Falls back to a robust combinatorial engine with uniqueness checks.
     */
    suspend fun generateDynamicSuggestions(
        count: Int = 4,
        targetAge: String = "4–6 years",
        preferredTone: String = "Gentle",
        artStyle: String = "Watercolor",
        language: String = "English"
    ): List<DynamicStorySuggestion> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNotBlank()) {
            try {
                val suggestions = fetchAiDynamicSuggestions(apiKey, count, targetAge, preferredTone, artStyle, language)
                if (suggestions.isNotEmpty()) {
                    suggestions.forEach { recordUsedConcept(it.title + " " + it.prompt) }
                    return@withContext suggestions
                }
            } catch (e: Exception) {
                // Fallback gracefully
            }
        }

        // Offline / Fallback Combinatorial Generation
        val result = mutableListOf<DynamicStorySuggestion>()
        var attempts = 0
        while (result.size < count && attempts < count * 4) {
            attempts++
            val dna = createStoryDna(ageGroup = targetAge, artStyle = artStyle, preferredTone = preferredTone)
            val charName = listOf("Barnaby", "Lumi", "Pip", "Zara", "Milo", "Kora", "Felix", "Nori", "Jasper").random()
            val categories = listOf("Bedtime", "Animals", "Fantasy", "Space", "Ocean", "Friendship", "Science", "Nature")
            val cat = categories[result.size % categories.size]

            val title = when (cat) {
                "Bedtime" -> "The ${dna.characterProfile.substringBefore(" ")} Who Sang to Starlight"
                "Animals" -> "$charName and the Secret Whispering Brook"
                "Fantasy" -> "The Shimmering Cloud Baker of Mount Sol"
                "Space" -> "$charName's Golden Compass in the Cosmos"
                "Ocean" -> "The Dancing Seahorse of Coral Bay"
                "Friendship" -> "$charName and the Silver Sunflower"
                "Science" -> "The Little Inventor and the Windmill Clock"
                else -> "The Journey to the Moonbeam Meadow"
            }

            val prompt = "${dna.characterProfile} sets out to ${dna.centralGoal.lowercase()}, but discovers ${dna.conflict.lowercase()}."

            if (!isTooSimilarToRecent(prompt) && result.none { it.title == title || it.category == cat }) {
                result.add(
                    DynamicStorySuggestion(
                        id = "dyn_${System.currentTimeMillis()}_${result.size}",
                        title = title,
                        category = cat,
                        prompt = prompt,
                        recommendedAge = targetAge,
                        recommendedTone = preferredTone,
                        characterName = charName,
                        characterType = if (dna.characterProfile.contains("boy") || dna.characterProfile.contains("girl")) "Child" else "Animal",
                        characterAppearance = "${dna.characterProfile}, wearing a cozy jacket with pockets full of keepsakes",
                        dna = dna
                    )
                )
                recordUsedConcept(title + " " + prompt)
            }
        }

        result
    }

    private fun fetchAiDynamicSuggestions(
        apiKey: String,
        count: Int,
        targetAge: String,
        preferredTone: String,
        artStyle: String,
        language: String
    ): List<DynamicStorySuggestion> {
        val recentExclusions = synchronized(recentConceptsHistory) {
            recentConceptsHistory.takeLast(6).joinToString("; ")
        }

        val prompt = """
You are an award-winning children's book author.
Generate exactly $count COMPLETELY DIFFERENT, fresh, original storybook concepts for children aged $targetAge.
Tone: $preferredTone. Art Style: $artStyle. Language: $language.

IMPORTANT CREATIVE VARIETY MANDATE:
- Do NOT reuse tired tropes (e.g. "character gets lost -> meets friend -> solves problem").
- Every single idea must have a DIFFERENT genre (choose from Adventure, Fantasy, Animals, Ocean, Space, Science, Mystery, Kindness).
- Every single idea must have completely distinct characters, settings, unique conflicts, and endings.
- Do NOT repeat these recently generated themes: [${recentExclusions.ifBlank { "None yet" }}].
- Return strictly valid JSON with no markdown wrapping, no backticks.

JSON format:
{
  "suggestions": [
    {
      "title": "Vivid Title",
      "category": "Genre",
      "prompt": "2-sentence creative story premise full of sensory wonder and goal",
      "characterName": "Name",
      "characterType": "Child | Animal | Fantasy Creature | Robot",
      "characterAppearance": "Vivid visual description for illustration consistency",
      "setting": "Unique immersive setting",
      "conflict": "Specific fresh challenge",
      "moral": "Subtle heartwarming lesson",
      "colorPalette": "3-4 harmony colors for art"
    }
  ]
}
""".trimIndent()

        val root = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", prompt)
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        root.put("contents", contentsArray)

        val genConfig = JSONObject()
        genConfig.put("responseMimeType", "application/json")
        genConfig.put("temperature", 0.95) // high creativity and variety
        root.put("generationConfig", genConfig)

        val url = "$BASE_URL?key=$apiKey"
        val body = root.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) return emptyList()

        val jsonResponse = JSONObject(responseBody)
        val candidates = jsonResponse.optJSONArray("candidates") ?: return emptyList()
        val candidate = candidates.optJSONObject(0) ?: return emptyList()
        val text = candidate.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text").orEmpty()

        val cleanJson = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val obj = JSONObject(cleanJson)
        val arr = obj.optJSONArray("suggestions") ?: return emptyList()

        val list = mutableListOf<DynamicStorySuggestion>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val dna = StoryDna(
                storySeed = Random.nextLong(),
                characterProfile = item.optString("characterAppearance"),
                setting = item.optString("setting"),
                centralGoal = item.optString("prompt"),
                conflict = item.optString("conflict"),
                moral = item.optString("moral"),
                visualStyle = artStyle,
                colorPalette = item.optString("colorPalette", "Warm pastel and glowing amber")
            )
            list.add(
                DynamicStorySuggestion(
                    id = "dyn_${System.currentTimeMillis()}_$i",
                    title = item.optString("title"),
                    category = item.optString("category", "Adventure"),
                    prompt = item.optString("prompt"),
                    recommendedAge = targetAge,
                    recommendedTone = preferredTone,
                    characterName = item.optString("characterName", "Hero"),
                    characterType = item.optString("characterType", "Animal"),
                    characterAppearance = item.optString("characterAppearance"),
                    dna = dna
                )
            )
        }
        return list
    }
}
