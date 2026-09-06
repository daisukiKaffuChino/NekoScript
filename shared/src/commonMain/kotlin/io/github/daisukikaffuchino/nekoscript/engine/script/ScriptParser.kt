package io.github.daisukikaffuchino.nekoscript.engine.script

/** Converts script source text into an AST without executing it. */
fun interface ScriptParser {
    /**
     * Parses [source] using [scriptId] for diagnostics and state identity.
     *
     * @throws io.github.daisukikaffuchino.nekoscript.engine.error.EngineException.ScriptParseError
     * when the source is malformed
     * @throws io.github.daisukikaffuchino.nekoscript.engine.error.EngineException.UnknownCommand
     * when a command name is unsupported
     */
    fun parse(scriptId: String, source: String): Script
}
