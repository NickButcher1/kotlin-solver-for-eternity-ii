# 1xN

## Ways to join 1 corner + N edge tiles, clockwise

- 1 tile (corner only) - 4
- 2 tiles - 45
- 3 tiles - 495
- 4 tiles - 5,336
- 5 tiles - 56,507
- 6 tiles - 587,808
- 7 tiles - 6,003,355
- 8 tiles - 60,184,906
- 9 tiles - 592,043,767
- 10 tiles - 5,712,070,312
- 11 tiles - 54,029,813,432
- 12 tiles - 500,822,997,668

## Ways to join 1 corner + N edge tiles, anticlockwise

- 1 tile (corner only) - 4
- 2 tiles - 45
- 3 tiles - 495
- 4 tiles - 5,300
- 5 tiles - 56,197
- 6 tiles - 585,136
- 7 tiles - 5,981,169
- 8 tiles - 60,010,343

## Ways to join N edge tiles into a 1xN block

- 1 tile - 56
- 2 tiles - 613
- 3 tiles - 6,600
- 4 tiles - 69,827
- 5 tiles - 725,643
- 6 tiles - 7,404,390
- 7 tiles - 74,158,615

## Ways to join N mid tiles into a 1xN block (includes duplicate orientations)

- 1 tile - 784
- 2 tiles - 35,280
- 3 tiles - 1,579,688
- 4 tiles - 70,373,500
- 5 tiles - 3,118,987,718
- 6 tiles - 137,521,419,190

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

# Backtracker Results

These are the best results obtained with the backtracker.

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
- The distribution of the 17 mid colours on the 56 edge tiles is very uneven. Two colours occur once only, two colour occur six times, and the rest are in the range 2-5.
- There are in theory 289 (17^2) ways that a tile can have two consecutive edge colours. But only 269 are actually used.
