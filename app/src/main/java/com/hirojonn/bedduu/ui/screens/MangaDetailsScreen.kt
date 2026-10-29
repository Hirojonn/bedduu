package com.hirojonn.bedduu.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hirojonn.bedduu.ui.components.CoverArt
import com.hirojonn.bedduu.R
import com.hirojonn.bedduu.ui.AppViewModel
import com.hirojonn.bedduu.ui.ChapterItem
import com.hirojonn.bedduu.ui.MockManga
import com.hirojonn.bedduu.ui.theme.*

private val DetailBackground: Color
    @Composable get() = BackgroundColor
private val DetailAccent: Color
    @Composable get() = if (LocalIsDarkMode.current) PrimaryLight else PrimaryColor
private val DetailSurface: Color
    @Composable get() = SurfaceColor
private val DetailSurfaceVariant: Color
    @Composable get() = SurfaceVariant
private val DetailSecondary: Color
    @Composable get() = TextMain
private val DetailMuted: Color
    @Composable get() = TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailsScreen(
    rawManga: MockManga,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onReadChapter: (MockManga, ChapterItem) -> Unit
) {
    val manga = rawManga.normalizedForDetails()
    val context = LocalContext.current
    val mangaKey = manga.sourceUrl.ifBlank { manga.title.lowercase() }
    val isBookmarked = viewModel.bookmarkedManga.any {
        it.title.equals(manga.title, ignoreCase = true)
    }
    val isFavorite = mangaKey in viewModel.favoriteMangaKeys
    val chapters = viewModel.detailsChapters.ifEmpty { manga.chapters }
    val availableLanguages = chapters.mapNotNull { it.language.orEmpty().takeIf(String::isNotBlank) }
        .distinct()
    val availableUploaders = chapters.mapNotNull {
        it.uploader?.takeIf(String::isNotBlank)
            ?.takeUnless { uploader ->
                uploader.equals("Unknown", ignoreCase = true) ||
                    uploader.equals("Unknown group", ignoreCase = true)
            }
    }.distinct()
    val selectedLanguage = viewModel.selectedDetailsLanguage
    val selectedUploader = viewModel.selectedDetailsUploader
    val filteredChapters = chapters
        .filter { selectedLanguage == null || it.language.orEmpty() == selectedLanguage }
        .filter { selectedUploader == null || it.uploader == selectedUploader }
    val lastReadUrl = viewModel.lastReadChapterUrls[mangaKey]
    val lastReadIndex = chapters.indexOfFirst { it.url == lastReadUrl }
    val currentChapter = chapters.getOrNull(lastReadIndex) ?: chapters.firstOrNull()
    val readCount = (lastReadIndex + 1).coerceAtLeast(0)
    val progress = if (chapters.isEmpty() || lastReadIndex < 0) {
        0f
    } else {
        readCount.toFloat() / chapters.size
    }
    LaunchedEffect(mangaKey) {
        viewModel.loadMangaDetails(manga, context)
    }
    LaunchedEffect(availableLanguages, selectedLanguage) {
        if (selectedLanguage != null && selectedLanguage !in availableLanguages) {
            viewModel.setDetailsLanguage(null)
        }
    }
    LaunchedEffect(availableUploaders, selectedUploader) {
        if (selectedUploader != null && selectedUploader !in availableUploaders) {
            viewModel.setDetailsUploader(null)
        }
    }

    val source = manga.sourceDomain.ifBlank {
        stringResource(R.string.details_source_unknown)
    }.uppercase()
    val status = manga.status.orEmpty().ifBlank {
        stringResource(R.string.details_status_unknown)
    }.uppercase()

    BottomSheetScaffold(
        sheetContent = {
            MangaChaptersSheet(
                languages = availableLanguages,
                selectedLanguage = selectedLanguage,
                uploaders = availableUploaders,
                selectedUploader = selectedUploader,
                chapters = filteredChapters,
                currentChapterUrl = lastReadUrl,
                totalChapterCount = chapters.size,
                onLanguageSelected = viewModel::setDetailsLanguage,
                onUploaderSelected = viewModel::setDetailsUploader,
                onChapterSelected = { chapter ->
                    viewModel.readChapter(manga, chapter)
                    onReadChapter(manga.copy(chapters = chapters), chapter)
                },
                loading = viewModel.isLoadingDetails,
                loadMessage = viewModel.detailsLoadMessage,
                onRetry = { viewModel.loadMangaDetails(manga, context, forceRefresh = true) },
                onOpenSource = {
                    if (manga.sourceUrl.isNotBlank()) viewModel.currentBrowserUrl = manga.sourceUrl
                },
                hasSource = manga.sourceUrl.isNotBlank()
                )
        },
        sheetPeekHeight = 236.dp,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContainerColor = DetailSurface,
        containerColor = DetailBackground
    ) { scaffoldPadding ->
        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DetailBackground)
                    .padding(scaffoldPadding)
        ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    CoverArt(
                        url = manga.coverUrl,
                        contentDescription = "",
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(28.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        DetailBackground.copy(alpha = 0.28f),
                                        DetailBackground.copy(alpha = 0.8f),
                                        DetailBackground
                                    )
                                )
                            )
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 252.dp)
                ) {
                    item {
                        MangaDetailTopBar(
                        onBack = onBack,
                        onShare = {
                            val shareText = listOf(manga.title, manga.sourceUrl)
                                .filter(String::isNotBlank)
                                .joinToString("\n")
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    context.getString(R.string.details_share_chooser)
                                )
                            )
                        },
                        onDownload = {
                            Toast.makeText(
                                context,
                                R.string.details_download_unavailable,
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onMore = { viewModel.setDetailsOverflowMenuExpanded(true) },
                        menuExpanded = viewModel.isDetailsMoreMenuExpanded,
                        onDismissMenu = { viewModel.setDetailsOverflowMenuExpanded(false) },
                        onOpenSource = {
                            viewModel.setDetailsOverflowMenuExpanded(false)
                            if (manga.sourceUrl.isNotBlank()) {
                                viewModel.currentBrowserUrl = manga.sourceUrl
                            }
                        },
                        onRetry = {
                            viewModel.setDetailsOverflowMenuExpanded(false)
                            viewModel.loadMangaDetails(manga, context, forceRefresh = true)
                        },
                        openSourceEnabled = manga.sourceUrl.isNotBlank()
                        )
                    }
                    item {
                        MangaDetailHeader(
                        coverUrl = manga.coverUrl,
                        statusAndSource = stringResource(
                            R.string.details_status_source,
                            status,
                            source
                        ),
                        title = manga.title,
                        alternateTitle = manga.alternateTitle.orEmpty().ifBlank {
                            stringResource(R.string.details_alt_title_unavailable)
                        },
                        author = manga.author.ifBlank {
                            stringResource(R.string.details_author_unknown)
                        }
                        )
                    }
                    item {
                        MangaProgressCard(
                        readCount = readCount,
                        totalCount = chapters.size,
                        progress = progress,
                        currentChapterTitle = chapters.getOrNull(lastReadIndex)?.title
                        )
                    }
                    item {
                        MangaDetailActions(
                        continueLabel = when {
                            currentChapter != null && lastReadIndex >= 0 ->
                                stringResource(R.string.details_continue_chapter, currentChapter.title)
                            currentChapter != null ->
                                stringResource(R.string.details_continue_chapter, currentChapter.title)
                            else -> stringResource(R.string.details_continue)
                        },
                        canContinue = currentChapter != null,
                        isFavorite = isFavorite,
                        isBookmarked = isBookmarked,
                        onContinue = {
                            currentChapter?.let { chapter ->
                                viewModel.readChapter(manga, chapter)
                                onReadChapter(manga.copy(chapters = chapters), chapter)
                            }
                        },
                        onFavorite = { viewModel.toggleFavorite(manga) },
                        onBookmark = {
                            if (isBookmarked) {
                                viewModel.removeBookmark(manga)
                            } else {
                                viewModel.bookmarkManga(
                                    title = manga.title,
                                    url = manga.sourceUrl,
                                    coverUrl = manga.coverUrl,
                                    author = manga.author,
                                    chapters = chapters
                                )
                            }
                        }
                        )
                    }
                    item {
                        MangaDescription(
                        description = manga.description.orEmpty().ifBlank {
                            stringResource(R.string.details_description_unavailable)
                        }
                        )
                    }
                    item {
                        MangaTags(
                            tags = manga.tags.orEmpty().ifEmpty {
                                listOf(stringResource(R.string.details_tags_unavailable))
                            }
                        )
                }
            }
        }
    }
}

