package at.aau.se2.skyjo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.aau.se2.skyjo.model.GameAction
import at.aau.se2.skyjo.network.GameStompClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val gameClient = GameStompClient()

    val lobbyState = gameClient.lobbyState
    val gameState = gameClient.gameState
    val errorMessage = gameClient.errorMessage
    val connectionError = gameClient.connectionError

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

    fun connect(playerName: String) {
        _myPlayerName.value = playerName
        viewModelScope.launch {
            gameClient.connect()
            gameClient.joinLobby(playerName)
        }
    }

    fun leaveLobby() = gameClient.leaveLobby()

    fun startGame(maxRounds: Int = 3, targetScore: Int = 100) =
        gameClient.startGame(maxRounds, targetScore)

    fun drawFromDeck() = gameClient.sendAction(GameAction(type = "DRAW", source = "DECK"))

    fun drawFromDiscard() = gameClient.sendAction(GameAction(type = "DRAW", source = "DISCARD"))

    fun replaceCard(row: Int, col: Int) =
        gameClient.sendAction(GameAction(type = "REPLACE", row = row, col = col))

    fun discardAndReveal(row: Int, col: Int) =
        gameClient.sendAction(GameAction(type = "DISCARD_AND_REVEAL", row = row, col = col))

    override fun onCleared() {
        super.onCleared()
        gameClient.disconnect()
    }
}
