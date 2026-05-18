package at.aau.se2.skyjo.ui.screens.friends

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.model.social.FriendDto
import at.aau.se2.skyjo.model.social.SocialUserDto
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

    // "Friends" also appears in the bottom nav tab, so use onAllNodesWithText
    @Test
    fun friendsScreen_shows_friends_title() {
        composeTestRule.setContent {
            SkyjoTheme {
                FriendsScreen(onNavigate = {})
            }
        }
        composeTestRule.onAllNodesWithText("Friends")[0].assertExists()
    }

    @Test
    fun friendsScreen_shows_friends_section() {
        composeTestRule.setContent {
            SkyjoTheme {
                FriendsScreen(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("Freunde").assertIsDisplayed()
    }

    @Test
    fun friendsScreen_shows_search_field() {
        composeTestRule.setContent {
            SkyjoTheme {
                FriendsScreen(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("User suchen").assertExists()
    }

    @Test
    fun friendsScreen_shows_empty_friend_state() {
        composeTestRule.setContent {
            SkyjoTheme {
                FriendsScreen(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("Noch keine Freunde").assertIsDisplayed()
    }

    @Test
    fun friendsScreen_shows_friend_invite_button() {
        composeTestRule.setContent {
            SkyjoTheme {
                FriendsScreen(
                    onNavigate = {},
                    friends = listOf(FriendDto("user-1", "FriendOne", online = true)),
                    activeLobbyId = "lobby-1",
                )
            }
        }
        composeTestRule.onAllNodesWithText("Invite")[0].assertExists()
    }

    @Test
    fun friendsScreen_shows_search_result_add_button() {
        composeTestRule.setContent {
            SkyjoTheme {
                FriendsScreen(
                    onNavigate = {},
                    searchResults = listOf(SocialUserDto("user-2", "SearchUser")),
                )
            }
        }
        composeTestRule.onAllNodesWithText("Add")[0].assertExists()
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
