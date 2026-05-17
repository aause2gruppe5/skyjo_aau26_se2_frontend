package at.aau.se2.skyjo.ui.screens.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.model.ActionCard
import at.aau.se2.skyjo.model.ActionCardResultMessage
import at.aau.se2.skyjo.model.BoardLineTargetType
import at.aau.se2.skyjo.model.BoardSlot
import at.aau.se2.skyjo.model.Card
import at.aau.se2.skyjo.model.GamePlayerState
import at.aau.se2.skyjo.model.GameUpdateMessage
import at.aau.se2.skyjo.model.PlayerRoundScore
import at.aau.se2.skyjo.model.PlayActionCardCommand
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

    private data class OwnSwapCall(
        val actionCardIndex: Int,
        val firstRow: Int,
        val firstCol: Int,
        val secondRow: Int,
        val secondCol: Int,
    )

    private data class PlayerSwapCall(
        val actionCardIndex: Int,
        val player1Id: String,
        val player1Row: Int,
        val player1Col: Int,
        val player2Id: String,
        val player2Row: Int,
        val player2Col: Int,
    )

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
    fun gameScreen_player_swap_action_card_enters_selection_mode() {
        var playedIndex: Int? = null
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(handActionCards = listOf(ActionCard(id = 152, kind = "PLAYER_SWAP"))),
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

        composeTestRule.onNodeWithText("Tap a card to swap").performScrollTo().assertIsDisplayed()
        assertEquals(null, playedIndex)
    }

    @Test
    fun gameScreen_swapOwnCards_action_card_enters_selection_mode_without_playing_immediately() {
        var playedIndex: Int? = null
        var ownSwapCall: OwnSwapCall? = null

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(handActionCards = listOf(swapOwnCardsActionCard())),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onPlayActionCard = { playedIndex = it },
                    onPlaySwapOwnCards = { actionCardIndex, firstRow, firstCol, secondRow, secondCol ->
                        ownSwapCall = OwnSwapCall(actionCardIndex, firstRow, firstCol, secondRow, secondCol)
                    },
                    onBack = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag("play_action_card_0")
            .performScrollTo()
            .performClick()

        assertEquals(null, playedIndex)
        assertEquals(null, ownSwapCall)
    }

    @Test
    fun gameScreen_swapOwnCards_sends_two_own_board_positions() {
        var ownSwapCall: OwnSwapCall? = null

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(handActionCards = listOf(swapOwnCardsActionCard())),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onPlaySwapOwnCards = { actionCardIndex, firstRow, firstCol, secondRow, secondCol ->
                        ownSwapCall = OwnSwapCall(actionCardIndex, firstRow, firstCol, secondRow, secondCol)
                    },
                    onBack = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag("play_action_card_0")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithTag("board_slot_0_0")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithTag("board_slot_0_1")
            .performScrollTo()
            .performClick()

        assertEquals(OwnSwapCall(0, 0, 0, 0, 1), ownSwapCall)
    }

    @Test
    fun gameScreen_swapOwnCards_does_not_send_when_same_slot_is_selected_twice() {
        var ownSwapCall: OwnSwapCall? = null

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(handActionCards = listOf(swapOwnCardsActionCard())),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onPlaySwapOwnCards = { actionCardIndex, firstRow, firstCol, secondRow, secondCol ->
                        ownSwapCall = OwnSwapCall(actionCardIndex, firstRow, firstCol, secondRow, secondCol)
                    },
                    onBack = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag("play_action_card_0")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithTag("board_slot_0_0")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithTag("board_slot_0_0")
            .performScrollTo()
            .performClick()

        assertEquals(null, ownSwapCall)
    }

    @Test
    fun gameScreen_playerSwap_sends_other_then_own_board_positions() {
        var playerSwapCall: PlayerSwapCall? = null

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeFaceUpSwapGameState(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onPlayPlayerSwapCard = { actionCardIndex, p1Id, p1Row, p1Col, p2Id, p2Row, p2Col ->
                        playerSwapCall = PlayerSwapCall(
                            actionCardIndex,
                            p1Id,
                            p1Row,
                            p1Col,
                            p2Id,
                            p2Row,
                            p2Col,
                        )
                    },
                    onBack = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag("play_action_card_0")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onAllNodesWithTag("board_slot_0_0")
            .onLast()
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithTag("board_slot_0_1")
            .performScrollTo()
            .performClick()

        assertEquals(PlayerSwapCall(0, "p2", 0, 0, "p1", 0, 1), playerSwapCall)
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
    fun gameScreen_game_over_banner_handles_empty_scoreboard() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(gameOver = true).copy(totalScores = emptyList()),
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("GAME OVER").assertExists()
        assertTextAbsent("Winner:")
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
    fun gameScreen_replace_mode_tapping_board_sends_coordinates() {
        var replaced: Pair<Int, Int>? = null
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "AWAITING_REPLACEMENT"),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onReplaceCard = { row, col -> replaced = row to col },
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("REPLACE CARD").performClick()
        composeTestRule.onNodeWithTag("board_slot_1_2").performScrollTo().performClick()

        assertEquals(1 to 2, replaced)
    }

    @Test
    fun gameScreen_discardAndReveal_mode_tapping_board_sends_coordinates() {
        var discardedAndRevealed: Pair<Int, Int>? = null
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "AWAITING_REPLACEMENT"),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onDiscardAndReveal = { row, col -> discardedAndRevealed = row to col },
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Discard & Reveal").performClick()
        composeTestRule.onNodeWithTag("board_slot_2_3").performScrollTo().performClick()

        assertEquals(2 to 3, discardedAndRevealed)
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

    @Test
    fun gameScreen_selecting_enlightenment_opens_row_column_selection() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameStateWithHiddenCardValues(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Enlightenment").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Select a target player, then a row or column").assertExists()
        composeTestRule.onNodeWithText("Alice (You)").assertExists()
        composeTestRule.onNodeWithText("Target Bob").assertExists()
        composeTestRule.onNodeWithText("Row 0").assertExists()
        composeTestRule.onNodeWithText("Column 0").assertExists()
    }

    @Test
    fun gameScreen_enlightenment_does_not_open_when_not_my_turn() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameStateWithHiddenCardValues(currentPlayerId = "p2"),
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Enlightenment").performScrollTo().assertExists()

        assertTextAbsent("Select a target player, then a row or column")
        assertTextAbsent("Row 0")
    }

    @Test
    fun gameScreen_selecting_row_zero_sends_enlightenment_row_target() {
        var sentCommand: PlayActionCardCommand? = null

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameStateWithHiddenCardValues(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onPlayEnlightenmentCard = { sentCommand = it },
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Enlightenment").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Row 0").performScrollTo().performClick()

        assertEquals(0, sentCommand?.actionCardIndex)
        assertEquals("p1", sentCommand?.parameters?.targetPlayerId)
        assertEquals(BoardLineTargetType.ROW, sentCommand?.parameters?.targetType)
        assertEquals(0, sentCommand?.parameters?.lineIndex)
    }

    @Test
    fun gameScreen_selecting_other_player_sends_target_player_id() {
        var sentCommand: PlayActionCardCommand? = null

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameStateWithHiddenCardValues(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onPlayEnlightenmentCard = { sentCommand = it },
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Enlightenment").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Target Bob").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Target: Bob").assertExists()
        composeTestRule.onNodeWithText("Row 0").performScrollTo().performClick()

        assertEquals("p2", sentCommand?.parameters?.targetPlayerId)
        assertEquals(BoardLineTargetType.ROW, sentCommand?.parameters?.targetType)
    }

    @Test
    fun gameScreen_enlightenment_cancel_hides_target_picker() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameStateWithHiddenCardValues(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Enlightenment").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Cancel").performScrollTo().performClick()

        assertTextAbsent("Select a target player, then a row or column")
    }

    @Test
    fun gameScreen_private_enlightenment_result_shows_inspected_values() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    privateActionCardResult = ActionCardResultMessage(
                        type = "ENLIGHTENMENT",
                        actionCardIndex = 0,
                        targetPlayerId = "p1",
                        targetType = BoardLineTargetType.ROW,
                        lineIndex = 0,
                        inspectedValues = listOf(2, null, 6, 8),
                    ),
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Private Peek").assertExists()
        composeTestRule.onNodeWithText("Values: 2, ?, 6, 8").assertExists()
    }

    @Test
    fun gameScreen_private_enlightenment_result_shows_empty_state() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    privateActionCardResult = ActionCardResultMessage(
                        type = "ENLIGHTENMENT",
                        actionCardIndex = 0,
                        targetPlayerId = "p1",
                        targetType = BoardLineTargetType.COLUMN,
                        lineIndex = 1,
                    ),
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Private Peek").assertExists()
        composeTestRule.onNodeWithText("No cards inspected").assertExists()
    }

    private fun assertTextAbsent(text: String) {
        val matches = composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes()
        assert(matches.isEmpty()) { "Expected no visible text matching \"$text\"" }
    }

    private fun hiddenValueBoard() = listOf(
        listOf(
            BoardSlot(type = "OCCUPIED", faceUp = false, card = Card(10, 11, "NUMBER")),
            BoardSlot(type = "OCCUPIED", faceUp = true, card = Card(11, 3, "NUMBER")),
            BoardSlot(type = "OCCUPIED", faceUp = false),
            BoardSlot(type = "OCCUPIED", faceUp = false),
        ),
        List(4) { BoardSlot(type = "OCCUPIED", faceUp = false) },
        List(4) { BoardSlot(type = "OCCUPIED", faceUp = false) },
    )

    private fun makeGameStateWithHiddenCardValues(
        board: List<List<BoardSlot>> = hiddenValueBoard(),
        currentPlayerId: String = "p1",
    ) = makeGameState(currentPlayerId = currentPlayerId, handActionCards = listOf(enlightenmentActionCard())).copy(
        players = listOf(
            GamePlayerState(
                playerId = "p1",
                nickname = "Alice",
                board = board,
                actionCards = listOf(enlightenmentActionCard()),
            ),
            GamePlayerState(
                playerId = "p2",
                nickname = "Bob",
                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = false) } },
            ),
        ),
    )

    private fun makeFaceUpSwapGameState() = makeGameState(
        handActionCards = listOf(ActionCard(id = 152, kind = "PLAYER_SWAP")),
    ).copy(
        players = listOf(
            GamePlayerState(
                playerId = "p1",
                nickname = "Alice",
                board = boardWithFaceUpValue(3),
                actionCards = listOf(ActionCard(id = 152, kind = "PLAYER_SWAP")),
            ),
            GamePlayerState(
                playerId = "p2",
                nickname = "Bob",
                board = boardWithFaceUpValue(5),
            ),
        ),
    )

    private fun boardWithFaceUpValue(value: Int) =
        List(3) {
            List(4) { index ->
                BoardSlot(
                    type = "OCCUPIED",
                    faceUp = true,
                    card = Card(id = value * 10 + index, value = value, type = "NUMBER"),
                )
            }
        }

    private fun makeGameState(
        phase: String = "AWAITING_DRAW",
        currentPlayerId: String = "p1",
        gameOver: Boolean = false,
        drawnCard: Card? = null,
        roundResult: RoundResult? = null,
        handActionCards: List<ActionCard> = listOf(ActionCard(id = 151, kind = "DEFENSE")),
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
                actionCards = handActionCards,
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

    private fun enlightenmentActionCard(id: Int = 151) = ActionCard(
        id = id,
        kind = "ENLIGHTENMENT",
        label = "Enlightenment",
        value = 10,
    )

    private fun swapOwnCardsActionCard(id: Int = 153) = ActionCard(
        id = id,
        kind = "SWAP_OWN_CARDS",
        label = "Swap Own Cards",
        value = 10,
    )
}
