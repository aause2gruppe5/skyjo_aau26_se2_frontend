package at.aau.se2.skyjo.game.model

data class BoardPosition(
    val row: Int,
    val column: Int,
) {
    init {
        require(row in 0 until BoardLayout.ROWS) { "row must be between 0 and ${BoardLayout.ROWS - 1}" }
        require(column in 0 until BoardLayout.COLUMNS) { "column must be between 0 and ${BoardLayout.COLUMNS - 1}" }
    }
}
