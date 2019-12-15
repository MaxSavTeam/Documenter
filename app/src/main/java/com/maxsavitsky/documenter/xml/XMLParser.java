package com.maxsavitsky.documenter.xml;

import android.content.Context;

import com.maxsavitsky.documenter.datatypes.Category;
import com.maxsavitsky.documenter.datatypes.Document;
import com.maxsavitsky.documenter.datatypes.MainData;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class XMLParser {
	private ArrayList<Category> mCategories = new ArrayList<>();
	private ArrayList<Document> mDocuments = new ArrayList<>();
	private SAXParser mSAXParser;

	public XMLParser() {
		try {
			mSAXParser = SAXParserFactory.newInstance().newSAXParser();
		} catch (ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<Category> parseCategories(Context context) throws SAXException, IOException {
		mCategories.clear();
		mSAXParser.parse(new File(context.getApplicationContext().getExternalFilesDir(null).getPath() + "/categories.xml"), new XMLCategoriesHandler());
		MainData.setCategoriesList(mCategories);

		return mCategories;
	}

	public ArrayList<Document> parseDocuments(Context context) throws IOException, SAXException {
		mDocuments.clear();
		File path = new File(context.getApplicationContext().getExternalFilesDir(null).getPath() + "/documents.xml");
		mSAXParser.parse(path, new XMLDocumentsHandler());
		MainData.setDocumentsList(mDocuments);

		return mDocuments;
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
}
