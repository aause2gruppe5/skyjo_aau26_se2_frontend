package at.aau.se2.skyjo.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ActionCardModelTest {

    @Test
    fun `play action card command defaults to no parameters`() {
        val command = PlayActionCardCommand(actionCardIndex = 2)

        assertEquals(2, command.actionCardIndex)
        assertNull(command.parameters)
    }

    @Test
    fun `action card result defaults inspected collections to empty`() {
        val result = ActionCardResultMessage(
            type = "ENLIGHTENMENT",
            actionCardIndex = 1,
            targetPlayerId = "p2",
            targetType = BoardLineTargetType.COLUMN,
            lineIndex = 2,
        )

        assertEquals(emptyList<Int?>(), result.inspectedValues)
        assertEquals(emptyList<InspectedCard>(), result.inspectedCards)
    }

    @Test
    fun `inspected card defaults nested card to null`() {
        val inspectedCard = InspectedCard(row = 1, col = 3, value = null)

        assertEquals(1, inspectedCard.row)
        assertEquals(3, inspectedCard.col)
        assertNull(inspectedCard.value)
        assertNull(inspectedCard.card)
    }

    @Test
    fun `draw three cards private result decodes drawn cards without enlightenment target`() {
        val result = Json { ignoreUnknownKeys = true }.decodeFromString<ActionCardResultMessage>(
            """
            {
              "type": "DRAW_THREE_CARDS",
              "actionCardIndex": 0,
              "drawnCards": [
                { "id": 1, "value": 5, "type": "NUMBER" },
                { "id": 2, "value": -1, "type": "NUMBER" },
                { "id": 3, "value": 12, "type": "NUMBER" }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("DRAW_THREE_CARDS", result.type)
        assertEquals(0, result.actionCardIndex)
        assertNull(result.targetPlayerId)
        assertNull(result.targetType)
        assertEquals(listOf(5, -1, 12), result.drawnCards.map { it.value })
    }

    @Test
    fun `draw three cards choice command encodes flat backend parameters`() {
        val command = PlayActionCardCommand(
            actionCardIndex = 0,
            parameters = ActionCardParameters.DrawThreeCardsChoice(
                mode = DrawThreeCardsChoiceMode.KEEP_ONE_AND_SWAP,
                chosenDrawnCardIndex = 1,
                targetRow = 2,
                targetColumn = 3,
                discardOrder = listOf(
                    DrawThreeCardsDiscardReference.DRAWN_CARD_0,
                    DrawThreeCardsDiscardReference.DRAWN_CARD_2,
                    DrawThreeCardsDiscardReference.SWAPPED_BOARD_CARD,
                ),
            ),
        )

        val encoded = Json { explicitNulls = false }.encodeToString(command)

        assertTrue(encoded.contains("\"actionCardIndex\":0"))
        assertTrue(encoded.contains("\"mode\":\"KEEP_ONE_AND_SWAP\""))
        assertTrue(encoded.contains("\"chosenDrawnCardIndex\":1"))
        assertTrue(encoded.contains("\"targetRow\":2"))
        assertTrue(encoded.contains("\"targetColumn\":3"))
        assertTrue(encoded.contains("\"discardOrder\":[\"DRAWN_CARD_0\",\"DRAWN_CARD_2\",\"SWAPPED_BOARD_CARD\"]"))
        assertTrue(!encoded.contains("DrawThreeCardsChoice"))
    }
}
