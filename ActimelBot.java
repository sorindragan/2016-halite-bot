import java.util.*;

public class ActimelBot {
	public static final double MAGIC_NUMBER = 10.5;

	private static HashMap<Direction, Site> getNeighbours(GameMap gameMap, int x, int y) {
		HashMap<Direction, Site> neighbours = new HashMap<>();

		neighbours.put(Direction.WEST, gameMap.getLocation((x - 1 + gameMap.width) % gameMap.width, y).getSite());
		neighbours.put(Direction.EAST, gameMap.getLocation((x + 1) % gameMap.width,y).getSite());
		neighbours.put(Direction.NORTH, gameMap.getLocation(x,(y - 1 + gameMap.height) % gameMap.height).getSite());
		neighbours.put(Direction.SOUTH, gameMap.getLocation(x,(y + 1) % gameMap.height).getSite());

		return neighbours;
	}

	// search for closest site with different id
	private static double findClosestBorder(GameMap gameMap, Location location, int myID, int id,
											int xRatio, int yRatio, int maxDistance) {
		int distance = 0;
		int x = xRatio;
		int y = yRatio;
		double density = 0;
		Site site;

		// search until maximum distance
		while (distance < maxDistance){
			site = gameMap.getLocation((location.getX() + x + gameMap.width) % gameMap.width,
					(location.getY() + y + gameMap.height) % gameMap.height).getSite();

			// find if the site is mine or neutral
			if (site.owner == myID || site.owner == id) {
				distance++;
				x += xRatio;
				y += yRatio;
			} else {
				if (site.production == 0) {
					return Double.MAX_VALUE;
				}
				return distance;
			}
		}

		return Double.MAX_VALUE;
	}

	// look for enemy
	private static Move getToClosestEnemy(GameMap gameMap, int myID, Location location) {
		// find the closest enemy in every direction
		double north = findClosestBorder(gameMap, location, myID, 0,0, -1,
				gameMap.height);
		double south = findClosestBorder(gameMap, location, myID, 0,0, 1,
				gameMap.height);
		double west = findClosestBorder(gameMap, location, myID, 0,-1, 0,
				gameMap.width);
		double east = findClosestBorder(gameMap, location, myID, 0,1, 0,
				gameMap.width);

		// choose the minimum distance
		double min = Math.min(west, Math.min(east, Math.min(north, south)));

		// if the minimum distance is MAX_VALUE, go for the closest border
		if (min == Double.MAX_VALUE) {
			return new Move(location, getToClosestBorder(gameMap, myID, location));
		}
		if (min == north) {
			return new Move(location, Direction.NORTH);
		} else if (min == south) {
			return new Move(location, Direction.SOUTH);
		} else if (min == west) {
			return new Move(location, Direction.WEST);
		} else if (min == east) {
			return new Move(location, Direction.EAST);
		}

		return new Move(location, Direction.STILL);
	}

	// search for the nearest neutral site
	private static Direction getToClosestBorder(GameMap gameMap, int myID, Location location) {
		// find the nearest neutral site in every direction
		double north = findClosestBorder(gameMap, location, myID, myID,0, -1,
				gameMap.height);
		double south = findClosestBorder(gameMap, location, myID, myID,0, 1,
				gameMap.height);
		double west = findClosestBorder(gameMap, location, myID, myID,-1, 0,
				gameMap.width);
		double east = findClosestBorder(gameMap, location, myID, myID,1, 0,
				gameMap.width);

		// pick minimum distance
		double min = Math.min(west, Math.min(east, Math.min(north, south)));

		if (min == north) {
			return Direction.NORTH;
		} else if (min == south) {
			return Direction.SOUTH;
		} else if (min == west) {
			return Direction.WEST;
		} else if (min == east) {
			return Direction.EAST;
		}

		return Direction.STILL;
	}

	// verify if a site is surrounded by other not owned sites
	private static boolean isSurrounded(HashMap<Direction, Site> neighbours, int myID) {
		for (Site site : neighbours.values()) {
			if (site.owner != myID && site.production != 0) {
				return false;
			}
		}
		return true;
	}

	// move function for border sites
	private static Move getBorderMove(HashMap<Direction, Site> neighbours, int myID,
									  Location location) {
		double minDensity = Double.MAX_VALUE;
		Direction minDirection = null;

		// determinates the best move analyzing the ratio for every neighbour
		for (Map.Entry<Direction, Site> entry : neighbours.entrySet()) {
			Site neighbourSite = entry.getValue();
			Direction neighbourDirection = entry.getKey();


			if (neighbourSite.production > 0
					&& neighbourSite.owner != myID
					&& neighbourSite.strength < location.getSite().strength
					&& neighbourSite.strength/ neighbourSite.production < minDensity) {

				minDensity = neighbourSite.strength / neighbourSite.production;
				minDirection = neighbourDirection;
			}
		}

		// if no move is a valid one, merge border cells
		Site north, south, east, west;
		Site site = location.getSite();
		if (minDirection == null) {

			north = neighbours.get(Direction.NORTH);
			south = neighbours.get(Direction.SOUTH);
			east = neighbours.get(Direction.EAST);
			west = neighbours.get(Direction.WEST);

			// neighbour case: north and south
			if (north.owner == myID && south.owner == myID) {
				if (site.strength + north.strength > north.strength + north.production
						&& north.strength > north.production && north.strength > MAGIC_NUMBER) {

					return new Move(location, Direction.NORTH);
				} else if (site.strength + south.strength > south.strength + south.production
						&& south.strength > south.production && south.strength > MAGIC_NUMBER) {

					return new Move(location, Direction.SOUTH);
				}
			} else if (east.owner == myID && west.owner == myID) {
				// neighbour case: east and west
				if (site.strength + east.strength > east.strength + east.production
						&& east.strength > east.production && east.strength > MAGIC_NUMBER) {
					return new Move(location, Direction.EAST);
				} else if (site.strength + west.strength > west.strength + west.production
						&& west.strength > west.production && west.strength > MAGIC_NUMBER) {
					return new Move(location, Direction.WEST);
				}
			}
			// if no move is good enough, remain still
			minDirection = Direction.STILL;
		}
		return new Move(location, minDirection);
	}

	public static void main(String[] args) throws java.io.IOException {

		final InitPackage iPackage = Networking.getInit();
		final int myID = iPackage.myID;
		final GameMap gameMap = iPackage.map;
		HashMap<Direction, Site> neighbours;

		Networking.sendInit("Actimel");

		while(true) {
			List<Move> moves = new ArrayList<>();
			Networking.updateFrame(gameMap);

			for (int y = 0; y < gameMap.height; y++) {
				for (int x = 0; x < gameMap.width; x++) {
					final Location location = gameMap.getLocation(x, y);
					final Site site = location.getSite();
					if(site.owner == myID) {
						neighbours = getNeighbours(gameMap, x, y);
						// if the accumulated strength is not enough, stay still
						if (site.strength < 4 * site.production) {
							moves.add(new Move(location, Direction.STILL));
							// three-quarters of full strength
						} else if (site.strength > 255 * 3 / 4) {
							// don't merge big cells
							moves.add(new Move(location, getToClosestBorder(gameMap, myID
									, location)));
						} else if (site.strength == 0) {
							moves.add(new Move(location, Direction.STILL));
						} else if (isSurrounded(neighbours, myID)) {
							// if it is surrounded by owned cells go in an enemy direction
							moves.add(getToClosestEnemy(gameMap, myID, location));
						} else {
							// border case
							moves.add(getBorderMove(neighbours, myID, location));
						}
					}
				}
			}
			Networking.sendFrame(moves);
		}
	}
}
