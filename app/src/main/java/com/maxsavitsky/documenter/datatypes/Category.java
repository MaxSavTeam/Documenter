package com.maxsavitsky.documenter.datatypes;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class Category {
	private String mId;
	private String mName;

	private ArrayList<Document> mDocuments = new ArrayList<>();

	public ArrayList<Document> getDocuments() {
		return mDocuments;
	}

	public void addDocument(Document document){
		mDocuments.add(document);
	}

	public void removeDocumentWithId(String documentId){
		for(int i = 0; i < mDocuments.size(); i++){
			if(mDocuments.get(i).getId().equals(documentId)){
				mDocuments.remove(i);
				return;
			}
		}
	}

	public Document getDocumentWithId(String id){
		for(int i = 0; i < mDocuments.size(); i++){
			if(mDocuments.get(i).getId().equals(id)){
				return mDocuments.get(i);
			}
		}
		return null;
	}

	public Category(String id, String name){
		this.mId = id;
		this.mName = name;
	}

	public String getId() {
		return mId;
	}

	public String getName() {
		return mName;
	}

	@NonNull
	@Override
	public String toString() {
		return "id=\"" + getId() + "\" name=\"" + getName() + "\"";
	}
}
