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

	public static ArrayList<Category> getCategoriesInWhichIncludedDocumentWithId(String id) throws Exception {
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
			throw new FileNotFoundException( "MainData.getCategoriesInWhichIncludedDocumentWithId: included_in.xml not found; id="+ id );
		}

		XMLParser xmlParser = new XMLParser();
		return xmlParser.parseCategoriesInWhichIncludedDocumentWithId(id);
	}

	public static Category getCategoryWithId(String id) throws NullPointerException {
		Category category = sCategoryMap.get( id );
		if(category == null){
			throw new NullPointerException( "MainData.getCategoryWithId: category with id=" + id + " not found" );
		}
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
		try{
			Category category = getCategoryWithId(id);

			if(category == null){
				throw new IllegalArgumentException("MainData.finallyDeleteCategoryWithId: category with id=" + id + " does not exist");
			}

			ArrayList<Document> documents = ParseSeparate.parseCategoryWithId( id );
			if(documents.size() != 0){
				for(Document document : documents){
					document.removeCategoryFromIncludedXml( id );
				}
			}
			removeCategoryWithId(id);
			Utils.saveCategoriesList(getCategoriesList());
			File file = new File( Utils.getExternalStoragePath().getPath() + "/categories/" + id );
			for(File file1 : file.listFiles()){
				file1.delete();
			}
			return file.delete();
		}catch (Exception e){
			e.printStackTrace();
			throw new Exception( e.toString() + "\n" + e.getStackTrace()[0] );
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

	private static boolean deleteDocument(String id) throws Exception {
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
		for(File file : path.listFiles()){
			file.delete();
		}
		return path.delete();
	}

	public static boolean finallyDeleteDocumentWithId(String id) throws Exception {
		File file = new File( Utils.getExternalStoragePath().getPath() + "/documents/" + id + "/" + id + ".xml" );
		if ( !file.exists() )
			throw new IllegalArgumentException( "MainData.finallyDeleteDocumentWithId: document with id=" + id + " does not exist" );

		Document document = MainData.getDocumentWithId( id );
		if ( document == null )
			throw new IllegalArgumentException( "MainData.finallyDeleteDocumentWithId: document not found; id=" + id );

		ArrayList<Entry> entries = ParseSeparate.parseDocumentWithId( id );
		if(entries.size() != 0){
			for(Entry entry : entries){
				entry.removeDocumentFromIncluded( document.getId() );
			}
		}
		ArrayList<Category> categories = document.getCategoriesInWhichIncludedDocument();
		for(Category category : categories){
			category.removeDocument( document );
		}
		return deleteDocument( id );

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

	public static boolean finallyDeleteEntryWithId(String id) throws Exception{
		Entry entry = getEntryWithId( id );
		ArrayList<Document> documents = entry.getDocumentsInWhichIncludedThisEntry();
		for(Document document : documents){
			entry.removeDocumentFromIncluded( document.getId() );
			document.removeEntry( entry );
		}

		return deleteEntryWithId( id );
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

	private static boolean deleteEntryWithId(String id){
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries/" + id );
		if(!file.exists()){
			return true;
		}
		if(!file.isDirectory()){
			throw new IllegalArgumentException( "MainData.deleteEntryWithId: id=" + id + " not a directory" );
		}

		removeEntryWithId( id );
		Utils.saveEntriesList( getEntriesList() );

		File[] files = file.listFiles();
		if(files == null)
			return file.delete();
		for(File subFile : files){
			if(!subFile.delete())
				return false;
		}
		return file.delete();
	}
}
