package com.hirojonn.bedduu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.hirojonn.bedduu.ui.AppViewModel
import com.hirojonn.bedduu.ui.CustomScraperSource
import com.hirojonn.bedduu.ui.ExploreViewModel
import com.hirojonn.bedduu.ui.MockManga
import com.hirojonn.bedduu.ui.components.CoverArt
import com.hirojonn.bedduu.ui.theme.BedduuTokens
import com.hirojonn.bedduu.ui.designsystem.BedduuDisplayFontFamily
import com.hirojonn.bedduu.ui.theme.PrimaryLight
import com.hirojonn.bedduu.ui.theme.SurfaceColor
import com.hirojonn.bedduu.ui.theme.SurfaceVariant
import com.hirojonn.bedduu.ui.theme.TextMain
import com.hirojonn.bedduu.ui.theme.TextMuted
import com.hirojonn.bedduu.ui.theme.TextSubtle
import kotlinx.coroutines.flow.collect
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.ui.draw.shadow

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalMaterialApi::class
)
@Composable
fun ExploreScreen(
    feedViewModel: ExploreViewModel,
    appViewModel: AppViewModel,
    scrollToTopSignal: Int,
    onMangaClick: (MockManga) -> Unit,
    onSourceClick: (CustomScraperSource) -> Unit
) {
    val feed = feedViewModel.state
    val gridState = rememberLazyGridState()
    val keyboard = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf(appViewModel.activeGlobalSearchQuery) }
    var selectedGenre by remember { mutableStateOf("All") }
    var filtersExpanded by remember { mutableStateOf(false) }

    val genres = remember(feed.items) {
        feed.items.flatMap { it.tags }
            .filter(String::isNotBlank)
            .distinctBy(String::lowercase)
            .take(12)
    }
    val visibleManga = remember(feed.items, query, selectedGenre, appViewModel.globalSearchResults) {
        val searchResults = appViewModel.globalSearchResults.values.flatten()
        val base = if (appViewModel.activeGlobalSearchQuery.isNotBlank()) searchResults else feed.items
        base.filter { manga ->
            (query.isBlank() || manga.title.contains(query, ignoreCase = true) ||
                manga.alternateTitle.contains(query, ignoreCase = true)) &&
                (selectedGenre == "All" || manga.tags.any { it.equals(selectedGenre, ignoreCase = true) })
        }.distinctBy { it.sourceUrl.ifBlank { "${it.sourceDomain}:${it.title}" } }
    }

    LaunchedEffect(Unit) {
        if (feed.items.isEmpty() && !feed.isLoading) feedViewModel.load()
    }
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) gridState.animateScrollToItem(0)
    }
    LaunchedEffect(gridState, feed.items.size, feed.hasMore, query, selectedGenre) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastVisible ->
                if (appViewModel.activeGlobalSearchQuery.isBlank() &&
                    feed.hasMore && lastVisible >= visibleManga.size - 4
                ) feedViewModel.loadMore()
            }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = feed.isRefreshing,
        onRefresh = { feedViewModel.load(refresh = true) }
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(BedduuTokens.ScreenBackground)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                "BEDDUU  /  BROWSE",
                color = PrimaryLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Find your next read",
                color = TextMain,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = BedduuDisplayFontFamily
            )
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .shadow(8.dp, CircleShape),
                shape = CircleShape,
                color = SurfaceColor,
                border = BorderStroke(1.dp, SurfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search manga",
                        tint = PrimaryLight,
                        modifier = Modifier.size(22.dp)
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            if (appViewModel.activeGlobalSearchQuery.isNotBlank()) {
                                appViewModel.clearGlobalSearch()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextMain,
                            fontSize = 14.sp
                        ),
                        cursorBrush = Brush.verticalGradient(listOf(PrimaryLight, PrimaryLight)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboard?.hide()
                            appViewModel.performGlobalSearch(query)
                        }),
                        decorationBox = { innerTextField ->
                            Box {
                                if (query.isEmpty()) {
                                    Text(
                                        "Search manga and creators",
                                        color = TextSubtle,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (query.isNotBlank()) {
                        IconButton(
                            onClick = {
                                query = ""
                                appViewModel.clearGlobalSearch()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = TextMuted)
                        }
                    }
                    IconButton(
                        onClick = { filtersExpanded = !filtersExpanded },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = if (filtersExpanded) "Hide filters" else "Show filters",
                            tint = if (filtersExpanded) PrimaryLight else TextMuted
                        )
                    }
                }
            }
        }

        if (filtersExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (listOf("All") + genres).forEach { genre ->
                        FilterChip(
                            selected = selectedGenre == genre,
                            onClick = { selectedGenre = genre },
                            label = { Text(genre) }
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomScraperSource.values().forEach { source ->
                        FilterChip(
                            selected = false,
                            onClick = { onSourceClick(source) },
                            label = {
                                Text(source.prettyName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        )
                    }
                }
            }
        }

        if (appViewModel.isGlobalSearching && appViewModel.activeGlobalSearchQuery.isNotBlank()) {
            LinearSearchProgress()
        }

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .pullRefresh(pullRefreshState)
        ) {
            when {
                feed.isLoading && feed.items.isEmpty() -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 108.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(9) { ExploreCoverPlaceholder() }
                }
                visibleManga.isEmpty() -> Column(
                    Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (feed.error != null) "Explore could not load titles."
                        else "No titles match this search or genre yet.",
                        color = TextMuted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (feed.error != null) feed.error.orEmpty() else "Try another title or clear your filters.",
                        color = TextSubtle,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    if (feed.error != null) {
                        TextButton(onClick = { feedViewModel.load(refresh = true) }) {
                            Text("Retry", color = PrimaryLight)
                        }
                    }
                }
                else -> LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 108.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        visibleManga,
                        key = { it.sourceUrl.ifBlank { "${it.sourceDomain}:${it.title}" } }
                    ) { manga ->
                        ExploreCover(manga, onClick = { onMangaClick(manga) })
                    }
                    if (feed.isLoadingMore && appViewModel.activeGlobalSearchQuery.isBlank()) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Box(
                                Modifier.fillMaxWidth().padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = PrimaryLight)
                            }
                        }
                    } else if (feed.error != null && feed.items.isNotEmpty() &&
                        appViewModel.activeGlobalSearchQuery.isBlank()
                    ) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            TextButton(
                                onClick = { feedViewModel.load(refresh = true) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Couldn't load more titles · Retry", color = PrimaryLight)
                            }
                        }
                    }
                }
            }
            PullRefreshIndicator(
                refreshing = feed.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = SurfaceColor,
                contentColor = PrimaryLight
            )
        }
    }
}

