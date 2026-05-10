package at.aau.se2.skyjo.ui.screens.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun gameScreen_shows_reveal_card_button() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(onBack = {})
            }
        }
        composeTestRule.onNodeWithText("REVEAL CARD").assertIsDisplayed()
    }

    @Test
    fun gameScreen_shows_action_market() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(onBack = {})
            }
        }
        composeTestRule.onNodeWithText("ACTION MARKET").assertIsDisplayed()
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
    fun erleuchtungPeekValues_returns_only_face_down_values_from_selected_row() {
        val grid = listOf(
            listOf(
                SkyjoCardState(4, isFaceUp = false),
                SkyjoCardState(2, isFaceUp = true),
                SkyjoCardState(-1, isFaceUp = false),
            ),
        )

        val values = erleuchtungPeekValues(
            grid = grid,
            selectionType = ErleuchtungSelectionType.ROW,
            index = 0,
        )

        assertEquals(listOf(4, -1), values)
    }

    @Test
    fun gameScreen_erleuchtung_shows_row_and_column_selection_controls() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(onBack = {})
            }
        }

        composeTestRule.onNodeWithText("Erleuchtung").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Select any row or column to peek at face-down cards")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Select Alice row 1")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Select Bob column 2")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun gameScreen_erleuchtung_selection_shows_private_peek_without_flipping_cards() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameScreen(onBack = {})
            }
        }

        composeTestRule.onNodeWithText("Erleuchtung").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Select Alice row 1")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithText("Alice Row 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("9").assertIsDisplayed()
        composeTestRule.onNodeWithText("-1").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Private peek only. These cards stay face-down.")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Done").performClick()
        composeTestRule.waitForIdle()

        assertTrue(composeTestRule.onAllNodesWithText("Alice Row 1").fetchSemanticsNodes().isEmpty())
        assertTrue(composeTestRule.onAllNodesWithText("9").fetchSemanticsNodes().isEmpty())
    }
}
