package eternityii.builder

import java.io.File

/**
 * Represents a partial solution.
 *
 * Similar to [Partial] but the ids lists use 1x4 edge block IDs, not tile IDs.
 *
 * @property depth The number of tiles in this partial.
 * @property ids A list of lists of edge block IDs. Each entry in this list represents one partial.
 */
data class PartialEdge(
    val depth: Int,
    var ids: List<IntArray> = listOf(),
) {
    fun toFile(filename: String) {
        var lastId1 = -1
        File("../output/$filename").printWriter().use { out ->
            out.println(depth)
            out.println(ids.size)
            ids.forEach { id ->
                if (id[0] != lastId1) {
                    out.println(id[0])
                    lastId1 = id[0]
                }
                out.println(" ${id[1]}")
            }
        }
    }

    companion object {
        fun fromFile(filename: String): PartialEdge {
            val inputLines: List<String> = File("../output/$filename").useLines { it.toList() }
            val depth = inputLines[0].toInt()
            val numEntries = inputLines[1].toInt()
            // TODO: Fix to handle IDs correctly.
            val ids: List<IntArray> =
                (2 until (numEntries + 2)).map { idx ->
                    inputLines[idx].split(" ").map { it.toInt() }.toIntArray()
                }.toList()

            return PartialEdge(depth, ids)
        }
    }
}
