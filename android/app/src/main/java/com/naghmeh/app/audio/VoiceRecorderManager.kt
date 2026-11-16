package com.naghmeh.app.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
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
import java.io.File

class VoiceRecorderManager(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var timerJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0L)
    val recordingDurationSec: StateFlow<Long> = _recordingDurationSec.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun startRecording(): Boolean {
        if (_isRecording.value) return false
        _errorMessage.value = null

        val recordingsDir = File(appContext.filesDir, "recordings").apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(recordingsDir, "naghmeh_practice_${System.currentTimeMillis()}.m4a")
        outputFile = targetFile

        return try {
            val recorder = createRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44_100)
                setAudioEncodingBitRate(128_000)
                setOutputFile(targetFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            _isRecording.value = true
            _recordingDurationSec.value = 0L
            startTimer()
            true
        } catch (e: Exception) {
            cleanupRecorder()
            targetFile.delete()
            outputFile = null
            _isRecording.value = false
            _errorMessage.value = "شروع ضبط صدا ممکن نشد. دسترسی میکروفون را بررسی کنید."
            false
        }
    }

    fun stopRecording(): File? {
        if (!_isRecording.value) return null
        stopTimer()
        val recordedFile = outputFile
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            recordedFile?.delete()
            _errorMessage.value = "ضبط بسیار کوتاه بود یا ذخیره نشد."
            mediaRecorder = null
            outputFile = null
            _isRecording.value = false
            _recordingDurationSec.value = 0L
            return null
        }

        mediaRecorder = null
        outputFile = null
        _isRecording.value = false
        _recordingDurationSec.value = 0L

        return if (recordedFile != null && recordedFile.exists() && recordedFile.length() > 0L) {
            recordedFile
        } else {
            recordedFile?.delete()
            null
        }
    }

    fun cancelRecording() {
        stopTimer()
        cleanupRecorder()
        outputFile?.delete()
        outputFile = null
        _isRecording.value = false
        _recordingDurationSec.value = 0L
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun release() {
        cancelRecording()
        scope.cancel()
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(appContext)
        } else {
            MediaRecorder()
        }
    }

    private fun cleanupRecorder() {
        try {
            mediaRecorder?.reset()
        } catch (_: Exception) {
        }
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                _recordingDurationSec.value += 1L
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
}
