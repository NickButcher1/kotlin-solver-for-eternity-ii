package eternityii

data class Cell(val row: Int, val col: Int)

/** Iterates over all cells from top left corner to bottom right corner, row by row. */
class CellIterator : Iterator<Cell> {
    private val iterator = ALL_CELLS.iterator()

    override fun hasNext(): Boolean = iterator.hasNext()

    override fun next(): Cell = iterator.next()

    companion object {
        private val ALL_CELLS = (0..15).map { row ->
            (0..15).map { col ->
                Cell(row, col)
            }.toList()
        }.toList().flatten()
    }
}
