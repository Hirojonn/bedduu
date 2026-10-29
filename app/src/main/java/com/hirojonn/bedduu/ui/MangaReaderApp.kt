package com.hirojonn.bedduu.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import com.hirojonn.bedduu.ui.screens.*
import com.hirojonn.bedduu.ui.theme.*

private enum class AppScreen {
    ONBOARDING,
    READER,
    BROWSER,
    MANGA_DETAILS,
    CATALOG,
    MAIN
}

private data class AppDestination(
    val screen: AppScreen,
    val manga: MockManga? = null,
    val chapter: ChapterItem? = null,
    val url: String? = null,
    val source: CustomScraperSource? = null
)

private val bedduuNavigationItems = listOf(
    NavItem("Explore", Icons.Filled.Explore),
    NavItem("Library", Icons.Filled.Bookmark),
    NavItem("Settings", Icons.Filled.Settings),
    NavItem("Profile", Icons.Filled.Person)
)

@Composable
fun MangaReaderApp(viewModel: AppViewModel) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val tabHistory = remember { mutableStateListOf<Int>() }
    var tabScrollSignal by remember { mutableIntStateOf(0) }
    val tabStateHolder = rememberSaveableStateHolder()
    val exploreViewModel: ExploreViewModel = composeViewModel()
    var readingManga by remember { mutableStateOf<MockManga?>(null) }
    var readingChapter by remember { mutableStateOf<ChapterItem?>(null) }
    var selectedManga by remember { mutableStateOf<MockManga?>(null) }
    var isBackNavigation by remember { mutableStateOf(false) }

    val hasPreviousDestination = readingManga != null ||
        selectedManga != null ||
        viewModel.selectedCatalogSource != null ||
        viewModel.currentBrowserUrl != null ||
        tabHistory.isNotEmpty() ||
        selectedTab != 0

    fun returnToPreviousDestination() {
        isBackNavigation = true
        when {
            readingManga != null -> {
                readingManga = null
                readingChapter = null
            }
            viewModel.currentBrowserUrl != null -> viewModel.currentBrowserUrl = null
            selectedManga != null -> selectedManga = null
            viewModel.selectedCatalogSource != null -> viewModel.closeCatalog()
            tabHistory.isNotEmpty() -> selectedTab = tabHistory.removeAt(tabHistory.lastIndex)
            selectedTab != 0 -> selectedTab = 0
        }
    }

    BackHandler(enabled = hasPreviousDestination && !viewModel.isFirstLaunch) {
        returnToPreviousDestination()
    }

    fun selectTab(tab: Int) {
        if (tab !in bedduuNavigationItems.indices) return
        if (tab == selectedTab) {
            tabScrollSignal++
            return
        }
        isBackNavigation = false
        tabHistory.add(selectedTab)
        selectedTab = tab
    }

    val destination = when {
        viewModel.isFirstLaunch -> AppDestination(AppScreen.ONBOARDING)
        readingManga != null -> AppDestination(
            screen = AppScreen.READER,
            manga = readingManga?.normalizedForAnimation(),
            chapter = readingChapter?.normalizedForAnimation()
        )
        viewModel.currentBrowserUrl != null -> AppDestination(
            screen = AppScreen.BROWSER,
            url = viewModel.currentBrowserUrl
        )
        selectedManga != null -> AppDestination(
            screen = AppScreen.MANGA_DETAILS,
            manga = selectedManga?.normalizedForAnimation()
        )
        viewModel.selectedCatalogSource != null -> AppDestination(
            screen = AppScreen.CATALOG,
            source = viewModel.selectedCatalogSource
        )
        else -> AppDestination(AppScreen.MAIN)
    }

    CompositionLocalProvider(
        LocalIsDarkMode provides viewModel.isDarkMode,
        LocalPureBlack provides viewModel.pureBlackOled
    ) {
        AnimatedContent(
            targetState = destination,
            transitionSpec = {
                if (isBackNavigation) {
                    (fadeIn(tween(140)) + slideInHorizontally(
                        animationSpec = tween(180)
                    ) { -it / 5 }) togetherWith
                        (fadeOut(tween(110)) + slideOutHorizontally(
                            animationSpec = tween(160)
                        ) { it / 8 })
                } else {
                    (fadeIn(tween(140)) + slideInHorizontally(
                        animationSpec = tween(180)
                    ) { it / 5 }) togetherWith
                        (fadeOut(tween(110)) + slideOutHorizontally(
                            animationSpec = tween(160)
                        ) { -it / 8 })
                }
            },
            label = "app-destination-transition"
        ) { target ->
            when (target.screen) {
                AppScreen.ONBOARDING -> OnboardingScreen(
                    viewModel = viewModel,
                    onFinished = { /* viewModel.isFirstLaunch set to false */ }
                )

                AppScreen.READER -> {
                    val manga = target.manga ?: return@AnimatedContent
                    val chapter = target.chapter ?: ChapterItem("Chapter 1", manga.sourceUrl)
                    val chapters = manga.chapters
                    val sameChapterFeed = chapters.filter {
                        it.uploader == chapter.uploader && it.language == chapter.language
                    }
                    val currentNumber = chapterNumber(chapter)
                    val numberedFeed = sameChapterFeed.mapNotNull { feedChapter ->
                        chapterNumber(feedChapter)?.let { it to feedChapter }
                    }
                    val nextChapter = if (currentNumber != null) {
                        numberedFeed
                            .filter { (number, _) -> number > currentNumber }
                            .minByOrNull { (number, _) -> number }
                            ?.second
                    } else {
                        adjacentBySourceOrder(sameChapterFeed, chapter, forward = true)
                    }
                    val previousChapter = if (currentNumber != null) {
                        numberedFeed
                            .filter { (number, _) -> number < currentNumber }
                            .maxByOrNull { (number, _) -> number }
                            ?.second
                    } else {
                        adjacentBySourceOrder(sameChapterFeed, chapter, forward = false)
                    }

                    ReaderScreen(
                        manga = manga,
                        chapter = chapter,
                        viewModel = viewModel,
                        onClose = ::returnToPreviousDestination,
                        onNextChapter = nextChapter?.let { next ->
                            {
                                isBackNavigation = false
                                readingChapter = next
                                viewModel.readChapter(manga, next)
                            }
                        },
                        onPrevChapter = previousChapter?.let { previous ->
                            {
                                isBackNavigation = true
                                readingChapter = previous
                                viewModel.readChapter(manga, previous)
                            }
                        }
                    )
                }

                AppScreen.BROWSER -> {
                    val url = target.url ?: return@AnimatedContent
                    BrowserScreen(
                        url = url,
                        viewModel = viewModel,
                        onClose = ::returnToPreviousDestination,
                        onToonGodVerified = {
                            viewModel.refreshCurrentCatalog()
                            if (selectedManga?.sourceDomain?.endsWith("toongod.org", true) == true) {
                                selectedManga = selectedManga?.copy(chapters = emptyList())
                            }
                        }
                    )
                }

                AppScreen.MANGA_DETAILS -> {
                    val manga = target.manga ?: return@AnimatedContent
                    MangaDetailsScreen(
                        rawManga = manga,
                        viewModel = viewModel,
                        onBack = ::returnToPreviousDestination,
                        onReadChapter = { reading, chapter ->
                            isBackNavigation = false
                            readingManga = reading
                            readingChapter = chapter
                        }
                    )
                }

                AppScreen.CATALOG -> {
                    val source = target.source ?: return@AnimatedContent
                    SourceCatalogScreen(
                        source = source,
                        viewModel = viewModel,
                        onBack = ::returnToPreviousDestination,
                        onMangaClick = { manga ->
                            isBackNavigation = false
                            selectedManga = manga
                        }
                    )
                }

                AppScreen.MAIN -> MainTabs(
                    viewModel = viewModel,
                    feedViewModel = exploreViewModel,
                    selectedTab = selectedTab,
                    scrollToTopSignal = tabScrollSignal,
                    isBackNavigation = isBackNavigation,
                    onSelectTab = ::selectTab,
                    stateHolder = tabStateHolder,
                    onMangaClick = { manga ->
                        isBackNavigation = false
                        selectedManga = manga
                    }
                )
            }
        }
    }
}