private fun MockManga.normalizedForDetails(): MockManga = MockManga(
    title = title.orEmpty().ifBlank { "Manga" },
    author = author.orEmpty(),
    coverUrl = coverUrl.orEmpty(),
    sourceUrl = sourceUrl.orEmpty(),
    sourceDomain = sourceDomain.orEmpty(),
    chapters = chapters.orEmpty().map { chapter ->
        ChapterItem(
            title = chapter.title.orEmpty(),
            url = chapter.url.orEmpty(),
            uploader = chapter.uploader,
            language = chapter.language.orEmpty(),
            date = chapter.date.orEmpty()
        )
    },
    alternateTitle = alternateTitle.orEmpty(),
    description = description.orEmpty(),
    tags = tags.orEmpty().filterNotNull(),
    status = status.orEmpty()
)

@Composable
fun MangaDetailTopBar(
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onMore: () -> Unit,
    menuExpanded: Boolean = false,
    onDismissMenu: () -> Unit = {},
    onOpenSource: () -> Unit = {},
    onRetry: () -> Unit = {},
    openSourceEnabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DetailCircleIconButton(
            icon = Icons.Default.ArrowBack,
            description = stringResource(R.string.details_back),
            onClick = onBack
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailCircleIconButton(
                icon = Icons.Default.Share,
                description = stringResource(R.string.details_share),
                onClick = onShare
            )
            DetailCircleIconButton(
                icon = Icons.Default.Download,
                description = stringResource(R.string.details_download),
                onClick = onDownload
            )
            Box {
                DetailCircleIconButton(
                    icon = Icons.Default.MoreVert,
                    description = stringResource(R.string.details_more),
                    onClick = onMore
                )
                DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.details_open_source)) },
                        onClick = onOpenSource,
                        enabled = openSourceEnabled
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.details_retry)) },
                        onClick = onRetry
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailCircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(DetailBackground.copy(alpha = 0.55f))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun MangaDetailHeader(
    coverUrl: String,
    statusAndSource: String,
    title: String,
    alternateTitle: String,
    author: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        CoverArt(
            url = coverUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 118.dp, height = 168.dp)
                .shadow(12.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(DetailSurfaceVariant)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = statusAndSource,
                color = DetailAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                color = TextMain,
                fontSize = 26.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = alternateTitle,
                color = DetailSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.details_author_line, author),
                color = DetailSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MangaProgressCard(
    readCount: Int,
    totalCount: Int,
    progress: Float,
    currentChapterTitle: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DetailSurface.copy(alpha = 0.96f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val label = if (readCount > 0 && currentChapterTitle != null) {
                stringResource(
                    R.string.details_progress_started,
                    currentChapterTitle,
                    totalCount,
                    (totalCount - readCount).coerceAtLeast(0)
                )
            } else {
                stringResource(R.string.details_progress_not_started, totalCount)
            }
            Text(
                text = label,
                color = DetailSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(
                    R.string.details_percent,
                    (progress.coerceIn(0f, 1f) * 100).toInt()
                ),
                color = DetailAccent,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(DetailSurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(DetailAccent)
            )
        }
    }
}

@Composable
fun MangaDetailActions(
    continueLabel: String,
    canContinue: Boolean,
    isFavorite: Boolean,
    isBookmarked: Boolean,
    onContinue: () -> Unit,
    onFavorite: () -> Unit,
    onBookmark: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onContinue,
            enabled = canContinue,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DetailAccent,
                contentColor = Color(0xFF08202A),
                disabledContainerColor = DetailSurfaceVariant,
                disabledContentColor = DetailMuted
            ),
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(
                text = continueLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        OutlinedIconButton(
            onClick = onFavorite,
            modifier = Modifier.size(52.dp),
            shape = CircleShape
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = stringResource(
                    if (isFavorite) R.string.details_unfavorite else R.string.details_favorite
                ),
                tint = if (isFavorite) DetailAccent else DetailSecondary
            )
        }
        OutlinedIconButton(
            onClick = onBookmark,
            modifier = Modifier.size(52.dp),
            shape = CircleShape
        ) {
            Icon(
                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = stringResource(
                    if (isBookmarked) R.string.details_unbookmark else R.string.details_bookmark
                ),
                tint = if (isBookmarked) DetailAccent else DetailSecondary
            )
        }
    }
}

