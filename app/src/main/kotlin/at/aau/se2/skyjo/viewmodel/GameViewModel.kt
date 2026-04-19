package at.aau.se2.skyjo.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.aau.se2.skyjo.network.GameStompClient
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    //Erstellt den Client genau EINMAL
    private val gameClient = GameStompClient()

    // Stellt den "Radiosender" für die UI bereit
    val messages = gameClient.messages

    init {
        //Sobald das ViewModel existiert, wird die Verbindung gestartet
        gameClient.connect()
    }

    //Funktionen für die Buttons in der App
    fun joinGame(playerName: String) {
        gameClient.joinGame(playerName)
    }

    fun leaveGame() {
        gameClient.leaveGame()
    }

    //Wenn das ViewModel beendet wird (App geschlossen), aufräumen
    override fun onCleared() {
        super.onCleared()
        leaveGame() //Schickt ein Leave-Signal an den Server
    }
}