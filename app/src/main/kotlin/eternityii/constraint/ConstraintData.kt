@file:OptIn(ExperimentalUnsignedTypes::class)

package eternityii.constraint

import eternityii.QuadSquares

/**
 * Represents immutable data about combinations of tiles.
 *
 * @property quadCorners All possible top left corner 2x2 blocks.  Lookup with
 * quadCorners[cornerId].
 */
data class ConstraintData(
    val quadCorners: List<QuadSquares>
)
