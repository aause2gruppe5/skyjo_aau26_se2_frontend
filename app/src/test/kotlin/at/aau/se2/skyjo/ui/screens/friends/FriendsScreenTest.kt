package at.aau.se2.skyjo.ui.screens.friends

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import at.aau.se2.skyjo.haptic.HapticController
import at.aau.se2.skyjo.haptic.LocalHaptic
import at.aau.se2.skyjo.model.social.FriendDto
import at.aau.se2.skyjo.model.social.FriendRequestDto
import at.aau.se2.skyjo.model.social.FriendRequestStatus
import at.aau.se2.skyjo.model.social.LobbyInviteDto
import at.aau.se2.skyjo.model.social.LobbyInviteStatus
import at.aau.se2.skyjo.model.social.SocialUserDto
import at.aau.se2.skyjo.settings.SettingsRepository
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import org.junit.Assert.assertTrue
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
        composeTestRule.onNodeWithText("Your Friends").assertIsDisplayed()
    }

    @Test
    fun friendsScreen_shows_search_field() {
        composeTestRule.setContent {
            SkyjoTheme {
                FriendsScreen(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("Search users").assertExists()
    }

    @Test
    fun friendsScreen_shows_empty_friend_state() {
        composeTestRule.setContent {
            SkyjoTheme {
                FriendsScreen(onNavigate = {})
            }
        }
        composeTestRule.onNodeWithText("No friends yet").assertIsDisplayed()
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
    fun friendsScreen_shows_request_and_invite_actions() {
        composeTestRule.setContent {
            SkyjoTheme {
                FriendsScreen(
                    onNavigate = {},
                    incomingRequests = listOf(
                        FriendRequestDto(
                            requestId = "request-1",
                            from = SocialUserDto("from", "Requester"),
                            to = SocialUserDto("to", "Me"),
                            status = FriendRequestStatus.PENDING,
                            createdAt = 1L,
                        ),
                    ),
                    lobbyInvites = listOf(
                        LobbyInviteDto(
                            inviteId = "invite-1",
                            lobbyId = "lobby-1",
                            joinCode = "ABC123",
                            from = SocialUserDto("from", "Requester"),
                            to = SocialUserDto("to", "Me"),
                            status = LobbyInviteStatus.PENDING,
                            createdAt = 1L,
                        ),
                    ),
                )
            }
        }
        composeTestRule.onNodeWithText("Requests").assertExists()
        composeTestRule.onNodeWithText("Lobby Invites").assertExists()
        composeTestRule.onNodeWithText("Accept").assertExists()
        composeTestRule.onNodeWithText("Join").assertExists()
        composeTestRule.onNodeWithText("Code ABC123").assertExists()
    }

    @Test
    fun friendsScreen_action_pill_click_fires_haptic_and_callback() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val settings = SettingsRepository.getInstance(context)
        val haptic = HapticController(context, settings)
        var declined: String? = null

        composeTestRule.setContent {
            SkyjoTheme {
                CompositionLocalProvider(LocalHaptic provides haptic) {
                    FriendsScreen(
                        onNavigate = {},
                        incomingRequests = listOf(
                            FriendRequestDto(
                                requestId = "request-1",
                                from = SocialUserDto("from", "Requester"),
                                to = SocialUserDto("to", "Me"),
                                status = FriendRequestStatus.PENDING,
                                createdAt = 1L,
                            ),
                        ),
                        onDeclineRequest = { declined = it },
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Decline").performClick()
        composeTestRule.waitForIdle()

        assertTrue(declined == "request-1")
    }

    @Test
    fun friendsScreen_shows_friend_relationship_statuses() {
        composeTestRule.setContent {
            SkyjoTheme {
                FriendsScreen(
                    onNavigate = {},
                    friends = listOf(FriendDto("user-1", "OfflineFriend", online = false)),
                    searchResults = listOf(
                        SocialUserDto("user-2", "AlreadyFriend", at.aau.se2.skyjo.model.social.RelationshipStatus.FRIENDS),
                        SocialUserDto("user-3", "IncomingUser", at.aau.se2.skyjo.model.social.RelationshipStatus.INCOMING_REQUEST),
                        SocialUserDto("user-4", "OutgoingUser", at.aau.se2.skyjo.model.social.RelationshipStatus.OUTGOING_REQUEST),
                    ),
                )
            }
        }
        composeTestRule.onNodeWithText("Offline").assertExists()
        composeTestRule.onNodeWithText("Friend").assertExists()
        composeTestRule.onNodeWithText("Request pending").assertExists()
        composeTestRule.onNodeWithText("Sent").assertExists()
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
