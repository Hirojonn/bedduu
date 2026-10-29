package com.hirojonn.bedduu.data.remote.parser

import com.hirojonn.bedduu.data.model.MangaDto
import org.jsoup.Jsoup

object WeebCentralParser {
    private const val SOURCE_ID = "weebcentral"
    private const val TITLE_SELECTOR = "article a[href*='/series/']"

    fun parseTitles(html: String, pageUrl: String): List<MangaDto> {
        val document = Jsoup.parse(html, pageUrl)
        val cards = document.select(TITLE_SELECTOR)
        if (cards.isEmpty()) {
            throw ParserException(SOURCE_ID, pageUrl, "Expected manga cards were not found")
        }

        val manga = cards.mapNotNull { link ->
            val url = link.absUrl("href").substringBefore('?').trimEnd('/')
            val image = link.selectFirst("img")
            val title = sequenceOf(
                link.text().trim(),
                link.attr("title").trim(),
                image?.attr("alt")?.trim().orEmpty()
            ).firstOrNull(String::isNotBlank).orEmpty()

            if (url.isBlank() || title.isBlank()) {
                null
            } else {
                MangaDto(
                    id = url.substringAfterLast('/'),
                    sourceId = SOURCE_ID,
                    title = title,
                    coverUrl = image?.let(ChapterImageParser::imageUrl).orEmpty(),
                    detailUrl = url
                )
            }
        }.distinctBy(MangaDto::detailUrl)

        if (manga.isEmpty()) {
            throw ParserException(SOURCE_ID, pageUrl, "Manga cards no longer contain a title and series URL")
        }
        return manga
    }
}
