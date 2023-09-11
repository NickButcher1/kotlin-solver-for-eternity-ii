@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalUnsignedTypes::class)

package eternityii.data

import java.io.File

/** Range 0-255. */
typealias TileId = UByte

fun Int.nw(west: Int): Int = (255 * this) + west
fun UByte.nw(west: UByte): Int = (255 * this.toInt()) + west.toInt()

fun Int.ec(clockwise: Int): Int = (5 * this) + clockwise
fun UByte.ec(clockwise: UByte): Int = (5 * this.toInt()) + clockwise.toInt()

/**
 * Represents the 256 tiles, with various subsets for convenience.
 */
class TileData(
    randomOrder: Boolean,
    debugMode: Boolean,
) {
    /**
     * The e2pieces.txt file has one line per tile, with the edge colours in the order NESW.
     * Read it into a list of UByteArray, one per tile.
     */
    private val originalInputList = File("../input/e2pieces.txt")
        .useLines { it.toList() }
        .map {
            val splitLine = it.split(" ")
            ubyteArrayOf(
                splitLine[0].toUByte(),
                splitLine[1].toUByte(),
                splitLine[2].toUByte(),
                splitLine[3].toUByte(),
            )
        }.toList()

    private val cornerIdToOriginalIdMapping = if (randomOrder) {
        (0..3).toMutableList().shuffled().toList()
    } else {
        (0..3).toList()
    }

    private val edgeIdToOriginalIdMapping = if (randomOrder) {
        (0..55).toMutableList().shuffled().toList()
    } else {
        (0..55).toList()
    }

    private val midIdToOriginalIdMapping = if (randomOrder) {
        (0..195).toMutableList().shuffled().toList()
    } else {
        (0..195).toList()
    }

    /**
     * Either the same as [originalInputList], or that list shuffled in a random order, depending on configuration.
     */
    private val inputList = if (randomOrder) {
        println("Random mode")
        println("Map C: $cornerIdToOriginalIdMapping")
        println("Map E: $edgeIdToOriginalIdMapping")
        println("Map M: $midIdToOriginalIdMapping")
        val newList: MutableList<UByteArray> = mutableListOf()
        cornerIdToOriginalIdMapping.forEach { cornerId -> newList.add(originalInputList[cornerId]) }
        edgeIdToOriginalIdMapping.forEach { edgeId -> newList.add(originalInputList[edgeId + 4]) }
        midIdToOriginalIdMapping.forEach { midId -> newList.add(originalInputList[midId + 60]) }
        newList.toList()
    } else {
        originalInputList
    }

    /**
     * Lookup with realColours[side][original tileId] where:
     * - side combines orientation and compass direction and is 0-3.
     * - tileId is 0-255
     *
     * Only used for display. All computation uses the optimised 'all' or subsets, which use
     * optimised colour indices.
     */
    val realColours: List<UByteArray> =
        listOf(
            List(originalInputList.size) { idx -> originalInputList[idx][0] }.toUByteArray(),
            List(originalInputList.size) { idx -> originalInputList[idx][1] }.toUByteArray(),
            List(originalInputList.size) { idx -> originalInputList[idx][2] }.toUByteArray(),
            List(originalInputList.size) { idx -> originalInputList[idx][3] }.toUByteArray(),
        )

    /**
     * Lookup with all[side][tileId] where:
     * - side combines orientation and compass direction and is 0-3.
     * - tileId is 0-255
     *
     * This list is only intended for assistance in building more specific subsets.
     * The stored colours are not the original colours - they are the colour indices. See [Colour].
     */
    private val all: List<UByteArray> =
        listOf(
            List(inputList.size) { idx ->
                Colour.ANY_COLOUR_TO_INDEX[inputList[idx][0]]!!.toUByte()
            }.toUByteArray(),
            List(inputList.size) { idx ->
                Colour.ANY_COLOUR_TO_INDEX[inputList[idx][1]]!!.toUByte()
            }.toUByteArray(),
            List(inputList.size) { idx ->
                Colour.ANY_COLOUR_TO_INDEX[inputList[idx][2]]!!.toUByte()
            }.toUByteArray(),
            List(inputList.size) { idx ->
                Colour.ANY_COLOUR_TO_INDEX[inputList[idx][3]]!!.toUByte()
            }.toUByteArray(),
        )

    /** Just the four corner tiles, lookup with corners[side][cornerId]. */
    val corners: List<UByteArray> = listOf(
        all[0].sliceArray(0..3),
        all[1].sliceArray(0..3),
        all[2].sliceArray(0..3),
        all[3].sliceArray(0..3),
    )

    /** Just the 56 edge tiles, lookup with edges[side][edgeId]. */
    val edges: List<UByteArray> = listOf(
        all[0].sliceArray(4..59),
        all[1].sliceArray(4..59),
        all[2].sliceArray(4..59),
        all[3].sliceArray(4..59),
    )

    /** Just the 196 mid tiles, lookup with mids[side][midId]. */
    val mids: List<UByteArray> = listOf(
        all[0].sliceArray(60..255),
        all[1].sliceArray(60..255),
        all[2].sliceArray(60..255),
        all[3].sliceArray(60..255),
    )

    /**
     * A subset of the corners list - only corner tiles with matching anticlockwise colour.
     * Lookup with cornersWithAnticlockwiseColour[edgeColour].
     */
    val cornersWithAnticlockwiseColour: List<UByteArray> = buildCornersWithColour(Compass.EAST)

    /**
     * A subset of the corners list - only corner tiles with matching clockwise colour.
     * Lookup with cornersWithClockwiseColour[edgeColour].
     */
    val cornersWithClockwiseColour: List<UByteArray> = buildCornersWithColour(Compass.NORTH)

    /**
     * A subset of the edges list - only edge tiles with matching anticlockwise colour.
     * Lookup with edgesWithAnticlockwiseColour[edgeColour].
     */
    val edgesWithAnticlockwiseColour: List<UByteArray> = buildEdgesWithColour(Compass.EAST)

    /**
     * A subset of the edges list - only edge tiles with matching clockwise colour.
     * Lookup with edgesWithClockwiseColour[edgeColour].
     */
    val edgesWithClockwiseColour: List<UByteArray> = buildEdgesWithColour(Compass.WEST)

    val reducedEdgesWithAnticlockwiseColour: List<UByteArray> = buildReducedEdgesWithColour(Compass.EAST)
    val reducedEdgesWithClockwiseColour: List<UByteArray> = buildReducedEdgesWithColour(Compass.WEST)

    /**
     * A subset of the edges list - only edges tiles which have both sides matching specific
     * colours.
     *
     * Lookup with edgesWithTwoEdgeColours[colourAnticlockwise.ec(colourClockwise)] to get a list of
     * edge IDs.
     */
    val edgesWithTwoEdgeColours: Map<Int, List<UByte>> = buildEdgesWithTwoColours()

    /**
     * A subset of the mids list - only mid tiles which have at least one side with this colour.
     */
    val midsWithColour: List<UByteArray> = buildMidsWithColour()

    /**
     * A subset of the mids list - only mid tiles which have two sides matching these colours.
     *
     * Lookup with midsWithTwoColours[colourN.nw(colourW)] to get a list of pairs of
     * (midId, orientation).
     */
    lateinit var midIdsWithTwoColours: Map<Int, List<UByte>>
    lateinit var midOrisWithTwoColours: Map<Int, List<UByte>>

    /**
     * A subset of the edge list - only edge tiles which have two sides matching these colours.
     */
    lateinit var edgeIdsRhsWithTwoColours: Map<Int, List<UByte>>

    /**
     * The fixed tile from the original puzzle, and the four optional hint tiles.
     */
    val clues = listOf(
        Clue(139U, 78U, Orientation.HALF, 8, 7),
        Clue(208U, 147U, Orientation.ANTICLOCKWISE_90, 2, 2),
        Clue(255U, 194U, Orientation.ANTICLOCKWISE_90, 2, 13),
        Clue(181U, 120U, Orientation.ANTICLOCKWISE_90, 13, 2),
        Clue(249U, 188U, Orientation.BASE, 13, 13),
    )

    /** Convenient shortcuts. */
    val cornersClockwiseSide = corners[toSide(Orientation.BASE, Compass.NORTH)]
    val cornersAnticlockwiseSide = corners[toSide(Orientation.BASE, Compass.EAST)]
    val edgesClockwiseSide = edges[toSide(Orientation.BASE, Compass.WEST)]
    val edgesAnticlockwiseSide = edges[toSide(Orientation.BASE, Compass.EAST)]

    init {
        buildEdgeIdsRhsWithTwoColours()
        buildMidsWithTwoColours()

        if (debugMode) {
            printList("cornersWithAnticlockwiseColour", cornersWithAnticlockwiseColour)
            printList("cornersWithClockwiseColour", cornersWithClockwiseColour)
            printList("edgesWithAnticlockwiseColour", edgesWithAnticlockwiseColour)
            printList("edgesWithClockwiseColour", edgesWithClockwiseColour)
            printList("reducedEdgesWithAnticlockwiseColour", reducedEdgesWithAnticlockwiseColour)
            printList("reducedEdgesWithClockwiseColour", reducedEdgesWithClockwiseColour)
            printList("midsWithColour", midsWithColour)

            println("edgesWithTwoEdgeColours")
            for (colourAnticlockwise in 0 until Colour.NUM_EDGE_COLOURS) {
                for (colourClockwise in 0 until Colour.NUM_EDGE_COLOURS) {
                    val biColour = colourAnticlockwise.ec(colourClockwise)
                    val edgeIds = edgesWithTwoEdgeColours[biColour]!!
                    println(
                        "  $colourAnticlockwise / $colourClockwise EC ($biColour) -> " +
                            "(${edgeIds.size}) ${edgeIds.joinToString()}",
                    )
                }
            }

            println("edgeIdsRhsWithTwoColours")
            for (colourN in 0 until Colour.NUM_EDGE_COLOURS) {
                for (colourW in 0 until Colour.NUM_MID_COLOURS) {
                    val ids = edgeIdsRhsWithTwoColours[colourN.nw(colourW)]
                    println("  N $colourN, W $colourW (${ids!!.size}) $ids")
                }
            }

            var total = 0
            val totalByNumTiles = UIntArray(8)
            println("midsWithTwoColours: mid IDs followed by orientations")
            for (colourN in 0..Colour.NUM_MID_COLOURS) {
                for (colourW in 0..Colour.NUM_MID_COLOURS) {
                    val ids = midIdsWithTwoColours[colourN.nw(colourW)]
                    val oris = midOrisWithTwoColours[colourN.nw(colourW)]
                    println("  N $colourN, W $colourW (${ids!!.size}) $ids, $oris")
                    if (colourN != Colour.NUM_MID_COLOURS && colourW != Colour.NUM_MID_COLOURS) {
                        val numTiles = ids.size
                        total += numTiles
                        totalByNumTiles[numTiles]++
                    }
                }
            }
            require(total == 4 * 196) { "No tiles have pattern ABAB." }
            println("Of the 256 possible colour pairs, frequency of number of tiles per pair:")
            totalByNumTiles.forEachIndexed { count, numTiles ->
                println("    $count -> $numTiles")
            }

            println("Edge tiles - distribution of mid colours (note how uneven it is):")
            val midColourCount = UByteArray(17)
            edges[0].forEach { mid -> midColourCount[mid.toInt()]++ }
            midColourCount.forEachIndexed { index, count ->
                println("    $index -> $count")
            }
        }
    }

    private fun printList(name: String, list: List<UByteArray>) {
        println(name)
        (list.indices).forEach { idx ->
            println("    Colour $idx (${list[idx].size}): ${list[idx].joinToString()}")
        }
    }

    /**
     * Map an orientation + compass direction to the index (0-3) to get the colour of the side of a tile.
     */
    fun toSide(orientation: UByte, compass: UByte): Int = ((orientation + compass) % 4U).toInt()

    fun toSide(
        tileType: TileType,
        id: UByte,
        orientation: UByte,
        compass: UByte,
    ): UByte =
        when (tileType) {
            TileType.CORNER ->
                corners[toSide(orientation, compass)][id.toInt()]
            TileType.EDGE ->
                edges[toSide(orientation, compass)][id.toInt()]
            TileType.MID ->
                mids[toSide(orientation, compass)][id.toInt()]
        }

    private fun buildCornersWithColour(compass: UByte): List<UByteArray> {
        val tempEdgesWithColour: MutableList<MutableList<UByte>> = mutableListOf()

        repeat(Colour.NUM_EDGE_COLOURS) {
            tempEdgesWithColour.add(mutableListOf())
        }

        (0..3).map { cornerId ->
            val colour = corners[toSide(Orientation.BASE, compass)][cornerId]
            tempEdgesWithColour[colour.toInt()].add(cornerId.toUByte())
        }

        return listOf(
            tempEdgesWithColour[0].sorted().toUByteArray(),
            tempEdgesWithColour[1].sorted().toUByteArray(),
            tempEdgesWithColour[2].sorted().toUByteArray(),
            tempEdgesWithColour[3].sorted().toUByteArray(),
            tempEdgesWithColour[4].sorted().toUByteArray(),
        )
    }

    private fun buildEdgesWithColour(compass: UByte): List<UByteArray> {
        val tempEdgesWithColour: MutableList<MutableList<UByte>> = mutableListOf()

        repeat(Colour.NUM_EDGE_COLOURS) {
            tempEdgesWithColour.add(mutableListOf())
        }

        (0..55).map { edgeId ->
            val colour = edges[toSide(Orientation.BASE, compass)][edgeId]
            tempEdgesWithColour[colour.toInt()].add(edgeId.toUByte())
        }

        return listOf(
            tempEdgesWithColour[0].sorted().toUByteArray(),
            tempEdgesWithColour[1].sorted().toUByteArray(),
            tempEdgesWithColour[2].sorted().toUByteArray(),
            tempEdgesWithColour[3].sorted().toUByteArray(),
            tempEdgesWithColour[4].sorted().toUByteArray(),
        )
    }

    private fun buildReducedEdgesWithColour(compass: UByte): List<UByteArray> {
        val tempEdgesWithColour: MutableList<MutableList<UByte>> = mutableListOf()
        val usedColours: MutableMap<Int, Boolean> = mutableMapOf()

        for (colourAnticlockwise in 0 until Colour.NUM_EDGE_COLOURS) {
            for (colourClockwise in 0 until Colour.NUM_EDGE_COLOURS) {
                usedColours[colourAnticlockwise.ec(colourClockwise)] = false
            }
        }

        repeat(Colour.NUM_EDGE_COLOURS) {
            tempEdgesWithColour.add(mutableListOf())
        }

        (0..55).map { edgeId ->
            val colour = edges[toSide(Orientation.BASE, compass)][edgeId]
            val oppositeColour = edges[toSide(Orientation.HALF, compass)][edgeId]
            if (!usedColours[colour.ec(oppositeColour)]!!) {
                usedColours[colour.ec(oppositeColour)] = true
                tempEdgesWithColour[colour.toInt()].add(edgeId.toUByte())
            }
        }

        return listOf(
            tempEdgesWithColour[0].sorted().toUByteArray(),
            tempEdgesWithColour[1].sorted().toUByteArray(),
            tempEdgesWithColour[2].sorted().toUByteArray(),
            tempEdgesWithColour[3].sorted().toUByteArray(),
            tempEdgesWithColour[4].sorted().toUByteArray(),
        )
    }

    private fun buildEdgesWithTwoColours(): Map<Int, List<UByte>> {
        val idsWithColour: MutableMap<Int, MutableList<UByte>> = mutableMapOf()

        for (colourAnticlockwise in 0 until Colour.NUM_EDGE_COLOURS) {
            for (colourClockwise in 0 until Colour.NUM_EDGE_COLOURS) {
                idsWithColour[colourAnticlockwise.ec(colourClockwise)] = mutableListOf()
            }
        }

        (0..55).map { edgeId ->
            val colourAnticlockwise = edges[toSide(Orientation.BASE, Compass.EAST)][edgeId]
            val colourClockwise = edges[toSide(Orientation.HALF, Compass.EAST)][edgeId]
            idsWithColour[colourAnticlockwise.ec(colourClockwise)]!!.add(edgeId.toUByte())
        }

        // Convert to an immutable map.
        val idsWithColour2: MutableMap<Int, List<UByte>> = mutableMapOf()

        for (colourAnticlockwise in 0 until Colour.NUM_EDGE_COLOURS) {
            for (colourClockwise in 0 until Colour.NUM_EDGE_COLOURS) {
                idsWithColour2[colourAnticlockwise.ec(colourClockwise)] =
                    idsWithColour[colourAnticlockwise.ec(colourClockwise)]!!.toList()
            }
        }

        return idsWithColour2.toMap()
    }

    private fun buildMidsWithColour(): List<UByteArray> {
        val tempWithColour: MutableList<MutableList<UByte>> = mutableListOf()

        repeat(Colour.NUM_MID_COLOURS) {
            tempWithColour.add(mutableListOf())
        }

        // Try all four compass directions.
        (0..195).map { midId ->
            Compass.ALL.forEach { compass ->
                val colour = mids[toSide(Orientation.BASE, compass)][midId]
                tempWithColour[colour.toInt()].add(midId.toUByte())
            }
        }

        return listOf(
            tempWithColour[0].sorted().distinct().toUByteArray(),
            tempWithColour[1].sorted().distinct().toUByteArray(),
            tempWithColour[2].sorted().distinct().toUByteArray(),
            tempWithColour[3].sorted().distinct().toUByteArray(),
            tempWithColour[4].sorted().distinct().toUByteArray(),
            tempWithColour[5].sorted().distinct().toUByteArray(),
            tempWithColour[6].sorted().distinct().toUByteArray(),
            tempWithColour[7].sorted().distinct().toUByteArray(),
            tempWithColour[8].sorted().distinct().toUByteArray(),
            tempWithColour[9].sorted().distinct().toUByteArray(),
            tempWithColour[10].sorted().distinct().toUByteArray(),
            tempWithColour[11].sorted().distinct().toUByteArray(),
            tempWithColour[12].sorted().distinct().toUByteArray(),
            tempWithColour[13].sorted().distinct().toUByteArray(),
            tempWithColour[14].sorted().distinct().toUByteArray(),
            tempWithColour[15].sorted().distinct().toUByteArray(),
            tempWithColour[16].sorted().distinct().toUByteArray(),
        )
    }

    private fun buildMidsWithTwoColours() {
        val idsWithColour: MutableMap<Int, MutableList<UByte>> = mutableMapOf()
        val orisWithColour: MutableMap<Int, MutableList<UByte>> = mutableMapOf()

        // Go one beyond the colours to account for MID_COLOUR_ANY.
        for (colourN in 0..Colour.NUM_MID_COLOURS) {
            for (colourW in 0..Colour.NUM_MID_COLOURS) {
                idsWithColour[colourN.nw(colourW)] = mutableListOf()
                orisWithColour[colourN.nw(colourW)] = mutableListOf()
            }
        }

        // Try all four compass directions.
        (0..195).map { midId ->
            val colourN = mids[0][midId].toInt()
            val colourE = mids[1][midId].toInt()
            val colourS = mids[2][midId].toInt()
            val colourW = mids[3][midId].toInt()
            idsWithColour[colourN.nw(colourW)]!!.add(midId.toUByte())
            idsWithColour[colourW.nw(colourS)]!!.add(midId.toUByte())
            idsWithColour[colourS.nw(colourE)]!!.add(midId.toUByte())
            idsWithColour[colourE.nw(colourN)]!!.add(midId.toUByte())
            orisWithColour[colourN.nw(colourW)]!!.add(Orientation.BASE)
            orisWithColour[colourE.nw(colourN)]!!.add(Orientation.ANTICLOCKWISE_90)
            orisWithColour[colourS.nw(colourE)]!!.add(Orientation.HALF)
            orisWithColour[colourW.nw(colourS)]!!.add(Orientation.CLOCKWISE_90)

            idsWithColour[Colour.MID_COLOUR_ANY.nw(colourW.toUByte())]!!.add(midId.toUByte())
            idsWithColour[Colour.MID_COLOUR_ANY.nw(colourN.toUByte())]!!.add(midId.toUByte())
            idsWithColour[Colour.MID_COLOUR_ANY.nw(colourE.toUByte())]!!.add(midId.toUByte())
            idsWithColour[Colour.MID_COLOUR_ANY.nw(colourS.toUByte())]!!.add(midId.toUByte())
            orisWithColour[Colour.MID_COLOUR_ANY.nw(colourW.toUByte())]!!.add(Orientation.BASE)
            orisWithColour[Colour.MID_COLOUR_ANY.nw(colourN.toUByte())]!!
                .add(Orientation.ANTICLOCKWISE_90)
            orisWithColour[Colour.MID_COLOUR_ANY.nw(colourE.toUByte())]!!.add(Orientation.HALF)
            orisWithColour[Colour.MID_COLOUR_ANY.nw(colourS.toUByte())]!!
                .add(Orientation.CLOCKWISE_90)

            idsWithColour[colourN.nw(Colour.MID_COLOUR_ANY.toInt())]!!.add(midId.toUByte())
            idsWithColour[colourE.nw(Colour.MID_COLOUR_ANY.toInt())]!!.add(midId.toUByte())
            idsWithColour[colourS.nw(Colour.MID_COLOUR_ANY.toInt())]!!.add(midId.toUByte())
            idsWithColour[colourW.nw(Colour.MID_COLOUR_ANY.toInt())]!!.add(midId.toUByte())
            orisWithColour[colourN.nw(Colour.MID_COLOUR_ANY.toInt())]!!.add(Orientation.BASE)
            orisWithColour[colourE.nw(Colour.MID_COLOUR_ANY.toInt())]!!
                .add(Orientation.ANTICLOCKWISE_90)
            orisWithColour[colourS.nw(Colour.MID_COLOUR_ANY.toInt())]!!.add(Orientation.HALF)
            orisWithColour[colourW.nw(Colour.MID_COLOUR_ANY.toInt())]!!
                .add(Orientation.CLOCKWISE_90)

            idsWithColour[Colour.MID_COLOUR_ANY.nw(Colour.MID_COLOUR_ANY)]!!.add(midId.toUByte())
            idsWithColour[Colour.MID_COLOUR_ANY.nw(Colour.MID_COLOUR_ANY)]!!.add(midId.toUByte())
            idsWithColour[Colour.MID_COLOUR_ANY.nw(Colour.MID_COLOUR_ANY)]!!.add(midId.toUByte())
            idsWithColour[Colour.MID_COLOUR_ANY.nw(Colour.MID_COLOUR_ANY)]!!.add(midId.toUByte())
            orisWithColour[Colour.MID_COLOUR_ANY.nw(Colour.MID_COLOUR_ANY)]!!.add(Orientation.BASE)
            orisWithColour[Colour.MID_COLOUR_ANY.nw(Colour.MID_COLOUR_ANY)]!!
                .add(Orientation.ANTICLOCKWISE_90)
            orisWithColour[Colour.MID_COLOUR_ANY.nw(Colour.MID_COLOUR_ANY)]!!.add(Orientation.HALF)
            orisWithColour[Colour.MID_COLOUR_ANY.nw(Colour.MID_COLOUR_ANY)]!!
                .add(Orientation.CLOCKWISE_90)
        }

        // Convert to an immutable map.
        val idsWithColour2: MutableMap<Int, List<UByte>> = mutableMapOf()
        val orisWithColour2: MutableMap<Int, List<UByte>> = mutableMapOf()

        for (colourN in 0..Colour.NUM_MID_COLOURS) {
            for (colourW in 0..Colour.NUM_MID_COLOURS) {
                idsWithColour2[colourN.nw(colourW)] = idsWithColour[colourN.nw(colourW)]!!.toList()
                orisWithColour2[colourN.nw(colourW)] = orisWithColour[colourN.nw(colourW)]!!.toList()
            }
        }

        midIdsWithTwoColours = idsWithColour2.toMap()
        midOrisWithTwoColours = orisWithColour2.toMap()
    }

    private fun buildEdgeIdsRhsWithTwoColours() {
        val idsWithColour: MutableMap<Int, MutableList<UByte>> = mutableMapOf()

        for (colourN in 0 until Colour.NUM_EDGE_COLOURS) {
            for (colourW in 0..Colour.NUM_MID_COLOURS) {
                idsWithColour[colourN.nw(colourW)] = mutableListOf()
            }
        }

        // Try all four compass directions.
        (0..55).map { edgeId ->
            val colourN = edges[1][edgeId].toInt()
            val colourW = edges[0][edgeId].toInt()
            idsWithColour[colourN.nw(colourW)]!!.add(edgeId.toUByte())
        }

        // Convert to an immutable map.
        val idsWithColour2: MutableMap<Int, List<UByte>> = mutableMapOf()

        for (colourN in 0 until Colour.NUM_EDGE_COLOURS) {
            for (colourW in 0 until Colour.NUM_MID_COLOURS) {
                idsWithColour2[colourN.nw(colourW)] = idsWithColour[colourN.nw(colourW)]!!.toList()
            }
        }

        edgeIdsRhsWithTwoColours = idsWithColour2.toMap()
    }

    /**
     * Convert a corner ID, edge ID or mid ID back to a real tile ID.
     *
     * If tile order was randomised on startup, also reverses that randomisation to give the
     * real tile ID.
     */
    fun idToRealTileId(tileType: TileType, id: UByte): TileId =
        when (tileType) {
            TileType.CORNER -> cornerIdToOriginalIdMapping[id.toInt()].toUByte()
            TileType.EDGE -> (edgeIdToOriginalIdMapping[id.toInt()].toUByte() + 4U).toUByte()
            TileType.MID -> (midIdToOriginalIdMapping[id.toInt()].toUByte() + 60U).toUByte()
        }
}
