package com.maxsavitsky.documenter.data;

import com.maxsavitsky.documenter.data.types.Category;
import com.maxsavitsky.documenter.data.types.Document;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.XMLParser;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainData {

	public static void clearAll(){
		sCategoriesList.clear();
		sCategoryMap.clear();

		sDocumentsList.clear();
		sDocumentMap.clear();

		sEntriesList.clear();
		sEntryMap.clear();
	}
	// START Categories

	private static final Map<String, Category> sCategoryMap = new HashMap<>();

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
		documents = XMLParser.newInstance().parseCategoryWithId( id );

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

		return XMLParser.newInstance().parseCategoriesInWhichIncludedDocumentWithId(id);
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

	public static boolean finallyDeleteCategoryWithId(String id) throws IOException, SAXException {
		Category category = getCategoryWithId(id);

		if(category == null){
			throw new IllegalArgumentException("MainData.finallyDeleteCategoryWithId: category with id=" + id + " does not exist");
		}

		ArrayList<Document> documents = XMLParser.newInstance().parseCategoryWithId( id );
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
	}

	public static void readAllCategories() throws IOException, SAXException {
		setCategoriesList(XMLParser.newInstance().parseCategories());
	}
	// END Categories
	// START Documents
	private static ArrayList<Document> sDocumentsList = new ArrayList<>();
	private static final Map<String, Document> sDocumentMap = new HashMap<>(  );

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

	private static boolean deleteDocument(String id)  {
		File path = new File(Utils.getExternalStoragePath().getPath() + "/documents/" + id);
		if(!path.isDirectory()){
			return false;
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

	public static boolean finallyDeleteDocumentWithId(String id, boolean deleteEntries) throws IOException, SAXException {
		File file = new File( Utils.getExternalStoragePath().getPath() + "/documents/" + id + "/" + id + ".xml" );
		if ( !file.exists() )
			throw new IllegalArgumentException( "MainData.finallyDeleteDocumentWithId: document with id=" + id + " does not exist" );

		Document document = MainData.getDocumentWithId( id );
		if ( document == null )
			throw new IllegalArgumentException( "MainData.finallyDeleteDocumentWithId: document not found; id=" + id );

		ArrayList<Entry> entries = XMLParser.newInstance().parseDocumentWithId( id );
		if(entries.size() != 0){
			for(Entry entry : entries){
				entry.removeDocumentFromIncluded( document.getId() );
				if(deleteEntries){
					if(entry.getDocumentsInWhichIncludedThisEntry().size() == 0){
						finallyDeleteEntryWithId( entry.getId() );
					}
				}
			}
		}
		ArrayList<Category> categories = document.getCategoriesInWhichIncludedDocument();
		for(Category category : categories){
			category.removeDocument( document );
		}
		return deleteDocument( id );

	}

	public static boolean finallyDeleteDocumentWithId(String id) throws IOException, SAXException {
		return finallyDeleteDocumentWithId( id, false );
	}

	public static void readAllDocuments() throws IOException, SAXException {
		setDocumentsList(XMLParser.newInstance().parseDocuments());
	}
	// END Documents
	// START Entries
	private static ArrayList<Entry> sEntriesList = new ArrayList<>();
	private static final Map<String, Entry> sEntryMap = new HashMap<>(  );

	public static ArrayList<Entry> getEntriesList() {
		return sEntriesList;
	}

	public static boolean finallyDeleteEntryWithId(String id) throws IOException, SAXException {
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

	public static ArrayList<Entry> getFreeEntries() throws IOException, SAXException {
		ArrayList<Entry> entries = new ArrayList<>();
		for(Entry entry : getEntriesList()){
			ArrayList<Document> documents = entry.getDocumentsInWhichIncludedThisEntry();
			if(documents.size() == 0) {
				entries.add( entry );
			}else{
				boolean isFree = true;
				for(Document document : documents){
					if(document.getCategoriesInWhichIncludedDocument().size() != 0){
						isFree = false;
						break;
					}
				}
				if(isFree)
					entries.add(entry);
			}
		}
		return entries;
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

	public static boolean isExists(String id){
		return sEntryMap.containsKey( id ) || sDocumentMap.containsKey( id ) || sCategoryMap.containsKey( id );
	}

	public static void readAllEntries() throws IOException, SAXException {
		setEntriesList( XMLParser.newInstance().parseEntries() );
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

		return deleteDir( file );
	}

	private static boolean deleteDir(File file){
		File[] files = file.listFiles();
		if(files != null && files.length > 0) {
			for (File subFile : files) {
				if ( subFile.isDirectory() )
					deleteDir( subFile );
				else
					subFile.delete();
			}
		}

		return file.delete();
	}
}
