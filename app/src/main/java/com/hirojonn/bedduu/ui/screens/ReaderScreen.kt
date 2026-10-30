package com.hirojonn.bedduu.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import android.view.WindowManager
import android.webkit.*
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.hirojonn.bedduu.ui.components.CoverArt
import coil.request.ImageRequest
import com.hirojonn.bedduu.ui.AppViewModel
import com.hirojonn.bedduu.ui.ChapterItem
import com.hirojonn.bedduu.ui.MangaScraperRepository
import com.hirojonn.bedduu.ui.MockManga
import com.hirojonn.bedduu.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import org.json.JSONArray
import java.util.concurrent.TimeUnit

private data class ReaderPanel(
    val imageUrl: String,
    val chapter: ChapterItem
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    manga: MockManga,
    chapter: ChapterItem,
    viewModel: AppViewModel,
    onClose: () -> Unit,
    onNextChapter: (() -> Unit)? = null,
    onPrevChapter: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    var showControls by remember { mutableStateOf(true) }
    var showBrightnessDialog by remember { mutableStateOf(false) }
    val initialScreenBrightness = remember {
        activity?.window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }
    var readerBrightness by remember {
        mutableStateOf(initialScreenBrightness.takeIf { it >= 0f } ?: 0.5f)
    }
    var extractedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var continuousPanels by remember(manga.sourceUrl) { mutableStateOf<List<ReaderPanel>>(emptyList()) }
    var attemptedContinuousChapters by remember(manga.sourceUrl) { mutableStateOf<Set<String>>(emptySet()) }
    var isLoadingNextChapter by remember { mutableStateOf(false) }
    var visibleChapter by remember(chapter.url) { mutableStateOf(chapter) }
    var visiblePage by remember(chapter.url) {
        mutableIntStateOf(viewModel.lastReadPage(manga, chapter) ?: 1)
    }
    var readerContentAvailable by remember(chapter.url) { mutableStateOf(false) }
    var webPageFailed by remember(chapter.url) { mutableStateOf(false) }
    var recordedChapterUrls by remember(manga.sourceUrl) { mutableStateOf<Set<String>>(emptySet()) }
    var restoredReadingPositions by remember(manga.sourceUrl) { mutableStateOf<Set<String>>(emptySet()) }
    val readerListState = rememberLazyListState()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { extractedImages.size })
    var useNativeViewer by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var webError by remember { mutableStateOf<String?>(null) }
    var challengeNotice by remember { mutableStateOf(false) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    // ── Track REAL Reading Time ──────────────────────────────────────────────
    val readerStartTime = remember { System.currentTimeMillis() }
    DisposableEffect(Unit) {
        onDispose {
            val elapsedMinutes = ((System.currentTimeMillis() - readerStartTime) / 60000).toInt()
            if (elapsedMinutes > 0) {
                viewModel.recordRealReadingTime(elapsedMinutes)
            }
        }
    }
    DisposableEffect(activity) {
        onDispose {
            activity?.window?.let { window ->
                window.attributes = window.attributes.apply {
                    screenBrightness = initialScreenBrightness
                }
            }
        }
    }
    DisposableEffect(activity) {
        val systemBarsController = activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            systemBarsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3_500)
            showControls = false
        }
    }

    val lastContinuousChapter = continuousPanels.lastOrNull()?.chapter ?: chapter
    val nextContinuousChapter = findNextContinuousChapter(manga.chapters, lastContinuousChapter)
    LaunchedEffect(
        readerListState,
        continuousPanels.size,
        readerContentAvailable,
        useNativeViewer,
        viewModel.readingMode
    ) {
        snapshotFlow {
            readerListState.layoutInfo.visibleItemsInfo
                .firstOrNull()
                ?.index
                ?.let { index ->
                    continuousPanels.getOrNull(index)?.let { panel ->
                        val page = continuousPanels
                            .take(index + 1)
                            .count { it.chapter.url == panel.chapter.url }
                            .coerceAtLeast(1)
                        panel.chapter to page
                    }
                }
        }.collectLatest { position ->
            if (position != null) {
                val (currentChapter, page) = position
                visibleChapter = currentChapter
                visiblePage = page
                if (
                    readerContentAvailable &&
                    useNativeViewer &&
                    viewModel.readingMode == "webtoon"
                ) {
                    delay(350)
                    if (visibleChapter.url == currentChapter.url && visiblePage == page) {
                        viewModel.recordReadPage(manga, currentChapter, page)
                    }
                }
            }
        }
    }
    LaunchedEffect(
        chapter.url,
        continuousPanels.size,
        readerContentAvailable,
        useNativeViewer,
        viewModel.readingMode
    ) {
        if (
            !readerContentAvailable ||
            !useNativeViewer ||
            viewModel.readingMode != "webtoon" ||
            chapter.url in restoredReadingPositions ||
            continuousPanels.isEmpty()
        ) {
            return@LaunchedEffect
        }
        val chapterStartIndex = continuousPanels.indexOfFirst { it.chapter.url == chapter.url }
        if (chapterStartIndex >= 0) {
            val savedPanelIndex = (viewModel.lastReadPage(manga, chapter) ?: 1) - 1
            readerListState.scrollToItem(
                (chapterStartIndex + savedPanelIndex).coerceIn(0, continuousPanels.lastIndex)
            )
            restoredReadingPositions = restoredReadingPositions + chapter.url
        }
    }
    LaunchedEffect(chapter.url, extractedImages.size, useNativeViewer) {
        if (useNativeViewer && extractedImages.isNotEmpty() && viewModel.readingMode != "webtoon") {
            val savedPage = viewModel.lastReadPage(manga, chapter)
            val targetPage = (savedPage?.minus(1) ?: 0).coerceIn(0, extractedImages.lastIndex)
            pagerState.scrollToPage(targetPage)
        }
    }
    LaunchedEffect(pagerState, chapter.url, readerContentAvailable, useNativeViewer, viewModel.readingMode) {
        if (!readerContentAvailable || !useNativeViewer || viewModel.readingMode == "webtoon") {
            return@LaunchedEffect
        }
        snapshotFlow { pagerState.currentPage + 1 }.collectLatest { page ->
            visiblePage = page
            delay(350)
            if (visiblePage == page) viewModel.recordReadPage(manga, chapter, page)
        }
    }
    LaunchedEffect(visibleChapter, readerContentAvailable) {
        val chapterToRecord = visibleChapter
        if (
            readerContentAvailable &&
            chapterToRecord.url.isNotBlank() &&
            chapterToRecord.url !in recordedChapterUrls
        ) {
            recordedChapterUrls = recordedChapterUrls + chapterToRecord.url
            viewModel.readChapter(manga, chapterToRecord)
        }
    }

    LaunchedEffect(
        readerListState,
        continuousPanels.size,
        lastContinuousChapter.url,
        nextContinuousChapter?.url,
        useNativeViewer
    ) {
        val nextChapter = nextContinuousChapter ?: return@LaunchedEffect
        if (!useNativeViewer || nextChapter.url in attemptedContinuousChapters) {
            return@LaunchedEffect
        }

        snapshotFlow {
            val layout = readerListState.layoutInfo
            layout.totalItemsCount > 0 &&
                (layout.visibleItemsInfo.lastOrNull()?.index ?: -1) >= layout.totalItemsCount - 3
        }.first { it }

        if (nextChapter.url in attemptedContinuousChapters) return@LaunchedEffect
        attemptedContinuousChapters = attemptedContinuousChapters + nextChapter.url
        isLoadingNextChapter = true
        try {
            val nextImages = MangaScraperRepository.fetchChapterImages(nextChapter.url)
            if (nextImages.isNotEmpty()) {
                continuousPanels = continuousPanels +
                    nextImages.map { ReaderPanel(it, nextChapter) }
            } else {
                Log.w(
                    "ReaderScreen",
                    "No panel URLs available for continuous chapter ${nextChapter.url}"
                )
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            Log.e(
                "ReaderScreen",
                "Could not load next chapter for continuous reading: ${nextChapter.url}",
                error
            )
        } finally {
            isLoadingNextChapter = false
        }
    }

    // Keep screen on while reading if enabled in settings
    DisposableEffect(viewModel.keepScreenOn) {
        if (viewModel.keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val readerBg = if (viewModel.pureBlackOled) Color.Black else Color(0xFF0F1017)

    // Determine target URL
    val targetUrl = remember(chapter, manga) {
        when {
            chapter.url.isNotBlank() -> chapter.url
            manga.sourceUrl.isNotBlank() -> manga.sourceUrl
            else -> ""
        }
    }
    val isComixChapter = remember(targetUrl) {
        targetUrl.contains("comix.to", ignoreCase = true)
    }

    // ── Custom Coil ImageLoader with Browser Headers (Fixes 403 / Black Panels) ─
    val customImageLoader = remember(targetUrl) {
        coil.ImageLoader.Builder(context)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val original = chain.request()
                        val host = original.url.host
                        val referer = when {
                            host.contains("mangadex") -> "https://mangadex.org/"
                            host.contains("weebcentral") -> "https://weebcentral.com/"
                            host.contains("projectsuki") -> "https://projectsuki.com/"
                            targetUrl.isNotBlank() -> targetUrl
                            else -> original.url.newBuilder().encodedPath("/").build().toString()
                        }
                        val request = original.newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36")
                            .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                        if (targetUrl.contains("comix.to", ignoreCase = true)) {
                            request.removeHeader("Referer").removeHeader("Origin")
                        } else {
                            request.header("Referer", referer)
                        }
                        val imageRequest = request.build()
                        chain.proceed(imageRequest)
                    }
                    .connectTimeout(25, TimeUnit.SECONDS)
                    .readTimeout(25, TimeUnit.SECONDS)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    LaunchedEffect(extractedImages, pagerState.currentPage, customImageLoader) {
        extractedImages
            .drop(pagerState.currentPage + 1)
            .take(2)
            .forEach { imageUrl ->
                customImageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(imageUrl)
                        .build()
                )
            }
    }

    // ── Direct Panel Extraction ──────────────────────────────────────────────
    LaunchedEffect(chapter, manga) {
        pagerState.scrollToPage(0)
        isLoading = true
        webError = null
        challengeNotice = false
        scale = 1f
        offset = androidx.compose.ui.geometry.Offset.Zero
        extractedImages = emptyList()
        continuousPanels = emptyList()
        attemptedContinuousChapters = setOf(chapter.url)
        isLoadingNextChapter = false
        visibleChapter = chapter
        readerContentAvailable = false
        webPageFailed = false

        if (targetUrl.isNotBlank()) {
            val directPanels = try {
                MangaScraperRepository.fetchChapterImages(targetUrl)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }

            if (directPanels.isNotEmpty()) {
                extractedImages = directPanels
                continuousPanels = directPanels.map { ReaderPanel(it, chapter) }
                useNativeViewer = true
                isLoading = false
                readerContentAvailable = true
            } else {
                // If direct extraction returned no images, fallback to web reader
                useNativeViewer = false
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val readerLoadTimeoutHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val readerLoadTimeout = remember { mutableStateOf<Runnable?>(null) }

    fun clearReaderLoadTimeout() {
        readerLoadTimeout.value?.let(readerLoadTimeoutHandler::removeCallbacks)
        readerLoadTimeout.value = null
    }

    fun startReaderLoadTimeout() {
        clearReaderLoadTimeout()
        val timeout = Runnable {
            isLoading = false
            webError = "The reader is taking too long to load. Check your connection and retry."
        }
        readerLoadTimeout.value = timeout
        readerLoadTimeoutHandler.postDelayed(timeout, 30_000)
    }

    DisposableEffect(targetUrl) {
        onDispose { clearReaderLoadTimeout() }
    }

    val bridge = remember {
        object {
            @JavascriptInterface
            fun toggleControls() {
                mainHandler.post { showControls = !showControls }
            }

            @JavascriptInterface
            fun onImagesFound(json: String) {
                try {
                    val arr = JSONArray(json)
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        val s = arr.getString(i)
                        if (s.isNotBlank() && !s.contains("picsum") && !s.contains("broken_image")) {
                            list.add(s)
                        }
                    }
                    mainHandler.post {
                        clearReaderLoadTimeout()
                        if (list.isNotEmpty()) {
                            extractedImages = list
                            continuousPanels = list.map { ReaderPanel(it, chapter) }
                            useNativeViewer = true
                            readerContentAvailable = true
                        } else if (isComixChapter) {
                            useNativeViewer = false
                        }
                        isLoading = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (isComixChapter) {
                        mainHandler.post {
                            clearReaderLoadTimeout()
                            useNativeViewer = false
                            isLoading = false
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readerBg)
    ) {
        if (targetUrl.isBlank() && extractedImages.isEmpty()) {
            // Clean Empty State (NO EMOJIS)
            BedduuEmptyState(
                icon = Icons.Outlined.MenuBook,
                title = "No Chapter Available",
                description = "Unable to locate pages for this chapter. Please return to the manga details screen.",
                actionText = "Back to Details",
                onActionClick = onClose,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (useNativeViewer && extractedImages.isNotEmpty()) {
            // ═════════════════════════════════════════════════════════════════
            // APP MODE: NATIVE FAST MANGA VIEWER (ZERO BLACK SCREEN)
            // ═════════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { tap ->
                                if (viewModel.readingMode != "webtoon" && extractedImages.isNotEmpty()) {
                                    when {
                                        tap.x < size.width * 0.28f -> {
                                            val page = if (viewModel.readLeftToRight) {
                                                pagerState.currentPage - 1
                                            } else {
                                                pagerState.currentPage + 1
                                            }
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(
                                                    page.coerceIn(0, extractedImages.lastIndex)
                                                )
                                            }
                                        }
                                        tap.x > size.width * 0.72f -> {
                                            val page = if (viewModel.readLeftToRight) {
                                                pagerState.currentPage + 1
                                            } else {
                                                pagerState.currentPage - 1
                                            }
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(
                                                    page.coerceIn(0, extractedImages.lastIndex)
                                                )
                                            }
                                        }
                                        else -> showControls = !showControls
                                    }
                                } else {
                                    showControls = !showControls
                                }
                            },
                            onDoubleTap = {
                                scale = if (scale > 1f) 1f else 2f
                                offset = androidx.compose.ui.geometry.Offset.Zero
                            }
                        )
                    }
            ) {
                val imageModifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 3.5f)
                            offset = if (scale > 1f) {
                                offset + pan
                            } else {
                                androidx.compose.ui.geometry.Offset.Zero
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )

                if (viewModel.readingMode == "webtoon") {
                    LazyColumn(
                        state = readerListState,
                        modifier = imageModifier
                    ) {
                        itemsIndexed(
                            items = continuousPanels,
                            key = { index, panel -> "${panel.chapter.url}-$index-${panel.imageUrl}" }
                        ) { index, panel ->
                            val previousPanel = continuousPanels.getOrNull(index - 1)
                            if (previousPanel?.chapter?.url != panel.chapter.url) {
                                Text(
                                    text = panel.chapter.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(readerBg)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    color = PrimaryLight,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 300.dp)
                                    .background(Color(0xFF11121A)),
                                contentAlignment = Alignment.Center
                            ) {
                                CoverArt(
                                    url = panel.imageUrl,
                                    contentDescription = "Panel ${index + 1} · ${panel.chapter.title}",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(),
                                    imageLoader = customImageLoader
                                )
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    if (isLoadingNextChapter) {
                                        "Loading next chapter…"
                                    } else if (nextContinuousChapter != null) {
                                        "Keep scrolling for ${nextContinuousChapter.title}"
                                    } else {
                                        "End of ${continuousPanels.lastOrNull()?.chapter?.title ?: chapter.title}"
                                    },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (isLoadingNextChapter) {
                                    Spacer(Modifier.height(12.dp))
                                    CircularProgressIndicator(
                                        color = PrimaryLight,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else if (nextContinuousChapter == null && onNextChapter != null) {
                                    Spacer(Modifier.height(16.dp))
                                    Button(
                                        onClick = onNextChapter,
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Next Chapter ›", fontWeight = FontWeight.Bold)
                                    }
                                } else if (nextContinuousChapter == null) {
                                    Spacer(Modifier.height(16.dp))
                                    OutlinedButton(onClick = onClose) {
                                        Text("Back to Details", color = PrimaryLight)
                                    }
                                }
                                Spacer(Modifier.height(60.dp))
                            }
                        }
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        reverseLayout = !viewModel.readLeftToRight,
                        modifier = imageModifier
                    ) { page ->
                        CoverArt(
                            url = extractedImages[page],
                            contentDescription = "Page ${page + 1} of ${extractedImages.size}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(readerBg),
                            imageLoader = customImageLoader
                        )
                    }
                }
            }
        } else {
            // ═════════════════════════════════════════════════════════════════
            // WEB MODE: INTERACTIVE READER
            // ═════════════════════════════════════════════════════════════════
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = if (isComixChapter && isLoading) 0f else 1f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.setSupportMultipleWindows(false)
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        settings.userAgentString = WebSettings.getDefaultUserAgent(ctx)
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        addJavascriptInterface(bridge, "BedduuReader")
                        setBackgroundColor(android.graphics.Color.parseColor("#12131A"))

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                if (newProgress >= 80) {
                                    isLoading = false
                                }
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                webError = null
                                challengeNotice = false
                                webPageFailed = false
                                startReaderLoadTimeout()
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val reqUrl: String = request?.url?.toString() ?: return false
                                return if (reqUrl.startsWith("http://", ignoreCase = true) || reqUrl.startsWith("https://", ignoreCase = true)) {
                                    false
                                } else {
                                    true
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    clearReaderLoadTimeout()
                                    isLoading = false
                                    webPageFailed = true
                                    webError = error?.description?.toString()
                                        ?: "The chapter page could not be loaded."
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: WebResourceResponse?
                            ) {
                                if (request?.isForMainFrame == true &&
                                    (errorResponse?.statusCode ?: 0) >= 400
                                ) {
                                    clearReaderLoadTimeout()
                                    isLoading = false
                                    webPageFailed = true
                                    if (errorResponse?.statusCode == 403) {
                                        webError = null
                                        challengeNotice = true
                                    } else {
                                        webError = "The source returned HTTP ${errorResponse?.statusCode}."
                                    }
                                }
                            }

                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                                clearReaderLoadTimeout()
                                handler?.cancel()
                                isLoading = false
                                webPageFailed = true
                                webError = "The source has an invalid security certificate. The connection was stopped."
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                clearReaderLoadTimeout()
                                webError = null
                                isLoading = false
                                if (isComixChapter) {
                                    isLoading = true
                                    startReaderLoadTimeout()
                                } else if (!webPageFailed && !challengeNotice) {
                                    readerContentAvailable = true
                                }
                                // Gentle styling and image extraction
                                val cleanScript = """
                                    (function() {
                                        var style = document.createElement('style');
                                        style.innerHTML = `
                                            header, nav, footer, .sidebar, .ads, .ad, [id*="ad"], [class*="ad"],
                                            .disqus, #disqus_thread, .floating-btn, .notification-bar {
                                                display: none !important;
                                            }
                                            body {
                                                background-color: #12131A !important;
                                                color: #FFFFFF !important;
                                            }
                                        `;
                                        document.head.appendChild(style);

                                        var isAtsu = location.hostname.endsWith('atsu.moe');
                                        var isComix = location.hostname.endsWith('comix.to');
                                        if (isComix) {
                                            style.innerHTML += `
                                                .rpage-body > *:not(#app-root), #app-root > *:not(.rpage),
                                                .rpage > *:not(.rpage-main),
                                                .rpage-main__inner > *:not(.rpage-page) {
                                                    display: none !important;
                                                }
                                            `;
                                        }
                                        function extractImages() {
                                            if (window.__bedduuReaderImagesSent) return;
                                            var imgs = isAtsu
                                                ? Array.from(document.querySelectorAll('#reader-scroll-inner img[src*="/static/pages/"]'))
                                                : isComix
                                                    ? Array.from(document.querySelectorAll('.rpage-page__img'))
                                                : Array.from(document.querySelectorAll('.reading-content img, .page-break img, img[data-src*="chapter"], img[src*="chapter"], .wp-manga-chapter-img, .entry-content img'));
                                            if (isComix) {
                                                imgs.forEach(function(image) {
                                                    image.loading = 'eager';
                                                    var source = image.getAttribute('data-src') ||
                                                        image.getAttribute('data-lazy-src') ||
                                                        image.getAttribute('data-original');
                                                    if (source && !image.getAttribute('src')) {
                                                        image.setAttribute('src', source);
                                                    }
                                                });
                                            }
                                            var urls = imgs.map(function(i) {
                                                var source = i.getAttribute('data-src') ||
                                                    i.getAttribute('data-lazy-src') ||
                                                    i.getAttribute('data-original') ||
                                                    i.src;
                                                try {
                                                    return source ? new URL(source, document.baseURI).href : '';
                                                } catch(e) {
                                                    return '';
                                                }
                                            }).filter(function(s) {
                                                return s && s.indexOf('http') === 0 && !s.includes('logo') && !s.includes('banner');
                                            });

                                            if ((isAtsu ? urls.length > 0 : (isComix ? urls.length > 0 : urls.length > 2)) && window.BedduuReader) {
                                                window.__bedduuReaderImagesSent = true;
                                                if (isAtsu) {
                                                    var reader = document.querySelector('#reader-scroll-inner');
                                                    var chapterContent = reader && reader.parentElement &&
                                                        reader.parentElement.parentElement &&
                                                        reader.parentElement.parentElement.parentElement;
                                                    if (chapterContent) {
                                                        Array.from(chapterContent.children).forEach(function(child, index) {
                                                            if (index !== 0) child.style.display = 'none';
                                                        });
                                                    }
                                                }
                                                window.BedduuReader.onImagesFound(JSON.stringify(urls));
                                                return true;
                                            }
                                            return false;
                                        }

                                        if (isComix) {
                                            var comixStableTicks = 0;
                                            var comixLastCount = 0;
                                            var comixPoll = window.setInterval(function() {
                                                var pages = Array.from(document.querySelectorAll('.rpage-page'));
                                                var pageCount = pages.length;
                                                var urlCount = Array.from(document.querySelectorAll('.rpage-page__img'))
                                                    .filter(function(image) {
                                                        return image.getAttribute('data-src') ||
                                                            image.getAttribute('data-lazy-src') ||
                                                            image.getAttribute('data-original') ||
                                                            image.src;
                                                    }).length;
                                                var count = pageCount + ':' + urlCount;
                                                comixStableTicks = count === comixLastCount ? comixStableTicks + 1 : 0;
                                                comixLastCount = count;
                                                if (pages.length > 0 && urlCount >= pages.length && comixStableTicks >= 2) {
                                                    window.clearInterval(comixPoll);
                                                    if (!extractImages() && window.BedduuReader) {
                                                        window.BedduuReader.onImagesFound('[]');
                                                    }
                                                } else if (comixStableTicks >= 15 && window.BedduuReader) {
                                                    window.clearInterval(comixPoll);
                                                    if (!extractImages()) window.BedduuReader.onImagesFound('[]');
                                                }
                                            }, 250);
                                        } else if (isAtsu) {
                                            var attempts = 0;
                                            var imagePoll = window.setInterval(function() {
                                                attempts++;
                                                if (extractImages() || attempts >= 80) {
                                                    window.clearInterval(imagePoll);
                                                }
                                            }, 250);
                                        } else {
                                            extractImages();
                                        }
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(cleanScript, null)
                            }
                        }
                        webViewInstance = this
                        tag = targetUrl
                        loadUrl(targetUrl)
                    }
                },
                update = { view ->
                    webViewInstance = view
                    if (view.tag != targetUrl) {
                        view.tag = targetUrl
                        view.loadUrl(targetUrl)
                    }
                }
            )
        }

        // ─── Subtle Top Linear Loading Indicator (NO FULL BLACK OVERLAY) ──────
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .height(3.dp),
                color = PrimaryLight,
                trackColor = Color.Transparent
            )
        }

        webError?.takeIf { !useNativeViewer }?.let { message ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 52.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceColor
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        message,
                        modifier = Modifier.weight(1f),
                        color = TextMain,
                        fontSize = 12.sp
                    )
                    TextButton(onClick = {
                        webError = null
                        isLoading = true
                        webViewInstance?.reload()
                    }) {
                        Text("Retry", color = PrimaryLight)
                    }
                }
            }
        }

        if (challengeNotice && !useNativeViewer) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 52.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceColor.copy(alpha = 0.94f)
            ) {
                Text(
                    "This source may require verification. Complete any check shown on its page.",
                    modifier = Modifier.padding(12.dp),
                    color = TextMain,
                    fontSize = 12.sp
                )
            }
        }

        if (useNativeViewer && extractedImages.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                color = Color(0xB31B1B2A),
                shape = CircleShape
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onPrevChapter?.invoke() },
                        enabled = onPrevChapter != null,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.NavigateBefore,
                            contentDescription = "Previous chapter",
                            tint = if (onPrevChapter != null) Color.White else Color.Gray
                        )
                    }
                    TextButton(
                        onClick = {
                            viewModel.readingMode = if (viewModel.readingMode == "webtoon") {
                                if (viewModel.readLeftToRight) "ltr" else "rtl"
                            } else {
                                "webtoon"
                            }
                            viewModel.savePreferences()
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            if (viewModel.readingMode == "webtoon") "Paged" else "Vertical",
                            color = Color.White
                        )
                    }
                    if (viewModel.showPageNumber && viewModel.readingMode == "webtoon" &&
                        continuousPanels.isNotEmpty()
                    ) {
                        val chapterPanelCount = continuousPanels.count {
                            it.chapter.url == visibleChapter.url
                        }
                        Text(
                            text = "$visiblePage / $chapterPanelCount",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else if (viewModel.showPageNumber && viewModel.readingMode != "webtoon") {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${extractedImages.size}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = { showBrightnessDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Brightness6,
                            contentDescription = "Adjust reader brightness",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { onNextChapter?.invoke() },
                        enabled = onNextChapter != null,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.NavigateNext,
                            contentDescription = "Next chapter",
                            tint = if (onNextChapter != null) Color.White else Color.Gray
                        )
                    }
                }
            }
        }

        if (showBrightnessDialog) {
            AlertDialog(
                onDismissRequest = { showBrightnessDialog = false },
                title = { Text("Reader brightness") },
                text = {
                    Column {
                        Text("${(readerBrightness * 100).toInt()}%")
                        Slider(
                            value = readerBrightness,
                            onValueChange = { brightness ->
                                readerBrightness = brightness
                                activity?.window?.let { window ->
                                    window.attributes = window.attributes.apply {
                                        screenBrightness = brightness
                                    }
                                }
                            },
                            valueRange = 0.05f..1f,
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBrightnessDialog = false }) {
                        Text("Done")
                    }
                }
            )
        }

        // Keep a compact island visible at the top while the reader controls collapse.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .displayCutoutPadding()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            Surface(
                color = Color(0xA61B1B2A),
                shape = CircleShape,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth(if (showControls) 0.94f else 0.76f)
                    .animateContentSize(
                        spring(dampingRatio = 0.78f, stiffness = 420f)
                    )
            ) {
                if (showControls) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                manga.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                visibleChapter.title,
                                fontSize = 12.sp,
                                color = PrimaryLight,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { onPrevChapter?.invoke() },
                            enabled = onPrevChapter != null,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.NavigateBefore,
                                contentDescription = "Previous chapter",
                                tint = if (onPrevChapter != null) Color.White else Color.DarkGray
                            )
                        }
                        IconButton(
                            onClick = { onNextChapter?.invoke() },
                            enabled = onNextChapter != null,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.NavigateNext,
                                contentDescription = "Next chapter",
                                tint = if (onNextChapter != null) Color.White else Color.DarkGray
                            )
                        }

                        // ── Mode Switcher Chip: [App | Web] ──────────────────
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xA6222538))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (useNativeViewer) PrimaryColor.copy(alpha = 0.82f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        if (extractedImages.isNotEmpty()) {
                                            useNativeViewer = true
                                        } else {
                                            Toast.makeText(context, "Extracting panels...", Toast.LENGTH_SHORT).show()
                                            coroutineScope.launch {
                                                isLoading = true
                                                val panels = MangaScraperRepository.fetchChapterImages(targetUrl)
                                                if (panels.isNotEmpty()) {
                                                    extractedImages = panels
                                                    continuousPanels = panels.map { ReaderPanel(it, chapter) }
                                                    attemptedContinuousChapters = setOf(chapter.url)
                                                    useNativeViewer = true
                                                } else {
                                                    Toast.makeText(context, "App mode not directly available for this source. Using Web mode.", Toast.LENGTH_SHORT).show()
                                                }
                                                isLoading = false
                                            }
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "App",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (useNativeViewer) Color.White else Color.Gray
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (!useNativeViewer) PrimaryColor.copy(alpha = 0.82f)
                                        else Color.Transparent
                                    )
                                    .clickable { useNativeViewer = false }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "Web",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!useNativeViewer) Color.White else Color.Gray
                                )
                            }
                        }

                        Spacer(Modifier.width(4.dp))

                        IconButton(onClick = {
                            if (useNativeViewer) {
                                coroutineScope.launch {
                                    isLoading = true
                                    val panels = MangaScraperRepository.fetchChapterImages(targetUrl)
                                    if (panels.isNotEmpty()) {
                                        extractedImages = panels
                                        continuousPanels = panels.map { ReaderPanel(it, chapter) }
                                        attemptedContinuousChapters = setOf(chapter.url)
                                    }
                                    isLoading = false
                                }
                            } else {
                                webViewInstance?.reload()
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.White)
                        }

                        val isBookmarked = viewModel.bookmarkedManga.any { it.title.equals(manga.title, ignoreCase = true) }
                        IconButton(onClick = {
                            if (isBookmarked) {
                                viewModel.removeBookmark(manga)
                                Toast.makeText(context, "Removed from Library", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.bookmarkManga(
                                    title = manga.title,
                                    url = manga.sourceUrl,
                                    coverUrl = manga.coverUrl,
                                    author = manga.author,
                                    chapters = manga.chapters
                                )
                                Toast.makeText(context, "Saved to Library!", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) PrimaryLight else Color.White
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .clickable { showControls = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showControls = true }
                        ) {
                            Text(
                                manga.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                chapter.title,
                                fontSize = 11.sp,
                                color = PrimaryLight,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { showControls = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Show reader controls",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

    }
}

private fun findNextContinuousChapter(
    chapters: List<ChapterItem>,
    currentChapter: ChapterItem
): ChapterItem? {
    val feed = chapters.filter {
        it.uploader == currentChapter.uploader &&
            it.language == currentChapter.language
    }
    if (feed.isEmpty()) return null

    val currentNumber = readerChapterNumber(currentChapter)
    if (currentNumber != null) {
        val nextNumberedChapter = feed.mapNotNull { candidate ->
            readerChapterNumber(candidate)?.let { number -> number to candidate }
        }.filter { (number, candidate) ->
            number > currentNumber && candidate.url != currentChapter.url
        }.minByOrNull { (number, _) -> number }
        if (nextNumberedChapter != null) return nextNumberedChapter.second
    }

    val currentIndex = feed.indexOfFirst { it.url == currentChapter.url }
    if (currentIndex < 0) return feed.firstOrNull { it.url != currentChapter.url }

    val numberedFeed = feed.mapNotNull(::readerChapterNumber)
    val newestFirst = numberedFeed.size > 1 && numberedFeed.first() > numberedFeed.last()
    val nextIndex = if (newestFirst) currentIndex - 1 else currentIndex + 1
    return feed.getOrNull(nextIndex)
}

private fun readerChapterNumber(chapter: ChapterItem): java.math.BigDecimal? =
    Regex(
        """(?:chapter|ch\.?|episode|ep\.?)\s*#?(\d+(?:\.\d+)?)""",
        RegexOption.IGNORE_CASE
    )
        .find(chapter.title)
        ?.groupValues
        ?.getOrNull(1)
        ?.toBigDecimalOrNull()
