package at.aau.se2.skyjo.model
import kotlinx.serialization.Serializable

@Serializable
data class ServerMessage(
    val type: MessageType,
    val content: String,
    val playerName: String? = null
)

@Serializable
enum class MessageType {
    PLAYER_JOINED,
    PLAYER_LEFT,
    ERROR,
    INFO
}