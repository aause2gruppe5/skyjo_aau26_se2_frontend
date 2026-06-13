package at.aau.se2.skyjo.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class PlayActionCardCommand(
    val actionCardIndex: Int,
    val parameters: ActionCardParameters? = null,
)

@Serializable(with = ActionCardParametersSerializer::class)
sealed interface ActionCardParameters {
    @Serializable
    data object None : ActionCardParameters

    @Serializable
    data class BoardLineTarget(
        val targetPlayerId: String,
        val targetType: BoardLineTargetType,
        val lineIndex: Int,
    ) : ActionCardParameters

    @Serializable
    data class DrawThreeCardsChoice(
        val mode: DrawThreeCardsChoiceMode,
        val chosenDrawnCardIndex: Int? = null,
        val targetRow: Int? = null,
        val targetColumn: Int? = null,
        val revealRow: Int? = null,
        val revealColumn: Int? = null,
        val discardOrder: List<DrawThreeCardsDiscardReference>,
        val targetPlayerId: String? = null,
    ) : ActionCardParameters
}

private object ActionCardParametersSerializer :
    JsonContentPolymorphicSerializer<ActionCardParameters>(ActionCardParameters::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ActionCardParameters> {
        val fields = element.jsonObject
        return when {
            "discardOrder" in fields -> ActionCardParameters.DrawThreeCardsChoice.serializer()
            "targetType" in fields || "lineIndex" in fields -> ActionCardParameters.BoardLineTarget.serializer()
            else -> ActionCardParameters.None.serializer()
        }
    }
}

@Serializable
enum class BoardLineTargetType {
    ROW,
    COLUMN,
}

@Serializable
enum class DrawThreeCardsChoiceMode {
    KEEP_ONE_AND_SWAP,
    DISCARD_ALL_AND_REVEAL,
}

@Serializable
enum class DrawThreeCardsDiscardReference {
    DRAWN_CARD_0,
    DRAWN_CARD_1,
    DRAWN_CARD_2,
    SWAPPED_BOARD_CARD,
}

@Serializable
data class ActionCardResultMessage(
    val type: String,
    val actionCardIndex: Int,
    val targetPlayerId: String? = null,
    val targetType: BoardLineTargetType? = null,
    val lineIndex: Int? = null,
    val inspectedValues: List<Int?> = emptyList(),
    val inspectedCards: List<InspectedCard> = emptyList(),
    val drawnCards: List<Card> = emptyList(),
)

@Serializable
data class InspectedCard(
    val row: Int,
    val col: Int,
    val value: Int?,
    val card: Card? = null,
)
