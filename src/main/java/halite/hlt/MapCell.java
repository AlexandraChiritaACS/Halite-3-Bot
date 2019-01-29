package halite.hlt;

public class MapCell {
	public final Position position;
	public int halite;
	public Ship ship;
	public Entity structure;
	private Double value;
	private int mineableHaliteThisTurn;
	private double surroundingHalite;
	private double surroundingHaliteDensity;
	private boolean inspirationEnabled;
	private int numNearbyEnemies;
	private int numNearbyAllies;
	private Position nearestDropoff;

	public MapCell(final Position position, final int halite) {
		this.position = position;
		this.halite = halite;
		this.value = -1.0;
		this.mineableHaliteThisTurn = (halite + 3) / 4;
		this.surroundingHaliteDensity = this.halite;
		this.surroundingHalite = this.halite;
		this.inspirationEnabled = false;
		this.numNearbyAllies = 0;
		this.numNearbyEnemies = 0;
	}

	public boolean isEmpty() {
		return ship == null && structure == null;
	}

	public boolean isOccupied() {
		return ship != null;
	}

	public boolean hasStructure() {
		return structure != null;
	}

	public void markUnsafe(final Ship ship) {
		this.ship = ship;
	}

	public void markSafe() {
		this.ship = null;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(Double value) {
		this.value = value;
	}

	/**
	 * @return the value
	 */
	public Double getValue() {
		return value;
	}

	/**
	 * @param mineableHaliteThisTurn the mineableHaliteThisTurn to set
	 */
	public void setMineableHaliteThisTurn(int mineableHaliteThisTurn) {
		this.mineableHaliteThisTurn = mineableHaliteThisTurn;
	}

	/**
	 * @return the mineableHaliteThisTurn
	 */
	public int getMineableHaliteThisTurn() {
		return mineableHaliteThisTurn;
	}

	/**
	 * @param surroundingHaliteDensity the surroundingHalite to set
	 */
	public void setSurroundingHaliteDensity(double surroundingHaliteDensity) {
		this.surroundingHaliteDensity = surroundingHaliteDensity;
	}

	/**
	 * @return the surroundingHalite
	 */
	public double getSurroundingHaliteDensity() {
		return surroundingHaliteDensity;
	}

	/**
	 * @param surroundingHalite the surroundingHalite to set
	 */
	public void setSurroundingHalite(double surroundingHalite) {
		this.surroundingHalite = surroundingHalite;
	}

	/**
	 * @return The total amount of halite surrounding the cell.
	 */
	public double getSurroundingHalite() {
		return surroundingHalite;
	}

	/**
	 * @return the inspirationEnabled
	 */
	public boolean isInspirationEnabled() {
		return inspirationEnabled;
	}

	/**
	 * @param inspirationEnabled the inspirationEnabled to set
	 */
	public void setInspirationEnabled(boolean inspirationEnabled) {
		this.inspirationEnabled = inspirationEnabled;
	}

	/**
	 * @param numNearbyEnemies the numNearbyEnemies to set
	 */
	public void setNumNearbyEnemies(int numNearbyEnemies) {
		this.numNearbyEnemies = numNearbyEnemies;
	}

	/**
	 * @return the numNearbyEnemies
	 */
	public int getNumNearbyEnemies() {
		return numNearbyEnemies;
	}

	/**
	 * @param numNearbyAllies the numNearbyAllies to set
	 */
	public void setNumNearbyAllies(int numNearbyAllies) {
		this.numNearbyAllies = numNearbyAllies;
	}

	/**
	 * @return the numNearbyAllies
	 */
	public int getNumNearbyAllies() {
		return numNearbyAllies;
	}

	/**
	 * @param nearestDropoff the nearestDropoff to set
	 */
	public void setNearestDropoff(Position nearestDropoff) {
		this.nearestDropoff = nearestDropoff;
	}

	/**
	 * @return the nearestDropoff
	 */
	public Position getNearestDropoff() {
		return nearestDropoff;
	}

	@Override
	public String toString() {
		return this.position.toString();
	}
}
