package eternityii.builder

import eternityii.data.TileData
import eternityii.display.Display

/**
 * Solver using the clues. For now, all it does is print the URL to verify the clue data.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class ClueSolver(
    private val tileData: TileData
) {
    fun solve() {
        println("ClueSolver")
        Display.buildUrlForClues(tileData)
    }
}
