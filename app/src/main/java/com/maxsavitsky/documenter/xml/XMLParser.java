package com.maxsavitsky.documenter.xml;

import com.maxsavitsky.documenter.EntriesList;
import com.maxsavitsky.documenter.datatypes.Category;
import com.maxsavitsky.documenter.datatypes.Document;
import com.maxsavitsky.documenter.datatypes.Entry;
import com.maxsavitsky.documenter.datatypes.MainData;
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
		File path = new File(Utils.getContext().getExternalFilesDir(null).getPath() + "/categories.xml");
		if(!path.exists()){
			path.createNewFile();
		}else {
			mSAXParser.parse(path, new XMLCategoriesHandler());
		}
		return mCategories;
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

	public ArrayList<Category> parseCategoriesInWhichIncludedDocumentWithId(String id) throws IOException, SAXException {
		File path = new File(Utils.getExternalStoragePath().getPath() + "/documents/" + id + "/included_in.xml");
		XMLCategoriesWithDocumentHandler handler = new XMLCategoriesWithDocumentHandler();
		mSAXParser.parse(path, handler);

		return handler.getCategories();
	}

	class XMLCategoriesWithDocumentHandler extends DefaultHandler{
		private ArrayList<Category> mCategoriesThis;

		ArrayList<Category> getCategories() {
			return mCategories;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			if(qName.equals("category")){
				String id = attributes.getValue("id");
				this.mCategoriesThis.add(new Category(id, MainData.getCategoryWithId(id).getName()));
			}
		}
	}

	// [END Categories_Parser]
	// [START Documents_Parser]

	public ArrayList<Document> parseDocuments() throws IOException, SAXException {
		mDocuments.clear();
		File path = new File(Utils.getContext().getExternalFilesDir(null).getPath() + "/documents.xml");
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
		}
		return mDocuments;
	}

	class XMLDocumentsHandler extends DefaultHandler{
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(qName.equals("document")){
				String id = attributes.getValue("id");
				String name = attributes.getValue("name");
				mDocuments.add(new Document(id, name));
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
}
