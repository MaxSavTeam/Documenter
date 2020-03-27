package com.maxsavitsky.documenter.xml.handlers;

import com.maxsavitsky.documenter.data.types.Entry;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class PropertyHandler extends DefaultHandler {
	private final Entry.Properties mProperties = new Entry.Properties();

	public Entry.Properties getProperties() {
		return mProperties;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		switch ( qName ) {
			case "textSize":
				mProperties.setTextSize( Integer.parseInt( attributes.getValue( "value" ) ) );
				break;
			case "bgColor":
				mProperties.setBgColor( Integer.parseInt( attributes.getValue( "value" ) ) );
				break;
			case "textColor":
				mProperties.setTextColor( Integer.parseInt( attributes.getValue( "value" ) ) );
				break;
			case "scrollPosition":
				mProperties.setScrollPosition( Integer.parseInt( attributes.getValue( "value" ) ) );
				break;
			case "textAlignment":
				mProperties.setTextAlignment( Integer.parseInt( attributes.getValue( "value" ) ) );
				break;
			case "saveLastPos":
				mProperties.setSaveLastPos( Boolean.parseBoolean( attributes.getValue( "value" ) ) );
				break;
			case "defaultColor":
				mProperties.setDefaultTextColor( Integer.parseInt( attributes.getValue( "value" ) ) );
				break;
		}
	}
}
