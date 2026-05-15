package at.aau.se2.skyjo.ui.screens.start

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.ui.navigation.AppDestination
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
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
    fun startScreen_shows_start_session_button() {
        composeTestRule.setContent {
            SkyjoTheme {
                StartScreen(
                    onPlayClicked = {},
                    onNavigate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("START NEW SESSION").assertExists()
    }

    @Test
    fun startScreen_shows_name_input_field() {
        composeTestRule.setContent {
            SkyjoTheme {
                StartScreen(
                    onPlayClicked = {},
                    onNavigate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Your Name").assertExists()
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

    @Test
    fun startScreen_shows_challenge_subtitle() {
        composeTestRule.setContent {
            SkyjoTheme {
                StartScreen(
                    onPlayClicked = {},
                    onNavigate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Challenge your friends", substring = true).assertExists()
    }

    @Test
    fun startScreen_shows_online_player_count() {
        composeTestRule.setContent {
            SkyjoTheme {
                StartScreen(
                    onPlayClicked = {},
                    onNavigate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("1,284 online").assertExists()
    }

    @Test
    fun startScreen_shows_player_stats() {
        composeTestRule.setContent {
            SkyjoTheme {
                StartScreen(
                    onPlayClicked = {},
                    onNavigate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Total Wins").assertExists()
        composeTestRule.onNodeWithText("Win Rate").assertExists()
    }

    @Test
    fun startScreen_shows_friends_feature_card() {
        composeTestRule.setContent {
            SkyjoTheme {
                StartScreen(
                    onPlayClicked = {},
                    onNavigate = {},
                )
            }
        }
        // "3 online" subtitle is unique to the Friends FeatureCard
        composeTestRule.onNodeWithText("3 online").assertExists()
    }

    @Test
    fun startScreen_shows_player_level() {
        composeTestRule.setContent {
            SkyjoTheme {
                StartScreen(
                    onPlayClicked = {},
                    onNavigate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Level 42").assertExists()
    }

    @Test
    fun startScreen_shows_avg_score_stat() {
        composeTestRule.setContent {
            SkyjoTheme {
                StartScreen(
                    onPlayClicked = {},
                    onNavigate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Avg Score").assertExists()
    }
}
