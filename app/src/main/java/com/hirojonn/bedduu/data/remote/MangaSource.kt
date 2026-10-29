package com.hirojonn.bedduu.data.remote

import com.hirojonn.bedduu.data.model.MangaDto

interface MangaSource {
    val id: String

    suspend fun browse(page: Int): List<MangaDto>
}
