package eternityii.data

/**
 * Represents the mandatory tile or one of the four hint tiles.
 *
 * tileId is in range 1-255 but midId, x and y are all zero-based.
 */
data class Clue(
    val tileId: TileId,
    val midId: UByte,
    val orientation: UByte,
    val row: Int,
    val col: Int,
)
