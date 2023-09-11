@file:OptIn(ExperimentalUnsignedTypes::class)

package eternityii.constraint

import eternityii.data.TileData
import eternityii.data.TileType

/**
 * The constraints on a specific board cell.  There are four possibilities:
 *
 * - Known tile ID and orientation.
 * - Known tile ID, but multiple possible orientations.
 * - Known orientation, but multiple possible tile IDs.
 * - Multiple possible (tile ID, orientation) pairs.
 *
 * Tile IDs here are cornerId, edgeId or midId.
 */
sealed class CellConstraint(val tileType: TileType) {
    abstract val numOptions: Int

    abstract fun containsTile(tileType: TileType, id: UByte): Boolean

    abstract fun possibleColours(tileData: TileData, compass: UByte): List<UByte>

    /** Reduce a wider Constraint to [Solved]. Only call if you know this is correct. */
    abstract fun toSolved(): Solved

    class Solved(
        tileType: TileType,
        val tileId: UByte,
        val orientation: UByte,
    ) : CellConstraint(tileType) {
        override val numOptions: Int = 0

        override fun containsTile(tileType: TileType, id: UByte): Boolean =
            tileType == this.tileType && id == this.tileId

        override fun possibleColours(tileData: TileData, compass: UByte): List<UByte> =
            listOf(
                tileData.toSide(tileType, tileId, orientation, compass),
            )

        override fun toSolved(): Solved {
            throw IllegalArgumentException()
        }

        override fun toString(): String = "Solved: $tileType ID $tileId, ori $orientation"
    }

    class FixedTileId(
        tileType: TileType,
        val tileId: UByte,
        val orientations: MutableList<UByte>,
    ) : CellConstraint(tileType) {
        override val numOptions: Int = orientations.size

        override fun containsTile(tileType: TileType, id: UByte): Boolean =
            tileType == this.tileType && id == this.tileId

        override fun possibleColours(tileData: TileData, compass: UByte): List<UByte> {
            val possibleColours = mutableListOf<UByte>()
            for (orientation in orientations) {
                possibleColours.add(
                    tileData.toSide(tileType, tileId, orientation, compass),
                )
            }
            return possibleColours
        }

        override fun toSolved(): Solved = Solved(tileType, tileId, orientations[0])

        /** The colour for a specific entry in the options and a specific compass direction. */
        fun colour(tileData: TileData, idx: Int, compass: UByte): UByte =
            tileData.toSide(tileType, tileId, orientations[idx], compass)

        override fun toString(): String = "Fixed ID: $tileId, oris: ${orientations.joinToString()}"
    }

    class FixedOrientation(
        tileType: TileType,
        val ids: MutableList<UByte>,
        val orientation: UByte,
    ) : CellConstraint(tileType) {
        override val numOptions: Int = ids.size

        override fun containsTile(tileType: TileType, id: UByte): Boolean =
            tileType == this.tileType && id in ids

        override fun possibleColours(tileData: TileData, compass: UByte): List<UByte> {
            val possibleColours = mutableListOf<UByte>()
            for (id in ids) {
                possibleColours.add(
                    tileData.toSide(tileType, id, orientation, compass),
                )
            }
            return possibleColours
        }

        override fun toSolved(): Solved = Solved(tileType, ids[0], orientation)

        /** The colour for a specific entry in the options and a specific compass direction. */
        fun colour(tileData: TileData, idx: Int, compass: UByte): UByte =
            tileData.toSide(tileType, ids[idx], orientation, compass)

        override fun toString(): String =
            "Fixed Ori: $orientation, IDs (${ids.size}): ${ids.joinToString()}"
    }

    class Options(
        tileType: TileType,
        val ids: MutableList<UByte>,
        val orientations: MutableList<UByte>,
    ) : CellConstraint(tileType) {
        init {
            require(ids.size == orientations.size) { "ERROR" }
        }

        override val numOptions: Int = ids.size

        override fun containsTile(tileType: TileType, id: UByte): Boolean =
            tileType == this.tileType && id in ids

        override fun toSolved(): Solved = Solved(tileType, ids[0], orientations[0])

        fun toFixedTileId(): FixedTileId = FixedTileId(tileType, ids[0], orientations)

        fun toFixedOrientation(): FixedOrientation = FixedOrientation(tileType, ids, orientations[0])

        override fun possibleColours(tileData: TileData, compass: UByte): List<UByte> {
            val possibleColours = mutableListOf<UByte>()
            for (idx in 0 until ids.size) {
                val id = ids[idx]
                val orientation = orientations[idx]
                possibleColours.add(
                    tileData.toSide(tileType, id, orientation, compass),
                )
            }
            return possibleColours
        }

        /** The colour for a specific entry in the options and a specific compass direction. */
        fun colour(tileData: TileData, idx: Int, compass: UByte): UByte =
            tileData.toSide(tileType, ids[idx], orientations[idx], compass)

        override fun toString(): String = "Options (${ids.size}): ${ids[0]}, ${orientations[0]}"
    }
}
