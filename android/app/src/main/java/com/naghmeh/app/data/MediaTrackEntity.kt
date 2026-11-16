package com.naghmeh.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TrackType(val labelFa: String) {
    MUSIC("فایل آهنگ"),
    RECORDING("ضبط صدا")
}

@Entity(
    tableName = "media_tracks",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["songId"])]
)
data class MediaTrackEntity(
    @PrimaryKey(autoGenerate = true)
    val trackId: Long = 0L,
    val songId: Long,
    val title: String,
    val uriString: String,
    val trackType: String = TrackType.MUSIC.name,
    val durationSec: Long = 0L,
    val displayTimestamp: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
