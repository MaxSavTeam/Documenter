package com.maxsavitsky.documenter.xml.handlers;

import com.maxsavitsky.documenter.data.types.Document;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class DocumentPropertiesHandler extends DefaultHandler {
	private final Document.Properties mProperties = new Document.Properties();

	public Document.Properties getProperties() {
		return mProperties;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if(qName.equals( "saveLastPos" )){
			mProperties.setSaveLastPos( Boolean.parseBoolean( attributes.getValue("value") ) );
		}
	}
}
