package io.github.daisukikaffuchino.nekoscript

import io.github.daisukikaffuchino.nekoscript.engine.project.GameProjectSource
import nekoscript.shared.generated.resources.Res

/** Reads project text files packaged in Compose Multiplatform resources. */
class ComposeResourceGameProjectSource(
    private val root: String = "files",
) : GameProjectSource {
    override suspend fun readText(location: String): String =
        Res.readBytes(composeResourcePath(root, location)).decodeToString()
}
