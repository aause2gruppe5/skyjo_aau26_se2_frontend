package at.aau.se2.skyjo.ui.screens.leaderboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.model.stats.LeaderboardEntryDto
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LeaderboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun leaderboardScreen_shows_empty_state() {
        composeTestRule.setContent {
            SkyjoTheme {
                LeaderboardScreen(onNavigate = {})
            }
        }

        composeTestRule.onAllNodesWithText("Leaderboard")[0].assertIsDisplayed()
        composeTestRule.onNodeWithText("Noch keine Spiele im Leaderboard").assertIsDisplayed()
    }

    @Test
    fun leaderboardScreen_shows_entries() {
        composeTestRule.setContent {
            SkyjoTheme {
                LeaderboardScreen(
                    onNavigate = {},
                    entries = listOf(
                        LeaderboardEntryDto(
                            rank = 1,
                            userId = "user-1",
                            username = "Alice",
                            averageScore = 7.5,
                            wins = 3,
                            gamesPlayed = 4,
                            bestScore = 2,
                            totalScore = 30,
                        ),
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("#1 Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("4 Spiele - 3 Wins").assertIsDisplayed()
        composeTestRule.onNodeWithText("7.5").assertIsDisplayed()
    }
}
