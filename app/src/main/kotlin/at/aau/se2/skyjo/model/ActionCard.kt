package at.aau.se2.skyjo.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayActionCardCommand(
    val actionCardIndex: Int,
    val parameters: ActionCardParameters.BoardLineTarget,
)

sealed interface ActionCardParameters {

    @Serializable
    data class BoardLineTarget(
        val targetPlayerId: String,
        val targetType: BoardLineTargetType,
        val lineIndex: Int,
    ) : ActionCardParameters
}

@Serializable
enum class BoardLineTargetType {
    ROW,
    COLUMN,
}

@Serializable
data class ActionCardResultMessage(
    val type: String,
    val actionCardIndex: Int,
    val targetPlayerId: String,
    val targetType: BoardLineTargetType,
    val lineIndex: Int,
    val inspectedValues: List<Int?> = emptyList(),
    val inspectedCards: List<InspectedCard> = emptyList(),
)

@Serializable
data class InspectedCard(
    val row: Int,
    val col: Int,
    val value: Int?,
    val card: Card? = null,
)