@Composable
private fun LinearSearchProgress() {
    androidx.compose.material3.LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth(),
        color = PrimaryLight,
        trackColor = SurfaceVariant
    )
}

@Composable
private fun ExploreCover(manga: MockManga, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        CoverArt(
            url = manga.coverUrl,
            contentDescription = manga.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .shadow(6.dp, BedduuTokens.ShapeMedium)
                .clip(BedduuTokens.ShapeMedium)
        )
        Text(
            manga.title,
            color = TextMain,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            manga.sourceDomain,
            color = TextMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExploreCoverPlaceholder() {
    val transition = rememberInfiniteTransition(label = "explore-cover-placeholder")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Restart
        ),
        label = "explore-placeholder-offset"
    )
    val shimmer = Brush.linearGradient(
        colors = listOf(
            SurfaceVariant,
            SurfaceVariant.copy(alpha = 0.55f),
            SurfaceVariant
        ),
        start = androidx.compose.ui.geometry.Offset(offset * 420f, 0f),
        end = androidx.compose.ui.geometry.Offset((offset + 1f) * 420f, 420f)
    )

    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(BedduuTokens.ShapeMedium)
                .background(shimmer)
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth(0.78f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(shimmer)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth(0.48f)
                .height(9.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(shimmer)
        )
    }
}
