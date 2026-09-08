package io.github.daisukikaffuchino.nekoscript

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.daisukikaffuchino.nekoscript.engine.save.JvmFileSaveStorage
import io.github.daisukikaffuchino.nekoscript.ui.rememberDesktopSaveFrameCapture
import java.nio.file.Path

fun main() = application {
    val saveStorage = JvmFileSaveStorage(
        Path.of(System.getProperty("user.home"), ".nekoscript", "saves"),
    )
    val windowState = rememberWindowState(size = DpSize(1280.dp, 720.dp))
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "NekoScript",
        resizable = true,
    ) {
        App(
            saveStorage = saveStorage,
            timestampProvider = System::currentTimeMillis,
            frameCapture = rememberDesktopSaveFrameCapture(),
        )
    }
}
