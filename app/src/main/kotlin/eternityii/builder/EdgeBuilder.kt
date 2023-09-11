package eternityii.builder

import eternityii.data.Orientation
import eternityii.data.TileData
import eternityii.data.TileType
import eternityii.display.Display

/**
 * Build all ways to place a corner and N edge tiles.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class EdgeBuilder(
    private val tileData: TileData,
) {
    fun solveCornerPlusEdgesClockwise(maxDepth: Int): Partial {
        println("solveCornerPlusEdgesClockwise: depth $maxDepth")
        require(maxDepth >= 3) { "Depth less than 3 not supported." }

        var partial = Partial(
            1,
            listOf(TileType.CORNER),
            listOf(Orientation.CLOCKWISE_90),
            (0..3).map { id -> ubyteArrayOf(id.toUByte()) }.toList(),
        )
        println("DEPTH 1 paths ${partial.ids.size}")

        partial = addEdgeTileToCornerClockwise(partial)

        println("DEPTH 2 paths ${partial.ids.size}")

        for (depth in 3..maxDepth) {
            partial = addEdgeTileToEdgeClockwise(partial)
            println("DEPTH $depth paths ${partial.ids.size}")
        }

        Display.buildUrl(tileData, partial.ids[100], partial.oris, partial.tileTypes)

        partial.toFile("corner-and-edges-$maxDepth-clockwise")
        return partial
    }

    fun solveCornerPlusEdgesAnticlockwise(maxDepth: Int): Partial {
        println("solveCornerPlusEdgesAnticlockwise: depth $maxDepth")
        require(maxDepth >= 3) { "Depth less than 3 not supported." }

        var partial = Partial(
            1,
            listOf(TileType.CORNER),
            listOf(Orientation.HALF),
            (0..3).map { id -> ubyteArrayOf(id.toUByte()) }.toList(),
        )
        println("DEPTH 1 paths ${partial.ids.size}")

        partial = addEdgeTileToCornerAnticlockwise(partial)

        println("DEPTH 2 paths ${partial.ids.size}")

        for (depth in 3..maxDepth) {
            partial = addEdgeTileToEdgeAnticlockwise(partial)
            println("DEPTH $depth paths ${partial.ids.size}")
        }

        Display.buildUrl(
            tileData,
            partial.ids[100].reversedArray(),
            partial.oris.reversed(),
            partial.tileTypes.reversed(),
        )

        partial.toFile("corner-and-edges-$maxDepth-anticlockwise")
        return partial
    }

    fun solveEdgesClockwise(maxDepth: Int): Partial {
        println("solveEdgesClockwise: depth $maxDepth")
        require(maxDepth >= 2) { "Depth less than 2 not supported." }

        var partial = Partial(
            1,
            listOf(TileType.EDGE),
            listOf(Orientation.HALF),
            (0..55).map { id -> ubyteArrayOf(id.toUByte()) }.toList(),
        )
        println("DEPTH 1 paths ${partial.ids.size}")

        for (depth in 2..maxDepth) {
            partial = addEdgeTileToEdgeClockwiseNoCorners(partial)
            println("DEPTH $depth paths ${partial.ids.size}")
        }

        Display.buildUrl(tileData, partial.ids[100], partial.oris, partial.tileTypes)

        partial.toFile("edges-$maxDepth-clockwise")
        return partial
    }

    private fun addEdgeTileToCornerClockwise(
        inputPartial: Partial,
    ): Partial {
        val newIdsList = mutableListOf<UByteArray>()

        inputPartial.ids.forEach { depthList ->
            val lastCornerId = depthList[depthList.size - 1]
            val colourToMatch = tileData.cornersClockwiseSide[lastCornerId.toInt()]

            tileData.edgesWithAnticlockwiseColour[colourToMatch.toInt()].forEach { edgeId ->
                val newDepthList = depthList.copyOf(depthList.size + 1)
                newDepthList[depthList.size] = edgeId
                newIdsList.add(newDepthList)
            }
        }

        return Partial(
            inputPartial.depth + 1,
            inputPartial.tileTypes + TileType.EDGE,
            inputPartial.oris + Orientation.HALF,
            newIdsList.toList(),
        )
    }

    private fun addEdgeTileToCornerAnticlockwise(
        inputPartial: Partial,
    ): Partial {
        val newIdsList = mutableListOf<UByteArray>()

        inputPartial.ids.forEach { depthList ->
            val lastCornerId = depthList[depthList.size - 1]
            val colourToMatch = tileData.cornersAnticlockwiseSide[lastCornerId.toInt()]

            tileData.edgesWithClockwiseColour[colourToMatch.toInt()].forEach { edgeId ->
                val newDepthList = depthList.copyOf(depthList.size + 1)
                newDepthList[depthList.size] = edgeId
                newIdsList.add(newDepthList)
            }
        }

        return Partial(
            inputPartial.depth + 1,
            inputPartial.tileTypes + TileType.EDGE,
            inputPartial.oris + Orientation.HALF,
            newIdsList.toList(),
        )
    }

    private fun addEdgeTileToEdgeClockwise(
        inputPartial: Partial,
    ): Partial {
        val newIdsList = mutableListOf<UByteArray>()

        inputPartial.ids.forEach { depthList ->
            val lastEdgeId = depthList[depthList.size - 1]
            val colourToMatch = tileData.edgesClockwiseSide[lastEdgeId.toInt()]

            // println("Try $depthList, tile: $lastTileId = $tile, colourToMatch $colourToMatch")
            tileData.edgesWithAnticlockwiseColour[colourToMatch.toInt()].forEach { edgeId ->
                // Don't check against the first tile because it is a corner.
                if (!byteArrayContainsEdgeIdSkipFirst(depthList, edgeId)) {
                    val newDepthList = depthList.copyOf(depthList.size + 1)
                    newDepthList[depthList.size] = edgeId
                    newIdsList.add(newDepthList)
                }
            }
        }

        return Partial(
            inputPartial.depth + 1,
            inputPartial.tileTypes + TileType.EDGE,
            inputPartial.oris + Orientation.HALF,
            newIdsList.toList(),
        )
    }

    private fun addEdgeTileToEdgeClockwiseNoCorners(
        inputPartial: Partial,
    ): Partial {
        val newIdsList = mutableListOf<UByteArray>()

        inputPartial.ids.forEach { depthList ->
            val lastEdgeId = depthList[depthList.size - 1]
            val colourToMatch = tileData.edgesClockwiseSide[lastEdgeId.toInt()]

            // println("Try $depthList, tile: $lastTileId = $tile, colourToMatch $colourToMatch")
            tileData.edgesWithAnticlockwiseColour[colourToMatch.toInt()].forEach { edgeId ->
                if (!byteArrayContainsEdgeId(depthList, edgeId)) {
                    val newDepthList = depthList.copyOf(depthList.size + 1)
                    newDepthList[depthList.size] = edgeId
                    newIdsList.add(newDepthList)
                }
            }
        }
        return Partial(
            inputPartial.depth + 1,
            inputPartial.tileTypes + TileType.EDGE,
            inputPartial.oris + Orientation.HALF,
            newIdsList.toList(),
        )
    }

    private fun addEdgeTileToEdgeAnticlockwise(
        inputPartial: Partial,
    ): Partial {
        val newIdsList = mutableListOf<UByteArray>()

        inputPartial.ids.forEach { depthList ->
            val lastEdgeId = depthList[depthList.size - 1]
            val colourToMatch = tileData.edgesAnticlockwiseSide[lastEdgeId.toInt()]

            // println("Try $depthList, tile: $lastTileId = $tile, colourToMatch $colourToMatch")
            tileData.edgesWithClockwiseColour[colourToMatch.toInt()].forEach { edgeId ->
                // Don't check against the first tile because it is a corner.
                if (!byteArrayContainsEdgeIdSkipFirst(depthList, edgeId)) {
                    val newDepthList = depthList.copyOf(depthList.size + 1)
                    newDepthList[depthList.size] = edgeId
                    newIdsList.add(newDepthList)
                }
            }
        }
        return Partial(
            inputPartial.depth + 1,
            inputPartial.tileTypes + TileType.EDGE,
            inputPartial.oris + Orientation.HALF,
            newIdsList.toList(),
        )
    }

    private fun byteArrayContainsEdgeId(depthList: UByteArray, edgeId: UByte): Boolean {
        depthList.forEach { edgeIdFromList ->
            if (edgeIdFromList == edgeId) {
                return true
            }
        }
        return false
    }

    private fun byteArrayContainsEdgeIdSkipFirst(depthList: UByteArray, edgeId: UByte): Boolean {
        for (idx in 1 until depthList.size) {
            if (depthList[idx] == edgeId) {
                return true
            }
        }
        return false
    }

    fun solveOneEdge() {
        println("solveOneEdge")

        val inputPartial = Partial.fromFile("corner-and-edges-4-clockwise")
        val inputPartial2 = Partial.fromFile("edges-4-clockwise")

        // Either list works, but both together uses too much memory.
//        val newIdsList = mutableListOf<UByteArray>()
        val newEdgeIdsList = mutableListOf<IntArray>()
        var totalAccepted = 0L
        var totalRejected = 0L

        for (idx1 in 0 until inputPartial.ids.size) {
            val block1 = inputPartial.ids[idx1]
            val block1EastColour = tileData.edgesClockwiseSide[block1[3].toInt()]
            for (idx2 in 0 until inputPartial2.ids.size) {
                val block2 = inputPartial2.ids[idx2]
                val block2WestColour = tileData.edgesAnticlockwiseSide[block2[0].toInt()]
                if (block1EastColour == block2WestColour &&
                    block1[1] != block2[0] &&
                    block1[1] != block2[1] &&
                    block1[1] != block2[2] &&
                    block1[1] != block2[3] &&
                    block1[2] != block2[0] &&
                    block1[2] != block2[1] &&
                    block1[2] != block2[2] &&
                    block1[2] != block2[3] &&
                    block1[3] != block2[0] &&
                    block1[3] != block2[1] &&
                    block1[3] != block2[2] &&
                    block1[3] != block2[3]
                ) {
                    totalAccepted++
//                    newIdsList.add(block1 + block2)
                    newEdgeIdsList.add(intArrayOf(idx1, idx2))
                } else {
                    totalRejected++
                }
            }
        }
        println("Accepted: $totalAccepted")
        println("Rejected: $totalRejected")
//        val newPartial = Partial(
//            8,
//            inputPartial.tileTypes + inputPartial2.tileTypes,
//            inputPartial.oris + inputPartial2.oris,
//            newIdsList.toList()
//        )
//        Display.buildUrl(tileData, newPartial.ids[100], newPartial.oris, newPartial.tileTypes)
//        newPartial.toFile("corner-and-edges-8-clockwise")

        val newPartialEdge = PartialEdge(
            8,
            newEdgeIdsList.toList(),
        )
        newPartialEdge.toFile("edge4blocks-8-clockwise")
    }
}
