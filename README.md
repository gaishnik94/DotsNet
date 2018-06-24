# DotsNet
This is network game. First player create game server, then second player connect to game server. 

***
## 5 Simple Rules

1. Players - blue and red - walk by turns.

2. The point is placed at the intersection of the horizontal and vertical lines.

3. One move puts only one point and it does not move any more.

4. It is necessary to surround the opponent's points, not letting you surround your own.

5. The player who captures more points of the opponent wins.

## Official rules

The game goes to a field of 39x32 pixels. The point is the intersection of lines on the field. They play two, different colors. Players walk in turn (1 move - one point).

All moves can be at any point, unless it is in the surrounded area. Possibilities to pass (skip a course) are not present.

When creating a continuous (vertically, horizontally, diagonally) closed line, a region is formed. If inside it there are enemy points (there may be points not occupied by anyone's points), then this is considered an area of ​​encirclement, into which it is further forbidden to put a point to any of the players. If there are no points, then the area is free and points can be put in it. When a free zone appears in the free area of ​​an opponent, the free area will be considered an area of ​​the environment, provided that the opponent's point was not the final point in his environment. Points that fall into the surrounding area, then do not participate in the formation of lines for the environment. Points placed on the edge of the field are not surrounded.


The game ends when one of the players presses STOP. If player A stops the game, player B will have 3 minutes, during which player B will put points one, and then free-up the free points of player A. After three minutes the game ends automatically.

Victory is determined by counting rounded points (the player who encircles more points of the opponent wins).

## Comments and Remarks

Let there be a continuous closed line that bounds a certain region. But in this area there are no enemy points. Then the enemy made a move into this area, this area will be considered surrounding, BUT only at the moment of the player's move to which the area belongs. In this case, the move can be at any other place on the field (it is not necessary to be part of that surrounding area). Let there be a surrounding area, which (at time X) is surrounded. In this case, the number of enemy points that the area contained before the time X is not taken into account when counting the surrounded points at the end of the game.