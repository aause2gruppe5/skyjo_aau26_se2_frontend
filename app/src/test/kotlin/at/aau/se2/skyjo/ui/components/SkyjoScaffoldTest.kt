package at.aau.se2.skyjo.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.ui.navigation.AppDestination
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SkyjoScaffoldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── SkyjoTopBar ───────────────────────────────────────────────────────

    @Test
    fun topBar_shows_app_title() {
        composeTestRule.setContent {
            SkyjoTheme {
                SkyjoTopBar(onMenuClick = {})
            }
        }
        composeTestRule.onNodeWithText("SKYJO ACTION").assertIsDisplayed()
    }

    @Test
    fun topBar_menu_button_triggers_callback() {
        var clicked = false
        composeTestRule.setContent {
            SkyjoTheme {
                SkyjoTopBar(onMenuClick = { clicked = true })
            }
        }
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        assert(clicked)
    }

    // ── SkyjoBottomNavBar ────────────────────────────────────────────────

    @Test
    fun bottomNavBar_shows_play_tab() {
        composeTestRule.setContent {
            SkyjoTheme {
                SkyjoBottomNavBar(
                    currentDestination = AppDestination.Start,
                    onNavigate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Play").assertIsDisplayed()
    }

    @Test
    fun bottomNavBar_shows_friends_tab() {
        composeTestRule.setContent {
            SkyjoTheme {
                SkyjoBottomNavBar(
                    currentDestination = AppDestination.Start,
                    onNavigate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Friends").assertIsDisplayed()
    }

    @Test
    fun bottomNavBar_shows_settings_tab() {
        composeTestRule.setContent {
            SkyjoTheme {
                SkyjoBottomNavBar(
                    currentDestination = AppDestination.Start,
                    onNavigate = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun bottomNavBar_navigate_callback_fires_on_tab_click() {
        var navigatedTo: AppDestination? = null
        composeTestRule.setContent {
            SkyjoTheme {
                SkyjoBottomNavBar(
                    currentDestination = AppDestination.Start,
                    onNavigate = { navigatedTo = it },
                )
            }
        }
        composeTestRule.onNodeWithText("Friends").performClick()
        assert(navigatedTo == AppDestination.Friends)
    }

    @Test
    fun bottomNavBar_navigate_to_settings_on_click() {
        var navigatedTo: AppDestination? = null
        composeTestRule.setContent {
            SkyjoTheme {
                SkyjoBottomNavBar(
                    currentDestination = AppDestination.Start,
                    onNavigate = { navigatedTo = it },
                )
            }
        }
        composeTestRule.onNodeWithText("Settings").performClick()
        assert(navigatedTo == AppDestination.Settings)
    }

    // ── SkyjoDrawerContent ───────────────────────────────────────────────

    @Test
    fun drawerContent_shows_player_name() {
        composeTestRule.setContent {
            SkyjoTheme {
                SkyjoDrawerContent(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("AcePlayer").assertIsDisplayed()
    }

    @Test
    fun drawerContent_shows_settings_menu_item() {
        composeTestRule.setContent {
            SkyjoTheme {
                SkyjoDrawerContent(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    // "Log Out" is pushed to the bottom of the drawer by a weight(1f) spacer;
    // use assertExists() since it may be outside the visible viewport in tests
    @Test
    fun drawerContent_shows_log_out_button() {
        composeTestRule.setContent {
            SkyjoTheme {
                SkyjoDrawerContent(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("Log Out").assertExists()
    }

    @Test
    fun drawerContent_settings_item_triggers_navigate() {
        var navigatedTo: AppDestination? = null
        composeTestRule.setContent {
            SkyjoTheme {
                SkyjoDrawerContent(onNavigate = { navigatedTo = it })
            }
        }
        composeTestRule.onNodeWithText("Settings").performClick()
        assert(navigatedTo == AppDestination.Settings)
    }

    // ── SkyjoDrawerScaffold ──────────────────────────────────────────────

    @Test
    fun drawerScaffold_renders_content() {
        composeTestRule.setContent {
            SkyjoTheme {
                SkyjoDrawerScaffold(
                    currentDestination = AppDestination.Start,
                    onNavigate = {},
                ) {
                    Text("Screen Content")
                }
            }
        }
        composeTestRule.onNodeWithText("Screen Content").assertIsDisplayed()
    }

    @Test
    fun drawerScaffold_shows_top_bar() {
        composeTestRule.setContent {
            SkyjoTheme {
                SkyjoDrawerScaffold(
                    currentDestination = AppDestination.Start,
                    onNavigate = {},
                ) {
                    Text("Content")
                }
            }
        }
        composeTestRule.onNodeWithText("SKYJO ACTION").assertIsDisplayed()
    }

    @Test
    fun drawerScaffold_shows_bottom_nav() {
        composeTestRule.setContent {
            SkyjoTheme {
                SkyjoDrawerScaffold(
                    currentDestination = AppDestination.Start,
                    onNavigate = {},
                ) {
                    Text("Content")
                }
            }
        }
        composeTestRule.onNodeWithText("Play").assertIsDisplayed()
    }
}
