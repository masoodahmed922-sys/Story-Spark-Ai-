package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY createdAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE userId = :userId ORDER BY createdAt DESC")
    fun getBooksForUser(userId: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getBooksForUserSync(userId: String): List<BookEntity>

    @Query("DELETE FROM books WHERE userId = :userId")
    suspend fun deleteBooksForUser(userId: String)

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM pages WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun getPagesForBook(bookId: Long): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE bookId = :bookId ORDER BY pageNumber ASC")
    suspend fun getPagesForBookSync(bookId: Long): List<PageEntity>

    @Query("SELECT * FROM characters WHERE bookId = :bookId ORDER BY isMainCharacter DESC, id ASC")
    fun getCharactersForBook(bookId: Long): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE bookId = :bookId ORDER BY isMainCharacter DESC, id ASC")
    suspend fun getCharactersForBookSync(bookId: Long): List<CharacterEntity>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookByIdSync(id: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<PageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: PageEntity): Long

    @Update
    suspend fun updatePage(page: PageEntity)

    @Delete
    suspend fun deletePage(page: PageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacters(characters: List<CharacterEntity>)

    @Query("DELETE FROM pages WHERE bookId = :bookId")
    suspend fun deletePagesForBook(bookId: Long)

    @Query("DELETE FROM characters WHERE bookId = :bookId")
    suspend fun deleteCharactersForBook(bookId: Long)

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    @Query("SELECT * FROM books")
    suspend fun getAllBooksList(): List<BookEntity>

    @Query("SELECT * FROM pages")
    suspend fun getAllPagesList(): List<PageEntity>

    @Query("SELECT * FROM book_translations WHERE bookId = :bookId")
    fun getTranslationsForBook(bookId: Long): Flow<List<BookTranslationEntity>>

    @Query("SELECT * FROM book_translations WHERE bookId = :bookId AND langCode = :langCode LIMIT 1")
    suspend fun getTranslation(bookId: Long, langCode: String): BookTranslationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(translation: BookTranslationEntity): Long

    @Query("DELETE FROM book_translations WHERE bookId = :bookId AND langCode = :langCode")
    suspend fun deleteTranslation(bookId: Long, langCode: String)
}
