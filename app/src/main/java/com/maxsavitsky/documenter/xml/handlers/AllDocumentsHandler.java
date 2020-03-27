package com.maxsavitsky.documenter.xml.handlers;

import com.maxsavitsky.documenter.data.types.Document;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

public class AllDocumentsHandler extends DefaultHandler {
	private final ArrayList<Document> mDocuments = new ArrayList<>();

	public AllDocumentsHandler() {
	}

	public ArrayList<Document> getDocuments() {
		return mDocuments;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if(qName.equals("document")){
			String id = attributes.getValue("id");
			String name = attributes.getValue("name");
			mDocuments.add(new Document(id, name));
		}
	}
}
