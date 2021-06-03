package com.maxsavitsky.documenter.data.types;

import com.maxsavitsky.documenter.data.EntitiesStorage;

import java.util.ArrayList;

public abstract class Entity {
	public enum Type{
		GROUP,
		ENTRY
	}

	protected final String id;
	protected String name;
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

	public void removeParent(String id){
		parents.removeIf( s->s.equals( id ) );
	}

	public void unlink(){
		for(String p : parents){
			EntitiesStorage.get().getGroup( p ).ifPresent( g->g.removeContainingEntity( id ) );
		}
		parents.clear();
	}

	public ArrayList<String> getParents() {
		return parents;
	}

	public boolean isRoot(){
		return parents.isEmpty();
	}

	public void rename(String name){
		this.name = name;
	}

	public abstract Type getType();

}
