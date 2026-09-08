package io.github.daisukikaffuchino.nekoscript.ui

import io.github.daisukikaffuchino.nekoscript.engine.save.RawFrame
import io.github.daisukikaffuchino.nekoscript.engine.save.SaveThumbnail
import io.github.daisukikaffuchino.nekoscript.engine.save.toSaveThumbnail

/** Captures the currently visible frame so save slots can persist a real thumbnail image. */
fun interface SaveFrameCapture {
    suspend fun capture(): RawFrame?
}

fun RawFrame.captureToSaveThumbnail(logicalPreview: SaveThumbnail? = null): SaveThumbnail? =
    toSaveThumbnail(logicalPreview)
