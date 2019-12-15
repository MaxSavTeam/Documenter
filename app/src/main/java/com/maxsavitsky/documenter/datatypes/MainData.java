package com.maxsavitsky.documenter.datatypes;

import java.util.ArrayList;

public class MainData {
	private static ArrayList<Category> sCategoriesList = new ArrayList<>();

	private static ArrayList<Document> sDocumentsList = new ArrayList<>();

	public static ArrayList<Category> getCategoriesList() {
		return sCategoriesList;
	}

	public static void setCategoriesList(ArrayList<Category> categoriesList) {
		MainData.sCategoriesList = categoriesList;
	}

	public static ArrayList<Document> getDocumentsList() {
		return sDocumentsList;
	}

	public static void setDocumentsList(ArrayList<Document> documentsList) {
		sDocumentsList = documentsList;
	}

	public static ArrayList<Document> getDocumentsFromCategoryWithId(String categoryId){
		for(int i = 0; i < sCategoriesList.size(); i++){
			if(sCategoriesList.get(i).getId().equals(categoryId)){
				return sCategoriesList.get(i).getDocuments();
			}
		}
		return null;
	}
}
