package io.github.daisukikaffuchino.nekoscript

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ComposeResourceGameProjectSourceTest {
    @Test
    fun readsPackagedManifestAndEntryScriptAsUtf8Text() = runTest {
        val source = ComposeResourceGameProjectSource()
        val manifest = source.readText("game.json")
        val script = source.readText("scripts/main.avg")
        assertTrue(manifest.contains("\"entryScript\": \"scripts/main.avg\""))
        assertTrue(script.contains("say \"悠希\" \"早上好。\""))
    }

    @Test
    fun rejectsUnsafeProjectLocations() = runTest {
        val source = ComposeResourceGameProjectSource()
        assertFailsWith<IllegalArgumentException> { source.readText("") }
        assertFailsWith<IllegalArgumentException> { source.readText("/game.json") }
        assertFailsWith<IllegalArgumentException> { source.readText("../game.json") }
        assertFailsWith<IllegalArgumentException> { source.readText("scripts/../../game.json") }
        assertFailsWith<IllegalArgumentException> { source.readText("C:/game.json") }
    }
}
