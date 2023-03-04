package eternityii.rustgen

import eternityii.CellIterator
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
    private val randomOrder: Boolean,
    private val midsOnly: Boolean
) {
    private val numRows: Int
    private val numCols: Int
    private val numCells: Int
    private val numTiles: Int
    private val numMids: Int
    private val numEdges: Int
    private val numColours: Int
    private val anyColour: Int
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
    private val addTileFunctions: MutableMap<Int, String> = mutableMapOf()

    init {
        println("RustGen: $inputFilename")
        val inputList = File("../input/$inputFilename").useLines { it.toList() }
        val firstLine = inputList[0].split(" ")
        numRows = firstLine[1].toInt()
        numCols = firstLine[0].toInt()
        numCells = numRows * numCols
        numMids = (numRows - 2) * (numCols - 2)
        numEdges = 2 * (numRows + numCols) - 8
        numTiles = if (midsOnly) { numMids } else { numCells }

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
        anyColour = numMidColours.size

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

        if (!midsOnly) {
            cornersWithClockwiseColour = buildCornersWithColour(Compass.NORTH.toInt())
            cornersWithAnticlockwiseColour = buildCornersWithColour(Compass.EAST.toInt())
            buildCornersWithTwoColours()
            edgesWithClockwiseColour = buildEdgesWithColour(Compass.WEST.toInt())
            edgesWithAnticlockwiseColour = buildEdgesWithColour(Compass.EAST.toInt())
            buildEdgesWithTwoColours()
        } else {
            cornersWithClockwiseColour = mutableMapOf()
            cornersWithAnticlockwiseColour = mutableMapOf()
            edgesWithClockwiseColour = mutableMapOf()
            edgesWithAnticlockwiseColour = mutableMapOf()
        }
        midsWithTwoColours = buildWithTwoColours(4 + numEdges, numCells)
    }

    fun generate() {
        val filename = if (midsOnly) { "autogenmids.rs" } else { "autogenfull.rs" }
        File("../output/$filename").bufferedWriter().use { out ->
            out.write("\nuse crate::Backtracker;")
            out.write("\nuse crate::celltype::{")
            out.write("\n    CellType, MID,")
            if (midsOnly) {
                out.write("\n    MID_LEFT, MID_TOP, MID_TOP_LEFT,")
                out.write("\n};")
                out.write("\nuse crate::ori::{Ori, ANY};")
            } else {
                out.write("\n    CORNER_BOTTOM_LEFT, CORNER_BOTTOM_RIGHT, CORNER_TOP_LEFT, CORNER_TOP_RIGHT,")
                out.write("\n    EDGE_BOTTOM, EDGE_LEFT, EDGE_RIGHT, EDGE_TOP,")
                out.write("\n};")
                out.write("\nuse crate::ori::{Ori, ANTICLOCKWISE_90, ANY, BASE, CLOCKWISE_90, HALF};")
            }
            out.write("\n")
            out.write("\npub const NUM_CELLS: usize = $numCells;")
            out.write("\npub const NUM_TILES: usize = $numTiles;")
            out.write("\npub const NUM_ROWS: usize = $numRows;")
            out.write("\npub const NUM_COLS: usize = $numCols;")
            out.write("\n")
            out.write("\n// pub const NUM_CORNERS: u32 = 4;")
            out.write("\n// pub const NUM_EDGES: u32 = $numEdges;")
            out.write("\n// pub const NUM_MIDS: usize = $numMids;")
            out.write("\n")
            out.write("\n// pub const NUM_COLOURS: u32 = $numColours;")
            out.write("\n// pub const NUM_COLOURS_INC_GREY: u32 = ${numColours + 1};")
            out.write("\n// pub const NUM_EDGE_COLOURS: u32 = ${numEdgeColours.size};")
            out.write("\n// pub const NUM_MID_COLOURS: u32 = ${numMidColours.size};")
            out.write("\n")
            out.write("\n#[cfg(feature = \"backtracker-mids\")]")
            out.write("\npub const ANY_COLOUR: usize = $anyColour;")
            out.write("\n")
            out.write("\nconst INVALID_CELL_IDX: u8 = 255;")
            out.write("\n")
            out.write("\n#[derive(Debug)]")
            out.write("\npub struct Cell {")
            out.write("\n    pub north_idx: u8,")
            out.write("\n    pub west_idx: u8,")
            out.write("\n    pub cell_type: CellType,")
            out.write("\n    pub ori: Ori,")
            out.write("\n}")
            out.write("\n")

            if (path != null) {
                out.write("\npub const FILL_ORDER: [Cell; NUM_TILES] = [")

                var ignoredCells = 0

                // Build a map from placement order to (row,col).
                val cellMap: MutableMap<Int, Int> = mutableMapOf()

                CellIterator().forEach { (row, col) ->
                    val idx = row * 16 + col
                    cellMap[path.fillOrder[idx]] = idx
                }

                for (idxOfAll in 0 until numCells) {
                    if (path.fillOrder[idxOfAll] == -1) {
                        addTileFunctions[idxOfAll] = "SHOULD NOT APPEAR IN OUTPUT"
                        ignoredCells++
                        continue
                    }
                    val idx = idxOfAll - ignoredCells

                    val oriString = when (path.orientations[idx]) {
                        Orientation.BASE -> "BASE"
                        Orientation.ANTICLOCKWISE_90 -> "ANTICLOCKWISE_90"
                        Orientation.HALF -> "HALF"
                        Orientation.CLOCKWISE_90 -> "CLOCKWISE_90"
                        else -> "ANY"
                    }

                    val (cellTypeString, addTileFunction) = when (path.tileTypes[idx]) {
                        TileType.CORNER ->
                            when (path.orientations[idx]) {
                                Orientation.BASE -> Pair("CORNER_BOTTOM_LEFT", "add_tile_bottom_left_corner")
                                Orientation.ANTICLOCKWISE_90 -> Pair("CORNER_BOTTOM_RIGHT", "add_tile_final")
                                Orientation.HALF -> Pair("CORNER_TOP_RIGHT", "add_tile_top_right_corner")
                                Orientation.CLOCKWISE_90 -> Pair("CORNER_TOP_LEFT", "add_tile_0")
                                else -> throw IllegalArgumentException()
                            }

                        TileType.EDGE ->
                            when (path.orientations[idx]) {
                                Orientation.BASE -> Pair("EDGE_BOTTOM", "add_tile_bottom_edge")
                                Orientation.ANTICLOCKWISE_90 -> Pair("EDGE_RIGHT", "add_tile_right_edge")
                                Orientation.HALF -> Pair("EDGE_TOP", "add_tile_top_edge")
                                Orientation.CLOCKWISE_90 -> Pair("EDGE_LEFT", "add_tile_left_edge")
                                else -> throw IllegalArgumentException()
                            }

                        else -> {
                            if (midsOnly) {
                                val cell = cellMap[idx]!!
                                val row = cell / 16
                                val col = cell % 16
                                when {
                                    row == 1 && col == 1 -> Pair("MID_TOP_LEFT", "add_tile_0")
                                    row == 1 -> Pair("MID_TOP", "add_tile_top")
                                    col == 1 -> Pair("MID_LEFT", "add_tile_left")
                                    row == (numRows - 2) && col == (numCols - 2) -> Pair("MID", "add_tile_final")
                                    else -> Pair("MID", "add_tile_mid")
                                }
                            } else {
                                Pair("MID", "add_tile_mid")
                            }
                        }
                    }
                    addTileFunctions[idx] = addTileFunction

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

                out.write("\npub const DISPLAY_TO_FILL_ORDER: [i16; NUM_CELLS] = [\n    ")
                for (idx in 0 until numCells) {
                    out.write("${path.fillOrder[idx]}, ")
                }
                out.write("\n];")
                out.write("\n")
            } else {
                val invalidCellIdx = 255
                out.write("\npub const FILL_ORDER: [Cell; NUM_CELLS] = [")

                for (idx in 0 until numCells) {
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
                    val addTileFunction = when {
                        row == 0 && col == 0 -> "add_tile_0"
                        row == 0 && col == (numCols - 1) -> "add_tile_top_right_corner"
                        row == (numRows - 1) && col == 0 -> "add_tile_bottom_left_corner"
                        row == (numRows - 1) && col == (numCols - 1) -> "add_tile_final"

                        row == 0 -> "add_tile_top_edge"
                        row == (numRows - 1) -> "add_tile_bottom_edge"
                        col == 0 -> "add_tile_left_edge"
                        col == (numCols - 1) -> "add_tile_right_edge"

                        else -> "add_tile_mid"
                    }
                    addTileFunctions[idx] = addTileFunction
                    val northIdx = if (cell.north_idx == invalidCellIdx) { "INVALID_CELL_IDX" } else { cell.north_idx.toString() }
                    val westIdx = if (cell.west_idx == invalidCellIdx) { "INVALID_CELL_IDX" } else { cell.west_idx.toString() }
                    out.write("\n    Cell { north_idx: $northIdx, west_idx: $westIdx, cell_type: ${cell.cell_type}, ori: ${cell.ori} }, // idx $idx")
                }
                out.write("\n];")
                out.write("\n")

                out.write("\npub const DISPLAY_TO_FILL_ORDER: [i16; NUM_TILES] = [\n    ")
                for (idx in 0 until numCells) {
                    out.write("$idx, ")
                }
                out.write("\n];")
                out.write("\n")
            }

            out.write("\npub const TILES: [u8; ${4 * numTiles}] = [")
            fileTiles.forEachIndexed { index, fileTile ->
                if (!midsOnly || index >= 60) {
                    out.write("\n    ${fileTile[0]}, ${fileTile[1]}, ${fileTile[2]}, ${fileTile[3]}, // id: $index, tile_type: ${fileTileTypes[index]}")
                }
            }
            out.write("\n];")
            out.write("\n")

            out.writeAddTileArray()
            out.writeBiColourMaps()
            out.writeBiColourArray()
        }
    }

    private fun BufferedWriter.writeBiColourArray() {
        // 4 orientations of each tile, then 4 bytes for each TileOri (id, ori, south colour, east colour).
        // Then 2 bytes for each list of TileOris that match a NW colour (num entries and unused zero).
        // And 2 bytes unused at offset zero, so offset zero can be used to indicate no entry for a specific bicolour.
        // If mids only, an extra 3 instances of each tile in each orientation.
        val midsCount = if (midsOnly) { (numMids * 4 * 4) + numMids * 3 * 4 * 4 } else { (numCells * 4 * 4) }
        val numEntries = midsCount +
            2 +
            2 * (
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

                    if (midsOnly && mainColour == (anyColour + 1) && acColour == (anyColour + 1)) {
                        // There are more than 255 entries, so we can't use a u8 - set to zero and handle in the Rust code.
                        write("\n    0, 0, // $bicolour\n   ")
                    } else {
                        write("\n    ${tileoris.size}, 0, // $bicolour\n   ")
                    }

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
                        val tileIdx = if (midsOnly) { tileori.idx - 60 } else { tileori.idx }
                        write(" $tileIdx, ${tileori.ori}, $southColour, $eastColour,")
                    }
                }
            }
        }
    }

    private fun BufferedWriter.writeAddTileArray() {
        write("\nimpl Backtracker<'_> {")
        write("\n    pub const ADD_TILE_FUNCTIONS: [fn(&mut Self, usize) -> (); $numTiles] = [")
        if (midsOnly) {
            for (idx in 0 until numCells) {
                if (path!!.fillOrder[idx] != -1) {
                    write("\n        Backtracker::${addTileFunctions[path.fillOrder[idx]]},")
                }
            }
        } else {
            for (idx in 0 until numCells) {
                write("\n        Backtracker::${addTileFunctions[idx]},")
            }
        }
        write("\n    ];")
        write("\n}")
        write("\n")
    }

    private fun BufferedWriter.writeBiColourMaps() {
        if (!midsOnly) {
            writeCornersToMap()
            writeTopEdgesArrayToMap("TOP_EDGES_COLOUR_ARRAY", topEdgesWithTwoColours)
            writeRightEdgesArrayToMap("RIGHT_EDGES_BICOLOUR_ARRAY", rightEdgesWithTwoColours)
            writeBottomEdgesArrayToMap("BOTTOM_EDGES_BICOLOUR_ARRAY", bottomEdgesWithTwoColours)
            writeLeftEdgesArrayToMap("LEFT_EDGES_COLOUR_ARRAY", leftEdgesWithTwoColours)
        }
        writeMidsArrayToMap("MIDS_BICOLOUR_ARRAY", midsWithTwoColours)
    }

    private fun BufferedWriter.writeCornersToMap() {
        val topLeftBicolour = toBicolour(0, 0)
        val topLeftTileoris = topLeftCornersWithTwoColours[topLeftBicolour]!!
        write("\npub static TOP_LEFT_CORNER_OFFSET: usize = $runningCount;")
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
        val maxMidColour = if (midsOnly) { numMidColours.size + 1 } else { numMidColours.size }
        write("\npub static $arrayName: [[u16; $maxMidColour]; $maxMidColour] = [")

        (1..maxMidColour).forEach { mainColour ->
            write("\n    [")
            (1..maxMidColour).forEach { acColour ->
                val bicolour = toBicolour(mainColour, acColour)
                if (bicolourMap.containsKey(bicolour)) {
                    val tileoris = bicolourMap[bicolour]!!
                    write("$runningCount, ")
                    incrementRunningCount(tileoris.size)
                } else {
                    write("0, ")
                }
            }
            if (mainColour > anyColour) {
                write("], // north ANY")
            } else {
                write("], // north $mainColour")
            }
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

            if (midsOnly) {
                listOf(
                    TileOri(idx, Orientation.BASE.toInt(), toBicolour(fileTile[0], anyColour + 1)),
                    TileOri(idx, Orientation.CLOCKWISE_90.toInt(), toBicolour(fileTile[3], anyColour + 1)),
                    TileOri(idx, Orientation.HALF.toInt(), toBicolour(fileTile[2], anyColour + 1)),
                    TileOri(idx, Orientation.ANTICLOCKWISE_90.toInt(), toBicolour(fileTile[1], anyColour + 1)),
                    TileOri(idx, Orientation.BASE.toInt(), toBicolour(anyColour + 1, fileTile[3])),
                    TileOri(idx, Orientation.CLOCKWISE_90.toInt(), toBicolour(anyColour + 1, fileTile[2])),
                    TileOri(idx, Orientation.HALF.toInt(), toBicolour(anyColour + 1, fileTile[1])),
                    TileOri(idx, Orientation.ANTICLOCKWISE_90.toInt(), toBicolour(anyColour + 1, fileTile[0])),
                    TileOri(idx, Orientation.BASE.toInt(), toBicolour(anyColour + 1, anyColour + 1)),
                    TileOri(idx, Orientation.CLOCKWISE_90.toInt(), toBicolour(anyColour + 1, anyColour + 1)),
                    TileOri(idx, Orientation.HALF.toInt(), toBicolour(anyColour + 1, anyColour + 1)),
                    TileOri(idx, Orientation.ANTICLOCKWISE_90.toInt(), toBicolour(anyColour + 1, anyColour + 1)),
                ).forEach { tileOri ->
                    if (tempWithColour[tileOri.biColour] == null) {
                        tempWithColour[tileOri.biColour] = mutableListOf()
                    }
                    tempWithColour[tileOri.biColour]!!.add(tileOri)
                }
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
