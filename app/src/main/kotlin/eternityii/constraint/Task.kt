package eternityii.constraint

import eternityii.data.TileType

/**
 * Tasks that the [ConstraintSolver] can perform.
 */
sealed class Task {
    object Print : Task() {
        override fun toString(): String = "Print"
    }

    class FixClue(val clueIndex: Int) : Task() {
        override fun toString(): String = "Fix clue $clueIndex"
    }

    class FixTile(
        val tileType: TileType,
        val id: UByte,
        val orientation: UByte,
        val row: Int,
        val col: Int
    ) : Task() {
        override fun toString(): String = "Fix tile $tileType $id $orientation at ($row,$col)"
    }

    object RecalculateAll : Task() {
        override fun toString(): String = "Recalculate all"
    }

    object Unsolvable : Task() {
        override fun toString(): String = "Unsolvable"
    }
}
