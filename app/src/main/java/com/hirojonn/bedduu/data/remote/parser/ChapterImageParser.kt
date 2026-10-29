package com.hirojonn.bedduu.data.remote.parser

import com.hirojonn.bedduu.data.model.PageDto
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object ChapterImageParser {
    private const val SOURCE_ID = "weebcentral"
    private const val IMAGE_SELECTOR = "section[x-data~=scroll] > img, #chapter-images img"
    private val imageAttributes = listOf("data-src", "data-lazy-src", "data-original", "src")

    fun parsePageImages(html: String, pageUrl: String): List<PageDto> {
        val document = Jsoup.parse(html, pageUrl)
        val images = document.select(IMAGE_SELECTOR)
        if (images.isEmpty()) {
            throw ParserException(SOURCE_ID, pageUrl, "Expected chapter page images were not found")
        }

        val pages = images.mapNotNull { image ->
            imageUrl(image).takeIf(String::isNotBlank)?.let(::PageDto)
        }.distinctBy(PageDto::imageUrl)

        if (pages.isEmpty()) {
            throw ParserException(SOURCE_ID, pageUrl, "Chapter images no longer contain usable URLs")
        }
        return pages
    }

    fun imageUrl(image: Element): String {
        val raw = imageAttributes
            .asSequence()
            .map { image.attr(it).trim() }
            .firstOrNull(String::isNotBlank)
            ?: return ""
        if (raw.startsWith("data:", ignoreCase = true)) return ""

        val attribute = imageAttributes.firstOrNull { image.attr(it).trim() == raw } ?: return ""
        return image.absUrl(attribute).ifBlank {
            when {
                raw.startsWith("//") -> "https:$raw"
                raw.startsWith("http://", ignoreCase = true) ||
                    raw.startsWith("https://", ignoreCase = true) -> raw
                else -> ""
            }
        }.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }.orEmpty()
    }
}