@Composable
fun MangaDescription(description: String) {
    var expanded by remember(description) { mutableStateOf(false) }
    val canExpand = description.length > 180

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .animateContentSize()
    ) {
        Text(
            text = description,
            color = DetailSecondary,
            fontSize = 15.sp,
            lineHeight = 22.5.sp,
            maxLines = if (expanded || !canExpand) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis
        )
        if (canExpand) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(if (expanded) "Show less" else "Read more", color = DetailAccent)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MangaTags(tags: List<String>) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            Surface(
                color = DetailSurface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(14.dp)
                )
            ) {
                Text(
                    text = tag,
                    color = DetailSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun MangaLanguageFilters(
    languages: List<String>,
    selectedLanguage: String?,
    onLanguageSelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MangaLanguageChip(
            label = stringResource(R.string.details_all_languages),
            selected = selectedLanguage == null,
            onClick = { onLanguageSelected(null) }
        )
        languages.forEach { language ->
            MangaLanguageChip(
                label = language,
                selected = selectedLanguage == language,
                onClick = { onLanguageSelected(language) }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MangaLanguageChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp, maxLines = 1) },
        shape = RoundedCornerShape(18.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = DetailAccent,
            selectedLabelColor = Color(0xFF08202A),
            containerColor = DetailSurfaceVariant,
            labelColor = DetailSecondary
        ),
        border = null
    )
}

@Composable
fun MangaChapterRow(
    number: String,
    title: String,
    metadata: String,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isCurrent) DetailAccent.copy(alpha = 0.1f) else Color.Transparent
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = number,
                color = if (isCurrent) DetailAccent else DetailMuted,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = metadata,
                    color = DetailSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isCurrent) {
                Surface(
                    shape = CircleShape,
                    color = DetailAccent.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "CONTINUE",
                        color = DetailAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.09f))
        )
    }
}

