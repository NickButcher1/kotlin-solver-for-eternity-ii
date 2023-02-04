package eternityii.backtracker

import eternityii.ProgressTracker
import eternityii.data.Colour
import eternityii.data.Compass
import eternityii.data.Orientation
import eternityii.data.TileData
import eternityii.data.TileType
import eternityii.data.ec
import eternityii.display.Display
import eternityii.display.fmt

/**
 * A backtracker for the complete edge only, but ignoring the mid colour on each edge tile,
 * which gives many duplicate tiles.
 *
 * Based on [Backtracker].
 *
 * For now, only north and west edges are checked for matches when placing a tile. There is no reason that south and
 * east matching can't be added.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class SwappableEdgeBacktracker(
    private val tileData: TileData,
    private val path: BacktrackerPath,
    private val maxDepth: Int,
    private val verboseMode: Boolean
) {
    /**
     * Whether each tile ID has been placed in placedTiles or not. Used to prevent placing duplicates.
     */
    private val placedCorners = UByteArray(4)

    /** The tile IDs that have been placed, with their orientations. Tile types are taken from [path]. */
    private val placedTiles = UByteArray(maxDepth)
    private val placedOris = UByteArray(maxDepth)

    private var numSolutions = 0L
    private val numSolutionsAtDepth = LongArray(maxDepth)

    /** Map from edgeId to the number of "identical" edge tiles, ignoring mid colour. */
    private val unplacedEdgesWithId: MutableMap<Int, Int> = run {
        // edgeId to number. Only the lowest edgeId is used.
        val newValuesMap = mutableMapOf<Int, Int>()

        tileData.edgesWithTwoEdgeColours.forEach { entry ->
            if (entry.value.isNotEmpty()) {
                newValuesMap[entry.value[0].toInt()] = entry.value.size
            }
        }
        println("unplacedEdgesWithId: $newValuesMap")
        newValuesMap
    }
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
        println("SwappableEdgeBackTracker: depth $maxDepth")
        addTile(0)

        reportProgress(progressTracker.elapsedTimeSeconds)
        progressTracker.cancel()
    }

    private fun addTile(depth: Int) {
        if (path.tileTypes[depth] == TileType.EDGE) {
            val edgesToTry = when (path.orientations[depth]) {
                Orientation.BASE -> {
                    val colour = colourOf(path.westIndex[depth], Compass.EAST).toInt()
                    tileData.reducedEdgesWithClockwiseColour[colour]
                }
                Orientation.CLOCKWISE_90 -> {
                    val colour = colourOf(path.northIndex[depth], Compass.SOUTH).toInt()
                    tileData.reducedEdgesWithClockwiseColour[colour]
                }
                Orientation.HALF -> {
                    val colour = colourOf(path.westIndex[depth], Compass.EAST).toInt()
                    tileData.reducedEdgesWithAnticlockwiseColour[colour]
                }
                Orientation.ANTICLOCKWISE_90 -> {
                    val colour = colourOf(path.northIndex[depth], Compass.SOUTH).toInt()
                    tileData.reducedEdgesWithAnticlockwiseColour[colour]
                }
                else -> throw IllegalArgumentException("Unexpected Orientation")
            }
            edgesToTry.forEach { id ->
                val intId = id.toInt()
                if (unplacedEdgesWithId[intId] != 0) {
                    if (RECORD_DEPTH_STATS) {
                        numSolutionsAtDepth[depth]++
                    }
                    if (depth == (maxDepth - 1)) {
                        numSolutions++
                        if (verboseMode) {
                            placedTiles[depth] = id
                            placedOris[depth] = path.orientations[depth]
                            val fixedPlacedTiles = fixPlacedTiles(depth)
                            Display.buildUrl(
                                tileData,
                                fixedPlacedTiles.map { it }.toList(),
                                placedOris.map { it }.toList(),
                                path.tileTypes,
                                path.fillOrder
                            )
                        }
                    } else {
                        unplacedEdgesWithId[intId] = unplacedEdgesWithId[intId]!! - 1
                        placedTiles[depth] = id
                        placedOris[depth] = path.orientations[depth]
                        addTile(depth + 1)
                        unplacedEdgesWithId[intId] = unplacedEdgesWithId[intId]!! + 1
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
                            val fixedPlacedTiles = fixPlacedTiles(depth)
                            Display.buildUrl(
                                tileData,
                                fixedPlacedTiles.map { it }.toList(),
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
                else -> throw IllegalArgumentException()
            }
        } else {
            Colour.MID_COLOUR_ANY
        }

    /**
     * Convert to a valid set of placed tiles, by replacing the interchangeable, duplicate edge IDs with unique edge IDs.
     */
    private fun fixPlacedTiles(depth: Int): UByteArray {
        val availableMap: MutableMap<Int, MutableList<Int>> = mutableMapOf()
        for (colourAnticlockwise in 0 until Colour.NUM_EDGE_COLOURS) {
            for (colourClockwise in 0 until Colour.NUM_EDGE_COLOURS) {
                val biColour = colourAnticlockwise.ec(colourClockwise)
                val edges = tileData.edgesWithTwoEdgeColours[biColour]
                if (edges != null && edges.isNotEmpty()) {
                    availableMap[edges[0].toInt()] = edges.map { it.toInt() }.toMutableList()
                }
            }
        }
        val fixedPlacedTiles = placedTiles.copyOf()
        for (idx in 0..depth) {
            if (path.tileTypes[idx] == TileType.EDGE) {
                fixedPlacedTiles[idx] = availableMap[placedTiles[idx].toInt()]!!.removeAt(0).toUByte()
            }
        }
        return fixedPlacedTiles
    }

    companion object {
        /** Hard coded, not configurable, so the compiler can optimise the code away when turned off. */
        private const val RECORD_DEPTH_STATS = true
    }
}
