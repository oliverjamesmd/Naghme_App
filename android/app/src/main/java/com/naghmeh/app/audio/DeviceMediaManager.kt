package com.naghmeh.app.audio

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceAudioItem(
    val id: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val uri: Uri,
    val displayName: String
)

class DeviceMediaManager(private val context: Context) {

    suspend fun loadDeviceAudios(): List<DeviceAudioItem> = withContext(Dispatchers.IO) {
        val audios = mutableListOf<DeviceAudioItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.IS_MUSIC
        )

        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf("5000") // at least 5 seconds
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val rawTitle = c.getString(titleCol)
                    val rawArtist = c.getString(artistCol)
                    val duration = c.getLong(durationCol)
                    val displayName = c.getString(nameCol) ?: "فایل صوتی"
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val cleanTitle = if (!rawTitle.isNullOrBlank()) rawTitle else displayName
                    val cleanArtist = if (!rawArtist.isNullOrBlank() && rawArtist != "<unknown>") rawArtist else ""

                    audios.add(
                        DeviceAudioItem(
                            id = id,
                            title = cleanTitle,
                            artist = cleanArtist,
                            durationMs = duration,
                            uri = contentUri,
                            displayName = displayName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Permission or cursor read error
        }

        audios
    }
}
