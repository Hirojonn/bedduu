package com.hirojonn.bedduu.data.model

data class MangaDto(
    val id: String,
    val sourceId: String,
    val title: String,
    val coverUrl: String,
    val detailUrl: String
)

data class ChapterDto(
    val id: String,
    val title: String,
    val url: String,
    val number: String? = null
)

data class PageDto(val imageUrl: String)
