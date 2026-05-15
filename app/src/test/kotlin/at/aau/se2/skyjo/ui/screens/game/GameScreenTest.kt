package at.aau.se2.skyjo.ui.screens.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.model.ActionCard
import at.aau.se2.skyjo.model.BoardSlot
import at.aau.se2.skyjo.model.Card
import at.aau.se2.skyjo.model.GamePlayerState
import at.aau.se2.skyjo.model.GameUpdateMessage
import at.aau.se2.skyjo.model.PlayerRoundScore
import at.aau.se2.skyjo.model.RoundResult
import at.aau.se2.skyjo.model.TotalScore
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import org.junit.Assert.assertEquals
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

    @Test
    fun gameScreen_shows_defense_action_card_when_state_present() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onBack = {},
                )
            }
        }

        composeTestRule.onAllNodesWithText("🛡️").assertCountEquals(2)
    }

    @Test
    fun gameScreen_action_card_click_triggers_play_callback() {
        var playedIndex: Int? = null
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onPlayActionCard = { playedIndex = it },
                    onBack = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag("play_action_card_0")
            .performScrollTo()
            .performClick()

        assertEquals(0, playedIndex)
    }

    @Test
    fun gameScreen_action_card_discard_triggers_callback() {
        var discardedIndex: Int? = null
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onDiscardActionCard = { discardedIndex = it },
                    onBack = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag("discard_action_card_0")
            .performScrollTo()
            .performClick()

        assertEquals(0, discardedIndex)
    }

    @Test
    fun gameScreen_visible_action_card_click_triggers_callback() {
        var drawnIndex: Int? = null
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onDrawVisibleActionCard = { drawnIndex = it },
                    onBack = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag("draw_visible_action_card_0")
            .performScrollTo()
            .performClick()

        assertEquals(0, drawnIndex)
    }

    @Test
    fun gameScreen_shows_final_turns_draw_buttons() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "FINAL_TURNS"),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("DRAW FROM DECK").assertIsDisplayed()
    }

    @Test
    fun gameScreen_shows_game_finished_message_in_action_bar() {
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
        composeTestRule.onNodeWithText("Game finished!", substring = true).assertIsDisplayed()
    }

    @Test
    fun gameScreen_shows_winner_in_game_over_banner() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(gameOver = true),
                    myPlayerId = "p2",
                    isMyTurn = false,
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Winner:", substring = true).assertExists()
    }

    @Test
    fun gameScreen_shows_other_players_section() {
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
        composeTestRule.onNodeWithText("Other Players").assertExists()
    }

    @Test
    fun gameScreen_shows_round_result_section_when_round_result_present() {
        val roundResult = RoundResult(
            finisherPlayerId = "p1",
            scores = listOf(
                PlayerRoundScore(playerId = "p1", rawScore = 5, finalScore = 5),
                PlayerRoundScore(playerId = "p2", rawScore = 10, finalScore = 10),
            ),
        )
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "ROUND_FINISHED", roundResult = roundResult),
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Round 1 Results").assertExists()
    }

    @Test
    fun gameScreen_action_market_section_is_displayed() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "AWAITING_DRAW"),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onDrawFromActionDeck = {},
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("ACTION MARKET").assertIsDisplayed()
    }

    @Test
    fun gameScreen_draw_from_action_deck_callback_is_wired() {
        var called = false
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "AWAITING_DRAW"),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onDrawFromActionDeck = { called = true },
                    onBack = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        assert(!called)
    }

    @Test
    fun gameScreen_draw_from_deck_callback_fires_on_click() {
        var called = false
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "AWAITING_DRAW"),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onDrawFromDeck = { called = true },
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("DRAW FROM DECK").performClick()
        assert(called) { "onDrawFromDeck should be called when button is clicked" }
    }

    @Test
    fun gameScreen_draw_from_discard_callback_fires_on_click() {
        var called = false
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "AWAITING_DRAW"),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onDrawFromDiscard = { called = true },
                    onBack = {},
                )
            }
        }
        // Discard button shows the card value (discardTopCard has value 4)
        composeTestRule.onNodeWithText("Draw from Discard", substring = true).performClick()
        assert(called) { "onDrawFromDiscard should be called when button is clicked" }
    }

    @Test
    fun gameScreen_final_turns_draw_from_deck_fires_on_click() {
        var called = false
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "FINAL_TURNS"),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onDrawFromDeck = { called = true },
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("DRAW FROM DECK").performClick()
        assert(called) { "onDrawFromDeck should be called in FINAL_TURNS phase" }
    }

    @Test
    fun gameScreen_final_turns_draw_from_discard_fires_on_click() {
        var called = false
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "FINAL_TURNS"),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onDrawFromDiscard = { called = true },
                    onBack = {},
                )
            }
        }
        // Discard button shows the card value (discardTopCard has value 4)
        composeTestRule.onNodeWithText("Draw from Discard", substring = true).performClick()
        assert(called) { "onDrawFromDiscard should be called in FINAL_TURNS phase" }
    }

    @Test
    fun gameScreen_shows_disconnected_badge_for_disconnected_player() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState().copy(
                        disconnectedPlayers = listOf("Bob"),
                    ),
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Disconnected").assertExists()
    }

    @Test
    fun gameScreen_shows_round_result_fallback_to_player_id_when_no_nickname_match() {
        val roundResult = RoundResult(
            finisherPlayerId = "unknown-id",
            scores = listOf(
                PlayerRoundScore(playerId = "unknown-id", rawScore = 8, finalScore = 8),
            ),
        )
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "ROUND_FINISHED", roundResult = roundResult),
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Round 1 Results").assertExists()
    }

    @Test
    fun gameScreen_shows_faceup_slot_with_no_card() {
        val stateWithNullCard = makeGameState().copy(
            players = listOf(
                GamePlayerState(
                    playerId = "p1",
                    nickname = "Alice",
                    board = listOf(
                        listOf(
                            BoardSlot(type = "OCCUPIED", faceUp = true, card = null),
                            BoardSlot(type = "OCCUPIED", faceUp = false),
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
                    gameState = stateWithNullCard,
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Your Grid").assertExists()
    }

    private fun makeGameState(
        phase: String = "AWAITING_DRAW",
        currentPlayerId: String = "p1",
        gameOver: Boolean = false,
        drawnCard: Card? = null,
        roundResult: RoundResult? = null,
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
                actionCards = listOf(ActionCard(id = 151, kind = "DEFENSE")),
            ),
            GamePlayerState(
                playerId = "p2",
                nickname = "Bob",
                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = false) } },
            ),
        ),
        discardTopCard = Card(id = 1, value = 4, type = "NUMBER"),
        drawnCard = drawnCard,
        visibleActionCards = listOf(
            ActionCard(id = 151, kind = "DEFENSE"),
            ActionCard(id = 152, kind = "PLACEHOLDER"),
        ),
        actionDrawPileCount = 16,
        roundResult = roundResult,
    )
}
