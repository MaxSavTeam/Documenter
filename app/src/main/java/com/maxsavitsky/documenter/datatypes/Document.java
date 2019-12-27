package com.maxsavitsky.documenter.datatypes;

import androidx.annotation.NonNull;

import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.ParseSeparate;
import com.maxsavitsky.documenter.xml.XMLParser;

import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class Document extends Type implements Comparable{
	private String id, name;
	private String pathDir;

	private ArrayList<Entry> mEntries = new ArrayList<>();
	private Info mInfo = new Info();

	public Document(String id, String name){
		this.id = id;
		this.name = name;
		this.pathDir = Utils.getExternalStoragePath().getPath() + "/documents/" + id;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setInfo(Info info) {
		mInfo = info;
	}

	public Info getInfo() {
		return mInfo;
	}

	public void setAndSaveInfo(Info info) throws IOException {
		setInfo( info );

		File file = new File( Utils.getExternalStoragePath().getPath() + "/documents/" );
		if(!file.exists())
			file.mkdir();

		file = new File( file.getPath() + "/" + getId() );
		if(!file.exists())
			file.mkdir();

		file = new File( file.getPath() + "/info.xml" );
		if(!file.exists())
			file.createNewFile();

		FileWriter fr = new FileWriter( file, false );
		fr.write( Utils.xmlHeader );
		fr.append( "<info>\n" );
		fr.append( "<timestamp value=\"" + Integer.toString( info.getTimeStamp() ) + "\" />" );
		fr.append( "</info>" );
		fr.flush();
		fr.close();
	}

	public ArrayList<Entry> getEntries() {
		return mEntries;
	}

	public void addEntry(Entry entry) throws Exception{
		mEntries = ParseSeparate.parseDocumentWithId( id );
		mEntries.add(entry);
		Utils.saveDocumentEntries( id, mEntries );
		/*FileWriter fr = new FileWriter( pathDir + "/" + id + ".xml" );
		fr.write( Utils.xmlHeader );
		fr.append( "<entries>\n" );
		for(Entry entry1 : mEntries){
			fr.append( "<entry id=\"" + entry1.getId() + "\" />\n" );
		}
		fr.append( "</entries>" );
		fr.flush();
		fr.close();*/
	}

	public void removeEntry(Entry entry) throws Exception{
		mEntries = ParseSeparate.parseDocumentWithId( id );
		for(int i = 0; i < mEntries.size(); i++){
			if(mEntries.get( i ).getId().equals( entry.getId() )){
				mEntries.remove( i );
				break;
			}
		}
		Utils.saveDocumentEntries( id, mEntries );
	}

	public void addCategoryToIncludedInXml(String categoryId) throws Exception {
		ArrayList<Category> categories = MainData.getCategoriesInWhichIncludedDocumentWithId( getId() );
		categories.add( MainData.getCategoryWithId( categoryId ) );
		saveInWhichCategoriesDocumentWithIdIncludedIn( categories );
	}

	public void removeCategoryFromIncludedXml( String categoryId) throws Exception {
		ArrayList<Category> categories = getCategoriesInWhichIncludedDocument();
		categories.remove( MainData.getCategoryWithId( categoryId ) );
		saveInWhichCategoriesDocumentWithIdIncludedIn( categories );
	}

	public ArrayList<Category> getCategoriesInWhichIncludedDocument() throws Exception{
		File file = new File( Utils.getExternalStoragePath().getPath() + "/documents" );
		if(!file.exists())
			return new ArrayList<>(  );

		file = new File( file.getPath() + "/" + id );
		if(!file.exists())
			return new ArrayList<>(  );

		file = new File( file.getPath() + "/included_in.xml" );
		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();

		final ArrayList<Category> categories = new ArrayList<>(  );
		class MyParser extends DefaultHandler{
			@Override
			public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
				if(qName.equals( "category" )){
					categories.add( MainData.getCategoryWithId( attributes.getValue( "id" ) ) );
				}
			}
		}

		saxParser.parse( file, new MyParser() );
		return categories;
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
