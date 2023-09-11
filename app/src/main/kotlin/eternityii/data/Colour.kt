package eternityii.data

object Colour {
    const val GREY: UByte = 0U
    const val GREY_INDEX = 30 // Must not match any other colour.

    val EDGE_COLOURS = listOf<UByte>(
        1U, // Orange background, cyan inner.
        9U, // Dark Blue background, yellow circular flower.
        17U, // Pink background, cyan mid, pink inner.
        5U, // Green background, thin dark blue circle.
        13U, // Maroon background, orange cross.
    )

    val EDGE_COLOUR_TO_INDEX = mapOf<UByte, UByte>(
        EDGE_COLOURS[0] to 0U,
        EDGE_COLOURS[1] to 1U,
        EDGE_COLOURS[2] to 2U,
        EDGE_COLOURS[3] to 3U,
        EDGE_COLOURS[4] to 4U,
    )

    val NUM_EDGE_COLOURS = EDGE_COLOURS.size

    val MID_COLOURS = listOf<UByte>(
        2U, // Pink background, thin yellow cross.
        10U, // Purple background, fat cyan cross.
        18U, // Yellow background, light blue star.
        6U, // Purple background, purple plus in yellow circle.
        14U, // Green background, thin pink cross.
        3U, // Maroon background, maroon plus in green circle.
        11U, // Green background, fat orange cross.
        19U, // Maroon background, yellow star.
        7U, // Cyan background, pink castle.
        15U, // Yellow background, thin green, hollow square.
        4U, // Cyan background, fat pink cross.
        12U, // Yellow background, dark blue castle.
        20U, // Orange background, purple star.
        8U, // Dark blue background, thin orange cross.
        16U, // Dark blue background, light blue square.
        21U, // Pink background, yellow castle.
        22U, // Dark blue background, dark blue cross in pink circle.
    )

    /** For wildcard matching when looking up by colour. */
    val MID_COLOUR_ANY: UByte = MID_COLOURS.size.toUByte()

    val MID_COLOUR_TO_INDEX = mapOf<UByte, UByte>(
        MID_COLOURS[0] to 0U,
        MID_COLOURS[1] to 1U,
        MID_COLOURS[2] to 2U,
        MID_COLOURS[3] to 3U,
        MID_COLOURS[4] to 4U,
        MID_COLOURS[5] to 5U,
        MID_COLOURS[6] to 6U,
        MID_COLOURS[7] to 7U,
        MID_COLOURS[8] to 8U,
        MID_COLOURS[9] to 9U,
        MID_COLOURS[10] to 10U,
        MID_COLOURS[11] to 11U,
        MID_COLOURS[12] to 12U,
        MID_COLOURS[13] to 13U,
        MID_COLOURS[14] to 14U,
        MID_COLOURS[15] to 15U,
        MID_COLOURS[16] to 16U,
    )

    val NUM_MID_COLOURS = MID_COLOURS.size

    val ANY_COLOUR_TO_INDEX = EDGE_COLOUR_TO_INDEX + MID_COLOUR_TO_INDEX + mapOf(GREY to 0U)

    val toBucasLetter = listOf(
        'a', //  0 Grey edge. EDGE ONLY.
        'b', //  1 Orange background, cyan inner. EDGE ONLY.
        'c', //  9 Dark Blue background, yellow circular flower. EDGE ONLY.
        'd', // 17 Pink background, cyan mid, pink inner. EDGE ONLY.
        'e', //  5 Green background, thin dark blue circle. EDGE ONLY.
        'f', // 13 Maroon background, orange cross. EDGE ONLY.
        'g', //  2 Pink background, thin yellow cross. MID ONLY.
        'h', // 10 Purple background, fat cyan cross. MID ONLY.
        'i', // 18 Yellow background, light blue star. MID ONLY.
        'j', //  6 Purple background, purple plus in yellow circle. MID ONLY.
        'k', // 14 Green background, thin pink cross. MID ONLY.
        'l', //  3 Maroon background, maroon plus in green circle. MID ONLY.
        'm', // 11 Green background, fat orange cross. MID ONLY.
        'n', // 19 Maroon background, yellow star. MID ONLY.
        'o', //  7 Cyan background, pink castle. MID ONLY.
        'p', // 15 Yellow background, thin green, hollow square. MID ONLY.
        'q', //  4 Cyan background, fat pink cross. MID ONLY.
        'r', // 12 Yellow background, dark blue castle. MID ONLY.
        's', // 20 Orange background, purple star. MID ONLY.
        't', //  8 Dark blue background, thin orange cross. MID ONLY.
        'u', // 16 Dark blue background, light blue square. MID ONLY.
        'v', // 21 Pink background, yellow castle. MID ONLY.
        'w', //  22 Dark blue background, dark blue cross in pink circle. MID ONLY.
    )
}
