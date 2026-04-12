package at.aau.se2.skyjo.ui.game

import androidx.lifecycle.ViewModel
import at.aau.se2.skyjo.game.model.BoardPosition
import at.aau.se2.skyjo.game.model.GameState
import at.aau.se2.skyjo.game.service.SkyjoEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel : ViewModel() {

    private val engine = SkyjoEngine()

    private val _state = MutableStateFlow<GameState?>(null)
    val state: StateFlow<GameState?> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun startGame(playerNames: List<String>) {
        val initialReveals = playerNames.associateWith {
            setOf(BoardPosition(0, 0), BoardPosition(0, 1))
        }
        _state.value = engine.startGame(playerNames, initialReveals)
    }

    fun drawFromDeck() = runAction { engine.drawFromDeck(it) }

    fun takeDiscardCard() = runAction { engine.takeDiscardCard(it) }

    fun replaceDrawnCard(position: BoardPosition) = runAction { engine.replaceDrawnCard(it, position) }

    fun discardAndReveal(position: BoardPosition) = runAction { engine.discardDrawnCardAndReveal(it, position) }

    fun clearError() {
        _error.value = null
    }

    private fun runAction(action: (GameState) -> GameState) {
        val current = _state.value ?: return
        try {
            _state.value = action(current)
        } catch (e: Exception) {
            _error.value = e.message
        }
    }
}
