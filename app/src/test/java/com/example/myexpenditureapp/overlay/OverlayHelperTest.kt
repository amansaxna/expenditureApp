package com.example.myexpenditureapp.overlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayHelperTest {

    @Test
    fun testOverlaySettingsDefaultsToTrue() {
        val defaultSetting = OverlayHelper.getOverlaySettingDefault()
        assertTrue(defaultSetting)
    }

    @Test
    fun testShouldLaunchOverlayLogic() {
        // When both permission and user setting are true
        assertTrue(OverlayHelper.shouldLaunchOverlay(canDrawOverlays = true, isUserSettingEnabled = true))
        // When permission is missing
        assertFalse(OverlayHelper.shouldLaunchOverlay(canDrawOverlays = false, isUserSettingEnabled = true))
        // When user setting is disabled
        assertFalse(OverlayHelper.shouldLaunchOverlay(canDrawOverlays = true, isUserSettingEnabled = false))
    }
}
