package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Data model for Book document in Cloud Firestore under:
 * users/{uid}/books/{bookId}
 */
data class FirestoreBookModel(
    val bookId: String = "",
    val id: Long = 0,
    val title: String = "",
    val subtitle: String = "",
    val description: String = "",
    val authorName: String = "",
    val authorBio: String = "",
    val coverImageRes: String = "",
    val coverStyle: String = "",
    val titlePosition: String = "",
    val targetAge: String = "",
    val storyType: String = "",
    val storyTone: String = "",
    val readingLevel: String = "",
    val artStyle: String = "",
    val pageLayout: String = "",
    val moralLesson: String = "",
    val storyDnaJson: String = "",
    val createdAt: Long = 0,
    val pageCount: Int = 0,
    val isFavorite: Boolean = false,
    val language: String = "en",
    val originalBookId: Long? = null,
    val translatedFrom: String? = null,
    val translationsJson: String = "{}",
    val userId: String = "",
    val updatedAt: Long = 0,
    val pages: List<FirestorePageModel> = emptyList(),
    val characters: List<FirestoreCharacterModel> = emptyList()
)

data class FirestorePageModel(
    val pageNumber: Int = 1,
    val text: String = "",
    val illustrationPrompt: String = "",
    val sceneDescription: String = "",
    val mood: String = "",
    val location: String = "",
    val charactersPresent: String = "",
    val importantObjects: String = "",
    val imageUri: String = ""
)

data class FirestoreCharacterModel(
    val name: String = "",
    val type: String = "Animal",
    val age: String = "6",
    val personality: String = "",
    val favoriteThing: String = "",
    val specialAbility: String = "",
    val appearance: String = "",
    val role: String = "Main Character",
    val isMainCharacter: Boolean = true
)

class FirestoreBookRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "FirestoreBookRepo"
    }

    /**
     * Saves or updates a storybook document under:
     * users/{uid}/books/{bookId}
     */
    suspend fun saveBook(
        userId: String,
        book: BookEntity,
        pages: List<PageEntity>,
        characters: List<CharacterEntity>
    ): Result<Unit> {
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        val bookDocId = book.id.toString()
        val firestoreModel = FirestoreBookModel(
            bookId = bookDocId,
            id = book.id,
            title = book.title,
            subtitle = book.subtitle,
            description = book.description,
            authorName = book.authorName,
            authorBio = book.authorBio,
            coverImageRes = book.coverImageRes,
            coverStyle = book.coverStyle,
            titlePosition = book.titlePosition,
            targetAge = book.targetAge,
            storyType = book.storyType,
            storyTone = book.storyTone,
            readingLevel = book.readingLevel,
            artStyle = book.artStyle,
            pageLayout = book.pageLayout,
            moralLesson = book.moralLesson,
            storyDnaJson = book.storyDnaJson,
            createdAt = book.createdAt,
            pageCount = if (book.pageCount > 0) book.pageCount else pages.size,
            isFavorite = book.isFavorite,
            language = book.language,
            originalBookId = book.originalBookId,
            translatedFrom = book.translatedFrom,
            translationsJson = book.translationsJson,
            userId = userId,
            updatedAt = System.currentTimeMillis(),
            pages = pages.map { page ->
                FirestorePageModel(
                    pageNumber = page.pageNumber,
                    text = page.text,
                    illustrationPrompt = page.illustrationPrompt,
                    sceneDescription = page.sceneDescription,
                    mood = page.mood,
                    location = page.location,
                    charactersPresent = page.charactersPresent,
                    importantObjects = page.importantObjects,
                    imageUri = page.imageUri
                )
            },
            characters = characters.map { char ->
                FirestoreCharacterModel(
                    name = char.name,
                    type = char.type,
                    age = char.age,
                    personality = char.personality,
                    favoriteThing = char.favoriteThing,
                    specialAbility = char.specialAbility,
                    appearance = char.appearance,
                    role = char.role,
                    isMainCharacter = char.isMainCharacter
                )
            }
        )

        return try {
            firestore.collection("users")
                .document(userId)
                .collection("books")
                .document(bookDocId)
                .set(firestoreModel, SetOptions.merge())
                .await()
            Log.d(TAG, "Successfully saved book '$bookDocId' for user '$userId' to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save book '$bookDocId' to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a storybook from users/{uid}/books/{bookId}
     */
    suspend fun deleteBook(userId: String, bookId: Long): Result<Unit> {
        if (userId.isBlank()) return Result.success(Unit)
        val bookDocId = bookId.toString()
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("books")
                .document(bookDocId)
                .delete()
                .await()
            Log.d(TAG, "Deleted book '$bookDocId' from Firestore for user '$userId'")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete book '$bookDocId' from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Loads all storybooks saved under users/{uid}/books
     */
    suspend fun loadBooks(userId: String): Result<List<FirestoreBookModel>> {
        if (userId.isBlank()) return Result.success(emptyList())
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("books")
                .get()
                .await()

            val books = snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirestoreBookModel::class.java)?.copy(
                    bookId = doc.id
                )
            }
            Log.d(TAG, "Loaded ${books.size} books from Firestore for user '$userId'")
            Result.success(books)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load books from Firestore for user '$userId': ${e.message}", e)
            Result.failure(e)
        }
    }
}
