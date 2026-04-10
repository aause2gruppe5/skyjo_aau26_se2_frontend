package at.aau.se2.skyjo.ui.screens.start

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.ui.navigation.AppDestination
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class StartScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun startScreen_renders_without_crash() {
        composeTestRule.setContent {
            SkyjoTheme {
                StartScreen(
                    onPlayClicked = {},
                    onNavigate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("PLAY").assertIsDisplayed()
    }

    @Test
    fun startScreen_shows_action_mode_label() {
        composeTestRule.setContent {
            SkyjoTheme {
                StartScreen(
                    onPlayClicked = {},
                    onNavigate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("ACTION MODE").assertIsDisplayed()
    }

    @Test
    fun startScreen_play_button_triggers_callback() {
        var clicked = false
        composeTestRule.setContent {
            SkyjoTheme {
                StartScreen(
                    onPlayClicked = { clicked = true },
                    onNavigate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("PLAY").performClick()
        assert(clicked)
    }

    @Test
    fun startScreen_navigate_callback_works() {
        var navigatedTo: AppDestination? = null
        composeTestRule.setContent {
            SkyjoTheme {
                StartScreen(
                    onPlayClicked = {},
                    onNavigate = { navigatedTo = it },
                )
            }
        }
        composeTestRule.waitForIdle()
        assert(navigatedTo == null)
    }
}
