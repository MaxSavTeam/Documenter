package com.maxsavitsky.documenter.xml;

import com.maxsavitsky.documenter.data.types.Category;
import com.maxsavitsky.documenter.data.types.Document;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.data.Info;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.utils.Utils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class XMLParser {
	private ArrayList<Category> mCategories = new ArrayList<>();
	private ArrayList<Document> mDocuments = new ArrayList<>();
	private ArrayList<Entry> mEntries = new ArrayList<>();
	private SAXParser mSAXParser;

	public XMLParser() {
		try {
			mSAXParser = SAXParserFactory.newInstance().newSAXParser();
		} catch (ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
	}
	// [START Categories_Parser]
	public ArrayList<Category> parseCategories() throws SAXException, IOException {
		mCategories.clear();
		File path = new File(Utils.getExternalStoragePath().getPath() + "/categories.xml");
		if(!path.exists()){
			path.createNewFile();
			FileWriter fr = new FileWriter(path, false);
			fr.write(Utils.xmlHeader);
			fr.append("<categories>\n</categories>");
			fr.flush();
			fr.close();
		}else {
			mSAXParser.parse(path, new XMLCategoriesHandler());
			for(int i = 0;  i < mCategories.size(); i++){
				Category category = mCategories.get( i );
				path = new File( Utils.getExternalStoragePath().getPath() + "/categories/" + category.getId() + "/info.xml" );
				InfoHandler infoHandler = new InfoHandler();
				mSAXParser.parse( path,  infoHandler );
				mCategories.get( i ).setInfo( infoHandler.mInfo );
			}
		}
		return mCategories;
	}

	class InfoHandler extends DefaultHandler{
		Info mInfo = new Info();

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(qName.equals( "timestamp" )){
				int timestamp = Integer.parseInt( attributes.getValue( "value" ) );
				mInfo.setTimeStamp( timestamp );
			}
		}
	}

	class XMLCategoriesHandler extends DefaultHandler {
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(qName.equals("category")) {
				String id = attributes.getValue("id");
				String name = attributes.getValue("name");
				mCategories.add(new Category(id, name));
			}
		}
	}

	private Category.Properties mCategoryProperties = new Category.Properties();

	public Category.Properties parseCategoryProperties(String id) throws IOException, SAXException {
		File path = new File( Utils.getExternalStoragePath().getPath() + "/categories/" + id + "/properties.xml" );
		if(!path.exists()){
			return new Category.Properties();
		}
		mSAXParser.parse( path, new CategoryPropertyHandler() );

		return mCategoryProperties;
	}

	private class CategoryPropertyHandler extends DefaultHandler{
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(qName.equals( "saveLastPos" )){
				mCategoryProperties.setSaveLastPos( Boolean.parseBoolean( attributes.getValue( "value" ) ) );
			}
		}
	}

	public ArrayList<Category> parseCategoriesInWhichIncludedDocumentWithId(String id) throws Exception {
		File path = new File(Utils.getExternalStoragePath().getPath() + "/documents/" + id + "/included_in.xml");
		XMLCategoriesWithDocumentHandler handler = new XMLCategoriesWithDocumentHandler();
		mSAXParser.parse(path, handler);

		return handler.getCategories();
	}

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	static class XMLCategoriesWithDocumentHandler extends DefaultHandler{
		private ArrayList<Category> mCategoriesThis = new ArrayList<>(  );

		ArrayList<Category> getCategories() {
			return mCategoriesThis;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)  throws SAXException{
			if(qName.equals("category")){
				String id = attributes.getValue("id");
				//try {
					this.mCategoriesThis.add(new Category(id, MainData.getCategoryWithId(id).getName()));
				/*} catch (NullPointerException e) {
					SAXException saxException = new SAXException( "NPE: category id=" + id + " not found in list" );
					saxException.printStackTrace();
					throw saxException;
				}*/
			}
		}
	}

	// [END Categories_Parser]
	// [START Documents_Parser]

	public ArrayList<Document> parseDocuments() throws IOException, SAXException {
		mDocuments.clear();
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
			mSAXParser.parse(path, new XMLDocumentsHandler());
			for(int i = 0; i < mDocuments.size(); i++){
				Document document = mDocuments.get( i );
				File file = new File( Utils.getExternalStoragePath().getPath() + "/documents/" + document.getId() + "/info.xml" );
				InfoHandler infoHandler = new InfoHandler();
				mSAXParser.parse( file, infoHandler );
				mDocuments.get( i ).setInfo( infoHandler.mInfo );
			}
		}
		return mDocuments;
	}

	private class XMLDocumentsHandler extends DefaultHandler{
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(qName.equals("document")){
				String id = attributes.getValue("id");
				String name = attributes.getValue("name");
				mDocuments.add(new Document(id, name));
			}
		}
	}

	private Document.Properties mDocumentProperties = new Document.Properties();

	public Document.Properties parseDocumentProperties(String id) throws IOException, SAXException {
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries/" + id + "/properties.xml" );
		if(!file.exists())
			return new Document.Properties();

		mSAXParser.parse( file, new DocumentPropertiesHandler() );

		return mDocumentProperties;
	}

	private class DocumentPropertiesHandler extends DefaultHandler{
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(qName.equals( "saveLastPos" )){
				mDocumentProperties.setSaveLastPos( Boolean.parseBoolean( attributes.getValue("value") ) );
			}
		}
	}
	// [END Documents_Parser]

	// [START Entries_Parser]
	public ArrayList<Entry> parseEntries() throws IOException, SAXException {
		mEntries.clear();
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
			mSAXParser.parse( file, new EntriesHandler() );
			for(int i = 0; i < mEntries.size(); i++){
				Entry entry = mEntries.get( i );
				File path = new File( entry.getPathDir() + "info.xml" );
				InfoHandler infoHandler = new InfoHandler();
				mSAXParser.parse( path,  infoHandler);
				mEntries.get( i ).setInfo( infoHandler.mInfo );
			}
		}
		return mEntries;
	}

	class EntriesHandler extends DefaultHandler{
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(qName.equals( "entry" )){
				String id = attributes.getValue( "id" );
				String name = attributes.getValue( "name" );
				mEntries.add( new Entry( id, name ) );
			}
		}
	}

	private Entry.Properties mEntryProperties = new Entry.Properties();

	public Entry.Properties parseEntryProperties(String entryId) throws IOException {
		File file = new File( MainData.getEntryWithId( entryId ).getPathDir() + "properties.xml" );
		try {
			mSAXParser.parse( file, new PropertyHandler() );
		}catch (SAXException e){
			e.printStackTrace();
			return new Entry.Properties();
		}
		return mEntryProperties;
	}

	class PropertyHandler extends DefaultHandler{
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			switch ( qName ) {
				case "textSize":
					mEntryProperties.setTextSize( Integer.parseInt( attributes.getValue( "value" ) ) );
					break;
				case "bgColor":
					mEntryProperties.setBgColor( Integer.parseInt( attributes.getValue( "value" ) ) );
					break;
				case "textColor":
					mEntryProperties.setTextColor( Integer.parseInt( attributes.getValue( "value" ) ) );
					break;
				case "scrollPosition":
					mEntryProperties.setScrollPosition( Integer.parseInt( attributes.getValue( "value" ) ) );
					break;
				case "textAlignment":
					mEntryProperties.setTextAlignment( Integer.parseInt( attributes.getValue( "value" ) ) );
					break;
				case "saveLastPos":
					mEntryProperties.setSaveLastPos( Boolean.parseBoolean( attributes.getValue( "value" ) ) );
					break;
				case "defaultColor":
					mEntryProperties.setDefaultTextColor( Integer.parseInt( attributes.getValue( "value" ) ) );
			}
		}
	}
}
