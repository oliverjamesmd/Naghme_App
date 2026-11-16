package com.naghmeh.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naghmeh.app.data.CategoryEntity
import com.naghmeh.app.data.SongWithTracks
import com.naghmeh.app.ui.toPersianDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    songs: List<SongWithTracks>,
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    searchQuery: String,
    isDarkTheme: Boolean,
    isBackupLoading: Boolean = false,
    backupOperationMessage: String? = null,
    backupUriToShare: Uri? = null,
    onToggleTheme: () -> Unit,
    onSelectCategory: (Long?) -> Unit,
    onCreateCategory: (String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSongClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onDeleteConfirm: (SongWithTracks) -> Unit,
    onCreateBackup: () -> Unit = {},
    onRestoreBackup: (Uri) -> Unit = {},
    onClearBackupShareUri: () -> Unit = {},
    onClearBackupMessage: () -> Unit = {}
) {
    val context = LocalContext.current
    var songPendingDeletion by remember { mutableStateOf<SongWithTracks?>(null) }
    var categoryPendingDeletion by remember { mutableStateOf<CategoryEntity?>(null) }
    var showCategoryManagerDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onRestoreBackup(uri)
        }
    }

    LaunchedEffect(backupUriToShare) {
        backupUriToShare?.let { uri ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری و ذخیره فایل پشتیبان"))
            onClearBackupShareUri()
        }
    }

    val categoryMap = remember(categories) {
        categories.associateBy { it.categoryId }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "شعرها و آهنگ‌های من",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showBackupDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Backup,
                            contentDescription = "پشتیبان‌گیری و بازیابی",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = if (isDarkTheme) "تغییر به حالت روز" else "تغییر به حالت شب",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.height(58.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                },
                text = {
                    Text(
                        text = "افزودن شعر جدید",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 10.dp),
                placeholder = {
                    Text(
                        text = "جستجو در عنوان یا متن شعر...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "جستجو",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "پاک کردن جستجو"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                textStyle = MaterialTheme.typography.bodyLarge
            )

            // Categories filter chip bar + "+ دسته جدید"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { onSelectCategory(null) },
                    label = {
                        Text(
                            text = "همه",
                            fontSize = 14.sp,
                            fontWeight = if (selectedCategoryId == null) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.height(42.dp)
                )

                categories.forEach { category ->
                    val isSelected = selectedCategoryId == category.categoryId
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectCategory(category.categoryId) },
                        label = {
                            Text(
                                text = category.name,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
                    onClick = { showCategoryManagerDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "دسته جدید / مدیریت",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (songs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (searchQuery.isBlank() && selectedCategoryId == null) {
                                "هنوز شعری اضافه نشده است"
                            } else {
                                "شعری در این دسته یا جستجو پیدا نشد"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (searchQuery.isBlank() && selectedCategoryId == null) {
                                "برای شروع دکمه «افزودن شعر جدید» را لمس کنید"
                            } else {
                                "دسته «همه» را انتخاب کنید یا عبارت جستجو را تغییر دهید"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = songs,
                        key = { it.song.id }
                    ) { item ->
                        val categoryName = item.song.categoryId?.let { categoryMap[it]?.name }
                        SongListCard(
                            songWithTracks = item,
                            categoryName = categoryName,
                            onClick = { onSongClick(item.song.id) },
                            onEditClick = { onEditClick(item.song.id) },
                            onDeleteClick = { songPendingDeletion = item }
                        )
                    }
                }
            }
        }
    }

    if (showCategoryManagerDialog) {
        AlertDialog(
            onDismissRequest = {
                showCategoryManagerDialog = false
                newCategoryName = ""
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "دسته‌بندی‌های شعر",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("مثلاً: شعر حافظ") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (newCategoryName.trim().isNotEmpty()) {
                                onCreateCategory(newCategoryName)
                                newCategoryName = ""
                            }
                        },
                        enabled = newCategoryName.trim().isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(text = "افزودن دسته جدید", fontWeight = FontWeight.Bold)
                    }

                    if (categories.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            categories.forEach { cat ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { categoryPendingDeletion = cat }) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "حذف دسته ${cat.name}",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        showCategoryManagerDialog = false
                        newCategoryName = ""
                    }
                ) {
                    Text(text = "بستن")
                }
            }
        )
    }

    songPendingDeletion?.let { target ->
        AlertDialog(
            onDismissRequest = { songPendingDeletion = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "حذف شعر",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "آیا مطمئن هستید که می‌خواهید «${target.song.title}» را حذف کنید؟",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteConfirm(target)
                        songPendingDeletion = null
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
                    onClick = { songPendingDeletion = null },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(text = "انصراف")
                }
            }
        )
    }

    categoryPendingDeletion?.let { target ->
        AlertDialog(
            onDismissRequest = { categoryPendingDeletion = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "حذف دسته‌بندی",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "آیا مطمئن هستید که می‌خواهید دسته‌بندی «${target.name}» را حذف کنید؟ شعرهای داخل این دسته حذف نمی‌شوند.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(target)
                        categoryPendingDeletion = null
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
                    onClick = { categoryPendingDeletion = null },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(text = "انصراف")
                }
            }
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { if (!isBackupLoading) showBackupDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "پشتیبان‌گیری و بازیابی",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "می‌توانید از تمام شعرها، دسته‌ها و صداهای ضبط‌شده فایل پشتیبان تهیه کنید، یا فایل قبلی خود را بازیابی نمایید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isBackupLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "در حال پردازش فایل...")
                        }
                    } else {
                        Button(
                            onClick = {
                                onCreateBackup()
                                showBackupDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "تهیه فایل پشتیبان (اشتراک‌گذاری)", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                restoreLauncher.launch(
                                    arrayOf(
                                        "application/zip",
                                        "application/x-zip-compressed",
                                        "application/json",
                                        "*/*"
                                    )
                                )
                                showBackupDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "بازیابی اطلاعات از فایل", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = { showBackupDialog = false },
                    enabled = !isBackupLoading
                ) {
                    Text(text = "بستن")
                }
            }
        )
    }

    backupOperationMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = onClearBackupMessage,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(text = "پیام سیستم", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(text = msg, style = MaterialTheme.typography.bodyLarge)
            },
            confirmButton = {
                Button(onClick = onClearBackupMessage) {
                    Text(text = "متوجه شدم", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SongListCard(
    songWithTracks: SongWithTracks,
    categoryName: String?,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val song = songWithTracks.song
    val tracks = songWithTracks.tracks
    val previewLines = remember(song.lyrics) {
        song.lyrics
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("\n")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "ویرایش ${song.title}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "حذف ${song.title}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = previewLines,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (tracks.isNotEmpty() || !categoryName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tracks.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val trackSummary = if (tracks.size == 1) {
                                    tracks.first().title
                                } else {
                                    "${toPersianDigits(tracks.size)} فایل صوتی و ضبط‌شده"
                                }
                                Text(
                                    text = trackSummary,
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (!categoryName.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
