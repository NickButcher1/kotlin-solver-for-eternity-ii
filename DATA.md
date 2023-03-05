# 1xN

## Ways to join 1 corner + N edge tiles, clockwise

| Tiles | Ways                 | Multiple | Time(s) |
| ----- | -------------------- | -------- | ------- |
|     1 |                    4 |          |         |
|     2 |                   45 |    11.25 |         |
|     3 |                  495 |    11.00 |         |
|     4 |                5,336 |    10.78 |         |
|     5 |               56,507 |    10.59 |         |
|     6 |              587,808 |    10.40 |         |
|     7 |            6,003,355 |    10.21 |         |
|     8 |           60,184,906 |    10.03 |         |
|     9 |          592,043,767 |     9.84 |       2 |
|    10 |        5,712,070,312 |     9.65 |      30 |
|    11 |       54,029,813,432 |     9.46 |         |
|    12 |      500,822,997,668 |     9.27 |         |

## Ways to join 1 corner + N edge tiles, anticlockwise

| Tiles | Ways                 | Multiple | Time(s) |
| ----- | -------------------- | -------- | ------- |
|     1 |                    4 |          |         |
|     2 |                   45 |          |         |
|     3 |                  495 |          |         |
|     4 |                5,300 |          |         |
|     5 |               56,197 |          |         |
|     6 |              585,136 |          |         |
|     7 |            5,981,169 |          |         |
|     8 |           60,010,343 |          |         |

## Ways to join N edge tiles into a 1xN block

| Tiles | Ways                 | Multiple | Time(s) |
| ----- | -------------------- | -------- | ------- |
|     1 |                   56 |          |         |
|     2 |                  613 |          |         |
|     3 |                6,600 |          |         |
|     4 |               69,827 |          |         |
|     5 |              725,643 |          |         |
|     6 |            7,404,390 |          |         |
|     7 |           74,158,615 |          |         |

## Ways to join N mid tiles into a 1xN block (includes duplicate orientations)

| Tiles | Ways                 | Multiple | Time(s) |
| ----- | -------------------- | -------- | ------- |
|     1 |                  784 |          |         |
|     2 |               35,280 |    45.00 |         |
|     3 |            1,579,688 |    44.78 |         |
|     4 |           70,373,500 |    44.55 |         |
|     5 |        3,118,987,718 |    44.32 |         |
|     6 |      137,521,419,190 |    44.09 |         |

# Ways to make a square

## 2x2

- 1,312 top left corner
- 73,003 top edge
- 1,014,988 mid tiles
- 4,059,952 mid tiles, including all four orientations of each block.

### 3x3

- 2,633,221 top left corner

### 4x4

- 29,775,113,571 top left corner

### 5x5

- Not attempted. Seen 1,445,073,698,679 on the forum which I need to verify. Also seen 1,596,901,885,652 using all five clue tiles.

# Rust Backtracker Results

These are the best results obtained with the Rust backtracker.

- 16x16 all tiles, scanrow, default order:
  - 215 tiles (13x16+7) in 8 minutes (40.5B placed).
  - Longest run: 150B placed in 2,205s, found 210x256, 211x70, 212x25, 213x5, 214x2, 215x1.
  - Longest run: 261B placed in 5,166s, found 210x521, 211x188, 212x87, 213x53, 214x13, 215x3.

- 16x16 all tiles, scanrow12, default order:
  - 212 tiles in 330s.
  - Longest run: 21B in 330s.

- 16x16 all tiles, scanrow11, default order:
  - 213 tiles in 1,634s.
  - Longest run: 204B in 5,120s.

- 16x16 all tiles, diagonal fill, default order:
  - 200 tiles in 360s.
  - Longest run: 300B placed in 3,049s.

- 16x16 all tiles, expanding square fill, default order:
  - 205 tiles in 1,100.
  - Longest run: 141B placed in 1,593s.

- 14x14 mids only, scanrow
  - Default order:
    - 181 tiles in 4,650s.
    - Longest run: 247B placed in 4,650s.
  - Random order:
    - 185 tiles (13x14+3) in 20,460s. Found twice.
    - Longest run: 1.8T placed in 52,980s (and 7 threads, averaging 1.7T placed per thread).

# Kotlin Backtracker Results

These are the best results obtained with the Kotlin backtracker.

- 16x16 all tiles
  - Scanrow, default order: 206 tiles (12x16+14) in 1 minute.
  - Scanrow, random order: 211 tiles (13x16+3) in 4 minutes.
  - Expanding square, default order: 203 tiles (14x14+7) in 2 minutes.
  - Expanding square, random order: 204 tiles (14x14+8) in 2 minutes.
  - Diagonal fill, default order: 121 tiles in 8 minutes (longest run 38 minutes).
  - Diagonal fill, random order: 123 tiles in 1 minute.
- 14x14 mid tiles only
  - Scanrow, default order: 179 tiles (13x13+10) in 1.5 hours.
  - Scanrow, random order: 180 tiles (13x13+11) in 1 hour.
- Complete edge
  - Expanding alternately in both directions from one corner: 60 tiles in 1 second.

# Colour Distribution

- The distribution of (anticlockwise colour + clockwise colour) on the edge tiles is very uneven. Of the 25 possible pairs, only one pair does not exist. The others range in frequency from one to four tiles.
- The distribution of the 17 mid colours on the 56 edge tiles is very uneven. Two colours occur once only, two colours occur six times, and the rest are in the range 2-5.
- There are in theory 289 (17^2) ways that a tile can have two consecutive edge colours. But only 269 are actually used.
