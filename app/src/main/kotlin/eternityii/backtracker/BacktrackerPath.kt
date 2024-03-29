package eternityii.backtracker

import eternityii.CellIterator
import eternityii.data.Orientation
import eternityii.data.TileType

/**
 * Implementations specify the path for the [Backtracker] to use.
 */
abstract class BacktrackerPath(
    val fillOrder: List<Int>,
) {
    /** The [TileType] for each cell in [fillOrder]. Autogenerated. */
    val tileTypes: List<TileType>

    /** The orientation for each cell in [fillOrder]. 99U (unused) for mids. Autogenerated. */
    val orientations: List<UByte>

    /** The index in [fillOrder] for the tile to the West of this tile. -1 if no tile. Autogenerated. */
    val westIndex: List<Int>

    /** The index in [fillOrder] for the tile to the North of this tile. -1 if no tile. Autogenerated. */
    val northIndex: List<Int>

    init {
        require(fillOrder.size == 256)

        // Build a map from placement order to (row,col).
        val cellMap: MutableMap<Int, Int> = mutableMapOf()

        CellIterator().forEach { (row, col) ->
            val idx = row * 16 + col
            cellMap[fillOrder[idx]] = idx
        }

        tileTypes = (0..255)
            .filter { idx -> cellMap[idx] != null }
            .map { idx ->
                val cell = cellMap[idx]!!
                val row = cell / 16
                val col = cell % 16
                when {
                    row == 0 && col == 0 -> TileType.CORNER
                    row == 0 && col == 15 -> TileType.CORNER
                    row == 15 && col == 0 -> TileType.CORNER
                    row == 15 && col == 15 -> TileType.CORNER
                    row == 0 || row == 15 -> TileType.EDGE
                    col == 0 || col == 15 -> TileType.EDGE
                    else -> TileType.MID
                }
            }.toList()

        orientations = (0..255)
            .filter { idx -> cellMap[idx] != null }
            .map { idx ->
                val cell = cellMap[idx]!!
                val row = cell / 16
                val col = cell % 16
                when {
                    row == 0 && col == 0 -> Orientation.CLOCKWISE_90
                    row == 0 && col == 15 -> Orientation.HALF
                    row == 15 && col == 0 -> Orientation.BASE
                    row == 15 && col == 15 -> Orientation.ANTICLOCKWISE_90
                    row == 0 -> Orientation.HALF
                    row == 15 -> Orientation.BASE
                    col == 0 -> Orientation.CLOCKWISE_90
                    col == 15 -> Orientation.ANTICLOCKWISE_90
                    else -> 99U
                }
            }.toList()

        westIndex = (0..255)
            .filter { idx -> cellMap[idx] != null }
            .map { idx ->
                val cell = cellMap[idx]!!
                val col = cell % 16
                if (col == 0) {
                    -1
                } else {
                    fillOrder[cell - 1]
                }
            }.toList()

        northIndex = (0..255)
            .filter { idx -> cellMap[idx] != null }
            .map { idx ->
                val cell = cellMap[idx]!!
                val row = cell / 16
                if (row == 0) {
                    -1
                } else {
                    fillOrder[cell - 16]
                }
            }.toList()
    }
}
