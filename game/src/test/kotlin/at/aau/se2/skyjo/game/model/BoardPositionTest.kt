package at.aau.se2.skyjo.game.model
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BoardPositionTest {
    @Test
    fun validPositions() {
        val minPos =  BoardPosition(0,0)
        assertEquals(0, minPos.row)
        assertEquals(0,minPos.column)

        val maxPos =  BoardPosition(BoardLayout.ROWS - 1,BoardLayout.COLUMNS - 1)
        assertEquals(BoardLayout.COLUMNS - 1 ,maxPos.column)
        assertEquals(BoardLayout.ROWS - 1,maxPos.row)
    }

    @Test
    fun rowNegative(){
        val exeption = assertThrows<IllegalArgumentException>{ BoardPosition(-1,0) }
        assertEquals("row must be between 0 and ${BoardLayout.ROWS - 1}", exeption.message)
    }

    @Test
    fun rowBiggerThenAllowed(){
        val exeption = assertThrows<IllegalArgumentException>{ BoardPosition(BoardLayout.ROWS, 0) }
        assertEquals("row must be between 0 and ${BoardLayout.ROWS - 1}", exeption.message)
    }

    @Test
    fun columnNegative(){
        val exeption = assertThrows<IllegalArgumentException>{ BoardPosition(0,-1) }
        assertEquals("column must be between 0 and ${BoardLayout.COLUMNS - 1}", exeption.message)
    }

    @Test
    fun columnBiggerThenAllowed(){
        val exception = assertThrows<IllegalArgumentException>{ BoardPosition(0, BoardLayout.COLUMNS) }
        assertEquals("column must be between 0 and ${BoardLayout.COLUMNS - 1}", exception.message)
    }

    @Test
    fun equalityWorks(){
        val pos1 = BoardPosition(1,2)
        val pos2 = BoardPosition(1,2)
        val pos3 = BoardPosition(2,1)

        assertEquals(pos1, pos2)
        assertNotEquals(pos3, pos1)
    }
}