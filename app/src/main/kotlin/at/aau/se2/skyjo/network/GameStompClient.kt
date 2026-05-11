package at.aau.se2.skyjo.network

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

class GameStompClient {
    private val stompClient = StompClient(OkHttpWebSocketClient())
    private var session: StompSession? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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

    suspend fun connect() {
        try {
            session = stompClient.connect("ws://10.0.2.2:8080/ws")
            _connectionError.value = null
            Log.d(TAG, "Connected successfully")
            scope.launch { collectLobby() }
            scope.launch { collectGame() }
            scope.launch { collectErrors() }
        } catch (e: Exception) {
            Log.e(TAG, "Connection error: ${e.message}")
            _connectionError.value = e.message ?: "Connection failed"
        }
    }

    private suspend fun collectLobby() {
        try {
            session?.subscribeText("/topic/lobby")?.collect { jsonText ->
                try {
                    _lobbyState.value = json.decodeFromString(jsonText)
                } catch (e: Exception) {
                    Log.e(TAG, "Lobby parse error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lobby subscribe error: ${e.message}")
        }
    }

    private suspend fun collectGame() {
        try {
            session?.subscribeText("/topic/game")?.collect { jsonText ->
                try {
                    _gameState.value = json.decodeFromString(jsonText)
                } catch (e: Exception) {
                    Log.e(TAG, "Game parse error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Game subscribe error: ${e.message}")
        }
    }

    private suspend fun collectErrors() {
        try {
            session?.subscribeText("/user/queue/errors")?.collect { jsonText ->
                try {
                    val errorMap = json.decodeFromString<Map<String, String>>(jsonText)
                    _errorMessage.tryEmit(errorMap["message"] ?: jsonText)
                } catch (e: Exception) {
                    _errorMessage.tryEmit(jsonText)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribe error: ${e.message}")
        }
    }

    fun joinLobby(playerName: String) {
        scope.launch {
            try {
                session?.sendText("/app/lobby.join", json.encodeToString(JoinLobbyMessage(playerName)))
            } catch (e: Exception) {
                Log.e(TAG, "Join lobby error: ${e.message}")
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

    fun disconnect() {
        scope.cancel()
    }

    companion object {
        private const val TAG = "GameStompClient"
    }
}
