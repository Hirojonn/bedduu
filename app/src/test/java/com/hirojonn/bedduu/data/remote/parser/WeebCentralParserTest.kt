package com.hirojonn.bedduu.data.remote.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class WeebCentralParserTest {
    @Test
    fun `parses titles and lazy cover urls from saved home page`() {
        val manga = WeebCentralParser.parseTitles(
            loadFixture("weebcentral-home.html"),
            "https://weebcentral.com/browse"
        )

        assertEquals(listOf("Alpha", "Beta"), manga.map { it.title })
        assertEquals(
            listOf(
                "https://weebcentral.com/covers/alpha.jpg",
                "https://cdn.example.test/beta.jpg"
            ),
            manga.map { it.coverUrl }
        )
        assertEquals(
            listOf(
                "https://weebcentral.com/series/alpha",
                "https://weebcentral.com/series/beta"
            ),
            manga.map { it.detailUrl }
        )
    }

    @Test
    fun `layout change and empty html produce a clear parser error`() {
        listOf("", "<html><body><main>changed markup</main></body></html>").forEach { html ->
            val error = try {
                WeebCentralParser.parseTitles(html, "https://weebcentral.com/browse")
                error("Expected selector failure")
            } catch (expected: ParserException) {
                expected
            }
            assertEquals("weebcentral", error.sourceId)
            assertEquals("https://weebcentral.com/browse", error.pageUrl)
        }
    }
}
