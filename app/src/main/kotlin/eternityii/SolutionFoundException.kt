package eternityii

import eternityii.data.TileType

class SolutionFoundException(
    val placedTiles: List<UByte>,
    val placedOris: List<UByte>,
    val tileTypes: List<TileType>
): Exception()