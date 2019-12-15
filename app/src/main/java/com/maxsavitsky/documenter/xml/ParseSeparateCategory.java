package com.maxsavitsky.documenter.xml;

import com.maxsavitsky.documenter.datatypes.Document;
import com.maxsavitsky.documenter.utils.Utils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class ParseSeparateCategory {
	private static ArrayList<Document> sDocuments = new ArrayList<>();

	public static ArrayList<Document> parseCategoryWithId(String id) throws ParserConfigurationException, SAXException, IOException {
		sDocuments.clear();
		File path = new File(Utils.getContext().getExternalFilesDir(null).getPath() + "/categories/" + id + ".xml");
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
				String name = attributes.getValue("name");
				sDocuments.add(new Document(id, name));
			}
		}
	}
}
