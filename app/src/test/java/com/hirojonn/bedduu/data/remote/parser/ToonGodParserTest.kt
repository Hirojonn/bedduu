package com.hirojonn.bedduu.data.remote.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ToonGodParserTest {
    @Test
    fun `parses singular webtoon routes lazy covers and excludes chapter links`() {
        val titles = ToonGodParser.parseTitles(
            loadFixture("toongod-catalog.html"),
            "https://www.toongod.org/webtoon/"
        )

        assertEquals(listOf("Series Alpha", "Series Beta"), titles.map { it.title })
        assertEquals(
            listOf(
                "https://www.toongod.org/webtoon/series-alpha",
                "https://www.toongod.org/webtoon/series-beta"
            ),
            titles.map { it.detailUrl }
        )
        assertEquals(
            listOf(
                "https://www.toongod.org/wp-content/uploads/alpha.jpg",
                "https://cdn.toongod.org/beta.webp"
            ),
            titles.map { it.coverUrl }
        )
    }

    @Test
    fun `empty or changed markup reports a parser error`() {
        val error = assertThrows(ParserException::class.java) {
            ToonGodParser.parseTitles(
                "<html><body><main>changed layout</main></body></html>",
                "https://www.toongod.org/webtoon/"
            )
        }

        assertEquals("toongod", error.sourceId)
    }
}
