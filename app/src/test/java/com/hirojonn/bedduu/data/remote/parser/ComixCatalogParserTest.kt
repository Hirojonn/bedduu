package com.hirojonn.bedduu.data.remote.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ComixCatalogParserTest {
    @Test
    fun `parses manga from any initial query response`() {
        val titles = ComixCatalogParser.parseHtml(
            loadFixture("comix-browse.html"),
            "https://comix.to/browse?order%5Bscore%5D=desc&page=1"
        )

        assertEquals(listOf("Comix Alpha", "Comix Beta"), titles.map { it.title })
        assertEquals(
            listOf(
                "https://comix.to/title/abc123-comix-alpha",
                "https://comix.to/title/def456-comix-beta"
            ),
            titles.map { it.detailUrl }
        )
        assertEquals(
            listOf("https://img.comix.to/abc.jpg", "https://img.comix.to/def.jpg"),
            titles.map { it.coverUrl }
        )
    }

    @Test
    fun `missing page data reports a useful parser error`() {
        val error = assertThrows(ParserException::class.java) {
            ComixCatalogParser.parseHtml(
                "<html><body>changed layout</body></html>",
                "https://comix.to/browse"
            )
        }

        assertEquals("comix", error.sourceId)
        assertEquals("https://comix.to/browse", error.pageUrl)
    }
}
