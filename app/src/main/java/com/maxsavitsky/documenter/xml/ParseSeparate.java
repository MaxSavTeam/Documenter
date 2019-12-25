package com.maxsavitsky.documenter.xml;

import com.maxsavitsky.documenter.datatypes.Document;
import com.maxsavitsky.documenter.datatypes.Entry;
import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.utils.Utils;

import org.xml.sax.Attributes;
import org.xml.sax.DocumentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class ParseSeparate {
	private static ArrayList<Document> sDocuments = new ArrayList<>();
	private static ArrayList<Entry> sEntries = new ArrayList<>();

	// [START categories[
	public static ArrayList<Document> parseCategoryWithId(String id) throws ParserConfigurationException, SAXException, IOException {
		sDocuments.clear();
		File path = new File(Utils.getExternalStoragePath().getPath() + "/categories/" + id + "/" + id + ".xml");
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		SAXParser parser = saxParserFactory.newSAXParser();
		parser.parse(path, new CategoryHandler());

		return sDocuments;
	}

	static class CategoryHandler extends DefaultHandler {
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(qName.equals("document")){
				String id = attributes.getValue("id");
				sDocuments.add(new Document(id, MainData.getDocumentWithId( id ).getName() ));
			}
		}
	}
	// [END categories]

	// [START documents]
	public static ArrayList<Entry> parseDocumentWithId(String id) throws ParserConfigurationException, SAXException, IOException {
		sEntries.clear();
		File path = new File( Utils.getExternalStoragePath().getPath() + "/documents/" + id + "/" + id + ".xml" );
		SAXParserFactory.newInstance().newSAXParser().parse( path, new DocumentHandler() );
		return sEntries;
	}

	static class DocumentHandler extends DefaultHandler{
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if(qName.equals( "entry" )){
				String id = attributes.getValue( "id" );
				Entry entry = new Entry( id, MainData.getEntryWithId(id).getName() );
				sEntries.add( entry );
			}
		}
	}

	public static ArrayList<Document> getDocumentsInWhichIncludedEntryWithId(String id) throws ParserConfigurationException, SAXException, IOException {
		File path = new File( Utils.getExternalStoragePath().getPath() + "/entries/" + id + "/included_in.xml" );
		DocumentsInWhichIncludedEntryHandler handler = new DocumentsInWhichIncludedEntryHandler();
		SAXParserFactory.newInstance().newSAXParser().parse( path, handler );
		return handler.getDocuments();
	}
	static class DocumentsInWhichIncludedEntryHandler extends DefaultHandler{
		private ArrayList<Document> mDocuments = new ArrayList<>(  );

		public ArrayList<Document> getDocuments() {
			return mDocuments;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			if(qName.equals( "document" )){
				String id = attributes.getValue( "id" );
				mDocuments.add( new Document( id, MainData.getDocumentWithId( id ).getName() ) );
			}
		}
	}
	// [END documents]
}
