package com.hirojonn.bedduu.data.remote.parser

import com.hirojonn.bedduu.data.model.ChapterDto
import org.jsoup.Jsoup

object MadaraParser {
    private const val SOURCE_ID = "madara"
    private const val CHAPTER_SELECTOR = ".wp-manga-chapter a, li.wp-manga-chapter a"
    private val chapterNumberPattern =
        Regex("""(?:chapter|ch\.?|episode|ep\.?)\s*#?\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)

    fun parseChapters(html: String, pageUrl: String): List<ChapterDto> {
        val document = Jsoup.parse(html, pageUrl)
        val links = document.select(CHAPTER_SELECTOR)
        if (links.isEmpty()) {
            throw ParserException(SOURCE_ID, pageUrl, "Expected chapter links were not found")
        }

        val chapters = links.mapNotNull { link ->
            if (link.attr("href").isBlank()) return@mapNotNull null
            val url = link.absUrl("href").substringBefore('#')
            val title = sequenceOf(
                link.text().replace(Regex("\\s+"), " ").trim(),
                link.attr("title").trim()
            ).firstOrNull(String::isNotBlank).orEmpty()
            if (url.isBlank() || title.isBlank()) {
                null
            } else {
                ChapterDto(
                    id = url.trimEnd('/').substringAfterLast('/'),
                    title = title,
                    url = url,
                    number = chapterNumberPattern.find(title)?.groupValues?.get(1)
                )
            }
        }.distinctBy(ChapterDto::url)

        if (chapters.isEmpty()) {
            throw ParserException(SOURCE_ID, pageUrl, "Chapter links no longer contain titles and URLs")
        }
        return chapters
    }
}
