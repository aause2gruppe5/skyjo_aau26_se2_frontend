package at.aau.se2.skyjo.model
import kotlinx.serialization.Serializable

@Serializable
data class PlayerMessage(
    val playerName: String,
    val gameId: String? = null
)