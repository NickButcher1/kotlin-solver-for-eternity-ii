package eternityii

import eternityii.backtracker.Backtracker
import eternityii.backtracker.DiagonalBacktrackerPath
import eternityii.backtracker.EdgeBacktrackerPath
import eternityii.backtracker.Scanrow11BacktrackerPath
import eternityii.backtracker.Scanrow12BacktrackerPath
import eternityii.backtracker.ScanrowBacktrackerPath
import eternityii.backtracker.ScanrowMidsOnlyBacktrackerPath
import eternityii.backtracker.ScanrowSquaresBacktrackerPath
import eternityii.backtracker.SwappableEdgeBacktracker
import eternityii.builder.ClueSolver
import eternityii.builder.EdgeBuilder
import eternityii.builder.QuadBuilder
import eternityii.constraint.ConstraintSolver
import eternityii.data.TileData
import eternityii.rustgen.RustGen

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
 * 'rustgen piecesfile [mids] [path] [random] [prefill N]' to generate Rust code for solving a specific puzzle.
 * - mids to only use the mid tiles, otherwise use all tiles.
 * - path to choose a fill order. Not compatible with mids, where only scanrow is supported for now.
 * - random to choose a random placement order for the tiles.
 * - prefill N to prefill the first N cells. If combined with random, the prefilled tiles are also chosen in random order.
 *
 * Extra parameters: use with any of the above:
 *
 * 'random' to randomise the tile order on startup.
 * 'debug' to print all TileData on startup.
 */
fun main(args: Array<String>) {
    fun Array<String>.depthArg() = this[1].toInt()
    val verboseMode = "verbose" in args

    val tileData = TileData("TODO" in args, "debug" in args)

    when (args[0]) {
        "quad" -> {
            val quadBuilder = QuadBuilder(tileData)
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

        "rustgen" -> {
            // path is only supported for a 16x16 board.
            val path = when {
                "scanrow" in args -> ScanrowBacktrackerPath
                "scanrow11" in args -> Scanrow11BacktrackerPath
                "scanrow12" in args -> Scanrow12BacktrackerPath
                "diagonal" in args -> DiagonalBacktrackerPath
                "square" in args -> ScanrowSquaresBacktrackerPath
                "mids" in args -> ScanrowMidsOnlyBacktrackerPath
                else -> null
            }

            if ("prefill" in args) {
                val depth = args[args.indexOf("prefill") + 1].toInt()
                try {
                    Backtracker(
                        tileData,
                        path ?: ScanrowBacktrackerPath,
                        depth,
                        verboseMode,
                        stopOnFirstSolution = true
                    ).solve()
                } catch (e: SolutionFoundException) {
                    RustGen(
                        tileData,
                        inputFilename = args[1],
                        path = path,
                        randomOrder = "random" in args,
                        midsOnly = "mids" in args,
                        prefillData = e
                    ).generate()
                }

            } else {
                RustGen(
                    tileData,
                    inputFilename = args[1],
                    path = path,
                    randomOrder = "random" in args,
                    midsOnly = "mids" in args
                ).generate()
            }
        }
    }
}
