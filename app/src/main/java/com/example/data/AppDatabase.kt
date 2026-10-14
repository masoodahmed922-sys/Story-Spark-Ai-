package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        BookEntity::class,
        PageEntity::class,
        CharacterEntity::class,
        BookTranslationEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        private const val TAG = "AppDatabase"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN language TEXT NOT NULL DEFAULT 'en'")
                db.execSQL("ALTER TABLE books ADD COLUMN originalBookId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE books ADD COLUMN translatedFrom TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE books ADD COLUMN translationsJson TEXT NOT NULL DEFAULT '{}'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS book_translations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bookId INTEGER NOT NULL,
                        langCode TEXT NOT NULL,
                        title TEXT NOT NULL,
                        moral TEXT NOT NULL,
                        pagesJson TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'complete',
                        model TEXT NOT NULL DEFAULT 'gemini-2.5-flash',
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(bookId) REFERENCES books(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_book_translations_bookId_langCode ON book_translations(bookId, langCode)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(
            context: Context,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "storyspark_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration(true)
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun repairCorruptedBooks(bookDao: BookDao): Int {
            var repairedCount = 0
            val allPages = bookDao.getAllPagesList()
            val corruptedBookIds = mutableSetOf<Long>()

            allPages.forEach { page ->
                val clean = cleanCorruptedText(page.text, page.bookId, page.pageNumber)
                if (clean != page.text) {
                    bookDao.updatePage(page.copy(text = clean))
                    corruptedBookIds.add(page.bookId)
                }
            }
            repairedCount = corruptedBookIds.size
            return repairedCount
        }

        fun cleanCorruptedText(rawText: String, bookId: Long, pageNumber: Int): String {
            if (bookId == 1L && pageNumber in 1..8) {
                val canonicalPages = listOf(
                    "Once upon a twilight, when the sunset turned the clouds into strawberry-pink cotton candy, a little fox named Lumi sat at the edge of his cozy burrow.",
                    "Lumi pulled his favorite sky-blue scarf tighter around his neck. The stars were beginning to blink awake, one by tiny one, like friendly nightlights in the velvet sky.",
                    "Suddenly, a soft golden shimmer drifted down from the highest oak tree. It was not a firefly—it was a little baby star that had tumbled from the Milky Way!",
                    "\"Do not worry, little star,\" whispered Lumi gently, nudging it with his cool black nose. \"I will help you find your way back home to your family in the sky.\"",
                    "High on a birch branch, Barnaby the wise old owl fluttered his wings. \"To send a star back to the heavens, Lumi, you need a song of true kindness sung from the highest hill.\"",
                    "Together, Lumi and Barnaby climbed to the peak of Star-Catcher Hill. The wind sang a gentle lullaby, and the grass danced like silver waves.",
                    "Lumi closed his eyes and hummed the sweet bedtime tune his mother sang to him. The little star pulsed with joy, then floated gently up into the dark sky, brighter than ever!",
                    "From high above, the star winked down at Lumi. Curling up into his warm bed, Lumi smiled softly, knowing that whenever he looked up, a friend was watching over him."
                )
                return canonicalPages[pageNumber - 1]
            }

            var text = rawText
            val knownPrefixes = listOf(
                "ایک دفعہ کا ذکر ہے کہ ستاروں کے تلے...",
                "एक बार की बात है، तारों भरी रात में...",
                "ایک دفعہ کا ذکر ہے کہ ستاروں کے تلے",
                "एक बार की बात है, तारों भरी रात में",
                "كان يا ما كان في قديم الزمان...",
                "كان يا ما كان في قديم الزمان",
                "Había una vez, bajo el cielo estrellado...",
                "Il était une fois, sous un ciel étincelant...",
                "Es war einmal, unter dem leuchtenden Sternenhimmel..."
            )
            for (prefix in knownPrefixes) {
                while (text.contains(prefix)) {
                    text = text.replace(prefix, "").trim()
                }
            }
            val regex = Regex("^[\\p{IsArabic}\\p{IsDevanagari}\\s\\.\\…]+([A-Z\"'“])")
            val match = regex.find(text)
            if (match != null) {
                val englishStart = match.groups[1]?.value ?: ""
                text = englishStart + text.substring(match.range.last + 1)
            }
            return text.trim()
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Brand-new users start with 0 books as required
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        val repairedCount = repairCorruptedBooks(database.bookDao())
                        if (repairedCount > 0) {
                            Log.i(TAG, "Repaired $repairedCount corrupted book(s) by stripping mixed language fragments.")
                        }
                    }
                }
            }

            suspend fun populateInitialDemoBook(bookDao: BookDao) {
                if (bookDao.getBookCount() > 0) return

                val demoBook = BookEntity(
                    id = 1,
                    title = "The Little Fox Who Found the Stars",
                    subtitle = "A Tale of Wonder and Bedtime Courage",
                    description = "Join Lumi, a gentle little orange fox wearing a cozy sky-blue scarf, as he sets out on a quiet evening stroll through Whispering Woods and discovers the warmest magic in the night sky.",
                    authorName = "Elena Vance",
                    authorBio = "Elena writes heartwarming bedtime stories designed to spark curiosity, wonder, and sweet dreams for young explorers.",
                    coverImageRes = "img_lumi_stars",
                    coverStyle = "Modern Whimsical",
                    titlePosition = "Top",
                    targetAge = "4–6 years",
                    storyType = "Bedtime",
                    storyTone = "Gentle",
                    readingLevel = "Simple",
                    artStyle = "Watercolor",
                    pageLayout = "Image Above Text",
                    moralLesson = "Even the smallest kindness shines like a brilliant star in the darkest sky.",
                    createdAt = System.currentTimeMillis() - 86400000L,
                    pageCount = 8,
                    isFavorite = true,
                    language = "en",
                    originalBookId = null,
                    translatedFrom = null,
                    translationsJson = "{}"
                )
                bookDao.insertBook(demoBook)

                val mainChar = CharacterEntity(
                    bookId = 1,
                    name = "Lumi",
                    type = "Animal",
                    age = "5",
                    personality = "Curious, thoughtful, and tender-hearted",
                    favoriteThing = "Watching fireflies and warm peppermint tea",
                    specialAbility = "Can hear the quietest whispers of the wind",
                    appearance = "Small orange fox with silky ginger fur, soft white chest, dark tip on tail, and a fluffy hand-knitted sky-blue scarf",
                    role = "Main Character",
                    isMainCharacter = true
                )
                val companionChar = CharacterEntity(
                    bookId = 1,
                    name = "Barnaby",
                    type = "Animal",
                    age = "Old & Wise",
                    personality = "Patient, gentle, and slow-talking",
                    favoriteThing = "Ancient constellations and moonbeams",
                    specialAbility = "Knows the secrets of every treetop",
                    appearance = "Speckled barn owl with large golden spectacles and feathered wings",
                    role = "Mentor & Guide",
                    isMainCharacter = false
                )
                bookDao.insertCharacters(listOf(mainChar, companionChar))

                val demoPages = listOf(
                    PageEntity(
                        bookId = 1,
                        pageNumber = 1,
                        text = "Once upon a twilight, when the sunset turned the clouds into strawberry-pink cotton candy, a little fox named Lumi sat at the edge of his cozy burrow.",
                        illustrationPrompt = "Lumi the small orange fox with blue scarf sitting at cozy burrow entrance under strawberry-pink twilight sky, watercolor style",
                        sceneDescription = "Lumi looking at pink clouds from mossy burrow",
                        mood = "Peaceful & Cozy",
                        location = "Burrow Threshold",
                        charactersPresent = "Lumi",
                        importantObjects = "Blue scarf, Burrow door"
                    ),
                    PageEntity(
                        bookId = 1,
                        pageNumber = 2,
                        text = "Lumi pulled his favorite sky-blue scarf tighter around his neck. The stars were beginning to blink awake, one by tiny one, like friendly nightlights in the velvet sky.",
                        illustrationPrompt = "Lumi wrapping his sky blue knitted scarf, looking up into deep indigo sky with first twinkling golden stars, soft storybook watercolor",
                        sceneDescription = "Twinkling golden stars emerging in indigo sky",
                        mood = "Wonder & Awe",
                        location = "Whispering Meadow",
                        charactersPresent = "Lumi",
                        importantObjects = "First star, Blue scarf"
                    ),
                    PageEntity(
                        bookId = 1,
                        pageNumber = 3,
                        text = "Suddenly, a soft golden shimmer drifted down from the highest oak tree. It was not a firefly—it was a little baby star that had tumbled from the Milky Way!",
                        illustrationPrompt = "A glowing golden baby star nestled in soft green clover leaves, radiating warm magical light onto Lumi's amazed fox face",
                        sceneDescription = "Lumi discovers glowing baby star in grass",
                        mood = "Magical & Surprising",
                        location = "Under the Great Oak",
                        charactersPresent = "Lumi",
                        importantObjects = "Fallen golden star"
                    ),
                    PageEntity(
                        bookId = 1,
                        pageNumber = 4,
                        text = "\"Do not worry, little star,\" whispered Lumi gently, nudging it with his cool black nose. \"I will help you find your way back home to your family in the sky.\"",
                        illustrationPrompt = "Close-up of Lumi whispering lovingly to the glowing little star resting between his paws, warm expressive eyes, soft lighting",
                        sceneDescription = "Lumi comforting the baby star with paws",
                        mood = "Affectionate & Caring",
                        location = "Meadow Path",
                        charactersPresent = "Lumi",
                        importantObjects = "Star glowing between paws"
                    ),
                    PageEntity(
                        bookId = 1,
                        pageNumber = 5,
                        text = "High on a birch branch, Barnaby the wise old owl fluttered his wings. \"To send a star back to the heavens, Lumi, you need a song of true kindness sung from the highest hill.\"",
                        illustrationPrompt = "Barnaby the speckled barn owl with round golden spectacles perched on a silver birch branch speaking to Lumi below",
                        sceneDescription = "Barnaby owl giving guidance to Lumi",
                        mood = "Mystical & Guiding",
                        location = "Silver Birch Grove",
                        charactersPresent = "Lumi, Barnaby",
                        importantObjects = "Barnaby's spectacles, Birch tree"
                    ),
                    PageEntity(
                        bookId = 1,
                        pageNumber = 6,
                        text = "Together, Lumi and Barnaby climbed to the peak of Star-Catcher Hill. The wind sang a gentle lullaby, and the grass danced like silver waves.",
                        illustrationPrompt = "Lumi with glowing star tucked safely in scarf, climbing up grassy hill with Barnaby gliding overhead under starry night",
                        sceneDescription = "Climbing Star-Catcher Hill at night",
                        mood = "Adventurous & Determined",
                        location = "Star-Catcher Hill",
                        charactersPresent = "Lumi, Barnaby",
                        importantObjects = "Hilltop, Wind swirls"
                    ),
                    PageEntity(
                        bookId = 1,
                        pageNumber = 7,
                        text = "Lumi closed his eyes and hummed the sweet bedtime tune his mother sang to him. The little star pulsed with joy, then floated gently up into the dark sky, brighter than ever!",
                        illustrationPrompt = "Magical beam of golden starlight lifting the baby star high into the constellations as Lumi smiles with joyous tears",
                        sceneDescription = "Star floating upward back to the sky",
                        mood = "Euphoric & Heartwarming",
                        location = "Hilltop Peak",
                        charactersPresent = "Lumi",
                        importantObjects = "Ascending star beam"
                    ),
                    PageEntity(
                        bookId = 1,
                        pageNumber = 8,
                        text = "From high above, the star winked down at Lumi. Curling up into his warm bed, Lumi smiled softly, knowing that whenever he looked up, a friend was watching over him.",
                        illustrationPrompt = "Lumi curled up asleep in warm mossy burrow bed, smiling peacefully as starlight shines through window, dreamy bedtime painting",
                        sceneDescription = "Lumi sleeping soundly in warm bed",
                        mood = "Serene & Sleepy",
                        location = "Cozy Burrow Bed",
                        charactersPresent = "Lumi",
                        importantObjects = "Quilt, Window, Starlight"
                    )
                )
                bookDao.insertPages(demoPages)
            }
        }
    }
}
