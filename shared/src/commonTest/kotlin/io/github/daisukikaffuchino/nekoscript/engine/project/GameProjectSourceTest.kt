package io.github.daisukikaffuchino.nekoscript.engine.project

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class GameProjectSourceTest {
    @Test
    fun defaultByteReadingKeepsGameProjectSourceSamCompatible() = runTest {
        val source = GameProjectSource { location -> "text:$location" }

        assertEquals("text:main.avg", source.readText("main.avg"))
        assertContentEquals("text:main.avg".encodeToByteArray(), source.readBytes("main.avg"))
    }

    @Test
    fun mapSourceReturnsDefensiveBinaryCopies() = runTest {
        val original = byteArrayOf(1, 2, 3)
        val source = MapGameProjectSource(emptyMap(), mapOf("audio/test.mp3" to original))

        val loaded = source.readBytes("audio/test.mp3")
        loaded[0] = 9

        assertContentEquals(byteArrayOf(1, 2, 3), source.readBytes("audio/test.mp3"))
    }
}
