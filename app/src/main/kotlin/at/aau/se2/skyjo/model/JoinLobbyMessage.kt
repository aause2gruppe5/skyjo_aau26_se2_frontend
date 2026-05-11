package at.aau.se2.skyjo.model

import kotlinx.serialization.Serializable

@Serializable
data class JoinLobbyMessage(val playerName: String)
