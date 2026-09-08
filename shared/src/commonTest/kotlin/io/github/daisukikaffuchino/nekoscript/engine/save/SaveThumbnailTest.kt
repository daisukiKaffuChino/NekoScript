package io.github.daisukikaffuchino.nekoscript.engine.save

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SaveThumbnailTest {
    @Test
    fun encodesRawFrameIntoPngThumbnail() {
        val frame = RawFrame(
            width = 2,
            height = 2,
            pixels = intArrayOf(
                0xFFFF0000.toInt(),
                0xFF00FF00.toInt(),
                0xFF0000FF.toInt(),
                0xFFFFFFFF.toInt(),
            ),
        )

        val thumbnail = requireNotNull(frame.toSaveThumbnail())

        assertNotNull(thumbnail.imageBytes)
        assertTrue(thumbnail.imageBytes.isNotEmpty())
        assertEquals(2, thumbnail.imageWidth)
        assertEquals(2, thumbnail.imageHeight)
    }
}
