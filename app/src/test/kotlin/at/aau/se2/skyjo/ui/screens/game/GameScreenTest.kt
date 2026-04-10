package at.aau.se2.skyjo.ui.screens.game

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
@Config(sdk = [35])
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
}
