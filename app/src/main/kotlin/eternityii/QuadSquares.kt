@file:OptIn(ExperimentalUnsignedTypes::class)

package eternityii

import eternityii.data.Colour
import eternityii.data.TileData
import eternityii.data.TileType
import java.io.File

/**
 * Represents a set of tiles up to a 3x3 block in size.
 *
 * The tiles are in this order.
 *     014
 *     235
 *     678
 *
 * @property oris A list of lists of orientations. Each entry in this list represents one block.
 * @property ids A list of lists of tile IDs. Each entry in this list represents one block.
 * @property tileTypes A list of tile types for the four tiles.
 */
data class QuadSquares(
    var oris: List<List<UByte>>,
    var ids: List<List<UByte>>,
    var tileTypes: List<TileType>
) {
    fun colourOf(tileData: TileData, idx: Int, oppositeIndex: Int, compass: UByte): UByte =
        if (oppositeIndex != -1) {
            val side = tileData.toSide(oris[idx][oppositeIndex], compass)
            when (tileTypes[oppositeIndex]) {
                TileType.CORNER -> tileData.corners[side][ids[idx][oppositeIndex].toInt()]
                TileType.EDGE -> tileData.edges[side][ids[idx][oppositeIndex].toInt()]
                TileType.MID -> tileData.mids[side][ids[idx][oppositeIndex].toInt()]
            }
        } else {
            Colour.MID_COLOUR_ANY
        }

    fun toFile(filename: String) {
        File("../output/$filename").printWriter().use { out ->
            out.println("4")
            out.println(ids.size)
            out.println(tileTypes.joinToString(" "))
            ids.indices.forEach { idx ->
                out.println(
                    "${Files.formatByteListForFile(ids[idx])} " +
                        Files.formatByteListForFile(oris[idx])
                )
            }
        }
    }
}