@Composable
private fun MainTabs(
    viewModel: AppViewModel,
    feedViewModel: ExploreViewModel,
    selectedTab: Int,
    scrollToTopSignal: Int,
    isBackNavigation: Boolean,
    onSelectTab: (Int) -> Unit,
    stateHolder: androidx.compose.runtime.saveable.SaveableStateHolder,
    onMangaClick: (MockManga) -> Unit
) {
    val swipeThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        AnimatedContent(
            modifier = Modifier
                .fillMaxSize()
                .displayCutoutPadding()
                .statusBarsPadding()
                .navigationBarsPadding()
                .pointerInput(selectedTab, swipeThreshold) {
                    var horizontalDrag = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount -> horizontalDrag += dragAmount },
                        onDragEnd = {
                            when {
                                horizontalDrag <= -swipeThreshold ->
                                    onSelectTab((selectedTab + 1).coerceAtMost(bedduuNavigationItems.lastIndex))
                                horizontalDrag >= swipeThreshold ->
                                    onSelectTab((selectedTab - 1).coerceAtLeast(0))
                            }
                            horizontalDrag = 0f
                        },
                        onDragCancel = { horizontalDrag = 0f }
                    )
                },
            targetState = selectedTab,
            transitionSpec = {
                (fadeIn(tween(140)) + slideInHorizontally(
                    tween(180)
                ) { if (isBackNavigation) -it / 5 else it / 5 }) togetherWith
                    (fadeOut(tween(110)) + slideOutHorizontally(
                        tween(160)
                    ) { if (isBackNavigation) it / 8 else -it / 8 })
            },
            label = "main-tab-transition"
        ) { tab ->
            stateHolder.SaveableStateProvider(key = "tab-$tab") {
                when (tab) {
                    0 -> ExploreScreen(
                        feedViewModel = feedViewModel,
                        appViewModel = viewModel,
                        scrollToTopSignal = scrollToTopSignal,
                        onMangaClick = onMangaClick,
                        onSourceClick = { source -> viewModel.openCatalog(source) }
                    )
                    1 -> LibraryScreen(viewModel, onMangaClick, scrollToTopSignal)
                    2 -> SettingsScreen(viewModel, scrollToTopSignal)
                    3 -> ProfileScreen(viewModel, scrollToTopSignal)
                }
            }
        }

        FloatingNavBar(
            items = bedduuNavigationItems,
            selectedIndex = selectedTab,
            onSelect = onSelectTab,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        )
    }
}

