package io.github.daisukikaffuchino.nekoscript.engine.runtime

import io.github.daisukikaffuchino.nekoscript.engine.audio.AudioPlayer
import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import io.github.daisukikaffuchino.nekoscript.engine.project.GameProject
import io.github.daisukikaffuchino.nekoscript.engine.project.MapGameProjectSource
import io.github.daisukikaffuchino.nekoscript.engine.save.InMemorySaveStorage
import io.github.daisukikaffuchino.nekoscript.engine.save.JsonSaveManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameSessionFactoryTest {
    @Test
    fun assemblesManifestEntryScriptEngineAndAssets() = runTest {
        val createdForProjects = mutableListOf<String>()
        val session = factory(
            files = mapOf(
                "game.json" to validManifest,
                "scripts/main.avg" to "background \"school\"\nsay \"Yuki\" \"Good morning\"",
            ),
            onCreateSaveManager = { createdForProjects += it.id },
        ).create()

        assertEquals("sample", session.project.id)
        assertEquals("scripts/main.avg", session.project.entryScript)
        assertEquals("scripts/main.avg", session.script.id)
        assertEquals(listOf("sample"), createdForProjects)

        session.engine.dispatch(GameAction.Next)
        assertEquals("school", session.engine.viewState.value.background?.assetId)
        assertEquals("Good morning", session.engine.viewState.value.dialogue?.text)
        assertTrue(session.engine.viewState.value.isWaiting)
        assertEquals("backgrounds/school.jpg", session.assetManager.loadBackground("school").location)
        assertEquals("characters/yuki/happy.png", session.assetManager.loadCharacter("yuki", "happy").location)
    }

    @Test
    fun passesProjectToAudioPlayerFactory() = runTest {
        var audioProject: GameProject? = null
        val audioPlayer = RecordingAudioPlayer()
        val session = factory(
            files = mapOf(
                "game.json" to validManifest,
                "scripts/main.avg" to "play_bgm \"theme\"\nsay \"Playing\"",
            ),
            audioPlayerFactory = AudioPlayerFactory { project ->
                audioProject = project
                audioPlayer
            },
        ).create()

        session.engine.dispatch(GameAction.Next)

        assertEquals("sample", audioProject?.id)
        assertEquals(listOf("theme"), audioPlayer.bgmIds)
    }

    @Test
    fun sessionReleasesItsAudioPlayer() = runTest {
        val audioPlayer = RecordingAudioPlayer()
        val session = factory(
            files = mapOf(
                "game.json" to validManifest,
                "scripts/main.avg" to "say \"Ready\"",
            ),
            audioPlayerFactory = AudioPlayerFactory { audioPlayer },
        ).create()

        session.release()

        assertTrue(audioPlayer.released)
    }

    @Test
    fun missingAudioIsReportedWithoutStoppingScriptExecution() = runTest {
        val session = factory(
            files = mapOf(
                "game.json" to validManifest,
                "scripts/main.avg" to "play_bgm \"theme\"\nsay \"Ready\"",
            ),
            audioPlayerFactory = AudioPlayerFactory { MissingAudioPlayer() },
        ).create()

        session.engine.dispatch(GameAction.Next)

        assertEquals("Ready", session.engine.viewState.value.dialogue?.text)
        assertEquals("theme", session.engine.viewState.value.debug?.bgm?.id)
        assertEquals(1, session.assetLoadMonitor.issues.value.size)
        assertEquals("theme", session.assetLoadMonitor.issues.value.single().assetId)
        assertEquals("audio/theme.mp3", session.assetLoadMonitor.issues.value.single().location)
    }

    @Test
    fun wrapsMissingManifestAndEntryScriptWithFileLocation() = runTest {
        val missingManifest = assertFailsWith<EngineException.ProjectLoadError> {
            factory(emptyMap()).create()
        }
        assertTrue(missingManifest.message.orEmpty().contains("project file: game.json"))
        assertIs<IllegalArgumentException>(missingManifest.cause)

        val missingScript = assertFailsWith<EngineException.ProjectLoadError> {
            factory(mapOf("game.json" to validManifest)).create()
        }
        assertTrue(missingScript.message.orEmpty().contains("project file: scripts/main.avg"))
        assertIs<IllegalArgumentException>(missingScript.cause)
    }

    @Test
    fun malformedManifestRetainsManifestLocationAndParserCause() = runTest {
        val error = assertFailsWith<EngineException.ProjectLoadError> {
            factory(mapOf("custom/game.json" to "not json")).create("custom/game.json")
        }

        assertTrue(error.message.orEmpty().contains("manifest: custom/game.json"))
        assertTrue(error.message.orEmpty().contains("malformed project manifest"))
        assertIs<EngineException.ScriptParseError>(error.cause)
    }

    @Test
    fun malformedEntryScriptRetainsProjectAndScriptContext() = runTest {
        val error = assertFailsWith<EngineException.ProjectLoadError> {
            factory(
                mapOf(
                    "game.json" to validManifest,
                    "scripts/main.avg" to "unknown_command \"value\"",
                ),
            ).create()
        }

        assertTrue(error.message.orEmpty().contains("project: sample"))
        assertTrue(error.message.orEmpty().contains("entry script: scripts/main.avg"))
        assertIs<EngineException.UnknownCommand>(error.cause)
    }

    @Test
    fun validatesProjectReferencesBeforeCreatingRuntimeServices() = runTest {
        var audioFactoryCalled = false
        val error = assertFailsWith<EngineException.ProjectLoadError> {
            factory(
                files = mapOf(
                    "game.json" to validManifest,
                    "scripts/main.avg" to "play_se \"missing\"\nsay \"Ready\"",
                ),
                audioPlayerFactory = AudioPlayerFactory {
                    audioFactoryCalled = true
                    RecordingAudioPlayer()
                },
            ).create()
        }

        assertTrue(error.message.orEmpty().contains("sound effect 'missing'"))
        assertEquals(false, audioFactoryCalled)
    }

    private fun factory(
        files: Map<String, String>,
        onCreateSaveManager: (GameProject) -> Unit = {},
        audioPlayerFactory: AudioPlayerFactory = AudioPlayerFactory { RecordingAudioPlayer() },
    ): GameSessionFactory = GameSessionFactory(
        source = MapGameProjectSource(files),
        saveManagerFactory = SaveManagerFactory { project ->
            onCreateSaveManager(project)
            JsonSaveManager(InMemorySaveStorage(), timestampProvider = { 1L })
        },
        audioPlayerFactory = audioPlayerFactory,
    )

    private class RecordingAudioPlayer : AudioPlayer {
        val bgmIds = mutableListOf<String>()
        var released = false

        override suspend fun playBgm(id: String, loop: Boolean) {
            bgmIds += id
        }
        override suspend fun stopBgm(fadeOutMillis: Long) = Unit
        override suspend fun playSe(id: String) = Unit
        override suspend fun playVoice(id: String) = Unit
        override suspend fun stopVoice() = Unit
        override fun release() {
            released = true
        }
    }

    private class MissingAudioPlayer : AudioPlayer {
        override suspend fun playBgm(id: String, loop: Boolean) {
            throw EngineException.AssetLoadError(
                assetType = "BGM",
                assetId = id,
                location = "audio/$id.mp3",
                message = "missing audio",
            )
        }

        override suspend fun stopBgm(fadeOutMillis: Long) = Unit
        override suspend fun playSe(id: String) = Unit
        override suspend fun playVoice(id: String) = Unit
        override suspend fun stopVoice() = Unit
    }

    private companion object {
        val validManifest = """
            {
              "id": "sample",
              "name": "Sample Game",
              "version": "1.0.0",
              "debuggable": true,
              "entryScript": "scripts/main.avg",
              "viewport": { "width": 1920, "height": 1080 },
              "backgrounds": { "school": "backgrounds/school.jpg" },
              "characters": {
                "yuki": {
                  "name": "Yuki",
                  "defaultExpression": "normal",
                  "expressions": {
                    "normal": "characters/yuki/normal.png",
                    "happy": "characters/yuki/happy.png"
                  }
                }
              },
              "audio": { "bgm": { "theme": "audio/theme.mp3" } }
            }
        """.trimIndent()
    }
}
