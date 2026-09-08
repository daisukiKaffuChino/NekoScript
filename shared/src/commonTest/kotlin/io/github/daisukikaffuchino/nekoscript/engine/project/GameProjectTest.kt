package io.github.daisukikaffuchino.nekoscript.engine.project

import io.github.daisukikaffuchino.nekoscript.engine.asset.ManifestAssetManager
import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GameProjectTest {
    private val source = """
        {
          "id": "sample",
          "name": "示例游戏",
          "version": "1.0.0",
          "entryScript": "scripts/main.avg",
          "viewport": { "width": 1024, "height": 768 },
          "backgrounds": { "school": "backgrounds/school.jpg" },
          "characters": {
            "yuki": {
              "name": "悠希",
              "defaultExpression": "normal",
              "expressions": {
                "normal": "characters/yuki/normal.png",
                "happy": "characters/yuki/happy.png"
              }
            }
          },
          "cg": { "ending": "cg/ending.png" },
          "audio": { "bgm": { "theme": "audio/bgm/theme.ogg" } }
        }
    """.trimIndent()

    @Test
    fun parsesPortableProjectManifest() {
        val project = GameProjectParser().parse(source)

        assertEquals("sample", project.id)
        assertEquals("示例游戏", project.name)
        assertEquals("scripts/main.avg", project.entryScript)
        assertEquals(ViewportConfig(1024, 768), project.viewport)
        assertEquals("悠希", project.characters.getValue("yuki").name)
        assertEquals("audio/bgm/theme.ogg", project.audio.bgm.getValue("theme"))
    }

    @Test
    fun resolvesLogicalAssetIdsWithoutReadingFiles() = runTest {
        val manager = ManifestAssetManager(GameProjectParser().parse(source))

        assertEquals("backgrounds/school.jpg", manager.loadBackground("school").location)
        assertEquals("characters/yuki/normal.png", manager.loadCharacter("yuki", null).location)
        assertEquals("characters/yuki/happy.png", manager.loadCharacter("yuki", "happy").location)
        assertEquals("cg/ending.png", manager.loadCg("ending").location)
        assertEquals("audio/bgm/theme.ogg", manager.loadBgm("theme").location)
        assertFailsWith<EngineException.AssetNotFound> { manager.loadBackground("missing") }
        assertFailsWith<EngineException.AssetNotFound> { manager.loadCharacter("yuki", "sad") }
        assertFailsWith<EngineException.AssetNotFound> { manager.loadVoice("missing") }
    }

    @Test
    fun rejectsMalformedAndInvalidManifests() {
        assertFailsWith<EngineException.ScriptParseError> { GameProjectParser().parse("not json") }
        assertFailsWith<EngineException.ScriptParseError> {
            GameProjectParser().parse(
                """{"id":"","name":"Game","version":"1","entryScript":"main.avg"}""",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            CharacterDefinition("Yuki", "normal", mapOf("happy" to "happy.png"))
        }
    }
}
