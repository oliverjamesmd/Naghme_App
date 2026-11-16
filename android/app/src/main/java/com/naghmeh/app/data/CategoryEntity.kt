package com.naghmeh.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val categoryId: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
