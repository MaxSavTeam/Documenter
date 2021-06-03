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

	public void addMember(Entity e) {
		if ( containingEntities.stream().noneMatch( en->en.getId().equals( e.getId() ) ) ) {
			ArrayList<Entity> entities = new ArrayList<>( containingEntities );
			entities.add( e );
			setContainingEntities( entities );
		}
	}

	public void removeContainingEntity(String id) {
		containingEntities.removeIf( e->e.getId().equals( id ) );
	}
}
