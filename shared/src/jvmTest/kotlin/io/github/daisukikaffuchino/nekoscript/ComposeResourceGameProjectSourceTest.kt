package io.github.daisukikaffuchino.nekoscript

import io.github.daisukikaffuchino.nekoscript.engine.project.GameProjectParser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ComposeResourceGameProjectSourceTest {
    @Test
    fun readsPackagedManifestAndEntryScriptAsUtf8Text() = runTest {
        val source = ComposeResourceGameProjectSource()
        val manifest = source.readText("game.json")
        val script = source.readText("scripts/main.avg")
        assertTrue(manifest.contains("\"entryScript\": \"scripts/main.avg\""))
        assertTrue(script.contains("choice:"))
        assertTrue(script.contains("background \"classroom\""))
        assertTrue(script.contains("say \"NekoScript Demo  完\""))
    }

    @Test
    fun packagedDemoMatchesTheInMemoryTestProject() = runTest {
        val packaged = ComposeResourceGameProjectSource()
        val parser = GameProjectParser()

        assertEquals(
            parser.parse(demoProjectSource.readText("game.json")),
            parser.parse(packaged.readText("game.json")),
        )
        assertEquals(
            demoProjectSource.readText("scripts/main.avg").normalizedLines(),
            packaged.readText("scripts/main.avg").normalizedLines(),
        )
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

    private fun String.normalizedLines(): String = lines().joinToString("\n").trim()
}
