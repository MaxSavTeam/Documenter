package com.maxsavitsky.documenter.data.types;

import java.util.ArrayList;

public class Group extends Entity {

	private final String id, name;

	private ArrayList<? extends Entity> containingEntities = new ArrayList<>();

	public Group(String id, String name) {
		super(id, name);
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public Type getType() {
		return Type.GROUP;
	}

	public void setContainingEntities(ArrayList<? extends Entity> containingEntities) {
		this.containingEntities = containingEntities;
	}

	public ArrayList<? extends Entity> getContainingEntities() {
		return containingEntities;
	}
}
