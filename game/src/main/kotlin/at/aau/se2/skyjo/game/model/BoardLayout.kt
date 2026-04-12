package at.aau.se2.skyjo.game.model

object BoardLayout {
    const val ROWS: Int = 3
    const val COLUMNS: Int = 4

    val POSITIONS: List<BoardPosition> = buildList {
        for (row in 0 until ROWS) {
            for (column in 0 until COLUMNS) {
                add(BoardPosition(row, column))
            }
        }
    }

    val VERTICAL_LINES: List<List<BoardPosition>> =
        (0 until COLUMNS).map { column ->
            (0 until ROWS).map { row -> BoardPosition(row, column) }
        }

    val HORIZONTAL_LINES: List<List<BoardPosition>> =
        (0 until ROWS).map { row ->
            (0 until COLUMNS).map { column -> BoardPosition(row, column) }
        }
}
