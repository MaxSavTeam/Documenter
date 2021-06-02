package com.maxsavitsky.documenter.data.types;

import java.util.ArrayList;

public class Group extends Entity {

	private ArrayList<? extends Entity> containingEntities = new ArrayList<>();

	public Group(String id, String name) {
		super( id, name );
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
