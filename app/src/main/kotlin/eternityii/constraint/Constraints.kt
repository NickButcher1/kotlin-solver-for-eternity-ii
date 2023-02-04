@file:OptIn(ExperimentalUnsignedTypes::class)

package eternityii.constraint

import eternityii.CellIterator
import eternityii.data.Compass
import eternityii.data.Orientation
import eternityii.data.TileData
import eternityii.data.TileType
import eternityii.display.Display

/**
 * Represents the board and a set of constraints on what tiles can be placed where:
 *
 * - A [CellConstraint] for each board cell. This represents either a fixed tile/orientation or a
 *   list of possible tile/orientation pairs for that cell. The goal is to arrive at a single
 *   tile/orientation for each cell.
 *
 * - A list of possible colours for each edge between two adjacent cells. See
 *   [horizontalColourConstraints] and [verticalColourConstraints].
 *
 * Additional data is calculated from these constraints, but is not itself a constraint:
 *
 * - [numCellsForCornerTile], [numCellsForEdgeTile] and [numCellsForMidTile] for each tile.
 *
 * Constraints are reduced in the following ways, whenever a [CellConstraint] is reduced, for
 * example by fixing a tile in a cell.
 *
 * - If a tile is fixed, eliminate it from all other cells
 * - If a [CellConstraint] is reduced, recalculate all other [CellConstraint].
 * - Reduce the list of possible edge colours, using the CellConstraints.
 * - Reduce the CellConstraints, using the possible edge colours.
 * - At the moment, constraints only propagate to adjacent cells.
 */
