package at.aau.se2.skyjo.ui.screens.rules

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import at.aau.se2.skyjo.ui.theme.*

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RulesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rulesScreen_renders_without_crash() {
        composeTestRule.setContent {
            SkyjoTheme {
                RulesScreen(onBack = {})
            }
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun rulesScreen_shows_topbar_title() {
        composeTestRule.setContent {
            SkyjoTheme {
                RulesScreen(onBack = {})
            }
        }
        // Prüft, ob der Text in der Top-Bar angezeigt wird
        composeTestRule.onNodeWithText("HOW TO PLAY").assertIsDisplayed()
    }

    @Test
    fun rulesScreen_shows_content_title() {
        composeTestRule.setContent {
            SkyjoTheme {
                RulesScreen(onBack = {})
            }
        }
        // Prüft, ob die große Überschrift im scrollbaren Bereich existiert
        composeTestRule.onNodeWithText("SKYJO ACTION").assertExists()
    }

    @Test
    fun rulesScreen_shows_back_button() {
        composeTestRule.setContent {
            SkyjoTheme {
                RulesScreen(onBack = {})
            }
        }
        // Der IconButton hat die contentDescription "Back"
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun rulesScreen_back_button_triggers_callback() {
        var backClicked = false
        composeTestRule.setContent {
            SkyjoTheme {
                RulesScreen(onBack = { backClicked = true })
            }
        }

        // Simuliert einen Klick auf den Zurück-Pfeil
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Verifiziert, dass onBack aufgerufen wurde
        assert(backClicked)
    }

    @Test
    fun rulesScreen_back_callback_not_triggered_on_render() {
        var backClicked = false
        composeTestRule.setContent {
            SkyjoTheme {
                RulesScreen(onBack = { backClicked = true })
            }
        }
        composeTestRule.waitForIdle()

        // Verifiziert, dass onBack NICHT einfach beim Aufbau des Screens gefeuert wird
        assert(!backClicked)
    }
}