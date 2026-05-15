package at.aau.se2.skyjo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import at.aau.se2.skyjo.model.ActionCardParameters
import at.aau.se2.skyjo.model.GameAction
import at.aau.se2.skyjo.model.BoardLineTargetType
import at.aau.se2.skyjo.network.GameStompClient
import at.aau.se2.skyjo.model.PlayActionCardCommand
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val gameClient = GameStompClient(application)

    val lobbyState = gameClient.lobbyState
    val gameState = gameClient.gameState
    val actionCardResults = gameClient.actionCardResults
    val hasRejoinedGame = gameClient.hasRejoinedGame
    val errorMessage = gameClient.errorMessage
    val connectionError = gameClient.connectionError
    val isConnected = gameClient.isConnected

    private val _myPlayerName = MutableStateFlow("")
    val myPlayerName: StateFlow<String> = _myPlayerName.asStateFlow()

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
                        gameClient.reconnect(name)
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

    fun leaveLobby() {
        gameClient.leaveLobby()
        gameClient.disconnect()
        _myPlayerName.value = ""
    }

    fun startGame(maxRounds: Int = 3, targetScore: Int = 100) =
        gameClient.startGame(maxRounds, targetScore)

    fun drawFromDeck() = gameClient.sendAction(GameAction(type = "DRAW", source = "DECK"))

    fun drawFromActionDeck() = gameClient.sendAction(GameAction(type = "DRAW", source = "ACTION_DECK"))

    fun drawFromDiscard() = gameClient.sendAction(GameAction(type = "DRAW", source = "DISCARD"))

    fun replaceCard(row: Int, col: Int) =
        gameClient.sendAction(GameAction(type = "REPLACE", row = row, col = col))

    fun discardAndReveal(row: Int, col: Int) =
        gameClient.sendAction(GameAction(type = "DISCARD_AND_REVEAL", row = row, col = col))

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
        )
    )

    override fun onCleared() {
        super.onCleared()
        gameClient.close()
    }
}
