package io.github.daisukikaffuchino.nekoscript.ui

import io.github.daisukikaffuchino.nekoscript.engine.save.RawFrame
import java.awt.KeyboardFocusManager
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Window

fun rememberDesktopSaveFrameCapture(): SaveFrameCapture = SaveFrameCapture {
    val window = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
        ?: Window.getWindows().firstOrNull { it.isVisible }
        ?: return@SaveFrameCapture null
    val bounds: Rectangle = window.bounds
    if (bounds.width <= 0 || bounds.height <= 0) return@SaveFrameCapture null
    val capture = Robot().createScreenCapture(bounds)
    val pixels = IntArray(bounds.width * bounds.height)
    capture.getRGB(0, 0, bounds.width, bounds.height, pixels, 0, bounds.width)
    RawFrame(bounds.width, bounds.height, pixels)
}
