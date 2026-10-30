package com.hirojonn.bedduu.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar

enum class CustomScraperSource(val id: String, val domain: String, val prettyName: String, val logoUrl: String) {
    ATSU(
        "atsu",
        "atsu.moe",
        "Atsu.moe",
        "https://www.google.com/s2/favicons?sz=128&domain=atsu.moe"
    ),
    MANGADEX(
        "mangadex",
        "mangadex.org",
        "MangaDex",
        "https://www.google.com/s2/favicons?sz=128&domain=mangadex.org"
    ),
    WEEBCENTRAL(
        "weebcentral",
        "weebcentral.com",
        "WeebCentral",
        "https://www.google.com/s2/favicons?sz=128&domain=weebcentral.com"
    ),
    PROJECT_SUKI(
        "project_suki",
        "projectsuki.com",
        "Project Suki",
        "https://www.google.com/s2/favicons?sz=128&domain=projectsuki.com"
    ),
    COMIX(
        "comix",
        "comix.to",
        "Comix.to",
        "https://www.google.com/s2/favicons?sz=128&domain=comix.to"
    ),
    TOONGOD(
        "toongod",
        "www.toongod.org",
        "ToonGod",
        "https://www.google.com/s2/favicons?sz=128&domain=toongod.org"
    )
}

data class ChapterItem(
    val title: String,
    val url: String,
    val uploader: String? = null,
    val language: String = "",
    val date: String = ""
)

data class ChapterUpdate(
    val manga: MockManga,
    val chapter: ChapterItem,
    val detectedAt: Long = System.currentTimeMillis()
)

data class MockManga(
    val title: String,
    val author: String,
    val coverUrl: String,
    val sourceUrl: String = "",
    val sourceDomain: String = "",
    val chapters: List<ChapterItem> = emptyList(),
    val alternateTitle: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val status: String = ""
)

