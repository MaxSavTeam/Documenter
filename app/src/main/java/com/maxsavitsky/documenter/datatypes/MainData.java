package com.maxsavitsky.documenter.datatypes;

import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.XMLParser;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
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

	public static Document getDocumentWithId(String id){
		for(int i = 0; i < sDocumentsList.size(); i++){
			if(sDocumentsList.get(i).getId().equals(id)){
				return sDocumentsList.get(i);
			}
		}
		return  null;
	}

	public static void readAllCategories() throws IOException, SAXException {
		XMLParser xmlParser = new XMLParser();
		setCategoriesList(xmlParser.parseCategories());
	}

	public static void readAllDocuments() throws IOException, SAXException {
		XMLParser xmlParser = new XMLParser();
		setDocumentsList(xmlParser.parseDocuments());
	}
}
