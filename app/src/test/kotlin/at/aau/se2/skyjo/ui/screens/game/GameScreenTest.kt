package at.aau.se2.skyjo.ui.screens.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.model.ActionCard
import at.aau.se2.skyjo.model.ActionCardResultMessage
import at.aau.se2.skyjo.model.BoardSlot
import at.aau.se2.skyjo.model.BoardLineTargetType
import at.aau.se2.skyjo.model.Card
import at.aau.se2.skyjo.model.GamePlayerState
import at.aau.se2.skyjo.model.GameUpdateMessage
import at.aau.se2.skyjo.model.PlayActionCardCommand
import at.aau.se2.skyjo.model.PlayerRoundScore
import at.aau.se2.skyjo.model.RoundResult
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

    @Test
    fun gameScreen_shows_server_provided_action_cards_in_hand() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(
                        player1ActionCards = listOf(
                            enlightenmentActionCard(),
                            placeholderActionCard(),
                        ),
                    ),
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Enlightenment").performScrollTo().assertExists()
        composeTestRule.onNodeWithText("Placeholder").performScrollTo().assertExists()
    }

    @Test
    fun gameScreen_placeholder_action_card_plays_without_target_picker() {
        var sentCommand: PlayActionCardCommand? = null
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(player1ActionCards = listOf(placeholderActionCard())),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onPlayActionCard = { sentCommand = it },
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Placeholder").performScrollTo().performClick()

        assert(sentCommand?.actionCardIndex == 0)
        assert(sentCommand?.parameters == null)
        assertTextAbsent("Select a target player, then a row or column")
    }

    @Test
    fun gameScreen_visible_action_card_callback_is_wired() {
        var selectedIndex: Int? = null
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(
                        visibleActionCards = listOf(enlightenmentActionCard(id = 201)),
                    ),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onDrawVisibleActionCard = { selectedIndex = it },
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Enlightenment").performScrollTo().performClick()

        assert(selectedIndex == 0)
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
                    onPlayActionCard = { sentCommand = it },
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Enlightenment").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Row 0").performScrollTo().performClick()

        assert(sentCommand?.actionCardIndex == 0)
        assert(sentCommand?.parameters?.targetPlayerId == "p1")
        assert(sentCommand?.parameters?.targetType == BoardLineTargetType.ROW)
        assert(sentCommand?.parameters?.lineIndex == 0)
    }

    @Test
    fun gameScreen_selecting_column_zero_sends_enlightenment_column_target() {
        var sentCommand: PlayActionCardCommand? = null

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameStateWithHiddenCardValues(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onPlayActionCard = { sentCommand = it },
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Enlightenment").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Column 0").performScrollTo().performClick()

        assert(sentCommand?.actionCardIndex == 0)
        assert(sentCommand?.parameters?.targetPlayerId == "p1")
        assert(sentCommand?.parameters?.targetType == BoardLineTargetType.COLUMN)
        assert(sentCommand?.parameters?.lineIndex == 0)
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
                    onPlayActionCard = { sentCommand = it },
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Enlightenment").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Target Bob").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Target: Bob").assertExists()
        composeTestRule.onNodeWithText("Row 0").performScrollTo().performClick()

        assert(sentCommand?.actionCardIndex == 0)
        assert(sentCommand?.parameters?.targetPlayerId == "p2")
        assert(sentCommand?.parameters?.targetType == BoardLineTargetType.ROW)
        assert(sentCommand?.parameters?.lineIndex == 0)
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
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No action cards in hand").assertExists()
    }

    @Test
    fun gameScreen_hidden_shared_state_values_are_not_revealed_before_private_result() {
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

        assertTextAbsent("11")
        composeTestRule.onNodeWithText("3").assertExists()
    }

    @Test
    fun gameScreen_does_not_mutate_board_state_for_enlightenment() {
        val originalBoard = hiddenValueBoard()
        val stateWithHiddenCardValues = makeGameStateWithHiddenCardValues(originalBoard)

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = stateWithHiddenCardValues,
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onPlayActionCard = {},
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Enlightenment").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Row 0").performScrollTo().performClick()

        val currentBoard = stateWithHiddenCardValues.players.first { it.playerId == "p1" }.board
        assert(currentBoard == originalBoard)
        assert(currentBoard[0][0].faceUp == false)
        assert(currentBoard[0][0].card?.value == 11)
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
    ) = makeGameState(currentPlayerId = currentPlayerId).copy(
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

    private fun makeGameState(
        phase: String = "AWAITING_DRAW",
        currentPlayerId: String = "p1",
        gameOver: Boolean = false,
        drawnCard: Card? = null,
        roundResult: RoundResult? = null,
        player1ActionCards: List<ActionCard> = emptyList(),
        visibleActionCards: List<ActionCard> = emptyList(),
        actionDrawPileCount: Int = 16,
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
                actionCards = player1ActionCards,
            ),
            GamePlayerState(
                playerId = "p2",
                nickname = "Bob",
                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = false) } },
            ),
        ),
        discardTopCard = Card(id = 1, value = 4, type = "NUMBER"),
        drawnCard = drawnCard,
        visibleActionCards = visibleActionCards,
        actionDrawPileCount = actionDrawPileCount,
        roundResult = roundResult,
    )

    private fun enlightenmentActionCard(id: Int = 151) = ActionCard(
        id = id,
        kind = "ENLIGHTENMENT",
        label = "Enlightenment",
        value = 10,
    )

    private fun placeholderActionCard(id: Int = 152) = ActionCard(
        id = id,
        kind = "PLACEHOLDER",
        label = "Action",
        value = 10,
    )
}
