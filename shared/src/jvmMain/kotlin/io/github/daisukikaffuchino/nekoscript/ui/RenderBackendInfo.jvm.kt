package io.github.daisukikaffuchino.nekoscript.ui

/**
 * Skiko exposes the requested API through this system property. When the host is set to Auto,
 * the actual API is not exposed through a stable Compose Desktop API, so we deliberately report
 * Unknown instead of guessing from the operating system.
 */
actual fun platformRenderBackendInfo(): RenderBackendInfo {
    val configured = normalizeRenderApi(System.getProperty("skiko.renderApi"))
    val actual = if (configured == "Auto") "Unknown" else configured
    return RenderBackendInfo(configured = configured, actual = actual)
}

private fun normalizeRenderApi(value: String?): String = when (value?.trim()?.uppercase()) {
    "D3D", "DIRECT3D" -> "Direct3D"
    "OPENGL" -> "OpenGL"
    "SOFTWARE" -> "Software"
    "METAL" -> "Metal"
    "VULKAN" -> "Vulkan"
    else -> "Auto"
}
