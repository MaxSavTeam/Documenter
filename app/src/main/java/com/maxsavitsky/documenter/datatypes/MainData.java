package com.maxsavitsky.documenter.datatypes;

import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.ParseSeparate;
import com.maxsavitsky.documenter.xml.XMLParser;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

public class MainData {

	// START Categories
	private static Map<String, Category> sCategoryMap = new HashMap<>();

	private static ArrayList<Category> sCategoriesList = new ArrayList<>();

	public static ArrayList<Category> getCategoriesList() {
		return sCategoriesList;
	}

	public static void setCategoriesList(ArrayList<Category> categoriesList) {
		sCategoriesList = categoriesList;
		sCategoryMap.clear();
		for(Category category : categoriesList){
			sCategoryMap.put(category.getId(), category);
		}
	}

	public static ArrayList<Document> getDocumentsFromThisCategory(String id) throws Exception {
		ArrayList<Document> documents;
		try {
			documents = ParseSeparate.parseCategoryWithId( id );
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception( e.toString() );
			//return new ArrayList<>(  );
		}
		return documents;
	}

	public static ArrayList<Category> getCategoriesInWhichIncludedDocumentWithId(String id) throws IOException, SAXException {
		File path = new File(Utils.getExternalStoragePath().getPath() + "/documents");
		if(!path.exists()){
			throw new FileNotFoundException("MainData.getCategoriesInWhichIncludedDocumentWithId: documents dir does not exist");
		}
		path = new File(path.getPath() + "/" + id);
		if(!path.exists()){
			throw new FileNotFoundException("MainData.getCategoriesInWhichIncludedDocumentWithId: dir for this id does not exist; id=" + id + "; path=" + path.getPath());
		}
		path = new File(path.getPath() + "/included_in.xml");
		if(!path.exists()){
			return new ArrayList<>(  );
		}

		XMLParser xmlParser = new XMLParser();
		return xmlParser.parseCategoriesInWhichIncludedDocumentWithId(id);
	}

	public static Category getCategoryWithId(String id){
		return sCategoryMap.get(id);
	}

	public static void removeCategoryWithId(String id){
		for(int i = 0; i < sCategoriesList.size(); i++){
			if(sCategoriesList.get(i).getId().equals(id)){
				sCategoriesList.remove(i);
				return;
			}
		}
		sCategoryMap.remove(id);
	}

	public static boolean finallyDeleteCategoryWithId(String id) throws Exception {
		File file = new File(Utils.getExternalStoragePath().getPath() + "/categories/" + id + "/" + id + ".xml");
		try{
			Category category = getCategoryWithId(id);

			if(category == null){
				throw new IllegalArgumentException("MainData.finallyDeleteCategoryWithId: category with id=" + id + " does not exist");
			}

			ArrayList<Document> documents = ParseSeparate.parseCategoryWithId( id );
			if(documents.size() != 0){
				for(Document document : documents){
					ArrayList<Category> categories = getCategoriesInWhichIncludedDocumentWithId(document.getId());
					if(categories.size() <= 1){
						deleteDocument(document.getId());
					}
				}
			}
			removeCategoryWithId(id);
			Utils.saveCategoriesList(getCategoriesList());
			return file.delete();
		}catch (Exception e){
			e.printStackTrace();
			throw new Exception( e.toString() );
		}
	}

	public static void readAllCategories() throws IOException, SAXException {
		XMLParser xmlParser = new XMLParser();
		setCategoriesList(xmlParser.parseCategories());
	}
	// END Categories
	// START Documents
	private static ArrayList<Document> sDocumentsList = new ArrayList<>();
	private static Map<String, Document> sDocumentMap = new HashMap<>(  );

	public static ArrayList<Document> getDocumentsList() {
		return sDocumentsList;
	}

	public static void setDocumentsList(ArrayList<Document> documentsList) {
		sDocumentsList = documentsList;
		sDocumentMap.clear();
		for(Document document : documentsList){
			sDocumentMap.put( document.getId(), document );
		}
	}

	private static void removeDocumentWithId(String id){
		sDocumentMap.remove( id );
		for(int i = 0; i < sDocumentsList.size(); i++){
			if(sDocumentsList.get(i).getId().equals(id)){
				sDocumentsList.remove(i);
				return;
			}
		}
	}

	public static Document getDocumentWithId(String id){
		return sDocumentMap.get( id );
	}

	private static void deleteDocument(String id) throws Exception {
		File path = new File(Utils.getExternalStoragePath().getPath() + "/documents/" + id);
		if(!path.isDirectory()){
			throw new Exception("MainData.deleteDocument: path with id=" + id + " not a directory");
		}
		removeDocumentWithId(id);

		Utils.saveDocumentsList(getDocumentsList());

		// Entries, which do not included in any document, will not be deleted
		/*ArrayList<Entry> entries = ParseSeparate.parseDocumentWithId( id );
		for(Entry entry : entries){
			ArrayList<Document> documents = ParseSeparate.getDocumentsInWhichIncludedEntryWithId( entry.getId() );
			if(documents.size() == 1){
				deleteEntryWithId( entry.getId() );
			}
		}*/
		path.delete();
	}

	public static void readAllDocuments() throws IOException, SAXException {
		XMLParser xmlParser = new XMLParser();
		setDocumentsList(xmlParser.parseDocuments());
	}
	// END Documents
	// START Entries
	private static ArrayList<Entry> sEntriesList = new ArrayList<>();
	private static Map<String, Entry> sEntryMap = new HashMap<>(  );

	public static ArrayList<Entry> getEntriesList() {
		return sEntriesList;
	}

	public static void setEntriesList(ArrayList<Entry> entriesList) {
		sEntriesList = entriesList;
		sEntryMap.clear();
		for(Entry entry : entriesList){
			sEntryMap.put( entry.getId(), entry );
		}
	}

	public static Entry getEntryWithId(String id){
		return sEntryMap.get( id );
	}

	public static void removeEntryWithId(String id){
		sEntryMap.remove( id );
		for(int i = 0; i < sEntriesList.size(); i++){
			if(sEntriesList.get( i ).getId().equals( id )){
				sEntriesList.remove( i );
				return;
			}
		}
	}

	public static void readAllEntries() throws IOException, SAXException {
		XMLParser xmlParser = new XMLParser();
		setEntriesList( xmlParser.parseEntries() );
	}

	// TODO: 19.12.2019 realize entry deletion full
	private static void deleteEntryWithId(String id){
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries/" + id );
		if(!file.exists()){
			return;
		}
		if(!file.isDirectory()){
			throw new IllegalArgumentException( "MainData.deleteEntryWithId: id=" + id + " not a directory" );
		}

		removeEntryWithId( id );
		Utils.saveEntriesList( getEntriesList() );
	}
}
