package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(
    tableName = "book_translations",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId", "langCode"], unique = true)]
)
data class BookTranslationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val langCode: String,
    val title: String,
    val moral: String,
    val pagesJson: String,
    val status: String = "complete", // "complete" or "partial"
    val model: String = "gemini-2.5-flash",
    val updatedAt: Long = System.currentTimeMillis()
)

data class TranslationPageItem(
    val pageNumber: Int,
    val text: String
)

data class BookTranslationData(
    val langCode: String,
    val title: String,
    val moral: String,
    val pages: List<TranslationPageItem>,
    val status: String = "complete",
    val model: String = "gemini-2.5-flash",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("langCode", langCode)
        obj.put("title", title)
        obj.put("moral", moral)
        obj.put("status", status)
        obj.put("model", model)
        obj.put("updatedAt", updatedAt)
        val arr = JSONArray()
        pages.forEach { p ->
            val pageObj = JSONObject()
            pageObj.put("pageNumber", p.pageNumber)
            pageObj.put("text", p.text)
            arr.put(pageObj)
        }
        obj.put("pages", arr)
        return obj
    }

    companion object {
        fun fromJson(jsonStr: String): BookTranslationData? {
            return try {
                val obj = JSONObject(jsonStr)
                val langCode = obj.optString("langCode")
                val title = obj.optString("title")
                val moral = obj.optString("moral")
                val status = obj.optString("status", "complete")
                val model = obj.optString("model", "gemini-2.5-flash")
                val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                val pages = mutableListOf<TranslationPageItem>()
                val arr = obj.optJSONArray("pages")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val p = arr.getJSONObject(i)
                        pages.add(TranslationPageItem(p.getInt("pageNumber"), p.getString("text")))
                    }
                }
                BookTranslationData(
                    langCode = langCode,
                    title = title,
                    moral = moral,
                    pages = pages,
                    status = status,
                    model = model,
                    updatedAt = updatedAt
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
