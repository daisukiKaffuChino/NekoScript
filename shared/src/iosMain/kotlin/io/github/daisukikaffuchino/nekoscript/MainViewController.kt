package io.github.daisukikaffuchino.nekoscript

import androidx.compose.ui.window.ComposeUIViewController
import io.github.daisukikaffuchino.nekoscript.engine.save.IosUserDefaultsSaveStorage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.gettimeofday
import platform.posix.timeval

@OptIn(ExperimentalForeignApi::class)
fun MainViewController() = ComposeUIViewController {
    App(
        saveStorage = IosUserDefaultsSaveStorage(),
        timestampProvider = {
            memScoped {
                val time = alloc<timeval>()
                gettimeofday(time.ptr, null)
                time.tv_sec * 1_000L + time.tv_usec / 1_000L
            }
        },
    )
}
