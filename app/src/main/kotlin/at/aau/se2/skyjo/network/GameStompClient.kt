package at.aau.se2.skyjo.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import at.aau.se2.skyjo.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import java.net.URLEncoder

class GameStompClient(context: Context) : GameRealtimeClient {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("skyjo_prefs", Context.MODE_PRIVATE)

    private val stompClient = StompClient(OkHttpWebSocketClient())
    private var session: StompSession? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val subscriptionJobs = mutableListOf<Job>()
    private val subscribedGameIds = mutableSetOf<String>()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val _lobbyState = MutableStateFlow<LobbyUpdateMessage?>(null)
    override val lobbyState: StateFlow<LobbyUpdateMessage?> = _lobbyState.asStateFlow()

    private val _gameState = MutableStateFlow<GameUpdateMessage?>(null)
    override val gameState: StateFlow<GameUpdateMessage?> = _gameState.asStateFlow()

    private val _actionCardResults = MutableSharedFlow<ActionCardResultMessage>(extraBufferCapacity = 1)
    override val actionCardResults: SharedFlow<ActionCardResultMessage> = _actionCardResults.asSharedFlow()

    private val _cheatPeekResults = MutableSharedFlow<CheatPeekResultMessage>(extraBufferCapacity = 1)
    override val cheatPeekResults: SharedFlow<CheatPeekResultMessage> = _cheatPeekResults.asSharedFlow()

    private val _incomingInvites = MutableSharedFlow<at.aau.se2.skyjo.model.social.LobbyInviteDto>(extraBufferCapacity = 4)
    override val incomingInvites: SharedFlow<at.aau.se2.skyjo.model.social.LobbyInviteDto> = _incomingInvites.asSharedFlow()

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    override val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _hasRejoinedGame = MutableStateFlow(false)
    override val hasRejoinedGame: StateFlow<Boolean> = _hasRejoinedGame.asStateFlow()

    override suspend fun connect() {
        connect(ticket = null, lobbyJoinCode = null)
    }

    override suspend fun connect(ticket: String?, lobbyJoinCode: String?) {
        _hasRejoinedGame.value = false
        cancelSubscriptions()
        try {
            session?.disconnect()
        } catch (_: Exception) {}

        try {
            session = stompClient.connect(ticket?.let(::ticketUrl) ?: SERVER_URL)
            _connectionError.value = null
            _isConnected.value = true
            Log.d(TAG, "Connected successfully")

            // Subscribe before returning so lobby actions cannot miss first updates.
            val s = session!!
            val lobbyFlow       = s.subscribeText(lobbyJoinCode?.let { "/topic/lobbies/$it" } ?: "/topic/lobby")
            val lobbyDirectFlow = s.subscribeText("/user/queue/lobby")
            val gameFlow        = s.subscribeText("/topic/game")
            val errorsFlow      = s.subscribeText("/user/queue/errors")
            val rejoinFlow      = s.subscribeText("/user/queue/gamestate")
            val actionCardResultsFlow = s.subscribeText("/user/queue/action-card-results")
            val cheatPeekResultsFlow = s.subscribeText("/user/queue/cheat-peek-results")
            val invitesFlow = s.subscribeText("/user/queue/invites")

            subscriptionJobs += listOf(
                scope.launch { collectLobby(lobbyFlow) },
                scope.launch { collectLobbyDirect(lobbyDirectFlow) },
                scope.launch { collectGame(gameFlow) },
                scope.launch { collectErrors(errorsFlow) },
                scope.launch { collectRejoinState(rejoinFlow) },
                scope.launch { collectActionCardResults(actionCardResultsFlow) },
                scope.launch { collectCheatPeekResults(cheatPeekResultsFlow) },
                scope.launch { collectInvites(invitesFlow) },
            )
        } catch (e: Exception) {
            Log.e(TAG, "Connection error: ${e.message}")
            _isConnected.value = false
            _connectionError.value = e.message ?: "Connection failed"
        }
    }

    private val retryDelays = listOf(1_000L, 3_000L, 9_000L)

    override suspend fun reconnect(playerName: String) {
        val storedGameId = prefs.getString(PREF_GAME_ID, null)
        for (delay in retryDelays) {
            delay(delay)
            connect()
            if (_isConnected.value) {
                joinLobby(playerName, storedGameId)
                return
            }
        }
        _connectionError.value = "Connection lost"
    }

    private fun cancelSubscriptions() {
        subscriptionJobs.forEach { it.cancel() }
        subscriptionJobs.clear()
        subscribedGameIds.clear()
    }

    override fun applyLobbyState(lobby: LobbyUpdateMessage) {
        _lobbyState.value = lobby
    }

