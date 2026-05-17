package at.aau.se2.skyjo.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
}
