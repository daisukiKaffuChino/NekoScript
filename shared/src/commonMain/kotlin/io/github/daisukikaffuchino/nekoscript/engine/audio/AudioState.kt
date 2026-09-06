package io.github.daisukikaffuchino.nekoscript.engine.audio

import kotlinx.serialization.Serializable

/**
 * Persistent logical audio state. One-shot sound effects are intentionally excluded.
 *
 * @property bgmId active project-defined BGM identifier
 * @property bgmLoop whether active BGM repeats
 * @property voiceId active project-defined voice identifier
 */
@Serializable
data class AudioState(
    val bgmId: String? = null,
    val bgmLoop: Boolean = true,
    val voiceId: String? = null,
)
