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

class GameStompClient(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("skyjo_prefs", Context.MODE_PRIVATE)

    private val stompClient = StompClient(OkHttpWebSocketClient())
    private var session: StompSession? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var subscriptionJobs: List<Job> = emptyList()

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

    suspend fun connect() {
        _hasRejoinedGame.value = false
        cancelSubscriptions()
        try {
            session?.disconnect()
        } catch (_: Exception) {}

        try {
            session = stompClient.connect(SERVER_URL)
            _connectionError.value = null
            _isConnected.value = true
            Log.d(TAG, "Connected successfully")

            // Subscriptions synchron aufbauen — alle sind aktiv bevor connect() zurückgibt,
            // damit joinLobby danach keine Nachrichten verpasst
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
        } catch (e: Exception) {
            Log.e(TAG, "Connection error: ${e.message}")
            _isConnected.value = false
            _connectionError.value = e.message ?: "Connection failed"
        }
    }

    private val retryDelays = listOf(1_000L, 3_000L, 9_000L)

    suspend fun reconnect(playerName: String) {
        val storedGameId = prefs.getString(PREF_GAME_ID, null)
        for (delay in retryDelays) {
            delay(delay)
            connect()
            if (_isConnected.value) {
                joinLobby(playerName, storedGameId)
                return
            }
        }
        _connectionError.value = "Verbindung getrennt"
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
                    msg.gameId?.let { prefs.edit().putString(PREF_GAME_ID, it).apply() }
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
                _isConnected.value = false
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
        private const val SERVER_URL = "ws://10.0.2.2:8080/ws"
        private const val PREF_GAME_ID = "game_id"
    }
}
