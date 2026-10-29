package com.hirojonn.bedduu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.ExperimentalComposeUiApi
import com.hirojonn.bedduu.ui.AppViewModel
import com.hirojonn.bedduu.ui.CustomScraperSource
import com.hirojonn.bedduu.ui.MockManga
import com.hirojonn.bedduu.ui.components.CoverArt
import com.hirojonn.bedduu.ui.theme.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SourceCatalogScreen(
    source: CustomScraperSource,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onMangaClick: (MockManga) -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val genres = viewModel.catalogMangaList
        .flatMap { it.tags }
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .take(8)
    val filteredManga = remember(viewModel.catalogMangaList, selectedGenre) {
        viewModel.catalogMangaList.filter { manga ->
            selectedGenre == null || selectedGenre in manga.tags
        }
    }
    val featuredManga = if (viewModel.catalogFilterQuery.isBlank() && selectedGenre == null) {
        viewModel.catalogMangaList.take(5)
    } else {
        emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // ── Top App Bar ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextMain)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.prettyName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                Text(
                    text = "${source.domain} • Catalog",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            IconButton(onClick = { viewModel.openCatalog(source) }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextMuted)
            }
        }

        // ── Search Input (Screenshot 2 style) ─────────────────────────────
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                if (it.isBlank()) {
                    viewModel.searchWithinCatalog("")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search manga in ${source.prettyName}...", color = TextSubtle, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryColor) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        viewModel.searchWithinCatalog("")
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = SurfaceVariant,
                focusedContainerColor = SurfaceColor,
                unfocusedContainerColor = SurfaceColor,
                cursorColor = PrimaryColor,
                focusedTextColor = TextMain,
                unfocusedTextColor = TextMain
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboard?.hide()
                viewModel.searchWithinCatalog(searchQuery)
            })
        )

        Spacer(Modifier.height(10.dp))

        // Only expose genres actually present in this source's catalog.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (listOf<String?>(null) + genres).forEach { genre ->
                FilterChip(
                    selected = selectedGenre == genre,
                    onClick = {
                        selectedGenre = genre
                        scope.launch { gridState.scrollToItem(0) }
                    },
                    label = { Text(genre ?: "All", fontSize = 12.sp) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── 3-Column Box Grid (Screenshot 2: Box form with cover) ─────────
        if (viewModel.isLoadingCatalog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryColor)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Loading ${source.prettyName} catalog...",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
            }
        } else if (filteredManga.isEmpty()) {
            BedduuEmptyState(
                icon = Icons.Default.Search,
                title = if (viewModel.catalogMangaList.isEmpty()) "Catalog unavailable" else "No titles match",
                description = if (viewModel.catalogMangaList.isEmpty()) {
                    "The source may be offline or blocking requests. Try loading the catalog again."
                } else {
                    "No manga in this catalog matches the selected genre."
                },
                actionText = if (viewModel.catalogMangaList.isEmpty()) "Retry catalog" else "Clear filter",
                onActionClick = {
                    if (viewModel.catalogMangaList.isEmpty()) {
                        viewModel.openCatalog(source)
                    } else {
                        selectedGenre = null
                    }
                }
            )
        } else {
            LaunchedEffect(
                gridState,
                viewModel.catalogMangaList.size,
                viewModel.catalogFilterQuery,
                viewModel.hasMoreCatalog
            ) {
                if (viewModel.catalogFilterQuery.isBlank() && viewModel.hasMoreCatalog) {
                    snapshotFlow {
                        gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                    }.collect { lastVisibleIndex ->
                        if (lastVisibleIndex >= viewModel.catalogMangaList.size - 6) {
                            viewModel.loadNextCatalogPage()
                        }
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp)
            ) {
                if (featuredManga.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Text(
                                "Featured",
                                color = TextMain,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 2.dp, bottom = 10.dp)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(
                                    featuredManga,
                                    key = { it.sourceUrl.ifBlank { it.title } }
                                ) { manga ->
                                    Box(modifier = Modifier.width(132.dp)) {
                                        MangaBoxItem(
                                            manga = manga,
                                            onClick = { onMangaClick(manga) }
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                        }
                    }
                }
                items(filteredManga, key = { it.sourceUrl.ifBlank { it.title } }) { manga ->
                    MangaBoxItem(
                        manga = manga,
                        onClick = { onMangaClick(manga) }
                    )
                }
                if (viewModel.isLoadingMoreCatalog) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = PrimaryColor,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MangaBoxItem(
    manga: MockManga,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // Poster / Cover Box with Badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.70f)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceColor)
        ) {
            CoverArt(
                url = manga.coverUrl,
                contentDescription = manga.title,
                modifier = Modifier.fillMaxSize()
            )

            // Subtle gradient overlay at top and bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f)
                            )
                        )
                    )
            )

            if (manga.status.isNotBlank()) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.78f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = manga.status,
                        color = Color.White,
                        fontSize = 9.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Manga Title
        Text(
            text = manga.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextMain,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
