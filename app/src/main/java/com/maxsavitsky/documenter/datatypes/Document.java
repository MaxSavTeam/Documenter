package com.maxsavitsky.documenter.datatypes;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class Document {
	private String id, name;

	private ArrayList<Entry> mEntries = new ArrayList<>();

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

	public ArrayList<Entry> getEntries() {
		return mEntries;
	}

	public void addEntry(Entry entry){
		mEntries.add(entry);
	}

	@NonNull
	@Override
	public String toString() {
		return "id=\"" + this.id + "\" name=\"" + this.name + "\"";
	}
}
