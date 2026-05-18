package at.aau.se2.skyjo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import at.aau.se2.skyjo.model.ActionCardParameters
import at.aau.se2.skyjo.model.BoardLineTargetType
import at.aau.se2.skyjo.model.GameAction
import at.aau.se2.skyjo.model.LobbyUpdateMessage
import at.aau.se2.skyjo.model.PlayActionCardCommand
import at.aau.se2.skyjo.model.stats.PlayerStatsDto
import at.aau.se2.skyjo.network.GameRealtimeClient
import at.aau.se2.skyjo.network.GameStompClient
import at.aau.se2.skyjo.network.SkyjoApi
import at.aau.se2.skyjo.network.SkyjoApiClient
import at.aau.se2.skyjo.session.EncryptedSessionStore
import at.aau.se2.skyjo.session.SessionStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(
    application: Application,
    private val apiClient: SkyjoApi,
    private val gameClient: GameRealtimeClient,
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, EncryptedSessionStore(application))

    private constructor(
        application: Application,
        sessionStore: SessionStore,
    ) : this(application, SkyjoApiClient(sessionStore), GameStompClient(application))

    val lobbyState = gameClient.lobbyState
    val gameState = gameClient.gameState
    val actionCardResults = gameClient.actionCardResults
    val incomingInvites = gameClient.incomingInvites
    val hasRejoinedGame = gameClient.hasRejoinedGame
    val errorMessage = gameClient.errorMessage
    val connectionError = gameClient.connectionError
    val isConnected = gameClient.isConnected

    private val _myPlayerName = MutableStateFlow("")
    val myPlayerName: StateFlow<String> = _myPlayerName.asStateFlow()

    private val _homeStats = MutableStateFlow<PlayerStatsDto?>(null)
    val homeStats: StateFlow<PlayerStatsDto?> = _homeStats.asStateFlow()

    private val _lobbyError = MutableStateFlow<String?>(null)
    val lobbyError: StateFlow<String?> = _lobbyError.asStateFlow()

    val isHost: StateFlow<Boolean> = combine(lobbyState, myPlayerName) { lobby, name ->
        name.isNotEmpty() && lobby?.players?.firstOrNull()?.nickname == name
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val myPlayerId: StateFlow<String?> = combine(gameState, myPlayerName) { game, name ->
        game?.players?.find { it.nickname == name }?.playerId
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isMyTurn: StateFlow<Boolean> = combine(gameState, myPlayerId) { game, myId ->
        myId != null && game?.currentPlayerId == myId
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            isConnected
                .drop(1)
                .distinctUntilChanged()
                .filter { !it }
                .collect {
                    val name = _myPlayerName.value
                    if (name.isNotEmpty()) {
                        runCatching {
                            val ticket = apiClient.createWebSocketTicket().ticket
                            gameClient.connect(ticket = ticket, lobbyJoinCode = lobbyState.value?.joinCode)
                        }
                    }
                }
        }
    }

    fun connect(playerName: String) {
        _myPlayerName.value = playerName
        viewModelScope.launch {
            gameClient.connect()
            gameClient.joinLobby(playerName)
        }
    }

    fun setAuthenticatedUsername(username: String) {
        _myPlayerName.value = username
    }

    fun refreshHomeStats() {
        viewModelScope.launch {
            runCatching { apiClient.myStats() }
                .onSuccess { _homeStats.value = it }
        }
    }

    fun createLobby(username: String) {
        _myPlayerName.value = username
        viewModelScope.launch {
            _lobbyError.value = null
            runCatching {
                val lobby = apiClient.createLobby()
                connectToLobby(lobby.joinCode)
                gameClient.applyLobbyState(
                    LobbyUpdateMessage(
                        lobbyId = lobby.lobbyId,
                        joinCode = lobby.joinCode,
                        players = lobby.players,
                        status = lobby.status,
                        maxPlayers = lobby.maxPlayers,
                    ),
                )
            }.onFailure { error ->
                _lobbyError.value = error.message ?: "Lobby konnte nicht erstellt werden"
            }
        }
    }

    fun joinLobbyByCode(username: String, joinCode: String) {
        _myPlayerName.value = username
        viewModelScope.launch {
            _lobbyError.value = null
            runCatching {
                val lobby = apiClient.joinLobby(joinCode)
                connectToLobby(lobby.joinCode)
                gameClient.applyLobbyState(
                    LobbyUpdateMessage(
                        lobbyId = lobby.lobbyId,
                        joinCode = lobby.joinCode,
                        players = lobby.players,
                        status = lobby.status,
                        maxPlayers = lobby.maxPlayers,
                    ),
                )
            }.onFailure { error ->
                _lobbyError.value = error.message ?: "Lobby konnte nicht betreten werden"
            }
        }
    }

    fun acceptLobbyInvite(username: String, inviteId: String) {
        _myPlayerName.value = username
        viewModelScope.launch {
            runCatching {
                val invite = apiClient.acceptLobbyInvite(inviteId)
                val lobby = apiClient.currentLobby()
                connectToLobby(invite.joinCode)
                lobby?.let {
                    gameClient.applyLobbyState(
                        LobbyUpdateMessage(
                            lobbyId = it.lobbyId,
                            joinCode = it.joinCode,
                            players = it.players,
                            status = it.status,
                            maxPlayers = it.maxPlayers,
                        ),
                    )
                }
            }.onFailure { error ->
                _lobbyError.value = error.message ?: "Einladung konnte nicht angenommen werden"
            }
        }
    }

    fun leaveLobby() {
        lobbyState.value?.lobbyId?.let { lobbyId ->
            viewModelScope.launch { runCatching { apiClient.leaveLobby(lobbyId) } }
        } ?: gameClient.leaveLobby()
        gameClient.clearStoredGame()
        gameClient.disconnect()
        _myPlayerName.value = ""
    }

    fun startGame(maxRounds: Int = 3, targetScore: Int = 100) =
        gameClient.startGame(maxRounds, targetScore)

    fun drawFromDeck() = gameClient.sendAction(GameAction(type = "DRAW", source = "DECK"))

    fun drawFromActionDeck() = gameClient.sendAction(GameAction(type = "DRAW", source = "ACTION_DECK"))

    fun drawVisibleActionCard(actionCardIndex: Int) =
        gameClient.sendAction(GameAction(type = "DRAW_VISIBLE_ACTION_CARD", actionCardIndex = actionCardIndex))

    fun drawFromDiscard() = gameClient.sendAction(GameAction(type = "DRAW", source = "DISCARD"))

    fun replaceCard(row: Int, col: Int) =
        gameClient.sendAction(GameAction(type = "REPLACE", row = row, col = col))

    fun discardAndReveal(row: Int, col: Int) =
        gameClient.sendAction(GameAction(type = "DISCARD_AND_REVEAL", row = row, col = col))

    fun playActionCard(actionCardIndex: Int) =
        gameClient.sendAction(GameAction(type = "PLAY_ACTION_CARD", actionCardIndex = actionCardIndex))

    fun playActionCard(command: PlayActionCardCommand) =
        gameClient.playActionCard(command)

    fun playEnlightenment(
        actionCardIndex: Int,
        targetPlayerId: String,
        targetType: BoardLineTargetType,
        lineIndex: Int,
    ) = gameClient.playActionCard(
        PlayActionCardCommand(
            actionCardIndex = actionCardIndex,
            parameters = ActionCardParameters.BoardLineTarget(
                targetPlayerId = targetPlayerId,
                targetType = targetType,
                lineIndex = lineIndex,
            ),
        ),
    )

    fun playPlayerSwapCard(
        actionCardIndex: Int,
        player1Id: String,
        player1Row: Int,
        player1Col: Int,
        player2Id: String,
        player2Row: Int,
        player2Col: Int,
    ) = gameClient.sendAction(
        GameAction(
            type = "PLAY_ACTION_CARD",
            actionCardIndex = actionCardIndex,
            targetPlayer1Id = player1Id,
            targetPlayer1Row = player1Row,
            targetPlayer1Col = player1Col,
            targetPlayer2Id = player2Id,
            targetPlayer2Row = player2Row,
            targetPlayer2Col = player2Col,
        ),
    )

    fun playSwapOwnCards(
        actionCardIndex: Int,
        firstRow: Int,
        firstCol: Int,
        secondRow: Int,
        secondCol: Int,
    ) = gameClient.sendAction(
        GameAction(
            type = "PLAY_ACTION_CARD",
            actionCardIndex = actionCardIndex,
            targetPlayer1Row = firstRow,
            targetPlayer1Col = firstCol,
            targetPlayer2Row = secondRow,
            targetPlayer2Col = secondCol,
        ),
    )

    fun discardActionCard(actionCardIndex: Int) =
        gameClient.sendAction(GameAction(type = "DISCARD_ACTION_CARD", actionCardIndex = actionCardIndex))

    override fun onCleared() {
        super.onCleared()
        gameClient.close()
    }

    private suspend fun connectToLobby(joinCode: String) {
        val ticket = apiClient.createWebSocketTicket().ticket
        gameClient.connect(ticket = ticket, lobbyJoinCode = joinCode)
    }
}
