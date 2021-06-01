package com.maxsavitsky.documenter.data.types;

public abstract class Entity {
	public enum Type{
		GROUP,
		ENTRY
	}

	protected final String id, name;
	protected long creationTimestamp = 0;

	public Entity(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public long getCreationTimestamp() {
		return creationTimestamp;
	}

	public void setCreationTimestamp(long creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}

	public abstract Type getType();

}
