@file:OptIn(ExperimentalUnsignedTypes::class)

package eternityii.builder

import eternityii.Files
import eternityii.data.TileType
import java.io.File

/**
 * Represents a partial solution.
 *
 * @property depth The number of tiles in this partial.
 * @property oris A list of orientations of the tiles in the lists in ids. All lists have the same
 * orientations.
 * @property ids A list of lists of tile IDs. Each entry in this list represents one partial.
 */
data class Partial(
    val depth: Int,
    var tileTypes: List<TileType> = listOf(),
    var oris: List<UByte> = listOf(),
    var ids: List<UByteArray> = listOf(),
) {
    fun toFile(filename: String) {
        File("../output/$filename").printWriter().use { out ->
            out.println(depth)
            out.println(ids.size)
            out.println(tileTypes.joinToString(" "))
            out.println(oris.joinToString(" "))
            ids.forEach { idArray ->
                out.println(Files.formatByteArrayForFile(idArray))
            }
        }
    }

    companion object {
        fun fromFile(filename: String): Partial {
            val inputLines: List<String> = File("../output/$filename").useLines { it.toList() }
            val depth = inputLines[0].toInt()
            val numEntries = inputLines[1].toInt()
            val tileTypes: List<TileType> = inputLines[2].split(" ").map { entry ->
                when (entry) {
                    TileType.CORNER.name -> TileType.CORNER
                    TileType.EDGE.name -> TileType.EDGE
                    TileType.MID.name -> TileType.MID
                    else -> throw IllegalArgumentException("Invalid orientation in file.")
                }
            }
            val oris: List<UByte> = inputLines[3].split(" ").map { entry ->
                entry.toUByte()
            }
            val ids: List<UByteArray> =
                (4 until (numEntries + 4)).map { idx ->
                    Files.readByteArrayFromFile(inputLines[idx])
                }.toList()

            return Partial(depth, tileTypes, oris, ids)
        }
    }
}
