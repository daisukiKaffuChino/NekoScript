package io.github.daisukikaffuchino.nekoscript.engine.error

/** Base type for failures reported by the engine core. */
sealed class EngineException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** Reports malformed script input with source context in [message]. */
    class ScriptParseError(message: String) : EngineException(message)

    /** Reports a command name unsupported by the active parser. */
    class UnknownCommand(message: String) : EngineException(message)

    /** Reports a jump to a label absent from the active script. */
    class UnknownLabel(message: String) : EngineException(message)

    /** Reports a variable value or operation that is invalid for its type. */
    class InvalidVariable(message: String) : EngineException(message)

    /** Reports a player action that is invalid for the current runtime state. */
    class InvalidAction(message: String) : EngineException(message)

    /** Reports malformed, unsupported, or inaccessible save data. */
    class SaveError(message: String, cause: Throwable? = null) : EngineException(message, cause)

    /** Reports a game project manifest or source that cannot be loaded. */
    class ProjectLoadError(message: String, cause: Throwable? = null) : EngineException(message, cause)

    /** Reports a project asset that could not be resolved. */
    class AssetNotFound(message: String) : EngineException(message)

    /** Reports a project asset that resolved but could not be loaded or decoded. */
    class AssetLoadError(
        val assetType: String,
        val assetId: String,
        val location: String,
        message: String,
        cause: Throwable? = null,
    ) : EngineException(message, cause)
}
