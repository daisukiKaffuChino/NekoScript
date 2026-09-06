package io.github.daisukikaffuchino.nekoscript.engine.logging

/** Host-provided logging boundary for Engine Core diagnostics. */
interface EngineLogger {
    fun debug(message: String)
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String, throwable: Throwable? = null)
}

/** Silent logger used unless a host explicitly configures diagnostics. */
object NoOpEngineLogger : EngineLogger {
    override fun debug(message: String) = Unit
    override fun info(message: String) = Unit
    override fun warn(message: String) = Unit
    override fun error(message: String, throwable: Throwable?) = Unit
}
