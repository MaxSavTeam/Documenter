package com.maxsavitsky.documenter.datatypes;

import androidx.annotation.NonNull;

import com.maxsavitsky.documenter.utils.Utils;

public class Entry {
	private String id, name, pathDir;

	public Entry(String id, String name) {
		this.id = id;
		this.name = name;

		this.pathDir = Utils.getContext().getExternalFilesDir(null).getPath() + "/" + id + "/";
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getPathDir() {
		return pathDir;
	}

	@NonNull
	@Override
	public String toString() {
		return "id=\"" + getId() + "\" name=\"" + getName() + "\"";
	}
}
