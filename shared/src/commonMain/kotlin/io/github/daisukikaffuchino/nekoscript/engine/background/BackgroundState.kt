package io.github.daisukikaffuchino.nekoscript.engine.background

import kotlinx.serialization.Serializable

/**
 * Runtime background selection.
 *
 * @property backgroundId project-defined identifier resolved by an asset manager
 */
@Serializable
data class BackgroundState(
    val backgroundId: String,
) {
    init {
        require(backgroundId.isNotBlank()) { "Background id must not be blank." }
    }
}