    private suspend fun collectLobby(flow: kotlinx.coroutines.flow.Flow<String>) {
        try {
            flow.collect { jsonText ->
                try {
                    _lobbyState.value = json.decodeFromString(jsonText)
                } catch (e: Exception) {
                    Log.e(TAG, "Lobby parse error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Lobby subscribe error: ${e.message}")
            _isConnected.value = false
        }
    }

    private suspend fun collectLobbyDirect(flow: kotlinx.coroutines.flow.Flow<String>) {
        try {
            flow.collect { jsonText ->
                try {
                    _lobbyState.value = json.decodeFromString(jsonText)
                } catch (e: Exception) {
                    Log.e(TAG, "Lobby direct parse error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Lobby direct subscribe error: ${e.message}")
        }
    }

    private suspend fun collectGame(flow: kotlinx.coroutines.flow.Flow<String>) {
        try {
            flow.collect { jsonText ->
                try {
                    val msg = json.decodeFromString<GameUpdateMessage>(jsonText)
                    if (msg.gameOver) {
                        clearStoredGame()
                    } else {
                        msg.gameId?.let { prefs.edit().putString(PREF_GAME_ID, it).apply() }
                        msg.gameId?.let { subscribeGameTopic(it) }
                    }
                    _gameState.value = msg
                } catch (e: Exception) {
                    Log.e(TAG, "Game parse error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Game subscribe error: ${e.message}")
            _isConnected.value = false
        }
    }

    private suspend fun collectErrors(flow: kotlinx.coroutines.flow.Flow<String>) {
        try {
            flow.collect { jsonText ->
                try {
                    val errorMap = json.decodeFromString<Map<String, String>>(jsonText)
                    _errorMessage.tryEmit(errorMap["message"] ?: jsonText)
                } catch (e: Exception) {
                    _errorMessage.tryEmit(jsonText)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error subscribe error: ${e.message}")
        }
    }

    private suspend fun collectRejoinState(flow: kotlinx.coroutines.flow.Flow<String>) {
        try {
            flow.collect { jsonText ->
                try {
                    val msg = json.decodeFromString<GameUpdateMessage>(jsonText)
                    _gameState.value = msg
                    if (msg.isRejoinable()) {
                        msg.gameId?.let { prefs.edit().putString(PREF_GAME_ID, it).apply() }
                        msg.gameId?.let { subscribeGameTopic(it) }
                        _hasRejoinedGame.value = true
                    } else {
                        clearStoredGame()
                        _hasRejoinedGame.value = false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Rejoin state parse error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Rejoin subscribe error: ${e.message}")
        }
    }

    private suspend fun collectActionCardResults(flow: kotlinx.coroutines.flow.Flow<String>) {
        try {
            flow.collect { jsonText ->
                try {
                    _actionCardResults.tryEmit(json.decodeFromString(jsonText))
                } catch (e: Exception) {
                    Log.e(TAG, "Action card result parse error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Action card result subscribe error: ${e.message}")
        }
    }

    private suspend fun collectCheatPeekResults(flow: kotlinx.coroutines.flow.Flow<String>) {
        try {
            flow.collect { jsonText ->
                try {
                    _cheatPeekResults.tryEmit(json.decodeFromString(jsonText))
                } catch (e: Exception) {
                    Log.e(TAG, "Cheat peek parse error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Cheat peek subscribe error: ${e.message}")
        }
    }

    private suspend fun collectInvites(flow: kotlinx.coroutines.flow.Flow<String>) {
        try {
            flow.collect { jsonText ->
                try {
                    _incomingInvites.tryEmit(json.decodeFromString(jsonText))
                } catch (e: Exception) {
                    Log.e(TAG, "Invite parse error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Invite subscribe error: ${e.message}")
        }
    }

    private fun subscribeGameTopic(gameId: String) {
        val s = session ?: return
        if (!subscribedGameIds.add(gameId)) return
        subscriptionJobs += scope.launch {
            runCatching {
                collectGame(s.subscribeText("/topic/games/$gameId"))
            }.onFailure { e ->
                if (e is CancellationException) throw e
                Log.e(TAG, "Game topic subscribe error: ${e.message}")
            }
        }
    }

    override fun joinLobby(playerName: String, gameId: String?) {
        scope.launch {
            try {
                session?.sendText(
                    "/app/lobby.join",
                    json.encodeToString(JoinLobbyMessage(playerName, gameId))
                )
            } catch (e: Exception) {
                Log.e(TAG, "Join lobby error: ${e.message}")
                _isConnected.value = false
            }
        }
    }

    override fun leaveLobby() {
        scope.launch {
            try {
                session?.sendText("/app/lobby.leave", "")
            } catch (e: Exception) {
                Log.e(TAG, "Leave lobby error: ${e.message}")
            }
        }
    }

    override fun startGame(maxRounds: Int, targetScore: Int) {
        scope.launch {
            try {
                session?.sendText(
                    "/app/game.start",
                    json.encodeToString(StartGameMessage(maxRounds, targetScore))
                )
            } catch (e: Exception) {
                Log.e(TAG, "Start game error: ${e.message}")
            }
        }
    }

    override fun sendAction(action: GameAction) {
        scope.launch {
            try {
                session?.sendText("/app/game.action", json.encodeToString(action))
            } catch (e: Exception) {
                Log.e(TAG, "Send action error: ${e.message}")
            }
        }
    }

    override fun playActionCard(command: PlayActionCardCommand) {
        scope.launch {
            try {
                session?.sendText("/app/game.action-card", json.encodeToString(command))
            } catch (e: Exception) {
                Log.e(TAG, "Play action card error: ${e.message}")
            }
        }
    }

    override fun cheatPeekDrawPile() {
        scope.launch {
            try {
                session?.sendText("/app/game.cheat-peek", "")
            } catch (e: Exception) {
                Log.e(TAG, "Cheat peek error: ${e.message}")
            }
        }
    }

    override fun clearStoredGame() {
        prefs.edit().remove(PREF_GAME_ID).apply()
    }

    override fun disconnect() {
        cancelSubscriptions()
        scope.launch {
            try { session?.disconnect() } catch (_: Exception) {}
        }
        _isConnected.value = false
    }

    override fun close() {
        disconnect()
        scope.cancel()
    }

    companion object {
        private const val TAG = "GameStompClient"
        private const val PHASE_ROUND_FINISHED = "ROUND_FINISHED"
        private val SERVER_URL = SkyjoApiClient.WS_BASE_URL
        private const val PREF_GAME_ID = "game_id"
    }

    private fun ticketUrl(ticket: String): String =
        "$SERVER_URL?ticket=${URLEncoder.encode(ticket, "UTF-8")}"

    private fun GameUpdateMessage.isRejoinable(): Boolean =
        !gameOver && phase != PHASE_ROUND_FINISHED
}
