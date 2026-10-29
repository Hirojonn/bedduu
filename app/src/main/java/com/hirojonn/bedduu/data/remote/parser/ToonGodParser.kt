package com.hirojonn.bedduu.data.remote.parser

import com.hirojonn.bedduu.data.model.MangaDto
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object ToonGodParser {
    private const val SERIES_LINKS =
        "a[href*='/webtoon/'], a[href*='/webtoons/'], a[href*='/manga/']"
    private const val CARD_SELECTOR = ".page-item-detail, .c-tabs-item__content, .item-summary"

    fun parseTitles(html: String, pageUrl: String): List<MangaDto> {
        val document = Jsoup.parse(html, pageUrl)
        val links = buildList {
            document.select(CARD_SELECTOR).forEach { card ->
                card.selectFirst(SERIES_LINKS)?.let(::add)
            }
            document.select(SERIES_LINKS).forEach(::add)
        }.distinctBy { it.absUrl("href") }

        val titles = links.mapNotNull { link ->
            val detailUrl = link.absUrl("href").substringBefore('#').substringBefore('?').trimEnd('/')
            val parsedUrl = detailUrl.toHttpUrlOrNull() ?: return@mapNotNull null
            if (!parsedUrl.host.endsWith("toongod.org", ignoreCase = true)) return@mapNotNull null
            val path = parsedUrl.encodedPath.split('/').filter(String::isNotBlank)
            if (path.size != 2 || path[0] !in setOf("webtoon", "webtoons", "manga")) {
                return@mapNotNull null
            }

            val card = link.parents().firstOrNull(::isCard)
                ?: link.parents().firstOrNull {
                    it.selectFirst("img, .post-title, .manga-title, .item-title, h3, h4") != null
                }
            val image = link.selectFirst("img")
                ?: card?.selectFirst("img")
                ?: link.parent()?.selectFirst("img")
            val title = sequenceOf(
                card?.selectFirst(".post-title a, .post-title, .manga-title, .item-title, h3, h4")?.text().orEmpty(),
                link.attr("title"),
                link.attr("aria-label"),
                link.text(),
                image?.attr("alt").orEmpty()
            ).map { it.replace(Regex("\\s+"), " ").trim() }
                .firstOrNull(String::isNotBlank)
                ?: path.last().replace('-', ' ').replaceFirstChar(Char::uppercase)

            MangaDto(
                id = path.last(),
                sourceId = "toongod",
                title = title,
                coverUrl = image?.let(ChapterImageParser::imageUrl).orEmpty(),
                detailUrl = detailUrl
            )
        }.distinctBy(MangaDto::detailUrl)

        if (titles.isEmpty()) {
            throw ParserException("toongod", pageUrl, "No ToonGod series cards were found")
        }
        return titles
    }

    private fun isCard(element: Element): Boolean =
        element.hasClass("page-item-detail") ||
            element.hasClass("c-tabs-item__content") ||
            element.hasClass("item-summary")
}
