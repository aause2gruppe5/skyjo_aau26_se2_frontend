package at.aau.se2.skyjo.game.model
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class BoardLayoutTest {
    @Test
    fun numberOfPositionsEqualsRowsTimesColumns() {
        val expectedTotal = BoardLayout.ROWS * BoardLayout.COLUMNS
        assertEquals(expectedTotal, BoardLayout.POSITIONS.size)
        assertEquals(12, BoardLayout.POSITIONS.size)
    }

    @Test
    fun verticalLinesAreColumns(){
        assertEquals(4, BoardLayout.VERTICAL_LINES.size) //wir erwarten 4 Spalten
        BoardLayout.VERTICAL_LINES.forEach { column -> assertEquals(3, column.size)} //jede Spalte hat drei ReihenElemente

        //erste Reihe wird als Stichprobe geprüft
        val firstColumn = BoardLayout.VERTICAL_LINES[0]
        assertEquals(BoardPosition(0,0), firstColumn[0])
        assertEquals(BoardPosition(1,0), firstColumn[1])
        assertEquals(BoardPosition(2,0), firstColumn[2])
    }

    @Test
    fun horizontalLinesAreRows(){
        assertEquals(3, BoardLayout.HORIZONTAL_LINES.size) //wir wollen 3 Reihen
        BoardLayout.HORIZONTAL_LINES.forEach { row -> assertEquals(4, row.size)} //jede Reihe hat 4 SpaltenElemente

        val firstRow = BoardLayout.HORIZONTAL_LINES[0]
        assertEquals(BoardPosition(0,0), firstRow[0])
        assertEquals(BoardPosition(0,1), firstRow[1])
        assertEquals(BoardPosition(0,2), firstRow[2])
        assertEquals(BoardPosition(0,3), firstRow[3])
    }

    @Test
    fun positionsAreUnique(){
        val uniquePositions = BoardLayout.POSITIONS.distinct().size
        assertEquals(BoardLayout.POSITIONS.size, uniquePositions)
    }
}