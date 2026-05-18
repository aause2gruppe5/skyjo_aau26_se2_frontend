package at.aau.se2.skyjo.viewmodel

import android.app.Application
import at.aau.se2.skyjo.model.social.FriendDto
import at.aau.se2.skyjo.model.social.FriendRequestDto
import at.aau.se2.skyjo.model.social.FriendRequestStatus
import at.aau.se2.skyjo.model.social.FriendRequestsResponse
import at.aau.se2.skyjo.model.social.LobbyInviteDto
import at.aau.se2.skyjo.model.social.LobbyInviteStatus
import at.aau.se2.skyjo.model.social.SocialUserDto
import at.aau.se2.skyjo.network.ApiException
import at.aau.se2.skyjo.network.SkyjoApi
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FriendsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val application = mockk<Application>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refresh loads friends requests and lobby invites`() {
        val api = FakeFriendsApi()
        val viewModel = FriendsViewModel(application, api)

        viewModel.refresh()

        assertEquals(listOf(friend), viewModel.state.value.friends)
        assertEquals(listOf(request), viewModel.state.value.incomingRequests)
        assertEquals(listOf(invite), viewModel.state.value.lobbyInvites)
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `refresh failure records error and stops loading`() {
        val viewModel = FriendsViewModel(application, FakeFriendsApi(refreshError = ApiException("offline")))

        viewModel.refresh()

        assertEquals("offline", viewModel.state.value.errorMessage)
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `short search query clears results without API call`() {
        val api = FakeFriendsApi()
        val viewModel = FriendsViewModel(application, api)

        viewModel.updateSearch("a")

        assertEquals("a", viewModel.state.value.query)
        assertTrue(viewModel.state.value.searchResults.isEmpty())
        assertEquals(0, api.searchCalls)
    }

    @Test
    fun `search query loads users`() {
        val api = FakeFriendsApi()
        val viewModel = FriendsViewModel(application, api)

        viewModel.updateSearch("ali")

        assertEquals(1, api.searchCalls)
        assertEquals(listOf(searchUser), viewModel.state.value.searchResults)
    }

    @Test
    fun `friend request actions refresh state on success`() {
        val api = FakeFriendsApi()
        val viewModel = FriendsViewModel(application, api)

        viewModel.sendFriendRequest("to-user")
        viewModel.acceptRequest("request-1")
        viewModel.declineRequest("request-2")

        assertEquals(1, api.sendRequestCalls)
        assertEquals(1, api.acceptCalls)
        assertEquals(1, api.declineCalls)
        assertEquals(3, api.refreshCalls)
    }

    @Test
    fun `friend request failure stores error`() {
        val viewModel = FriendsViewModel(application, FakeFriendsApi(sendRequestError = ApiException("duplicate")))

        viewModel.sendFriendRequest("to-user")

        assertEquals("duplicate", viewModel.state.value.errorMessage)
    }

    @Test
    fun `invite friend requires active lobby`() {
        val api = FakeFriendsApi()
        val viewModel = FriendsViewModel(application, api)

        viewModel.inviteFriend(null, "friend-1")
        viewModel.inviteFriend("lobby-1", "friend-1")

        assertEquals(1, api.inviteCalls)
        assertEquals("lobby-1", api.lastLobbyId)
    }

    @Test
    fun `invite friend failure stores error`() {
        val viewModel = FriendsViewModel(application, FakeFriendsApi(inviteError = ApiException("not friends")))

        viewModel.inviteFriend("lobby-1", "friend-1")

        assertEquals("not friends", viewModel.state.value.errorMessage)
    }

    @Test
    fun `add lobby invite replaces duplicate and remove deletes invite`() {
        val viewModel = FriendsViewModel(application, FakeFriendsApi())

        viewModel.addLobbyInvite(invite.copy(joinCode = "AAA111"))
        viewModel.addLobbyInvite(invite.copy(joinCode = "BBB222"))
        viewModel.removeLobbyInvite(invite.inviteId)

        assertTrue(viewModel.state.value.lobbyInvites.isEmpty())
    }

    @Test
    fun `decline lobby invite removes invite on success`() {
        val api = FakeFriendsApi()
        val viewModel = FriendsViewModel(application, api)
        viewModel.addLobbyInvite(invite)

        viewModel.declineLobbyInvite(invite.inviteId)

        assertEquals(1, api.declineInviteCalls)
        assertTrue(viewModel.state.value.lobbyInvites.isEmpty())
    }

    @Test
    fun `decline lobby invite failure keeps error`() {
        val viewModel = FriendsViewModel(application, FakeFriendsApi(declineInviteError = ApiException("missing")))

        viewModel.declineLobbyInvite(invite.inviteId)

        assertEquals("missing", viewModel.state.value.errorMessage)
    }

    private class FakeFriendsApi(
        private val refreshError: Throwable? = null,
        private val sendRequestError: Throwable? = null,
        private val inviteError: Throwable? = null,
        private val declineInviteError: Throwable? = null,
    ) : SkyjoApi {
        var refreshCalls = 0
        var searchCalls = 0
        var sendRequestCalls = 0
        var acceptCalls = 0
        var declineCalls = 0
        var inviteCalls = 0
        var declineInviteCalls = 0
        var lastLobbyId: String? = null

        override suspend fun friends(): List<FriendDto> {
            refreshCalls++
            refreshError?.let { throw it }
            return listOf(friend)
        }

        override suspend fun friendRequests(): FriendRequestsResponse =
            FriendRequestsResponse(incoming = listOf(request), outgoing = emptyList())

        override suspend fun lobbyInvites(): List<LobbyInviteDto> = listOf(invite)

        override suspend fun searchUsers(query: String): List<SocialUserDto> {
            searchCalls++
            return listOf(searchUser)
        }

        override suspend fun sendFriendRequest(toUserId: String): FriendRequestDto {
            sendRequestCalls++
            sendRequestError?.let { throw it }
            return request
        }

        override suspend fun acceptFriendRequest(requestId: String): FriendRequestDto {
            acceptCalls++
            return request.copy(status = FriendRequestStatus.ACCEPTED)
        }

        override suspend fun declineFriendRequest(requestId: String): FriendRequestDto {
            declineCalls++
            return request.copy(status = FriendRequestStatus.DECLINED)
        }

        override suspend fun sendLobbyInvite(lobbyId: String, toUserId: String): LobbyInviteDto {
            inviteCalls++
            lastLobbyId = lobbyId
            inviteError?.let { throw it }
            return invite
        }

        override suspend fun declineLobbyInvite(inviteId: String): LobbyInviteDto {
            declineInviteCalls++
            declineInviteError?.let { throw it }
            return invite.copy(status = LobbyInviteStatus.DECLINED)
        }
    }

    private companion object {
        val friend = FriendDto("friend-1", "FriendOne", online = true, currentLobbyId = "lobby-1")
        val searchUser = SocialUserDto("search-1", "SearchOne")
        val request = FriendRequestDto(
            requestId = "request-1",
            from = searchUser,
            to = SocialUserDto("me", "Me"),
            status = FriendRequestStatus.PENDING,
            createdAt = 10L,
        )
        val invite = LobbyInviteDto(
            inviteId = "invite-1",
            lobbyId = "lobby-1",
            joinCode = "ABC123",
            from = searchUser,
            to = SocialUserDto("me", "Me"),
            status = LobbyInviteStatus.PENDING,
            createdAt = 20L,
        )
    }
}
