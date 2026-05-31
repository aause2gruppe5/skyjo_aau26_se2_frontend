package at.aau.se2.skyjo.network

import at.aau.se2.skyjo.InMemorySessionStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyjoApiClientTest {

    @Test
    fun `auth methods send expected requests and decode responses`() = runBlocking {
        val responder = QueueResponder(
            201 to authJson("token-a", "user-a", "Alice"),
            200 to authJson("token-b", "user-a", "Alice"),
            200 to """{"userId":"user-a","username":"Alice"}""",
            204 to "",
            200 to """{"ticket":"ws-ticket","expiresAt":12345}""",
        )
        val api = SkyjoApiClient(InMemorySessionStore("session-token"), responder.client, "http://localhost")

        assertEquals("token-a", api.register("Alice", "SecurePass123").token)
        assertEquals("token-b", api.login("Alice", "SecurePass123").token)
        assertEquals("Alice", api.me().username)
        api.logout()
        assertEquals("ws-ticket", api.createWebSocketTicket().ticket)

        assertEquals(listOf("POST", "POST", "GET", "POST", "POST"), responder.records.map { it.method })
        assertEquals(
            listOf("/api/auth/register", "/api/auth/login", "/api/auth/me", "/api/auth/logout", "/api/auth/ws-ticket"),
            responder.records.map { it.path },
        )
        assertTrue(responder.records.all { it.authorization == "Bearer session-token" })
        assertTrue(responder.records[0].body.contains("\"username\":\"Alice\""))
    }

    @Test
    fun `lobby stats and social methods encode paths and decode responses`() = runBlocking {
        val lobbyJson = """{"lobbyId":"lobby/1","joinCode":"ABC123","players":[{"nickname":"Alice","isHost":true}],"status":"WAITING","maxPlayers":6}"""
        val friendJson = """[{"userId":"friend-1","username":"Friend","online":true,"currentLobbyId":"lobby-1"}]"""
        val requestsJson = """{"incoming":[],"outgoing":[]}"""
        val inviteJson = """{"inviteId":"invite-1","lobbyId":"lobby-1","joinCode":"ABC123","from":{"userId":"from","username":"From"},"to":{"userId":"to","username":"To"},"status":"PENDING","createdAt":1}"""
        val responder = QueueResponder(
            200 to """{"userId":"user-a","username":"Alice","gamesPlayed":1,"wins":1,"totalScore":5,"bestScore":5,"averageScore":5.0}""",
            200 to """[{"rank":1,"userId":"user-a","username":"Alice","averageScore":5.0,"wins":1,"gamesPlayed":1,"bestScore":5,"totalScore":5}]""",
            201 to lobbyJson,
            200 to lobbyJson,
            204 to "",
            200 to lobbyJson,
            200 to """[{"userId":"user-b","username":"Bob","relationshipStatus":"NONE"}]""",
            200 to friendJson,
            200 to requestsJson,
            201 to """{"requestId":"request-1","from":{"userId":"from","username":"From"},"to":{"userId":"to","username":"To"},"status":"PENDING","createdAt":1}""",
            200 to """{"requestId":"request-1","from":{"userId":"from","username":"From"},"to":{"userId":"to","username":"To"},"status":"ACCEPTED","createdAt":1,"respondedAt":2}""",
            200 to """{"requestId":"request-1","from":{"userId":"from","username":"From"},"to":{"userId":"to","username":"To"},"status":"DECLINED","createdAt":1,"respondedAt":2}""",
            200 to """[$inviteJson]""",
            201 to inviteJson,
            200 to inviteJson.replace("\"PENDING\"", "\"ACCEPTED\""),
            200 to inviteJson.replace("\"PENDING\"", "\"DECLINED\""),
        )
        val api = SkyjoApiClient(InMemorySessionStore("session-token"), responder.client, "http://localhost")

        assertEquals(1, api.myStats().gamesPlayed)
        assertEquals("Alice", api.leaderboard(limit = 3).single().username)
        assertEquals("ABC123", api.createLobby().joinCode)
        assertEquals("ABC123", api.joinLobby("AB C123").joinCode)
        assertNull(api.currentLobby())
        assertEquals("lobby/1", api.leaveLobby("lobby/1").lobbyId)
        assertEquals("Bob", api.searchUsers("Bo B").single().username)
        assertEquals("Friend", api.friends().single().username)
        assertTrue(api.friendRequests().incoming.isEmpty())
        assertEquals("request-1", api.sendFriendRequest("friend-1").requestId)
        assertEquals("ACCEPTED", api.acceptFriendRequest("request/1").status.name)
        assertEquals("DECLINED", api.declineFriendRequest("request/1").status.name)
        assertEquals("invite-1", api.lobbyInvites().single().inviteId)
        assertEquals("ABC123", api.sendLobbyInvite("lobby/1", "friend-1").joinCode)
        assertEquals("ACCEPTED", api.acceptLobbyInvite("invite/1").status.name)
        assertEquals("DECLINED", api.declineLobbyInvite("invite/1").status.name)

        val paths = responder.records.map { it.path }
        assertTrue(paths.contains("/api/lobbies/AB+C123/join"))
        assertTrue(paths.contains("/api/lobbies/lobby%2F1/leave"))
        assertTrue(paths.contains("/api/users/search?query=Bo+B"))
        assertTrue(paths.contains("/api/friends/requests/request%2F1/accept"))
        assertTrue(paths.contains("/api/lobbies/lobby%2F1/invites"))
        assertTrue(paths.contains("/api/lobbies/invites/invite%2F1/decline"))
    }

    @Test
    fun `error responses throw API exception with server message`() = runBlocking {
        val responder = QueueResponder(400 to """{"message":"bad request"}""")
        val api = SkyjoApiClient(InMemorySessionStore(), responder.client, "http://localhost")

        val error = runCatching { api.login("Alice", "wrong") }.exceptionOrNull()

        assertTrue(error is ApiException)
        assertEquals("bad request", error?.message)
        assertEquals(400, (error as ApiException).statusCode)
    }

    @Test
    fun `plain error responses fall back to response message`() = runBlocking {
        val responder = QueueResponder(500 to "not-json")
        val api = SkyjoApiClient(InMemorySessionStore(), responder.client, "http://localhost")

        val error = runCatching { api.me() }.exceptionOrNull()

        assertTrue(error is ApiException)
        assertEquals(500, (error as ApiException).statusCode)
    }

    private fun authJson(token: String, userId: String, username: String) =
        """{"token":"$token","user":{"userId":"$userId","username":"$username"}}"""

    private data class Record(
        val method: String,
        val path: String,
        val authorization: String?,
        val body: String,
    )

    private class QueueResponder(vararg responses: Pair<Int, String>) {
        private val responses = ArrayDeque(responses.toList())
        val records = mutableListOf<Record>()
        val client: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor())
            .build()

        private fun interceptor() = Interceptor { chain ->
            val request = chain.request()
            val body = request.body?.let {
                val buffer = Buffer()
                it.writeTo(buffer)
                buffer.readUtf8()
            }.orEmpty()
            records += Record(
                method = request.method,
                path = request.url.encodedPath + request.url.encodedQuery?.let { "?$it" }.orEmpty(),
                authorization = request.header("Authorization"),
                body = body,
            )
            val (code, payload) = responses.removeFirst()
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(if (code in 200..299) "OK" else "Error")
                .body(payload.toResponseBody("application/json".toMediaType()))
                .build()
        }
    }
}