private fun chapterNumber(chapter: ChapterItem): java.math.BigDecimal? =
    Regex("""(?:chapter|ch\.?)\s*#?(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        .find(chapter.title)
        ?.groupValues
        ?.getOrNull(1)
        ?.toBigDecimalOrNull()

private fun MockManga.normalizedForAnimation(): MockManga = MockManga(
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

private fun ChapterItem.normalizedForAnimation(): ChapterItem = ChapterItem(
    title = title.orEmpty(),
    url = url.orEmpty(),
    uploader = uploader,
    language = language.orEmpty(),
    date = date.orEmpty()
)

private fun adjacentBySourceOrder(
    chapters: List<ChapterItem>,
    currentChapter: ChapterItem,
    forward: Boolean
): ChapterItem? {
    val currentIndex = chapters.indexOfFirst { it.url == currentChapter.url }
    if (currentIndex < 0) return null

    val numbered = chapters.mapNotNull(::chapterNumber)
    val sourceIsNewestFirst = numbered.firstOrNull() != null &&
        numbered.lastOrNull() != null &&
        numbered.first() > numbered.last()
    val step = when {
        forward && sourceIsNewestFirst -> -1
        forward -> 1
        sourceIsNewestFirst -> 1
        else -> -1
    }
    return chapters.getOrNull(currentIndex + step)
}
