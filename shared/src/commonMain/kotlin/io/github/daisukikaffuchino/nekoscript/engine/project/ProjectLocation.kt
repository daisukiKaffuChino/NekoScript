package io.github.daisukikaffuchino.nekoscript.engine.project

/** Validates and normalizes a location relative to a game project's root. */
internal fun validateProjectLocation(location: String): String {
    val normalized = location.replace('\\', '/')
    require(normalized.isNotBlank()) { "Project file location must not be blank." }
    require(!normalized.startsWith('/') && !normalized.contains("://") && !normalized.contains(':')) {
        "Project file location must be relative."
    }
    require(normalized.split('/').none { it == ".." }) {
        "Project file location must not escape the project root."
    }
    return normalized
}
