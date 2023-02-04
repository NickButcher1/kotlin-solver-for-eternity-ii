package eternityii

import eternityii.backtracker.Backtracker
import eternityii.backtracker.DiagonalBacktrackerPath
import eternityii.backtracker.EdgeBacktrackerPath
import eternityii.backtracker.ScanrowBacktrackerPath
import eternityii.backtracker.ScanrowMidsOnlyBacktrackerPath
import eternityii.backtracker.ScanrowSquaresBacktrackerPath
import eternityii.backtracker.SwappableEdgeBacktracker
import eternityii.builder.ClueSolver
import eternityii.builder.EdgeBuilder
import eternityii.builder.QuadBuilder
import eternityii.constraint.ConstraintSolver
import eternityii.data.TileData

/**
 * Parameters - use any one of the following:
 *
 * 'constraint [verbose]' to run ConstraintSolver.
 *
 * 'fullback maxDepth [verbose]' to run a backtracker - 16x16 with scanrow path.
 * 'squareback maxDepth [verbose]' to run a backtracker - 16x16 with expanding squares path.
 * 'midback maxDepth [verbose]' to run a backtracker - 14x14 mid tiles only scanrow path.
 * 'edgeback maxDepth [verbose]' to run a backtracker - corner/edge tiles only, alternating clockwise and anticlockwise path.
 * 'diagback maxDepth [verbose]' to run a backtracker - corner/edge tiles only, clockwise path.
 * 'swappableedgeback maxDepth [verbose]' to run a backtracker - corner/edge tiles only, ignoring different mid colours.
 *
 * 'edge' to build all ways to place a corner + N edge tiles (both directions) and N edge tiles.
 *
 * 'quad' to build all 2x2 mid and some 3x3 blocks
 *
 * 'clue' to print just the mandatory tile and four clue tiles.
 *
 * Extra parameters: use with any of the above:
 *
 * 'random' to randomise the tile order on startup.
 * 'debug' to print all TileData on startup.
 */
fun main(args: Array<String>) {
    fun Array<String>.depthArg() = this[1].toInt()
    val verboseMode = "verbose" in args

    val tileData = TileData("random" in args, "debug" in args)
    val quadBuilder = QuadBuilder(tileData)

    when (args[0]) {
        "quad" -> {
            quadBuilder.solveCorners()
            quadBuilder.solveEdges()
            quadBuilder.solveMids()
        }

        "fullback" ->
            Backtracker(tileData, ScanrowBacktrackerPath, args.depthArg(), verboseMode).solve()

        "squareback" ->
            Backtracker(tileData, ScanrowSquaresBacktrackerPath, args.depthArg(), verboseMode).solve()

        "midback" ->
            Backtracker(tileData, ScanrowMidsOnlyBacktrackerPath, args.depthArg(), verboseMode).solve()

        "edgeback" ->
            Backtracker(tileData, EdgeBacktrackerPath, args.depthArg(), verboseMode).solve()

        "swappableedgeback" ->
            SwappableEdgeBacktracker(tileData, EdgeBacktrackerPath, args.depthArg(), verboseMode).solve()

        "diagback" ->
            Backtracker(tileData, DiagonalBacktrackerPath, args.depthArg(), verboseMode).solve()

        "edge" -> {
            val edgeBuilder = EdgeBuilder(tileData)
            edgeBuilder.solveCornerPlusEdgesClockwise(4)
            edgeBuilder.solveCornerPlusEdgesAnticlockwise(4)
            edgeBuilder.solveEdgesClockwise(4)
            edgeBuilder.solveOneEdge()
        }

        "clue" -> ClueSolver(tileData).solve()

        "constraint" -> ConstraintSolver(tileData, verboseMode).solveAllCornerPermutations()
    }
}
