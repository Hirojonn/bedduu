package com.hirojonn.bedduu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
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
import coil.compose.AsyncImage
import com.hirojonn.bedduu.ui.AppViewModel
import com.hirojonn.bedduu.ui.CustomScraperSource
import com.hirojonn.bedduu.ui.MockManga
import com.hirojonn.bedduu.ui.ParserModel
import com.hirojonn.bedduu.ui.theme.*

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    viewModel: AppViewModel,
    onMangaClick: (MockManga) -> Unit,
    scrollToTopSignal: Int = 0
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var inputQuery by remember { mutableStateOf(viewModel.activeGlobalSearchQuery) }
    val searchResultsState = rememberLazyListState()
    val sourceGridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        viewModel.fetchParsers()
    }
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) {
            keyboard?.hide()
            if (viewModel.activeGlobalSearchQuery.isNotBlank()) searchResultsState.animateScrollToItem(0)
            else sourceGridState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BedduuTokens.ScreenBackground)
    ) {
        // ── Top Header / Search Bar ───────────────────────────────────────
        if (viewModel.activeGlobalSearchQuery.isNotBlank()) {
            // Search Header matching Screenshot 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    inputQuery = ""
                    viewModel.clearGlobalSearch()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextMain)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        viewModel.activeGlobalSearchQuery,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                    Text(
                        "Search results",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
                IconButton(onClick = {
                    inputQuery = ""
                    viewModel.clearGlobalSearch()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp)
            ) {
                Text(
                    "BENDDUU · MANGA READER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryLight,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "Discover manga",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMain
                )
                Text(
                    "Search across sources or browse a catalog",
                    fontSize = 13.sp,
                    color = TextMuted
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Global Search Input Field ────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search manga title (e.g. Demon Slayer)...", color = TextSubtle, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryColor) },
                trailingIcon = {
                    if (inputQuery.isNotBlank()) {
                        IconButton(onClick = {
                            inputQuery = ""
                            viewModel.clearGlobalSearch()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                shape = BedduuTokens.ShapeMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryColor,
                    unfocusedBorderColor = BedduuTokens.GlassBorder,
                    focusedContainerColor = BedduuTokens.GlassSurface,
                    unfocusedContainerColor = BedduuTokens.GlassSurface,
                    cursorColor = PrimaryColor,
                    focusedTextColor = TextMain,
                    unfocusedTextColor = TextMain
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboard?.hide()
                    viewModel.performGlobalSearch(inputQuery)
                })
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    keyboard?.hide()
                    viewModel.performGlobalSearch(inputQuery)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = BedduuTokens.ShapeMedium,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 15.dp)
            ) {
                Text("Search", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Loading Bar for Search ────────────────────────────────────────
        if (viewModel.isGlobalSearching) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryLight,
                trackColor = SurfaceVariant
            )
        }

        // ── MAIN CONTENT: Search Results (Screenshot 1) OR Source List (Screenshot 2 Entry) ──
        if (viewModel.activeGlobalSearchQuery.isNotBlank()) {
            // ══════════════════════════════════════════════════════════════
            // SCREENSHOT 1: Direct Source Fetching with Horizontal Cards
            // ══════════════════════════════════════════════════════════════
            if (!viewModel.isGlobalSearching && viewModel.globalSearchResults.isEmpty()) {
                BedduuEmptyState(
                    icon = Icons.Default.Search,
                    title = "No Results for '${viewModel.activeGlobalSearchQuery}'",
                    description = "Try searching for another manga title or browse available sources below."
                )
            } else {
                LazyColumn(
                    state = searchResultsState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    viewModel.globalSearchResults.forEach { (sourceName, mangaList) ->
                        item(key = sourceName) {
                            GlobalSourceResultSection(
                                sourceName = sourceName,
                                mangaList = mangaList,
                                onShowAll = {
                                    val matchedSource = CustomScraperSource.values().firstOrNull {
                                        sourceName.contains(it.prettyName, ignoreCase = true) || sourceName.contains(it.name, ignoreCase = true)
                                    } ?: CustomScraperSource.ATSU
                                    viewModel.openCatalog(matchedSource)
                                },
                                onMangaClick = onMangaClick
                            )
                        }
                    }

                }
            }
        } else {
            // ══════════════════════════════════════════════════════════════
            // AVAILABLE SOURCES: Clicking opens SourceCatalogScreen (Screenshot 2)!
            // ══════════════════════════════════════════════════════════════
            Text(
                "Available Sources",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            if (viewModel.isLoadingParsers) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            } else {
                LazyVerticalGrid(
                    state = sourceGridState,
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
                ) {
                    items(viewModel.parsersList) { parser ->
                        val src = parser.source ?: CustomScraperSource.ATSU
                        SourceBoxCard(
                            parser = parser,
                            onClick = {
                                // INSTEAD OF OPENING WEB, OPEN SOURCE CATALOG (SCREENSHOT 2)!
                                viewModel.openCatalog(src)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── Screenshot 1: Grouped Source Result Section with Horizontal Cards ────────
@Composable
fun GlobalSourceResultSection(
    sourceName: String,
    mangaList: List<MockManga>,
    onShowAll: () -> Unit,
    onMangaClick: (MockManga) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section Header: [Source Name]       [Show all]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sourceName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            TextButton(
                onClick = onShowAll,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Show all",
                    fontSize = 13.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Horizontal Row of Manga Cards
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(mangaList) { manga ->
                GlobalMangaCard(
                    manga = manga,
                    onClick = { onMangaClick(manga) }
                )
            }
        }
    }
}

// ─── Single Horizontal Manga Card (Screenshot 1) ──────────────────────────────
@Composable
fun GlobalMangaCard(
    manga: MockManga,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(115.dp)
            .clickable { onClick() }
    ) {
        // Poster / Cover
        Box(
            modifier = Modifier
                .width(115.dp)
                .height(165.dp)
                .clip(BedduuTokens.ShapeSmall)
                .background(SurfaceColor)
        ) {
            AsyncImage(
                model = manga.coverUrl,
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Subtle dark bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.45f)
                            )
                        )
                    )
            )
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
            lineHeight = 15.sp
        )
    }
}

// ─── Available Source Card ───────────────────────────────────────────────────
@Composable
fun SourceBoxCard(
    parser: ParserModel,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.35f)
            .clip(BedduuTokens.ShapeMedium)
            .background(BedduuTokens.GlassSurface)
            .border(1.dp, BedduuTokens.GlassBorder, BedduuTokens.ShapeMedium)
            .bedduuSpringClickable(onClick)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = parser.logoUrl,
                    contentDescription = parser.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                parser.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                textAlign = TextAlign.Center
            )
            Text(
                parser.path,
                fontSize = 11.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Browse Catalog ›",
                fontSize = 11.sp,
                color = PrimaryLight,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
