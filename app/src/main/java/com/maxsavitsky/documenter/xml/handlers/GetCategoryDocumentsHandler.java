package com.maxsavitsky.documenter.xml.handlers;

import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.types.Document;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

public class GetCategoryDocumentsHandler extends DefaultHandler {
	private final ArrayList<Document> documents = new ArrayList<>();

	public ArrayList<Document> getDocuments() {
		return documents;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if(qName.equals("document")){
			String id = attributes.getValue("id");
			Document document = MainData.getDocumentWithId( id );
			if(document != null) {
				documents.add( new Document( id, document.getName() ) );
			}
		}
	}
}