data class ParserModel(
    val name: String,
    val path: String,
    val type: String,
    val logoUrl: String = "",
    val source: CustomScraperSource? = null,
    var isInstalled: Boolean = false
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences("bendduu_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // ─── First Launch & User Profile ──────────────────────────────────────
    var isFirstLaunch        by mutableStateOf(prefs.getBoolean("is_first_launch", true))
    var username             by mutableStateOf(prefs.getString("username", "Reader_99") ?: "Reader_99")
    var userAvatarIndex      by mutableStateOf(prefs.getInt("avatar_index", 0))
    var customAvatarPath     by mutableStateOf(prefs.getString("custom_avatar_path", null))
        private set
    var avatarUploadError    by mutableStateOf<String?>(null)
        private set
    var dailyGoalMinutes     by mutableStateOf(prefs.getInt("daily_goal", 30))
    var weeklyActivity       by mutableStateOf<List<Int>>(loadWeeklyStats())

    // ─── Settings (Backed by persistent SharedPreferences) ───────────────
    var isDarkMode           by mutableStateOf(prefs.getBoolean("is_dark_mode", true))
    var pureBlackOled        by mutableStateOf(prefs.getBoolean("pure_black_oled", false))
    var notificationsEnabled by mutableStateOf(prefs.getBoolean("notifications", true))
    var highQualityImages    by mutableStateOf(prefs.getBoolean("high_quality", true))
    var autoBookmark         by mutableStateOf(prefs.getBoolean("auto_bookmark", false))
    var showPageNumber       by mutableStateOf(prefs.getBoolean("show_page_number", true))
    var readLeftToRight      by mutableStateOf(prefs.getBoolean("read_ltr", false))
    var readingMode          by mutableStateOf(prefs.getString("reading_mode", "webtoon") ?: "webtoon")
    var keepScreenOn         by mutableStateOf(prefs.getBoolean("keep_screen_on", true))

    // ─── Stats (Persistent real tracking) ─────────────────────────────────
    var readingTimeMinutes   by mutableStateOf(prefs.getInt("read_time_minutes", 0))
    var currentStreak        by mutableStateOf(prefs.getInt("streak", 0))
    var chaptersRead         by mutableStateOf(prefs.getInt("chapters_read", 0))
    var totalSessions        by mutableStateOf(prefs.getInt("total_sessions", 0))

    // ─── Library ──────────────────────────────────────────────────────────
    var bookmarkedManga      by mutableStateOf<List<MockManga>>(loadBookmarks())
    var favoriteMangaKeys    by mutableStateOf(
        prefs.getStringSet("favorite_manga_keys", emptySet())?.toSet().orEmpty()
    )
    var favoriteMangaItems   by mutableStateOf(loadMangaList("favorite_manga_items"))
        private set
    var readingHistory       by mutableStateOf(loadMangaList("reading_history"))
        private set
    var chapterUpdates      by mutableStateOf(loadChapterUpdates())
        private set
    var isCheckingChapterUpdates by mutableStateOf(false)
        private set
    var chapterUpdateCheckMessage by mutableStateOf<String?>(null)
        private set
    private var knownChapterUrlsByManga = loadKnownChapterUrls()
    var lastReadChapterUrls  by mutableStateOf(
        runCatching {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson<Map<String, String>>(
                prefs.getString("last_read_chapters", null),
                type
            ).orEmpty()
        }.getOrDefault(emptyMap())
    )
    var lastReadPageNumbers by mutableStateOf(
        runCatching {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson<Map<String, Int>>(
                prefs.getString("last_read_pages", null),
                type
            ).orEmpty()
        }.getOrDefault(emptyMap())
    )
    var detailsChapters      by mutableStateOf<List<ChapterItem>>(emptyList())
        private set
    var isLoadingDetails     by mutableStateOf(false)
        private set
    var detailsLoadMessage   by mutableStateOf<String?>(null)
        private set
    var selectedDetailsLanguage by mutableStateOf<String?>(null)
    var selectedDetailsUploader by mutableStateOf<String?>(null)
    var isDetailsMoreMenuExpanded by mutableStateOf(false)
        private set
    private var detailsMangaKey: String? = null
    private var detailsLoadedMangaKey: String? = null
    private var detailsLoadJob: Job? = null

    // ─── Extensions & Browser ─────────────────────────────────────────────
    var parsersList          by mutableStateOf<List<ParserModel>>(emptyList())
    var isLoadingParsers     by mutableStateOf(false)
    var searchQuery          by mutableStateOf("")
    var currentBrowserUrl    by mutableStateOf<String?>(null)

    // ─── Global Search (Screenshot 1: multi-source horizontal results) ─────
    var globalSearchResults  by mutableStateOf<Map<String, List<MockManga>>>(emptyMap())
    var isGlobalSearching    by mutableStateOf(false)
    var activeGlobalSearchQuery by mutableStateOf("")
    private var globalSearchJob: Job? = null

    // ─── Source Catalog (Screenshot 2: 3-column box grid of manga covers) ──
    var selectedCatalogSource by mutableStateOf<CustomScraperSource?>(null)
    var catalogMangaList      by mutableStateOf<List<MockManga>>(emptyList())
    var isLoadingCatalog      by mutableStateOf(false)
    var catalogFilterQuery    by mutableStateOf("")
    var isLoadingMoreCatalog  by mutableStateOf(false)
    var hasMoreCatalog        by mutableStateOf(false)
    private var catalogPage = 1
    private var catalogRequestId = 0
    private var catalogLoadJob: Job? = null

    // ─── Persistence Helpers ──────────────────────────────────────────────
    fun savePreferences() {
        prefs.edit().apply {
            putBoolean("is_first_launch", isFirstLaunch)
            putString("username", username)
            putInt("avatar_index", userAvatarIndex)
            putInt("daily_goal", dailyGoalMinutes)
            putString("weekly_stats", gson.toJson(weeklyActivity))
            putBoolean("is_dark_mode", isDarkMode)
            putBoolean("pure_black_oled", pureBlackOled)
            putBoolean("notifications", notificationsEnabled)
            putBoolean("high_quality", highQualityImages)
            putBoolean("auto_bookmark", autoBookmark)
            putBoolean("show_page_number", showPageNumber)
            putBoolean("read_ltr", readLeftToRight)
            putString("reading_mode", readingMode)
            putBoolean("keep_screen_on", keepScreenOn)
            putInt("read_time_minutes", readingTimeMinutes)
            putInt("streak", currentStreak)
            putInt("chapters_read", chaptersRead)
            putInt("total_sessions", totalSessions)
            putString("bookmarks_json", gson.toJson(bookmarkedManga))
            putStringSet("favorite_manga_keys", favoriteMangaKeys)
            putString("favorite_manga_items", gson.toJson(favoriteMangaItems))
            putString("reading_history", gson.toJson(readingHistory))
            putString("last_read_chapters", gson.toJson(lastReadChapterUrls))
            putString("last_read_pages", gson.toJson(lastReadPageNumbers))
            putString("custom_avatar_path", customAvatarPath)
            putString("chapter_updates", gson.toJson(chapterUpdates))
            putString("known_chapter_urls", gson.toJson(knownChapterUrlsByManga))
            apply()
        }
    }

    init {
        prefs.edit().remove("recent_json").apply()
        prefs.edit().remove("release_notifications").apply()
        WorkManager.getInstance(application).cancelUniqueWork("keiyoushi_release_check")
        // If stored stats match previous fake defaults, reset them to zero
        if (weeklyActivity == listOf(25, 40, 15, 60, 35, 75, 50)) {
            resetStatistics()
        }
    }

    private fun loadWeeklyStats(): List<Int> {
        val json = prefs.getString("weekly_stats", null) ?: return listOf(0, 0, 0, 0, 0, 0, 0)
        return try {
            val type = object : TypeToken<List<Int>>() {}.type
            val list: List<Int>? = gson.fromJson(json, type)
            if (list != null && list.size == 7) {
                if (list == listOf(25, 40, 15, 60, 35, 75, 50)) {
                    listOf(0, 0, 0, 0, 0, 0, 0)
                } else list
            } else listOf(0, 0, 0, 0, 0, 0, 0)
        } catch (e: Exception) {
            listOf(0, 0, 0, 0, 0, 0, 0)
        }
    }

    private fun loadBookmarks(): List<MockManga> {
        val json = prefs.getString("bookmarks_json", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<MockManga>>() {}.type
            val list: List<MockManga> = gson.fromJson(json, type) ?: emptyList()
            list.map { m ->
                if (m.coverUrl.contains("picsum")) {
                    m.copy(coverUrl = fallbackCover(m.title))
                } else m
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadMangaList(preferenceKey: String): List<MockManga> {
        val json = prefs.getString(preferenceKey, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<MockManga>>() {}.type
            val list: List<MockManga> = gson.fromJson(json, type) ?: emptyList()
            list.filter { it.title.isNotBlank() }.map { manga ->
                if (manga.coverUrl.contains("picsum")) {
                    manga.copy(coverUrl = fallbackCover(manga.title))
                } else {
                    manga
                }
            }
        } catch (error: Exception) {
            error.printStackTrace()
            emptyList()
        }
    }

    private fun fallbackCover(title: String): String {
        val pkg = getApplication<Application>().packageName
        return "android.resource://$pkg/drawable/ic_launcher"
    }

    // ─── Onboarding Action ────────────────────────────────────────────────
    fun completeOnboarding(
        name: String,
        avatar: Int,
        mode: String,
        oled: Boolean,
        quality: Boolean
    ) {
        username = name.ifBlank { "Bendduu Reader" }
        userAvatarIndex = avatar
        readingMode = mode
        pureBlackOled = oled
        highQualityImages = quality
        isFirstLaunch = false
        savePreferences()
    }

    fun updateProfile(name: String, avatar: Int) {
        username = name.ifBlank { "Bendduu Reader" }
        userAvatarIndex = avatar
        savePreferences()
    }

    fun updateProfile(name: String) {
        username = name.ifBlank { "Bendduu Reader" }
        savePreferences()
    }

    fun saveAvatar(uri: Uri, context: Context) {
        avatarUploadError = null
        viewModelScope.launch {
            try {
                val savedPath = withContext(Dispatchers.IO) {
                    val resolver = context.applicationContext.contentResolver
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                        ?: throw IOException("The selected image could not be opened.")
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                        throw IOException("The selected file is not a supported image.")
                    }

                    var sampleSize = 1
                    while (bounds.outWidth / sampleSize > 512 ||
                        bounds.outHeight / sampleSize > 512
                    ) {
                        sampleSize *= 2
                    }
                    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    val bitmap = resolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, options)
                    } ?: throw IOException("The selected image could not be decoded.")

                    val target = File(context.applicationContext.filesDir, "profile-avatar.jpg")
                    val temporary = File(context.applicationContext.filesDir, "profile-avatar.tmp")
                    try {
                        FileOutputStream(temporary).use { output ->
                            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
                                throw IOException("The selected image could not be saved.")
                            }
                        }
                    } finally {
                        bitmap.recycle()
                    }
                    if (target.exists() && !target.delete()) {
                        temporary.delete()
                        throw IOException("The previous profile image could not be replaced.")
                    }
                    if (!temporary.renameTo(target)) {
                        temporary.delete()
                        throw IOException("The profile image could not be stored.")
                    }
                    target.absolutePath
                }
                customAvatarPath = savedPath
                savePreferences()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("AppViewModel", "Could not save the selected profile image.", error)
                avatarUploadError = "Could not save image: ${error.message ?: "unsupported image"}"
            }
        }
    }

    suspend fun checkBookmarkedChapterUpdates(context: Context) {
        if (isCheckingChapterUpdates || bookmarkedManga.isEmpty()) return

        isCheckingChapterUpdates = true
        chapterUpdateCheckMessage = "Checking bookmarked manga for new chapters…"
        var checkedCount = 0
        var failedCount = 0
        val updatedItems = chapterUpdates.toMutableList()
        val knownUrls = knownChapterUrlsByManga.toMutableMap()
        val refreshedMangaByKey = mutableMapOf<String, MockManga>()

        try {
            for (manga in bookmarkedManga) {
                val key = mangaKey(manga)
                try {
                    val fetched = MangaScraperRepository.fetchChapters(
                        manga,
                        context.applicationContext
                    )
                    if (fetched.isEmpty()) {
                        failedCount++
                        Log.w("AppViewModel", "No chapters returned while checking ${manga.title}.")
                        continue
                    }

                    checkedCount++
                    val priorUrls = knownUrls[key]
                        ?: manga.chapters.map { chapterUrlKey(it.url) }.toList()
                    val newChapters = if (priorUrls.isEmpty() && manga.chapters.isEmpty()) {
                        emptyList()
                    } else {
                        fetched.filter { chapterUrlKey(it.url) !in priorUrls }
                    }

                    val refreshedManga = manga.copy(
                        chapters = (fetched + manga.chapters)
                            .distinctBy { chapterUrlKey(it.url) }
                    )
                    refreshedMangaByKey[key] = refreshedManga
                    newChapters.filterNot { chapter ->
                        updatedItems.any {
                            mangaKey(it.manga) == key &&
                                chapterUrlKey(it.chapter.url) == chapterUrlKey(chapter.url)
                        }
                    }.forEach { chapter ->
                        updatedItems.add(
                            0,
                            ChapterUpdate(manga = refreshedManga, chapter = chapter)
                        )
                    }
                    knownUrls[key] = (fetched + manga.chapters)
                        .map { chapterUrlKey(it.url) }
                        .distinct()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    failedCount++
                    Log.w("AppViewModel", "Could not check chapters for ${manga.title}.", error)
                }
            }

            chapterUpdates = updatedItems
            knownChapterUrlsByManga = knownUrls
            bookmarkedManga = bookmarkedManga.map { manga ->
                refreshedMangaByKey[mangaKey(manga)] ?: manga
            }
            savePreferences()
            chapterUpdateCheckMessage = when {
                failedCount > 0 ->
                    "Checked $checkedCount of ${bookmarkedManga.size} bookmarks; $failedCount could not be checked."
                checkedCount > 0 ->
                    "Checked $checkedCount bookmarked manga. ${chapterUpdates.size} new chapter updates."
                else -> "No bookmarked manga could be checked."
            }
        } finally {
            isCheckingChapterUpdates = false
        }
    }

    fun clearChapterUpdates() {
        chapterUpdates = emptyList()
        savePreferences()
    }

    private fun chapterUrlKey(url: String): String = url.substringBefore('#').trimEnd('/')

    private fun loadKnownChapterUrls(): Map<String, List<String>> =
        runCatching {
            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            gson.fromJson<Map<String, List<String>>>(
                prefs.getString("known_chapter_urls", null),
                type
            ).orEmpty()
        }.getOrDefault(emptyMap())

    private fun loadChapterUpdates(): List<ChapterUpdate> =
        runCatching {
            val type = object : TypeToken<List<ChapterUpdate>>() {}.type
            gson.fromJson<List<ChapterUpdate>>(
                prefs.getString("chapter_updates", null),
                type
            ).orEmpty()
        }.getOrDefault(emptyList())

    // ─── Actions ──────────────────────────────────────────────────────────

    fun bookmarkManga(
        title: String,
        url: String,
        coverUrl: String = "",
        author: String = "Unknown Author",
        chapters: List<ChapterItem> = emptyList()
    ) {
        val cleanTitle = title.ifBlank { "Manga" }
        val domain = try { java.net.URI(url).host ?: "" } catch (e: Exception) { "" }

        val finalCover = if (coverUrl.isNotBlank() && !coverUrl.contains("picsum")) {
            coverUrl
        } else {
            fallbackCover(cleanTitle)
        }

        val existingIndex = bookmarkedManga.indexOfFirst {
            it.title.equals(cleanTitle, ignoreCase = true) || (url.isNotBlank() && it.sourceUrl == url)
        }
        if (existingIndex >= 0) {
            val existing = bookmarkedManga[existingIndex]
            val updated = existing.copy(
                coverUrl = if (finalCover.contains("ic_launcher") && !existing.coverUrl.contains("ic_launcher")) existing.coverUrl else finalCover,
                chapters = if (chapters.isNotEmpty()) chapters else existing.chapters,
                author = if (author != "Unknown Author") author else existing.author
            )
            bookmarkedManga = bookmarkedManga.toMutableList().also { it[existingIndex] = updated }
        } else {
            val newManga = MockManga(
                title = cleanTitle,
                author = author,
                coverUrl = finalCover,
                sourceUrl = url,
                sourceDomain = domain,
                chapters = chapters
            )
            bookmarkedManga = listOf(newManga) + bookmarkedManga
        }
        savePreferences()
    }

    fun removeBookmark(manga: MockManga) {
        bookmarkedManga = bookmarkedManga.filter { it.title != manga.title }
        chapterUpdates = chapterUpdates.filterNot { mangaKey(it.manga) == mangaKey(manga) }
        knownChapterUrlsByManga = knownChapterUrlsByManga - mangaKey(manga)
        savePreferences()
    }

    fun toggleFavorite(manga: MockManga) {
        val key = mangaKey(manga)
        val isFavorite = key in favoriteMangaKeys
        favoriteMangaKeys = favoriteMangaKeys.toMutableSet().apply {
            if (isFavorite) remove(key) else add(key)
        }
        favoriteMangaItems = if (isFavorite) {
            favoriteMangaItems.filterNot { mangaKey(it) == key }
        } else {
            listOf(manga) + favoriteMangaItems.filterNot { mangaKey(it) == key }
        }
        savePreferences()
    }

    fun loadMangaDetails(manga: MockManga, context: Context, forceRefresh: Boolean = false) {
        val key = mangaKey(manga)
        if (detailsMangaKey != key) {
            detailsLoadJob?.cancel()
            detailsMangaKey = key
            detailsLoadedMangaKey = null
            detailsChapters = manga.chapters
            detailsLoadMessage = null
            selectedDetailsLanguage = null
            selectedDetailsUploader = null
            isDetailsMoreMenuExpanded = false
        }
        if (!forceRefresh &&
            (isLoadingDetails || detailsLoadedMangaKey == key || detailsLoadMessage != null)
        ) return

        detailsLoadJob?.cancel()
        isLoadingDetails = true
        detailsLoadMessage = null
        detailsLoadJob = viewModelScope.launch {
            try {
                val fetched = MangaScraperRepository.fetchChapters(manga, context.applicationContext)
                if (detailsMangaKey == key) {
                    if (fetched.isNotEmpty()) {
                        detailsChapters = (fetched + detailsChapters)
                            .distinctBy { it.url.substringBefore('#').trimEnd('/') }
                    } else if (detailsChapters.isEmpty()) {
                        detailsLoadMessage = getApplication<Application>()
                            .getString(com.hirojonn.bedduu.R.string.details_chapters_unavailable)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (detailsMangaKey == key && detailsChapters.isEmpty()) {
                    detailsLoadMessage = getApplication<Application>()
                        .getString(com.hirojonn.bedduu.R.string.details_chapters_load_failed)
                }
            } finally {
                if (detailsMangaKey == key) {
                    detailsLoadedMangaKey = key
                    isLoadingDetails = false
                }
            }
        }
    }

    fun setDetailsLanguage(language: String?) {
        selectedDetailsLanguage = language
    }

    fun setDetailsUploader(uploader: String?) {
        selectedDetailsUploader = uploader
    }

    fun setDetailsOverflowMenuExpanded(expanded: Boolean) {
        isDetailsMoreMenuExpanded = expanded
    }

    fun clearAllBookmarks() {
        bookmarkedManga = emptyList()
        chapterUpdates = emptyList()
        knownChapterUrlsByManga = emptyMap()
        savePreferences()
    }

    fun readChapter(manga: MockManga, chapter: ChapterItem? = null) {
        chaptersRead += 1
        totalSessions += 1
        if (currentStreak == 0) currentStreak = 1
        chapter?.url?.takeIf(String::isNotBlank)?.let { chapterUrl ->
            lastReadChapterUrls = lastReadChapterUrls + (mangaKey(manga) to chapterUrl)
        }

        val updatedManga = if (manga.coverUrl.contains("picsum")) {
            manga.copy(coverUrl = fallbackCover(manga.title))
        } else manga
        val historyKey = mangaKey(updatedManga)
        readingHistory = listOf(updatedManga) + readingHistory.filterNot {
            mangaKey(it) == historyKey
        }.take(99)

        // Auto-bookmark on read if enabled
        if (autoBookmark && bookmarkedManga.none { it.title.equals(manga.title, ignoreCase = true) }) {
            bookmarkedManga = listOf(updatedManga) + bookmarkedManga
        }

        savePreferences()
    }

    fun lastReadPage(manga: MockManga, chapter: ChapterItem): Int? =
        lastReadPageNumbers[chapterProgressKey(manga, chapter)]

    fun recordReadPage(manga: MockManga, chapter: ChapterItem, page: Int) {
        if (chapter.url.isBlank()) return
        lastReadPageNumbers = lastReadPageNumbers +
            (chapterProgressKey(manga, chapter) to page.coerceAtLeast(1))
        savePreferences()
    }

    private fun chapterProgressKey(manga: MockManga, chapter: ChapterItem): String =
        "${mangaKey(manga)}\u0000${chapter.url}"

    private fun mangaKey(manga: MockManga): String =
        manga.sourceUrl.ifBlank { manga.title.lowercase() }

    /**
     * Records real tracked reading time (in minutes) from active reader sessions.
     */
    fun recordRealReadingTime(minutes: Int) {
        if (minutes <= 0) return
        readingTimeMinutes += minutes

        val dayIndex = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0=Mon..6=Sun
        val updatedWeekly = weeklyActivity.toMutableList()
        if (dayIndex in updatedWeekly.indices) {
            updatedWeekly[dayIndex] = updatedWeekly[dayIndex] + minutes
            weeklyActivity = updatedWeekly
        }
        if (currentStreak == 0) currentStreak = 1
        savePreferences()
    }

    fun resetStatistics() {
        chaptersRead = 0
        readingTimeMinutes = 0
        currentStreak = 0
        totalSessions = 0
        weeklyActivity = listOf(0, 0, 0, 0, 0, 0, 0)
        savePreferences()
    }

    fun clearCache(context: Context) {
        try {
            context.cacheDir.deleteRecursively()
            WebView(context).clearCache(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openWebsite(domain: String) {
        currentBrowserUrl = "https://$domain"
    }

    fun fetchParsers() {
        if (parsersList.isNotEmpty()) return
        isLoadingParsers = true
        viewModelScope.launch {
            try {
                parsersList = CustomScraperSource.values().map { source ->
                    ParserModel(
                        name    = source.prettyName,
                        path    = source.domain,
                        type    = "custom_scraper",
                        logoUrl = source.logoUrl,
                        source  = source
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingParsers = false
            }
        }
    }

    // ─── Global Search (Screenshot 1) ─────────────────────────────────────
    fun performGlobalSearch(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            clearGlobalSearch()
            return
        }
        globalSearchJob?.cancel()
        activeGlobalSearchQuery = normalizedQuery
        globalSearchResults = emptyMap()
        isGlobalSearching = true
        globalSearchJob = viewModelScope.launch {
            try {
                val results = MangaScraperRepository.searchAllSources(
                    normalizedQuery,
                    getApplication()
                )
                if (activeGlobalSearchQuery == normalizedQuery) {
                    globalSearchResults = results
                }
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                e.printStackTrace()
                if (activeGlobalSearchQuery == normalizedQuery) {
                    globalSearchResults = emptyMap()
                }
            } finally {
                if (activeGlobalSearchQuery == normalizedQuery) {
                    isGlobalSearching = false
                }
            }
        }
    }

    fun clearGlobalSearch() {
        globalSearchJob?.cancel()
        globalSearchJob = null
        activeGlobalSearchQuery = ""
        globalSearchResults = emptyMap()
        isGlobalSearching = false
    }

    // ─── Source Catalog (Screenshot 2: Box Form) ──────────────────────────
    fun openCatalog(source: CustomScraperSource) {
        catalogLoadJob?.cancel()
        val requestId = ++catalogRequestId
        selectedCatalogSource = source
        isLoadingCatalog = true
        isLoadingMoreCatalog = false
        hasMoreCatalog = false
        catalogPage = 1
        catalogFilterQuery = ""
        catalogLoadJob = viewModelScope.launch {
            try {
                val list = MangaScraperRepository.fetchCatalog(source, getApplication())
                if (requestId == catalogRequestId && selectedCatalogSource == source) {
                    catalogMangaList = list
                    hasMoreCatalog = list.isNotEmpty()
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    e.printStackTrace()
                    if (requestId == catalogRequestId && selectedCatalogSource == source) {
                        catalogMangaList = emptyList()
                    }
                } else {
                    throw e
                }
            } finally {
                if (requestId == catalogRequestId) {
                    isLoadingCatalog = false
                    catalogLoadJob = null
                }
            }
        }
    }

    fun closeCatalog() {
        catalogLoadJob?.cancel()
        catalogLoadJob = null
        catalogRequestId += 1
        selectedCatalogSource = null
        catalogMangaList = emptyList()
        catalogFilterQuery = ""
        isLoadingCatalog = false
        isLoadingMoreCatalog = false
        hasMoreCatalog = false
        catalogPage = 1
    }

    fun refreshCurrentCatalog() {
        val source = selectedCatalogSource ?: return
        val query = catalogFilterQuery
        if (query.isBlank()) {
            openCatalog(source)
        } else {
            searchWithinCatalog(query)
        }
    }

    fun loadNextCatalogPage() {
        val source = selectedCatalogSource ?: return
        if (catalogFilterQuery.isNotBlank() ||
            isLoadingCatalog ||
            isLoadingMoreCatalog ||
            !hasMoreCatalog
        ) return

        val nextPage = catalogPage + 1
        val requestId = ++catalogRequestId
        isLoadingMoreCatalog = true
        catalogLoadJob = viewModelScope.launch {
            try {
                val page = MangaScraperRepository.fetchCatalogPage(source, nextPage, getApplication())
                if (requestId != catalogRequestId ||
                    selectedCatalogSource != source ||
                    catalogFilterQuery.isNotBlank()
                ) return@launch
                catalogPage = nextPage
                catalogMangaList = catalogMangaList + page
                hasMoreCatalog = page.isNotEmpty()
            } catch (e: Exception) {
                if (e !is CancellationException) e.printStackTrace() else throw e
            } finally {
                if (requestId == catalogRequestId) {
                    isLoadingMoreCatalog = false
                    catalogLoadJob = null
                }
            }
        }
    }

    fun searchWithinCatalog(query: String) {
        val src = selectedCatalogSource ?: return
        if (query.isBlank()) {
            openCatalog(src)
            return
        }
        catalogLoadJob?.cancel()
        val requestId = ++catalogRequestId
        catalogFilterQuery = query
        isLoadingMoreCatalog = false
        hasMoreCatalog = false
        isLoadingCatalog = true
        catalogLoadJob = viewModelScope.launch {
            try {
                val results = when (src) {
                    CustomScraperSource.ATSU -> MangaScraperRepository.searchAtsu(query)
                    CustomScraperSource.MANGADEX -> MangaScraperRepository.searchMangaDex(query)
                    CustomScraperSource.WEEBCENTRAL -> MangaScraperRepository.searchWeebCentral(query)
                    CustomScraperSource.PROJECT_SUKI -> MangaScraperRepository.searchProjectSuki(query)
                    CustomScraperSource.COMIX ->
                        MangaScraperRepository.searchComix(getApplication(), query)
                    CustomScraperSource.TOONGOD ->
                        MangaScraperRepository.searchToonGod(query, getApplication())
                }
                if (requestId == catalogRequestId &&
                    selectedCatalogSource == src &&
                    catalogFilterQuery == query
                ) {
                    catalogMangaList = results
                }
            } catch (e: Exception) {
                if (e !is CancellationException) e.printStackTrace() else throw e
            } finally {
                if (requestId == catalogRequestId) {
                    isLoadingCatalog = false
                    catalogLoadJob = null
                }
            }
        }
    }

    val readingTimeHours: Int get() = readingTimeMinutes / 60
}
