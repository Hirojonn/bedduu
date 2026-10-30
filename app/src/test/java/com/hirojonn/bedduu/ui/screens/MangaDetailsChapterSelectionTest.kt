package com.hirojonn.bedduu.ui.screens

import com.hirojonn.bedduu.ui.ChapterItem
import org.junit.Assert.assertEquals
import org.junit.Test

class MangaDetailsChapterSelectionTest {
    @Test
    fun firstReadingChapterSelectsLowestNumberWhenNewestIsFirst() {
        val chapter99 = ChapterItem("Chapter 99 (End)", "chapter-99")
        val chapter2 = ChapterItem("Chapter 2", "chapter-2")
        val chapter1 = ChapterItem("Chapter 1", "chapter-1")

        assertEquals(chapter1, firstReadingChapter(listOf(chapter99, chapter2, chapter1)))
    }

    @Test
    fun firstReadingChapterUsesOldestSourceEntryWhenTitlesAreUnnumbered() {
        val oldest = ChapterItem("Final part", "oldest")
        val newest = ChapterItem("Latest update", "newest")

        assertEquals(oldest, firstReadingChapter(listOf(newest, oldest)))
    }
}
