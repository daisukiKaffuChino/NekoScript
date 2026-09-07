package io.github.daisukikaffuchino.nekoscript

import io.github.daisukikaffuchino.nekoscript.engine.project.validateProjectLocation

/** Resolves a project-relative location inside a packaged Compose resource root. */
internal fun composeResourcePath(root: String, location: String): String {
    val normalized = validateProjectLocation(location)
    return listOf(root.trim('/'), normalized.trim('/'))
        .filter(String::isNotEmpty)
        .joinToString("/")
}
