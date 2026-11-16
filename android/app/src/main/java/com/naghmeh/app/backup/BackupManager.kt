package com.naghmeh.app.backup

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.naghmeh.app.data.CategoryEntity
import com.naghmeh.app.data.MediaTrackEntity
import com.naghmeh.app.data.SongDao
import com.naghmeh.app.data.SongEntity
import com.naghmeh.app.data.SongWithTracks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class RestoreSummary(
    val songsCount: Int,
    val categoriesCount: Int
)

class BackupManager(private val context: Context, private val songDao: SongDao) {

    suspend fun createBackupZip(): Uri? = withContext(Dispatchers.IO) {
        try {
            val categories = songDao.getAllCategoriesList()
            val songsWithTracks = songDao.getAllSongsWithTracksList()

            // 1. Build JSON
            val rootJson = JSONObject().apply {
                put("app", "naghmeh")
                put("version", 1)
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

                val songArray = JSONArray()
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
                            trackArray.put(JSONObject().apply {
                                put("trackId", t.trackId)
                                put("title", t.title)
                                put("uriString", t.uriString)
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
                put("songs", songArray)
            }

            // 2. Prepare Zip File in cache/backups
            val backupsDir = File(context.cacheDir, "backups").apply {
                if (!exists()) mkdirs()
            }
            // clean previous backups in cache
            backupsDir.listFiles()?.forEach { it.delete() }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val zipFile = File(backupsDir, "naghmeh_backup_$timestamp.zip")

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                // Add data.json
                zos.putNextEntry(ZipEntry("naghmeh_data.json"))
                zos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Add in-app voice recordings
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

            // 3. Generate Shareable Uri via FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun restoreFromBackup(backupUri: Uri): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        try {
            var jsonContent: String? = null
            val recordingsDir = File(context.filesDir, "recordings").apply {
                if (!exists()) mkdirs()
            }

            // Try reading as ZIP first
            val isZip = context.contentResolver.openInputStream(backupUri)?.use { rawIn ->
                val zis = ZipInputStream(BufferedInputStream(rawIn))
                var entry = zis.nextEntry
                var foundAnyZipEntry = false
                while (entry != null) {
                    foundAnyZipEntry = true
                    if (entry.name == "naghmeh_data.json" || entry.name.endsWith(".json")) {
                        val baos = java.io.ByteArrayOutputStream()
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
                    }
                    entry = zis.nextEntry
                }
                foundAnyZipEntry
            } ?: false

            // Fallback: If not zip or json not found in zip, try reading as raw JSON
            if (jsonContent.isNullOrBlank()) {
                context.contentResolver.openInputStream(backupUri)?.use { rawIn ->
                    jsonContent = BufferedReader(InputStreamReader(rawIn, Charsets.UTF_8)).readText()
                }
            }

            if (jsonContent.isNullOrBlank()) {
                return@withContext Result.failure(Exception("فایل پشتیبان خالی یا نامعتبر است."))
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
                        val uriStr = tObj.optString("uriString", "")
                        val tType = tObj.optString("trackType", "MUSIC")
                        val durationSec = tObj.optLong("durationSec", 0L)
                        val displayTs = tObj.optString("displayTimestamp", "")
                        val tCreatedAt = tObj.optLong("createdAt", System.currentTimeMillis())

                        tracksToInsert.add(
                            MediaTrackEntity(
                                title = tTitle,
                                uriString = uriStr,
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

            Result.success(RestoreSummary(restoredSongsCount, catArray.length()))
        } catch (e: Exception) {
            Result.failure(Exception("خطا در بازیابی اطلاعات: ${e.localizedMessage ?: "فایل نامعتبر است"}"))
        }
    }
}
