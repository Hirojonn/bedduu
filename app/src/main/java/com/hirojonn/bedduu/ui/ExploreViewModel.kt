package com.hirojonn.bedduu.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

private data class SourceCatalogResult(
    val source: CustomScraperSource,
    val items: List<MockManga>,
    val error: String? = null
)

data class ExploreCatalogState(
    val items: List<MockManga> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null
)

class ExploreViewModel(application: Application) : AndroidViewModel(application) {
    var state by mutableStateOf(ExploreCatalogState())
        private set

    private val sources = CustomScraperSource.values().toList()
    private var nextPage = 2
    private var requestId = 0

    fun load(refresh: Boolean = false) {
        if (state.isLoading || state.isRefreshing) return
        val request = ++requestId
        state = state.copy(
            isLoading = !refresh && state.items.isEmpty(),
            isRefreshing = refresh,
            error = null
        )
        viewModelScope.launch {
            try {
                val results = kotlinx.coroutines.coroutineScope {
                    sources.map { source ->
                        async {
                            try {
                                SourceCatalogResult(
                                    source,
                                    MangaScraperRepository.fetchCatalog(source, getApplication())
                                )
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                android.util.Log.w("ExploreCatalog", "${source.prettyName} catalog failed", error)
                                SourceCatalogResult(
                                    source,
                                    emptyList(),
                                    error.message ?: "Request failed"
                                )
                            }
                        }
                    }.map { it.await() }
                }
                if (request != requestId) return@launch
                val items = results.flatMap(SourceCatalogResult::items)
                    .filter { it.title.isNotBlank() && it.coverUrl.isNotBlank() }
                    .distinctBy { it.sourceUrl.ifBlank { "${it.sourceDomain}:${it.title}" } }
                val failedSources = results.filter { it.error != null }
                    .map { it.source.prettyName }
                state = state.copy(
                    items = if (refresh) items else (state.items + items).distinctBy {
                        it.sourceUrl.ifBlank { "${it.sourceDomain}:${it.title}" }
                    },
                    isLoading = false,
                    isRefreshing = false,
                    hasMore = items.isNotEmpty(),
                    error = when {
                        failedSources.isNotEmpty() ->
                            "Couldn't load: ${failedSources.joinToString()}. Pull to retry."
                        items.isEmpty() -> "Sources returned no titles. Check your connection and retry."
                        else -> null
                    }
                )
                nextPage = 2
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (request == requestId) {
                    state = state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = error.message ?: "Could not load Explore."
                    )
                }
            }
        }
    }

    fun loadMore() {
        if (state.isLoading || state.isRefreshing || state.isLoadingMore || !state.hasMore) return
        val request = requestId
        val page = nextPage
        state = state.copy(isLoadingMore = true)
        viewModelScope.launch {
            try {
                val results = kotlinx.coroutines.coroutineScope {
                    sources.map { source ->
                        async {
                            try {
                                SourceCatalogResult(
                                    source,
                                    MangaScraperRepository.fetchCatalogPage(source, page, getApplication())
                                )
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                android.util.Log.w("ExploreCatalog", "${source.prettyName} page load failed", error)
                                SourceCatalogResult(
                                    source,
                                    emptyList(),
                                    error.message ?: "Request failed"
                                )
                            }
                        }
                    }.map { it.await() }
                }
                if (request != requestId) return@launch
                val newItems = results.flatMap(SourceCatalogResult::items)
                    .filter { it.title.isNotBlank() && it.coverUrl.isNotBlank() }
                val failedSources = results.filter { it.error != null }
                    .map { it.source.prettyName }
                state = state.copy(
                    items = (state.items + newItems).distinctBy {
                        it.sourceUrl.ifBlank { "${it.sourceDomain}:${it.title}" }
                    },
                    hasMore = newItems.isNotEmpty(),
                    isLoadingMore = false,
                    error = failedSources.takeIf { it.isNotEmpty() }
                        ?.let { "Couldn't load more from: ${it.joinToString()}. Pull to retry." }
                )
                nextPage += 1
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (request == requestId) {
                    state = state.copy(
                        isLoadingMore = false,
                        hasMore = false,
                        error = error.message ?: "Could not load more titles. Try refreshing."
                    )
                }
            }
        }
    }
}
