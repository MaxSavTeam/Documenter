package com.maxsavitsky.documenter.data;

import com.maxsavitsky.documenter.data.types.Entity;
import com.maxsavitsky.documenter.data.types.EntryEntity;
import com.maxsavitsky.documenter.data.types.Group;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntitiesStorage {

	private static EntitiesStorage instance;

	public static EntitiesStorage get() {
		if ( instance == null ) {
			instance = new EntitiesStorage();
		}
		return instance;
	}

	private ArrayList<Group> mGroups = new ArrayList<>();
	private ArrayList<EntryEntity> mEntryEntities = new ArrayList<>();

	public void setGroups(ArrayList<Group> groups) {
		mGroups = groups;
	}

	public void setEntryEntities(ArrayList<EntryEntity> entryEntities) {
		mEntryEntities = entryEntities;
	}

	public Optional<Group> getGroup(String id) {
		for (Group g : mGroups)
			if ( g.getId().equals( id ) ) {
				return Optional.of( g );
			}
		return Optional.empty();
	}

	public ArrayList<Entity> getRootEntities() {
		return Stream.concat(
				mGroups.stream()
						.filter( Entity::isRoot ),
				mEntryEntities.stream()
						.filter( Entity::isRoot ) )
				.collect( Collectors.toCollection( ArrayList::new ) );
	}

}
