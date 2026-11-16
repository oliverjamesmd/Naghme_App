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
    val fontSize: Float = 20f,
    val createdAt: Long = System.currentTimeMillis()
)
