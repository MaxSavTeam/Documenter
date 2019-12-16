package com.maxsavitsky.documenter.datatypes;

import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.XMLParser;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
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

	public static ArrayList<Category> getCategoriesInWhichIncludedDocumentWithId(String id) throws IOException, SAXException {
		File path = new File(Utils.getExternalStoragePath().getPath() + "/documents");
		if(!path.exists()){
			throw new FileNotFoundException("MainData.getCategoriesInWhichIncludedDocumentWithId: documents dir does not exist");
		}
		path = new File(path.getPath() + "/" + id);
		if(!path.exists()){
			throw new FileNotFoundException("MainData.getCategoriesInWhichIncludedDocumentWithId: dir for this id does not exist");
		}
		path = new File(path.getPath() + "/included_in.xml");

		XMLParser xmlParser = new XMLParser();
		return xmlParser.parseCategoriesInWhichIncludedDocumentWithId(id);
	}

	public static Category getCategoryWithId(String id){
		for(int i = 0; i < sCategoriesList.size(); i++){
			if(sCategoriesList.get(i).getId().equals(id)){
				return sCategoriesList.get(i);
			}
		}
		return null;
	}

	private static void removeCategoryWithId(String id){
		for(int i = 0; i < sCategoriesList.size(); i++){
			if(sCategoriesList.get(i).getId().equals(id)){
				sCategoriesList.remove(i);
				return;
			}
		}
	}

	/*public static boolean finallyDeleteDocumentWithId(String id) throws FileNotFoundException {
		File path = new File(Utils.getExternalStoragePath().getPath() + "/documents");
		if (!path.exists()) {
			throw new FileNotFoundException("MainData.finallyDeleteDocumentWithId: documents dir not found");
		}
		path = new File(path.getPath() + "/" + id);
		if(!path.exists()){
			throw new FileNotFoundException("MainData.finallyDeleteDocumentWithId: id dir not found; id=" + id);
		}
		path = new File(path.getPath())
	}*/



	public static boolean finallyDeleteCategoryWithId(String id){
		File file = new File(Utils.getExternalStoragePath().getPath() + "/categories/" + id + ".xml");
		if(file.delete()){
			/*removeCategoryWithId(id);
			Utils.saveCategoriesList(getCategoriesList());*/

			try{
				Category category = getCategoryWithId(id);
				assert category != null;

				ArrayList<Document> documents = category.getDocuments();
				if(documents == null || documents.size() == 0){
					removeCategoryWithId(id);
					Utils.saveCategoriesList(getCategoriesList());
				}else{
					for(Document document : documents){
						ArrayList<Category> categories = getCategoriesInWhichIncludedDocumentWithId(id);
						if(categories.size() == 1){
							// TODO: 16.12.2019 method to delete documents
						}
					}
				}
			}catch (Exception e){
				e.printStackTrace();
				return false;
			}
			return true;
		}else{
			return false;
		}
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
