package com.naghmeh.app.audio

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class AudioPlayerManager(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null
    private var loadedUri: Uri? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isPrepared = MutableStateFlow(false)
    val isPrepared: StateFlow<Boolean> = _isPrepared.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Disable video track decoding so MP4/MKV video files play audio-only smoothly with zero lag
    private val trackSelector = DefaultTrackSelector(appContext).apply {
        setParameters(
            buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startProgressTicker()
            } else {
                stopProgressTicker()
                updatePositionFromPlayer()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    _isPrepared.value = true
                    _errorMessage.value = null
                    val rawDuration = exoPlayer.duration
                    _durationMs.value = if (rawDuration != C.TIME_UNSET && rawDuration > 0L) rawDuration else 0L
                    updatePositionFromPlayer()
                }
                Player.STATE_ENDED -> {
                    _isPlaying.value = false
                    stopProgressTicker()
                    exoPlayer.seekTo(0L)
                    exoPlayer.pause()
                    _currentPositionMs.value = 0L
                }
                Player.STATE_IDLE -> {
                    _isPrepared.value = false
                }
                Player.STATE_BUFFERING -> {
                    val rawDuration = exoPlayer.duration
                    if (rawDuration != C.TIME_UNSET && rawDuration > 0L) {
                        _durationMs.value = rawDuration
                    }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _isPlaying.value = false
            _isPrepared.value = false
            stopProgressTicker()
            _errorMessage.value = "فایل صوتی یا ویدیویی قابل پخش نیست. لطفاً دوباره انتخاب کنید."
        }
    }

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setTrackSelector(trackSelector)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
        .apply {
            addListener(playerListener)
        }

    fun prepareUri(uri: Uri) {
        if (loadedUri == uri && _isPrepared.value) return
        loadTrackInternal(uri = uri, autoPlay = false)
    }

    fun playUri(uri: Uri) {
        if (loadedUri == uri && _isPrepared.value) {
            resume()
            return
        }
        loadTrackInternal(uri = uri, autoPlay = true)
    }

    fun switchTrack(uri: Uri, autoPlay: Boolean = false) {
        if (loadedUri == uri && _isPrepared.value) {
            if (autoPlay) resume()
            return
        }
        loadTrackInternal(uri = uri, autoPlay = autoPlay)
    }

    private fun loadTrackInternal(uri: Uri, autoPlay: Boolean) {
        loadedUri = uri
        _errorMessage.value = null
        _isPrepared.value = false
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
        try {
            exoPlayer.stop()
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.playbackParameters = PlaybackParameters(_playbackSpeed.value, 1.0f)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = autoPlay
        } catch (e: Exception) {
            _errorMessage.value = "دسترسی به فایل انتخاب‌شده امکان‌پذیر نیست."
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.5f, 1.5f)
        _playbackSpeed.value = safeSpeed
        exoPlayer.playbackParameters = PlaybackParameters(safeSpeed, 1.0f)
    }

    fun pause() {
        exoPlayer.pause()
        updatePositionFromPlayer()
    }

    fun resume() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0L)
        }
        exoPlayer.play()
    }

    fun seekTo(ms: Long) {
        val duration = _durationMs.value
        val clamped = if (duration > 0L) ms.coerceIn(0L, duration) else ms.coerceAtLeast(0L)
        exoPlayer.seekTo(clamped)
        _currentPositionMs.value = clamped
    }

    fun seekForward5s() {
        seekTo(exoPlayer.currentPosition + 5_000L)
    }

    fun seekBackward5s() {
        seekTo(exoPlayer.currentPosition - 5_000L)
    }

    fun stop() {
        stopProgressTicker()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        loadedUri = null
        _isPlaying.value = false
        _isPrepared.value = false
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
        _errorMessage.value = null
    }

    fun release() {
        stopProgressTicker()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
        scope.cancel()
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                updatePositionFromPlayer()
                delay(200L)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updatePositionFromPlayer() {
        val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
        _currentPositionMs.value = pos
        val dur = exoPlayer.duration
        if (dur != C.TIME_UNSET && dur > 0L) {
            _durationMs.value = dur
        }
    }
}
