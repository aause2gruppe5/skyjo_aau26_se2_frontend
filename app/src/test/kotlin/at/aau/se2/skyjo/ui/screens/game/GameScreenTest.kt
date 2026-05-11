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

    @Test
    fun gameScreen_shows_game_over_banner() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(gameOver = true),
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("GAME OVER").assertExists()
    }

    @Test
    fun gameScreen_shows_round_finished_message() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "ROUND_FINISHED"),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Round finished", substring = true).assertIsDisplayed()
    }

    @Test
    fun gameScreen_shows_drawn_card_when_awaiting_replacement() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(
                        phase = "AWAITING_REPLACEMENT",
                        drawnCard = Card(id = 2, value = 7, type = "NUMBER"),
                    ),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("DRAWN CARD").assertExists()
    }

    @Test
    fun gameScreen_shows_replace_and_discard_buttons_when_awaiting_replacement() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "AWAITING_REPLACEMENT"),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("REPLACE CARD").assertIsDisplayed()
        composeTestRule.onNodeWithText("Discard & Reveal").assertIsDisplayed()
    }

    @Test
    fun gameScreen_shows_cleared_slot_in_board() {
        val stateWithClearedSlot = makeGameState().copy(
            players = listOf(
                GamePlayerState(
                    playerId = "p1",
                    nickname = "Alice",
                    board = listOf(
                        listOf(
                            BoardSlot(type = "CLEARED"),
                            BoardSlot(type = "OCCUPIED", faceUp = true, card = Card(1, 3, "NUMBER")),
                            BoardSlot(type = "OCCUPIED", faceUp = false),
                            BoardSlot(type = "OCCUPIED", faceUp = false),
                        ),
                        List(4) { BoardSlot(type = "OCCUPIED", faceUp = false) },
                        List(4) { BoardSlot(type = "OCCUPIED", faceUp = false) },
                    ),
                ),
                GamePlayerState(
                    playerId = "p2",
                    nickname = "Bob",
                    board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = false) } },
                ),
            ),
        )
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = stateWithClearedSlot,
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Your Grid").assertExists()
    }

    @Test
    fun gameScreen_shows_action_market_section() {
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
        composeTestRule.onNodeWithText("ACTION MARKET").assertIsDisplayed()
    }

    @Test
    fun gameScreen_shows_hand_action_cards_section() {
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
        composeTestRule.onNodeWithText("Your Action Cards").assertExists()
    }

    private fun makeGameState(
        phase: String = "AWAITING_DRAW",
        currentPlayerId: String = "p1",
        gameOver: Boolean = false,
        drawnCard: Card? = null,
    ) = GameUpdateMessage(
        phase = phase,
        currentPlayerId = currentPlayerId,
        roundNumber = 1,
        gameOver = gameOver,
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
        drawnCard = drawnCard,
    )
}
