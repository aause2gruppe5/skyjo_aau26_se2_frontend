package at.aau.se2.skyjo.ui.screens.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_renders_without_crash() {
        composeTestRule.setContent {
            SkyjoTheme {
                SettingsScreen(onNavigate = {})
            }
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }

    // "Settings" also appears in the bottom nav tab and drawer item, so use onAllNodesWithText
    @Test
    fun settingsScreen_shows_settings_title() {
        composeTestRule.setContent {
            SkyjoTheme {
                SettingsScreen(onNavigate = {})
            }
        }
        composeTestRule.onAllNodesWithText("Settings")[0].assertExists()
    }

    // "AcePlayer" also appears in the always-composed drawer header, so use onAllNodesWithText
    @Test
    fun settingsScreen_shows_player_name() {
        composeTestRule.setContent {
            SkyjoTheme {
                SettingsScreen(onNavigate = {})
            }
        }
        composeTestRule.onAllNodesWithText("AcePlayer")[0].assertExists()
    }

    @Test
    fun settingsScreen_shows_player_level() {
        composeTestRule.setContent {
            SkyjoTheme {
                SettingsScreen(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("Level 42").assertIsDisplayed()
    }

    // Items below the fold in the scrollable column use assertExists() instead of assertIsDisplayed()
    @Test
    fun settingsScreen_shows_game_preferences_section() {
        composeTestRule.setContent {
            SkyjoTheme {
                SettingsScreen(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("Game Preferences").assertExists()
    }

    @Test
    fun settingsScreen_shows_sound_fx_toggle() {
        composeTestRule.setContent {
            SkyjoTheme {
                SettingsScreen(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("Sound FX").assertExists()
    }

    @Test
    fun settingsScreen_shows_music_toggle() {
        composeTestRule.setContent {
            SkyjoTheme {
                SettingsScreen(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("Music").assertExists()
    }

    @Test
    fun settingsScreen_shows_haptic_feedback_toggle() {
        composeTestRule.setContent {
            SkyjoTheme {
                SettingsScreen(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("Haptic Feedback").assertExists()
    }

    @Test
    fun settingsScreen_shows_account_section() {
        composeTestRule.setContent {
            SkyjoTheme {
                SettingsScreen(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("Account").assertExists()
    }

    @Test
    fun settingsScreen_shows_link_account_option() {
        composeTestRule.setContent {
            SkyjoTheme {
                SettingsScreen(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("Link Account").assertExists()
    }

    @Test
    fun settingsScreen_shows_app_version() {
        composeTestRule.setContent {
            SkyjoTheme {
                SettingsScreen(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("Skyjo Action v1.0.0").assertExists()
    }

    @Test
    fun settingsScreen_navigate_callback_not_triggered_on_render() {
        var navigated = false
        composeTestRule.setContent {
            SkyjoTheme {
                SettingsScreen(onNavigate = { navigated = true })
            }
        }
        composeTestRule.waitForIdle()
        assert(!navigated)
    }
}
