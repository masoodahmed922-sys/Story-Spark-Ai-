package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "characters",
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
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val name: String,
    val type: String = "Animal",
    val age: String = "6",
    val personality: String = "Curious and kind",
    val favoriteThing: String = "Stars and cozy blankets",
    val specialAbility: String = "Can understand animal whispers",
    val appearance: String = "Small orange fox with golden eyes and a sky-blue knitted scarf",
    val role: String = "Main Character",
    val isMainCharacter: Boolean = true
)
