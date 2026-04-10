package at.aau.se2.skyjo.ui.screens.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
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
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_shows_game_preferences_section() {
        composeTestRule.setContent {
            SkyjoTheme {
                SettingsScreen(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("Game Preferences").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_shows_account_section() {
        composeTestRule.setContent {
            SkyjoTheme {
                SettingsScreen(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("Account").assertIsDisplayed()
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
