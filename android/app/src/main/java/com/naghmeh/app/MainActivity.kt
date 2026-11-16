package com.naghmeh.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.naghmeh.app.audio.AudioPlayerManager
import com.naghmeh.app.audio.VoiceRecorderManager
import com.naghmeh.app.data.AppDatabase
import com.naghmeh.app.ui.SongViewModel
import com.naghmeh.app.ui.SongViewModelFactory
import com.naghmeh.app.ui.screens.AddEditScreen
import com.naghmeh.app.ui.screens.HomeScreen
import com.naghmeh.app.ui.screens.PracticeScreen
import com.naghmeh.app.ui.theme.NaghmehTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val audioPlayerManager = AudioPlayerManager(applicationContext)
        val voiceRecorderManager = VoiceRecorderManager(applicationContext)
        val viewModelFactory = SongViewModelFactory(
            appContext = applicationContext,
            songDao = database.songDao(),
            audioPlayerManager = audioPlayerManager,
            voiceRecorderManager = voiceRecorderManager
        )

        setContent {
            val songViewModel: SongViewModel = viewModel(factory = viewModelFactory)
            val selectedFontOption by songViewModel.selectedFontOption.collectAsStateWithLifecycle()
            val isDarkTheme by songViewModel.isDarkTheme.collectAsStateWithLifecycle()

            NaghmehTheme(
                darkTheme = isDarkTheme,
                fontOption = selectedFontOption
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    NaghmehNavGraph(viewModel = songViewModel)
                }
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val PRACTICE = "practice/{songId}"
    const val ADD_EDIT = "add_edit?songId={songId}"

    fun practice(songId: Long): String = "practice/$songId"
    fun addEdit(songId: Long? = null): String =
        if (songId != null) "add_edit?songId=$songId" else "add_edit"
}

@Composable
private fun NaghmehNavGraph(viewModel: SongViewModel) {
    val navController = rememberNavController()

    val songs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val practiceSong by viewModel.practiceSong.collectAsStateWithLifecycle()
    val selectedTrackId by viewModel.selectedTrackId.collectAsStateWithLifecycle()
    val selectedFontOption by viewModel.selectedFontOption.collectAsStateWithLifecycle()
    val addEditState by viewModel.addEditState.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val audioError by viewModel.audioError.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsStateWithLifecycle()
    val recorderError by viewModel.recorderError.collectAsStateWithLifecycle()
    val inlinePlayingKey by viewModel.inlinePlayingKey.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                songs = songs,
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                searchQuery = searchQuery,
                isDarkTheme = isDarkTheme,
                onToggleTheme = viewModel::toggleTheme,
                onSelectCategory = viewModel::selectCategoryFilter,
                onCreateCategory = { name -> viewModel.createCategory(name, assignToForm = false) },
                onDeleteCategory = viewModel::deleteCategory,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onSongClick = { songId ->
                    navController.navigate(Routes.practice(songId))
                },
                onEditClick = { songId ->
                    navController.navigate(Routes.addEdit(songId))
                },
                onAddClick = {
                    navController.navigate(Routes.addEdit())
                },
                onDeleteConfirm = viewModel::deleteSong
            )
        }

        composable(
            route = Routes.PRACTICE,
            arguments = listOf(
                navArgument("songId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getLong("songId") ?: 0L
            LaunchedEffect(songId) {
                viewModel.loadSongForPractice(songId)
            }

            PracticeScreen(
                songWithTracks = practiceSong,
                selectedTrackId = selectedTrackId,
                selectedFontOption = selectedFontOption,
                isDarkTheme = isDarkTheme,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                playbackSpeed = playbackSpeed,
                audioError = audioError,
                isRecording = isRecording,
                recordingDurationSec = recordingDurationSec,
                recorderError = recorderError,
                onToggleTheme = viewModel::toggleTheme,
                onBackClick = { navController.popBackStack() },
                onEditClick = { editId ->
                    navController.navigate(Routes.addEdit(editId))
                },
                onIncreaseFontSize = viewModel::increaseFontSize,
                onDecreaseFontSize = viewModel::decreaseFontSize,
                onSelectFontOption = viewModel::selectFontOption,
                onSelectTrack = { track -> viewModel.selectTrack(track, autoPlay = false) },
                onSelectPlaybackSpeed = viewModel::setPlaybackSpeed,
                onTogglePlayPause = viewModel::togglePlayPause,
                onSeekTo = viewModel::seekTo,
                onSeekForward5s = viewModel::seekForward5s,
                onSeekBackward5s = viewModel::seekBackward5s,
                onStartRecording = viewModel::startVoiceRecording,
                onStopRecording = viewModel::stopVoiceRecordingAndSaveToPracticeSong,
                onClearRecorderError = viewModel::clearRecorderError,
                onLeaveScreen = viewModel::stopAllAudioAndRecorder
            )
        }

        composable(
            route = Routes.ADD_EDIT,
            arguments = listOf(
                navArgument("songId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getLong("songId") ?: -1L
            val songId = if (rawId > 0L) rawId else null

            LaunchedEffect(songId) {
                viewModel.prepareAddEditForm(songId)
            }

            AddEditScreen(
                uiState = addEditState,
                categories = categories,
                isDarkTheme = isDarkTheme,
                isRecording = isRecording,
                recordingDurationSec = recordingDurationSec,
                recorderError = recorderError,
                inlinePlayingKey = inlinePlayingKey,
                isAudioPlaying = isPlaying,
                onToggleTheme = viewModel::toggleTheme,
                onTitleChange = viewModel::updateFormTitle,
                onLyricsChange = viewModel::updateFormLyrics,
                onCategorySelect = viewModel::updateFormCategory,
                onCreateAndSelectCategory = { name -> viewModel.createCategory(name, assignToForm = true) },
                onAddMusicSlot = viewModel::addEmptyMusicTrackSlot,
                onUpdateMusicTitle = viewModel::updateMusicTrackTitle,
                onAttachFileToMusicSlot = viewModel::attachFileToMusicTrack,
                onRemoveMusicSlot = viewModel::removeMusicTrack,
                onStartRecording = viewModel::startVoiceRecording,
                onStopRecording = viewModel::stopVoiceRecordingIntoDraft,
                onToggleInlineRecordingPlayback = viewModel::toggleInlineRecordingPlayback,
                onRemoveRecordedTrack = viewModel::removeRecordedTrack,
                onSaveClick = {
                    viewModel.saveSong {
                        navController.popBackStack()
                    }
                },
                onBackClick = {
                    viewModel.stopAllAudioAndRecorder()
                    navController.popBackStack()
                }
            )
        }
    }
}
