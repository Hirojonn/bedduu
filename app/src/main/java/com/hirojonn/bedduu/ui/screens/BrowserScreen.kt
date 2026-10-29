package com.hirojonn.bedduu.ui.screens

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.*
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.hirojonn.bedduu.ui.AppViewModel
import com.hirojonn.bedduu.ui.ChapterItem
import com.hirojonn.bedduu.ui.theme.PrimaryColor
import com.hirojonn.bedduu.ui.theme.PrimaryLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    url: String,
    viewModel: AppViewModel,
    onClose: () -> Unit,
    onToonGodVerified: () -> Unit = {}
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf(url) }
    var currentUrl by remember { mutableStateOf(url) }
    var isLoading by remember { mutableStateOf(true) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var challengeNotice by remember { mutableStateOf(false) }
    var toonGodRefreshTriggered by remember(url) { mutableStateOf(false) }

    val isBookmarked = viewModel.bookmarkedManga.any {
        it.title.equals(pageTitle.replace(Regex("\\s*[-–|].*$"), "").trim(), ignoreCase = true) ||
        (currentUrl.isNotBlank() && it.sourceUrl == currentUrl)
    }

    fun handleBookmark() {
        val wv = webViewInstance ?: return
        val activeUrl = wv.url ?: currentUrl
        val rawTitle = (wv.title ?: pageTitle).ifBlank { "Manga" }
        val cleanTitle = rawTitle.replace(Regex("\\s*[-–|].*$"), "").trim().ifBlank { "Manga" }

        val alreadySaved = viewModel.bookmarkedManga.firstOrNull {
            it.title.equals(cleanTitle, ignoreCase = true) || (activeUrl.isNotBlank() && it.sourceUrl == activeUrl)
        }

        if (alreadySaved != null) {
            viewModel.removeBookmark(alreadySaved)
            Toast.makeText(context, "Removed '$cleanTitle' from Library", Toast.LENGTH_SHORT).show()
        } else {
            // Immediately save to library on main UI thread — 100% reliable
            viewModel.bookmarkManga(
                title = cleanTitle,
                url = activeUrl,
                coverUrl = "",
                author = "Web Source",
                chapters = emptyList()
            )
            Toast.makeText(context, "Saved '$cleanTitle' to Library!", Toast.LENGTH_SHORT).show()

            // Try to asynchronously enrich cover artwork if page has an og:image or poster
            try {
                val extractJs = """
                    (function() {
                        try {
                            var img = document.querySelector('meta[property="og:image"]')?.content 
                                || document.querySelector('.summary_image img, .manga-poster img, .poster img, .thumb img, .tab-summary img')?.getAttribute('data-src')
                                || document.querySelector('.summary_image img, .manga-poster img, .poster img, .thumb img, .tab-summary img')?.src 
                                || '';
                            return img;
                        } catch(e) { return ''; }
                    })()
                """.trimIndent()
                wv.evaluateJavascript(extractJs) { coverResult ->
                    val foundCover = coverResult?.trim('"', '\'', ' ') ?: ""
                    if (foundCover.isNotBlank() && foundCover != "null" && !foundCover.contains("picsum")) {
                        viewModel.bookmarkManga(
                            title = cleanTitle,
                            url = activeUrl,
                            coverUrl = foundCover
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore extraction errors — manga is already saved
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = pageTitle,
                    maxLines = 1,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    val webView = webViewInstance
                    if (webView?.canGoBack() == true) {
                        webView.goBack()
                    } else {
                        onClose()
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = { webViewInstance?.reload() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.White)
                }
                IconButton(onClick = { handleBookmark() }) {
                    Icon(
                        if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) PrimaryLight else Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryColor)
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                trackColor = PrimaryColor
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.setSupportMultipleWindows(false)
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            pageError = null
                            challengeNotice = false
                            url?.let { currentUrl = it }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            pageTitle = view?.title ?: url ?: ""
                            url?.let { currentUrl = it }
                            if (!toonGodRefreshTriggered &&
                                url?.let { android.net.Uri.parse(it).host?.endsWith("toongod.org", true) } == true
                            ) {
                                view?.evaluateJavascript(
                                    """
                                        (function() {
                                            var text = (document.body && document.body.innerText || '').slice(0, 5000);
                                            var challenge = /captcha|verify you are human|checking your browser|cloudflare|just a moment/i.test(text);
                                            var hasTitles = document.querySelectorAll('a[href*="/webtoons/"]').length > 0;
                                            return !challenge && hasTitles;
                                        })();
                                    """.trimIndent()
                                ) { result ->
                                    if (result == "true" && !toonGodRefreshTriggered) {
                                        toonGodRefreshTriggered = true
                                        onToonGodVerified()
                                    }
                                }
                            }
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val reqUrl: String = request?.url?.toString() ?: return false
                            // Allow HTTP/HTTPS only; block ad schemes (intent://, market://, etc.)
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
                                isLoading = false
                                pageError = error?.description?.toString()
                                    ?: "The page could not be loaded."
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
                                isLoading = false
                                if (errorResponse?.statusCode == 403) {
                                    pageError = null
                                    challengeNotice = true
                                } else {
                                    pageError = "The source returned HTTP ${errorResponse?.statusCode}."
                                }
                            }
                        }

                        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                            handler?.cancel()
                            isLoading = false
                            pageError = "The source has an invalid security certificate. The connection was stopped."
                        }
                    }
                    webViewInstance = this
                    loadUrl(url)
                }
                },
                update = { view ->
                    webViewInstance = view
                    if (view.url != url) view.loadUrl(url)
                }
            )

            pageError?.let { message ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(24.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Page unavailable", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(message, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            pageError = null
                            isLoading = true
                            webViewInstance?.reload()
                        }) {
                            Text("Retry")
                        }
                    }

                    if (challengeNotice) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(12.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                        ) {
                            Text(
                                "This source may require verification. Complete any check shown on its page.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
