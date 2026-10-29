package com.hirojonn.bedduu.domain.repository

import com.hirojonn.bedduu.data.model.MangaDto

interface MangaRepository {
    suspend fun browse(sourceId: String, page: Int): List<MangaDto>
}
