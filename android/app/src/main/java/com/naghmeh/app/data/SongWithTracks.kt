package com.naghmeh.app.data

import androidx.room.Embedded
import androidx.room.Relation

data class SongWithTracks(
    @Embedded
    val song: SongEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "songId"
    )
    val tracks: List<MediaTrackEntity>
)
