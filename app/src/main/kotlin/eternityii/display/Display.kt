package eternityii.display

import eternityii.data.Colour
import eternityii.data.Compass
import eternityii.data.TileData
import eternityii.data.TileType

fun Int.fmt(): String = this.toLong().fmt()

fun Long.fmt(): String =
    this.toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()

@OptIn(ExperimentalUnsignedTypes::class)
object Display {
    const val BASE_URL = "https://e2.bucas.name/#puzzle=NickB&board_w=16" +
        "&board_h=16&motifs_order=jblackwood&board_edges="

    const val BOARD_PIECES_PARAM = "&board_pieces="

    private val CLUES = listOf(
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 2, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, 0, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 4, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    )

    /**
     * Build a URL for e2.bucas.name.
     *
     * The URL comprises a fixed part, followed by four letters for each tile: top, right, bottom,
     * left.
     *
     * @param orientations List of orientations for each tile to include.
     * @param tileIds List of tile IDs, in order, from top left corner. Must be same length as
     * orientations.
     * @param tileData The immutable tile data.
     */
    fun buildUrl(
        tileData: TileData,
        tileIds: UByteArray,
        oris: List<UByte>,
        tileTypes: List<TileType>,
    ) {
        val tileIdsForDisplay = mutableListOf<UByte>()
        var boardUrl = BASE_URL
        var piecesUrl = BOARD_PIECES_PARAM

        (oris.indices).forEach { ii ->
            val tileId = tileData.idToRealTileId(tileTypes[ii], tileIds[ii]).toInt()
            tileIdsForDisplay.add((tileId + 1).toUByte())
            val sideN = tileData.realColours[tileData.toSide(oris[ii], Compass.NORTH)][tileId]
            val sideE = tileData.realColours[tileData.toSide(oris[ii], Compass.EAST)][tileId]
            val sideS = tileData.realColours[tileData.toSide(oris[ii], Compass.SOUTH)][tileId]
            val sideW = tileData.realColours[tileData.toSide(oris[ii], Compass.WEST)][tileId]
            boardUrl += Colour.toBucasLetter[sideN.toInt()]
            boardUrl += Colour.toBucasLetter[sideE.toInt()]
            boardUrl += Colour.toBucasLetter[sideS.toInt()]
            boardUrl += Colour.toBucasLetter[sideW.toInt()]
            piecesUrl = addTileIdToPieceUrl(piecesUrl, tileId + 1)
        }

        boardUrl += piecesUrl
        println(
            "DISPLAY:\n    $tileTypes\n    $oris\n    " +
                "${tileIdsForDisplay.joinToString()}\n    $boardUrl",
        )
    }

    /** For the entire board. tileIds must contain 256 entries, in scanrow order from top left. */
    fun buildUrlForEntireBoard(
        tileData: TileData,
        tileIds: List<UByte>,
        oris: List<UByte>,
        tileTypes: List<TileType>,
        cellIds: List<Int>,
    ) = buildUrl(tileData, tileIds, oris, tileTypes, cellIds)

    fun buildUrlForClues(
        tileData: TileData,
    ) = buildUrl(
        tileData,
        (0 until tileData.clues.size).map { idx ->
            tileData.clues[idx].midId
        }.toList(),
        (0 until tileData.clues.size).map { idx ->
            tileData.clues[idx].orientation
        }.toList(),
        listOf(TileType.MID, TileType.MID, TileType.MID, TileType.MID, TileType.MID),
        CLUES,
    )

    fun buildUrl(
        tileData: TileData,
        tileIds: List<UByte>,
        oris: List<UByte>,
        tileTypes: List<TileType>,
        cellIds: List<Int>,
    ) {
        val tileIdsForDisplay = mutableListOf<UByte>()
        var boardUrl = BASE_URL
        var piecesUrl = BOARD_PIECES_PARAM

        // Map the order of tile IDs supplied to the scan row order for the URL.
        (cellIds.indices).forEach { cellId ->
            val ii = cellIds[cellId]
            if (ii != -1 && ii < tileIds.size) {
                val tileId = tileData.idToRealTileId(tileTypes[ii], tileIds[ii]).toInt()
                tileIdsForDisplay.add((tileId + 1).toUByte())

                val sideN = tileData.realColours[tileData.toSide(oris[ii], Compass.NORTH)][tileId]
                val sideE = tileData.realColours[tileData.toSide(oris[ii], Compass.EAST)][tileId]
                val sideS = tileData.realColours[tileData.toSide(oris[ii], Compass.SOUTH)][tileId]
                val sideW = tileData.realColours[tileData.toSide(oris[ii], Compass.WEST)][tileId]
                boardUrl += Colour.toBucasLetter[sideN.toInt()]
                boardUrl += Colour.toBucasLetter[sideE.toInt()]
                boardUrl += Colour.toBucasLetter[sideS.toInt()]
                boardUrl += Colour.toBucasLetter[sideW.toInt()]
                piecesUrl = addTileIdToPieceUrl(piecesUrl, tileId + 1)
            } else {
                boardUrl = addEmptyCellToBoardUrl(boardUrl)
                piecesUrl = addEmptyPieceUrl(piecesUrl)
            }
        }

        boardUrl += piecesUrl
        println(
            "DISPLAY:\n    $tileTypes\n    $oris\n    " +
                "${tileIdsForDisplay.joinToString()}\n    $boardUrl",
        )
    }

    private fun addEmptyCellToBoardUrl(boardUrl: String): String =
        boardUrl +
            Colour.toBucasLetter[Colour.GREY.toInt()] +
            Colour.toBucasLetter[Colour.GREY.toInt()] +
            Colour.toBucasLetter[Colour.GREY.toInt()] +
            Colour.toBucasLetter[Colour.GREY.toInt()]

    private fun addTileIdToPieceUrl(pieceUrl: String, tileId: Int): String =
        "$pieceUrl${tileId.toString().padStart(3, '0')}"

    private fun addEmptyPieceUrl(pieceUrl: String): String = pieceUrl + "000"
}
