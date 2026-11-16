package com.naghmeh.app.backup

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.naghmeh.app.data.CategoryEntity
import com.naghmeh.app.data.MediaTrackEntity
import com.naghmeh.app.data.SongDao
import com.naghmeh.app.data.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class RestoreSummary(
    val songsCount: Int,
    val categoriesCount: Int,
    val mediaFilesCount: Int
)

class BackupManager(private val context: Context, private val songDao: SongDao) {

    private suspend fun generateBackupZipFile(): File? = withContext(Dispatchers.IO) {
        try {
            val categories = songDao.getAllCategoriesList()
            val songsWithTracks = songDao.getAllSongsWithTracksList()

            val backupsDir = File(context.cacheDir, "backups").apply {
                if (!exists()) mkdirs()
            }
            backupsDir.listFiles()?.forEach { it.delete() }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val zipFile = File(backupsDir, "naghmeh_backup_$timestamp.zip")

            val tracksDir = File(context.filesDir, "tracks").apply {
                if (!exists()) mkdirs()
            }

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                val songArray = JSONArray()

                // Process songs and bundle their attached media files
                songsWithTracks.forEach { item ->
                    val sJson = JSONObject().apply {
                        put("id", item.song.id)
                        put("title", item.song.title)
                        put("lyrics", item.song.lyrics)
                        put("categoryId", item.song.categoryId ?: JSONObject.NULL)
                        put("fontSize", item.song.fontSize.toDouble())
                        put("createdAt", item.song.createdAt)

                        val trackArray = JSONArray()
                        item.tracks.forEach { t ->
                            var bundledFileName: String? = null

                            // If track has a valid URI, try bundling the actual audio/video file into the ZIP
                            if (t.uriString.isNotBlank()) {
                                try {
                                    val uri = Uri.parse(t.uriString)
                                    val safeExt = when {
                                        t.uriString.contains(".mp4", ignoreCase = true) -> "mp4"
                                        t.uriString.contains(".mkv", ignoreCase = true) -> "mkv"
                                        t.uriString.contains(".wav", ignoreCase = true) -> "wav"
                                        t.uriString.contains(".m4a", ignoreCase = true) -> "m4a"
                                        t.uriString.contains(".aac", ignoreCase = true) -> "aac"
                                        t.uriString.contains(".ogg", ignoreCase = true) -> "ogg"
                                        t.uriString.contains(".flac", ignoreCase = true) -> "flac"
                                        else -> "mp3"
                                    }
                                    val zipEntryName = "media/track_${item.song.id}_${t.trackId}_${System.nanoTime()}.$safeExt"

                                    val inputStream = try {
                                        when {
                                            uri.scheme == "content" -> context.contentResolver.openInputStream(uri)
                                            uri.scheme == "file" -> {
                                                val f = File(uri.path ?: "")
                                                if (f.exists()) FileInputStream(f) else null
                                            }
                                            uri.path != null -> {
                                                val f = File(uri.path ?: "")
                                                if (f.exists()) FileInputStream(f) else null
                                            }
                                            else -> {
                                                val f = File(t.uriString)
                                                if (f.exists()) FileInputStream(f) else null
                                            }
                                        }
                                    } catch (e: Exception) {
                                        null
                                    }

                                    if (inputStream != null) {
                                        try {
                                            zos.putNextEntry(ZipEntry(zipEntryName))
                                            inputStream.use { input ->
                                                input.copyTo(zos)
                                            }
                                            zos.closeEntry()
                                            bundledFileName = zipEntryName
                                        } catch (e: Exception) {
                                            try { zos.closeEntry() } catch (ignored: Exception) {}
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Fallback: keep original URI if file streaming fails
                                }
                            }

                            trackArray.put(JSONObject().apply {
                                put("trackId", t.trackId)
                                put("title", t.title)
                                put("uriString", t.uriString)
                                put("bundledFileName", bundledFileName ?: JSONObject.NULL)
                                put("trackType", t.trackType)
                                put("durationSec", t.durationSec)
                                put("displayTimestamp", t.displayTimestamp)
                                put("createdAt", t.createdAt)
                            })
                        }
                        put("tracks", trackArray)
                    }
                    songArray.put(sJson)
                }

                // 1. Build & write root JSON
                val rootJson = JSONObject().apply {
                    put("app", "naghmeh")
                    put("version", 2)
                    put("createdAt", System.currentTimeMillis())

                    val catArray = JSONArray()
                    categories.forEach { cat ->
                        catArray.put(JSONObject().apply {
                            put("categoryId", cat.categoryId)
                            put("name", cat.name)
                            put("createdAt", cat.createdAt)
                        })
                    }
                    put("categories", catArray)
                    put("songs", songArray)
                }

                zos.putNextEntry(ZipEntry("naghmeh_data.json"))
                zos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 2. Also bundle voice practice recordings
                val recordingsDir = File(context.filesDir, "recordings")
                if (recordingsDir.exists()) {
                    recordingsDir.listFiles()?.forEach { recFile ->
                        if (recFile.isFile && recFile.length() > 0) {
                            zos.putNextEntry(ZipEntry("recordings/${recFile.name}"))
                            FileInputStream(recFile).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
            }

            zipFile
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createBackupZip(): Uri? = withContext(Dispatchers.IO) {
        val file = generateBackupZipFile() ?: return@withContext null
        try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveBackupToDownloads(): Result<String> = withContext(Dispatchers.IO) {
        val sourceFile = generateBackupZipFile()
            ?: return@withContext Result.failure(Exception("خطا در ایجاد بسته پشتیبان."))

        val fileName = sourceFile.name

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val downloadUri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return@withContext Result.failure(Exception("امکان ایجاد فایل در Downloads فراهم نشد."))

                context.contentResolver.openOutputStream(downloadUri)?.use { out ->
                    FileInputStream(sourceFile).use { inStream ->
                        inStream.copyTo(out)
                    }
                } ?: return@withContext Result.failure(Exception("نوشتن در پوشه Downloads امکان‌پذیر نشد."))

                Result.success(fileName)
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val destFile = File(downloadsDir, fileName)
                FileInputStream(sourceFile).use { inStream ->
                    FileOutputStream(destFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf("application/zip"),
                    null
                )
                Result.success(fileName)
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطا در ذخیره در دانلودها: ${e.localizedMessage ?: "دسترسی مجاز نیست"}"))
        }
    }

    suspend fun restoreFromBackup(backupUri: Uri): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        try {
            var jsonContent: String? = null
            val recordingsDir = File(context.filesDir, "recordings").apply {
                if (!exists()) mkdirs()
            }
            val restoredTracksDir = File(context.filesDir, "tracks").apply {
                if (!exists()) mkdirs()
            }

            var mediaRestoredCount = 0

            context.contentResolver.openInputStream(backupUri)?.use { rawIn ->
                val zis = ZipInputStream(BufferedInputStream(rawIn))
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "naghmeh_data.json" || entry.name.endsWith(".json")) {
                        val baos = ByteArrayOutputStream()
                        val buf = ByteArray(4096)
                        var readLen: Int
                        while (zis.read(buf).also { readLen = it } > 0) {
                            baos.write(buf, 0, readLen)
                        }
                        jsonContent = baos.toString("UTF-8")
                    } else if (entry.name.startsWith("recordings/") && !entry.isDirectory) {
                        val fileName = File(entry.name).name
                        val targetRec = File(recordingsDir, fileName)
                        FileOutputStream(targetRec).use { fos ->
                            zis.copyTo(fos)
                        }
                        mediaRestoredCount++
                    } else if (entry.name.startsWith("media/") && !entry.isDirectory) {
                        val fileName = File(entry.name).name
                        val targetFile = File(restoredTracksDir, fileName)
                        FileOutputStream(targetFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        mediaRestoredCount++
                    }
                    entry = zis.nextEntry
                }
            }

            if (jsonContent.isNullOrBlank()) {
                return@withContext Result.failure(Exception("فایل پشتیبان نامعتبر است یا اطلاعاتی در آن یافت نشد."))
            }

            val rootJson = JSONObject(jsonContent!!)
            val catArray = rootJson.optJSONArray("categories") ?: JSONArray()
            val songArray = rootJson.optJSONArray("songs") ?: JSONArray()

            val categoryIdMap = mutableMapOf<Long, Long>()

            // 1. Restore Categories
            for (i in 0 until catArray.length()) {
                val cObj = catArray.getJSONObject(i)
                val oldId = cObj.optLong("categoryId", 0L)
                val name = cObj.optString("name", "").trim()
                val createdAt = cObj.optLong("createdAt", System.currentTimeMillis())

                if (name.isNotEmpty()) {
                    val newId = songDao.insertCategory(
                        CategoryEntity(name = name, createdAt = createdAt)
                    )
                    if (oldId > 0) {
                        categoryIdMap[oldId] = newId
                    }
                }
            }

            var restoredSongsCount = 0

            // 2. Restore Songs & Tracks
            for (i in 0 until songArray.length()) {
                val sObj = songArray.getJSONObject(i)
                val title = sObj.optString("title", "").trim()
                val lyrics = sObj.optString("lyrics", "")
                val oldCatId = if (sObj.isNull("categoryId")) null else sObj.optLong("categoryId")
                val newCatId = oldCatId?.let { categoryIdMap[it] }
                val fontSize = sObj.optDouble("fontSize", 20.0).toFloat()
                val createdAt = sObj.optLong("createdAt", System.currentTimeMillis())

                if (title.isNotEmpty()) {
                    val trackArray = sObj.optJSONArray("tracks") ?: JSONArray()
                    val tracksToInsert = mutableListOf<MediaTrackEntity>()

                    for (tIdx in 0 until trackArray.length()) {
                        val tObj = trackArray.getJSONObject(tIdx)
                        val tTitle = tObj.optString("title", "آهنگ")
                        val originalUriStr = tObj.optString("uriString", "")
                        val bundledFileName = if (tObj.isNull("bundledFileName")) null else tObj.optString("bundledFileName")
                        val tType = tObj.optString("trackType", "MUSIC")
                        val durationSec = tObj.optLong("durationSec", 0L)
                        val displayTs = tObj.optString("displayTimestamp", "")
                        val tCreatedAt = tObj.optLong("createdAt", System.currentTimeMillis())

                        // If file was bundled in zip, point to restored local file
                        val effectiveUriStr = if (!bundledFileName.isNullOrBlank()) {
                            val localFile = File(restoredTracksDir, File(bundledFileName).name)
                            if (localFile.exists()) {
                                Uri.fromFile(localFile).toString()
                            } else {
                                originalUriStr
                            }
                        } else if (tType == "RECORDING") {
                            val recFileName = File(Uri.parse(originalUriStr).path ?: "").name
                            val localRecFile = File(recordingsDir, recFileName)
                            if (localRecFile.exists()) {
                                Uri.fromFile(localRecFile).toString()
                            } else {
                                originalUriStr
                            }
                        } else {
                            originalUriStr
                        }

                        tracksToInsert.add(
                            MediaTrackEntity(
                                title = tTitle,
                                uriString = effectiveUriStr,
                                trackType = tType,
                                durationSec = durationSec,
                                displayTimestamp = displayTs,
                                createdAt = tCreatedAt,
                                songId = 0L
                            )
                        )
                    }

                    songDao.saveSongWithTracks(
                        SongEntity(
                            title = title,
                            lyrics = lyrics,
                            categoryId = newCatId,
                            fontSize = fontSize,
                            createdAt = createdAt
                        ),
                        tracksToInsert
                    )
                    restoredSongsCount++
                }
            }

            Result.success(RestoreSummary(restoredSongsCount, catArray.length(), mediaRestoredCount))
        } catch (e: Exception) {
            Result.failure(Exception("خطا در بازیابی اطلاعات: ${e.localizedMessage ?: "فایل نامعتبر است"}"))
        }
    }
}
