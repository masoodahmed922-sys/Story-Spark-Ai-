package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class PageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val pageNumber: Int,
    val text: String,
    val illustrationPrompt: String,
    val sceneDescription: String = "",
    val mood: String = "Warm & Peaceful",
    val location: String = "Starry Meadow",
    val charactersPresent: String = "Lumi",
    val importantObjects: String = "Glowing Star, Knitted Scarf",
    val imageUri: String = ""
)
