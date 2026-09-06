package io.github.daisukikaffuchino.nekoscript.engine.project

import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Parses and validates a portable `game.json` manifest. */
class GameProjectParser {
    private val json = Json { ignoreUnknownKeys = true }

    /** Parses [source], reporting manifest errors as engine exceptions. */
    fun parse(source: String): GameProject = try {
        json.decodeFromString(GameProject.serializer(), source)
    } catch (error: SerializationException) {
        throw EngineException.ScriptParseError("game.json: malformed project manifest: ${error.message}")
    } catch (error: IllegalArgumentException) {
        throw EngineException.ScriptParseError("game.json: invalid project manifest: ${error.message}")
    }
}
