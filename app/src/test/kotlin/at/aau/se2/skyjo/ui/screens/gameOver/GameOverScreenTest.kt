package at.aau.se2.skyjo.ui.screens.gameOver

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import at.aau.se2.skyjo.ui.theme.*
import at.aau.se2.skyjo.model.*

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class GameOverScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Eine unsortierte Liste an Mock-Daten, um die Sortierlogik zu testen
    private val mockScores = listOf(
        TotalScore(nickname = "Bob", playerId = "2", totalScore = 34),    // Wird 2.
        TotalScore(nickname = "Alice", playerId = "1", totalScore = -15), // Wird 1.
        TotalScore(nickname = "Charlie", playerId = "3", totalScore = 34),// Wird ebenfalls 2.
        TotalScore(nickname = "Diana", playerId = "4", totalScore = 120)  // Wird 4.
    )

    @Test
    fun gameOverScreen_renders_without_crash() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameOverScreen(
                    totalScores = emptyList(),
                    onBackToStart = {}
                )
            }
        }
        // Prüft das Haupt-Element auf Existenz
        composeTestRule.onNodeWithText("GAME OVER").assertIsDisplayed()
    }

    @Test
    fun gameOverScreen_shows_topbar_title() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameOverScreen(
                    totalScores = emptyList(),
                    onBackToStart = {}
                )
            }
        }
        // Prüft, ob der Titel in der Top-Bar angezeigt wird
        composeTestRule.onNodeWithText("SKYJO ACTION").assertIsDisplayed()
    }

    @Test
    fun gameOverScreen_shows_leaderboard_headers() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameOverScreen(
                    totalScores = emptyList(),
                    onBackToStart = {}
                )
            }
        }

        // ignoreCase = true löst das Problem, falls der Text im UI als "FINAL RESULTS" gerendert wird
        composeTestRule.onNodeWithText("Final Results", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("Leaderboard", ignoreCase = true).assertExists()
    }

    @Test
    fun gameOverScreen_topbar_back_button_triggers_callback() {
        var backClicked = false
        composeTestRule.setContent {
            SkyjoTheme {
                GameOverScreen(
                    totalScores = emptyList(),
                    onBackToStart = { backClicked = true }
                )
            }
        }

        // Simuliert einen Klick auf den Zurück-Pfeil in der Top-Bar
        composeTestRule.onNodeWithContentDescription("Back to Start").performClick()

        // Verifiziert, dass onBackToStart aufgerufen wurde
        assert(backClicked)
    }

    @Test
    fun gameOverScreen_primary_button_triggers_callback() {
        var backClicked = false
        composeTestRule.setContent {
            SkyjoTheme {
                GameOverScreen(
                    totalScores = emptyList(),
                    onBackToStart = { backClicked = true }
                )
            }
        }

        // Simuliert einen Klick auf den großen Primary-Button am unteren Rand
        composeTestRule.onNodeWithText("BACK TO START").performClick()

        // Verifiziert, dass onBackToStart auch hier aufgerufen wurde
        assert(backClicked)
    }

    @Test
    fun gameOverScreen_sorts_and_displays_scores_correctly() {
        composeTestRule.setContent {
            SkyjoTheme {
                GameOverScreen(
                    totalScores = mockScores,
                    onBackToStart = {}
                )
            }
        }

        // 1. Platz: Alice sollte mit der niedrigsten Punktzahl gewinnen und den Pokal haben
        composeTestRule.onNodeWithText("Alice", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("-15 pts", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("\uD83C\uDFC6", useUnmergedTree = true).assertExists()

        // 2. Platz: Bob und Charlie teilen sich denselben Rang
        composeTestRule.onNodeWithText("Bob", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Charlie", useUnmergedTree = true).assertExists()
        composeTestRule.onAllNodesWithText("34 pts", useUnmergedTree = true).assertCountEquals(2)
        composeTestRule.onAllNodesWithText("\uD83E\uDD48", useUnmergedTree = true).assertCountEquals(2)
        composeTestRule.onAllNodesWithText("\uD83E\uDD49", useUnmergedTree = true).assertCountEquals(0)

        // 4. Platz: Diana
        composeTestRule.onNodeWithText("Diana", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("120 pts", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("4.", useUnmergedTree = true).assertExists()
    }
}
