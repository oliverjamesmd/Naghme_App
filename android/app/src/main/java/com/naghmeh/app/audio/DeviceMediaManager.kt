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
            MediaStore.Audio.Media.DISPLAY_NAME
        )

        // Broader query: do not filter strictly by duration so Telegram/Downloads audio files aren't missed
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val durationCol = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val nameCol = c.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)

                while (c.moveToNext()) {
                    val id = if (idCol >= 0) c.getLong(idCol) else continue
                    val rawTitle = if (titleCol >= 0) c.getString(titleCol) else null
                    val rawArtist = if (artistCol >= 0) c.getString(artistCol) else null
                    val duration = if (durationCol >= 0) c.getLong(durationCol) else 0L
                    val displayName = if (nameCol >= 0) c.getString(nameCol) ?: "آهنگ" else "آهنگ"

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
