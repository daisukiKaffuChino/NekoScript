package io.github.daisukikaffuchino.nekoscript

import io.github.daisukikaffuchino.nekoscript.engine.project.GameProjectSource
import nekoscript.shared.generated.resources.Res

/** Reads project text files packaged in Compose Multiplatform resources. */
class ComposeResourceGameProjectSource(
    private val root: String = "files",
) : GameProjectSource {
    override suspend fun readText(location: String): String = Res.readBytes(resolvePath(location)).decodeToString()

    private fun resolvePath(location: String): String {
        val normalized = location.replace('\\', '/')
        require(normalized.isNotBlank()) { "Project file location must not be blank." }
        require(!normalized.startsWith('/') && !normalized.contains("://") && !normalized.contains(':')) {
            "Project file location must be relative."
        }
        require(normalized.split('/').none { it == ".." }) {
            "Project file location must not escape the project root."
        }
        return listOf(root.trim('/'), normalized.trim('/'))
            .filter(String::isNotEmpty)
            .joinToString("/")
    }
}
