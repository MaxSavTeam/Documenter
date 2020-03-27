package com.maxsavitsky.documenter.xml;

import com.maxsavitsky.documenter.data.types.Category;
import com.maxsavitsky.documenter.data.types.Document;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.handlers.AllCategoriesHandler;
import com.maxsavitsky.documenter.xml.handlers.AllDocumentsHandler;
import com.maxsavitsky.documenter.xml.handlers.AllEntriesHandler;
import com.maxsavitsky.documenter.xml.handlers.CategoryPropertyHandler;
import com.maxsavitsky.documenter.xml.handlers.DocumentPropertiesHandler;
import com.maxsavitsky.documenter.xml.handlers.DocumentsInWhichIncludedEntryHandler;
import com.maxsavitsky.documenter.xml.handlers.GetCategoriesWithDocumentHandler;
import com.maxsavitsky.documenter.xml.handlers.GetCategoryDocumentsHandler;
import com.maxsavitsky.documenter.xml.handlers.GetDocumentsEntriesHandler;
import com.maxsavitsky.documenter.xml.handlers.InfoHandler;
import com.maxsavitsky.documenter.xml.handlers.PropertyHandler;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class XMLParser {
	private SAXParser mSAXParser;

	private XMLParser() {
		try {
			mSAXParser = SAXParserFactory.newInstance().newSAXParser();
		} catch (ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
	}

	public static XMLParser newInstance(){
		return new XMLParser();
	}

	// [START categories[
	public ArrayList<Document> parseCategoryWithId(String id) throws IOException, SAXException {
		File path = new File(Utils.getExternalStoragePath().getPath() + "/categories/" + id + "/" + id + ".xml");
		GetCategoryDocumentsHandler handler = new GetCategoryDocumentsHandler();
		mSAXParser.parse(path, handler);

		return handler.getDocuments();
	}

	// [START documents]
	public ArrayList<Entry> parseDocumentWithId(String id) throws SAXException, IOException {
		if(id.equals( "free_entries" )){
			return MainData.getFreeEntries();
		}
		File path = new File( Utils.getExternalStoragePath().getPath() + "/documents/" + id + "/" + id + ".xml" );
		GetDocumentsEntriesHandler handler = new GetDocumentsEntriesHandler();
		mSAXParser.parse( path, handler );
		return handler.getEntries();
	}

	public ArrayList<Document> getDocumentsInWhichIncludedEntryWithId(String id) throws IOException, SAXException {
		File path = new File( Utils.getExternalStoragePath().getPath() + "/entries/" + id + "/included_in.xml" );
		DocumentsInWhichIncludedEntryHandler handler = new DocumentsInWhichIncludedEntryHandler();
		mSAXParser.parse( path, handler );
		return handler.getDocuments();
	}

	// [START Categories_Parser]
	public ArrayList<Category> parseCategories() throws SAXException, IOException {
		ArrayList<Category> categories = new ArrayList<>();
		File path = new File(Utils.getExternalStoragePath().getPath() + "/categories.xml");
		if(!path.exists()){
			path.createNewFile();
			FileWriter fr = new FileWriter(path, false);
			fr.write(Utils.xmlHeader);
			fr.append("<categories>\n</categories>");
			fr.flush();
			fr.close();
		}else {
			AllCategoriesHandler handler = new AllCategoriesHandler();
			mSAXParser.parse(path, handler);
			categories = handler.getCategories();
			for(int i = 0;  i < categories.size(); i++){
				Category category = categories.get( i );
				path = new File( Utils.getExternalStoragePath().getPath() + "/categories/" + category.getId() + "/info.xml" );
				InfoHandler infoHandler = new InfoHandler();
				mSAXParser.parse( path,  infoHandler );
				categories.get( i ).setInfo( infoHandler.getInfo() );
			}
		}
		return categories;
	}

	public Category.Properties parseCategoryProperties(String id) throws IOException, SAXException {
		File path = new File( Utils.getExternalStoragePath().getPath() + "/categories/" + id + "/properties.xml" );
		if(!path.exists()){
			return new Category.Properties();
		}
		CategoryPropertyHandler handler = new CategoryPropertyHandler();
		mSAXParser.parse( path, handler );

		return handler.getProperties();
	}

	public ArrayList<Category> parseCategoriesInWhichIncludedDocumentWithId(String id) throws IOException, SAXException {
		File path = new File(Utils.getExternalStoragePath().getPath() + "/documents/" + id + "/included_in.xml");
		GetCategoriesWithDocumentHandler handler = new GetCategoriesWithDocumentHandler();
		mSAXParser.parse(path, handler);

		return handler.getCategories();
	}

	// [END Categories_Parser]
	// [START Documents_Parser]

	public ArrayList<Document> parseDocuments() throws IOException, SAXException {
		ArrayList<Document> documents = new ArrayList<>();
		File path = new File(Utils.getExternalStoragePath().getPath() + "/documents.xml");
		if(!path.exists()){
			path.createNewFile();
			try{
				FileWriter fr = new FileWriter(path, false);
				fr.write(Utils.xmlHeader);
				fr.append("<documents>\n</documents>");
				fr.flush();
				fr.close();
			}catch (Exception e){
				e.printStackTrace();
			}
		}else {
			AllDocumentsHandler handler = new AllDocumentsHandler();
			mSAXParser.parse(path, handler);
			documents = handler.getDocuments();
			for(int i = 0; i < documents.size(); i++){
				Document document = documents.get( i );
				File file = new File( Utils.getExternalStoragePath().getPath() + "/documents/" + document.getId() + "/info.xml" );
				InfoHandler infoHandler = new InfoHandler();
				mSAXParser.parse( file, infoHandler );
				documents.get( i ).setInfo( infoHandler.getInfo() );
			}
		}
		return documents;
	}

	public Document.Properties parseDocumentProperties(String id) throws IOException, SAXException {
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries/" + id + "/properties.xml" );
		if(!file.exists())
			return new Document.Properties();

		DocumentPropertiesHandler handler = new DocumentPropertiesHandler();
		mSAXParser.parse( file, handler );

		return handler.getProperties();
	}

	// [END Documents_Parser]

	// [START Entries_Parser]
	public ArrayList<Entry> parseEntries() throws IOException, SAXException {
		ArrayList<Entry> entries = new ArrayList<>();
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries.xml" );
		if(!file.exists()){
			file.createNewFile();
			try{
				FileWriter fr = new FileWriter( file, false );
				fr.write( Utils.xmlHeader );
				fr.write( "<entries>\n</entries>" );
				fr.flush();
				fr.close();
			}catch (Exception e){
				e.printStackTrace();
			}
		}else{

			AllEntriesHandler handler = new AllEntriesHandler();
			mSAXParser.parse( file, handler );
			entries = handler.getEntries();
			for(int i = 0; i < entries.size(); i++){
				Entry entry = entries.get( i );
				File path = new File( entry.getPathDir() + "info.xml" );
				InfoHandler infoHandler = new InfoHandler();
				mSAXParser.parse( path,  infoHandler);
				entries.get( i ).setInfo( infoHandler.getInfo() );
			}
		}
		return entries;
	}

	public Entry.Properties parseEntryProperties(String entryId) throws IOException, SAXException {
		File file = new File( MainData.getEntryWithId( entryId ).getPathDir() + "properties.xml" );

		PropertyHandler handler = new PropertyHandler();
		mSAXParser.parse( file, handler );
		return handler.getProperties();
	}
}
