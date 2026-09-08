package io.github.daisukikaffuchino.nekoscript.ui

/**
 * Presentation-only information about the graphics renderer selected by the host.
 * Engine state and save data must not depend on this value.
 */
data class RenderBackendInfo(
    val configured: String,
    val actual: String,
) {
    val displayName: String
        get() = when {
            configured == "Software" -> "Software"
            configured == actual -> actual
            else -> "$configured → $actual"
        }

    companion object {
        val Unknown: RenderBackendInfo = RenderBackendInfo("Auto", "Unknown")
    }
}

expect fun platformRenderBackendInfo(): RenderBackendInfo
