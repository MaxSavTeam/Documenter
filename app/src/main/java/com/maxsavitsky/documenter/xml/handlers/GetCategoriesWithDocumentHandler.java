package com.maxsavitsky.documenter.xml.handlers;

import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.types.Category;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

public class GetCategoriesWithDocumentHandler extends DefaultHandler {
	private final ArrayList<Category> mCategoriesThis = new ArrayList<>(  );

	public ArrayList<Category> getCategories() {
		return mCategoriesThis;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes)  throws SAXException {
		if(qName.equals("category")){
			String id = attributes.getValue("id");
			Category category = MainData.getCategoryWithId( id );
			if(category != null)
				mCategoriesThis.add(category);
		}
	}
}
