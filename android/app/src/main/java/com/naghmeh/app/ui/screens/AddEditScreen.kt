package com.naghmeh.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.naghmeh.app.audio.DeviceAudioItem
import com.naghmeh.app.data.CategoryEntity
import com.naghmeh.app.data.TrackType
import com.naghmeh.app.ui.AddEditUiState
import com.naghmeh.app.ui.DraftTrack
import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextAlign
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    uiState: AddEditUiState,
    categories: List<CategoryEntity>,
    isDarkTheme: Boolean,
    isRecording: Boolean,
    recordingDurationSec: Long,
    recorderError: String?,
    inlinePlayingKey: String?,
    isAudioPlaying: Boolean,
    deviceAudios: List<DeviceAudioItem> = emptyList(),
    isScanningAudios: Boolean = false,
    onLoadDeviceAudios: () -> Unit = {},
    onToggleTheme: () -> Unit,
    onTitleChange: (String) -> Unit,
    onLyricsChange: (String) -> Unit,
    onCategorySelect: (Long?) -> Unit,
    onCreateAndSelectCategory: (String) -> Unit,
    onAddMusicSlot: () -> Unit,
    onUpdateMusicTitle: (String, String) -> Unit,
    onAttachFileToMusicSlot: (String, Uri, String) -> Unit,
    onRemoveMusicSlot: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onToggleInlineRecordingPlayback: (DraftTrack) -> Unit,
    onRemoveRecordedTrack: (String) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var activePickerTargetKey by remember { mutableStateOf<String?>(null) }
    var showPickerSourceDialog by remember { mutableStateOf(false) }
    var showDeviceAudioPickerDialog by remember { mutableStateOf(false) }
    var audioSearchQuery by remember { mutableStateOf("") }
    var micPermissionError by remember { mutableStateOf<String?>(null) }
    var showInlineCategoryInput by remember { mutableStateOf(false) }
    var newCategoryDraft by remember { mutableStateOf("") }
    var trackPendingDeletion by remember { mutableStateOf<Pair<TrackType, DraftTrack>?>(null) }

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var audioPermissionDenied by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            audioPermissionDenied = false
            showPickerSourceDialog = false
            audioSearchQuery = ""
            onLoadDeviceAudios()
            showDeviceAudioPickerDialog = true
        } else {
            audioPermissionDenied = true
        }
    }

    val previewPlayer = remember { MediaPlayer() }
    var previewPlayingUri by remember { mutableStateOf<Uri?>(null) }

    fun stopPreview() {
        try {
            if (previewPlayer.isPlaying) {
                previewPlayer.stop()
            }
            previewPlayer.reset()
        } catch (e: Exception) {
        } finally {
            previewPlayingUri = null
        }
    }

    fun startPreview(uri: Uri) {
        try {
            stopPreview()
            previewPlayer.setDataSource(context, uri)
            previewPlayer.prepare()
            previewPlayer.start()
            previewPlayer.setOnCompletionListener {
                previewPlayingUri = null
            }
            previewPlayingUri = uri
        } catch (e: Exception) {
            stopPreview()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (previewPlayer.isPlaying) {
                    previewPlayer.stop()
                }
                previewPlayer.release()
            } catch (e: Exception) {
            }
        }
    }

    val videoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val galleryVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            val targetKey = activePickerTargetKey
            if (uri != null && targetKey != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                }
                val displayName = resolveMediaDisplayName(context, uri)
                onAttachFileToMusicSlot(targetKey, uri, displayName)
            }
        }
        activePickerTargetKey = null
    }

    fun openGalleryVideo() {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).apply {
            type = "video/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val targetIntent = if (pickIntent.resolveActivity(context.packageManager) != null) {
            Intent.createChooser(pickIntent, "انتخاب ویدیو از گالری")
        } else {
            val getContentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "video/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(getContentIntent, "انتخاب ویدیو از گالری")
        }

        try {
            galleryVideoLauncher.launch(targetIntent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "video/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                galleryVideoLauncher.launch(fallbackIntent)
            } catch (ignored: Exception) {
            }
        }
    }

    val videoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showPickerSourceDialog = false
            openGalleryVideo()
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val targetKey = activePickerTargetKey
        if (uri != null && targetKey != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
            }
            val displayName = resolveMediaDisplayName(context, uri)
            onAttachFileToMusicSlot(targetKey, uri, displayName)
        }
        activePickerTargetKey = null
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            micPermissionError = null
            onStartRecording()
        } else {
            micPermissionError = "برای ضبط صدا، دسترسی میکروفون لازم است."
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = if (uiState.songId == null) "افزودن شعر جدید" else "ویرایش شعر",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "بازگشت"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = if (isDarkTheme) "حالت روز" else "حالت شب",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!uiState.errorMessage.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "عنوان شعر",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    placeholder = { Text("مثلاً: غزل شماره ۱ حافظ یا تصنیف مرغ سحر") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "دسته‌بندی (اختیاری)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val selectedCategoryIds = uiState.selectedCategoryIds.ifEmpty {
                        uiState.categoryId?.let { setOf(it) } ?: emptySet()
                    }

                    FilterChip(
                        selected = selectedCategoryIds.isEmpty(),
                        onClick = { onCategorySelect(null) },
                        label = { Text("بدون دسته", fontSize = 14.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.height(42.dp)
                    )

                    categories.forEach { category ->
                        val selected = selectedCategoryIds.contains(category.categoryId)
                        FilterChip(
                            selected = selected,
                            onClick = { onCategorySelect(category.categoryId) },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = category.name,
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.height(42.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = { showInlineCategoryInput = !showInlineCategoryInput },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("دسته جدید", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (showInlineCategoryInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCategoryDraft,
                            onValueChange = { newCategoryDraft = it },
                            placeholder = { Text("نام دسته (مثلاً: شعر حافظ)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newCategoryDraft.trim().isNotEmpty()) {
                                    onCreateAndSelectCategory(newCategoryDraft)
                                    newCategoryDraft = ""
                                    showInlineCategoryInput = false
                                }
                            },
                            enabled = newCategoryDraft.trim().isNotEmpty(),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text("افزودن", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "متن شعر",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    FilledTonalButton(
                        onClick = {
                            val pasted = clipboardManager.getText()?.text.orEmpty()
                            if (pasted.isNotBlank()) {
                                val nextLyrics = if (uiState.lyrics.isBlank()) {
                                    pasted
                                } else {
                                    "${uiState.lyrics.trimEnd()}\n$pasted"
                                }
                                onLyricsChange(nextLyrics)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "الصاق متن", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                OutlinedTextField(
                    value = uiState.lyrics,
                    onValueChange = onLyricsChange,
                    placeholder = { Text("متن کامل شعر یا ترانه را اینجا بنویسید یا الصاق کنید...") },
                    minLines = 7,
                    maxLines = 14,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Section 1: Dedicated Music Tracks Section ("فایل‌های آهنگ و همراهی")
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "فایل‌های آهنگ و همراهی",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    uiState.musicTracks.forEach { track ->
                        MusicTrackSlotCard(
                            track = track,
                            onTitleChange = { onUpdateMusicTitle(track.localKey, it) },
                            onPickFile = {
                                activePickerTargetKey = track.localKey
                                showPickerSourceDialog = true
                            },
                            onRemove = { trackPendingDeletion = TrackType.MUSIC to track }
                        )
                    }

                    OutlinedButton(
                        onClick = onAddMusicSlot,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "افزودن آهنگ جدید",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Section 2: Dedicated Voice Practice Recorder ("استودیوی ضبط صدا")
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "استودیوی ضبط صدا",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val activeMicError = micPermissionError ?: recorderError
                    if (!activeMicError.isNullOrBlank()) {
                        Text(
                            text = activeMicError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = {
                            if (isRecording) {
                                onStopRecording()
                            } else {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    micPermissionError = null
                                    onStartRecording()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            contentColor = if (isRecording) {
                                MaterialTheme.colorScheme.onError
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRecording) {
                                "توقف ضبط (${formatSecondsMmSs(recordingDurationSec)})"
                            } else {
                                "ضبط صدای تمرین"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    uiState.recordedTracks.forEach { rec ->
                        val isPlayingThis = inlinePlayingKey == rec.localKey && isAudioPlaying
                        RecordedTrackRowCard(
                            track = rec,
                            isPlaying = isPlayingThis,
                            onTogglePlay = { onToggleInlineRecordingPlayback(rec) },
                            onDelete = { trackPendingDeletion = TrackType.RECORDING to rec }
                        )
                    }
                }
            }

            Button(
                onClick = onSaveClick,
                enabled = !uiState.isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "ذخیره", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    trackPendingDeletion?.let { (trackType, targetTrack) ->
        val fallbackLabel = if (trackType == TrackType.MUSIC) "این آهنگ" else "این فایل ضبط‌شده"
        val displayTitle = targetTrack.title.trim().ifEmpty { fallbackLabel }
        AlertDialog(
            onDismissRequest = { trackPendingDeletion = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = if (trackType == TrackType.MUSIC) "حذف فایل آهنگ" else "حذف صدای ضبط‌شده",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "آیا مطمئن هستید که می‌خواهید «$displayTitle» را حذف کنید؟",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (trackType == TrackType.MUSIC) {
                            onRemoveMusicSlot(targetTrack.localKey)
                        } else {
                            onRemoveRecordedTrack(targetTrack.localKey)
                        }
                        trackPendingDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(text = "بله، حذف شود", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { trackPendingDeletion = null },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(text = "انصراف")
                }
            }
        )
    }

    if (showPickerSourceDialog) {
        AlertDialog(
            onDismissRequest = {
                showPickerSourceDialog = false
                activePickerTargetKey = null
                audioPermissionDenied = false
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "انتخاب فایل همراهی",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "منبع مورد نظر برای انتخاب فایل را مشخص کنید:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FilledTonalButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                audioPermission
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                audioPermissionDenied = false
                                showPickerSourceDialog = false
                                audioSearchQuery = ""
                                onLoadDeviceAudios()
                                showDeviceAudioPickerDialog = true
                            } else {
                                audioPermissionLauncher.launch(audioPermission)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "آهنگ‌های گوشی", fontWeight = FontWeight.Bold)
                    }

                    if (audioPermissionDenied) {
                        Text(
                            text = "برای نمایش خودکار آهنگ‌های گوشی، دسترسی لازم است. همچنین می‌توانید از بخش «پوشه‌ها و فایل‌ها» استفاده کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                videoPermission
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                showPickerSourceDialog = false
                                openGalleryVideo()
                            } else {
                                videoPermissionLauncher.launch(videoPermission)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "ویدیو از گالری", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            showPickerSourceDialog = false
                            mediaPickerLauncher.launch(arrayOf("audio/*", "video/*"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "پوشه‌ها و فایل‌ها")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showPickerSourceDialog = false
                        activePickerTargetKey = null
                        audioPermissionDenied = false
                    }
                ) {
                    Text(text = "انصراف")
                }
            }
        )
    }

    if (showDeviceAudioPickerDialog) {
        val filteredAudios = remember(deviceAudios, audioSearchQuery) {
            val q = audioSearchQuery.trim()
            if (q.isEmpty()) {
                deviceAudios
            } else {
                deviceAudios.filter {
                    it.title.contains(q, ignoreCase = true) ||
                        it.artist.contains(q, ignoreCase = true) ||
                        it.displayName.contains(q, ignoreCase = true)
                }
            }
        }

        AlertDialog(
            onDismissRequest = {
                stopPreview()
                showDeviceAudioPickerDialog = false
                activePickerTargetKey = null
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "آهنگ‌های شناسایی‌شده در گوشی",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = audioSearchQuery,
                        onValueChange = { audioSearchQuery = it },
                        placeholder = { Text("جستجو در نام آهنگ یا خواننده...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isScanningAudios) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "در حال بارگذاری آهنگ‌ها...")
                        }
                    } else if (filteredAudios.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (audioSearchQuery.isBlank()) {
                                    "هیچ فایل صوتی در گوشی یافت نشد."
                                } else {
                                    "آهنگی با این نام پیدا نشد."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            OutlinedButton(
                                onClick = {
                                    stopPreview()
                                    showDeviceAudioPickerDialog = false
                                    mediaPickerLauncher.launch(arrayOf("audio/*", "video/*"))
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "انتخاب مستقیم از فایل‌ها")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredAudios) { audio ->
                                val isThisPreviewPlaying = previewPlayingUri == audio.uri
                                Surface(
                                    color = if (isThisPreviewPlaying) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = if (isThisPreviewPlaying) {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    } else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            stopPreview()
                                            activePickerTargetKey?.let { targetKey ->
                                                onAttachFileToMusicSlot(
                                                    targetKey,
                                                    audio.uri,
                                                    audio.displayName
                                                )
                                            }
                                            showDeviceAudioPickerDialog = false
                                            activePickerTargetKey = null
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilledTonalIconButton(
                                            onClick = {
                                                if (isThisPreviewPlaying) {
                                                    stopPreview()
                                                } else {
                                                    startPreview(audio.uri)
                                                }
                                            },
                                            modifier = Modifier.size(38.dp),
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = if (isThisPreviewPlaying) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                },
                                                contentColor = if (isThisPreviewPlaying) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                }
                                            )
                                        ) {
                                            Icon(
                                                imageVector = if (isThisPreviewPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = if (isThisPreviewPlaying) "توقف پخش" else "گوش دادن به آهنگ",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = audio.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val subText = if (audio.artist.isNotBlank()) {
                                                val totalSec = audio.durationMs / 1000
                                                val m = totalSec / 60
                                                val s = totalSec % 60
                                                "${audio.artist} • ${String.format(Locale.US, "%02d:%02d", m, s)}"
                                            } else {
                                                val totalSec = audio.durationMs / 1000
                                                val m = totalSec / 60
                                                val s = totalSec % 60
                                                String.format(Locale.US, "%02d:%02d", m, s)
                                            }
                                            Text(
                                                text = if (isThisPreviewPlaying) "در حال پخش پیش‌نمایش..." else subText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isThisPreviewPlaying) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        stopPreview()
                        showDeviceAudioPickerDialog = false
                        activePickerTargetKey = null
                    }
                ) {
                    Text(text = "انصراف")
                }
            }
        )
    }
}

@Composable
private fun MusicTrackSlotCard(
    track: DraftTrack,
    onTitleChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = track.title,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "حذف ${track.title}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            FilledTonalButton(
                onClick = onPickFile,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AudioFile,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (track.fileName.isNotBlank()) {
                        track.fileName
                    } else {
                        "انتخاب فایل (MP3, WAV, MP4/MKV)"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecordedTrackRowCard(
    track: DraftTrack,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalIconButton(
                onClick = onTogglePlay,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "توقف" else "پخش ${track.title}"
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val meta = buildString {
                    append(formatSecondsMmSs(track.durationSec))
                    if (track.displayTimestamp.isNotBlank()) {
                        append(" · ")
                        append(track.displayTimestamp)
                    }
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "حذف ${track.title}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatSecondsMmSs(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    return String.format(Locale.US, "%02d:%02d", safe / 60L, safe % 60L)
}

private fun resolveMediaDisplayName(context: Context, uri: Uri): String {
    var displayName: String? = null
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) {
                displayName = cursor.getString(idx)
            }
        }
    }
    return displayName?.takeIf { it.isNotBlank() }
        ?: uri.lastPathSegment
        ?: "فایل انتخاب‌شده"
}
