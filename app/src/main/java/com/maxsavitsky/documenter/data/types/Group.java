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

	public boolean addMember(Entity e) {
		if ( e.getId().equals( getId() ) || containingEntities.stream().anyMatch( en->en.getId().equals( e.getId() ) ) ) {
			return false;
		}
		if(e instanceof Group){
			if(isContainsRoot( (Group) e ))
				return false;
		}
		ArrayList<Entity> entities = new ArrayList<>( containingEntities );
		entities.add( e );
		setContainingEntities( entities );
		e.addParent( getId() );
		return true;
	}

	private boolean isContainsRoot(Group g){
		for(Entity en : g.getContainingEntities()){
			if(en.getId().equals( getId() ))
				return true;
			if(en instanceof Group && isContainsRoot( (Group) en ))
				return true;
		}
		return false;
	}

	public void removeContainingEntity(String id) {
		containingEntities.removeIf( e->e.getId().equals( id ) );
	}
}
