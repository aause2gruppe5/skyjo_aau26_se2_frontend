package at.aau.se2.skyjo.network

import at.aau.se2.skyjo.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.hildan.krossbow.stomp.*
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient

class GameStompClient {
    //Krossbow STOMP Client mit OkHttp
    private val stompClient = StompClient(OkHttpWebSocketClient())
    private var session: StompSession? = null

    //Hintergrung für die Coroutines
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    //JSON Übersetzter
    private val json = Json {ignoreUnknownKeys = true}

    //Sender für UI
    private val _messages = MutableSharedFlow<ServerMessage>(replay = 1)
    val messages: SharedFlow<ServerMessage> = _messages.asSharedFlow()

    fun connect(){
        scope.launch {
            try{
                //Verbindung Aufabuen (Localhost von Android Emulator)
                session = stompClient.connect("ws://10.0.2.2:8080/ws")
                println("STOMP: Connected Sucsessfully")
                //sofort öffentlichen Kanal "zuhören"
                subscribeToPublicTopic()
            }catch(e: Exception){
                println("STOMP Error: ${e.message}")
            }
        }
    }

    private suspend fun subscribeToPublicTopic(){
        //höre auf /topic/public
        session?.subscribeText("/topic/public")?.collect{ jsonText ->
            try{
                // Wandelt das ankommende JSON direkt in die Kotlin ServerMessage um
                val message = json.decodeFromString<ServerMessage>(jsonText)
                _messages.emit(message) //leitet Nachricht an App/UI weiter
                println("Server says: ${message.content}")
            }catch (e: Exception){
                println("JSON Parsing Error: ${e.message}")
            }
        }
    }

    fun joinGame(playerName: String){
        scope.launch{
            val playerMsg = PlayerMessage(playerName = playerName)
            val jsonText = json.encodeToString(playerMsg)

            //Sende an /app/game.join (Präfix + @MessageMapping)
            session?.sendText("/app/game.join", jsonText)
        }
    }

    fun leaveGame(){
        scope.launch{
            //Sende leeren Text an /app/game.leave, da der Controller kein @Payload erwartet
            session?.sendText("/app/game.leave", "")
        }
    }
}