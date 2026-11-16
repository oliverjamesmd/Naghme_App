package com.naghmeh.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val lyrics: String,
    val categoryId: Long? = null,
    val categoryIds: String = "",
    val fontSize: Float = 20f,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getAllCategoryIds(): Set<Long> {
        val ids = categoryIds.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .toMutableSet()
        if (categoryId != null && categoryId > 0L) {
            ids.add(categoryId)
        }
        return ids
    }
}
