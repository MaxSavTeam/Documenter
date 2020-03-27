package com.maxsavitsky.documenter.xml.handlers;

import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.types.Document;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

public class DocumentsInWhichIncludedEntryHandler extends DefaultHandler {
	private final ArrayList<Document> mDocuments = new ArrayList<>(  );

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
