package com.hirojonn.bedduu.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import com.hirojonn.bedduu.ui.designsystem.BedduuDisplayFontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.hirojonn.bedduu.ui.components.CoverArt
import com.hirojonn.bedduu.ui.AppViewModel
import com.hirojonn.bedduu.ui.MockManga
import com.hirojonn.bedduu.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: AppViewModel,
    onMangaClick: (MockManga) -> Unit,
    scrollToTopSignal: Int = 0
) {
    var selectedShelf by remember { mutableStateOf("Saved") }
    val gridState = rememberLazyGridState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0 && gridState.layoutInfo.totalItemsCount > 0) {
            gridState.animateScrollToItem(0)
        }
    }
    val isUpdatesShelf = selectedShelf == "Updates"
    val shelfItems = when (selectedShelf) {
        "Favorites" -> viewModel.favoriteMangaItems
        "History" -> viewModel.readingHistory
        "Downloads" -> emptyList()
        "Updates" -> emptyList()
        else -> viewModel.bookmarkedManga
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BedduuTokens.ScreenBackground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "YOUR COLLECTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryLight,
                    letterSpacing = 1.4.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "My Library",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMain,
                    fontFamily = BedduuDisplayFontFamily
                )
            }
            Surface(
                shape = CircleShape,
                color = PrimaryColor.copy(alpha = 0.16f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = PrimaryLight)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Saved", "Favorites", "History", "Downloads", "Updates").forEach { shelf ->
                FilterChip(
                    selected = selectedShelf == shelf,
                    onClick = { selectedShelf = shelf },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(shelf)
                            if (shelf == "Updates" && viewModel.chapterUpdates.isNotEmpty()) {
                                Spacer(Modifier.width(6.dp))
                                Badge { Text(viewModel.chapterUpdates.size.toString()) }
                            }
                        }
                    },
                    leadingIcon = {
                        when (shelf) {
                            "Favorites" -> Icon(Icons.Outlined.FavoriteBorder, contentDescription = null)
                            "History" -> Icon(Icons.Outlined.History, contentDescription = null)
                            "Downloads" -> Icon(Icons.Outlined.Download, contentDescription = null)
                            "Updates" -> Icon(Icons.Default.Refresh, contentDescription = null)
                            else -> Icon(Icons.Default.Bookmark, contentDescription = null)
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }

        if (isUpdatesShelf) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        viewModel.chapterUpdateCheckMessage
                            ?: "New chapters from your saved manga appear here.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        enabled = !viewModel.isCheckingChapterUpdates,
                        onClick = {
                            scope.launch {
                                viewModel.checkBookmarkedChapterUpdates(context.applicationContext)
                            }
                        }
                    ) {
                        if (viewModel.isCheckingChapterUpdates) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryLight
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Check for updates", tint = PrimaryLight)
                        }
                    }
                    if (viewModel.chapterUpdates.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearChapterUpdates) {
                            Text("Clear", color = PrimaryLight)
                        }
                    }
                }

                if (viewModel.chapterUpdates.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        BedduuEmptyState(
                            icon = Icons.Default.Refresh,
                            title = if (viewModel.isCheckingChapterUpdates) "Checking your library" else "No new chapters",
                            description = viewModel.chapterUpdateCheckMessage
                                ?: "We'll check your bookmarks when you open the app."
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 112.dp)
                    ) {
                        items(
                            items = viewModel.chapterUpdates,
                            key = { "${it.manga.sourceUrl}:${it.chapter.url}" }
                        ) { update ->
                            Card(
                                onClick = { onMangaClick(update.manga) },
                                colors = CardDefaults.cardColors(containerColor = BedduuTokens.GlassSurface),
                                shape = BedduuTokens.ShapeSmall
                            ) {
                                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                    Text(
                                        update.manga.title,
                                        color = TextMain,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(update.chapter.title, color = PrimaryLight, fontSize = 13.sp)
                                    val detail = listOfNotNull(
                                        update.chapter.uploader?.takeIf(String::isNotBlank),
                                        update.chapter.language.takeIf(String::isNotBlank),
                                        update.chapter.date.takeIf(String::isNotBlank)
                                    ).joinToString(" • ")
                                    if (detail.isNotBlank()) {
                                        Spacer(Modifier.height(3.dp))
                                        Text(detail, color = TextMuted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (shelfItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                BedduuEmptyState(
                    icon = when (selectedShelf) {
                        "Favorites" -> Icons.Outlined.FavoriteBorder
                        "History" -> Icons.Outlined.History
                        "Downloads" -> Icons.Outlined.Download
                        else -> Icons.Outlined.CollectionsBookmark
                    },
                    title = when (selectedShelf) {
                        "Favorites" -> "No favorites yet"
                        "History" -> "No reading history yet"
                        "Downloads" -> "Offline downloads aren't available yet"
                        else -> "Your library is empty"
                    },
                    description = when (selectedShelf) {
                        "Favorites" -> "Favorite a series from its detail page to keep it close."
                        "History" -> "Titles you open will appear here so you can continue reading."
                        "Downloads" -> "Downloaded chapters will be available here when offline downloads are added."
                        else -> "Browse a catalog and save a series to your personal reading shelf."
                    }
                )
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 112.dp)
            ) {
                items(shelfItems, key = { it.sourceUrl.ifBlank { it.title } }) { manga ->
                    MangaCoverItem(
                        manga = manga,
                        onClick = { onMangaClick(manga) },
                        onLongClick = {
                            when (selectedShelf) {
                                "Saved" -> viewModel.removeBookmark(manga)
                                "Favorites" -> viewModel.toggleFavorite(manga)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaCoverItem(
    manga: MockManga,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Card(
            shape = BedduuTokens.ShapeSmall,
            colors = CardDefaults.cardColors(containerColor = BedduuTokens.GlassSurface),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.70f)
                .clip(BedduuTokens.ShapeSmall)
                .border(1.dp, BedduuTokens.GlassBorder, BedduuTokens.ShapeSmall)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                CoverArt(
                    url = manga.coverUrl,
                    contentDescription = manga.title,
                    modifier = Modifier.fillMaxSize()
                )

                if (manga.sourceDomain.isNotBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(bottomStart = 8.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = manga.sourceDomain.take(12),
                            fontSize = 9.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(5.dp))

        Text(
            text = manga.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMain,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
