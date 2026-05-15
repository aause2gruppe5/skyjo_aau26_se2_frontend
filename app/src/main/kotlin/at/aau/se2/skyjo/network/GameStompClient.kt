package at.aau.se2.skyjo.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import at.aau.se2.skyjo.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.sendText
import org.hildan.krossbow.stomp.subscribeText
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient

class GameStompClient(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("skyjo_prefs", Context.MODE_PRIVATE)

    private val stompClient = StompClient(OkHttpWebSocketClient())
    private var session: StompSession? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var subscriptionJobs: List<Job> = emptyList()

    // Serializes connect/reconnect so concurrent callers can't race on `session`
    // and `subscriptionJobs` (which would leak sockets and double-join the lobby).
    private val connectMutex = Mutex()

    @Volatile
    private var reconnecting = false

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val _lobbyState = MutableStateFlow<LobbyUpdateMessage?>(null)
    val lobbyState: StateFlow<LobbyUpdateMessage?> = _lobbyState.asStateFlow()

    private val _gameState = MutableStateFlow<GameUpdateMessage?>(null)
    val gameState: StateFlow<GameUpdateMessage?> = _gameState.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _hasRejoinedGame = MutableStateFlow(false)
    val hasRejoinedGame: StateFlow<Boolean> = _hasRejoinedGame.asStateFlow()

    suspend fun connect() = connectMutex.withLock { connectInternal() }

    private suspend fun connectInternal() {
        _hasRejoinedGame.value = false
        cancelSubscriptions()
        try {
            session?.disconnect()
        } catch (_: Exception) {}

        try {
            session = stompClient.connect(SERVER_URL)
            Log.d(TAG, "Connected successfully")

            // Build all subscriptions before declaring connected so that a failed
            // subscribeText() never creates a spurious true→false transition on _isConnected.
            val s = session!!
            val lobbyFlow       = s.subscribeText("/topic/lobby")
            val lobbyDirectFlow = s.subscribeText("/user/queue/lobby")
            val gameFlow        = s.subscribeText("/topic/game")
            val errorsFlow      = s.subscribeText("/user/queue/errors")
            val rejoinFlow      = s.subscribeText("/user/queue/gamestate")

            subscriptionJobs = listOf(
                scope.launch { collectLobby(lobbyFlow) },
                scope.launch { collectLobbyDirect(lobbyDirectFlow) },
                scope.launch { collectGame(gameFlow) },
                scope.launch { collectErrors(errorsFlow) },
                scope.launch { collectRejoinState(rejoinFlow) },
            )
            _connectionError.value = null
            _isConnected.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Connection error: ${e.message}")
            _isConnected.value = false
            _connectionError.value = e.message ?: "Connection failed"
        }
    }

    private val retryDelays = listOf(1_000L, 3_000L, 9_000L)
    private val maxRetryDelay = 9_000L

    suspend fun reconnect(playerName: String) {
        // Single-flight: never run two reconnect loops at once.
        if (reconnecting) return
        reconnecting = true
        try {
            val storedGameId = prefs.getString(PREF_GAME_ID, null)
            var attempt = 0
            // Retry indefinitely with capped backoff so the app recovers whenever
            // the server comes back, instead of giving up after 3 tries forever.
            while (true) {
                delay(retryDelays.getOrElse(attempt) { maxRetryDelay })
                connect()
                if (_isConnected.value) {
                    joinLobby(playerName, storedGameId)
                    return
                }
                _connectionError.value = "Verbindung getrennt, versuche erneut…"
                attempt++
            }
        } finally {
            reconnecting = false
        }
    }

    private fun cancelSubscriptions() {
        subscriptionJobs.forEach { it.cancel() }
        subscriptionJobs = emptyList()
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
                    _hasRejoinedGame.value = true
                } catch (e: Exception) {
                    Log.e(TAG, "Rejoin state parse error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Rejoin subscribe error: ${e.message}")
        }
    }

    fun joinLobby(playerName: String, gameId: String? = null) {
        scope.launch {
            try {
                session?.sendText(
                    "/app/lobby.join",
                    json.encodeToString(JoinLobbyMessage(playerName, gameId))
                )
            } catch (e: Exception) {
                Log.e(TAG, "Join lobby error: ${e.message}")
                // A failed send does not mean the WebSocket is gone; the subscription
                // collectors will set _isConnected = false if the session truly closes.
            }
        }
    }

    fun leaveLobby() {
        scope.launch {
            try {
                session?.sendText("/app/lobby.leave", "")
            } catch (e: Exception) {
                Log.e(TAG, "Leave lobby error: ${e.message}")
            }
        }
    }

    fun startGame(maxRounds: Int, targetScore: Int) {
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

    fun sendAction(action: GameAction) {
        scope.launch {
            try {
                session?.sendText("/app/game.action", json.encodeToString(action))
            } catch (e: Exception) {
                Log.e(TAG, "Send action error: ${e.message}")
            }
        }
    }

    fun clearStoredGame() {
        prefs.edit().remove(PREF_GAME_ID).apply()
    }

    fun disconnect() {
        cancelSubscriptions()
        scope.launch {
            try { session?.disconnect() } catch (_: Exception) {}
        }
        _isConnected.value = false
    }

    fun close() {
        disconnect()
        scope.cancel()
    }

    companion object {
        private const val TAG = "GameStompClient"
        private const val SERVER_URL = "ws://10.0.2.2:8765/ws"
        private const val PREF_GAME_ID = "game_id"
    }
}
