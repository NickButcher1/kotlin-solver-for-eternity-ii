package eternityii.backtracker

import eternityii.ProgressTracker
import eternityii.data.Colour
import eternityii.data.Compass
import eternityii.data.Orientation
import eternityii.data.TileData
import eternityii.data.TileType
import eternityii.data.nw
import eternityii.display.Display
import eternityii.display.fmt

/**
 * A simple backtracker that uses a configurable path.
 *
 * For now, only north and west edges are checked for matches when placing a tile. There is no reason that south and
 * east matching can't be added.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class Backtracker(
    private val tileData: TileData,
    private val path: BacktrackerPath,
    private val maxDepth: Int,
    private val verboseMode: Boolean
) {
    /**
     * Whether each tile ID has been placed in placedTiles or not. Used to prevent placing duplicates.
     */
    private val placedCorners = UByteArray(4)
    private val placedEdges = UByteArray(56)
    private val placedMids = UByteArray(196)

    /** The tile IDs that have been placed, with their orientations. Tile types are taken from [path]. */
    private val placedTiles = UByteArray(maxDepth)
    private val placedOris = UByteArray(maxDepth)

    private var numSolutions = 0L
    private val numSolutionsAtDepth = LongArray(maxDepth)

    private val progressTracker: ProgressTracker = ProgressTracker(this::reportProgress)

    private fun reportProgress(elapsedTimeSeconds: Long) {
        if (RECORD_DEPTH_STATS) {
            println("DEPTH: ")
            for (depth in 0 until maxDepth) {
                println("    ${depth + 1} -> ${numSolutionsAtDepth[depth].fmt()}")
            }
        }

        val rate = if (elapsedTimeSeconds == 0L) { 0 } else { numSolutions / elapsedTimeSeconds }
        println(
            "Num solutions: ${numSolutions.fmt()} for depth $maxDepth in " +
                "$elapsedTimeSeconds seconds, ${rate.fmt()} per second"
        )
    }

    init {
        require(maxDepth >= 2) { "Depth less than 2 not supported." }
        require(maxDepth <= path.tileTypes.size) { "ERROR" }
        require(maxDepth <= path.orientations.size) { "ERROR" }
    }

    fun solve() {
        println("BackTracker: depth $maxDepth")
        addTile(0)

        reportProgress(progressTracker.elapsedTimeSeconds)
        progressTracker.cancel()
    }

    private fun addTile(depth: Int) {
        if (path.tileTypes[depth] == TileType.MID) {
            val eastColour = colourOf(path.westIndex[depth], Compass.EAST)
            val southColour = colourOf(path.northIndex[depth], Compass.SOUTH)
            val biColour = southColour.nw(eastColour)
            val possibleIds = tileData.midIdsWithTwoColours[biColour]!!
            for (idx in possibleIds.indices) {
                val id = possibleIds[idx]

                if (placedMids[id.toInt()] == 0.toUByte()) {
                    if (RECORD_DEPTH_STATS) {
                        numSolutionsAtDepth[depth]++
                    }
                    if (depth == (maxDepth - 1)) {
                        numSolutions++
                        if (verboseMode) {
                            placedTiles[depth] = id
                            placedOris[depth] = tileData.midOrisWithTwoColours[biColour]!![idx]
                            Display.buildUrl(
                                tileData,
                                placedTiles.map { it }.toList(),
                                placedOris.map { it }.toList(),
                                path.tileTypes,
                                path.fillOrder
                            )
                        }
                    } else {
                        placedMids[id.toInt()] = 1U
                        placedTiles[depth] = id
                        placedOris[depth] = tileData.midOrisWithTwoColours[biColour]!![idx]
                        addTile(depth + 1)
                        placedMids[id.toInt()] = 0U
                    }
                }
            }
        } else if (path.tileTypes[depth] == TileType.EDGE) {
            val edgesToTry = when (path.orientations[depth]) {
                Orientation.BASE -> {
                    val colour = colourOf(path.westIndex[depth], Compass.EAST).toInt()
                    tileData.edgesWithClockwiseColour[colour]
                }
                Orientation.CLOCKWISE_90 -> {
                    val colour = colourOf(path.northIndex[depth], Compass.SOUTH).toInt()
                    tileData.edgesWithClockwiseColour[colour]
                }
                Orientation.HALF -> {
                    val colour = colourOf(path.westIndex[depth], Compass.EAST).toInt()
                    tileData.edgesWithAnticlockwiseColour[colour]
                }
                Orientation.ANTICLOCKWISE_90 -> {
                    if (path is EdgeBacktrackerPath) {
                        val colour = colourOf(path.northIndex[depth], Compass.SOUTH).toInt()
                        tileData.edgesWithAnticlockwiseColour[colour]
                    } else {
                        val eastColour = colourOf(path.westIndex[depth], Compass.EAST)
                        val southColour = colourOf(path.northIndex[depth], Compass.SOUTH)
                        val biColour = southColour.nw(eastColour)
                        tileData.edgeIdsRhsWithTwoColours[biColour]!!
                    }
                }
                else -> throw IllegalArgumentException("Unexpected Orientation")
            }

            edgesToTry.forEach { id ->
                if (placedEdges[id.toInt()] == 0.toUByte()) {
                    if (RECORD_DEPTH_STATS) {
                        numSolutionsAtDepth[depth]++
                    }
                    if (depth == (maxDepth - 1)) {
                        numSolutions++
                        if (verboseMode) {
                            placedTiles[depth] = id
                            placedOris[depth] = path.orientations[depth]
                            Display.buildUrl(
                                tileData,
                                placedTiles.map { it }.toList(),
                                placedOris.map { it }.toList(),
                                path.tileTypes,
                                path.fillOrder
                            )
                        }
                    } else {
                        placedEdges[id.toInt()] = 1U
                        placedTiles[depth] = id
                        placedOris[depth] = path.orientations[depth]
                        addTile(depth + 1)
                        placedEdges[id.toInt()] = 0U
                    }
                }
            }
        } else if (path.tileTypes[depth] == TileType.CORNER) {
            val cornersToTry = when (path.orientations[depth]) {
                Orientation.BASE -> {
                    val colour = colourOf(path.northIndex[depth], Compass.SOUTH).toInt()
                    tileData.cornersWithClockwiseColour[colour]
                }
                Orientation.CLOCKWISE_90 -> {
                    ubyteArrayOf(0U, 1U, 2U, 3U)
                }
                Orientation.HALF -> {
                    val colour = colourOf(path.westIndex[depth], Compass.EAST).toInt()
                    tileData.cornersWithAnticlockwiseColour[colour]
                }
                Orientation.ANTICLOCKWISE_90 -> {
                    ubyteArrayOf(0U, 1U, 2U, 3U)
                }
                else -> throw IllegalArgumentException("Unexpected Orientation")
            }

            cornersToTry.forEach { id ->
                if (placedCorners[id.toInt()] == 0.toUByte()) {
                    if (RECORD_DEPTH_STATS) {
                        numSolutionsAtDepth[depth]++
                    }
                    if (depth == (maxDepth - 1)) {
                        numSolutions++
                        if (verboseMode) {
                            placedTiles[depth] = id
                            placedOris[depth] = path.orientations[depth]
                            Display.buildUrl(
                                tileData,
                                placedTiles.map { it }.toList(),
                                placedOris.map { it }.toList(),
                                path.tileTypes,
                                path.fillOrder
                            )
                        }
                    } else {
                        placedCorners[id.toInt()] = 1U
                        placedTiles[depth] = id
                        placedOris[depth] = path.orientations[depth]
                        addTile(depth + 1)
                        placedCorners[id.toInt()] = 0U
                    }
                }
            }
        }
    }

    private fun colourOf(oppositeIndex: Int, compass: UByte): UByte =
        if (oppositeIndex != -1) {
            val side = tileData.toSide(placedOris[oppositeIndex], compass)
            when (path.tileTypes[oppositeIndex]) {
                TileType.CORNER -> tileData.corners[side][placedTiles[oppositeIndex].toInt()]
                TileType.EDGE -> tileData.edges[side][placedTiles[oppositeIndex].toInt()]
                TileType.MID -> tileData.mids[side][placedTiles[oppositeIndex].toInt()]
            }
        } else {
            Colour.MID_COLOUR_ANY
        }

    companion object {
        /** Hard coded, not configurable, so the compiler can optimise the code away when turned off. */
        private const val RECORD_DEPTH_STATS = true
    }
}
