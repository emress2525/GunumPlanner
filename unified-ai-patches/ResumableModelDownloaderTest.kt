package com.unifiedai.app.data.provider.local

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ResumableModelDownloaderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun retriesEmptyBodyWithoutAdvancingAndCompletesByRange() {
        server.enqueue(
            MockResponse.Builder()
                .code(206)
                .addHeader("Content-Range", "bytes 0-3/12")
                .body("")
                .build(),
        )
        server.enqueue(rangeResponse(0, 3, 12, "abcd"))
        server.enqueue(rangeResponse(4, 7, 12, "efgh"))
        server.enqueue(rangeResponse(8, 11, 12, "ijkl"))

        val target = temporaryFolder.newFile("model.part").apply { delete() }
        val downloader = ResumableModelDownloader(
            client = OkHttpClient(),
            url = server.url("/model").toString(),
            expectedSize = 12,
            chunkSizeBytes = 4,
            maxAttemptsPerChunk = 3,
        )

        downloader.download(target)

        assertEquals("abcdefghijkl", target.readText())
        assertEquals("bytes=0-3", server.takeRequest().headers["Range"])
        assertEquals("bytes=0-3", server.takeRequest().headers["Range"])
        assertEquals("bytes=4-7", server.takeRequest().headers["Range"])
        assertEquals("bytes=8-11", server.takeRequest().headers["Range"])
    }

    @Test
    fun resumesFromExistingPartialFile() {
        server.enqueue(rangeResponse(4, 7, 12, "efgh"))
        server.enqueue(rangeResponse(8, 11, 12, "ijkl"))

        val target = temporaryFolder.newFile("model.part").apply { writeText("abcd") }
        val downloader = ResumableModelDownloader(
            client = OkHttpClient(),
            url = server.url("/model").toString(),
            expectedSize = 12,
            chunkSizeBytes = 4,
            maxAttemptsPerChunk = 3,
        )

        downloader.download(target)

        assertEquals("abcdefghijkl", target.readText())
        assertEquals("bytes=4-7", server.takeRequest().headers["Range"])
        assertEquals("bytes=8-11", server.takeRequest().headers["Range"])
    }

    @Test
    fun failsAfterRepeatedEmptyBodiesWithoutCorruptingPartialFile() {
        repeat(3) {
            server.enqueue(
                MockResponse.Builder()
                    .code(206)
                    .addHeader("Content-Range", "bytes 4-7/12")
                    .body("")
                    .build(),
            )
        }

        val target = temporaryFolder.newFile("model.part").apply { writeText("abcd") }
        val downloader = ResumableModelDownloader(
            client = OkHttpClient(),
            url = server.url("/model").toString(),
            expectedSize = 12,
            chunkSizeBytes = 4,
            maxAttemptsPerChunk = 3,
        )

        val error = runCatching { downloader.download(target) }.exceptionOrNull()

        checkNotNull(error)
        assertEquals("abcd", target.readText())
        assertEquals(3, server.requestCount)
    }

    private fun rangeResponse(start: Int, end: Int, total: Int, body: String): MockResponse =
        MockResponse.Builder()
            .code(206)
            .addHeader("Content-Range", "bytes $start-$end/$total")
            .body(body)
            .build()
}
