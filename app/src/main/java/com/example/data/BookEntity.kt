package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val title: String,
    val subtitle: String = "",
    val description: String = "",
    val authorName: String = "StorySpark Creator",
    val authorBio: String = "",
    val coverImageRes: String = "img_lumi_stars",
    val coverStyle: String = "Modern Whimsical",
    val titlePosition: String = "Top",
    val targetAge: String = "4–6 years",
    val storyType: String = "Bedtime",
    val storyTone: String = "Gentle",
    val readingLevel: String = "Simple",
    val artStyle: String = "Watercolor",
    val pageLayout: String = "Image Above Text",
    val moralLesson: String = "Kindness and courage light up the darkest night.",
    val storyDnaJson: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val pageCount: Int = 8,
    val isFavorite: Boolean = false,
    val language: String = "en",
    val originalBookId: Long? = null,
    val translatedFrom: String? = null,
    val translationsJson: String = "{}"
)
