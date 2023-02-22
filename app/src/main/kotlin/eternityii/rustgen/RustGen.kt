package eternityii.rustgen

import eternityii.backtracker.BacktrackerPath
import eternityii.data.Compass
import eternityii.data.Orientation
import eternityii.data.TileType
import java.io.BufferedWriter
import java.io.File

private data class Cell(
    val north_idx: Int,
    val west_idx: Int,
    val cell_type: String,
    val ori: String
)
private data class TileOri(
    val idx: Int,
    val ori: Int,
    val biColour: Int
)

class RustGen(
    inputFilename: String,
    private val path: BacktrackerPath?,
    private val randomOrder: Boolean
) {
    private val numRows: Int
    private val numCols: Int
    private val numTiles: Int
    private val numMids: Int
    private val numEdges: Int
    private val numColours: Int
    private var numEdgeColours: MutableMap<Int, Boolean> = mutableMapOf()
    private var numMidColours: MutableMap<Int, Boolean> = mutableMapOf()
    private val originalFileTiles: List<List<Int>>
    private val fileTiles: MutableList<List<Int>>
    private val fileTileTypes: MutableList<String> = mutableListOf()
    private val cornersWithClockwiseColour: MutableMap<Int, MutableList<Int>>
    private val cornersWithAnticlockwiseColour: MutableMap<Int, MutableList<Int>>
    private val topLeftCornersWithTwoColours: MutableMap<Int, MutableList<TileOri>> = mutableMapOf()
    private val topRightCornersWithTwoColours: MutableMap<Int, MutableList<TileOri>> = mutableMapOf()
    private val bottomLeftCornersWithTwoColours: MutableMap<Int, MutableList<TileOri>> = mutableMapOf()
    private val bottomRightCornersWithTwoColours: MutableMap<Int, MutableList<TileOri>> = mutableMapOf()
    private val edgesWithClockwiseColour: MutableMap<Int, MutableList<Int>>
    private val edgesWithAnticlockwiseColour: MutableMap<Int, MutableList<Int>>
    private val topEdgesWithTwoColours: MutableMap<Int, MutableList<TileOri>> = mutableMapOf()
    private val rightEdgesWithTwoColours: MutableMap<Int, MutableList<TileOri>> = mutableMapOf()
    private val bottomEdgesWithTwoColours: MutableMap<Int, MutableList<TileOri>> = mutableMapOf()
    private val leftEdgesWithTwoColours: MutableMap<Int, MutableList<TileOri>> = mutableMapOf()
    private val midsWithTwoColours: MutableMap<Int, MutableList<TileOri>>
    private var runningCount = 2

    init {
        println("RustGen: $inputFilename")
        val inputList = File("../input/$inputFilename").useLines { it.toList() }
        val firstLine = inputList[0].split(" ")
        numRows = firstLine[1].toInt()
        numCols = firstLine[0].toInt()
        numTiles = numRows * numCols
        numMids = (numRows - 2) * (numCols - 2)
        numEdges = 2 * (numRows + numCols) - 8

        originalFileTiles = inputList.subList(1, inputList.size).map {
            val splitString = it.split(" ")
            // Rotate to standard format.
            val fromFile = listOf(
                splitString[0].toInt(),
                splitString[1].toInt(),
                splitString[2].toInt(),
                splitString[3].toInt()
            )
            when (fromFile.count { colour -> colour == 0 }) {
                0 -> {
                    fileTileTypes.add("MID")
                    numMidColours[fromFile[0]] = true
                    numMidColours[fromFile[1]] = true
                    numMidColours[fromFile[2]] = true
                    numMidColours[fromFile[3]] = true
                    fromFile
                }
                1 -> {
                    fileTileTypes.add("EDGE")
                    if (fromFile[0] == 0) {
                        numEdgeColours[fromFile[1]] = true
                        numEdgeColours[fromFile[3]] = true
                        listOf(fromFile[2], fromFile[3], fromFile[0], fromFile[1])
                    } else if (fromFile[2] == 0) {
                        numEdgeColours[fromFile[1]] = true
                        numEdgeColours[fromFile[3]] = true
                        fromFile
                    } else {
                        throw IllegalArgumentException()
                    }
                }
                2 -> {
                    fileTileTypes.add("CORNER")
                    if (fromFile[0] == 0 && fromFile[1] == 0) {
                        listOf(fromFile[2], fromFile[3], fromFile[0], fromFile[1])
                    } else if (fromFile[2] == 0 && fromFile[3] == 0) {
                        fromFile
                    } else {
                        throw IllegalArgumentException()
                    }
                }
                else -> throw IllegalArgumentException()
            }
        }.toList()

        numColours = numEdgeColours.size + numMidColours.size

        val newEdgeColourMap: MutableMap<Int, Int> = mutableMapOf()
        val newMidColourMap: MutableMap<Int, Int> = mutableMapOf()
        var nextFreeEdgeColour = 1
        var nextFreeMidColour = 1

        (1..numColours).forEach { colour ->
            if (numEdgeColours.containsKey(colour)) {
                newEdgeColourMap[colour] = nextFreeEdgeColour
                nextFreeEdgeColour++
            }
            if (numMidColours.containsKey(colour)) {
                newMidColourMap[colour] = nextFreeMidColour
                nextFreeMidColour++
            }
        }

        fileTiles = mutableListOf()
        originalFileTiles.forEachIndexed { idx, entry ->
            when {
                idx < 4 ->
                    fileTiles.add(
                        listOf(
                            newEdgeColourMap[entry[0]]!!,
                            newEdgeColourMap[entry[1]]!!,
                            entry[2],
                            entry[3]
                        )
                    )

                idx < (4 + numEdges) ->
                    fileTiles.add(
                        listOf(
                            newMidColourMap[entry[0]]!!,
                            newEdgeColourMap[entry[1]]!!,
                            entry[2],
                            newEdgeColourMap[entry[3]]!!
                        )
                    )

                else ->
                    fileTiles.add(
                        listOf(
                            newMidColourMap[entry[0]]!!,
                            newMidColourMap[entry[1]]!!,
                            newMidColourMap[entry[2]]!!,
                            newMidColourMap[entry[3]]!!
                        )
                    )
            }
        }

        cornersWithClockwiseColour = buildCornersWithColour(Compass.NORTH.toInt())
        cornersWithAnticlockwiseColour = buildCornersWithColour(Compass.EAST.toInt())
        buildCornersWithTwoColours()
        edgesWithClockwiseColour = buildEdgesWithColour(Compass.WEST.toInt())
        edgesWithAnticlockwiseColour = buildEdgesWithColour(Compass.EAST.toInt())
        buildEdgesWithTwoColours()
        midsWithTwoColours = buildWithTwoColours(4 + numEdges, numTiles)
    }

    fun generate() {
        File("../output/autogen.rs").bufferedWriter().use { out ->
            out.write("// Autogenerated code.")
            out.write("\nuse crate::celltype::{CORNER_BOTTOM_LEFT, CORNER_BOTTOM_RIGHT, CORNER_TOP_LEFT, CORNER_TOP_RIGHT, EDGE_BOTTOM, EDGE_LEFT, EDGE_RIGHT, EDGE_TOP, MID, CellType};")
            out.write("\nuse crate::ori::{ANTICLOCKWISE_90, ANY, BASE, CLOCKWISE_90, HALF, Ori};")
            out.write("\n")
            out.write("\npub const NUM_TILES: usize = $numTiles;")
            out.write("\npub const NUM_ROWS: usize = $numRows;")
            out.write("\npub const NUM_COLS: usize = $numCols;")
            out.write("\n")
            out.write("\n// pub const NUM_CORNERS: u32 = 4;")
            out.write("\n// pub const NUM_EDGES: u32 = $numEdges;")
            out.write("\n// pub const NUM_MIDS: u32 = $numMids;")
            out.write("\n")
            out.write("\n// pub const NUM_COLOURS: u32 = $numColours;")
            out.write("\n// pub const NUM_COLOURS_INC_GREY: u32 = ${numColours + 1};")
            out.write("\n// pub const NUM_EDGE_COLOURS: u32 = ${numEdgeColours.size};")
            out.write("\n// pub const NUM_MID_COLOURS: u32 = ${numMidColours.size};")
            out.write("\n")
            out.write("\nconst INVALID_CELL_IDX: u8 = 255;")
            out.write("\n")
            out.write("\n#[derive(Debug)]")
            out.write("\npub struct Cell {")
            out.write("\n    pub north_idx: u8,")
            out.write("\n    pub west_idx: u8,")
            out.write("\n    pub cell_type: CellType,")
            out.write("\n    pub ori: Ori")
            out.write("\n}")
            out.write("\n")

            if (path != null) {
                out.write("\npub const FILL_ORDER: [Cell; NUM_TILES] = [")
                for (idx in 0 until numTiles) {

                    val oriString = when (path.orientations[idx]) {
                        Orientation.BASE -> "BASE"
                        Orientation.ANTICLOCKWISE_90 -> "ANTICLOCKWISE_90"
                        Orientation.HALF -> "HALF"
                        Orientation.CLOCKWISE_90 -> "CLOCKWISE_90"
                        else -> "ANY"
                    }

                    val cellTypeString = when (path.tileTypes[idx]) {
                        TileType.CORNER ->
                            when (path.orientations[idx]) {
                                Orientation.BASE -> "CORNER_BOTTOM_LEFT"
                                Orientation.ANTICLOCKWISE_90 -> "CORNER_BOTTOM_RIGHT"
                                Orientation.HALF -> "CORNER_TOP_RIGHT"
                                Orientation.CLOCKWISE_90 -> "CORNER_TOP_LEFT"
                                else -> throw IllegalArgumentException()
                            }

                        TileType.EDGE ->
                            when (path.orientations[idx]) {
                                Orientation.BASE -> "EDGE_BOTTOM"
                                Orientation.ANTICLOCKWISE_90 -> "EDGE_RIGHT"
                                Orientation.HALF -> "EDGE_TOP"
                                Orientation.CLOCKWISE_90 -> "EDGE_LEFT"
                                else -> throw IllegalArgumentException()
                            }

                        else -> "MID"
                    }

                    val northIdx = if (path.northIndex[idx] == -1) {
                        "INVALID_CELL_IDX"
                    } else {
                        path.northIndex[idx].toString()
                    }
                    val westIdx = if (path.westIndex[idx] == -1) {
                        "INVALID_CELL_IDX"
                    } else {
                        path.westIndex[idx].toString()
                    }

                    out.write("\n    Cell { north_idx: $northIdx, west_idx: $westIdx, cell_type: $cellTypeString, ori: $oriString }, // idx $idx")
                }
                out.write("\n];")
                out.write("\n")

                out.write("\npub const DISPLAY_TO_FILL_ORDER: [u8; NUM_TILES] = [\n    ")
                for (idx in 0 until numTiles) {
                    out.write("${path.fillOrder[idx]}, ")
                }
                out.write("\n];")
                out.write("\n")
            } else {
                val invalidCellIdx = 255
                out.write("\npub const FILL_ORDER: [Cell; NUM_TILES] = [")

                for (idx in 0 until numTiles) {
                    val row = idx / numCols
                    val col = idx % numCols
                    val cell = when {
                        row == 0 && col == 0 -> Cell(invalidCellIdx, invalidCellIdx, "CORNER_TOP_LEFT", "CLOCKWISE_90")
                        row == 0 && col == (numCols - 1) -> Cell(invalidCellIdx, idx - 1, "CORNER_TOP_RIGHT", "HALF")
                        row == (numRows - 1) && col == 0 -> Cell(idx - numCols, invalidCellIdx, "CORNER_BOTTOM_LEFT", "BASE")
                        row == (numRows - 1) && col == (numCols - 1) -> Cell(idx - numCols, idx - 1, "CORNER_BOTTOM_RIGHT", "ANTICLOCKWISE_90")

                        row == 0 -> Cell(invalidCellIdx, idx - 1, "EDGE_TOP", "HALF")
                        row == (numRows - 1) -> Cell(idx - numCols, idx - 1, "EDGE_BOTTOM", "BASE")
                        col == 0 -> Cell(idx - numCols, invalidCellIdx, "EDGE_LEFT", "CLOCKWISE_90")
                        col == (numCols - 1) -> Cell(idx - numCols, idx - 1, "EDGE_RIGHT", "ANTICLOCKWISE_90")

                        else -> Cell(idx - numCols, idx - 1, "MID", "ANY")
                    }
                    val northIdx = if (cell.north_idx == invalidCellIdx) { "INVALID_CELL_IDX" } else { cell.north_idx.toString() }
                    val westIdx = if (cell.west_idx == invalidCellIdx) { "INVALID_CELL_IDX" } else { cell.west_idx.toString() }
                    out.write("\n    Cell { north_idx: $northIdx, west_idx: $westIdx, cell_type: ${cell.cell_type}, ori: ${cell.ori} }, // idx $idx")
                }
                out.write("\n];")
                out.write("\n")

                out.write("\npub const DISPLAY_TO_FILL_ORDER: [u8; NUM_TILES] = [\n    ")
                for (idx in 0 until numTiles) {
                    out.write("$idx, ")
                }
                out.write("\n];")
                out.write("\n")
            }

            out.write("\npub const TILES: [u8; ${4 * numTiles}] = [")
            fileTiles.forEachIndexed { index, fileTile ->
                out.write("\n    ${fileTile[0]}, ${fileTile[1]}, ${fileTile[2]}, ${fileTile[3]}, // id: $index, tile_type: ${fileTileTypes[index]}")
            }
            out.write("\n];")
            out.write("\n")

            out.writeBiColourMaps()
            out.writeBiColourArray()
        }
    }

    private fun BufferedWriter.writeBiColourArray() {
        // 4 orientations of each tile, then 4 bytes for each TileOri (id, ori, south colour, east colour).
        // Then 2 bytes for each list of TileOris that match a NW colour (num entries and unused zero).
        // And 2 bytes unused at offset zero, so offset zero can be used to indicate no entry for a specific bicolour.
        val numEntries = (numTiles * 4 * 4) + 2 + 2 * (
                topLeftCornersWithTwoColours.size +
                topRightCornersWithTwoColours.size +
                bottomLeftCornersWithTwoColours.size +
                bottomRightCornersWithTwoColours.size +
                topEdgesWithTwoColours.size +
                rightEdgesWithTwoColours.size +
                bottomEdgesWithTwoColours.size +
                leftEdgesWithTwoColours.size +
                midsWithTwoColours.size
            )
        write("\npub static BICOLOUR_TILES: [u8; $numEntries] = [")
        write("\n    // unused")
        write("\n    0, 0,")
        write("\n    // topLeftCornersWithTwoColours")
        writeOneMapToArray(topLeftCornersWithTwoColours)
        write("\n    // topRightCornersWithTwoColours")
        writeOneMapToArray(topRightCornersWithTwoColours)
        write("\n    // bottomLeftCornersWithTwoColours")
        writeOneMapToArray(bottomLeftCornersWithTwoColours)
        write("\n    // bottomRightCornersWithTwoColours")
        writeOneMapToArray(bottomRightCornersWithTwoColours)
        write("\n    // topEdgesWithTwoColours")
        writeOneMapToArray(topEdgesWithTwoColours)
        write("\n    // rightEdgesWithTwoColours")
        writeOneMapToArray(rightEdgesWithTwoColours)
        write("\n    // bottomEdgesWithTwoColours")
        writeOneMapToArray(bottomEdgesWithTwoColours)
        write("\n    // leftEdgesWithTwoColours")
        writeOneMapToArray(leftEdgesWithTwoColours)
        write("\n    // midsWithTwoColours")
        writeOneMapToArray(midsWithTwoColours)
        write("\n];")
        write("\n")
    }

    private fun BufferedWriter.writeOneMapToArray(
        bicolourMap: MutableMap<Int, MutableList<TileOri>>
    ) {
        for (mainColour in 0..numColours) {
            for (acColour in 0..numColours) {
                val bicolour = toBicolour(mainColour, acColour)
                if (bicolourMap.containsKey(bicolour)) {
                    val originalTileoris = bicolourMap[bicolour]!!
                    require(originalTileoris.size != 0)
                    val tileoris = if (randomOrder) {
                        originalTileoris.shuffled()
                    } else {
                        originalTileoris
                    }

                    write("\n    ${tileoris.size}, 0, // $bicolour\n   ")
                    tileoris.forEach { tileori ->
                        // Reduce colours by one so they are zero-based, not one-based. 99 indicates grey border  which
                        // will never be used.
                        var southColour = fileTiles[tileori.idx][(tileori.ori + 2) % 4] - 1
                        if (southColour == -1) {
                            southColour = 99
                        }
                        var eastColour = fileTiles[tileori.idx][(tileori.ori + 1) % 4] - 1
                        if (eastColour == -1) {
                            eastColour = 99
                        }
                        write(" ${tileori.idx}, ${tileori.ori}, $southColour, $eastColour,")
                    }
                }
            }
        }
    }

    private fun BufferedWriter.writeBiColourMaps() {
        writeCornersToMap()
        writeTopEdgesArrayToMap("TOP_EDGES_COLOUR_ARRAY", topEdgesWithTwoColours)
        writeRightEdgesArrayToMap("RIGHT_EDGES_BICOLOUR_ARRAY", rightEdgesWithTwoColours)
        writeBottomEdgesArrayToMap("BOTTOM_EDGES_BICOLOUR_ARRAY", bottomEdgesWithTwoColours)
        writeLeftEdgesArrayToMap("LEFT_EDGES_COLOUR_ARRAY", leftEdgesWithTwoColours)
        writeMidsArrayToMap("MIDS_BICOLOUR_ARRAY", midsWithTwoColours)
    }

    private fun BufferedWriter.writeCornersToMap() {
        val topLeftBicolour = toBicolour(0, 0)
        val topLeftTileoris = topLeftCornersWithTwoColours[topLeftBicolour]!!
        write("\npub static TOP_LEFT_CORNER_OFFSET: u16 = $runningCount;")
        incrementRunningCount(topLeftTileoris.size)
        write("\n")

        write("\npub static TOP_RIGHT_CORNER_COLOUR_ARRAY: [u16; ${numEdgeColours.size}] = [")
        (1..numEdgeColours.size).forEach { colour ->
            val bicolour = toBicolour(0, colour)
            if (topRightCornersWithTwoColours.containsKey(bicolour)) {
                val tileoris = topRightCornersWithTwoColours[bicolour]!!
                write("\n    $runningCount, // west $colour")
                incrementRunningCount(tileoris.size)
            } else {
                write("\n    0,")
            }
        }
        write("\n];")
        write("\n")

        write("\npub static BOTTOM_LEFT_CORNER_COLOUR_ARRAY: [u16; ${numEdgeColours.size}] = [")
        (1..numEdgeColours.size).forEach { colour ->
            val bicolour = toBicolour(colour, 0)
            if (bottomLeftCornersWithTwoColours.containsKey(bicolour)) {
                val tileoris = bottomLeftCornersWithTwoColours[bicolour]!!
                write("\n    $runningCount, // north $colour")
                incrementRunningCount(tileoris.size)
            } else {
                write("\n    0,")
            }
        }
        write("\n];")
        write("\n")

        write("\npub static BOTTOM_RIGHT_CORNER_BICOLOUR_ARRAY: [[u16; ${numEdgeColours.size}]; ${numEdgeColours.size}] = [")
        (1..numEdgeColours.size).forEach { mainColour ->
            write("\n    [")
            (1..numEdgeColours.size).forEach { acColour ->
                val bicolour = toBicolour(mainColour, acColour)
                if (bottomRightCornersWithTwoColours.containsKey(bicolour)) {
                    val tileoris = bottomRightCornersWithTwoColours[bicolour]!!
                    write("$runningCount, ")
                    incrementRunningCount(tileoris.size)
                } else {
                    write("0, ")
                }
            }
            write("], // north $mainColour")
        }
        write("\n];")
        write("\n")
    }

    private fun BufferedWriter.writeTopEdgesArrayToMap(
        arrayName: String,
        bicolourMap: MutableMap<Int, MutableList<TileOri>>
    ) {
        write("\npub static $arrayName: [u16; ${numEdgeColours.size}] = [")
        (1..numEdgeColours.size).forEach { colour ->
            val bicolour = toBicolour(0, colour)
            if (bicolourMap.containsKey(bicolour)) {
                val tileoris = bicolourMap[bicolour]!!
                write("\n    $runningCount,")
                incrementRunningCount(tileoris.size)
            }
        }
        write("\n];")
        write("\n")
    }

    private fun BufferedWriter.writeRightEdgesArrayToMap(
        arrayName: String,
        bicolourMap: MutableMap<Int, MutableList<TileOri>>
    ) {
        write("\npub static $arrayName: [[u16; ${numMidColours.size}]; ${numEdgeColours.size}] = [")
        (1..numEdgeColours.size).forEach { mainColour ->
            write("\n    [")
            (1..numMidColours.size).forEach { acColour ->
                val bicolour = toBicolour(mainColour, acColour)
                if (bicolourMap.containsKey(bicolour)) {
                    val tileoris = bicolourMap[bicolour]!!
                    write("$runningCount, ")
                    incrementRunningCount(tileoris.size)
                } else {
                    write("0, ")
                }
            }
            write("], // north $mainColour")
        }
        write("\n];")
        write("\n")
    }

    private fun BufferedWriter.writeBottomEdgesArrayToMap(
        arrayName: String,
        bicolourMap: MutableMap<Int, MutableList<TileOri>>
    ) {
        write("\npub static $arrayName: [[u16; ${numEdgeColours.size}]; ${numMidColours.size}] = [")
        (1..numMidColours.size).forEach { mainColour ->
            write("\n    [")
            (1..numEdgeColours.size).forEach { acColour ->
                val bicolour = toBicolour(mainColour, acColour)
                if (bicolourMap.containsKey(bicolour)) {
                    val tileoris = bicolourMap[bicolour]!!
                    write("$runningCount, ")
                    incrementRunningCount(tileoris.size)
                } else {
                    write("0, ")
                }
            }
            write("], // north $mainColour")
        }
        write("\n];")
        write("\n")
    }

    private fun BufferedWriter.writeLeftEdgesArrayToMap(
        arrayName: String,
        bicolourMap: MutableMap<Int, MutableList<TileOri>>
    ) {
        write("\npub static $arrayName: [u16; ${numEdgeColours.size}] = [")
        (1..numEdgeColours.size).forEach { colour ->
            val bicolour = toBicolour(colour, 0)
            if (bicolourMap.containsKey(bicolour)) {
                val tileoris = bicolourMap[bicolour]!!
                require(tileoris.size != 0)
                write("\n    $runningCount,")
                incrementRunningCount(tileoris.size)
            }
        }
        write("\n];")
        write("\n")
    }

    private fun BufferedWriter.writeMidsArrayToMap(
        arrayName: String,
        bicolourMap: MutableMap<Int, MutableList<TileOri>>
    ) {
        write("\npub static $arrayName: [[u16; ${numMidColours.size}]; ${numMidColours.size}] = [")
        (1..numMidColours.size).forEach { mainColour ->
            write("\n    [")
            (1..numMidColours.size).forEach { acColour ->
                val bicolour = toBicolour(mainColour, acColour)
                if (bicolourMap.containsKey(bicolour)) {
                    val tileoris = bicolourMap[bicolour]!!
                    write("$runningCount, ")
                    incrementRunningCount(tileoris.size)
                } else {
                    write("0, ")
                }
            }
            write("], // north $mainColour")
        }
        write("\n];")
        write("\n")
    }

    private fun incrementRunningCount(size: Int) {
        // 2 byte fixed + 4 bytes per tileori.
        runningCount += (2 + 4 * size)
    }

    /**
     * Map an orientation + compass direction to the index (0-3) to get the colour of the side of a tile.
     */
    private fun toSide(orientation: Int, compass: Int): Int = (orientation + compass) % 4

    private fun buildCornersWithColour(compass: Int): MutableMap<Int, MutableList<Int>> {
        val tempWithColour: MutableMap<Int, MutableList<Int>> = mutableMapOf()

        (0..3).map { tileId ->
            val colour = fileTiles[tileId][toSide(Orientation.BASE.toInt(), compass)]
            if (tempWithColour[colour] == null) {
                tempWithColour[colour] = mutableListOf()
            }
            val tileList = tempWithColour[colour]!!
            tileList.add(tileId)
        }

        return tempWithColour
    }

    private fun buildEdgesWithColour(compass: Int): MutableMap<Int, MutableList<Int>> {
        val tempWithColour: MutableMap<Int, MutableList<Int>> = mutableMapOf()

        (4 until (numEdges + 4)).map { tileId ->
            val colour = fileTiles[tileId][toSide(Orientation.BASE.toInt(), compass)]
            if (tempWithColour[colour] == null) {
                tempWithColour[colour] = mutableListOf()
            }
            val tileList = tempWithColour[colour]!!
            tileList.add(tileId)
        }

        return tempWithColour
    }

    private fun buildWithTwoColours(fromIdx: Int, toIdx: Int): MutableMap<Int, MutableList<TileOri>> {
        val tempWithColour: MutableMap<Int, MutableList<TileOri>> = mutableMapOf()

        (fromIdx until toIdx).forEach { idx ->
            val fileTile = fileTiles[idx]

            listOf(
                TileOri(idx, Orientation.BASE.toInt(), toBicolour(fileTile[0], fileTile[3])),
                TileOri(idx, Orientation.CLOCKWISE_90.toInt(), toBicolour(fileTile[3], fileTile[2])),
                TileOri(idx, Orientation.HALF.toInt(), toBicolour(fileTile[2], fileTile[1])),
                TileOri(idx, Orientation.ANTICLOCKWISE_90.toInt(), toBicolour(fileTile[1], fileTile[0]))
            ).forEach { tileOri ->
                if (tempWithColour[tileOri.biColour] == null) {
                    tempWithColour[tileOri.biColour] = mutableListOf()
                }
                tempWithColour[tileOri.biColour]!!.add(tileOri)
            }
        }

        return tempWithColour
    }

    private fun buildCornersWithTwoColours() {
        (0..3).map { idx ->
            val fileTile = fileTiles[idx]

            topRightCornersWithTwoColours.addTileOri(
                TileOri(idx, Orientation.HALF.toInt(), toBicolour(fileTile[2], fileTile[1]))
            )

            bottomRightCornersWithTwoColours.addTileOri(
                TileOri(idx, Orientation.ANTICLOCKWISE_90.toInt(), toBicolour(fileTile[1], fileTile[0]))
            )

            bottomLeftCornersWithTwoColours.addTileOri(
                TileOri(idx, Orientation.BASE.toInt(), toBicolour(fileTile[0], fileTile[3]))
            )

            topLeftCornersWithTwoColours.addTileOri(
                TileOri(idx, Orientation.CLOCKWISE_90.toInt(), toBicolour(fileTile[3], fileTile[2]))
            )
        }
    }

    private fun buildEdgesWithTwoColours() {
        (4 until (numEdges + 4)).map { idx ->
            val fileTile = fileTiles[idx]

            topEdgesWithTwoColours.addTileOri(
                TileOri(idx, Orientation.HALF.toInt(), toBicolour(fileTile[2], fileTile[1]))
            )

            rightEdgesWithTwoColours.addTileOri(
                TileOri(idx, Orientation.ANTICLOCKWISE_90.toInt(), toBicolour(fileTile[1], fileTile[0]))
            )

            bottomEdgesWithTwoColours.addTileOri(
                TileOri(idx, Orientation.BASE.toInt(), toBicolour(fileTile[0], fileTile[3]))
            )

            leftEdgesWithTwoColours.addTileOri(
                TileOri(idx, Orientation.CLOCKWISE_90.toInt(), toBicolour(fileTile[3], fileTile[2]))
            )
        }
    }

    private fun MutableMap<Int, MutableList<TileOri>>.addTileOri(tileOri: TileOri) {
        if (this[tileOri.biColour] == null) {
            this[tileOri.biColour] = mutableListOf()
        }
        this[tileOri.biColour]!!.add(tileOri)
    }

    private fun toBicolour(mainColour: Int, acColour: Int): Int = 256 * mainColour + acColour
}
