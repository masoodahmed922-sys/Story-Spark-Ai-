package com.example.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

class BookRepository(
    private val bookDao: BookDao,
    val firestoreRepo: FirestoreBookRepository = FirestoreBookRepository()
) {
    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getBooksForUser(userId: String): Flow<List<BookEntity>> = bookDao.getBooksForUser(userId)

    suspend fun getBooksForUserSync(userId: String): List<BookEntity> = bookDao.getBooksForUserSync(userId)

    fun getBookById(id: Long): Flow<BookEntity?> = bookDao.getBookById(id)

    fun getPagesForBook(bookId: Long): Flow<List<PageEntity>> = bookDao.getPagesForBook(bookId)

    fun getCharactersForBook(bookId: Long): Flow<List<CharacterEntity>> = bookDao.getCharactersForBook(bookId)

    fun getTranslationsForBook(bookId: Long): Flow<List<BookTranslationEntity>> = bookDao.getTranslationsForBook(bookId)

    suspend fun getTranslation(bookId: Long, langCode: String): BookTranslationEntity? =
        bookDao.getTranslation(bookId, langCode)

    suspend fun saveTranslation(bookId: Long, translation: BookTranslationData) {
        val entity = BookTranslationEntity(
            bookId = bookId,
            langCode = translation.langCode,
            title = translation.title,
            moral = translation.moral,
            pagesJson = translation.toJson().getJSONArray("pages").toString(),
            status = translation.status,
            model = translation.model,
            updatedAt = translation.updatedAt
        )
        bookDao.insertTranslation(entity)

        // Also update translationsJson in BookEntity for quick reference
        val currentBook = bookDao.getAllBooksList().find { it.id == bookId }
        if (currentBook != null) {
            val root = try {
                JSONObject(currentBook.translationsJson.ifBlank { "{}" })
            } catch (e: Exception) {
                JSONObject()
            }
            root.put(translation.langCode, translation.toJson())
            bookDao.updateBook(currentBook.copy(translationsJson = root.toString()))
        }
    }

    suspend fun deleteTranslation(bookId: Long, langCode: String) {
        bookDao.deleteTranslation(bookId, langCode)
        val currentBook = bookDao.getAllBooksList().find { it.id == bookId }
        if (currentBook != null) {
            val root = try {
                JSONObject(currentBook.translationsJson.ifBlank { "{}" })
            } catch (e: Exception) {
                JSONObject()
            }
            root.remove(langCode)
            bookDao.updateBook(currentBook.copy(translationsJson = root.toString()))
        }
    }

    suspend fun insertBookWithDetails(
        book: BookEntity,
        pages: List<PageEntity>,
        characters: List<CharacterEntity>
    ): Long {
        val newBookId = bookDao.insertBook(book)
        val pagesWithId = pages.map { it.copy(bookId = newBookId) }
        val charsWithId = characters.map { it.copy(bookId = newBookId) }
        bookDao.insertPages(pagesWithId)
        bookDao.insertCharacters(charsWithId)
        return newBookId
    }

    suspend fun updateBook(book: BookEntity) = bookDao.updateBook(book)

    suspend fun deleteBook(book: BookEntity) = bookDao.deleteBook(book)

    suspend fun updatePage(page: PageEntity) = bookDao.updatePage(page)

    suspend fun deletePage(page: PageEntity) = bookDao.deletePage(page)

    suspend fun insertPage(page: PageEntity) = bookDao.insertPage(page)

    suspend fun reorderPages(pages: List<PageEntity>) {
        pages.forEachIndexed { index, page ->
            bookDao.updatePage(page.copy(pageNumber = index + 1))
        }
    }

    suspend fun duplicateBook(
        book: BookEntity,
        pages: List<PageEntity>,
        characters: List<CharacterEntity>
    ): Long {
        val duplicatedBook = book.copy(
            id = 0,
            title = "${book.title} (Copy)",
            createdAt = System.currentTimeMillis()
        )
        return insertBookWithDetails(duplicatedBook, pages, characters)
    }

    suspend fun repairCorruptedBooks(): Int {
        var repairedCount = 0
        val allPages = bookDao.getAllPagesList()
        val corruptedBookIds = mutableSetOf<Long>()

        allPages.forEach { page ->
            val clean = AppDatabase.Companion.cleanCorruptedText(page.text, page.bookId, page.pageNumber)
            if (clean != page.text) {
                bookDao.updatePage(page.copy(text = clean))
                corruptedBookIds.add(page.bookId)
            }
        }
        repairedCount = corruptedBookIds.size
        return repairedCount
    }

    suspend fun saveBookToFirestore(userId: String, bookId: Long) {
        if (userId.isBlank()) return
        val book = bookDao.getBookByIdSync(bookId) ?: return
        val pages = bookDao.getPagesForBookSync(bookId)
        val chars = bookDao.getCharactersForBookSync(bookId)
        firestoreRepo.saveBook(userId, book, pages, chars)
    }

    suspend fun deleteBookFromFirestore(userId: String, bookId: Long) {
        if (userId.isBlank()) return
        firestoreRepo.deleteBook(userId, bookId)
    }

    suspend fun syncBooksFromFirestore(userId: String): Int {
        if (userId.isBlank()) return 0
        val result = firestoreRepo.loadBooks(userId)
        val cloudBooks = result.getOrNull() ?: return 0
        var syncedCount = 0

        val localBooks = bookDao.getAllBooksList()
        val localBookMap = localBooks.associateBy { it.id }

        cloudBooks.forEach { cloudBook ->
            val localBook = localBookMap[cloudBook.id] ?: localBooks.find { it.title.equals(cloudBook.title, ignoreCase = true) }
            val targetId = localBook?.id ?: cloudBook.id

            val bookEntity = BookEntity(
                id = targetId,
                userId = userId,
                title = cloudBook.title,
                subtitle = cloudBook.subtitle,
                description = cloudBook.description,
                authorName = cloudBook.authorName,
                authorBio = cloudBook.authorBio,
                coverImageRes = cloudBook.coverImageRes,
                coverStyle = cloudBook.coverStyle,
                titlePosition = cloudBook.titlePosition,
                targetAge = cloudBook.targetAge,
                storyType = cloudBook.storyType,
                storyTone = cloudBook.storyTone,
                readingLevel = cloudBook.readingLevel,
                artStyle = cloudBook.artStyle,
                pageLayout = cloudBook.pageLayout,
                moralLesson = cloudBook.moralLesson,
                storyDnaJson = cloudBook.storyDnaJson,
                createdAt = if (cloudBook.createdAt > 0) cloudBook.createdAt else System.currentTimeMillis(),
                pageCount = if (cloudBook.pageCount > 0) cloudBook.pageCount else cloudBook.pages.size,
                isFavorite = cloudBook.isFavorite,
                language = cloudBook.language,
                originalBookId = cloudBook.originalBookId,
                translatedFrom = cloudBook.translatedFrom,
                translationsJson = cloudBook.translationsJson
            )

            val finalId = bookDao.insertBook(bookEntity)

            val pageEntities = cloudBook.pages.map { p ->
                PageEntity(
                    bookId = finalId,
                    pageNumber = p.pageNumber,
                    text = p.text,
                    illustrationPrompt = p.illustrationPrompt,
                    sceneDescription = p.sceneDescription,
                    mood = p.mood,
                    location = p.location,
                    charactersPresent = p.charactersPresent,
                    importantObjects = p.importantObjects,
                    imageUri = p.imageUri
                )
            }
            if (pageEntities.isNotEmpty()) {
                bookDao.deletePagesForBook(finalId)
                bookDao.insertPages(pageEntities)
            }

            val charEntities = cloudBook.characters.map { c ->
                CharacterEntity(
                    bookId = finalId,
                    name = c.name,
                    type = c.type,
                    age = c.age,
                    personality = c.personality,
                    favoriteThing = c.favoriteThing,
                    specialAbility = c.specialAbility,
                    appearance = c.appearance,
                    role = c.role,
                    isMainCharacter = c.isMainCharacter
                )
            }
            if (charEntities.isNotEmpty()) {
                bookDao.deleteCharactersForBook(finalId)
                bookDao.insertCharacters(charEntities)
            }

            syncedCount++
        }

        // Also push local books that are not in Firestore to Firestore
        val cloudIds = cloudBooks.map { it.bookId }.toSet()
        val cloudTitles = cloudBooks.map { it.title.trim().lowercase() }.toSet()
        localBooks.forEach { local ->
            if (!cloudIds.contains(local.id.toString()) && !cloudTitles.contains(local.title.trim().lowercase())) {
                val pages = bookDao.getPagesForBookSync(local.id)
                val chars = bookDao.getCharactersForBookSync(local.id)
                firestoreRepo.saveBook(userId, local, pages, chars)
            }
        }

        return syncedCount
    }
}
