package com.maxsavitsky.documenter.data.types;

public class EntryEntity extends Entity {
	public EntryEntity(String id, String name) {
		super( id, name );
	}

	@Override
	public Type getType() {
		return Type.ENTRY;
	}
}
