package halite.hlt;

public class Entity {
	public final PlayerId owner;
	public final EntityId id;
	public final Position position;

	public Entity(final PlayerId owner, final EntityId id, final Position position) {
		this.owner = owner;
		this.id = id;
		this.position = position;
	}

	/**
	 * Determines whether an entity is owned by the given Player.
	 * 
	 * @param player
	 * @return - True if the entity belongs to the given Player.
	 */
	public boolean belongsTo(final Player player) {
		return owner.equals(player.id);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Entity entity = (Entity) o;

		if (!owner.equals(entity.owner)) return false;
		if (!id.equals(entity.id)) return false;
		return position.equals(entity.position);
	}

	@Override
	public int hashCode() {
		int result = owner.hashCode();
		result = 31 * result + id.hashCode();
		result = 31 * result + position.hashCode();
		return result;
	}
}
