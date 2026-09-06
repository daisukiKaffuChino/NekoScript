package io.github.daisukikaffuchino.nekoscript

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.daisukikaffuchino.nekoscript.engine.save.JvmFileSaveStorage
import java.nio.file.Path

fun main() = application {
    val saveStorage = JvmFileSaveStorage(
        Path.of(System.getProperty("user.home"), ".nekoscript", "saves"),
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "NekoScript",
    ) {
        App(
            saveStorage = saveStorage,
            timestampProvider = System::currentTimeMillis,
        )
    }
}
