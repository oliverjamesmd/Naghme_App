package com.naghmeh.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Forward5
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay5
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.naghmeh.app.data.MediaTrackEntity
import com.naghmeh.app.data.SongWithTracks
import com.naghmeh.app.data.TrackType
import com.naghmeh.app.ui.SongViewModel
import com.naghmeh.app.ui.theme.PersianFontOption
import com.naghmeh.app.ui.theme.fontFamilyFor
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    songWithTracks: SongWithTracks?,
    selectedTrackId: Long?,
    selectedFontOption: PersianFontOption,
    isDarkTheme: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    audioError: String?,
    isRecording: Boolean,
    recordingDurationSec: Long,
    recorderError: String?,
    onToggleTheme: () -> Unit,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onIncreaseFontSize: () -> Unit,
    onDecreaseFontSize: () -> Unit,
    onSelectFontOption: (PersianFontOption) -> Unit,
    onSelectTrack: (MediaTrackEntity) -> Unit,
    onSelectPlaybackSpeed: (Float) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward5s: () -> Unit,
    onSeekBackward5s: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onClearRecorderError: () -> Unit,
    onLeaveScreen: () -> Unit
) {
    val context = LocalContext.current
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedMessage = null
            onStartRecording()
        } else {
            permissionDeniedMessage = "برای ضبط صدا، دسترسی میکروفون لازم است."
        }
    }

    // Keep screen awake automatically while practicing lyrics
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onLeaveScreen()
        }
    }

    val song = songWithTracks?.song
    val tracks = songWithTracks?.tracks.orEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = song?.title ?: "در حال بارگذاری...",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = if (isDarkTheme) "حالت روز" else "حالت شب",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        if (song != null) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = onDecreaseFontSize,
                                        enabled = song.fontSize > SongViewModel.MIN_FONT_SIZE,
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier.height(42.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "A-",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = onIncreaseFontSize,
                                        enabled = song.fontSize < SongViewModel.MAX_FONT_SIZE,
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier.height(42.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "A+",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { onEditClick(song.id) },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "ویرایش شعر"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Visible Persian Font Selector Chip Row
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PersianFontOption.entries.forEach { option ->
                            val selected = option == selectedFontOption
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectFontOption(option) },
                                label = {
                                    Text(
                                        text = option.shortLabelFa,
                                        fontFamily = fontFamilyFor(option),
                                        fontSize = 14.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.height(40.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        },
        bottomBar = {
            if (song != null) {
                RehearsalPlayerDock(
                    tracks = tracks,
                    selectedTrackId = selectedTrackId,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    playbackSpeed = playbackSpeed,
                    audioError = audioError,
                    isRecording = isRecording,
                    recordingDurationSec = recordingDurationSec,
                    recorderError = permissionDeniedMessage ?: recorderError,
                    onSelectTrack = onSelectTrack,
                    onSelectPlaybackSpeed = onSelectPlaybackSpeed,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeekTo = onSeekTo,
                    onSeekForward5s = onSeekForward5s,
                    onSeekBackward5s = onSeekBackward5s,
                    onRecordButtonClick = {
                        onClearRecorderError()
                        permissionDeniedMessage = null
                        if (isRecording) {
                            onStopRecording()
                        } else {
                            val hasMicPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasMicPermission) {
                                onStartRecording()
                            } else {
                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        if (song == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "صفحه روشن می‌ماند · ${selectedFontOption.shortLabelFa} · اندازه: ${song.fontSize.toInt()}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = song.lyrics,
                    fontFamily = fontFamilyFor(selectedFontOption),
                    fontSize = song.fontSize.sp,
                    lineHeight = (song.fontSize * selectedFontOption.lineHeightMultiplier).sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun RehearsalPlayerDock(
    tracks: List<MediaTrackEntity>,
    selectedTrackId: Long?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    audioError: String?,
    isRecording: Boolean,
    recordingDurationSec: Long,
    recorderError: String?,
    onSelectTrack: (MediaTrackEntity) -> Unit,
    onSelectPlaybackSpeed: (Float) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekForward5s: () -> Unit,
    onSeekBackward5s: () -> Unit,
    onRecordButtonClick: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableFloatStateOf(0f) }
    var speedMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(currentPositionMs, isDragging) {
        if (!isDragging) {
            dragPositionMs = currentPositionMs.toFloat()
        }
    }

    val safeDuration = durationMs.coerceAtLeast(1L).toFloat()
    val sliderValue = if (isDragging) dragPositionMs else currentPositionMs.toFloat().coerceIn(0f, safeDuration)
    val displayedPositionMs = if (isDragging) dragPositionMs.toLong() else currentPositionMs

    val speedOptions = listOf(0.75f to "0.75x", 0.9f to "0.9x", 1.0f to "1.0x", 1.5f to "1.5x")
    val activeSpeedLabel = speedOptions.firstOrNull {
        kotlin.math.abs(it.first - playbackSpeed) < 0.01f
    }?.second ?: "1.0x"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 2.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tracks.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tracks.forEach { track ->
                                val isSelected = track.trackId == selectedTrackId ||
                                    (selectedTrackId == null && track == tracks.first())
                                val isRec = track.trackType == TrackType.RECORDING.name
                                val containerColor = when {
                                    isSelected && isRec -> MaterialTheme.colorScheme.secondaryContainer
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                }
                                val contentColor = when {
                                    isSelected && isRec -> MaterialTheme.colorScheme.onSecondaryContainer
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Surface(
                                    onClick = { onSelectTrack(track) },
                                    shape = CircleShape,
                                    color = containerColor,
                                    contentColor = contentColor,
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(horizontal = 12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isRec) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Mic,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Text(
                                                text = track.title,
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = LocalTextStyle.current.copy(
                                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                                ),
                                                modifier = Modifier
                                                    .widthIn(max = 150.dp)
                                                    .offset(y = (-2).dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "بدون فایل صوتی",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (tracks.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = "${formatMillisToMmSs(displayedPositionMs)} / ${formatMillisToMmSs(durationMs)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!audioError.isNullOrBlank()) {
                    Text(
                        text = audioError,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (!recorderError.isNullOrBlank()) {
                    Text(
                        text = recorderError,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (tracks.isNotEmpty()) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Slider(
                            value = sliderValue.coerceIn(0f, safeDuration),
                            onValueChange = { next ->
                                isDragging = true
                                dragPositionMs = next
                            },
                            onValueChangeFinished = {
                                isDragging = false
                                onSeekTo(dragPositionMs.toLong())
                            },
                            valueRange = 0f..safeDuration,
                            enabled = durationMs > 0L,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                        )
                    }
                }

                // Single Compact 44dp Control Row: Speed (Right) + (-5s / Play / +5s) + Record (Left)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tracks.isNotEmpty()) {
                        Box {
                            Surface(
                                onClick = { speedMenuExpanded = true },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 11.dp)
                                ) {
                                    Text(
                                        text = activeSpeedLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowUp,
                                        contentDescription = "انتخاب سرعت پخش",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = speedMenuExpanded,
                                onDismissRequest = { speedMenuExpanded = false }
                            ) {
                                speedOptions.forEach { (speedValue, label) ->
                                    val selected = kotlin.math.abs(playbackSpeed - speedValue) < 0.01f
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = label,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        },
                                        onClick = {
                                            onSelectPlaybackSpeed(speedValue)
                                            speedMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onSeekBackward5s,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Replay5,
                                        contentDescription = "۵ ثانیه به عقب",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Surface(
                                    onClick = onTogglePlayPause,
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = if (isPlaying) "توقف" else "پخش",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onSeekForward5s,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Forward5,
                                        contentDescription = "۵ ثانیه به جلو",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onRecordButtonClick,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp),
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
                        modifier = if (tracks.isEmpty()) {
                            Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        } else {
                            Modifier.height(38.dp)
                        }
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isRecording) {
                                "پایان (${formatMillisToMmSs(recordingDurationSec * 1000L)})"
                            } else {
                                "ضبط"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatMillisToMmSs(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
