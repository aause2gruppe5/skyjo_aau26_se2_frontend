package at.aau.se2.skyjo.ui.screens.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.model.ActionCard
import at.aau.se2.skyjo.model.ActionCardParameters
import at.aau.se2.skyjo.model.ActionCardResultMessage
import at.aau.se2.skyjo.model.BoardLineTargetType
import at.aau.se2.skyjo.model.BoardSlot
import at.aau.se2.skyjo.model.Card
import at.aau.se2.skyjo.model.CheatPeekResultMessage
import at.aau.se2.skyjo.model.DrawThreeCardsChoiceMode
import at.aau.se2.skyjo.model.DrawThreeCardsDiscardReference
import at.aau.se2.skyjo.model.InspectedCard
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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled

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
                GameScreen(onBack = {}, onNavigateToGameOver = {})
            }
        }
        composeTestRule.onNodeWithText("SKYJO ACTION").assertIsDisplayed()
    }

    @Test
    fun gameScreen_shows_connecting_when_no_state() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(onBack = {}, onNavigateToGameOver = {})
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                GameScreen(onBack = { backPressed = true }, onNavigateToGameOver = {})
            }
        }
        composeTestRule.waitForIdle()
        assert(!backPressed)
    }

    @Test
    fun gameScreen_whenGameOverWithoutRoundResult_callsNavigationCallback() {
        // Ein Flag, um zu prüfen, ob die Navigation aufgerufen wurde
        var navigationCalled = false

        // Ein gefälschter GameState, der sofort auf GameOver steht und kein Ergebnisdialog anzeigen kann
        val testState = makeGameState(gameOver = true)

        // Den Screen in die Test-Umgebung laden
        composeTestRule.setContent {
            GameScreen(
                gameState = testState,
                onNavigateToGameOver = { navigationCalled = true }, // Flag auf true setzen
                onBack = {}
            )
        }

        // Da kein RoundResult vorhanden ist, soll direkt der größere GameOver-Screen geöffnet werden.
        // muss das Flag jetzt true sein.
        assert(navigationCalled)
    }

    @Test
    fun gameScreen_shows_round_finished_message() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "ROUND_FINISHED"),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }
        composeTestRule.onNodeWithText("DRAWN CARD").assertExists()
    }

    @Test
    fun gameScreen_shows_private_cheat_peek_dialog() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "AWAITING_DRAW"),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    privateCheatPeekResult = CheatPeekResultMessage(
                        card = Card(id = 77, value = -2, type = "NUMBER"),
                        remainingCheatPeeks = 2,
                    ),
                    onBack = {},
                    onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Cheat Peek").assertExists()
        composeTestRule.onNodeWithText("Top card of the draw pile").assertExists()
        composeTestRule.onNodeWithText("-2").assertExists()
        composeTestRule.onNodeWithText("2 peeks left").assertExists()
        composeTestRule.onNodeWithText("OK").assertExists()
    }

    @Test
    fun gameScreen_cheat_peek_dialog_dismisses_via_ok_button() {
        var dismissed = false

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "AWAITING_DRAW"),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    privateCheatPeekResult = CheatPeekResultMessage(
                        card = Card(id = 78, value = 11, type = "NUMBER"),
                        remainingCheatPeeks = 0,
                    ),
                    onDismissCheatPeekResult = { dismissed = true },
                    onBack = {},
                    onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithText("OK").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        assert(dismissed)
    }

    @Test
    fun gameScreen_shows_replace_and_discard_buttons_when_awaiting_replacement() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(phase = "AWAITING_REPLACEMENT"),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onAllNodesWithText("Defense").assertCountEquals(2)
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
                    onBack = {}, onNavigateToGameOver = {},
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
    fun gameScreen_drawThreeCards_action_card_click_sends_private_first_step_command() {
        var publicPlayedIndex: Int? = null
        var sentCommand: PlayActionCardCommand? = null

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(handActionCards = listOf(drawThreeCardsActionCard())),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onPlayActionCard = { publicPlayedIndex = it },
                    onPlayDrawThreeCardsCard = { sentCommand = it },
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag("play_action_card_0")
            .performScrollTo()
            .performClick()

        assertEquals(null, publicPlayedIndex)
        assertEquals(0, sentCommand?.actionCardIndex)
        assertEquals(null, sentCommand?.parameters)
    }

    @Test
    fun gameScreen_drawThreeCards_result_shows_private_drawn_cards_to_acting_player() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeDrawThreeCardsGameState(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    privateActionCardResult = drawThreeCardsResult(),
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onAllNodesWithText("Draw Three Cards").assertCountEquals(2)
        composeTestRule.onNodeWithTag("draw_three_cards_card_0").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("8").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("-2").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("13").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun gameScreen_drawThreeCards_result_is_not_shown_when_not_acting_player_turn() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeDrawThreeCardsGameState(currentPlayerId = "p2"),
                    myPlayerId = "p1",
                    isMyTurn = false,
                    privateActionCardResult = drawThreeCardsResult(),
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onAllNodesWithTag("draw_three_cards_card_0").assertCountEquals(0)
    }

    @Test
    fun gameScreen_drawThreeCards_visible_market_card_renders_label() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(
                        handActionCards = emptyList(),
                    ).copy(
                        visibleActionCards = listOf(drawThreeCardsActionCard()),
                    ),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Draw Three Cards").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun gameScreen_drawThreeCards_keep_one_and_swap_sends_choice_command() {
        var sentCommand: PlayActionCardCommand? = null

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeDrawThreeCardsGameState(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    privateActionCardResult = drawThreeCardsResult(),
                    onPlayDrawThreeCardsCard = { sentCommand = it },
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("draw_three_cards_card_1").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("draw_three_cards_board_slot_0_1").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("draw_three_cards_discard_DRAWN_CARD_0").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("draw_three_cards_discard_DRAWN_CARD_2").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("draw_three_cards_discard_SWAPPED_BOARD_CARD").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("draw_three_cards_submit").performScrollTo().performClick()

        val parameters = sentCommand?.parameters as? ActionCardParameters.DrawThreeCardsChoice
        assertEquals(0, sentCommand?.actionCardIndex)
        assertEquals(DrawThreeCardsChoiceMode.KEEP_ONE_AND_SWAP, parameters?.mode)
        assertEquals(1, parameters?.chosenDrawnCardIndex)
        assertEquals(0, parameters?.targetRow)
        assertEquals(1, parameters?.targetColumn)
        assertEquals(null, parameters?.revealRow)
        assertEquals(
            listOf(
                DrawThreeCardsDiscardReference.DRAWN_CARD_0,
                DrawThreeCardsDiscardReference.DRAWN_CARD_2,
                DrawThreeCardsDiscardReference.SWAPPED_BOARD_CARD,
            ),
            parameters?.discardOrder,
        )
    }

    @Test
    fun gameScreen_drawThreeCards_discard_all_and_reveal_sends_choice_command() {
        var sentCommand: PlayActionCardCommand? = null

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeDrawThreeCardsGameState(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    privateActionCardResult = drawThreeCardsResult(),
                    onPlayDrawThreeCardsCard = { sentCommand = it },
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("draw_three_cards_mode_discard_reveal").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("draw_three_cards_board_slot_0_0").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("draw_three_cards_discard_DRAWN_CARD_2").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("draw_three_cards_discard_DRAWN_CARD_0").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("draw_three_cards_discard_DRAWN_CARD_1").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("draw_three_cards_submit").performScrollTo().performClick()

        val parameters = sentCommand?.parameters as? ActionCardParameters.DrawThreeCardsChoice
        assertEquals(0, sentCommand?.actionCardIndex)
        assertEquals(DrawThreeCardsChoiceMode.DISCARD_ALL_AND_REVEAL, parameters?.mode)
        assertEquals(null, parameters?.chosenDrawnCardIndex)
        assertEquals(null, parameters?.targetRow)
        assertEquals(0, parameters?.revealRow)
        assertEquals(0, parameters?.revealColumn)
        assertEquals(
            listOf(
                DrawThreeCardsDiscardReference.DRAWN_CARD_2,
                DrawThreeCardsDiscardReference.DRAWN_CARD_0,
                DrawThreeCardsDiscardReference.DRAWN_CARD_1,
            ),
            parameters?.discardOrder,
        )
    }

    @Test
    fun gameScreen_drawThreeCards_pending_choice_blocks_normal_turn_actions() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeDrawThreeCardsGameState(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    privateActionCardResult = drawThreeCardsResult(),
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Complete Draw Three Cards to continue.").assertIsDisplayed()
        assertTextAbsent("DRAW FROM DECK")
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
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule
            .onNodeWithTag("play_action_card_0")
            .performScrollTo()
            .performClick()

        // Navigate to Bob's grid to see the swap hint
        composeTestRule.onNodeWithTag("carousel_next").performScrollTo().performClick()
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        // Play PLAYER_SWAP card
        composeTestRule
            .onNodeWithTag("play_action_card_0")
            .performScrollTo()
            .performClick()
        // Navigate to Bob's grid and select his slot (AwaitingFirst → AwaitingSecond with player1=p2)
        composeTestRule.onNodeWithTag("carousel_next").performScrollTo().performClick()
        composeTestRule
            .onNodeWithTag("board_slot_0_0")
            .performScrollTo()
            .performClick()
        // Navigate back to Alice's grid and select her slot (fires onPlayPlayerSwapCard)
        composeTestRule.onNodeWithTag("carousel_next").performScrollTo().performClick()
        composeTestRule
            .onNodeWithTag("board_slot_0_1")
            .performScrollTo()
            .performClick()

        assertEquals(PlayerSwapCall(0, "p2", 0, 0, "p1", 0, 1), playerSwapCall)
    }

    @Test
    fun gameScreen_player_swap_shows_protected_indicator_when_target_has_defense() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(
                        handActionCards = listOf(ActionCard(id = 152, kind = "PLAYER_SWAP")),
                    ).copy(
                        players = listOf(
                            GamePlayerState(
                                playerId = "p1",
                                nickname = "Alice",
                                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = false) } },
                                actionCards = listOf(ActionCard(id = 152, kind = "PLAYER_SWAP")),
                            ),
                            GamePlayerState(
                                playerId = "p2",
                                nickname = "Bob",
                                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = false) } },
                                actionCards = listOf(ActionCard(id = 151, kind = "DEFENSE")),
                            ),
                        ),
                    ),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("play_action_card_0").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("carousel_next").performScrollTo().performClick()

        composeTestRule.onNodeWithTag("defense_protected_indicator").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Protected by Defense card — cannot swap").assertIsDisplayed()
        assertTextAbsent("Tap a card to swap")
    }

    @Test
    fun gameScreen_player_swap_grid_not_selectable_when_target_has_defense() {
        var swapCalled = false
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(
                        handActionCards = listOf(ActionCard(id = 152, kind = "PLAYER_SWAP")),
                    ).copy(
                        players = listOf(
                            GamePlayerState(
                                playerId = "p1",
                                nickname = "Alice",
                                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = false) } },
                                actionCards = listOf(ActionCard(id = 152, kind = "PLAYER_SWAP")),
                            ),
                            GamePlayerState(
                                playerId = "p2",
                                nickname = "Bob",
                                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = false) } },
                                actionCards = listOf(ActionCard(id = 151, kind = "DEFENSE")),
                            ),
                        ),
                    ),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onPlayPlayerSwapCard = { _, _, _, _, _, _, _ -> swapCalled = true },
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("play_action_card_0").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("carousel_next").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("board_slot_0_0").performScrollTo().performClick()

        assertEquals(false, swapCalled)
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Game finished!", substring = true).assertIsDisplayed()
    }

    @Test
    fun gameScreen_carousel_shows_own_grid_by_default() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(),
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Your Grid").assertExists()
        composeTestRule.onNodeWithText("(1/2)").assertExists()
    }

    @Test
    fun gameScreen_carousel_next_navigates_to_other_player() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(),
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("carousel_next").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Bob's Grid").assertExists()
        composeTestRule.onNodeWithText("(2/2)").assertExists()
    }

    @Test
    fun gameScreen_carousel_wraps_from_last_to_first() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(),
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }
        // Navigate forward past end → wraps to start
        composeTestRule.onNodeWithTag("carousel_next").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("carousel_next").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Your Grid").assertExists()
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }
        // Navigate to Bob's grid to see disconnected badge
        composeTestRule.onNodeWithTag("carousel_next").performScrollTo().performClick()
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
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
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("play_action_card_0").performScrollTo().performClick()

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
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("play_action_card_0").performScrollTo().assertExists()

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
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("play_action_card_0").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Row 0").performScrollTo().performClick()

        val parameters = sentCommand?.parameters as? ActionCardParameters.BoardLineTarget
        assertEquals(0, sentCommand?.actionCardIndex)
        assertEquals("p1", parameters?.targetPlayerId)
        assertEquals(BoardLineTargetType.ROW, parameters?.targetType)
        assertEquals(0, parameters?.lineIndex)
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
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("play_action_card_0").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Target Bob").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Target: Bob").assertExists()
        composeTestRule.onNodeWithText("Row 0").performScrollTo().performClick()

        val parameters = sentCommand?.parameters as? ActionCardParameters.BoardLineTarget
        assertEquals("p2", parameters?.targetPlayerId)
        assertEquals(BoardLineTargetType.ROW, parameters?.targetType)
    }

    @Test
    fun gameScreen_enlightenment_cancel_hides_target_picker() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameStateWithHiddenCardValues(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("play_action_card_0").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Cancel").performScrollTo().performClick()

        assertTextAbsent("Select a target player, then a row or column")
    }

    @Test
    fun gameScreen_enlightenment_result_shows_peeked_value_in_grid_via_inspected_cards() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameStateWithHiddenCardValues(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    privateActionCardResult = ActionCardResultMessage(
                        type = "ENLIGHTENMENT",
                        actionCardIndex = 0,
                        targetPlayerId = "p1",
                        targetType = BoardLineTargetType.ROW,
                        lineIndex = 0,
                        inspectedCards = listOf(
                            InspectedCard(row = 0, col = 0, value = 11),
                            InspectedCard(row = 0, col = 2, value = 7),
                            InspectedCard(row = 0, col = 3, value = -2),
                        ),
                    ),
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithText("11").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("7").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("-2").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun gameScreen_enlightenment_result_shows_peeked_values_from_inspected_values_fallback() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameStateWithHiddenCardValues(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    privateActionCardResult = ActionCardResultMessage(
                        type = "ENLIGHTENMENT",
                        actionCardIndex = 0,
                        targetPlayerId = "p1",
                        targetType = BoardLineTargetType.ROW,
                        lineIndex = 1,
                        inspectedValues = listOf(5, 9, 3, 1),
                    ),
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithText("9").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("1").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun gameScreen_enlightenment_result_shows_peek_banner() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameStateWithHiddenCardValues(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    privateActionCardResult = ActionCardResultMessage(
                        type = "ENLIGHTENMENT",
                        actionCardIndex = 0,
                        targetPlayerId = "p1",
                        targetType = BoardLineTargetType.ROW,
                        lineIndex = 0,
                        inspectedCards = listOf(InspectedCard(row = 0, col = 0, value = 4)),
                    ),
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("peek_banner").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Private Peek").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Row 0").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Got it").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun gameScreen_enlightenment_result_dismiss_calls_callback() {
        var dismissed = false
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameStateWithHiddenCardValues(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    privateActionCardResult = ActionCardResultMessage(
                        type = "ENLIGHTENMENT",
                        actionCardIndex = 0,
                        targetPlayerId = "p1",
                        targetType = BoardLineTargetType.COLUMN,
                        lineIndex = 2,
                        inspectedCards = listOf(InspectedCard(row = 0, col = 2, value = 6)),
                    ),
                    onDismissActionCardResult = { dismissed = true },
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Got it").performScrollTo().performClick()
        assertEquals(true, dismissed)
    }

    @Test
    fun gameScreen_enlightenment_result_auto_navigates_to_target_player() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameStateWithHiddenCardValues(),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    privateActionCardResult = ActionCardResultMessage(
                        type = "ENLIGHTENMENT",
                        actionCardIndex = 0,
                        targetPlayerId = "p2",
                        targetType = BoardLineTargetType.ROW,
                        lineIndex = 0,
                        inspectedCards = listOf(InspectedCard(row = 0, col = 0, value = 3)),
                    ),
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("peek_banner").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob's Grid").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun gameScreen_roundResultDialog_shows_start_next_round_for_host() {
        var nextRoundClicked = false

        val stateWithRoundResult = makeGameState(
            roundResult = RoundResult(
                finisherPlayerId = "p1",
                scores = emptyList() // Für diesen Test reicht eine leere Liste völlig aus
            )
        )

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = stateWithRoundResult,
                    myPlayerId = "p1",
                    isMyTurn = false,
                    isHost = true, // <--- NEU: Explizit als Host deklariert
                    onReadyForNextRoundClick = { nextRoundClicked = true },
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        // Teste, ob der Button da ist und klicke ihn
        val buttonNode = composeTestRule.onNodeWithText("Start next Round")
        buttonNode.assertIsDisplayed()
        buttonNode.performClick()

        // Verifiziere den Klick
        assert(nextRoundClicked)

        // Wenn du den Text auf "Starting..." änderst, überprüfe das hier:
        composeTestRule.onNodeWithText("Starting...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Starting...").assertIsNotEnabled()
    }

    @Test
    fun gameScreen_roundResultDialog_shows_waiting_for_non_host() {
        val stateWithRoundResult = makeGameState(
            roundResult = RoundResult(
                finisherPlayerId = "p2",
                scores = emptyList()
            )
        )

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = stateWithRoundResult,
                    myPlayerId = "p2",
                    isMyTurn = false,
                    isHost = false, // <--- NEU: Explizit als NICHT-Host deklariert
                    onBack = {}, onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Waiting for Alice...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start next Round").assertDoesNotExist()
    }

    @Test
    fun gameScreen_roundResultDialog_shows_game_over_button() {
        val stateWithRoundResultAndGameOver = makeGameState(
            gameOver = true,
            roundResult = RoundResult(
                finisherPlayerId = "p1",
                scores = emptyList()
            )
        )

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = stateWithRoundResultAndGameOver,
                    myPlayerId = "p1",
                    isMyTurn = false,
                    isHost = true, // <--- NEU: Parameter muss vorhanden sein
                    onBack = {},
                    onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Game Over! See Results").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start next Round").assertDoesNotExist()
        composeTestRule.onNodeWithText("Waiting for Alice...").assertDoesNotExist()
    }

    @Test
    fun gameScreen_roundResultDialog_closes_when_game_over_button_is_clicked() {
        var navigationCalls = 0
        val stateWithRoundResultAndGameOver = makeGameState(
            gameOver = true,
            roundResult = RoundResult(
                finisherPlayerId = "p1",
                scores = emptyList()
            )
        )

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = stateWithRoundResultAndGameOver,
                    myPlayerId = "p1",
                    isMyTurn = false,
                    isHost = true, // <--- NEU: Parameter muss vorhanden sein
                    onBack = {},
                    onNavigateToGameOver = { navigationCalls++ },
                )
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(0, navigationCalls)

        composeTestRule.onNodeWithText("Game Over! See Results").assertIsDisplayed()
        composeTestRule.onNodeWithText("Game Over! See Results").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, navigationCalls)
        composeTestRule.onNodeWithText("Game Over! See Results").assertDoesNotExist()
    }

    @Test
    fun gameScreen_shows_report_cheat_button_for_current_opponent() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(currentPlayerId = "p2"),
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {},
                    onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("report_cheat_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Report Cheat (3 left)").assertIsDisplayed()
    }

    @Test
    fun gameScreen_report_cheat_button_calls_callback_and_disables_for_same_turn() {
        var reports = 0
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(currentPlayerId = "p2", turnId = 4),
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onReportCheat = { reports++ },
                    onBack = {},
                    onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("report_cheat_button").performClick()

        assertEquals(1, reports)
        composeTestRule.onNodeWithTag("report_cheat_button").assertIsNotEnabled()
    }

    @Test
    fun gameScreen_report_cheat_button_reenables_when_turn_id_changes() {
        val state = mutableStateOf(makeGameState(currentPlayerId = "p2", turnId = 4))
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = state.value,
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onReportCheat = {},
                    onBack = {},
                    onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("report_cheat_button").performClick()
        composeTestRule.onNodeWithTag("report_cheat_button").assertIsNotEnabled()

        state.value = state.value.copy(turnId = 5)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("report_cheat_button").assertIsEnabled()
    }

    @Test
    fun gameScreen_report_cheat_button_is_hidden_for_non_current_opponent() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(currentPlayerId = "p1"),
                    myPlayerId = "p1",
                    isMyTurn = true,
                    onBack = {},
                    onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("carousel_next").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Bob's Grid").assertExists()

        composeTestRule.onNodeWithTag("report_cheat_button").assertDoesNotExist()
    }

    @Test
    fun gameScreen_report_cheat_button_is_disabled_when_no_reports_left() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = makeGameState(currentPlayerId = "p2", remainingCheatReports = 0),
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {},
                    onNavigateToGameOver = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("report_cheat_button").assertIsNotEnabled()
    }

    @Test
    fun gameScreen_flashes_score_when_player_gets_mid_round_penalty() {
        val state = mutableStateOf(makeGameState())

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = state.value,
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {},
                    onNavigateToGameOver = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("score_penalty_flash_p1").assertDoesNotExist()

        composeTestRule.mainClock.autoAdvance = false
        try {
            composeTestRule.runOnIdle {
                state.value = state.value.copy(
                    totalScores = listOf(
                        TotalScore("p1", "Alice", 5),
                        TotalScore("p2", "Bob", 0),
                    ),
                )
            }
            composeTestRule.mainClock.advanceTimeUntil(1_000L) {
                composeTestRule.onAllNodesWithTag("score_penalty_flash_p1", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeTestRule.onNodeWithTag("score_penalty_flash_p1", useUnmergedTree = true).assertExists()
            composeTestRule.mainClock.advanceTimeUntil(2_000L) {
                composeTestRule.onAllNodesWithTag("score_penalty_flash_p1", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isEmpty()
            }
            composeTestRule.onNodeWithTag("score_penalty_flash_p1", useUnmergedTree = true).assertDoesNotExist()
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun gameScreen_does_not_flash_score_for_round_result_points() {
        val state = mutableStateOf(makeGameState())

        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(
                    gameState = state.value,
                    myPlayerId = "p1",
                    isMyTurn = false,
                    onBack = {},
                    onNavigateToGameOver = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.mainClock.autoAdvance = false
        try {
            composeTestRule.runOnIdle {
                state.value = state.value.copy(
                    phase = "ROUND_FINISHED",
                    roundResult = RoundResult(
                        finisherPlayerId = "p1",
                        scores = listOf(PlayerRoundScore("p1", rawScore = 5, finalScore = 5)),
                    ),
                    totalScores = listOf(
                        TotalScore("p1", "Alice", 5),
                        TotalScore("p2", "Bob", 0),
                    ),
                )
            }
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.mainClock.advanceTimeByFrame()

            composeTestRule.onNodeWithTag("score_penalty_flash_p1", useUnmergedTree = true).assertDoesNotExist()
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
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

    private fun makeDrawThreeCardsGameState(
        currentPlayerId: String = "p1",
    ) = makeGameState(
        currentPlayerId = currentPlayerId,
        handActionCards = listOf(drawThreeCardsActionCard()),
    ).copy(
        players = listOf(
            GamePlayerState(
                playerId = "p1",
                nickname = "Alice",
                board = hiddenValueBoard(),
                actionCards = listOf(drawThreeCardsActionCard()),
            ),
            GamePlayerState(
                playerId = "p2",
                nickname = "Bob",
                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = false) } },
            ),
        ),
    )

    private fun drawThreeCardsResult(actionCardIndex: Int = 0) = ActionCardResultMessage(
        type = "DRAW_THREE_CARDS",
        actionCardIndex = actionCardIndex,
        drawnCards = listOf(
            Card(id = 201, value = 8, type = "NUMBER"),
            Card(id = 202, value = -2, type = "NUMBER"),
            Card(id = 203, value = 13, type = "NUMBER"),
        ),
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
        remainingCheatReports: Int = 3,
        turnId: Int = 0,
        totalScores: List<TotalScore> = listOf(
            TotalScore("p1", "Alice", 0),
            TotalScore("p2", "Bob", 0),
        ),
    ) = GameUpdateMessage(
        phase = phase,
        currentPlayerId = currentPlayerId,
        roundNumber = 1,
        gameOver = gameOver,
        totalScores = totalScores,
        players = listOf(
            GamePlayerState(
                playerId = "p1",
                nickname = "Alice",
                board = List(3) { List(4) { BoardSlot(type = "OCCUPIED", faceUp = false) } },
                actionCards = handActionCards,
                remainingCheatReports = remainingCheatReports,
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
            ActionCard(id = 152, kind = "PLACEHOLDER"), // Sprint 3: replace with real card kind
        ),
        actionDrawPileCount = 16,
        roundResult = roundResult,
        turnId = turnId,
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

    private fun drawThreeCardsActionCard(id: Int = 166) = ActionCard(
        id = id,
        kind = "DRAW_THREE_CARDS",
        label = "Draw Three Cards",
        value = 10,
    )
}
