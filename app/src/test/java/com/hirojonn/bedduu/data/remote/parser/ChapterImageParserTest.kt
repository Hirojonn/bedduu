package com.hirojonn.bedduu.data.remote.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterImageParserTest {
    @Test
    fun `resolves lazy and regular image attributes and ignores invalid sources`() {
        val pages = ChapterImageParser.parsePageImages(
            loadFixture("weebcentral-chapter.html"),
            "https://weebcentral.com/chapters/example"
        )

        assertEquals(
            listOf(
                "https://weebcentral.com/pages/001.jpg",
                "https://cdn.example.test/pages/002.jpg",
                "https://images.example.test/pages/003.jpg",
                "https://weebcentral.com/pages/004.jpg"
            ),
            pages.map { it.imageUrl }
        )
    }

    @Test
    fun `image selector mismatch reports layout change`() {
        val error = try {
            ChapterImageParser.parsePageImages(
                "<html><body><div class='reader-v2'></div></body></html>",
                "https://weebcentral.com/chapters/example"
            )
            error("Expected selector failure")
        } catch (expected: ParserException) {
            expected
        }

        assertEquals("weebcentral", error.sourceId)
    }
}
