package at.aau.se2.skyjo.network

import at.aau.se2.skyjo.model.auth.AuthResponse
import at.aau.se2.skyjo.model.auth.AuthUserDto
import at.aau.se2.skyjo.model.auth.WsTicketResponse
import at.aau.se2.skyjo.model.lobby.LobbySummaryResponse
import at.aau.se2.skyjo.model.social.FriendDto
import at.aau.se2.skyjo.model.social.FriendRequestDto
import at.aau.se2.skyjo.model.social.FriendRequestsResponse
import at.aau.se2.skyjo.model.social.LobbyInviteDto
import at.aau.se2.skyjo.model.social.SocialUserDto
import at.aau.se2.skyjo.model.stats.LeaderboardEntryDto
import at.aau.se2.skyjo.model.stats.PlayerStatsDto

interface SkyjoApi {
    suspend fun register(username: String, password: String): AuthResponse = unsupported()
    suspend fun login(username: String, password: String): AuthResponse = unsupported()
    suspend fun me(): AuthUserDto = unsupported()
    suspend fun logout(): Unit = unsupported()
    suspend fun createWebSocketTicket(): WsTicketResponse = unsupported()
    suspend fun myStats(): PlayerStatsDto = unsupported()
    suspend fun leaderboard(limit: Int = 50): List<LeaderboardEntryDto> = unsupported()
    suspend fun createLobby(): LobbySummaryResponse = unsupported()
    suspend fun joinLobby(joinCode: String): LobbySummaryResponse = unsupported()
    suspend fun currentLobby(): LobbySummaryResponse? = unsupported()
    suspend fun leaveLobby(lobbyId: String): LobbySummaryResponse = unsupported()
    suspend fun searchUsers(query: String): List<SocialUserDto> = unsupported()
    suspend fun friends(): List<FriendDto> = unsupported()
    suspend fun friendRequests(): FriendRequestsResponse = unsupported()
    suspend fun sendFriendRequest(toUserId: String): FriendRequestDto = unsupported()
    suspend fun acceptFriendRequest(requestId: String): FriendRequestDto = unsupported()
    suspend fun declineFriendRequest(requestId: String): FriendRequestDto = unsupported()
    suspend fun lobbyInvites(): List<LobbyInviteDto> = unsupported()
    suspend fun sendLobbyInvite(lobbyId: String, toUserId: String): LobbyInviteDto = unsupported()
    suspend fun acceptLobbyInvite(inviteId: String): LobbyInviteDto = unsupported()
    suspend fun declineLobbyInvite(inviteId: String): LobbyInviteDto = unsupported()

    private fun unsupported(): Nothing =
        throw UnsupportedOperationException("SkyjoApi method not implemented")
}
