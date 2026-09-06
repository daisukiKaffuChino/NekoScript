package io.github.daisukikaffuchino.nekoscript.engine.character

import io.github.daisukikaffuchino.nekoscript.engine.background.BackgroundState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SceneStateTest {
    @Test
    fun characterUsesCenterAsDefaultPosition() {
        val character = CharacterState(characterId = "yuki", expression = "normal")

        assertEquals(CharacterPosition.Center, character.position)
    }

    @Test
    fun sceneIdentifiersCannotBeBlank() {
        assertFailsWith<IllegalArgumentException> { CharacterState(" ") }
        assertFailsWith<IllegalArgumentException> { BackgroundState("") }
    }
}
