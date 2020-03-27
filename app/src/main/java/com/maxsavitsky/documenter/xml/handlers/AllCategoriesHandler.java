package com.maxsavitsky.documenter.xml.handlers;

import com.maxsavitsky.documenter.data.types.Category;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

public class AllCategoriesHandler extends DefaultHandler {
	private final ArrayList<Category> mCategories = new ArrayList<>();

	public ArrayList<Category> getCategories() {
		return mCategories;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if(qName.equals("category")) {
			String id = attributes.getValue("id");
			String name = attributes.getValue("name");
			mCategories.add(new Category(id, name));
		}
	}
}
