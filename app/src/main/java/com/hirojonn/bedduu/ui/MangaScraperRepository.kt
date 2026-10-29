package com.hirojonn.bedduu.ui

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.CookieManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import com.hirojonn.bedduu.data.remote.parser.ChapterImageParser
import com.hirojonn.bedduu.data.remote.parser.ComixCatalogParser
import com.hirojonn.bedduu.data.remote.parser.MadaraParser
import com.hirojonn.bedduu.data.remote.parser.ParserException
import com.hirojonn.bedduu.data.remote.parser.ToonGodParser
import com.hirojonn.bedduu.data.remote.parser.WeebCentralParser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object MangaScraperRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Bendduu/1.0"

    // ─── 1. Atsu.moe Search ────────────────────────────────────────────────
    suspend fun searchAtsu(query: String): List<MockManga> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://atsu.moe/collections/manga/documents/search?q=$q&query_by=title&per_page=15"
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext emptyList()
            parseAtsuDocuments(body)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ─── 2. Atsu.moe Catalog ───────────────────────────────────────────────
    suspend fun fetchAtsuCatalog(perPage: Int = 30, page: Int = 1): List<MockManga> = withContext(Dispatchers.IO) {
        val url = "https://atsu.moe/collections/manga/documents/search?q=*&query_by=title&per_page=$perPage&page=$page"
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext emptyList()
            parseAtsuDocuments(body)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseAtsuDocuments(jsonStr: String): List<MockManga> {
        val list = mutableListOf<MockManga>()
        try {
            val json = JSONObject(jsonStr)
            val hits = json.optJSONArray("hits") ?: return emptyList()
            for (i in 0 until hits.length()) {
                val doc = hits.getJSONObject(i).optJSONObject("document") ?: continue
                val id = doc.optString("id", "")
                val title = doc.optString("title", "Unknown")
                val poster = doc.optString("posterMedium", doc.optString("poster", ""))
                val cover = if (poster.isNotBlank()) {
                    if (poster.startsWith("http")) poster else "https://atsu.moe$poster"
                } else ""

                val authorsArr = doc.optJSONArray("authors")
                val author = if (authorsArr != null && authorsArr.length() > 0) authorsArr.getString(0) else "Unknown Author"
                val weebCentralId = doc.optString("weebCentralId", "")
                val sourceUrl = if (weebCentralId.isNotBlank()) {
                    "https://weebcentral.com/series/$weebCentralId"
                } else {
                    "https://atsu.moe/manga/$id"
                }

                list.add(
                    MockManga(
                        title = title,
                        author = author,
                        coverUrl = cover,
                        sourceUrl = sourceUrl,
                        sourceDomain = "atsu.moe"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // ─── 3. MangaDex Search ────────────────────────────────────────────────
    suspend fun searchMangaDex(query: String): List<MockManga> {
        if (query.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
                val q = URLEncoder.encode(query.trim(), "UTF-8")
                val url = "https://api.mangadex.org/manga?title=$q&limit=15&includes[]=cover_art"
                try {
                    val req = Request.Builder()
                        .url(url)
                        .header("User-Agent", USER_AGENT)
                        .build()
                    val resp = client.newCall(req).execute()
                    val body = resp.body?.string() ?: return@withContext emptyList()
                    parseMangaDexData(body)
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
        }
    }

    // ─── 4. MangaDex Catalog (Popular) ────────────────────────────────────
    suspend fun fetchMangaDexCatalog(perPage: Int = 30, offset: Int = 0): List<MockManga> = withContext(Dispatchers.IO) {
        val url = "https://api.mangadex.org/manga?limit=$perPage&offset=$offset&order[followedCount]=desc&includes[]=cover_art"
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext emptyList()
            parseMangaDexData(body)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }

    }

    suspend fun searchWeebCentral(query: String): List<MockManga> {
        if (query.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
                val url =
                    "https://weebcentral.com/search/data?search=${URLEncoder.encode(query.trim(), "UTF-8")}"
                fetchHtml(url)?.let { parseWeebCentralManga(it, url) }.orEmpty()
            }
    }

    suspend fun searchProjectSuki(query: String): List<MockManga> {
        if (query.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
                val url = "https://projectsuki.com/search?q=${URLEncoder.encode(query.trim(), "UTF-8")}"
                fetchHtml(url)?.let { parseProjectSukiManga(it, url) }.orEmpty()
            }
    }

    suspend fun searchToonGod(query: String, context: Context? = null): List<MockManga> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        fetchToonGodTitles(toonGodSearchUrls(query.trim()), context)
    }

    private fun parseMangaDexData(jsonStr: String): List<MockManga> {
        val list = mutableListOf<MockManga>()
        try {
            val json = JSONObject(jsonStr)
            val data = json.optJSONArray("data") ?: return emptyList()
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val id = item.optString("id", "")
                val attrs = item.optJSONObject("attributes") ?: continue
                val titleObj = attrs.optJSONObject("title")
                val title = titleObj?.let { titles ->
                    sequenceOf(
                        titles.optString("en"),
                        titles.optString("ja-ro"),
                        titles.keys().asSequence().map(titles::optString).firstOrNull(String::isNotBlank).orEmpty()
                    ).firstOrNull(String::isNotBlank)
                } ?: "Manga"

                // Find cover art filename
                var coverFileName = ""
                val rels = item.optJSONArray("relationships")
                if (rels != null) {
                    for (r in 0 until rels.length()) {
                        val rel = rels.getJSONObject(r)
                        if (rel.optString("type") == "cover_art") {
                            coverFileName = rel.optJSONObject("attributes")?.optString("fileName", "") ?: ""
                            break
                        }
                    }
                }

                val cover = if (coverFileName.isNotBlank()) {
                    "https://uploads.mangadex.org/covers/$id/$coverFileName.256.jpg"
                } else ""

                list.add(
                    MockManga(
                        title = title,
                        author = "MangaDex",
                        coverUrl = cover,
                        sourceUrl = "https://mangadex.org/title/$id",
                        sourceDomain = "mangadex.org"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // ─── 5. Multi-Source Global Search ────────────────────────────────────
    suspend fun searchAllSources(
        query: String,
        context: Context
    ): Map<String, List<MockManga>> = coroutineScope {
        if (query.isBlank()) return@coroutineScope emptyMap()

        val atsuDeferred = async { searchAtsu(query) }
        val mangaDexDeferred = async { searchMangaDex(query) }
        val weebCentralDeferred = async { searchWeebCentral(query) }
        val projectSukiDeferred = async { searchProjectSuki(query) }
        val comixDeferred = async { searchComix(context, query) }
        val toonGodDeferred = async { searchToonGod(query, context) }

        val results = mutableMapOf<String, List<MockManga>>()
        val sourceResults = listOf(
            "Atsu.moe" to atsuDeferred,
            "MangaDex" to mangaDexDeferred,
            "WeebCentral" to weebCentralDeferred,
            "Project Suki" to projectSukiDeferred,
            "Comix.to" to comixDeferred,
            "ToonGod" to toonGodDeferred
        )
        sourceResults.forEach { (sourceName, deferred) ->
            try {
                deferred.await().takeIf(List<MockManga>::isNotEmpty)?.let { results[sourceName] = it }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                android.util.Log.w("MangaSearch", "$sourceName search failed", error)
            }
        }

        results
    }

    suspend fun searchComix(context: Context, query: String): List<MockManga> =
        fetchComixCatalog(context, page = 1, searchQuery = query)

    // ─── 6. Catalog for Selected Source ───────────────────────────────────
    const val CATALOG_PAGE_SIZE = 30

    suspend fun fetchCatalog(source: CustomScraperSource, context: Context? = null): List<MockManga> {
        return when (source) {
            CustomScraperSource.ATSU -> fetchAtsuCatalog(CATALOG_PAGE_SIZE, page = 1)
            CustomScraperSource.MANGADEX -> fetchMangaDexCatalog(CATALOG_PAGE_SIZE, offset = 0)
            CustomScraperSource.WEEBCENTRAL -> fetchWeebCentralCatalog()
            CustomScraperSource.PROJECT_SUKI -> fetchProjectSukiCatalog(page = 1)
            CustomScraperSource.COMIX -> context?.let { fetchComixCatalog(it, page = 1) } ?: fetchComixCatalog()
            CustomScraperSource.TOONGOD -> fetchToonGodCatalog(page = 1, context)
        }
    }

    /** Load the next page of results to append to the catalog. */
    suspend fun fetchCatalogPage(source: CustomScraperSource, page: Int, context: Context? = null): List<MockManga> {
        return when (source) {
            CustomScraperSource.ATSU -> fetchAtsuCatalog(CATALOG_PAGE_SIZE, page = page)
            CustomScraperSource.MANGADEX -> fetchMangaDexCatalog(CATALOG_PAGE_SIZE, offset = (page - 1) * CATALOG_PAGE_SIZE)
            CustomScraperSource.WEEBCENTRAL -> fetchWeebCentralCatalogPage(page)
            CustomScraperSource.PROJECT_SUKI -> fetchProjectSukiCatalog(page)
            CustomScraperSource.COMIX -> context?.let { fetchComixCatalog(it, page) }.orEmpty()
            CustomScraperSource.TOONGOD -> fetchToonGodCatalog(page, context)
        }
    }

    private suspend fun fetchWeebCentralCatalog(offset: Int = 0): List<MockManga> = withContext(Dispatchers.IO) {
        val url = "https://weebcentral.com/search/data?limit=32&offset=$offset"
        fetchHtml(url)?.let { parseWeebCentralManga(it, url) }.orEmpty()
    }

    private suspend fun fetchWeebCentralCatalogPage(page: Int): List<MockManga> =
        fetchWeebCentralCatalog(offset = (page - 1) * 32)

    private suspend fun fetchProjectSukiCatalog(page: Int): List<MockManga> = withContext(Dispatchers.IO) {
        val browseUrl = "https://projectsuki.com/browse/${(page - 1).coerceAtLeast(0)}"
        fetchHtml(browseUrl)?.let { parseProjectSukiManga(it, browseUrl) }.orEmpty()
    }

    private suspend fun fetchToonGodCatalog(page: Int, context: Context? = null): List<MockManga> =
        fetchToonGodTitles(toonGodArchiveUrls(page), context)

    private suspend fun fetchToonGodTitles(
        urls: List<String>,
        context: Context?
    ): List<MockManga> {
        val cookie = toonGodCookie()
        urls.forEach { url ->
            val html = fetchHtml(url, cookie, referer = "https://www.toongod.org/") ?: return@forEach
            try {
                return ToonGodParser.parseTitles(html, url).map(::toMockManga)
            } catch (error: ParserException) {
                android.util.Log.w("MangaScraperRepository", error.message ?: "Could not parse ToonGod catalog")
            }
        }
        if (context != null) {
            val url = urls.firstOrNull() ?: return emptyList()
            return try {
                ToonGodParser.parseTitles(fetchToonGodHtmlInWebView(context, url), url).map(::toMockManga)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                android.util.Log.w("MangaScraperRepository", "ToonGod browser fallback failed", error)
                emptyList()
            }
        }
        return emptyList()
    }

    private suspend fun fetchToonGodHtmlInWebView(context: Context, url: String): String =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val handler = Handler(Looper.getMainLooper())
                lateinit var webView: WebView
                var completed = false

                fun finish(result: Result<String>) {
                    if (completed) return
                    completed = true
                    handler.removeCallbacksAndMessages(null)
                    webView.removeJavascriptInterface("BedduuToonGod")
                    webView.stopLoading()
                    webView.destroy()
                    if (!continuation.isActive) return
                    result.fold(continuation::resume, continuation::resumeWithException)
                }

                val bridge = object {
                    @JavascriptInterface
                    fun onHtml(html: String) {
                        handler.post { finish(Result.success(html)) }
                    }

                    @JavascriptInterface
                    fun onError(message: String) {
                        handler.post {
                            finish(Result.failure(IllegalStateException(message)))
                        }
                    }
                }

                webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = WebSettings.getDefaultUserAgent(context)
                    addJavascriptInterface(bridge, "BedduuToonGod")
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                            view?.evaluateJavascript(
                                """
                                    (() => {
                                        if (window.__bedduuToonGodScrapeStarted) return;
                                        window.__bedduuToonGodScrapeStarted = true;
                                        let attempts = 0;
                                        const poll = () => {
                                            const body = document.body?.innerText || '';
                                            const title = document.title || '';
                                            const challenged = /just a moment|checking your browser|verify you are human/i
                                                .test(title + ' ' + body.slice(0, 1200));
                                            const hasSeries = document.querySelector(
                                                '.page-item-detail, .c-tabs-item__content, .item-summary, a[href*="/webtoon/"]'
                                            );
                                            if (!challenged && (hasSeries || attempts >= 5)) {
                                                window.BedduuToonGod.onHtml(
                                                    document.documentElement.outerHTML
                                                );
                                                return;
                                            }
                                            if (attempts++ >= 50) {
                                                window.BedduuToonGod.onError(
                                                    'Timed out waiting for ToonGod to finish loading'
                                                );
                                                return;
                                            }
                                            setTimeout(poll, 400);
                                        };
                                        poll();
                                    })();
                                """.trimIndent(),
                                null
                            )
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                finish(
                                    Result.failure(
                                        IllegalStateException(error?.description?.toString() ?: "ToonGod page failed to load")
                                    )
                                )
                            }
                        }
                    }
                    loadUrl(url)
                }

                handler.postDelayed(
                    { finish(Result.failure(IllegalStateException("Timed out loading ToonGod catalog"))) },
                    30_000
                )
                continuation.invokeOnCancellation {
                    handler.post {
                        if (!completed) {
                            completed = true
                            handler.removeCallbacksAndMessages(null)
                            webView.removeJavascriptInterface("BedduuToonGod")
                            webView.stopLoading()
                            webView.destroy()
                        }
                    }
                }
            }
        }

    private suspend fun toonGodCookie(): String? =
        withContext(Dispatchers.Main.immediate) {
            CookieManager.getInstance().getCookie("https://www.toongod.org")
        }

    private suspend fun pageRequestCookie(url: String): String? {
        if (url.toHttpUrlOrNull()?.host?.endsWith("toongod.org", ignoreCase = true) != true) {
            return null
        }
        return withContext(Dispatchers.Main.immediate) {
            CookieManager.getInstance().getCookie(url)
        }
    }

    private fun toonGodArchiveUrls(page: Int): List<String> {
        val safePage = page.coerceAtLeast(1)
        val paths = if (safePage == 1) {
            listOf("/webtoon/", "/webtoons/")
        } else {
            listOf("/home/page/$safePage/", "/webtoon/page/$safePage/", "/webtoons/page/$safePage/")
        }
        return paths.map { "https://www.toongod.org$it" }
    }

    private fun toonGodSearchUrls(query: String): List<String> =
        listOf("/webtoon-genre/manhwa/", "/", "/webtoon/").map { path ->
            "https://www.toongod.org$path".toHttpUrlOrNull()!!
                .newBuilder()
                .addQueryParameter("s", query)
                .addQueryParameter("post_type", "wp-manga")
                .build()
                .toString()
        }

    private fun toMockManga(manga: com.hirojonn.bedduu.data.model.MangaDto) = MockManga(
        title = manga.title,
        author = if (manga.sourceId == "comix") "Comix" else "ToonGod",
        coverUrl = manga.coverUrl,
        sourceUrl = manga.detailUrl,
        sourceDomain = if (manga.sourceId == "comix") "comix.to" else "www.toongod.org"
    )

    private fun parseWeebCentralManga(html: String, pageUrl: String): List<MockManga> {
        return try {
            WeebCentralParser.parseTitles(html, pageUrl).map { manga ->
                MockManga(
                    title = manga.title,
                    author = "WeebCentral",
                    coverUrl = manga.coverUrl,
                    sourceUrl = manga.detailUrl,
                    sourceDomain = "weebcentral.com"
                )
            }
        } catch (error: com.hirojonn.bedduu.data.remote.parser.ParserException) {
            android.util.Log.w("MangaScraperRepository", error.message ?: "Could not parse WeebCentral catalog")
            emptyList()
        }
    }

    private fun parseProjectSukiManga(html: String, pageUrl: String): List<MockManga> {
        val document = Jsoup.parse(html, pageUrl)
        return document.select("a[href^='/book/']").mapNotNull { link ->
            val url = link.absUrl("href").substringBefore('#').trimEnd('/')
            val image = link.selectFirst("img")
            val title = sequenceOf(
                link.attr("aria-label").trim(),
                link.text().trim(),
                image?.attr("alt")?.trim().orEmpty()
            ).firstOrNull(String::isNotBlank).orEmpty()
            if (url.isBlank() || title.isBlank()) return@mapNotNull null
            val cover = image?.let(::imageSource).orEmpty()
            MockManga(title, "Project Suki", cover, url, "projectsuki.com")
        }.distinctBy { it.sourceUrl }
    }

    private suspend fun fetchHtml(
        url: String,
        cookie: String? = null,
        referer: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .apply { referer?.let { header("Referer", it) } }
                .apply { cookie?.takeIf(String::isNotBlank)?.let { header("Cookie", it) } }
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Source returned HTTP ${response.code}: $url")
                }
                response.body?.string()
                    ?: throw IllegalStateException("Source returned an empty page: $url")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun fetchComixCatalog(): List<MockManga> = withContext(Dispatchers.IO) {
        val url = comixBrowseUrl(page = 1)
        val html = fetchHtml(url) ?: return@withContext emptyList()
        try {
            ComixCatalogParser.parseHtml(html, url).map(::toMockManga)
        } catch (error: ParserException) {
            android.util.Log.w("MangaScraperRepository", error.message ?: "Could not parse Comix catalog")
            emptyList()
        }
    }

    private fun comixBrowseUrl(page: Int, searchQuery: String? = null): String {
        val builder = "https://comix.to/browse".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("order[score]", "desc")
            .addQueryParameter("page", page.coerceAtLeast(1).toString())
        searchQuery?.takeIf(String::isNotBlank)?.let { builder.addQueryParameter("q", it) }
        return builder.build().toString()
    }

    private fun comixApiModuleDiscoveryScript(): String = """
        const mainScript = document.querySelector('script[type="module"][src*="/dist/main-"]');
        if (!mainScript) throw new Error('Comix application bundle was not found');
        const bundle = await (await fetch(mainScript.src)).text();
        const modulePaths = [...new Set(
            [...bundle.matchAll(/from\s*["'](\.\/[^"']+\.js)["']/g)].map(match => match[1])
        )];
        const modules = await Promise.all(modulePaths.map(async path => {
            try {
                return await import(new URL(path, mainScript.src).href);
            } catch (_) {
                return null;
            }
        }));
        const mangaApi = modules
            .filter(Boolean)
            .flatMap(module => Object.values(module))
            .find(value =>
                value &&
                typeof value === 'object' &&
                typeof value.DD === 'function' &&
                typeof value.yS === 'function'
            );
        if (!mangaApi) throw new Error('Comix manga API is unavailable');
    """.trimIndent()

    private suspend fun fetchComixCatalog(
        context: Context,
        page: Int,
        searchQuery: String? = null
    ): List<MockManga> =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val handler = Handler(Looper.getMainLooper())
                lateinit var webView: WebView
                var completed = false
                val pageUrl = comixBrowseUrl(page, searchQuery)

                fun finish(result: Result<List<MockManga>>) {
                    if (completed) return
                    completed = true
                    handler.removeCallbacksAndMessages(null)
                    webView.removeJavascriptInterface("BedduuComixCatalog")
                    webView.stopLoading()
                    webView.destroy()
                    result.fold(
                        onSuccess = { continuation.resume(it) },
                        onFailure = { continuation.resumeWithException(it) }
                    )
                }

                val bridge = object {
                    @JavascriptInterface
                    fun onInitialData(initialData: String) {
                        try {
                            val manga = ComixCatalogParser.parseInitialData(initialData, pageUrl)
                                .map(::toMockManga)
                            handler.post { finish(Result.success(manga)) }
                        } catch (e: Exception) {
                            handler.post { finish(Result.failure(e)) }
                        }
                    }

                    @JavascriptInterface
                    fun onError(message: String) {
                        handler.post {
                            finish(Result.failure(IllegalStateException("Comix catalog API failed: $message")))
                        }
                    }
                }

                webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = WebSettings.getDefaultUserAgent(context)
                    addJavascriptInterface(bridge, "BedduuComixCatalog")
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            val script = """
                                (function() {
                                    if (window.__bedduuComixCatalogStarted) return;
                                    window.__bedduuComixCatalogStarted = true;
                                    try {
                                        const initialData = document.querySelector('script#initial-data');
                                        if (!initialData) throw new Error('Comix catalog initial data was not found');
                                        window.BedduuComixCatalog.onInitialData(
                                            initialData.textContent || initialData.innerText || ''
                                        );
                                    } catch (error) {
                                        window.BedduuComixCatalog.onError(String(error));
                                    }
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(script, null)
                        }
                    }
                    loadUrl(pageUrl)
                }

                handler.postDelayed(
                    { finish(Result.failure(IllegalStateException("Timed out loading Comix catalog"))) },
                    45_000
                )
                continuation.invokeOnCancellation {
                    handler.post {
                        if (!completed) {
                            completed = true
                            handler.removeCallbacksAndMessages(null)
                            webView.removeJavascriptInterface("BedduuComixCatalog")
                            webView.stopLoading()
                            webView.destroy()
                        }
                    }
                }
            }
        }

    // ─── 7. Fetch Chapters for a Manga ────────────────────────────────────
    suspend fun fetchChapters(manga: MockManga, context: Context? = null): List<ChapterItem> = withContext(Dispatchers.IO) {
        val chapters = mutableListOf<ChapterItem>()

        if (manga.sourceDomain.equals("comix.to", ignoreCase = true) && context != null) {
            try {
                val comixChapters = fetchComixChapters(context, manga.sourceUrl)
                if (comixChapters.isNotEmpty()) return@withContext comixChapters
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Atsu's chapter API includes chapters and sources omitted from manga-page HTML.
        val atsuUrl = manga.sourceUrl.toHttpUrlOrNull()
        val atsuPath = atsuUrl?.encodedPath?.split('/')?.filter(String::isNotBlank).orEmpty()
        if (atsuUrl?.host.equals("atsu.moe", ignoreCase = true) &&
            atsuPath.size >= 2 &&
            atsuPath[0] == "manga"
        ) {
            try {
                val apiUrl = "https://atsu.moe/api/manga/allChapters".toHttpUrlOrNull()
                    ?.newBuilder()
                    ?.addQueryParameter("mangaId", atsuPath[1])
                    ?.build()
                    ?: throw IllegalStateException("Invalid Atsu chapter API URL")
                val request = Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .header("Referer", manga.sourceUrl)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Atsu returned HTTP ${response.code} for its chapter list")
                    }
                    val chapterData = JSONObject(response.body?.string().orEmpty())
                        .optJSONArray("chapters")
                    if (chapterData != null) {
                        val scanlators = try {
                            val pageRequest = Request.Builder()
                                .url(manga.sourceUrl)
                                .header("User-Agent", USER_AGENT)
                                .header("Accept", "text/html,application/xhtml+xml")
                                .apply {
                                    pageRequestCookie(manga.sourceUrl)
                                        ?.takeIf(String::isNotBlank)
                                        ?.let { header("Cookie", it) }
                                }
                                .build()
                            client.newCall(pageRequest).execute().use { pageResponse ->
                                if (pageResponse.isSuccessful) {
                                    parseAtsuScanlators(pageResponse.body?.string().orEmpty())
                                } else {
                                    emptyMap()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            emptyMap()
                        }
                        for (index in 0 until chapterData.length()) {
                            val chapter = chapterData.optJSONObject(index) ?: continue
                            val chapterId = chapter.optString("id", "")
                            if (chapterId.isBlank()) continue
                            val scanlatorId = chapter.optString("scanlationMangaId", "")
                            val title = chapter.optString("title", "").ifBlank {
                                chapter.optString("number", "").takeIf(String::isNotBlank)
                                    ?.let { "Chapter $it" } ?: "Chapter"
                            }
                            chapters.add(
                                ChapterItem(
                                    title = title,
                                    url = "https://atsu.moe/read/${atsuPath[1]}/$chapterId",
                                    uploader = scanlators[scanlatorId]
                                        ?: scanlatorId.takeIf(String::isNotBlank)
                                            ?.let { "Group ${it.take(8)}" }
                                        ?: "Unknown group"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (chapters.isNotEmpty()) {
                return@withContext chapters.distinctBy { normalizeChapterUrl(it.url) }
            }
        }

        // A) WeebCentral / Atsu chapters
        val mangaUrl = manga.sourceUrl.toHttpUrlOrNull()
        if (mangaUrl?.host.equals("weebcentral.com", ignoreCase = true) &&
            mangaUrl?.encodedPath?.split('/')?.filter(String::isNotBlank)?.firstOrNull() == "series"
        ) {
            val seriesId = mangaUrl.encodedPath.split('/').filter(String::isNotBlank).getOrNull(1)
                ?: return@withContext emptyList()
            val listUrl = "https://weebcentral.com/series/$seriesId/full-chapter-list"
            try {
                val req = Request.Builder()
                    .url(listUrl)
                    .header("User-Agent", USER_AGENT)
                    .build()
                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val html = response.body?.string().orEmpty()
                        val doc = Jsoup.parse(html, listUrl)
                        doc.select("div[x-data] > a").forEach { link ->
                            val chapterUrl = link.absUrl("href")
                            if (chapterUrl.isBlank()) return@forEach
                            val title = link.selectFirst("span.flex > span")?.text()
                                ?.replace(Regex("\\s+"), " ")
                                ?.trim()
                                .orEmpty()
                            chapters.add(
                                ChapterItem(
                                    title = title.ifBlank { "Chapter" },
                                    url = chapterUrl,
                                    uploader = if (link.select("img").any {
                                            it.attr("src").contains("official", ignoreCase = true)
                                        }
                                    ) {
                                        "Official"
                                    } else {
                                        "Unknown"
                                    }
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // B) MangaDex chapters
        val mangaDexId = extractMangaDexId(manga.sourceUrl)
        if (chapters.isEmpty() && mangaDexId != null) {
            try {
                var offset = 0
                val candidates = mutableListOf<MangaDexChapter>()
                while (true) {
                    val pageUrl = "https://api.mangadex.org/manga/$mangaDexId/feed".toHttpUrlOrNull()
                        ?.newBuilder()
                        ?.addQueryParameter("includes[]", "scanlation_group")
                        ?.addQueryParameter("order[chapter]", "desc")
                        ?.addQueryParameter("limit", MANGADEX_CHAPTER_PAGE_SIZE.toString())
                        ?.addQueryParameter("offset", offset.toString())
                        ?.build()
                        ?: throw IllegalStateException("Invalid MangaDex chapter feed URL")
                    val pageRequest = Request.Builder()
                        .url(pageUrl)
                        .header("User-Agent", USER_AGENT)
                        .build()
                    val pageData = client.newCall(pageRequest).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IllegalStateException("MangaDex returned HTTP ${response.code}")
                        }
                        JSONObject(response.body?.string().orEmpty()).optJSONArray("data")
                            ?: org.json.JSONArray()
                    }
                    for (i in 0 until pageData.length()) {
                        val item = pageData.optJSONObject(i) ?: continue
                        val chId = item.optString("id", "")
                        val attrs = item.optJSONObject("attributes") ?: continue
                        if (chId.isBlank()) continue
                        val chNum = attrs.optString("chapter", "").trim()
                        val chTitle = attrs.optString("title", "").trim()
                        val displayName = when {
                            chNum.isBlank() && chTitle.isBlank() -> "Oneshot"
                            chNum.isBlank() -> chTitle
                            chTitle.isBlank() -> "Chapter $chNum"
                            else -> "Chapter $chNum - $chTitle"
                        }
                        val relationships = item.optJSONArray("relationships")
                        var uploader = ""
                        if (relationships != null) {
                            for (relationshipIndex in 0 until relationships.length()) {
                                val relationship = relationships.optJSONObject(relationshipIndex) ?: continue
                                if (relationship.optString("type") == "scanlation_group") {
                                    uploader = relationship.optJSONObject("attributes")
                                        ?.optString("name")
                                        .orEmpty()
                                    if (uploader.isNotBlank()) break
                                }
                            }
                        }
                        candidates.add(
                            MangaDexChapter(
                                item = ChapterItem(
                                    title = displayName,
                                    url = "https://mangadex.org/chapter/$chId",
                                    uploader = uploader.ifBlank { null },
                                    language = mangaDexLanguageName(
                                        attrs.optString("translatedLanguage", "")
                                    ),
                                    date = attrs.optString("readableAt", "")
                                ),
                                number = chNum,
                                volume = attrs.optString("volume", "").trim(),
                                uploadedAt = attrs.optString("readableAt", ""),
                                groupId = relationships
                                    ?.let { relations ->
                                        (0 until relations.length())
                                            .asSequence()
                                            .mapNotNull(relations::optJSONObject)
                                            .firstOrNull { it.optString("type") == "scanlation_group" }
                                            ?.optString("id")
                                    }
                                    .orEmpty()
                            )
                        )
                    }
                    if (pageData.length() < MANGADEX_CHAPTER_PAGE_SIZE) break
                    offset += MANGADEX_CHAPTER_PAGE_SIZE
                }
                val groupNames = fetchMangaDexGroupNames(candidates.map(MangaDexChapter::groupId).distinct())
                chapters.addAll(
                    candidates
                        .map { chapter ->
                            chapter.copy(
                                item = chapter.item.copy(
                                    uploader = chapter.item.uploader?.takeIf(String::isNotBlank)
                                        ?: groupNames[chapter.groupId]
                                        ?: chapter.groupId.takeIf(String::isNotBlank)
                                            ?.let { "Group ${it.take(8)}" }
                                        ?: "Unknown group"
                                )
                            )
                        }
                        .sortedWith(
                            compareByDescending<MangaDexChapter> {
                                    it.number.toBigDecimalOrNull() ?: java.math.BigDecimal.valueOf(-1)
                                }
                                .thenByDescending(MangaDexChapter::uploadedAt)
                        )
                        .map(MangaDexChapter::item)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // C) Extract real chapter links from source-rendered manga pages.
        if (chapters.isEmpty() &&
            manga.sourceUrl.startsWith("https://") &&
            !manga.sourceDomain.equals("comix.to", ignoreCase = true)
        ) {
            try {
                val toonGodRequest = manga.sourceUrl.toHttpUrlOrNull()
                    ?.host
                    ?.endsWith("toongod.org", ignoreCase = true) == true
                val cookie = if (toonGodRequest) pageRequestCookie(manga.sourceUrl) else null
                val request = Request.Builder()
                    .url(manga.sourceUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml")
                    .apply {
                        if (toonGodRequest) header("Referer", "https://www.toongod.org/")
                        cookie?.takeIf(String::isNotBlank)?.let { header("Cookie", it) }
                    }
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val html = response.body?.string().orEmpty()
                        if (chapters.isEmpty() &&
                            manga.sourceDomain.equals("atsu.moe", ignoreCase = true) &&
                            manga.sourceUrl.contains("atsu.moe/manga/")
                        ) {
                            chapters.addAll(parseAtsuChapterLinks(html))
                        }
                        if (chapters.isEmpty() &&
                            manga.sourceUrl.toHttpUrlOrNull()?.host?.endsWith("projectsuki.com", ignoreCase = true) == true
                        ) {
                            chapters.addAll(parseProjectSukiChapterLinks(html, manga.sourceUrl))
                        }
                        if (chapters.isEmpty() &&
                            manga.sourceUrl.toHttpUrlOrNull()?.host?.endsWith("toongod.org", ignoreCase = true) == true
                        ) {
                            chapters.addAll(parseMadaraChapterLinks(html, manga.sourceUrl))
                        }
                        if (chapters.isEmpty()) {
                            chapters.addAll(parseChapterLinks(html, manga.sourceUrl))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        chapters.distinctBy { normalizeChapterUrl(it.url) }
        chapters.distinctBy { normalizeChapterUrl(it.url) }
    }

    private data class MangaDexChapter(
        val item: ChapterItem,
        val number: String,
        val volume: String,
        val uploadedAt: String,
        val groupId: String
    )

    private fun fetchMangaDexGroupNames(groupIds: List<String>): Map<String, String> {
        val validGroupIds = groupIds.filter(String::isNotBlank).distinct()
        if (validGroupIds.isEmpty()) return emptyMap()
        val names = mutableMapOf<String, String>()
        validGroupIds.chunked(100).forEach { batch ->
            try {
                val urlBuilder = "https://api.mangadex.org/group".toHttpUrlOrNull()
                    ?.newBuilder()
                    ?: throw IllegalStateException("Invalid MangaDex group API URL")
                batch.forEach { groupId ->
                    urlBuilder.addQueryParameter("ids[]", groupId)
                }
                urlBuilder.addQueryParameter("limit", batch.size.toString())
                val request = Request.Builder()
                    .url(urlBuilder.build())
                    .header("User-Agent", USER_AGENT)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("MangaDex returned HTTP ${response.code} for scanlation groups")
                    }
                    val groups = JSONObject(response.body?.string().orEmpty()).optJSONArray("data")
                        ?: return@use
                    for (index in 0 until groups.length()) {
                        val group = groups.optJSONObject(index) ?: continue
                        val id = group.optString("id", "")
                        val name = group.optJSONObject("attributes")?.optString("name").orEmpty()
                        if (id.isNotBlank() && name.isNotBlank()) names[id] = name
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return names
    }

    private fun normalizeChapterUrl(url: String): String {
        val parsed = url.toHttpUrlOrNull() ?: return url.substringBefore('#').trimEnd('/')
        val builder = parsed.newBuilder().fragment(null)
        parsed.queryParameterNames
            .filter { it.startsWith("utm_", ignoreCase = true) || it.equals("fbclid", true) || it.equals("gclid", true) }
            .forEach(builder::removeAllQueryParameters)
        val normalized = builder.build()
        return normalized.toString().trimEnd('/')
    }

    private suspend fun fetchComixChapters(context: Context, mangaUrl: String): List<ChapterItem> =
        withContext(Dispatchers.Main.immediate) {
            val mangaId = mangaUrl.toHttpUrlOrNull()?.encodedPath
                ?.split('/')?.filter(String::isNotBlank)?.getOrNull(1)
                ?.substringBefore('-')
                ?.takeIf(String::isNotBlank)
                ?: throw IllegalArgumentException("Could not determine Comix manga ID")

            suspendCancellableCoroutine { continuation ->
                val handler = Handler(Looper.getMainLooper())
                lateinit var webView: WebView
                var completed = false

                fun finish(result: Result<List<ChapterItem>>) {
                    if (completed) return
                    completed = true
                    handler.removeCallbacksAndMessages(null)
                    webView.removeJavascriptInterface("BedduuComix")
                    webView.stopLoading()
                    webView.destroy()
                    result.fold(
                        onSuccess = { continuation.resume(it) },
                        onFailure = { continuation.resumeWithException(it) }
                    )
                }

                val bridge = object {
                    @JavascriptInterface
                    fun onChapters(json: String) {
                        try {
                            val data = JSONArray(json)
                            val chapters = buildList {
                                for (index in 0 until data.length()) {
                                    val item = data.optJSONObject(index) ?: continue
                                    val chapterUrl = item.optString("url", "")
                                    if (chapterUrl.isBlank()) continue
                                    val number = item.optString("number", "")
                                    val title = item.optString("name", "").ifBlank {
                                        number.takeIf(String::isNotBlank)?.let { "Chapter $it" } ?: "Chapter"
                                    }
                                    add(
                                        ChapterItem(
                                            title = title,
                                            url = if (chapterUrl.startsWith("http")) chapterUrl
                                            else "https://comix.to/${chapterUrl.trimStart('/')}",
                                            uploader = item.optJSONObject("group")
                                                ?.optString("name")
                                                ?.takeIf(String::isNotBlank),
                                            language = item.optString("language", ""),
                                            date = item.optString("createdAtFormatted", "")
                                        )
                                    )
                                }
                            }.distinctBy { normalizeChapterUrl(it.url) }
                            handler.post { finish(Result.success(chapters)) }
                        } catch (e: Exception) {
                            handler.post { finish(Result.failure(e)) }
                        }
                    }

                    @JavascriptInterface
                    fun onError(message: String) {
                        handler.post {
                            finish(Result.failure(IllegalStateException("Comix chapter API failed: $message")))
                        }
                    }
                }

                webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = WebSettings.getDefaultUserAgent(context)
                    addJavascriptInterface(bridge, "BedduuComix")
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            val script = """
                                (async function() {
                                    if (window.__bedduuComixChaptersStarted) return;
                                    window.__bedduuComixChaptersStarted = true;
                                    try {
                                        ${comixApiModuleDiscoveryScript()}
                                        const items = [];
                                        for (let page = 1; page <= 200; page++) {
                                            const response = await mangaApi.yS(
                                                ${JSONObject.quote(mangaId)},
                                                { page: page, limit: 100, order: { number: 'desc' } }
                                            );
                                            const pageItems = response.RB || response.items || [];
                                            items.push(...pageItems.map(item => ({
                                                url: item.Gq || item.url || '',
                                                number: item.zS ?? item.number ?? '',
                                                name: item.QH || item.name || '',
                                                group: item.EW
                                                    ? { name: item.EW.QH }
                                                    : item.group,
                                                language: item.oJ || item.language || '',
                                                createdAtFormatted: item.EK || item.createdAtFormatted || ''
                                            })));
                                            const meta = response.${'$'}k || response.meta || response.pagination || {};
                                            const lastPage = meta.oV || meta.lastPage || meta.last_page;
                                            if (typeof meta.hasNext === 'boolean') {
                                                if (!meta.hasNext) break;
                                            } else if (typeof meta.${'$'}T === 'boolean') {
                                                if (!meta.${'$'}T) break;
                                            } else if (typeof meta.has_next === 'boolean') {
                                                if (!meta.has_next) break;
                                            } else if (lastPage) {
                                                if (page >= Number(lastPage)) break;
                                            } else if (pageItems.length < 100) {
                                                break;
                                            }
                                        }
                                        window.BedduuComix.onChapters(JSON.stringify(items));
                                    } catch (error) {
                                        window.BedduuComix.onError(String(error));
                                    }
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(script, null)
                        }
                    }
                    loadUrl(mangaUrl)
                }

                val timeout = Runnable {
                    finish(Result.failure(IllegalStateException("Timed out loading Comix chapters")))
                }
                handler.postDelayed(timeout, 45_000)
                continuation.invokeOnCancellation {
                    handler.post {
                        if (!completed) {
                            completed = true
                            handler.removeCallbacksAndMessages(null)
                            webView.removeJavascriptInterface("BedduuComix")
                            webView.stopLoading()
                            webView.destroy()
                        }
                    }
                }
            }
        }

    // ─── 8. Fetch Chapter Images (Direct panel URLs) ──────────────────────
    suspend fun fetchChapterImages(chapterUrl: String): List<String> = withContext(Dispatchers.IO) {
        val images = mutableListOf<String>()

        val projectSukiUrl = chapterUrl.toHttpUrlOrNull()
        val projectSukiPath = projectSukiUrl?.encodedPath?.split('/')?.filter(String::isNotBlank).orEmpty()
        if (projectSukiUrl != null &&
            projectSukiUrl.host.equals("projectsuki.com", ignoreCase = true) &&
            projectSukiPath.size >= 3 &&
            projectSukiPath[0] == "read"
        ) {
            try {
                val payload = JSONObject()
                    .put("bookid", projectSukiPath[1])
                    .put("chapterid", projectSukiPath[2])
                    .put("first", "true")
                    .toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("https://projectsuki.com/callpage")
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Referer", "https://projectsuki.com/browse")
                    .post(payload)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Project Suki returned HTTP ${response.code} for chapter pages")
                    }
                    val html = JSONObject(response.body?.string().orEmpty()).optString("src", "")
                    images.addAll(
                        Jsoup.parseBodyFragment(html, "https://projectsuki.com/")
                            .select("img")
                            .mapNotNull { image -> imageSource(image).takeIf(String::isNotBlank) }
                            .distinct()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext images
        }

        // Atsu's read-page HTML is a client-rendered app; fetch its page list directly.
        val atsuChapterUrl = chapterUrl.toHttpUrlOrNull()
        val atsuChapterPath = atsuChapterUrl?.encodedPath?.split('/')?.filter(String::isNotBlank).orEmpty()
        if (atsuChapterUrl?.host.equals("atsu.moe", ignoreCase = true) &&
            atsuChapterPath.size >= 3 &&
            atsuChapterPath[0] == "read"
        ) {
            try {
                val apiUrl = "https://atsu.moe/api/read/chapter".toHttpUrlOrNull()
                    ?.newBuilder()
                    ?.addQueryParameter("mangaId", atsuChapterPath[1])
                    ?.addQueryParameter("chapterId", atsuChapterPath[2])
                    ?.build()
                    ?: throw IllegalStateException("Invalid Atsu chapter page API URL")
                val request = Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .header("Referer", chapterUrl)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Atsu returned HTTP ${response.code} for chapter pages")
                    }
                    val pageData = JSONObject(response.body?.string().orEmpty())
                        .optJSONObject("readChapter")
                        ?.optJSONArray("pages")
                    if (pageData != null) {
                        for (index in 0 until pageData.length()) {
                            val image = pageData.optJSONObject(index)?.optString("image", "").orEmpty()
                            if (image.isBlank()) continue
                            val imageUrl = when {
                                image.startsWith("https://", ignoreCase = true) -> image
                                image.startsWith("http://", ignoreCase = true) ->
                                    image.replaceFirst(Regex("^http://"), "https://")
                                image.startsWith("//") -> "https:$image"
                                else -> "https://cdn.atsu.moe/static/${image.removePrefix("/").removePrefix("static/")}"
                            }
                            images.add(imageUrl)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (images.isNotEmpty()) return@withContext images.distinct()
        }

        // A) WeebCentral chapter images
        if (chapterUrl.contains("weebcentral.com/chapters/")) {
            val chId = chapterUrl.substringAfter("chapters/").trimEnd('/')
            val imgUrl = "https://weebcentral.com/chapters/$chId/images?is_prev=False&reading_style=long_strip"
            try {
                val req = Request.Builder()
                    .url(imgUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", chapterUrl)
                    .build()
                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val html = response.body?.string().orEmpty()
                        try {
                            images.addAll(
                                ChapterImageParser.parsePageImages(html, imgUrl)
                                    .map { it.imageUrl }
                                    .filterNot { src ->
                                        src.contains("broken_image", true) ||
                                            src.contains("logo", true) ||
                                            src.contains("banner", true)
                                    }
                            )
                        } catch (error: com.hirojonn.bedduu.data.remote.parser.ParserException) {
                            android.util.Log.w(
                                "MangaScraperRepository",
                                error.message ?: "Could not parse chapter images"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // B) MangaDex chapter images
        if (images.isEmpty() && chapterUrl.contains("mangadex.org/chapter/")) {
            val chId = chapterUrl.toHttpUrlOrNull()
                ?.encodedPath
                ?.split('/')
                ?.filter(String::isNotBlank)
                ?.let { segments ->
                    segments.getOrNull(segments.indexOf("chapter") + 1)
                }
                ?.takeIf(String::isNotBlank)
                ?: return@withContext images
            val atHomeUrl = "https://api.mangadex.org/at-home/server/$chId"
            try {
                val req = Request.Builder()
                    .url(atHomeUrl)
                    .header("User-Agent", USER_AGENT)
                    .build()
                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string().orEmpty())
                        val baseUrl = json.optString("baseUrl", "")
                        val chapterObj = json.optJSONObject("chapter")
                        val hash = chapterObj?.optString("hash", "") ?: ""
                        val dataArr = chapterObj?.optJSONArray("data")
                        if (baseUrl.isNotBlank() && hash.isNotBlank() && dataArr != null) {
                            for (i in 0 until dataArr.length()) {
                                val fn = dataArr.getString(i)
                                images.add("$baseUrl/data/$hash/$fn")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // C) Generic HTML image lookup for supported chapter pages.
        if (images.isEmpty() && chapterUrl.startsWith("http") &&
            !chapterUrl.contains("comix.to", ignoreCase = true)
        ) {
            try {
                val toonGodRequest = chapterUrl.toHttpUrlOrNull()
                    ?.host
                    ?.endsWith("toongod.org", ignoreCase = true) == true
                val cookie = if (toonGodRequest) pageRequestCookie(chapterUrl) else null
                val req = Request.Builder()
                    .url(chapterUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Referer", if (toonGodRequest) "https://www.toongod.org/" else chapterUrl)
                    .apply { cookie?.takeIf(String::isNotBlank)?.let { header("Cookie", it) } }
                    .build()
                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val html = response.body?.string().orEmpty()
                        if (html.isNotBlank()) {
                            val doc = Jsoup.parse(html, chapterUrl)
                            val imgElements = doc.select(".reading-content img, .page-break img, .wp-manga-chapter-img, #chapter-images img, .reader-area img, .chapter-image img, img.chapter-img, img[data-src*='chapter']")
                            for (el in imgElements) {
                                val src = imageSource(el)
                                if (src.isNotBlank() &&
                                    !src.contains("logo", true) &&
                                    !src.contains("banner", true) &&
                                    !src.contains("ad", true)
                                ) {
                                    images.add(src)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        images
    }

    private fun parseChapterLinks(html: String, pageUrl: String): List<ChapterItem> {
        val document = Jsoup.parse(html, pageUrl)
        val links = document.select(
            "a[href*='chapter'], a[href*='chap-'], a[href*='/chap/'], a[href*='episode'], a[href*='/read/']"
        )
        val pageHost = pageUrl.toHttpUrlOrNull()?.host
        return links.mapNotNull { link ->
            val url = link.absUrl("href").substringBefore('#')
            val linkHost = url.toHttpUrlOrNull()?.host
            if (url.isBlank() || url == pageUrl || (pageHost != null && linkHost != pageHost)) return@mapNotNull null
            val label = link.text().replace(Regex("\\s+"), " ").trim()
                .ifBlank { link.attr("title").trim() }
            val looksLikeChapter = label.contains(
                Regex("chapter|episode|\\bch\\.?\\s*\\d|\\b\\d+(?:\\.\\d+)?\\b", RegexOption.IGNORE_CASE)
            ) || url.contains(
                Regex("chapter|episode|\\bch[-_/]?\\d", RegexOption.IGNORE_CASE)
            )
            if (!looksLikeChapter) return@mapNotNull null
            ChapterItem(label.ifBlank { "Chapter" }, url)
        }.distinctBy { normalizeChapterUrl(it.url) }
    }

    private fun parseProjectSukiChapterLinks(html: String, pageUrl: String): List<ChapterItem> {
        val document = Jsoup.parse(html, pageUrl)
        return document.select("a[href*='/read/']").mapNotNull { link ->
            val url = link.absUrl("href").substringBefore('#')
            val title = sequenceOf(
                link.text().replace(Regex("\\s+"), " ").trim(),
                link.attr("title").trim(),
                link.attr("aria-label").trim()
            ).firstOrNull(String::isNotBlank).orEmpty()
            if (url.isBlank() || title.isBlank()) return@mapNotNull null
            ChapterItem(title, url)
        }.distinctBy { normalizeChapterUrl(it.url) }
    }

    private fun parseMadaraChapterLinks(html: String, pageUrl: String): List<ChapterItem> {
        return try {
            MadaraParser.parseChapters(html, pageUrl).map { chapter ->
                ChapterItem(chapter.title, normalizeChapterUrl(chapter.url))
            }
        } catch (error: com.hirojonn.bedduu.data.remote.parser.ParserException) {
            android.util.Log.w(
                "MangaScraperRepository",
                error.message ?: "Could not parse chapter list"
            )
            emptyList()
        }
    }

    private fun extractMangaDexId(url: String): String? {
        val path = url.toHttpUrlOrNull()?.encodedPath ?: return null
        val segments = path.split('/').filter(String::isNotBlank)
        val titleIndex = segments.indexOf("title")
        if (titleIndex < 0) return null
        return segments.getOrNull(titleIndex + 1)
            ?.takeIf { it.matches(Regex("[0-9a-fA-F-]{36}")) }
    }

    private fun mangaDexLanguageName(code: String): String = when (code) {
        "en" -> "English"
        "ja" -> "Japanese"
        "ko" -> "Korean"
        "zh" -> "Chinese"
        "zh-hk" -> "Chinese (Hong Kong)"
        "zh-ro" -> "Chinese (Romanized)"
        "vi" -> "Vietnamese"
        "th" -> "Thai"
        "id" -> "Indonesian"
        "es" -> "Spanish"
        "es-la" -> "Spanish (Latin America)"
        "pt-br" -> "Portuguese (Brazil)"
        "fr" -> "French"
        "de" -> "German"
        "it" -> "Italian"
        "ru" -> "Russian"
        "ar" -> "Arabic"
        "tr" -> "Turkish"
        else -> code.takeIf(String::isNotBlank)?.uppercase().orEmpty()
    }

    private fun parseAtsuChapterLinks(html: String): List<ChapterItem> {
        val mangaPage = parseAtsuMangaPage(html) ?: return emptyList()
        val mangaId = mangaPage.optString("id", "")
        val sourceChapters = mangaPage.optJSONArray("chapters") ?: return emptyList()
        if (mangaId.isBlank()) return emptyList()
        val scanlators = parseAtsuScanlators(mangaPage)

        return (0 until sourceChapters.length()).mapNotNull { index ->
            val chapter = sourceChapters.optJSONObject(index) ?: return@mapNotNull null
            val chapterId = chapter.optString("id", "")
            if (chapterId.isBlank()) return@mapNotNull null
            val number = chapter.optString("number", "")
            val title = chapter.optString("title", "").ifBlank {
                number.takeIf(String::isNotBlank)?.let { "Chapter $it" } ?: "Chapter"
            }
            val scanlatorId = chapter.optString("scanlationMangaId", "")
            ChapterItem(
                title = title,
                url = "https://atsu.moe/read/$mangaId/$chapterId",
                uploader = scanlators[scanlatorId]
                    ?: scanlatorId.takeIf(String::isNotBlank)?.let { "Group ${it.take(8)}" }
                    ?: "Unknown group"
            )
        }.distinctBy { normalizeChapterUrl(it.url) }
    }

    private fun parseAtsuScanlators(html: String): Map<String, String> =
        parseAtsuMangaPage(html)?.let(::parseAtsuScanlators).orEmpty()

    private fun parseAtsuScanlators(mangaPage: JSONObject): Map<String, String> {
        val scanlators = mangaPage.optJSONArray("scanlators") ?: return emptyMap()
        return buildMap {
            for (index in 0 until scanlators.length()) {
                val scanlator = scanlators.optJSONObject(index) ?: continue
                val id = scanlator.optString("id", "")
                val name = scanlator.optString("name", "")
                if (id.isNotBlank() && name.isNotBlank()) put(id, name)
            }
        }
    }

    private fun parseAtsuMangaPage(html: String): JSONObject? {
        val script = Jsoup.parse(html).select("script")
            .firstOrNull { it.data().contains("window.mangaPage") || it.html().contains("window.mangaPage") }
            ?: return null
        val scriptContent = script.data().ifBlank { script.html() }
        val assignment = Regex("""window\.mangaPage\s*=\s*""").find(scriptContent) ?: return null
        val jsonStart = scriptContent.indexOf('{', assignment.range.last + 1)
        if (jsonStart < 0) return null

        var depth = 0
        var inString = false
        var escaped = false
        var jsonEnd = -1
        for (index in jsonStart until scriptContent.length) {
            val char = scriptContent[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
            } else {
                when (char) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            jsonEnd = index + 1
                            break
                        }
                    }
                }
            }
        }
        if (jsonEnd < 0) return null

        return runCatching {
            JSONObject(scriptContent.substring(jsonStart, jsonEnd))
                .optJSONObject("mangaPage")
        }.getOrNull()
    }

    private fun imageSource(image: Element): String {
        return ChapterImageParser.imageUrl(image)
    }

    private const val MANGADEX_CHAPTER_PAGE_SIZE = 100
}
