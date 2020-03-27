package com.maxsavitsky.documenter.xml.handlers;

import com.maxsavitsky.documenter.data.types.Category;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CategoryPropertyHandler extends DefaultHandler {
	private final Category.Properties mProperties = new Category.Properties();
	public CategoryPropertyHandler() {
	}

	public Category.Properties getProperties() {
		return mProperties;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if(qName.equals( "saveLastPos" )){
			mProperties.setSaveLastPos( Boolean.parseBoolean( attributes.getValue( "value" ) ) );
		}
	}
}
