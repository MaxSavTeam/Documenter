package com.maxsavitsky.documenter.xml.handlers;

import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.types.Entry;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

public class GetDocumentsEntriesHandler extends DefaultHandler {
	private final ArrayList<Entry> entries = new ArrayList<>();
	public GetDocumentsEntriesHandler() {
	}

	public ArrayList<Entry> getEntries() {
		return entries;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if(qName != null && qName.equals( "entry" )){
			String id = attributes.getValue( "id" );
			Entry loadedEntry = MainData.getEntryWithId( id );
			if(loadedEntry != null) {
				Entry entry = new Entry( id, loadedEntry.getName() );
				entries.add( entry );
			}
		}
	}
}
