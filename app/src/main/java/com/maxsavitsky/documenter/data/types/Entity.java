package com.maxsavitsky.documenter.data.types;

import java.util.ArrayList;

public abstract class Entity {
	public enum Type{
		GROUP,
		ENTRY
	}

	protected final String id, name;
	protected long creationTimestamp = 0;
	protected final ArrayList<String> parents = new ArrayList<>();

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

	public void addParent(String id){
		parents.add( id );
	}

	public boolean isRoot(){
		return parents.isEmpty();
	}

	public abstract Type getType();

}
