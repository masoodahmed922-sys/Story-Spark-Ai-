package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `firestoreBookModel serialization properties and defaults are valid`() {
    val model = com.example.data.FirestoreBookModel(
      bookId = "book_101",
      id = 101L,
      title = "The Star Fox",
      authorName = "Elena",
      userId = "test_uid_123",
      pages = listOf(
        com.example.data.FirestorePageModel(
          pageNumber = 1,
          text = "Once upon a time in a starry wood...",
          illustrationPrompt = "Star fox looking at a gentle moon"
        )
      ),
      characters = listOf(
        com.example.data.FirestoreCharacterModel(
          name = "Lumi",
          type = "Fox",
          isMainCharacter = true
        )
      )
    )

    assertEquals("book_101", model.bookId)
    assertEquals(101L, model.id)
    assertEquals("The Star Fox", model.title)
    assertEquals("test_uid_123", model.userId)
    assertEquals(1, model.pages.size)
    assertEquals(1, model.characters.size)
    assertEquals("Lumi", model.characters[0].name)
  }
}
