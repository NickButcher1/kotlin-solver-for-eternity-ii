package eternityii.constraint

import eternityii.builder.QuadBuilder
import eternityii.data.Orientation
import eternityii.data.TileData
import eternityii.data.TileType

class ConstraintSolver(
    private val tileData: TileData,
    private val verboseMode: Boolean
) {
    private val taskQueue: MutableList<Task> = mutableListOf()

    private val constraintData: ConstraintData

    init {
        val quadBuilder = QuadBuilder(tileData)
        val quadCorners = listOf(
            quadBuilder.get2x2Corners(0U),
            quadBuilder.get2x2Corners(1U),
            quadBuilder.get2x2Corners(2U),
            quadBuilder.get2x2Corners(3U)
        )

        constraintData = ConstraintData(quadCorners)
    }

    fun solveAllCornerPermutations() {
        val scores = mutableListOf<Pair<List<UByte>, ConstraintsScore>>()
        for (c0 in 0..3) {
            for (c1 in 0..3) {
                for (c2 in 0..3) {
                    if (c0 != c1 && c0 != c2 && c1 != c2) {
                        val constraints = Constraints(tileData, constraintData, verboseMode)
                        val corners = listOf(c0.toUByte(), c1.toUByte(), c2.toUByte())
                        val score = solve(corners, constraints)
                        scores.add(Pair(corners, score))
                    }
                }
            }
        }

        println("SCORES BY CORNER")
        scores.forEach { score ->
            println("  ${score.first}  ${score.second}")
        }
    }

    private fun solve(
        corners: List<UByte>,
        constraints: Constraints
    ): ConstraintsScore {
        taskQueue.add(Task.Print)
        taskQueue.add(Task.FixClue(0))
        taskQueue.add(Task.FixClue(1))
        taskQueue.add(Task.FixClue(2))
        taskQueue.add(Task.FixClue(3))
        taskQueue.add(Task.FixClue(4))

        // Fix up to three corners. No need to fix the fourth - the solver will do that
        // automatically.
        taskQueue.add(Task.FixTile(TileType.CORNER, corners[0], Orientation.CLOCKWISE_90, 0, 0))
        taskQueue.add(Task.FixTile(TileType.CORNER, corners[1], Orientation.HALF, 0, 15))
        taskQueue.add(Task.FixTile(TileType.CORNER, corners[2], Orientation.BASE, 15, 0))

        // Now fix some more.
//        taskQueue.add(Task.FixTile(TileType.MID, 0U, Orientation.ANTICLOCKWISE_90, 3, 3))
//        taskQueue.add(Task.FixTile(TileType.MID, 1U, Orientation.BASE, 4, 4))
        taskQueue.add(Task.FixTile(TileType.MID, (118U - 61U).toUByte(), Orientation.BASE, 1, 12))
//        taskQueue.add(Task.FixTile(TileType.MID, (221U - 61U).toUByte(), Orientation.ANTICLOCKWISE_90, 14, 3))

        // Now start it recalculating.
        taskQueue.add(Task.RecalculateAll)
        taskQueue.add(Task.Print)

        var lastTask: Task? = null

        while (taskQueue.isNotEmpty()) {
            val task = taskQueue.removeAt(0)
            val score = constraints.score
            println("PROCESS TASK $score: $task")

            when (task) {
                is Task.Print -> {
                    if (lastTask != Task.Print) {
                        constraints.print()
                    }
                }

                is Task.FixClue -> doFixClue(task.clueIndex)

                is Task.FixTile -> doFixTile(task, constraints)

                is Task.RecalculateAll -> {
                    val stillSolvable = constraints.recalculateAll()
                    if (!stillSolvable) {
                        taskQueue.add(0, Task.Unsolvable)
                        taskQueue.add(0, Task.Print)
                    } else if (constraints.score != score) {
                        // Recalculating made progress, then always do another pass.
                        taskQueue.add(Task.RecalculateAll)
                        taskQueue.add(Task.Print)
                    }
                }

                is Task.Unsolvable -> break
            }
            lastTask = task
        }

        println("NOTHING TO DO ${constraints.score}")
        constraints.buildUrl()

        return constraints.score
    }

    private fun doFixClue(clueIndex: Int) {
        val clue = tileData.clues[clueIndex]
        val task = Task.FixTile(
            TileType.MID,
            clue.midId,
            clue.orientation,
            clue.row,
            clue.col
        )
        taskQueue.add(0, task)
    }

    private fun doFixTile(task: Task.FixTile, constraints: Constraints) {
        constraints.fixTile(
            task.tileType,
            task.id,
            task.orientation,
            task.row,
            task.col
        )
    }
}
