package eternityii.backtracker

/**
 * A simple backtracker that builds up to a 16x16 square, using an expanding square order.
 */
object ScanrowSquaresBacktrackerPath : BacktrackerPath(
    listOf(
        0, 1, 4, 9, 16, 25, 36, 49, 64, 81, 100, 121, 144, 169, 196, 225,
        2, 3, 5, 10, 17, 26, 37, 50, 65, 82, 101, 122, 145, 170, 197, 226,
        6, 7, 8, 11, 18, 27, 38, 51, 66, 83, 102, 123, 146, 171, 198, 227,
        12, 13, 14, 15, 19, 28, 39, 52, 67, 84, 103, 124, 147, 172, 199, 228,
        20, 21, 22, 23, 24, 29, 40, 53, 68, 85, 104, 125, 148, 173, 200, 229,
        30, 31, 32, 33, 34, 35, 41, 54, 69, 86, 105, 126, 149, 174, 201, 230,
        42, 43, 44, 45, 46, 47, 48, 55, 70, 87, 106, 127, 150, 175, 202, 231,
        56, 57, 58, 59, 60, 61, 62, 63, 71, 88, 107, 128, 151, 176, 203, 232,
        72, 73, 74, 75, 76, 77, 78, 79, 80, 89, 108, 129, 152, 177, 204, 233,
        90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 109, 130, 153, 178, 205, 234,
        110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 131, 154, 179, 206, 235,
        132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 155, 180, 207, 236,
        156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 181, 208, 237,
        182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 209, 238,
        210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 239,
        240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255,
    ),
)
