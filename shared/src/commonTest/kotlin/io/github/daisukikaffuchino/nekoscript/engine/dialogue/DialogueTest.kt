package io.github.daisukikaffuchino.nekoscript.engine.dialogue

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DialogueTest {
    @Test
    fun supportsNarrationAndUnicodeText() {
        val dialogue = Dialogue(speaker = null, text = "今日は晴れ。天气很好。")

        assertNull(dialogue.speaker)
        assertEquals("今日は晴れ。天气很好。", dialogue.text)
    }
}
