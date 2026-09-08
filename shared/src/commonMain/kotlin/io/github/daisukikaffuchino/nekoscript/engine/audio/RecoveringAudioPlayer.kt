package io.github.daisukikaffuchino.nekoscript.engine.audio

import io.github.daisukikaffuchino.nekoscript.engine.asset.AssetLoadMonitor
import io.github.daisukikaffuchino.nekoscript.engine.error.EngineException

/** Keeps script execution running when a declared audio file cannot be loaded. */
class RecoveringAudioPlayer(
    private val delegate: AudioPlayer,
    private val assetLoadMonitor: AssetLoadMonitor,
) : AudioPlayer {
    override suspend fun playBgm(id: String, loop: Boolean) = recoverAssetFailure {
        delegate.playBgm(id, loop)
    }

    override suspend fun stopBgm(fadeOutMillis: Long) = delegate.stopBgm(fadeOutMillis)

    override suspend fun playSe(id: String) = recoverAssetFailure {
        delegate.playSe(id)
    }

    override suspend fun playVoice(id: String) = recoverAssetFailure {
        delegate.playVoice(id)
    }

    override suspend fun stopVoice() = delegate.stopVoice()

    override fun release() = delegate.release()

    private suspend fun recoverAssetFailure(block: suspend () -> Unit) {
        try {
            block()
        } catch (error: EngineException.AssetLoadError) {
            assetLoadMonitor.report(error)
        }
    }
}
