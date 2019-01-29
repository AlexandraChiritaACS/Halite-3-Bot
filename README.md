# Halite 3 Bot

## Disclaimer
This code should not be taken as an example of good coding practices. It was written for a competition, not for cleanliness; as such, there are many pieces that potentially make no sense (and could use a healthy refactoring).

## Introduction
This is a bot written for the [Halite 3](https://halite.io/) competition; it ranked #62 out of 4014 total contestants. The [game rules](https://halite.io/learn-programming-challenge/game-overview) are a good place to start if you are unfamiliar with the competition.

## Algorithm
While I will not spell out every nuance of the algorithm, I will attempt to summarize all of its main functions. There are quite a few magic numbers sprinkled throughout the algorithm, and while some of them were tuned to their final value, many were simply pulled from thin air.

### Pre-game Setup
There is very little setup that is done before the first turn. The main things that are done in the pre-game allotment are:
1. Calculate the amount of halite initially available on the map.
1. Determine how many players are playing.
1. Calculate (based on the size of the map) what the maximum number of turns will be.
1. Set some parameters based on the number of players and the size of the map.

### Every Turn
The set of actions taken each turn are as follows:
1. Designate a ship to build a dropoff if certain conditions are met (described below).
1. Calculate moves for ships that are returning back to base.
1. For every non-returning ship, calculate a score for each cell on the map.
1. Calculate moves for ships that are en route to construct a dropoff.
1. Determine whether a ship should begin returning to a dropoff.
1. For every non-returning, non-constructing ship, assign a mining target based on the cell scores, and determine whether to move towards the target or stay still.
1. Decide whether to spawn a new ship.

### Dropoff Construction Conditions
The dropoff logic is, honestly, very messy. The brunt of the bot was already written before the dropoff logic was added, so it always felt a little shoehorned in.

The conditions required for a cell to be considered as a dropoff target are as follows:
- The current number of owned ships is at least `(current_dropoffs + 1) * 7`.
- There are no other dropoffs within x of the cell.
- The amount of halite within a 4-radius circle around the cell is at least 8,500.

Once the potential dropoff targets have been selected, they are sorted by the following: `halite_on_target + (halite_surrounding_target / (2r * (r + 1)))` where `r` is the circle's radius. This serves to sort the targets favoring those that have both a high amount of halite on them, as well as a high average amount of halite surrounding them.

After sorting the potential dropoff targets, a ship is designated to construct a dropoff if it is within x of the target cell.

### Scoring Cells
The algorithm used to score cells is actually fairly simple. Each ship assigns each cell a score, and will attempt to navigate to the cell with the highest score (assuming no other ship has already claimed it). The scoring metric for a given cell is as follows (with a higher value being better):
- A base value is calculated as the amount of halite on the cell plus the average amount of halite surrounding the cell (in a 4-radius circle).
- If there are only two players, the cell is occupied by an enemy ship, and the cell has more allies nearby than enemies, the occupying ship's halite is added to the base value.
    - This acts as a simple (read naive) way to encourage taking out wealthy, isolated ships.
- If the cell is currently inspired, the inspiration bonus is added to the base value.
- The distance from the cell to the nearest dropoff is calculated.
    - For this calculation the potential dropoff targets are counted as being real, though the distance is calculated as the average between the nearest _real_ dropoff and the _potential_ dropoff (just in case the potential dropoff never gets constructed).
- The final score is calculated as `base_value / (distance_from_ship + distance_to_dropoff)`.

### Ship Navigation
Coming soon...

### Returning Back to Base
A ship decides to return back to base if it is holding at least a threshold amount of halite (900 for 2p, 850 for 4p). Once a ship has decided it is returning back to base, it doesn't stop until it gets there.

### Deciding to Spawn
The problem of deciding when to spawn a ship (or more specifically, when to _stop_ spawning ships) is one that I feel was more difficult than anticipated. The bot will spawn a ship if:
- All friendly ships have been destroyed and there are more than 15 turns remaining.
    - This, in hindsight, was not exactly helpful, and was occasionally directly harmful.
- At least 20% of the game remains and:
    - The percentage of halite remaining on the map (`current_halite / initial_halite`) is at least x%,
    - The amount of stored halite is enough to actually spawn a ship.

The algorithm also takes into account whether a ship is currently en route to construct a dropoff; if that's the case, it only attempts to spawn a ship if the above criteria is met and the amount of halite the ship would require to construct the dropoff plus SHIP_COST is <= 0. The thought being that we don't want to delay constructing a dropoff by using halite to spawn ships.

## Things I Would Change
There are quite a few things I would do differently given the chance:
- Devote more time to tooling. For almost all of the competition I used the wonderful Fluorine replay viewer, but having a system that I could integrate with my bot would have been very helpful. I struggled a lot with watching a replay and determining why a game resulted in a loss, and what could have happened differently. I think having a system to visualize and evaluate different metrics might have been a boon.
- Spend less time futzing with manually tweaking magic numbers. While I attempted to use [CLOP](https://www.remi-coulom.fr/CLOP/) to tune the parameters, I never successfully found values that worked well. It's very likely that I attempted to tune too many parameters at once, or that the parameters that I was attempting to tune didn't generalize well across different map-size/player combinations.
- Spend less time attempting to implement pathfinding algorithms. One of the things I attempted early(ish) in the competition was to implement the [A* search algorithm](https://en.wikipedia.org/wiki/A*_search_algorithm) for use in ship navigation. While it worked (and carried me along for quite some time!) it was _dreadfully_ slow. This made any and all iterations to other parts of the algorithm slow down substantially. It was only after I worked out my own (rather ugly) navigation algorithm that I was able to get back to tweaking more essential pieces of the bot.
- Depend less on local testing. While testing locally definitely has its uses, I think there was a tendency to overly rely on local testing. This lead to a few situations of overfitting, and there were multiple changes that I ended up uploading that performed undeniably better in my local testing but didn't translate to better performance against anyone else.

## Thanks
I have to give thanks to Two Sigma for hosting yet another great competition, and to the Halite community at large for being as helpful, insightful, and welcoming as they are.

I would be remiss if I didn't give huge thanks to fohristiwhirl and mlomb for creating their amazing tools for the community: [Fohristiwhirl's Fluorine replay viewer](https://github.com/fohristiwhirl/fluorine) and [Mlomb's statistics site](https://halite2018.mlomb.me/).
