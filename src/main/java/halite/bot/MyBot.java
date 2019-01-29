package halite.bot;

import static halite.hlt.Constants.MAX_HALITE;
import static halite.hlt.Constants.SHIP_COST;
import static halite.hlt.Direction.STILL;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import halite.hlt.Command;
import halite.hlt.Constants;
import halite.hlt.Direction;
import halite.hlt.Entity;
import halite.hlt.EntityId;
import halite.hlt.F_Log;
import halite.hlt.Game;
import halite.hlt.GameMap;
import halite.hlt.Log;
import halite.hlt.MapCell;
import halite.hlt.Player;
import halite.hlt.Position;
import halite.hlt.Ship;

public class MyBot {
	public static void main(final String[] args) {
		// IGNORE_PERCENT is the percentage of a cell's maximum halite that will be left
		// on the ground.
		double IGNORE_PERCENT = 0.055;
		boolean debug = false;
		boolean local = false;
		for (final String arg : args) {
			if (arg.equals("--debug")) {
				debug = true;
			} else if (arg.equals("--local")) {
				local = true;
			}
		}

		Game game = new Game(debug, local);
		boolean is2p = game.players.size() == 2;

		// Calculate the maximum number of turns that can be played for the map size.
		int maxTurns = 300 + (25 * game.gameMap.width / 8);

		// Spawning / construction magic numbers
		final int minDropoffTurn;
		final double minDropoffDistance;
		// When the percentage of remaining halite on the map is lower than this number,
		// ships will stop spawning
		double remainingHaliteToSpawnUntil;
		final int dropoffTravelDist;
		if (!is2p) {
			remainingHaliteToSpawnUntil = 0.51;
			switch (game.gameMap.width) {
			case 40:
				dropoffTravelDist = 5;
				remainingHaliteToSpawnUntil = 0.35;
				minDropoffTurn = 135;
				minDropoffDistance = (game.gameMap.width / 4) * 1.25;
				break;
			case 48:
				dropoffTravelDist = 6;
				remainingHaliteToSpawnUntil = 0.4;
				minDropoffTurn = 120;
				minDropoffDistance = (game.gameMap.width / 4) * 1.25;
				break;
			case 56:
				dropoffTravelDist = 6;
				remainingHaliteToSpawnUntil = 0.4;
				minDropoffTurn = 120;
				minDropoffDistance = 14;
				break;
			case 64:
				dropoffTravelDist = 6;
				remainingHaliteToSpawnUntil = 0.40;
				minDropoffTurn = 150;
				minDropoffDistance = 14;
				break;
			case 32:
			default:
				dropoffTravelDist = 4;
				remainingHaliteToSpawnUntil = 0.40;
				minDropoffTurn = 120;
				minDropoffDistance = 10;
				break;
			}
		} else {
			remainingHaliteToSpawnUntil = 0.41;
			switch (game.gameMap.width) {
			case 40:
				minDropoffTurn = 135;
				minDropoffDistance = 15;
				dropoffTravelDist = 6;
				break;
			case 48:
				minDropoffTurn = 120;
				minDropoffDistance = 14;
				remainingHaliteToSpawnUntil = 0.5;
				dropoffTravelDist = 8;
				break;
			case 56:
				minDropoffTurn = 120;
				minDropoffDistance = 15;
				remainingHaliteToSpawnUntil = 0.55;
				dropoffTravelDist = 10;
				break;
			case 64:
				minDropoffTurn = 120;
				minDropoffDistance = 15;
				remainingHaliteToSpawnUntil = 0.5;
				dropoffTravelDist = 12;
				break;
			case 32:
			default:
				dropoffTravelDist = 4;
				minDropoffTurn = 150;
				minDropoffDistance = 16;
				break;
			}
		}

		Double avgTime = 0.0;
		int turnsSpentWaiting = 0;
		final Map<EntityId, String> shipStatus = new HashMap<>();
		final Map<EntityId, MapCell> shipsDroppingOff = new HashMap<>();
		final List<Entity> allStructures = new ArrayList<>();
		final int CIRCLE_RAD = Constants.INSPIRATION_RADIUS;

		final String BOT_NAME = "MyBot_40-1";
		game.ready(BOT_NAME);
		Log.log("Successfully created bot %s! My Player ID is %d.", BOT_NAME, game.myId.id);
		F_Log.log("[");

		Comparator<MapCell> comp = comparing(
				c -> c.halite + (c.getSurroundingHalite() - c.halite) / (2 * CIRCLE_RAD * (CIRCLE_RAD + 1)));

		for (;;) {
			game.updateFrame();
			final long startTime = System.nanoTime();
			final Player me = game.me;
			final GameMap gameMap = game.gameMap;
			final ArrayList<Command> commandQueue = new ArrayList<>();
			final List<MapCell> unrolledMap = gameMap.cellsList;
			final Map<Ship, Direction> shipPaths = new HashMap<>();
			final List<MapCell> targetCells = new ArrayList<>();
			final int remainingTurns = maxTurns - game.turnNumber;
			int haliteRequiredToConstruct = 4_000;

			// Remove ships that no longer exist.
			shipStatus.entrySet().removeIf(e -> !me.ships.containsKey(e.getKey()));
			shipsDroppingOff.entrySet().removeIf(e -> !me.ships.containsKey(e.getKey()));

			// Construct a list of all structures.
			for (final Player player : game.players) {
				allStructures.add(player.shipyard);
				allStructures.addAll(player.dropoffs.values());
			}

			// Calculate inspiration and halite density
			final Set<Position> futureDropoffs = new HashSet<>();
			final Set<MapCell> dropoffTargets = new HashSet<>();
			for (final MapCell cell : unrolledMap) {
				List<MapCell> circle = gameMap.getCircle(cell.position, CIRCLE_RAD);
				int totalHalite = 0;
				int numNearbyEnemies = 0;
				for (final MapCell c : circle) {
					totalHalite += c.halite;
					if (!c.isOccupied()) {
						continue;
					} else if (!c.ship.belongsTo(me)) {
						numNearbyEnemies++;
					}
				}
				cell.setSurroundingHalite(totalHalite);
				cell.setSurroundingHaliteDensity(totalHalite / circle.size());
				cell.setMineableHaliteThisTurn(
						((cell.halite + 3) / 4) * (numNearbyEnemies >= Constants.INSPIRATION_SHIP_COUNT ? 3 : 1));
				// Only consider inspiration once midgame starts?
				if (!is2p || game.turnNumber >= minDropoffTurn) {
					cell.setInspirationEnabled(numNearbyEnemies >= Constants.INSPIRATION_SHIP_COUNT);
				}
				// Iterate through a larger circle to count nearby allies/enemies
				numNearbyEnemies = 0;
				int numNearbyAllies = 0;
				circle = gameMap.getCircle(cell.position, is2p ? 6 : 4);
				for (final MapCell c : circle) {
					if (!c.isOccupied()) {
						continue;
					} else if (c.ship.belongsTo(me)) {
						numNearbyAllies++;
					} else {
						numNearbyEnemies++;
					}
				}
				cell.setNumNearbyAllies(numNearbyAllies);
				cell.setNumNearbyEnemies(numNearbyEnemies);

				// Calculate the nearest friendly structure to the cell
				final int shipyardDist = gameMap.calculateDistance(cell.position, me.shipyard.position);
				final Position nearestDropoff = me.dropoffs.values().stream()
						.filter(d -> gameMap.calculateDistance(cell.position, d.position) < shipyardDist)
						.min(comparing(d -> gameMap.calculateDistance(cell.position, d.position))).map(d -> d.position)
						.orElse(me.shipyard.position);
				cell.setNearestDropoff(nearestDropoff);

				// Dropoff target logic
				if ((gameMap.currentFreeHalite / gameMap.initialHalite) > remainingHaliteToSpawnUntil
						* (is2p || gameMap.width == 40 || gameMap.width == 48 ? 1 : 1.25) && cell.halite > 100
						&& (cell.getSurroundingHalite()) >= 8_500) {
					dropoffTargets.add(cell);
				}
			}

			dropoffTargets.stream().sorted(comp.reversed()).forEachOrdered(target -> {
				boolean nearDropoff = false;
				for (final Entity structure : allStructures) {
					if (gameMap.calculateDistance(target.position, structure.position) <= minDropoffDistance) {
						nearDropoff = true;
						break;
					}
				}
				if (gameMap.calculateDistance(target.position,
						target.getNearestDropoff()) <= (minDropoffDistance * 0.75)) {
					nearDropoff = true;
				}
				for (final Position futureDropoff : futureDropoffs) {
					if (nearDropoff
							|| gameMap.calculateDistance(target.position, futureDropoff) <= minDropoffDistance) {
						nearDropoff = true;
						break;
					}
				}
				if (!nearDropoff) {
					futureDropoffs.add(target.position);
				}
			});

			// F-Log future dropoffs
			for (final Position futureDropoff : futureDropoffs) {
				F_Log.log(futureDropoff, "teal", "Future Dropoff Location");
				F_Log.log(futureDropoff, "", "\\nSurrounding Halite: %f",
						gameMap.at(futureDropoff).getSurroundingHalite());
			}

			futureDropoffs.forEach(dropoffTarget -> {
				if (!shipsDroppingOff.isEmpty() || me.ships.size() <= (me.dropoffs.size() + 1) * 7) {
					return;
				}
				me.ships.values().stream().min(comparing(s -> gameMap.calculateDistance(dropoffTarget, s.position)))
						.ifPresent(ship -> {
							if (gameMap.calculateDistance(dropoffTarget, ship.position) <= dropoffTravelDist) {
								shipsDroppingOff.put(ship.id, gameMap.at(dropoffTarget));
								shipStatus.put(ship.id, "makeDropoff");
							}
						});
			});

			for (final Ship ship : me.ships.values()) {
				if (!shipStatus.containsKey(ship.id)) {
					shipStatus.put(ship.id, "exploring");
				}

				final MapCell shipCell = gameMap.at(ship);
				final boolean shipIsOnStructure = shipCell.hasStructure() && shipCell.structure.belongsTo(me);

				// Determine whether the ship should rush back to the base for end-game
				if (remainingTurns <= 1.5 * gameMap.calculateDistance(ship.position, shipCell.getNearestDropoff())) {
					shipStatus.put(ship.id, "finalRush");
				}

				if (shipStatus.get(ship.id).equals("finalRush")) {
					F_Log.log(ship.position, "brown", "Rushing Home");
					if (shipIsOnStructure) {
						shipCell.markSafe();
						shipPaths.put(ship, STILL);
					} else {
						final boolean allowCombat = gameMap.calculateDistance(ship.position,
								shipCell.getNearestDropoff()) <= 4;
						final Direction path = gameMap.getDirection(ship.position, shipCell.getNearestDropoff(),
								"finalRush", allowCombat);
						shipPaths.put(ship, path);
					}
					continue;
				}
				if (shipStatus.get(ship.id).equals("returning")) {
					if (shipIsOnStructure) {
						shipStatus.put(ship.id, "exploring");
					} else {
						F_Log.log(ship.position, "chocolate", "Returning Home");
						final boolean allowCombat = gameMap.calculateDistance(ship.position,
								shipCell.getNearestDropoff()) <= 4;
						final Direction path = gameMap.getDirection(ship.position, shipCell.getNearestDropoff(),
								"returning", allowCombat);
						shipPaths.put(ship, path);
						continue;
					}
				}

				// Calculate the highest-valued cells.
				final List<MapCell> topCells = new ArrayList<>();
				for (final MapCell cell : unrolledMap) {
					if (cell.hasStructure()) {
						continue;
					}
					final double dist = gameMap.calculateDistance(ship.position, cell.position);
					int distToBase = gameMap.calculateDistance(cell.position, cell.getNearestDropoff());
					for (final Position futureDropoff : futureDropoffs) {
						final int distance = gameMap.calculateDistance(cell.position, futureDropoff);
						if (distance < distToBase) {
							distToBase = (distToBase + distance) / 2;
						}
					}
					double baseValue = cell.halite
							+ (cell.getSurroundingHalite() - cell.halite) / (2 * CIRCLE_RAD * (CIRCLE_RAD + 1));
					if (is2p && cell.isOccupied() && !cell.ship.belongsTo(me)
							&& cell.getNumNearbyAllies() > cell.getNumNearbyEnemies()) {
						baseValue += cell.ship.halite;
					}
					final double value = cell.isInspirationEnabled()
							? (baseValue + (cell.halite * Constants.INSPIRED_BONUS_MULTIPLIER))
							: baseValue;
					cell.setValue(value / (dist + distToBase));
					topCells.add(cell);
				}
				topCells.sort(comparing(MapCell::getValue).reversed());

				// If the ship is on its way to make a dropoff.
				if (shipStatus.get(ship.id).equals("makeDropoff")) {
					final Position dropoffTarget = shipsDroppingOff.get(ship.id).position;
					haliteRequiredToConstruct -= gameMap.at(dropoffTarget).halite + me.halite;
					if (ship.position.equals(dropoffTarget)) {
						shipStatus.put(ship.id, "waitingToConstruct");
						// Reset haliteRequiredToConstruct
						haliteRequiredToConstruct = 4_000;
					} else {
						boolean nearDropoff = allStructures.stream().anyMatch(
								s -> gameMap.calculateDistance(dropoffTarget, s.position) <= minDropoffDistance);
						if (nearDropoff) {
							shipsDroppingOff.remove(ship.id);
							shipStatus.put(ship.id, "exploring");
						} else {
							boolean shipShouldMove = false;
							haliteRequiredToConstruct = Math.min(MAX_HALITE, haliteRequiredToConstruct);
							if (shipCell.halite <= 10 || ship.halite >= haliteRequiredToConstruct) {
								shipShouldMove = true;
							} else {
								final List<MapCell> potentialTargets = new ArrayList<>();
								final List<Direction> dirs = gameMap.getUnsafeMoves(ship.position, dropoffTarget);
								for (final Direction dir : dirs) {
									potentialTargets.add(gameMap.at(shipCell.position.directionalOffset(dir)));
								}
								for (final MapCell n : potentialTargets) {
									// If moving to the neighbour would give at least 50% more halite
									shipShouldMove = ((n.getMineableHaliteThisTurn()
											- (int) Math.floor(shipCell.halite * 0.1)
											- shipCell.getMineableHaliteThisTurn())
											- shipCell.getMineableHaliteThisTurn())
											/ (float) shipCell.getMineableHaliteThisTurn() > 0.5;
									if (shipShouldMove) {
										break;
									}
								}
							}
							F_Log.log(dropoffTarget, "yellow");
							if (shipShouldMove) {
								final boolean allowCombat = gameMap.calculateDistance(ship.position,
										dropoffTarget) <= 2;
								final Direction path = gameMap.getDirection(ship.position, dropoffTarget, allowCombat);
								shipPaths.put(ship, path);
								F_Log.log(ship.position, "yellow", "Navigating to Dropoff Target: %s", dropoffTarget);
							} else {
								F_Log.log(ship.position, "yellow", "Mining to form Dropoff. Need %d to construct.",
										haliteRequiredToConstruct);
								shipPaths.put(ship, STILL);
							}
							continue;
						}
					}
				}
				// If the ship is already at the dropoff destination, but is waiting to be able
				// to construct
				if (shipStatus.get(ship.id).equals("waitingToConstruct")) {
					haliteRequiredToConstruct -= ship.halite + shipCell.halite + me.halite;
					// Don't attempt to construct on top of a structure.
					if (shipCell.hasStructure() || turnsSpentWaiting >= 50) {
						shipsDroppingOff.remove(ship.id);
						shipStatus.put(ship.id, "exploring");
						turnsSpentWaiting = 0;
					} else if ((me.halite + ship.halite + shipCell.halite) >= 4_000) {
						F_Log.log(ship.position, "yellow", "Waiting to Construct Dropoff");
						me.halite = me.halite - (4_000 - ship.halite + shipCell.halite);
						commandQueue.add(ship.makeDropoff());
						turnsSpentWaiting = 0;
					} else if (turnsSpentWaiting > 0 && turnsSpentWaiting % 10 == 0) {
						// Move to the richest neighbor
						MapCell neighbor = gameMap.getNeighbors(shipCell).stream()
								.filter(cell -> cell.halite > shipCell.halite).max(comparing(cell -> cell.halite))
								.orElse(shipCell);
						shipPaths.put(ship, gameMap.getDirectionToNeighbor(shipCell, neighbor));
						turnsSpentWaiting++;
					} else {
						F_Log.log(ship.position, "yellow", "Waiting to Construct Dropoff");
						shipPaths.put(ship, STILL);
						turnsSpentWaiting++;
					}
					continue;
				}
				// If the ship is __% full, begin returning home
				if (ship.halite >= MAX_HALITE * (is2p ? 0.9 : 0.85)) {
					F_Log.log(ship.position, "chocolate", "Returning Home");
					shipStatus.put(ship.id, "returning");
					final Direction path = gameMap.getDirection(ship.position, shipCell.getNearestDropoff(),
							"returning", false);
					shipPaths.put(ship, path);
					continue;
				}

				// If you could move to the target and mine more halite, do so?
				boolean shipShouldMove = false;
				for (final MapCell target : topCells) {
					if (shipCell.halite < MAX_HALITE * IGNORE_PERCENT) {
						shipShouldMove = true;
						break;
					}
					if (targetCells.contains(target)) {
						continue;
					}
					final List<MapCell> potentialTargets = new ArrayList<>();
					final List<Direction> dirs = gameMap.getUnsafeMoves(ship.position, target.position);
					for (final Direction dir : dirs) {
						potentialTargets.add(gameMap.at(shipCell.position.directionalOffset(dir)));
					}

					for (final MapCell n : potentialTargets) {
						// If moving to the neighbour would give at least 5% more halite
						shipShouldMove = ((n.getMineableHaliteThisTurn() - (int) Math.floor(shipCell.halite * 0.1)
								- shipCell.getMineableHaliteThisTurn()) - shipCell.getMineableHaliteThisTurn())
								/ (float) shipCell.getMineableHaliteThisTurn() > 0.05;
						if (shipShouldMove) {
							break;
						}
					}

					break;
				}
				if (shipShouldMove) {
					topCells.removeAll(targetCells);
					MapCell target = ofNullable(topCells.isEmpty() ? null : topCells.get(0)).orElse(shipCell);
					targetCells.add(target);
					shipStatus.put(ship.id, "exploring");
				} else {
					shipPaths.put(ship, STILL);
					shipStatus.put(ship.id, "mining");
				}
			}

			for (final MapCell target : targetCells) {
				Optional<Ship> closestShip = me.ships.values().stream()
						.filter(ship -> shipStatus.get(ship.id).equals("exploring"))
						.min(comparing(ship -> gameMap.calculateDistance(target.position, ship.position)));
				if (closestShip.isPresent()) {
					final Ship ship = closestShip.get();
					final MapCell shipCell = gameMap.at(ship);
					final boolean allowCombat = shipCell.getNumNearbyAllies() > (shipCell.getNumNearbyEnemies() * 1.35);
					shipPaths.put(ship, gameMap.getDirection(ship.position, target.position, allowCombat));
					shipStatus.put(ship.id, "mining");
					F_Log.log(ship.position, "", "Target Cell: %s", target);
					F_Log.log(target.position, "#0000FF", "Targeted by Ship: %s", ship.id);
				}
			}

			List<Command> moveCommands = gameMap.navigate(shipPaths, shipStatus, startTime);
			commandQueue.addAll(moveCommands);

			// Ship spawning conditional logic.
			if ((me.ships.size() == 0 && remainingTurns >= 15 && me.halite > SHIP_COST)
					|| game.turnNumber <= maxTurns * 0.8f
							&& (gameMap.currentFreeHalite / gameMap.initialHalite) > remainingHaliteToSpawnUntil
							&& !gameMap.at(me.shipyard).isOccupied()) {
				if ((shipStatus.containsValue("waitingToConstruct") || shipStatus.containsValue("makeDropoff"))
						&& haliteRequiredToConstruct + SHIP_COST <= 0 && me.halite >= SHIP_COST) {
					commandQueue.add(me.shipyard.spawn());
				} else if (!(shipStatus.containsValue("waitingToConstruct") || shipStatus.containsValue("makeDropoff"))
						&& me.halite >= SHIP_COST) {
					commandQueue.add(me.shipyard.spawn());
				}
			}

			// Calculate and log how long the current turn took, as well as the current
			// total execution time.
			long endTime = System.nanoTime();
			Double took = (endTime - startTime) / 1_000_000.0;
			avgTime += took;
			Log.log("Turn took: %f ms", took);
			Log.log("Average turn time: %f ms", avgTime / game.turnNumber);
			game.endTurn(commandQueue);
		}
	}
}
