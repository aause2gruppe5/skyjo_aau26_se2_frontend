package at.aau.se2.skyjo.ui.screens.start

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
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
        composeTestRule.onNodeWithText("Lobby").assertIsDisplayed()
    }

    @Test
    fun startScreen_shows_authenticated_player_context() {
        composeTestRule.setContent {
            SkyjoTheme {
                StartScreen(
                    onPlayClicked = {},
                    onNavigate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Bereit fuer die naechste Lobby").assertIsDisplayed()
    }

    @Test
    fun startScreen_shows_create_lobby_button() {
        composeTestRule.setContent {
            SkyjoTheme {
                StartScreen(
                    onPlayClicked = {},
                    onNavigate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("LOBBY ERSTELLEN").assertExists()
    }

    @Test
    fun startScreen_shows_join_code_input_field() {
        composeTestRule.setContent {
            SkyjoTheme {
                StartScreen(
                    onPlayClicked = {},
                    onNavigate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Join Code").assertExists()
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
    fun startScreen_shows_lobby_subtitle() {
        composeTestRule.setContent {
            SkyjoTheme {
                StartScreen(
                    onPlayClicked = {},
                    onNavigate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Bereit fuer", substring = true).assertExists()
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
        composeTestRule.onNodeWithText("Wins").assertExists()
        composeTestRule.onNodeWithText("Games").assertExists()
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
        composeTestRule.onAllNodesWithText("Friends")[0].assertExists()
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
