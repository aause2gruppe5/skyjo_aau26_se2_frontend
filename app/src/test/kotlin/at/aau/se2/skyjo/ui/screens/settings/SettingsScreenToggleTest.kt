package at.aau.se2.skyjo.ui.screens.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.haptic.HapticController
import at.aau.se2.skyjo.haptic.LocalHaptic
import at.aau.se2.skyjo.settings.SettingsRepository
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SettingsScreenToggleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun toggles_invoke_their_callbacks_and_fire_haptics() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val settings = SettingsRepository.getInstance(context)
        val haptic = HapticController(context, settings)

        var soundChanged = false
        var musicChanged = false
        var hapticChanged = false

        composeTestRule.setContent {
            SkyjoTheme {
                CompositionLocalProvider(LocalHaptic provides haptic) {
                    SettingsScreen(
                        onNavigate = {},
                        musicEnabled = false,
                        soundEnabled = true,
                        hapticEnabled = true,
                        onMusicChange = { musicChanged = true },
                        onSoundChange = { soundChanged = true },
                        onHapticChange = { hapticChanged = true },
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("toggle_Sound FX").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("toggle_Music").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("toggle_Haptic Feedback").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertTrue(soundChanged)
        assertTrue(musicChanged)
        assertTrue(hapticChanged)
    }
}
