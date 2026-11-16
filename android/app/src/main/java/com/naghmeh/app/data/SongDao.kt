package com.naghmeh.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM categories ORDER BY createdAt ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("UPDATE songs SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategoryFromSongs(categoryId: Long)

    @Delete
    suspend fun deleteCategoryEntity(category: CategoryEntity)

    @Transaction
    suspend fun deleteCategorySafely(category: CategoryEntity) {
        clearCategoryFromSongs(category.categoryId)
        deleteCategoryEntity(category)
    }

    @Transaction
    @Query("SELECT * FROM songs ORDER BY createdAt DESC")
    fun getAllSongsWithTracksFlow(): Flow<List<SongWithTracks>>

    @Transaction
    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongWithTracksById(id: Long): SongWithTracks?

    @Transaction
    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    fun getSongWithTracksByIdFlow(id: Long): Flow<SongWithTracks?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET fontSize = :fontSize WHERE id = :id")
    suspend fun updateFontSize(id: Long, fontSize: Float)

    @Delete
    suspend fun deleteSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: MediaTrackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<MediaTrackEntity>)

    @Delete
    suspend fun deleteTrack(track: MediaTrackEntity)

    @Query("DELETE FROM media_tracks WHERE songId = :songId")
    suspend fun deleteTracksForSong(songId: Long)

    @Transaction
    suspend fun saveSongWithTracks(
        song: SongEntity,
        tracks: List<MediaTrackEntity>
    ): Long {
        val songId = if (song.id == 0L) {
            insertSong(song)
        } else {
            updateSong(song)
            song.id
        }
        deleteTracksForSong(songId)
        if (tracks.isNotEmpty()) {
            insertTracks(tracks.map { it.copy(songId = songId) })
        }
        return songId
    }
}
