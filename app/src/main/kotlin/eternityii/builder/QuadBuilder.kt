package eternityii.builder

import eternityii.QuadSquares
import eternityii.backtracker.ScanrowSquaresBacktrackerPath
import eternityii.data.Colour
import eternityii.data.Compass
import eternityii.data.Orientation
import eternityii.data.TileData
import eternityii.data.TileType
import eternityii.data.nw
import eternityii.display.Display
import eternityii.display.fmt

/**
 * Build all ways to place 4 mid tiles into a 2x2 block.
 *
 * Excludes rotations of the same 2x2 block.
 *
 */
@OptIn(ExperimentalUnsignedTypes::class)
class QuadBuilder(
    private val tileData: TileData,
) {
    private fun QuadSquares.buildUrl(idx: Int) =
        Display.buildUrl(
            tileData,
            ids[idx],
            oris[idx],
            tileTypes,
            ScanrowSquaresBacktrackerPath.fillOrder,
        )

    /** Get a list of all possible 2x2 top left corners, with the specific corner tile. */
    fun get2x2Corners(specificCornerId: UByte): QuadSquares {
        var quadSquares = initCornersDepth1(specificCornerId)
        quadSquares = addEdgeTile(quadSquares, 0, -1, Orientation.HALF)
        quadSquares = addEdgeTile(quadSquares, -1, 0, Orientation.CLOCKWISE_90)
        return addMidTile(quadSquares, 2, 1)
    }

    fun solveCorners() {
        // Builds them in top-left orientation only.
        //     014
        //     235
        //     678
        println("Solve 3x3 corner blocks")

        var quadSquares = initCornersDepth1()

        quadSquares = addEdgeTile(quadSquares, 0, -1, Orientation.HALF)
        quadSquares = addEdgeTile(quadSquares, -1, 0, Orientation.CLOCKWISE_90)
        quadSquares = addMidTile(quadSquares, 2, 1)
        quadSquares = addEdgeTile(quadSquares, 1, -1, Orientation.HALF)
        quadSquares = addMidTile(quadSquares, 3, 4)
        quadSquares = addEdgeTile(quadSquares, -1, 2, Orientation.CLOCKWISE_90)
        quadSquares = addMidTile(quadSquares, 6, 3)
        quadSquares = addMidTile(quadSquares, 7, 5)

        quadSquares.toFile("quads-corners")

        quadSquares.buildUrl(0)
        quadSquares.buildUrl(120)
    }

    fun solveEdges() {
        // Builds them in top edge orientation only.
        println("Solve 2x3 edge blocks")

        var quadSquares = initEdgesDepth1()

        quadSquares = addEdgeTile(quadSquares, 0, -1, Orientation.HALF)
        quadSquares = addMidTile(quadSquares, -1, 0)
        quadSquares = addMidTile(quadSquares, 2, 1)
        quadSquares = addEdgeTile(quadSquares, 1, -1, Orientation.HALF)
        quadSquares = addMidTile(quadSquares, 3, 4)
        // quadSquares = addMidTile(quadSquares, -1, 2)

        quadSquares.toFile("quads-edges")

        quadSquares.buildUrl(0)
        quadSquares.buildUrl(120)
    }
    fun solveMids() {
        println("Solve 2x2 mid blocks")

        var quadSquares = initMidsDepth1()

        quadSquares = addMidTile(quadSquares, 0, -1)
        quadSquares = addMidTile(quadSquares, -1, 0)
        quadSquares = addMidTile(quadSquares, 2, 1)

        quadSquares.toFile("quads-mids")

        quadSquares.buildUrl(0)
        quadSquares.buildUrl(120)
    }

    private fun addMidTile(
        inputQuadSquares: QuadSquares,
        westIndex: Int,
        northIndex: Int,
    ): QuadSquares {
        val newOris: MutableList<List<UByte>> = mutableListOf()
        val newIds: MutableList<List<UByte>> = mutableListOf()

        for (idx in 0 until inputQuadSquares.ids.size) {
            val eastColour = inputQuadSquares.colourOf(tileData, idx, westIndex, Compass.EAST)
            val southColour = inputQuadSquares.colourOf(tileData, idx, northIndex, Compass.SOUTH)

            val possibleIds = tileData.midIdsWithTwoColours[southColour.nw(eastColour)]!!
            val possibleOris = tileData.midOrisWithTwoColours[southColour.nw(eastColour)]!!
            (possibleIds.indices).forEach { newIdx ->
                val newMidId = possibleIds[newIdx]
                // To avoid storing all four rotations of each 2x2 block, the lowest midId must be
                // first.
                if ((inputQuadSquares.tileTypes[0] != TileType.MID || newMidId > inputQuadSquares.ids[idx][0]) &&
                    !duplicateId(
                        inputQuadSquares.tileTypes,
                        inputQuadSquares.ids[idx],
                        newMidId,
                        inputQuadSquares.ids[idx].size,
                    )
                ) {
                    newOris.add(inputQuadSquares.oris[idx] + possibleOris[newIdx])
                    newIds.add(inputQuadSquares.ids[idx] + newMidId)
                }
            }
        }

        printDepth(newIds)
        return QuadSquares(newOris.toList(), newIds.toList(), inputQuadSquares.tileTypes)
    }

    private fun addEdgeTile(
        inputQuadSquares: QuadSquares,
        westIndex: Int,
        northIndex: Int,
        orientation: UByte,
    ): QuadSquares {
        val newOris: MutableList<List<UByte>> = mutableListOf()
        val newIds: MutableList<List<UByte>> = mutableListOf()

        for (idx in 0 until inputQuadSquares.ids.size) {
            val eastColour = inputQuadSquares.colourOf(tileData, idx, westIndex, Compass.EAST)
            val southColour = inputQuadSquares.colourOf(tileData, idx, northIndex, Compass.SOUTH)

            for (newEdgeId in 0..55) {
                val newWestColour =
                    tileData.edges[tileData.toSide(Orientation.HALF, Compass.WEST)][newEdgeId]
                val newNorthColour =
                    tileData.edges[tileData.toSide(Orientation.CLOCKWISE_90, Compass.NORTH)][newEdgeId]
                if ((eastColour == Colour.MID_COLOUR_ANY || newWestColour == eastColour) &&
                    (southColour == Colour.MID_COLOUR_ANY || newNorthColour == southColour) &&
                    !duplicateId(
                        inputQuadSquares.tileTypes,
                        inputQuadSquares.ids[idx],
                        newEdgeId.toUByte(),
                        inputQuadSquares.ids[idx].size,
                    )
                ) {
                    newOris.add(inputQuadSquares.oris[idx] + orientation)
                    newIds.add(inputQuadSquares.ids[idx] + newEdgeId.toUByte())
                }
            }
        }

        printDepth(newIds)
        return QuadSquares(newOris.toList(), newIds.toList(), inputQuadSquares.tileTypes)
    }

    private fun duplicateId(
        tileTypes: List<TileType>,
        ids: List<UByte>,
        newId: UByte,
        depth: Int,
    ): Boolean {
        for (idx in 0 until depth) {
            if (tileTypes[idx] == tileTypes[depth] && ids[idx] == newId
            ) {
                return true
            }
        }
        return false
    }

    private fun initCornersDepth1(specificCornerId: UByte? = null): QuadSquares {
        val ids = mutableListOf<List<UByte>>()
        val oris: MutableList<List<UByte>> = mutableListOf()
        if (specificCornerId == null) {
            for (cornerId in 0..3) {
                ids.add(listOf(cornerId.toUByte()))
                oris.add(listOf(Orientation.CLOCKWISE_90))
            }
        } else {
            ids.add(listOf(specificCornerId))
            oris.add(listOf(Orientation.CLOCKWISE_90))
        }

        printDepth(ids)
        return QuadSquares(oris.toList(), ids.toList(), TILE_TYPES_CORNER_3X3)
    }

    private fun initEdgesDepth1(): QuadSquares {
        val ids = mutableListOf<List<UByte>>()
        val oris: MutableList<List<UByte>> = mutableListOf()
        for (edgeId in 0..55) {
            ids.add(listOf(edgeId.toUByte()))
            oris.add(listOf(Orientation.HALF))
        }

        printDepth(ids)
        return QuadSquares(oris.toList(), ids.toList(), TILE_TYPES_EDGE_3X3)
    }

    private fun initMidsDepth1(): QuadSquares {
        val ids = mutableListOf<List<UByte>>()
        val oris: MutableList<List<UByte>> = mutableListOf()
        for (midId in 0..195) {
            for (orientation in 0..3) {
                ids.add(listOf(midId.toUByte()))
                oris.add(listOf(orientation.toUByte()))
            }
        }

        printDepth(ids)

        return QuadSquares(oris.toList(), ids.toList(), TILE_TYPES_MIDS)
    }

    private fun printDepth(ids: List<List<UByte>>) {
        println("DEPTH ${ids[0].size} paths ${ids.size.fmt()}")
    }

    companion object {
        private val TILE_TYPES_CORNER_2X2 = listOf(
            TileType.CORNER,
            TileType.EDGE,
            TileType.EDGE,
            TileType.MID,
        )
        private val TILE_TYPES_CORNER_3X3 = TILE_TYPES_CORNER_2X2 +
            listOf(
                TileType.EDGE,
                TileType.MID,
                TileType.EDGE,
                TileType.MID,
                TileType.MID,
            )
        private val TILE_TYPES_EDGE_2X2 = listOf(
            TileType.EDGE,
            TileType.EDGE,
            TileType.MID,
            TileType.MID,
        )
        private val TILE_TYPES_EDGE_3X3 = TILE_TYPES_EDGE_2X2 +
            listOf(
                TileType.EDGE,
                TileType.MID,
                TileType.MID,
                TileType.MID,
                TileType.MID,
            )
        private val TILE_TYPES_MIDS = listOf(TileType.MID, TileType.MID, TileType.MID, TileType.MID)
    }
}
