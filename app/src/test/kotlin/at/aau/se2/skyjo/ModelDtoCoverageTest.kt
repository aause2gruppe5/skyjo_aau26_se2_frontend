package at.aau.se2.skyjo

import at.aau.se2.skyjo.model.LobbyPlayer
import at.aau.se2.skyjo.model.auth.AuthResponse
import at.aau.se2.skyjo.model.auth.AuthUserDto
import at.aau.se2.skyjo.model.auth.ErrorResponse
import at.aau.se2.skyjo.model.auth.LoginRequest
import at.aau.se2.skyjo.model.auth.RegisterRequest
import at.aau.se2.skyjo.model.auth.WsTicketResponse
import at.aau.se2.skyjo.model.lobby.LobbySummaryResponse
import at.aau.se2.skyjo.model.social.FriendDto
import at.aau.se2.skyjo.model.social.FriendRequestDto
import at.aau.se2.skyjo.model.social.FriendRequestStatus
import at.aau.se2.skyjo.model.social.FriendRequestsResponse
import at.aau.se2.skyjo.model.social.LobbyInviteDto
import at.aau.se2.skyjo.model.social.LobbyInviteRequest
import at.aau.se2.skyjo.model.social.LobbyInviteStatus
import at.aau.se2.skyjo.model.social.RelationshipStatus
import at.aau.se2.skyjo.model.social.SendFriendRequestRequest
import at.aau.se2.skyjo.model.social.SocialUserDto
import at.aau.se2.skyjo.model.stats.LeaderboardEntryDto
import at.aau.se2.skyjo.model.stats.PlayerStatsDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelDtoCoverageTest {

    @Test
    fun `auth lobby stats and social DTOs expose constructor values`() {
        val user = AuthUserDto("user-1", "Alice")
        assertEquals("Alice", AuthResponse("token", user).user.username)
        assertEquals("Alice", RegisterRequest("Alice", "secret").username)
        assertEquals("secret", LoginRequest("Alice", "secret").password)
        assertEquals("ticket", WsTicketResponse("ticket", 10L).ticket)
        assertEquals("error", ErrorResponse("error").message)

        val lobby = LobbySummaryResponse("lobby-1", "ABC123", listOf(LobbyPlayer("Alice", true)), "WAITING", 6)
        assertEquals("ABC123", lobby.joinCode)
        assertTrue(lobby.players.single().isHost)

        val stats = PlayerStatsDto("user-1", "Alice", 2, 1, 20, 5, 10.0)
        assertEquals(10.0, stats.averageScore, 0.0)
        val entry = LeaderboardEntryDto(1, "user-1", "Alice", 10.0, 1, 2, 5, 20)
        assertEquals(1, entry.rank)

        val socialUser = SocialUserDto("user-2", "Bob", RelationshipStatus.FRIENDS)
        val friend = FriendDto("user-2", "Bob", online = true, currentLobbyId = "lobby-1")
        val friendRequest = FriendRequestDto(
            "request-1",
            from = socialUser,
            to = SocialUserDto("user-1", "Alice"),
            status = FriendRequestStatus.PENDING,
            createdAt = 1L,
            respondedAt = null,
        )
        val requests = FriendRequestsResponse(incoming = listOf(friendRequest), outgoing = emptyList())
        val invite = LobbyInviteDto(
            "invite-1",
            "lobby-1",
            "ABC123",
            from = socialUser,
            to = SocialUserDto("user-1", "Alice"),
            status = LobbyInviteStatus.PENDING,
            createdAt = 2L,
            respondedAt = null,
        )

        assertEquals("lobby-1", friend.currentLobbyId)
        assertEquals(friendRequest, requests.incoming.single())
        assertEquals("user-2", SendFriendRequestRequest("user-2").toUserId)
        assertEquals("user-2", LobbyInviteRequest("user-2").toUserId)
        assertEquals("ABC123", invite.joinCode)
        assertEquals(LobbyInviteStatus.ACCEPTED, invite.copy(status = LobbyInviteStatus.ACCEPTED).status)
    }
}
