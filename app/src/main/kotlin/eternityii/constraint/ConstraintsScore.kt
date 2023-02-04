package eternityii.constraint

import eternityii.display.fmt

/**
 * Represents the 'score' for a specific set of [Constraints]. This helps to evaluate which is
 * better, or if progress is being made.
 */
data class ConstraintsScore(
    private val numOptions: Int,
    private val numColourOptions: Int,
    private val numCellsForTiles: Int,
    private val isStillSolvable: Boolean
) {
    private val score: Long = numOptions.toLong() * numColourOptions * numCellsForTiles

    override fun toString(): String =
        "Score(${score.fmt()}  ${numOptions.fmt()}  " +
            "${numColourOptions.fmt()}  ${numCellsForTiles.fmt()}  $isStillSolvable)"
}
