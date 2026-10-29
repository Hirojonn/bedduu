package com.hirojonn.bedduu.data.repository

import com.hirojonn.bedduu.data.model.MangaDto
import com.hirojonn.bedduu.data.remote.MangaSource
import java.net.SocketTimeoutException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaRepositoryImplTest {
    @Test
    fun `returns source results`() = runTest {
        val expected = listOf(
            MangaDto("alpha", "fake", "Alpha", "", "https://fake.test/alpha")
        )
        val repository = MangaRepositoryImpl(listOf(FakeSource { expected }))

        assertEquals(expected, repository.browse("fake", page = 1))
    }

    @Test
    fun `preserves an empty successful response`() = runTest {
        val repository = MangaRepositoryImpl(listOf(FakeSource { emptyList() }))

        assertEquals(emptyList<MangaDto>(), repository.browse("fake", page = 1))
    }

    @Test
    fun `wraps timeout as a source network failure`() = runTest {
        val repository = MangaRepositoryImpl(
            listOf(FakeSource { throw SocketTimeoutException("request timed out") })
        )

        val error = try {
            repository.browse("fake", page = 1)
            error("Expected network failure")
        } catch (expected: SourceException.Network) {
            expected
        }
        assertEquals("request timed out", error.cause?.message)
    }

    @Test
    fun `unknown source fails explicitly`() = runTest {
        val repository = MangaRepositoryImpl(emptyList())

        val error = try {
            repository.browse("missing", page = 1)
            error("Expected missing-source failure")
        } catch (expected: SourceException.UnknownSource) {
            expected
        }
        assertTrue(error.message?.contains("missing") == true)
    }

    private class FakeSource(
        private val result: suspend (Int) -> List<MangaDto>
    ) : MangaSource {
        override val id: String = "fake"

        override suspend fun browse(page: Int): List<MangaDto> = result(page)
    }
}
