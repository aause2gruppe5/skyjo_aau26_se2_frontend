package at.aau.se2.skyjo.network

import at.aau.se2.skyjo.BuildConfig
import at.aau.se2.skyjo.model.auth.AuthResponse
import at.aau.se2.skyjo.model.auth.AuthUserDto
import at.aau.se2.skyjo.model.auth.LoginRequest
import at.aau.se2.skyjo.model.auth.RegisterRequest
import at.aau.se2.skyjo.model.auth.WsTicketResponse
import at.aau.se2.skyjo.model.lobby.LobbySummaryResponse
import at.aau.se2.skyjo.model.social.FriendDto
import at.aau.se2.skyjo.model.social.FriendRequestsResponse
import at.aau.se2.skyjo.model.social.LobbyInviteDto
import at.aau.se2.skyjo.model.social.LobbyInviteRequest
import at.aau.se2.skyjo.model.social.SendFriendRequestRequest
import at.aau.se2.skyjo.model.social.SocialUserDto
import at.aau.se2.skyjo.model.stats.LeaderboardEntryDto
import at.aau.se2.skyjo.model.stats.PlayerStatsDto
import at.aau.se2.skyjo.session.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

class ApiException(message: String, val statusCode: Int? = null) : Exception(message)

@kotlinx.serialization.Serializable
private data class ServerErrorResponse(
    val message: String? = null,
    val error: String? = null,
    val status: Int? = null,
)

class SkyjoApiClient(
    private val sessionStore: SessionStore,
    private val client: OkHttpClient = OkHttpClient(),
    private val baseUrl: String = HTTP_BASE_URL,
) : SkyjoApi {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun register(username: String, password: String): AuthResponse =
        post("/api/auth/register", RegisterRequest(username, password))

    override suspend fun login(username: String, password: String): AuthResponse =
        post("/api/auth/login", LoginRequest(username, password))

    override suspend fun me(): AuthUserDto =
        get("/api/auth/me")

    override suspend fun logout() {
        request<Unit>("POST", "/api/auth/logout")
    }

    override suspend fun createWebSocketTicket(): WsTicketResponse =
        postEmpty("/api/auth/ws-ticket")

    override suspend fun myStats(): PlayerStatsDto =
        get("/api/stats/me")

    override suspend fun leaderboard(limit: Int): List<LeaderboardEntryDto> =
        get("/api/leaderboard?limit=$limit")

    override suspend fun createLobby(): LobbySummaryResponse =
        postEmpty("/api/lobbies")

    override suspend fun joinLobby(joinCode: String): LobbySummaryResponse =
        postEmpty("/api/lobbies/${encode(joinCode.trim())}/join")

    override suspend fun currentLobby(): LobbySummaryResponse? =
        request("GET", "/api/lobbies/current", allowNoContent = true)

    override suspend fun leaveLobby(lobbyId: String): LobbySummaryResponse =
        postEmpty("/api/lobbies/${encode(lobbyId)}/leave")

    override suspend fun searchUsers(query: String): List<SocialUserDto> =
        get("/api/users/search?query=${encode(query.trim())}")

    override suspend fun friends(): List<FriendDto> =
        get("/api/friends")

    override suspend fun friendRequests(): FriendRequestsResponse =
        get("/api/friends/requests")

    override suspend fun sendFriendRequest(toUserId: String) =
        post<SendFriendRequestRequest, at.aau.se2.skyjo.model.social.FriendRequestDto>(
            "/api/friends/requests",
            SendFriendRequestRequest(toUserId),
        )

    override suspend fun acceptFriendRequest(requestId: String) =
        postEmpty<at.aau.se2.skyjo.model.social.FriendRequestDto>("/api/friends/requests/${encode(requestId)}/accept")

    override suspend fun declineFriendRequest(requestId: String) =
        postEmpty<at.aau.se2.skyjo.model.social.FriendRequestDto>("/api/friends/requests/${encode(requestId)}/decline")

    override suspend fun lobbyInvites(): List<LobbyInviteDto> =
        get("/api/lobbies/invites")

    override suspend fun sendLobbyInvite(lobbyId: String, toUserId: String): LobbyInviteDto =
        post("/api/lobbies/${encode(lobbyId)}/invites", LobbyInviteRequest(toUserId))

    override suspend fun acceptLobbyInvite(inviteId: String): LobbyInviteDto =
        postEmpty("/api/lobbies/invites/${encode(inviteId)}/accept")

    override suspend fun declineLobbyInvite(inviteId: String): LobbyInviteDto =
        postEmpty("/api/lobbies/invites/${encode(inviteId)}/decline")

    private suspend inline fun <reified T> get(path: String): T =
        request("GET", path) ?: throw ApiException("Leere Antwort")

    private suspend inline fun <reified RequestBody : Any, reified ResponseBody> post(
        path: String,
        body: RequestBody,
    ): ResponseBody =
        request("POST", path, json.encodeToString(body)) ?: throw ApiException("Leere Antwort")

    private suspend inline fun <reified T> postEmpty(path: String): T =
        request("POST", path) ?: throw ApiException("Leere Antwort")

    private suspend inline fun <reified T> request(
        method: String,
        path: String,
        bodyJson: String? = null,
        allowNoContent: Boolean = false,
    ): T? = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url(baseUrl.trimEnd('/') + path)
        sessionStore.getToken()?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }
        val body = bodyJson?.toRequestBody(JSON_MEDIA_TYPE)
        when (method) {
            "GET" -> requestBuilder.get()
            "POST" -> requestBuilder.post(body ?: ByteArray(0).toRequestBody(JSON_MEDIA_TYPE))
            else -> error("Unsupported method $method")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (response.code == 204 && allowNoContent) {
                return@withContext null
            }
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching {
                    json.decodeFromString<ServerErrorResponse>(responseBody)
                        .let { it.message ?: it.error ?: "HTTP ${response.code}" }
                }.getOrElse { "HTTP ${response.code}" }
                throw ApiException(message, response.code)
            }
            if (T::class == Unit::class) {
                @Suppress("UNCHECKED_CAST")
                return@withContext Unit as T
            }
            json.decodeFromString<T>(responseBody)
        }
    }

    companion object {
        val HTTP_BASE_URL: String = BuildConfig.HTTP_BASE_URL
        val WS_BASE_URL: String = BuildConfig.WS_BASE_URL
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8")
}
