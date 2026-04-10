package at.aau.se2.skyjo.ui.screens.friends

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class FriendsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun friendsScreen_renders_without_crash() {
        composeTestRule.setContent {
            SkyjoTheme {
                FriendsScreen(onNavigate = {})
            }
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun friendsScreen_content_is_composed() {
        composeTestRule.setContent {
            SkyjoTheme {
                FriendsScreen(onNavigate = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun friendsScreen_navigate_callback_not_triggered_on_render() {
        var navigated = false
        composeTestRule.setContent {
            SkyjoTheme {
                FriendsScreen(onNavigate = { navigated = true })
            }
        }
        composeTestRule.waitForIdle()
        assert(!navigated)
    }
}
