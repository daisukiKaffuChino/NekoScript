package io.github.daisukikaffuchino.nekoscript

/** Resolves a project-relative location inside a packaged Compose resource root. */
internal fun composeResourcePath(root: String, location: String): String {
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
