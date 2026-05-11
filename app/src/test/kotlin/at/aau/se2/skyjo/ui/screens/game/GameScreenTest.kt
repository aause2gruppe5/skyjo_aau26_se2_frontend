package at.aau.se2.skyjo.ui.screens.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.model.BoardSlot
import at.aau.se2.skyjo.model.Card
import at.aau.se2.skyjo.model.GamePlayerState
import at.aau.se2.skyjo.model.GameUpdateMessage
import at.aau.se2.skyjo.model.TotalScore
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class GameScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun gameScreen_renders_without_crash() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(onBack = {})
            }
        }
        composeTestRule.onNodeWithText("SKYJO ACTION").assertIsDisplayed()
    }

    @Test
    fun gameScreen_shows_connecting_when_no_state() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(onBack = {})
            }
        }
        composeTestRule.onNodeWithText("Connecting...").assertIsDisplayed()
    }

    @Test
    fun gameScreen_shows_round_number_when_state_present() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(),
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Round 1").assertIsDisplayed()
    }

    @Test
    fun gameScreen_shows_my_grid_section_when_state_present() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(),
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Your Grid").assertExists()
    }

    @Test
    fun gameScreen_shows_draw_buttons_when_awaiting_draw_and_my_turn() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "AWAITING_DRAW"),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("DRAW FROM DECK").assertIsDisplayed()
    }

    @Test
    fun gameScreen_shows_waiting_when_not_my_turn() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "AWAITING_DRAW"),
                    myPlayerId = "p2",
                    isMyTurn = false,
                    onBack = {},
                )
            }
        }
        // Use substring to avoid ellipsis character encoding issues
        composeTestRule.onNodeWithText("Waiting for Alice", substring = true).assertIsDisplayed()
    }

    @Test
    fun gameScreen_back_callback_works() {
        var backPressed = false
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(onBack = { backPressed = true })
            }
        }
        composeTestRule.waitForIdle()
        assert(!backPressed)
    }

    private fun makeGameState(
        phase: String = "AWAITING_DRAW",
        currentPlayerId: String = "p1",
    ) = GameUpdateMessage(
        phase = phase,
        currentPlayerId = currentPlayerId,
        roundNumber = 1,
        gameOver = false,
        totalScores = listOf(
            TotalScore("p1", "Alice", 0),
            TotalScore("p2", "Bob", 0),
        ),
        players = listOf(
            GamePlayerState(
                playerId = "p1",
                nickname = "Alice",
                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = false) } },
            ),
            GamePlayerState(
                playerId = "p2",
                nickname = "Bob",
                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = false) } },
            ),
        ),
        discardTopCard = Card(id = 1, value = 4, type = "NUMBER"),
    )
}
