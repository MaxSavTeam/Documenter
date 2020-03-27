package com.maxsavitsky.documenter.xml.handlers;

import com.maxsavitsky.documenter.data.types.Entry;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

public class AllEntriesHandler extends DefaultHandler {
	private final ArrayList<Entry> mEntries = new ArrayList<>();

	public ArrayList<Entry> getEntries() {
		return mEntries;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if(qName.equals( "entry" )){
			String id = attributes.getValue( "id" );
			String name = attributes.getValue( "name" );
			mEntries.add( new Entry( id, name ) );
		}
	}
}