@Composable
fun MangaChaptersSheet(
    languages: List<String>,
    selectedLanguage: String?,
    uploaders: List<String>,
    selectedUploader: String?,
    chapters: List<ChapterItem>,
    currentChapterUrl: String?,
    totalChapterCount: Int,
    onLanguageSelected: (String?) -> Unit,
    onUploaderSelected: (String?) -> Unit,
    onChapterSelected: (ChapterItem) -> Unit,
    loading: Boolean,
    loadMessage: String?,
    onRetry: () -> Unit,
    onOpenSource: () -> Unit,
    hasSource: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 236.dp, max = 680.dp)
            .background(DetailSurface),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            MangaLanguageFilters(
                languages = languages,
                selectedLanguage = selectedLanguage,
                onLanguageSelected = onLanguageSelected
            )
        }
        if (uploaders.isNotEmpty()) {
            item {
                MangaUploaderFilters(
                    uploaders = uploaders,
                    selectedUploader = selectedUploader,
                    onUploaderSelected = onUploaderSelected
                )
            }
        }
        if (loading && chapters.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.details_loading_chapters),
                        color = DetailSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else if (chapters.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = loadMessage ?: stringResource(R.string.details_chapters_unavailable),
                        color = DetailSecondary,
                        fontSize = 14.sp
                    )
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.details_retry), color = DetailAccent)
                    }
                    if (hasSource) {
                        TextButton(onClick = onOpenSource) {
                            Text(stringResource(R.string.details_open_source), color = DetailAccent)
                        }
                    }
                }
            }
        } else {
            itemsIndexed(chapters, key = { index, chapter -> chapter.url.ifBlank { "$index-${chapter.title}" } }) { index, chapter ->
                val originalIndex = (totalChapterCount - index).coerceAtLeast(1)
                val numberFromTitle = Regex("""(?:chapter|ch\.?)\s*#?(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
                    .find(chapter.title)
                    ?.groupValues
                    ?.getOrNull(1)
                val number = "#${numberFromTitle ?: originalIndex}"
                val by = chapter.uploader?.takeIf(String::isNotBlank)
                    ?: stringResource(R.string.details_author_unknown)
                val date = chapter.date.orEmpty().takeIf(String::isNotBlank)
                    ?: stringResource(R.string.details_date_unknown)
                MangaChapterRow(
                    number = number,
                    title = chapter.title,
                    metadata = stringResource(R.string.details_chapter_metadata, date, by),
                    isCurrent = chapter.url == currentChapterUrl,
                    onClick = { onChapterSelected(chapter) }
                )
            }
        }
    }
}

@Composable
fun MangaUploaderFilters(
    uploaders: List<String>,
    selectedUploader: String?,
    onUploaderSelected: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.details_scanlation_group),
            color = DetailMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, top = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MangaLanguageChip(
                label = stringResource(R.string.details_all_groups),
                selected = selectedUploader == null,
                onClick = { onUploaderSelected(null) }
            )
            uploaders.forEach { uploader ->
                MangaLanguageChip(
                    label = uploader,
                    selected = selectedUploader == uploader,
                    onClick = { onUploaderSelected(uploader) }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08141C)
@Composable
private fun MangaDetailTopBarPreview() {
    MaterialTheme {
        MangaDetailTopBar({}, {}, {}, {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08141C)
@Composable
private fun MangaDetailHeaderPreview() {
    MaterialTheme {
        MangaDetailHeader(
            coverUrl = "",
            statusAndSource = "FINISHED · MANGADEX",
            title = "Non Milk-Milk Coffee Webcomic",
            alternateTitle = "Bạc xỉu không sữa",
            author = "Senukin"
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08141C)
@Composable
private fun MangaProgressCardPreview() {
    MaterialTheme {
        MangaProgressCard(13, 18, 13f / 18f, "Chapter 13")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08141C)
@Composable
private fun MangaDetailActionsPreview() {
    MaterialTheme {
        MangaDetailActions("Continue chapter 13", true, false, false, {}, {}, {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08141C)
@Composable
private fun MangaDescriptionPreview() {
    MaterialTheme {
        MangaDescription("A male office worker falls in love with the owner of a small coffee shop in a corner of the big city.")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08141C)
@Composable
private fun MangaTagsPreview() {
    MaterialTheme {
        MangaTags(listOf("Web Comic", "Romance", "Slice of Life", "Office Workers", "Comedy"))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08141C)
@Composable
private fun MangaLanguageFiltersPreview() {
    MaterialTheme {
        MangaLanguageFilters(listOf("English", "Tiếng Việt", "Español"), "Tiếng Việt", {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08141C)
@Composable
private fun MangaChapterRowPreview() {
    MaterialTheme {
        MangaChapterRow("#6", "Ngày 6", "Jul 10, 2023 · Senukin", true, {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08141C)
@Composable
private fun MangaChaptersSheetPreview() {
    MaterialTheme {
        MangaChaptersSheet(
            languages = listOf("English", "Tiếng Việt", "Español"),
            selectedLanguage = "Tiếng Việt",
            uploaders = listOf("Senukin", "Group Alpha"),
            selectedUploader = "Senukin",
            chapters = listOf(
                ChapterItem("Chapter 6", "", "Senukin", "Tiếng Việt", "Jul 10, 2023"),
                ChapterItem("Chapter 5", "", "Senukin", "Tiếng Việt", "Jul 3, 2023")
            ),
            currentChapterUrl = "",
            totalChapterCount = 18,
            onLanguageSelected = {},
            onUploaderSelected = {},
            onChapterSelected = {},
            loading = false,
            loadMessage = null,
            onRetry = {},
            onOpenSource = {},
            hasSource = false
        )
    }
}
