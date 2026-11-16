package com.naghmeh.app.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.naghmeh.app.audio.AudioPlayerManager
import com.naghmeh.app.audio.VoiceRecorderManager
import com.naghmeh.app.data.CategoryEntity
import com.naghmeh.app.data.MediaTrackEntity
import com.naghmeh.app.data.SongDao
import com.naghmeh.app.data.SongEntity
import com.naghmeh.app.data.SongWithTracks
import com.naghmeh.app.data.TrackType
import com.naghmeh.app.ui.theme.PersianFontOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DraftTrack(
    val localKey: String,
    val trackId: Long = 0L,
    val title: String,
    val uriString: String = "",
    val fileName: String = "",
    val trackType: TrackType = TrackType.MUSIC,
    val durationSec: Long = 0L,
    val displayTimestamp: String = ""
)

data class AddEditUiState(
    val songId: Long? = null,
    val title: String = "",
    val lyrics: String = "",
    val categoryId: Long? = null,
    val musicTracks: List<DraftTrack> = listOf(createEmptyMusicSlot(1)),
    val recordedTracks: List<DraftTrack> = emptyList(),
    val fontSize: Float = 20f,
    val createdAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)

private fun createEmptyMusicSlot(indexNumber: Int): DraftTrack = DraftTrack(
    localKey = "music_${System.nanoTime()}_$indexNumber",
    title = "آهنگ ${toPersianDigits(indexNumber)}",
    trackType = TrackType.MUSIC
)

fun toPersianDigits(number: Int): String {
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    return number.toString().map { ch ->
        if (ch in '0'..'9') persianDigits[ch - '0'] else ch
    }.joinToString("")
}

