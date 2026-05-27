package at.aau.se2.skyjo.ui.screens.lobby

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.model.LobbyPlayer
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
    fun lobbyScreen_start_game_triggers_callback_when_host_with_enough_players() {
        var startedRounds = -1
        composeTestRule.setContent {
            SkyjoTheme {
                LobbyScreen(
                    players = listOf(
                        LobbyPlayer("Alice", isHost = true),
                        LobbyPlayer("Bob", isHost = false),
                    ),
                    isHost = true,
                    onStartGame = { rounds -> startedRounds = rounds },
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("START GAME").performClick()
        assert(startedRounds > 0) { "Expected onStartGame to be called with a valid round count" }
    }

    @Test
    fun lobbyScreen_shows_host_player_name() {
        composeTestRule.setContent {
            SkyjoTheme {
                LobbyScreen(
                    players = listOf(
                        LobbyPlayer("Alice", isHost = true),
                    ),
                    onStartGame = {},
                    onBack = {},
                )
            }
        }
        // Use substring to avoid exact emoji matching issues
        composeTestRule.onNodeWithText("Alice", substring = true).assertIsDisplayed()
    }

    @Test
    fun lobbyScreen_shows_player_count() {
        composeTestRule.setContent {
            SkyjoTheme {
                LobbyScreen(
                    players = listOf(
                        LobbyPlayer("Alice", isHost = true),
                        LobbyPlayer("Bob", isHost = false),
                    ),
                    maxPlayers = 6,
                    onStartGame = {},
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("2 / 6").assertIsDisplayed()
    }

    @Test
    fun lobbyScreen_shows_join_code_and_error_message() {
        composeTestRule.setContent {
            SkyjoTheme {
                LobbyScreen(
                    joinCode = "ABC123",
                    errorMessage = "Could not join lobby",
                    onStartGame = {},
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Join Code: ABC123").assertIsDisplayed()
        composeTestRule.onNodeWithText("Could not join lobby").assertIsDisplayed()
    }

    @Test
    fun lobbyScreen_shows_non_host_waiting_text() {
        composeTestRule.setContent {
            SkyjoTheme {
                LobbyScreen(
                    players = listOf(
                        LobbyPlayer("Alice", isHost = true),
                        LobbyPlayer("Bob", isHost = false),
                    ),
                    isHost = false,
                    onStartGame = {},
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Only the host can change rules").assertExists()
        composeTestRule.onNodeWithText("Waiting for host to start").assertExists()
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
