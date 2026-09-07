package io.github.daisukikaffuchino.nekoscript.engine.audio

/** Platform audio boundary used by the script runtime. */
interface AudioPlayer {
    /** Starts background music identified by [id]. */
    suspend fun playBgm(id: String, loop: Boolean = true)

    /** Stops background music, optionally fading for [fadeOutMillis]. */
    suspend fun stopBgm(fadeOutMillis: Long = 0)

    /** Plays a one-shot sound effect identified by [id]. */
    suspend fun playSe(id: String)

    /** Starts a voice clip identified by [id]. */
    suspend fun playVoice(id: String)

    /** Stops the active voice clip. */
    suspend fun stopVoice()

    /** Releases channels and decoded resources owned by this player. */
    fun release() = Unit
}

/** Silent default used when a host does not configure audio playback. */
object NoOpAudioPlayer : AudioPlayer {
    override suspend fun playBgm(id: String, loop: Boolean) = Unit
    override suspend fun stopBgm(fadeOutMillis: Long) = Unit
    override suspend fun playSe(id: String) = Unit
    override suspend fun playVoice(id: String) = Unit
    override suspend fun stopVoice() = Unit
}
