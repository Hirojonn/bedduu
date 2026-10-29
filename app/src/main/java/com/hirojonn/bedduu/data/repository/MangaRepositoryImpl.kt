package com.hirojonn.bedduu.data.repository

import com.hirojonn.bedduu.data.remote.MangaSource
import com.hirojonn.bedduu.domain.repository.MangaRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException

class MangaRepositoryImpl(
    sources: List<MangaSource>
) : MangaRepository {
    private val sourcesById = sources.associateBy(MangaSource::id)

    override suspend fun browse(sourceId: String, page: Int) =
        request(sourceId) { it.browse(page) }

    private suspend fun <T> request(
        sourceId: String,
        operation: suspend (MangaSource) -> T
    ): T {
        val source = sourcesById[sourceId]
            ?: throw SourceException.UnknownSource(sourceId)
        return try {
            operation(source)
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            throw SourceException.Network(sourceId, error)
        }
    }
}

sealed class SourceException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    class UnknownSource(sourceId: String) :
        SourceException("No scraper source is registered for '$sourceId'")

    class Network(sourceId: String, cause: IOException) :
        SourceException("Network request failed for '$sourceId'", cause)
}
