package io.github.daisukikaffuchino.nekoscript

import io.github.daisukikaffuchino.nekoscript.engine.asset.ManifestAssetManager
import io.github.daisukikaffuchino.nekoscript.engine.project.GameProjectParser
import io.github.daisukikaffuchino.nekoscript.ui.asset.ComposeResourceImageAssetResolver
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
    fun resolvesPackagedBackgroundCharacterAndCgAsImageData() = runTest {
        val source = ComposeResourceGameProjectSource()
        val project = GameProjectParser().parse(source.readText("game.json"))
        val resolver = ComposeResourceImageAssetResolver(ManifestAssetManager(project))
        val background = resolver.resolveBackground("school_day")
        val character = resolver.resolveCharacter("yuki", "normal")
        val cg = resolver.resolveCg("club_photo")

        assertTrue(background.data.hasJpegSignature(), "school_day should contain JPEG data")
        assertTrue(character.data.hasPngSignature(), "yuki:normal should contain PNG data")
        assertTrue(cg.data.hasJpegSignature(), "club_photo should contain JPEG data")
        assertTrue(listOf(background, character, cg).all { it.cacheKey.startsWith("nekoscript:") })
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

    private fun ByteArray.hasJpegSignature(): Boolean =
        size >= 3 && this[0].toInt() and 0xFF == 0xFF && this[1].toInt() and 0xFF == 0xD8 &&
            this[2].toInt() and 0xFF == 0xFF

    private fun ByteArray.hasPngSignature(): Boolean =
        size >= 8 && take(8).map { it.toInt() and 0xFF } == listOf(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
}
