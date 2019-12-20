package com.maxsavitsky.documenter.datatypes;

import androidx.annotation.NonNull;

import com.maxsavitsky.documenter.utils.Utils;

import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Document implements Comparable{
	private String id, name;

	private ArrayList<Entry> mEntries = new ArrayList<>();

	public Document(String id, String name){
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public ArrayList<Entry> getEntries() {
		return mEntries;
	}

	public void addEntry(Entry entry){
		mEntries.add(entry);
	}

	public void addCategoryToIncludedInXml(String categoryId) throws IOException, SAXException {
		ArrayList<Category> categories = MainData.getCategoriesInWhichIncludedDocumentWithId( getId() );
		categories.add( MainData.getCategoryWithId( categoryId ) );
		Utils.saveInWhichCategoriesDocumentWithIdIncludedIn( getId(), categories );
	}

	public void removeCategoryFromIncludedXml( String categoryId) throws IOException, SAXException {
		ArrayList<Category> categories = MainData.getCategoriesInWhichIncludedDocumentWithId( getId() );
		categories.remove( MainData.getCategoryWithId( categoryId ) );
		Utils.saveInWhichCategoriesDocumentWithIdIncludedIn( getId(), categories );
	}

	public void saveInWhichCategoriesDocumentWithIdIncludedIn(ArrayList<Category> categories) throws IOException {
		File file = new File( Utils.getExternalStoragePath().getPath() + "/documents/" + getId() );
		if(!file.exists()){
			file.mkdir();
		}
		file = new File( file.getPath() + "/included_in.xml" );
		if(!file.exists())
			file.createNewFile();

		FileWriter fr = new FileWriter( file, false );
		fr.write( Utils.xmlHeader );
		fr.append( "<categories>\n" );
		for(Category category : categories){
			fr.append( "<category id=\"" + category.getId() + "\" />\n" );
		}
		fr.append( "</categories>" );
		fr.flush();
		fr.close();
	}

	@Override
	public int compareTo(@NotNull Object o) {
		Document document = (Document) o;
		return getName().compareTo( document.getName() );
	}

	@NonNull
	@Override
	public String toString() {
		return "id=\"" + this.id + "\" name=\"" + this.name + "\"";
	}
}
