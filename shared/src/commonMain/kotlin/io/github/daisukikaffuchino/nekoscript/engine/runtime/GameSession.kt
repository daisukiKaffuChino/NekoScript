package io.github.daisukikaffuchino.nekoscript.engine.runtime

import io.github.daisukikaffuchino.nekoscript.engine.asset.AssetManager
import io.github.daisukikaffuchino.nekoscript.engine.asset.AssetLoadMonitor
import io.github.daisukikaffuchino.nekoscript.engine.asset.ManifestAssetManager
import io.github.daisukikaffuchino.nekoscript.engine.audio.AudioPlayer
import io.github.daisukikaffuchino.nekoscript.engine.audio.NoOpAudioPlayer
import io.github.daisukikaffuchino.nekoscript.engine.audio.RecoveringAudioPlayer
import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import io.github.daisukikaffuchino.nekoscript.engine.logging.EngineLogger
import io.github.daisukikaffuchino.nekoscript.engine.logging.NoOpEngineLogger
import io.github.daisukikaffuchino.nekoscript.engine.project.GameProject
import io.github.daisukikaffuchino.nekoscript.engine.project.GameProjectParser
import io.github.daisukikaffuchino.nekoscript.engine.project.GameProjectSource
import io.github.daisukikaffuchino.nekoscript.engine.project.GameProjectValidator
import io.github.daisukikaffuchino.nekoscript.engine.interaction.HotspotRegistry
import io.github.daisukikaffuchino.nekoscript.engine.interaction.RectHotspot
import io.github.daisukikaffuchino.nekoscript.engine.viewport.LogicalRect
import io.github.daisukikaffuchino.nekoscript.engine.save.SaveManager
import io.github.daisukikaffuchino.nekoscript.engine.script.AvgScriptParser
import io.github.daisukikaffuchino.nekoscript.engine.script.DefaultScriptRuntime
import io.github.daisukikaffuchino.nekoscript.engine.script.Script
import io.github.daisukikaffuchino.nekoscript.engine.script.ScriptParser
import kotlinx.coroutines.CancellationException

/** Fully assembled, project-independent game runtime and its asset resolver. */
data class GameSession(
    val project: GameProject,
    val script: Script,
    val engine: GameEngine,
    val assetManager: AssetManager,
    val assetLoadMonitor: AssetLoadMonitor,
    val hotspotRegistry: HotspotRegistry,
    private val audioPlayer: AudioPlayer,
) {
    /** Releases runtime services owned by this session. */
    fun release() = audioPlayer.release()
}

/** Creates a [SaveManager] scoped to one [GameProject]. */
fun interface SaveManagerFactory {
    fun create(project: GameProject): SaveManager
}

/** Creates an [AudioPlayer] capable of resolving one project's audio identifiers. */
fun interface AudioPlayerFactory {
    fun create(project: GameProject): AudioPlayer
}

/**
 * Assembles a game session from a portable manifest and its entry script.
 *
 * Platform file, audio, and persistence capabilities enter through interfaces;
 * the resulting runtime remains entirely in common Kotlin.
 */
class GameSessionFactory(
    private val source: GameProjectSource,
    private val saveManagerFactory: SaveManagerFactory,
    private val audioPlayerFactory: AudioPlayerFactory = AudioPlayerFactory { NoOpAudioPlayer },
    private val logger: EngineLogger = NoOpEngineLogger,
    private val projectParser: GameProjectParser = GameProjectParser(),
    private val scriptParser: ScriptParser = AvgScriptParser(),
    private val projectValidator: GameProjectValidator = GameProjectValidator(),
) {
    /** Loads [manifestLocation] and creates a ready, not-yet-started session. */
    suspend fun create(manifestLocation: String = DEFAULT_MANIFEST_LOCATION): GameSession {
        require(manifestLocation.isNotBlank()) { "Manifest location must not be blank." }
        val manifestText = readProjectText(manifestLocation)
        val project = try {
            projectParser.parse(manifestText)
        } catch (error: Exception) {
            throw EngineException.ProjectLoadError("manifest: $manifestLocation: ${error.message}", error)
        }
        val scriptText = readProjectText(project.entryScript)
        val script = try {
            scriptParser.parse(project.entryScript, scriptText)
        } catch (error: Exception) {
            throw EngineException.ProjectLoadError(
                "project: ${project.id}, entry script: ${project.entryScript}: ${error.message}",
                error,
            )
        }
        projectValidator.validate(project, script)
        val assetManager = ManifestAssetManager(project)
        val assetLoadMonitor = AssetLoadMonitor()
        val audioPlayer = RecoveringAudioPlayer(
            delegate = audioPlayerFactory.create(project),
            assetLoadMonitor = assetLoadMonitor,
        )
        val runtime = DefaultScriptRuntime(
            script = script,
            audioPlayer = audioPlayer,
            logger = logger,
        )
        val hotspotRegistry = HotspotRegistry(project.hotspots.map { (id, definition) ->
            RectHotspot(
                id = id,
                bounds = LogicalRect(
                    x = definition.x,
                    y = definition.y,
                    width = definition.width,
                    height = definition.height,
                ),
            )
        })
        return GameSession(
            project = project,
            script = script,
            engine = GameEngine(
                runtime = runtime,
                saveManager = saveManagerFactory.create(project),
                project = project,
                hotspotBindings = project.hotspots,
            ),
            assetManager = assetManager,
            assetLoadMonitor = assetLoadMonitor,
            hotspotRegistry = hotspotRegistry,
            audioPlayer = audioPlayer,
        )
    }

    private suspend fun readProjectText(location: String): String = try {
        source.readText(location)
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        throw EngineException.ProjectLoadError("project file: $location: failed to read", error)
    }

    companion object {
        const val DEFAULT_MANIFEST_LOCATION: String = "game.json"
    }
}