class Constraints(
    private val tileData: TileData,
    private val constraintData: ConstraintData,
    private val verboseMode: Boolean
) {
    /**
     * constraints[row][col] returns the constraint on that grid cell.
     */
    private val constraints: MutableList<MutableList<CellConstraint>> = mutableListOf()

    /**
     * horizontalColourConstraints[row 0-15][0-14] returns a list of valid colours between two
     * cells in the same ROW. Column index is N for the edge between cells N and N+1.
     */
    private val horizontalColourConstraints: MutableList<MutableList<List<UByte>>> =
        mutableListOf()

    /**
     * verticalColourConstraints[0-14][col 0-15] returns a list of valid colours between two cells
     * in the same COLUMN. Row index is N for the edge between cells N and N+1.
     */
    private val verticalColourConstraints: MutableList<MutableList<List<UByte>>> =
        mutableListOf()

    private var numCellsForCornerTile: UByteArray = UByteArray(4)
    private var numCellsForEdgeTile: UByteArray = UByteArray(56)
    private var numCellsForMidTile: UByteArray = UByteArray(196)

    private var isStillSolvable = true

    init {
        val allCornerIds = (0..3).map { id -> id.toUByte() }.toList()
        val allEdgeIds = (0..55).map { id -> id.toUByte() }.toList()
        val allMidIds = mutableListOf<UByte>()
        val allMidOris = mutableListOf<UByte>()
        for (midId in 0..195) {
            for (ori in 0..3) {
                allMidIds.add(midId.toUByte())
                allMidOris.add(ori.toUByte())
            }
        }

        for (row in 0..15) {
            val rowList = mutableListOf<CellConstraint>()
            constraints.add(rowList)

            for (col in 0..15) {
                val constraint = when {
                    row == 0 && col == 0 ->
                        CellConstraint.FixedOrientation(
                            TileType.CORNER,
                            allCornerIds.toMutableList(),
                            Orientation.CLOCKWISE_90
                        )

                    row == 0 && col == 15 ->
                        CellConstraint.FixedOrientation(
                            TileType.CORNER,
                            allCornerIds.toMutableList(),
                            Orientation.HALF
                        )

                    row == 15 && col == 0 ->
                        CellConstraint.FixedOrientation(
                            TileType.CORNER,
                            allCornerIds.toMutableList(),
                            Orientation.BASE
                        )

                    row == 15 && col == 15 ->
                        CellConstraint.FixedOrientation(
                            TileType.CORNER,
                            allCornerIds.toMutableList(),
                            Orientation.ANTICLOCKWISE_90
                        )

                    row == 0 ->
                        CellConstraint.FixedOrientation(
                            TileType.EDGE,
                            allEdgeIds.toMutableList(),
                            Orientation.HALF
                        )

                    row == 15 ->
                        CellConstraint.FixedOrientation(
                            TileType.EDGE,
                            allEdgeIds.toMutableList(),
                            Orientation.BASE
                        )

                    col == 0 ->
                        CellConstraint.FixedOrientation(
                            TileType.EDGE,
                            allEdgeIds.toMutableList(),
                            Orientation.CLOCKWISE_90
                        )

                    col == 15 ->
                        CellConstraint.FixedOrientation(
                            TileType.EDGE,
                            allEdgeIds.toMutableList(),
                            Orientation.ANTICLOCKWISE_90
                        )

                    else ->
                        CellConstraint.Options(
                            TileType.MID,
                            allMidIds.toMutableList(),
                            allMidOris.toMutableList()
                        )
                }
                rowList.add(constraint)
            }
        }

        recalculateNumCellsForTiles()
        recalculateEdgeColours()
    }

    private fun recalculateNumCellsForTiles() {
        numCellsForCornerTile = UByteArray(4)
        numCellsForEdgeTile = UByteArray(56)
        numCellsForMidTile = UByteArray(196)

        CellIterator().forEach { (row, col) ->
            val constraint = constraints[row][col]

            for (cornerId in 0..3) {
                if (constraint.containsTile(TileType.CORNER, cornerId.toUByte())) {
                    numCellsForCornerTile[cornerId]++
                }
            }

            for (edgeId in 0..55) {
                if (constraint.containsTile(TileType.EDGE, edgeId.toUByte())) {
                    numCellsForEdgeTile[edgeId]++
                }
            }

            for (midId in 0..195) {
                if (constraint.containsTile(TileType.MID, midId.toUByte())) {
                    numCellsForMidTile[midId]++
                }
            }
        }
    }

    private fun recalculateEdgeColours() {
        horizontalColourConstraints.clear()
        verticalColourConstraints.clear()

        for (row in 0..15) {
            val rowList = mutableListOf<List<UByte>>()
            horizontalColourConstraints.add(rowList)

            for (col in 0..14) {
                val validColoursEast = calculatePossibleColours(row, col, Compass.EAST)
                val validColoursWest = calculatePossibleColours(row, col + 1, Compass.WEST)
                val validColoursCombined = validColoursEast.distinct()
                    .intersect(validColoursWest.distinct().toSet()).toList().sorted()
                rowList.add(validColoursCombined)
            }
        }

        for (row in 0..14) {
            val rowList = mutableListOf<List<UByte>>()
            verticalColourConstraints.add(rowList)

            for (col in 0..15) {
                val validColoursSouth = calculatePossibleColours(row, col, Compass.SOUTH)
                val validColoursNorth = calculatePossibleColours(row + 1, col, Compass.NORTH)
                val validColoursCombined = validColoursSouth.distinct()
                    .intersect(validColoursNorth.distinct().toSet()).toList().sorted()
                rowList.add(validColoursCombined)
            }
        }
    }

    fun buildUrl() {
        val oris: MutableList<UByte> = mutableListOf()
        val tileIds: MutableList<UByte> = mutableListOf()
        val cellIds: MutableList<Int> = mutableListOf()
        val tileTypes: MutableList<TileType> = mutableListOf()
        var idx = 0

        CellIterator().forEach { (row, col) ->
            val constraint = constraints[row][col]
            if (constraint is CellConstraint.Solved) {
                tileIds.add(constraint.tileId)
                oris.add(constraint.orientation)
                cellIds.add(idx)
                tileTypes.add(constraint.tileType)
                idx++
            } else {
                cellIds.add(-1)
            }
        }
        Display.buildUrlForEntireBoard(
            tileData,
            tileIds,
            oris,
            tileTypes,
            cellIds
        )
    }

    /**
     * Print the grid (blank for solved cell, otherwise number of possible (tile ID, orientation)
     * pairs. Then print the detailed constraints for each cell.
     */
    fun print() {
        printCellGrid()
        printEdgeGrid()
        if (verboseMode) {
            printCellConstraints()
            printNumCellsForTiles()
            printColourConstraints()
        }
    }

    private fun printCellGrid() {
        println("Cell Grid:")
        val output = buildString {
            fun gridRow() {
                append("\n+")
                repeat(16) { append("-----+") }
            }

            for (row in 0..15) {
                gridRow()
                append("\n|")
                for (col in 0..15) {
                    when (constraints[row][col]) {
                        is CellConstraint.Solved -> append("     |")
                        is CellConstraint.FixedTileId -> append("  FT |")
                        is CellConstraint.FixedOrientation -> append("  FO |")
                        is CellConstraint.Options -> append(" OPT |")
                    }
                }
                append("\n|")
                for (col in 0..15) {
                    when (val constraint = constraints[row][col]) {
                        is CellConstraint.Solved -> append("     |")
                        is CellConstraint.FixedTileId -> append("     |")
                        is CellConstraint.FixedOrientation -> append("     |")
                        is CellConstraint.Options ->
                            append(" ${constraint.ids.size.toString().padStart(3, ' ')} |")
                    }
                }

                append("\n|")
                for (col in 0..15) {
                    when (val constraint = constraints[row][col]) {
                        is CellConstraint.Solved -> append("     |")
                        is CellConstraint.FixedTileId ->
                            append(" ${constraint.orientations.size.toString().padStart(3, ' ')} |")
                        is CellConstraint.FixedOrientation ->
                            append(" ${constraint.ids.size.toString().padStart(3, ' ')} |")
                        is CellConstraint.Options ->
                            append(" ${constraint.ids.distinct().size.toString().padStart(3, ' ')} |")
                    }
                }
            }
            gridRow()
        }

        println(output)
    }

    private fun printEdgeGrid() {
        println("Edge Grid:")
        val output = buildString {
            fun gridRow() {
                append("\n+")
                repeat(16) { append("-----+") }
            }

            fun addBlankRow() {
                append("\n|")
                repeat(16) { append("     |") }
            }

            fun addHorizontalRow(row: Int) {
                append("\n|")
                for (col in 0..14) {
                    val numColours = horizontalColourConstraints[row][col].size
                    if (numColours == 1) {
                        append("      ")
                    } else {
                        append("    ${numColours.toString().padStart(2, ' ')}")
                    }
                }
                append("     |")
            }

            fun addColumnRow(row: Int) {
                append("\n+")
                for (col in 0..15) {
                    val numColours = verticalColourConstraints[row][col].size
                    append(
                        when {
                            numColours == 1 -> "-   -+"
                            numColours < 10 -> "- $numColours -+"
                            else -> "- ${numColours.toString().padStart(2, ' ')}-+"
                        }
                    )
                }
            }

            gridRow()

            for (row in 0..15) {
                addBlankRow()
                addHorizontalRow(row)
                addBlankRow()
                if (row != 15) {
                    addColumnRow(row)
                }
            }

            gridRow()
        }

        println(output)
    }

    private fun printCellConstraints() {
        println("\nCell constraints")

        CellIterator().forEach { (row, col) ->
            val constraint = constraints[row][col]
            println("  Cell $row,$col: $constraint")
        }
    }

    private fun printColourConstraints() {
        println("\nColour constraints")

        for (row in 0..15) {
            for (col in 0..14) {
                val colours = horizontalColourConstraints[row][col]
                println("  Cell $row,$col east edge: (${colours.size}) ${colours.joinToString()}")
            }
        }

        for (row in 0..14) {
            for (col in 0..15) {
                val colours = verticalColourConstraints[row][col]
                println("  Cell $row,$col south edge: (${colours.size}) ${colours.joinToString()}")
            }
        }
    }

    val score: ConstraintsScore
        get() = ConstraintsScore(
            numOptions(),
            numColourOptions(),
            numCellsForTiles(),
            isStillSolvable
        )

    /** This number should go down as the constraints are eliminated. */
    private fun numOptions(): Int {
        var totalOptions = 0

        CellIterator().forEach { (row, col) ->
            val constraint = constraints[row][col]
            totalOptions += constraint.numOptions
        }
        return totalOptions
    }

    /** This number should go down as the constraints are eliminated. */
    private fun numColourOptions(): Int {
        var totalOptions = 0

        for (row in 0..15) {
            for (col in 0..14) {
                totalOptions += horizontalColourConstraints[row][col].size
            }
        }

        for (row in 0..14) {
            for (col in 0..15) {
                totalOptions += verticalColourConstraints[row][col].size
            }
        }

        return totalOptions
    }

    private fun numCellsForTiles(): Int {
        var numOptions = 0

        for (cornerId in 0..3) {
            numOptions += numCellsForCornerTile[cornerId].toInt() - 1
        }
        for (edgeId in 0..55) {
            numOptions += numCellsForEdgeTile[edgeId].toInt() - 1
        }
        for (midId in 0..195) {
            numOptions += numCellsForMidTile[midId].toInt() - 1
        }

        return numOptions
    }

    private fun printNumCellsForTiles() {
        println("\nNumber of possible cells for each corner tile:")
        for (cornerId in 0..3) {
            println("  $cornerId  ${numCellsForCornerTile[cornerId]}")
        }
        println("\nNumber of possible cells for each edge tile:")
        for (edgeId in 0..55) {
            println("  $edgeId  ${numCellsForEdgeTile[edgeId]}")
        }
        println("\nNumber of possible cells for each mid tile:")
        for (midId in 0..195) {
            println("  $midId  ${numCellsForMidTile[midId]}")
        }
    }

    fun fixTile(
        tileType: TileType,
        id: UByte,
        orientation: UByte,
        fixRow: Int,
        fixCol: Int
    ) {
        // Fix the tile in the new cell.
        val newSolvedConstraint = CellConstraint.Solved(tileType, id, orientation)
        constraints[fixRow][fixCol] = newSolvedConstraint

        removeTileFromAllOtherCells(tileType, id, fixRow, fixCol)
    }

    private fun removeTileFromAllOtherCells(
        tileType: TileType,
        id: UByte,
        fixRow: Int,
        fixCol: Int
    ) {
        val moreWork: MutableList<() -> Unit> = mutableListOf()

        for (row in 0..15) {
            for (col in 0..15) {
                if (row == fixRow && col == fixCol) {
                    continue
                }

                val newConstraint = when (val constraint = constraints[row][col]) {
                    is CellConstraint.Solved -> {
                        constraint
                    }

                    is CellConstraint.FixedTileId -> {
                        require(tileType != constraint.tileType || id != constraint.tileId)
                        constraint
                    }

                    is CellConstraint.FixedOrientation -> {
                        for (idx in (constraint.ids.size - 1) downTo 0) {
                            if (tileType == constraint.tileType && id == constraint.ids[idx]) {
                                constraint.ids.removeAt(idx)
                            }
                        }

                        if (constraint.ids.size == 1) {
                            println("Fixed another cell at ($row,$col) was FixedOrientation")
                            moreWork.add {
                                removeTileFromAllOtherCells(
                                    constraint.tileType,
                                    constraint.ids[0],
                                    row,
                                    col
                                )
                            }
                            constraint.toSolved()
                        } else {
                            constraint
                        }
                    }

                    is CellConstraint.Options -> {
                        for (idx in (constraint.ids.size - 1) downTo 0) {
                            if (tileType == constraint.tileType && id == constraint.ids[idx]) {
                                constraint.ids.removeAt(idx)
                                constraint.orientations.removeAt(idx)
                            }
                        }

                        when {
                            constraint.ids.size == 1 -> {
                                println("Fixed another cell at ($row,$col) was Options")
                                moreWork.add {
                                    removeTileFromAllOtherCells(
                                        constraint.tileType,
                                        constraint.ids[0],
                                        row,
                                        col
                                    )
                                }
                                constraint.toSolved()
                            }

                            constraint.ids.distinct().size == 1 -> constraint.toFixedTileId()

                            constraint.orientations.distinct().size == 1 -> constraint.toFixedOrientation()

                            else -> constraint
                        }
                    }
                }
                constraints[row][col] = newConstraint
            }
        }

        moreWork.forEach { work -> work.invoke() }
    }

    private fun calculatePossibleColours(row: Int, col: Int, compass: UByte): List<UByte> =
        constraints[row][col].possibleColours(tileData, compass)

    /**
     * @return true if not proved unsolvable, false if proved unsolvable.
     */
    fun recalculateAll(): Boolean {
        recalculateNumCellsForTiles()
        recalculateEdgeColours()
        recalculateConstraintsFromEdgeColours()
        recalculateConstraintsFromAdjacents()
        return isStillSolvable
    }

    private fun recalculateConstraintsFromEdgeColours() {
        val moreWork: MutableList<() -> Unit> = mutableListOf()

        CellIterator().forEach { (row, col) ->
            val eastColours = if (col != 15) {
                horizontalColourConstraints[row][col]
            } else {
                emptyList()
            }
            val westColours = if (col != 0) {
                horizontalColourConstraints[row][col - 1]
            } else {
                emptyList()
            }
            val southColours = if (row != 15) {
                verticalColourConstraints[row][col]
            } else {
                emptyList()
            }
            val northColours = if (row != 0) {
                verticalColourConstraints[row - 1][col]
            } else {
                emptyList()
            }

            val newConstraint = when (val constraint = constraints[row][col]) {
                is CellConstraint.Solved -> {
                    constraint
                }

                is CellConstraint.FixedTileId -> {
                    for (idx in (constraint.orientations.size - 1) downTo 0) {
                        if ((row != 0 && constraint.colour(tileData, idx, Compass.NORTH) !in northColours) ||
                            (col != 15 && constraint.colour(tileData, idx, Compass.EAST) !in eastColours) ||
                            (row != 15 && constraint.colour(tileData, idx, Compass.SOUTH) !in southColours) ||
                            (col != 0 && constraint.colour(tileData, idx, Compass.WEST) !in westColours)
                        ) {
                            constraint.orientations.removeAt(idx)
                        }
                    }
                    if (constraint.orientations.size == 1) {
                        println("Fixed another cell at ($row,$col) was FixedTileId")
                        constraint.toSolved()
                    } else {
                        if (constraint.orientations.size == 0) {
                            isStillSolvable = false
                        }
                        constraint
                    }
                }

                is CellConstraint.FixedOrientation -> {
                    for (idx in (constraint.ids.size - 1) downTo 0) {
                        if ((row != 0 && constraint.colour(tileData, idx, Compass.NORTH) !in northColours) ||
                            (col != 15 && constraint.colour(tileData, idx, Compass.EAST) !in eastColours) ||
                            (row != 15 && constraint.colour(tileData, idx, Compass.SOUTH) !in southColours) ||
                            (col != 0 && constraint.colour(tileData, idx, Compass.WEST) !in westColours)
                        ) {
                            constraint.ids.removeAt(idx)
                        }
                    }

                    if (constraint.ids.size == 1) {
                        println("Fixed another cell at ($row,$col) was FixedOrientation")
                        moreWork.add {
                            removeTileFromAllOtherCells(
                                constraint.tileType,
                                constraint.ids[0],
                                row,
                                col
                            )
                        }
                        constraint.toSolved()
                    } else {
                        if (constraint.ids.size == 0) {
                            isStillSolvable = false
                        }
                        constraint
                    }
                }

                is CellConstraint.Options -> {
                    for (idx in (constraint.ids.size - 1) downTo 0) {
                        if ((row != 0 && constraint.colour(tileData, idx, Compass.NORTH) !in northColours) ||
                            (col != 15 && constraint.colour(tileData, idx, Compass.EAST) !in eastColours) ||
                            (row != 15 && constraint.colour(tileData, idx, Compass.SOUTH) !in southColours) ||
                            (col != 0 && constraint.colour(tileData, idx, Compass.WEST) !in westColours)
                        ) {
                            constraint.ids.removeAt(idx)
                            constraint.orientations.removeAt(idx)
                        }
                    }

                    when {
                        constraint.ids.size == 1 -> {
                            println("Fixed another cell at ($row,$col) was Options")
                            moreWork.add {
                                removeTileFromAllOtherCells(
                                    constraint.tileType,
                                    constraint.ids[0],
                                    row,
                                    col
                                )
                            }
                            constraint.toSolved()
                        }

                        constraint.ids.distinct().size == 1 -> constraint.toFixedTileId()

                        constraint.orientations.distinct().size == 1 -> constraint.toFixedOrientation()

                        else -> {
                            if (constraint.ids.size == 0) {
                                isStillSolvable = false
                            }
                            constraint
                        }
                    }
                }
            }
            constraints[row][col] = newConstraint
        }

        moreWork.forEach { work -> work.invoke() }
    }

    private fun recalculateConstraintsFromAdjacents() {
//        val quadSquares = constraintData.quadCorners[0]
//        val cellX1Y1Constraint = constraints[1][1]

        // TODO If CellConstraint is Solved -> reduce the other 3 CellConstraints if possible (might do better than
        // already reduced constraints).
        // TODO If CellConstraint is FixedId -> reduce the other 3 CellConstraints if possible (might do better than
        // already reduced constraints).
        // TODO If CellConstraint is FixedOrientation -> reduce the other 3 CellConstraints if possible (might do better
        // than already reduced constraints).
        // TODO If CellConstraint is Options -> reduce the other 3 CellConstraints if possible (might do better than
        // already reduced constraints).

        // TODO For each of the above 4 cases, they have built a reduced QuadSquares.

        // TODO Now use the reduced QuadSquares to reduce any non-matching entries from each of the C
    }
}
