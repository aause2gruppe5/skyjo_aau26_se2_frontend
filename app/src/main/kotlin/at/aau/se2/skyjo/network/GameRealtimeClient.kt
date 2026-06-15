package at.aau.se2.skyjo.network

import at.aau.se2.skyjo.model.ActionCardResultMessage
import at.aau.se2.skyjo.model.CheatPeekResultMessage
import at.aau.se2.skyjo.model.CheatReportResultMessage
import at.aau.se2.skyjo.model.GameAction
import at.aau.se2.skyjo.model.GameUpdateMessage
import at.aau.se2.skyjo.model.LobbyUpdateMessage
import at.aau.se2.skyjo.model.PlayActionCardCommand
import at.aau.se2.skyjo.model.social.LobbyInviteDto
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface GameRealtimeClient {
    val lobbyState: StateFlow<LobbyUpdateMessage?>
    val gameState: StateFlow<GameUpdateMessage?>
    val actionCardResults: SharedFlow<ActionCardResultMessage>
    val cheatPeekResults: SharedFlow<CheatPeekResultMessage>
    val cheatReportResults: SharedFlow<CheatReportResultMessage>
    val incomingInvites: SharedFlow<LobbyInviteDto>
    val errorMessage: SharedFlow<String>
    val connectionError: StateFlow<String?>
    val isConnected: StateFlow<Boolean>
    val hasRejoinedGame: StateFlow<Boolean>

    suspend fun connect()
    suspend fun connect(ticket: String?, lobbyJoinCode: String?)
    suspend fun connectForInvites(ticket: String?)
    suspend fun reconnect(playerName: String)
    fun applyLobbyState(lobby: LobbyUpdateMessage)
    fun joinLobby(playerName: String, gameId: String? = null)
    fun leaveLobby()
    fun startGame(maxRounds: Int, targetScore: Int)
    fun sendAction(action: GameAction)
    fun playActionCard(command: PlayActionCardCommand)
    fun cheatPeekDrawPile()
    fun cheatReportCurrentPlayer()
    fun clearStoredGame()
    fun disconnect()
    fun close()
}
