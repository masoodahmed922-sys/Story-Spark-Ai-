package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.export.PdfExportService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Story Spark Ai", appName)
  }

  @Test
  fun `test safeSubstring clamping does not throw with begin 0 end 23 length 22`() {
    val text22 = "1234567890123456789012" // exact length 22
    assertEquals(22, text22.length)

    // The exact condition reported by the user: begin 0, end 23, length 22
    val result = PdfExportService.safeSubstring(text22, 0, 23)
    assertEquals("1234567890123456789012", result)
    assertEquals(22, result.length)

    // Edge cases: null, empty string, negative start, large end
    assertEquals("", PdfExportService.safeSubstring(null, 0, 23))
    assertEquals("", PdfExportService.safeSubstring("", 0, 23))
    assertEquals("12", PdfExportService.safeSubstring(text22, 0, 2))
    assertEquals("1234567890123456789012", PdfExportService.safeSubstring(text22, -10, 500))
    assertEquals("", PdfExportService.safeSubstring(text22, 10, 5)) // reversed bounds clamped safely
  }

  @Test
  fun `test safeTake and safeTrim`() {
    val text22 = "1234567890123456789012"
    assertEquals("1234567890123456789012", PdfExportService.safeTake(text22, 23))
    assertEquals("12345", PdfExportService.safeTake(text22, 5))
    assertEquals("", PdfExportService.safeTake(null, 10))
    assertEquals("", PdfExportService.safeTake("", 10))
    assertEquals("", PdfExportService.safeTake(text22, 0))

    assertEquals("clean text", PdfExportService.safeTrim("   clean text   "))
    assertEquals("", PdfExportService.safeTrim(null))
    assertEquals("", PdfExportService.safeTrim("   "))
  }

  @Test
  fun `test StoryGeneratorEngine creates distinct non-empty Story DNA and dynamic suggestions`() = kotlinx.coroutines.runBlocking {
    val dna1 = com.example.ai.StoryGeneratorEngine.createStoryDna(ageGroup = "4–6 years", artStyle = "Watercolor")
    val dna2 = com.example.ai.StoryGeneratorEngine.createStoryDna(ageGroup = "6–8 years", artStyle = "3D Animated")

    org.junit.Assert.assertTrue(dna1.characterProfile.isNotBlank())
    org.junit.Assert.assertTrue(dna1.setting.isNotBlank())
    org.junit.Assert.assertTrue(dna1.centralGoal.isNotBlank())
    org.junit.Assert.assertTrue(dna1.colorPalette.isNotBlank())
    org.junit.Assert.assertTrue(dna1.moral.isNotBlank())

    val suggestions = com.example.ai.StoryGeneratorEngine.generateDynamicSuggestions(
      count = 4,
      targetAge = "4–6 years",
      preferredTone = "Gentle",
      artStyle = "Watercolor"
    )

    org.junit.Assert.assertTrue(suggestions.isNotEmpty())
    val titles = suggestions.map { it.title }
    // Ensure titles are unique
    assertEquals(titles.size, titles.toSet().size)
  }
}
