package at.aau.se2.skyjo.ui.screens.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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

    @Test
    fun settingsScreen_shows_player_name() {
        composeTestRule.setContent {
            SkyjoTheme {
                SettingsScreen(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("AcePlayer").assertIsDisplayed()
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
