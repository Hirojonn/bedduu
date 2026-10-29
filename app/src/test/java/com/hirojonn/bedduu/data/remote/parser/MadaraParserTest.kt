package com.hirojonn.bedduu.data.remote.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class MadaraParserTest {
    @Test
    fun `preserves chapter order extracts numbering and skips incomplete entries`() {
        val chapters = MadaraParser.parseChapters(
            loadFixture("madara-series-detail.html"),
            "https://reader.example.test/manga/fixture/"
        )

        assertEquals(
            listOf("Chapter 12: The Return", "Chapter 11", "Chapter 9"),
            chapters.map { it.title }
        )
        assertEquals(listOf("12", "11", "9"), chapters.map { it.number })
        assertEquals(
            listOf(
                "https://reader.example.test/manga/fixture/chapter-12/",
                "https://reader.example.test/manga/fixture/chapter-11/",
                "https://reader.example.test/manga/fixture/chapter-9/"
            ),
            chapters.map { it.url }
        )
    }

    @Test
    fun `malformed html with no chapter selector reports layout change`() {
        val error = try {
            MadaraParser.parseChapters("<html><body><div>new layout</div>", "https://reader.test/series")
            error("Expected selector failure")
        } catch (expected: ParserException) {
            expected
        }

        assertEquals("madara", error.sourceId)
    }
}
