package at.aau.se2.skyjo.ui.screens.lobby

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LobbyScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun lobbyScreen_renders_without_crash() {
        composeTestRule.setContent {
            SkyjoTheme {
                LobbyScreen(
                    onStartGame = {},
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("CURRENT LOBBY").assertIsDisplayed()
    }

    @Test
    fun lobbyScreen_shows_start_game_button() {
        composeTestRule.setContent {
            SkyjoTheme {
                LobbyScreen(
                    onStartGame = {},
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("START GAME").assertIsDisplayed()
    }

    @Test
    fun lobbyScreen_start_game_triggers_callback() {
        var started = false
        composeTestRule.setContent {
            SkyjoTheme {
                LobbyScreen(
                    onStartGame = { started = true },
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("START GAME").performClick()
        assert(started)
    }

    @Test
    fun lobbyScreen_back_callback_works() {
        var backPressed = false
        composeTestRule.setContent {
            SkyjoTheme {
                LobbyScreen(
                    onStartGame = {},
                    onBack = { backPressed = true },
                )
            }
        }
        composeTestRule.waitForIdle()
        assert(!backPressed)
    }
}
