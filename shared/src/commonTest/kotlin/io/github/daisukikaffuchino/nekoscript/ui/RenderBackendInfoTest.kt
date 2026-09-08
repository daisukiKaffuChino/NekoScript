package io.github.daisukikaffuchino.nekoscript.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class RenderBackendInfoTest {
    @Test
    fun autoConfigurationIncludesUnknownActualBackend() {
        assertEquals("Auto → Unknown", RenderBackendInfo.Unknown.displayName)
    }

    @Test
    fun explicitBackendDoesNotRepeatConfiguration() {
        assertEquals("Direct3D", RenderBackendInfo("Direct3D", "Direct3D").displayName)
        assertEquals("Software", RenderBackendInfo("Software", "Software").displayName)
    }

    @Test
    fun configuredAndDetectedBackendsAreShownTogether() {
        assertEquals("Auto → Direct3D", RenderBackendInfo("Auto", "Direct3D").displayName)
    }
}
