package at.aau.se2.skyjo.model

import kotlinx.serialization.Serializable

@Serializable
data class LobbyPlayer(
    val nickname: String,
    val isHost: Boolean
)

@Serializable
data class LobbyUpdateMessage(
    val players: List<LobbyPlayer>,
    val status: String,
    val maxPlayers: Int
)
