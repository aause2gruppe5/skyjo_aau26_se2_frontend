package at.aau.se2.skyjo.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test

class SkyjoApiDefaultTest {

    private val api = object : SkyjoApi {}

    @Test
    fun `default interface methods fail loudly when not implemented`() {
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.register("a", "b") } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.login("a", "b") } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.me() } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.logout() } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.createWebSocketTicket() } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.myStats() } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.leaderboard() } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.createLobby() } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.joinLobby("ABC123") } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.currentLobby() } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.leaveLobby("lobby") } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.searchUsers("a") } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.friends() } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.friendRequests() } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.sendFriendRequest("user") } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.acceptFriendRequest("request") } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.declineFriendRequest("request") } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.lobbyInvites() } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.sendLobbyInvite("lobby", "user") } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.acceptLobbyInvite("invite") } }
        assertThrows(UnsupportedOperationException::class.java) { runBlocking { api.declineLobbyInvite("invite") } }
    }
}