@OptIn(ExperimentalCoroutinesApi::class)
class SongViewModel(
    appContext: Context,
    private val songDao: SongDao,
    private val audioPlayerManager: AudioPlayerManager,
    private val voiceRecorderManager: VoiceRecorderManager
) : ViewModel() {

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean(KEY_DARK_THEME, false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _selectedFontOption = MutableStateFlow(
        PersianFontOption.fromKey(prefs.getString(KEY_FONT_OPTION, PersianFontOption.VAZIRMATN.storageKey))
    )
    val selectedFontOption: StateFlow<PersianFontOption> = _selectedFontOption.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = songDao.getAllCategoriesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    val allSongs: StateFlow<List<SongWithTracks>> = songDao.getAllSongsWithTracksFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    val filteredSongs: StateFlow<List<SongWithTracks>> = combine(
        allSongs,
        _searchQuery,
        _selectedCategoryId
    ) { songs, query, categoryId ->
        val trimmed = query.trim()
        songs.filter { item ->
            val matchesCategory = categoryId == null || item.song.categoryId == categoryId
            val matchesQuery = trimmed.isEmpty() ||
                item.song.title.contains(trimmed, ignoreCase = true) ||
                item.song.lyrics.contains(trimmed, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList()
    )

    private val _practiceSongId = MutableStateFlow<Long?>(null)
    val practiceSong: StateFlow<SongWithTracks?> = _practiceSongId
        .flatMapLatest { songId ->
            if (songId == null || songId <= 0L) flowOf(null)
            else songDao.getSongWithTracksByIdFlow(songId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null
        )

    private val _selectedTrackId = MutableStateFlow<Long?>(null)
    val selectedTrackId: StateFlow<Long?> = _selectedTrackId.asStateFlow()

    private val _inlinePlayingKey = MutableStateFlow<String?>(null)
    val inlinePlayingKey: StateFlow<String?> = _inlinePlayingKey.asStateFlow()

    private val _addEditState = MutableStateFlow(AddEditUiState())
    val addEditState: StateFlow<AddEditUiState> = _addEditState.asStateFlow()

    val isPlaying: StateFlow<Boolean> = audioPlayerManager.isPlaying
    val currentPositionMs: StateFlow<Long> = audioPlayerManager.currentPositionMs
    val durationMs: StateFlow<Long> = audioPlayerManager.durationMs
    val isAudioPrepared: StateFlow<Boolean> = audioPlayerManager.isPrepared
    val playbackSpeed: StateFlow<Float> = audioPlayerManager.playbackSpeed
    val audioError: StateFlow<String?> = audioPlayerManager.errorMessage

    val isRecording: StateFlow<Boolean> = voiceRecorderManager.isRecording
    val recordingDurationSec: StateFlow<Long> = voiceRecorderManager.recordingDurationSec
    val recorderError: StateFlow<String?> = voiceRecorderManager.errorMessage

    fun toggleTheme() {
        val next = !_isDarkTheme.value
        _isDarkTheme.value = next
        prefs.edit().putBoolean(KEY_DARK_THEME, next).apply()
    }

    fun selectFontOption(option: PersianFontOption) {
        _selectedFontOption.value = option
        prefs.edit().putString(KEY_FONT_OPTION, option.storageKey).apply()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun selectCategoryFilter(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun createCategory(rawName: String, assignToForm: Boolean = false) {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) return
        val existing = categories.value.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        if (existing != null) {
            if (assignToForm) {
                _addEditState.update { it.copy(categoryId = existing.categoryId) }
            }
            return
        }
        viewModelScope.launch {
            val insertedId = songDao.insertCategory(CategoryEntity(name = trimmed))
            if (assignToForm) {
                _addEditState.update { it.copy(categoryId = insertedId) }
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            if (_selectedCategoryId.value == category.categoryId) {
                _selectedCategoryId.value = null
            }
            if (_addEditState.value.categoryId == category.categoryId) {
                _addEditState.update { it.copy(categoryId = null) }
            }
            songDao.deleteCategorySafely(category)
        }
    }

    fun loadSongForPractice(songId: Long) {
        _practiceSongId.value = songId
        viewModelScope.launch {
            val loaded = songDao.getSongWithTracksById(songId)
            val firstTrack = loaded?.tracks?.firstOrNull()
            if (firstTrack != null) {
                _selectedTrackId.value = firstTrack.trackId
                audioPlayerManager.prepareUri(Uri.parse(firstTrack.uriString))
            } else {
                _selectedTrackId.value = null
                audioPlayerManager.stop()
            }
        }
    }

    fun selectTrack(track: MediaTrackEntity, autoPlay: Boolean = false) {
        _selectedTrackId.value = track.trackId
        audioPlayerManager.switchTrack(Uri.parse(track.uriString), autoPlay = autoPlay)
    }

    fun setPlaybackSpeed(speed: Float) {
        audioPlayerManager.setPlaybackSpeed(speed)
    }

    fun stopAllAudioAndRecorder() {
        if (isRecording.value) {
            voiceRecorderManager.cancelRecording()
        }
        _inlinePlayingKey.value = null
        audioPlayerManager.stop()
    }

    fun togglePlayPause() {
        val songWithTracks = practiceSong.value ?: return
        val currentTrack = songWithTracks.tracks.firstOrNull { it.trackId == _selectedTrackId.value }
            ?: songWithTracks.tracks.firstOrNull()
            ?: return

        if (isPlaying.value) {
            audioPlayerManager.pause()
        } else if (isAudioPrepared.value) {
            audioPlayerManager.resume()
        } else {
            audioPlayerManager.playUri(Uri.parse(currentTrack.uriString))
        }
    }

    fun toggleInlineRecordingPlayback(track: DraftTrack) {
        if (track.uriString.isBlank()) return
        if (_inlinePlayingKey.value == track.localKey && isPlaying.value) {
            audioPlayerManager.pause()
        } else if (_inlinePlayingKey.value == track.localKey && isAudioPrepared.value) {
            audioPlayerManager.resume()
        } else {
            _inlinePlayingKey.value = track.localKey
            audioPlayerManager.playUri(Uri.parse(track.uriString))
        }
    }

    fun seekTo(positionMs: Long) {
        audioPlayerManager.seekTo(positionMs)
    }

    fun seekForward5s() {
        audioPlayerManager.seekForward5s()
    }

    fun seekBackward5s() {
        audioPlayerManager.seekBackward5s()
    }

    fun increaseFontSize() {
        updatePracticeFontSize(delta = 2f)
    }

    fun decreaseFontSize() {
        updatePracticeFontSize(delta = -2f)
    }

    private fun updatePracticeFontSize(delta: Float) {
        val current = practiceSong.value?.song ?: return
        val nextSize = (current.fontSize + delta).coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
        if (nextSize == current.fontSize) return
        viewModelScope.launch {
            songDao.updateFontSize(current.id, nextSize)
        }
    }

    fun startVoiceRecording() {
        if (isPlaying.value) {
            audioPlayerManager.pause()
        }
        voiceRecorderManager.startRecording()
    }

    fun stopVoiceRecordingIntoDraft() {
        val duration = recordingDurationSec.value
        val recordedFile = voiceRecorderManager.stopRecording() ?: return
        val nextIndex = _addEditState.value.recordedTracks.size + 1
        val timestamp = SimpleDateFormat("HH:mm · yyyy/MM/dd", Locale.US).format(Date())
        val draft = DraftTrack(
            localKey = "rec_${System.nanoTime()}",
            title = "ضبط صدا ${toPersianDigits(nextIndex)}",
            uriString = Uri.fromFile(recordedFile).toString(),
            fileName = recordedFile.name,
            trackType = TrackType.RECORDING,
            durationSec = duration,
            displayTimestamp = timestamp
        )
        _addEditState.update { state ->
            state.copy(recordedTracks = state.recordedTracks + draft)
        }
    }

    fun stopVoiceRecordingAndSaveToPracticeSong() {
        val songId = _practiceSongId.value ?: return
        val existingTracks = practiceSong.value?.tracks.orEmpty()
        val recordingCount = existingTracks.count { it.trackType == TrackType.RECORDING.name } + 1
        val duration = recordingDurationSec.value
        val recordedFile = voiceRecorderManager.stopRecording() ?: return
        val timestamp = SimpleDateFormat("HH:mm · yyyy/MM/dd", Locale.US).format(Date())

        viewModelScope.launch {
            val entity = MediaTrackEntity(
                songId = songId,
                title = "ضبط صدا ${toPersianDigits(recordingCount)}",
                uriString = Uri.fromFile(recordedFile).toString(),
                trackType = TrackType.RECORDING.name,
                durationSec = duration,
                displayTimestamp = timestamp,
                createdAt = System.currentTimeMillis()
            )
            val insertedId = songDao.insertTrack(entity)
            _selectedTrackId.value = insertedId
            audioPlayerManager.prepareUri(Uri.fromFile(recordedFile))
        }
    }

    fun clearRecorderError() {
        voiceRecorderManager.clearError()
    }

    fun prepareAddEditForm(songId: Long?) {
        _inlinePlayingKey.value = null
        if (songId == null || songId <= 0L) {
            _addEditState.value = AddEditUiState()
            return
        }
        viewModelScope.launch {
            val existing = songDao.getSongWithTracksById(songId)
            if (existing != null) {
                val music = existing.tracks
                    .filter { it.trackType != TrackType.RECORDING.name }
                    .map { track ->
                        DraftTrack(
                            localKey = "db_${track.trackId}",
                            trackId = track.trackId,
                            title = track.title,
                            uriString = track.uriString,
                            fileName = Uri.parse(track.uriString).lastPathSegment ?: track.title,
                            trackType = TrackType.MUSIC,
                            durationSec = track.durationSec,
                            displayTimestamp = track.displayTimestamp
                        )
                    }
                    .ifEmpty { listOf(createEmptyMusicSlot(1)) }

                val recordings = existing.tracks
                    .filter { it.trackType == TrackType.RECORDING.name }
                    .map { track ->
                        DraftTrack(
                            localKey = "db_${track.trackId}",
                            trackId = track.trackId,
                            title = track.title,
                            uriString = track.uriString,
                            fileName = Uri.parse(track.uriString).lastPathSegment ?: track.title,
                            trackType = TrackType.RECORDING,
                            durationSec = track.durationSec,
                            displayTimestamp = track.displayTimestamp
                        )
                    }

                _addEditState.value = AddEditUiState(
                    songId = existing.song.id,
                    title = existing.song.title,
                    lyrics = existing.song.lyrics,
                    categoryId = existing.song.categoryId,
                    musicTracks = music,
                    recordedTracks = recordings,
                    fontSize = existing.song.fontSize,
                    createdAt = existing.song.createdAt
                )
            } else {
                _addEditState.value = AddEditUiState()
            }
        }
    }

    fun updateFormTitle(title: String) {
        _addEditState.update { it.copy(title = title, errorMessage = null) }
    }

    fun updateFormLyrics(lyrics: String) {
        _addEditState.update { it.copy(lyrics = lyrics, errorMessage = null) }
    }

    fun updateFormCategory(categoryId: Long?) {
        _addEditState.update { it.copy(categoryId = categoryId) }
    }

    fun addEmptyMusicTrackSlot() {
        _addEditState.update { state ->
            val nextNumber = state.musicTracks.size + 1
            state.copy(musicTracks = state.musicTracks + createEmptyMusicSlot(nextNumber))
        }
    }

    fun updateMusicTrackTitle(localKey: String, newTitle: String) {
        _addEditState.update { state ->
            state.copy(
                musicTracks = state.musicTracks.map { track ->
                    if (track.localKey == localKey) track.copy(title = newTitle) else track
                }
            )
        }
    }

    fun attachFileToMusicTrack(localKey: String, uri: Uri, displayName: String) {
        _addEditState.update { state ->
            state.copy(
                musicTracks = state.musicTracks.map { track ->
                    if (track.localKey == localKey) {
                        track.copy(uriString = uri.toString(), fileName = displayName)
                    } else {
                        track
                    }
                },
                errorMessage = null
            )
        }
    }

    fun removeMusicTrack(localKey: String) {
        _addEditState.update { state ->
            val remaining = state.musicTracks.filterNot { it.localKey == localKey }
            state.copy(
                musicTracks = if (remaining.isEmpty()) listOf(createEmptyMusicSlot(1)) else remaining
            )
        }
    }

    fun removeRecordedTrack(localKey: String) {
        if (_inlinePlayingKey.value == localKey) {
            audioPlayerManager.stop()
            _inlinePlayingKey.value = null
        }
        _addEditState.update { state ->
            state.copy(recordedTracks = state.recordedTracks.filterNot { it.localKey == localKey })
        }
    }

    fun saveSong(onSaved: (Long) -> Unit) {
        val state = _addEditState.value
        val trimmedTitle = state.title.trim()
        val trimmedLyrics = state.lyrics.trim()

        if (trimmedTitle.isEmpty()) {
            _addEditState.update { it.copy(errorMessage = "لطفاً عنوان شعر را وارد کنید.") }
            return
        }
        if (trimmedLyrics.isEmpty()) {
            _addEditState.update { it.copy(errorMessage = "لطفاً متن شعر را وارد کنید.") }
            return
        }

        _addEditState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val songEntity = SongEntity(
                id = state.songId ?: 0L,
                title = trimmedTitle,
                lyrics = trimmedLyrics,
                categoryId = state.categoryId,
                fontSize = state.fontSize,
                createdAt = if (state.songId != null) state.createdAt else System.currentTimeMillis()
            )
            val validMusic = state.musicTracks
                .filter { it.uriString.isNotBlank() }
                .mapIndexed { index, draft ->
                    MediaTrackEntity(
                        trackId = draft.trackId,
                        songId = state.songId ?: 0L,
                        title = draft.title.trim().ifBlank { "آهنگ ${toPersianDigits(index + 1)}" },
                        uriString = draft.uriString,
                        trackType = TrackType.MUSIC.name,
                        durationSec = draft.durationSec,
                        displayTimestamp = draft.displayTimestamp
                    )
                }
            val validRecordings = state.recordedTracks
                .filter { it.uriString.isNotBlank() }
                .mapIndexed { index, draft ->
                    MediaTrackEntity(
                        trackId = draft.trackId,
                        songId = state.songId ?: 0L,
                        title = draft.title.trim().ifBlank { "ضبط صدا ${toPersianDigits(index + 1)}" },
                        uriString = draft.uriString,
                        trackType = TrackType.RECORDING.name,
                        durationSec = draft.durationSec,
                        displayTimestamp = draft.displayTimestamp
                    )
                }

            val savedId = songDao.saveSongWithTracks(songEntity, validMusic + validRecordings)
            _addEditState.update { it.copy(isSaving = false) }
            onSaved(savedId)
        }
    }

    fun deleteSong(songWithTracks: SongWithTracks) {
        viewModelScope.launch {
            if (_practiceSongId.value == songWithTracks.song.id) {
                stopAllAudioAndRecorder()
                _practiceSongId.value = null
            }
            songWithTracks.tracks
                .filter { it.trackType == TrackType.RECORDING.name }
                .forEach { track ->
                    val path = Uri.parse(track.uriString).path
                    if (!path.isNullOrBlank()) {
                        File(path).delete()
                    }
                }
            songDao.deleteSong(songWithTracks.song)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerManager.release()
        voiceRecorderManager.release()
    }

    companion object {
        const val MIN_FONT_SIZE = 16f
        const val MAX_FONT_SIZE = 40f
        private const val PREFS_NAME = "naghmeh_preferences"
        private const val KEY_FONT_OPTION = "persian_font_option"
        private const val KEY_DARK_THEME = "dark_theme_enabled"
    }
}

class SongViewModelFactory(
    private val appContext: Context,
    private val songDao: SongDao,
    private val audioPlayerManager: AudioPlayerManager,
    private val voiceRecorderManager: VoiceRecorderManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongViewModel::class.java)) {
            return SongViewModel(appContext, songDao, audioPlayerManager, voiceRecorderManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
