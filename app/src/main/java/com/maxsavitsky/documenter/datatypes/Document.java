package com.maxsavitsky.documenter.datatypes;

public class Document {
	private String id, name;
	public Document(String id, String name){
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
