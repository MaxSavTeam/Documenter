package com.maxsavitsky.documenter.xml.handlers;

import com.maxsavitsky.documenter.data.Info;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class InfoHandler extends DefaultHandler {
	private final Info mInfo = new Info();

	public Info getInfo() {
		return mInfo;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if(qName.equals( "timestamp" )){
			int timestamp = Integer.parseInt( attributes.getValue( "value" ) );
			mInfo.setTimeStamp( timestamp );
		}
	}
}
