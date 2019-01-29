package halite.hlt;

import static halite.hlt.Direction.STILL;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GameMap {
	public final int width;
	public final int height;
	public final MapCell[][] cells;
	public List<MapCell> cellsList = new ArrayList<>();
	public double initialHalite = 0;
	public double currentFreeHalite = 0;
	private final Player me;
	public boolean debug = false;

	public GameMap(final int width, final int height, final Player me) {
		this.width = width;
		this.height = height;
		this.me = me;

		cells = new MapCell[height][];
		for (int y = 0; y < height; ++y) {
			cells[y] = new MapCell[width];
		}
	}

	/**
	 * Unrolls the game map into a flat list of MapCells.
	 * 
	 * @return - The unrolled map list.
	 */
	public List<MapCell> asList() {
		final List<MapCell> unrolledMap = new ArrayList<>();
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				unrolledMap.add(cells[y][x]);
			}
		}
		return unrolledMap;
	}

	public MapCell at(final Position position) {
		final Position normalized = normalize(position);
		return cells[normalized.y][normalized.x];
	}

	public MapCell at(final Entity entity) {
		return at(entity.position);
	}

	public MapCell at(final int x, final int y) {
		final Position pos = new Position(x, y);
		return at(pos);
	}

	/**
	 * Gets the cells that are cardinally adjacent to the given cell.
	 * 
	 * @param cell
	 * @return - The four adjacent neighbouring cells.
	 */
	public List<MapCell> getNeighbors(final MapCell cell) {
		return cell.position.getSurroundingCardinals().stream().map(pos -> at(pos)).collect(toList());
	}

	/**
	 * Gets a list of all the cells that are within a given radius of a given
	 * Position.
	 * 
	 * @param origin
	 * @param radius
	 */
	public List<MapCell> getCircle(final Position origin, final int radius) {
		final Set<MapCell> cells = new HashSet<>();
		for (int i = 0; i <= radius; i++) {
			for (int j = radius; j >= 0; j--) {
				MapCell cell_leftUp = at(origin.x - i, origin.y - j);
				MapCell cell_leftDown = at(origin.x - i, origin.y + j);
				MapCell cell_rightUp = at(origin.x + i, origin.y - j);
				MapCell cell_rightDown = at(origin.x + i, origin.y + j);
				if (calculateDistance(origin, cell_leftUp.position) <= radius) {
					cells.add(cell_leftUp);
				}
				if (calculateDistance(origin, cell_leftDown.position) <= radius) {
					cells.add(cell_leftDown);
				}
				if (calculateDistance(origin, cell_rightUp.position) <= radius) {
					cells.add(cell_rightUp);
				}
				if (calculateDistance(origin, cell_rightDown.position) <= radius) {
					cells.add(cell_rightDown);
				}
			}
		}
		return new ArrayList<>(cells);
	}

	/**
	 * Compute the Manhattan distance between two locations. Accounts for
	 * wrap-around.
	 * 
	 * @param source - The source from where to calculate
	 * @param target - The target to calculate to
	 * @return - The distance between the two posititions
	 */
	public int calculateDistance(final Position source, final Position target) {
		final Position normalizedSource = normalize(source);
		final Position normalizedTarget = normalize(target);

		final int dx = Math.abs(normalizedSource.x - normalizedTarget.x);
		final int dy = Math.abs(normalizedSource.y - normalizedTarget.y);

		final int toroidal_dx = Math.min(dx, width - dx);
		final int toroidal_dy = Math.min(dy, height - dy);

		return toroidal_dx + toroidal_dy;
	}

	/**
	 * Normalize the position within the bounds of the toroidal map. i.e.: Takes a
	 * point which may or may not be within width and height bounds, and places it
	 * within those bounds, considering wrap-around.
	 * 
	 * @param position - The position to normalize
	 * @return - A normalized position fitting within the bounds of the map
	 */
	public Position normalize(final Position position) {
		final int x = ((position.x % width) + width) % width;
		final int y = ((position.y % height) + height) % height;
		return new Position(x, y);
	}

	/**
	 * Return the Direction(s) to move closer to the target point, or empty if the
	 * points are the same. This move mechanic does not account for collisions. The
	 * multiple directions are if both directional movements are viable.
	 * 
	 * @param source      - The starting position
	 * @param destination - The destination towards which you wish to move
	 * @return - A list of valid (closest) Directions towards the destination
	 */
	public ArrayList<Direction> getUnsafeMoves(final Position source, final Position destination) {
		final ArrayList<Direction> possibleMoves = new ArrayList<>();

		final Position normalizedSource = normalize(source);
		final Position normalizedDestination = normalize(destination);

		final int dx = Math.abs(normalizedSource.x - normalizedDestination.x);
		final int dy = Math.abs(normalizedSource.y - normalizedDestination.y);
		final int wrapped_dx = width - dx;
		final int wrapped_dy = height - dy;

		if (normalizedSource.x < normalizedDestination.x) {
			possibleMoves.add(dx > wrapped_dx ? Direction.WEST : Direction.EAST);
		} else if (normalizedSource.x > normalizedDestination.x) {
			possibleMoves.add(dx < wrapped_dx ? Direction.WEST : Direction.EAST);
		}

		if (normalizedSource.y < normalizedDestination.y) {
			possibleMoves.add(dy > wrapped_dy ? Direction.NORTH : Direction.SOUTH);
		} else if (normalizedSource.y > normalizedDestination.y) {
			possibleMoves.add(dy < wrapped_dy ? Direction.NORTH : Direction.SOUTH);
		}

		return possibleMoves;
	}

	/**
	 * Returns the Direction to move closer to the (normalized) adjacent MapCell.
	 * 
	 * @param source   - The starting cell
	 * @param neighbor - The adjacent cell
	 * @return - The Direction towards the neighbor
	 */
	public Direction getDirectionToNeighbor(final MapCell source, final MapCell neighbor) {
		return getDirectionToNeighbor(source.position, neighbor.position);
	}

	/**
	 * Returns the Direction to move closer to the (normalized) adjacent MapCell.
	 * 
	 * @param source   - The starting cell
	 * @param neighbor - The adjacent cell
	 * @return - The Direction towards the neighbor
	 */
	public Direction getDirectionToNeighbor(final Position source, final Position neighbor) {
		final int dx = Math.abs(source.x - neighbor.x);
		final int dy = Math.abs(source.y - neighbor.y);
		final int wrapped_dx = width - dx;
		final int wrapped_dy = height - dy;

		Direction ret = STILL;
		if (source.x < neighbor.x) {
			ret = dx > wrapped_dx ? Direction.WEST : Direction.EAST;
		} else if (source.x > neighbor.x) {
			ret = dx < wrapped_dx ? Direction.WEST : Direction.EAST;
		} else if (source.y < neighbor.y) {
			ret = dy > wrapped_dy ? Direction.NORTH : Direction.SOUTH;
		} else if (source.y > neighbor.y) {
			ret = dy < wrapped_dy ? Direction.NORTH : Direction.SOUTH;
		}
		return ret;
	}

	/**
	 * Determines whether or not a given ship has enough halite to move.
	 * 
	 * @param ship - The ship in question
	 * @return - True if the ship has enough halite to move from its current cell.
	 */
	public Boolean canShipMove(final Ship ship) {
		return ship.halite >= Math.floor(at(ship).halite * 0.10);
	}

	/**
	 * Determines whether the given cell is surrounded by other ships.
	 * 
	 * @param cell
	 * @return - True if all of the cell's neighbors have a ship on them.
	 */
	public Boolean cellIsSurrounded(final MapCell cell) {
		return cell.position.getSurroundingCardinals().stream().allMatch(pos -> at(pos).isOccupied());
	}

	/**
	 * Calculates which direction will best navigate from the starting position to
	 * the goal position. This method does take into account collision avoidance and
	 * ship swapping.
	 * 
	 * @param start       - The beginning position.
	 * @param goal        - The target position.
	 * @param allowCombat - Whether or not "combat" is allowed.
	 * @return A {@link Direction} that will move a ship closer to its target
	 */
	public Direction getDirection(final Position start, final Position goal, final boolean allowCombat) {
		return getDirection(start, goal, "", allowCombat);
	}

	/**
	 * Calculates which direction will best navigate from the starting position to
	 * the goal position. This method does take into account collision avoidance and
	 * ship swapping.
	 * 
	 * @param start       - The beginning position.
	 * @param goal        - The target position.
	 * @param shipStatus  - A string representing the ship's current status (e.g.
	 *                    "exploring", "returning", etc.)
	 * @param allowCombat - Whether or not "combat" is allowed.
	 * @return A {@link Direction} that will move a ship closer to its target
	 */
	public Direction getDirection(final Position start, final Position goal, final String shipStatus,
			final boolean allowCombat) {
		final MapCell startCell = at(start);
		final int moveCost = (int) Math.floor(startCell.halite * 0.10);
		final int dist = calculateDistance(start, goal);
		final boolean returning = "returning".equals(shipStatus);
		final boolean finalRush = "finalRush".equals(shipStatus);
		List<MapCell> neighbors = getNeighbors(startCell);
		MapCell target;

		/**
		 * This predicate is used to determine whether the neighbor is an acceptable
		 * target to travel to.
		 */
		Predicate<MapCell> neighborIsAcceptable;
		Predicate<MapCell> cellIsFriendlyDropoff = c -> c.hasStructure() && c.structure.belongsTo(me);
		Predicate<MapCell> neighborIsOpen = n -> !n.isOccupied();
		Predicate<MapCell> neighborHasFriendlyShip = n -> n.isOccupied() && n.ship.belongsTo(me);
		Predicate<MapCell> dropoffIsOccupied = n -> cellIsFriendlyDropoff.test(n) && n.isOccupied()
				&& !n.ship.belongsTo(me);
		Predicate<MapCell> enemyShipIsRicher = n -> {
			if (returning || finalRush) {
				return allowCombat && n.isOccupied() && !n.ship.belongsTo(me);
			}
			return allowCombat && n.isOccupied() && !n.ship.belongsTo(me)
					&& n.ship.halite + n.halite > (startCell.ship.halite + startCell.halite) * 1.75;
		};
		Predicate<MapCell> enemyPredictedHere = n -> {
			// If the neighbor is a friendly dropoff (or if combat is allowed), don't worry
			// about a potential collision.
			if (allowCombat || cellIsFriendlyDropoff.test(n)) {
				return false;
			}
			final List<MapCell> adjacent = getNeighbors(n);
			boolean enemyPredicted = false;
			for (final MapCell c : adjacent) {
				if (c.isOccupied() && !c.ship.belongsTo(me)) {
					final Ship s = c.ship;
					final double enemyShipVal = s.halite - Math.floor(c.halite * 0.1);
					final double myShipVal = startCell.ship.halite - moveCost;
					// If n.hasStructure(), it's guaranteed to be an enemy's
					// If my ship is worth __% more than the enemy's, don't risk it
					if (n.hasStructure() || myShipVal * 1.25 > enemyShipVal) {
						enemyPredicted = true;
						break;
					}
				}
			}
			return enemyPredicted;
		};

		neighborIsAcceptable = neighborIsOpen.or(dropoffIsOccupied).or(enemyShipIsRicher);
		if (returning || finalRush || (cellIsFriendlyDropoff.test(startCell) && cellIsSurrounded(startCell))
				|| startCell.halite >= 10 || (startCell.halite == 0 && !cellIsFriendlyDropoff.test(startCell))) {
			// Allow swapping
			neighborIsAcceptable = neighborIsAcceptable.or(neighborHasFriendlyShip);
		}
		neighborIsAcceptable = enemyPredictedHere.negate().and(neighborIsAcceptable);

		// If there aren't any acceptable neighbors (as defined by the predicate above).
		if (neighbors.stream().noneMatch(neighborIsAcceptable)) {
			return STILL;
		}

		List<MapCell> closerNeighbors = new ArrayList<>();
		List<MapCell> acceptableNeighbors = new ArrayList<>();
		Comparator<MapCell> comp = comparing(n -> n.halite / (1 + calculateDistance(n.position, goal)));
		for (final MapCell n : neighbors) {
			if (!neighborIsAcceptable.test(n)) {
				continue;
			}

			final int distanceToGoal = calculateDistance(n.position, goal);
			if (distanceToGoal < dist) {
				closerNeighbors.add(n);
			} else if (returning || finalRush || n.halite >= (startCell.halite * 1.5)) {
				acceptableNeighbors.add(n);
			}
		}
		if (!closerNeighbors.isEmpty()) {
			target = (returning || finalRush) ? closerNeighbors.stream().min(comp).get()
					: closerNeighbors.stream().max(comp).get();
		} else {
			target = (returning || finalRush) ? acceptableNeighbors.stream().min(comp).orElse(startCell)
					: acceptableNeighbors.stream().max(comp).orElse(startCell);
		}

		return getDirectionToNeighbor(startCell, target);
	}

	/**
	 * Resolves attempted ship movement. Will attempt to move ships to their desired
	 * destination unless there is no viable path forward.
	 * 
	 * NOTE: A warning will be logged and this method will return if the resolution
	 * takes too long.
	 * 
	 * @param shipPaths  - The map of ships to their desired destination.
	 * @param shipStatus - The map of ships to their current status (e.g.
	 *                   "returning", "exploring", etc.)
	 * @param startTime  - The timestamp (see {@link System#nanoTime()}) calculated
	 *                   at the beginning of the current turn.
	 * @return A list of {@link Command}s to be passed to the game engine to resolve
	 *         movement.
	 */
	public List<Command> navigate(Map<Ship, Direction> shipPaths, final Map<EntityId, String> shipStatus,
			final long startTime) {
		List<Command> ret = new ArrayList<>();
		Comparator<Entry<Ship, Direction>> comp = comparing(e -> e.getKey().halite);
		boolean movementMade = true;
		while (movementMade) {
			movementMade = false;
			// If we're getting close to running out of time for the turn, break out.
			if (!this.debug && (System.nanoTime() - startTime) / 1_000_000.0 >= 1_900) {
				Log.log("*** WARNING: Breaking from Navigate! ***");
				break;
			}

			final List<Entry<Ship, Direction>> ships_sorted_by_halite = shipPaths.entrySet().stream()
					.sorted(comp.reversed()).collect(Collectors.toList());
			for (Entry<Ship, Direction> e : ships_sorted_by_halite) {
				final Ship ship = e.getKey();
				if (ship.hasMoved()) {
					continue;
				} else if (!canShipMove(ship)) {
					ret.add(ship.stayStill());
					movementMade = true;
					continue;
				}

				final Direction dir = e.getValue();
				final Position target = ship.position.directionalOffset(dir);
				final MapCell targetCell = at(target);
				final boolean targetIsDropoff = targetCell.hasStructure() && targetCell.structure.belongsTo(me);

				// If the ship wants to stay still
				if (dir.equals(STILL)) {
					ret.add(ship.stayStill());
					movementMade = true;
					continue;
				}

				// If the ship is rushing back home.
				if (targetIsDropoff && shipStatus.get(ship.id).equals("finalRush")) {
					at(ship).markSafe();
					ret.add(ship.move(dir));
					movementMade = true;
					continue;
				}

				// If the target has a friendly ship on it
				if (targetCell.isOccupied() && targetCell.ship.belongsTo(me)) {
					final Ship otherShip = targetCell.ship;
					if (!shipPaths.containsKey(otherShip)) {
						continue;
					}
					// If the other ship can still move
					if (!otherShip.hasMoved() && canShipMove(otherShip)) {
						Direction otherDir = shipPaths.get(otherShip);
						// If the other ship wants to move into my space, swap places
						if (otherDir != null && !otherDir.equals(STILL) && otherDir.equals(dir.invertDirection())) {
							targetCell.markUnsafe(ship);
							at(ship).markUnsafe(otherShip);
							ret.add(ship.move(dir));
							ret.add(otherShip.move(otherDir));
							movementMade = true;
							// If the other ship is staying still
						} else if (otherDir != null && otherDir.equals(STILL)) {
							ret.add(ship.stayStill());
							movementMade = true;
						}
						// Otherwise, wait for it to move?
					} else {
						ret.add(ship.stayStill());
						movementMade = true;
					}
				} else {
					targetCell.markUnsafe(ship);
					at(ship).markSafe();
					ret.add(ship.move(dir));
					movementMade = true;
				}
			}
		}

		return ret;
	}

	void _update() {
		this.currentFreeHalite = 0;
		final int updateCount = Input.readInput().getInt();

		for (int i = 0; i < updateCount; ++i) {
			final Input input = Input.readInput();
			final int x = input.getInt();
			final int y = input.getInt();

			final int halite = input.getInt();
			cells[y][x].halite = halite;
		}

		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				cells[y][x].ship = null;
				this.currentFreeHalite += cells[y][x].halite;
			}
		}
	}

	static GameMap _generate(final Player me, final boolean debug) {
		final Input mapInput = Input.readInput();
		final int width = mapInput.getInt();
		final int height = mapInput.getInt();

		final GameMap map = new GameMap(width, height, me);
		map.debug = debug;

		for (int y = 0; y < height; ++y) {
			final Input rowInput = Input.readInput();

			for (int x = 0; x < width; ++x) {
				final int halite = rowInput.getInt();
				map.initialHalite += halite;
				final MapCell cell = new MapCell(new Position(x, y), halite);
				map.cells[y][x] = cell;
			}
		}
		map.currentFreeHalite = map.initialHalite;
		map.cellsList = map.asList();

		return map;
	}
}
