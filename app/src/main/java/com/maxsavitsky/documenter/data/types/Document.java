package com.maxsavitsky.documenter.data.types;

import android.util.Log;

import androidx.annotation.NonNull;

import com.maxsavitsky.documenter.data.Info;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.XMLParser;

import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class Document extends Type implements Comparable{
	private final String id;
	private final String name;
	private Document.Properties mProperties;

	private ArrayList<Entry> mEntries = null;
	private Info mInfo = new Info();

	public Document(String id, String name){
		this.id = id;
		this.name = name;
		String pathDir = Utils.getExternalStoragePath().getPath() + "/documents/" + id;
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
		fr.append( "\t<timestamp value=\"" ).append( String.valueOf( info.getTimeStamp() ) ).append( "\" />" );
		fr.append( "</info>" );
		fr.flush();
		fr.close();
	}

	public void applySaveLastPosState(boolean state) throws IOException, SAXException {
		for(Entry entry : getEntries()){
			entry.applySaveLastPos( state );
		}

		if(mProperties == null) {
			mProperties = XMLParser.newInstance().parseDocumentProperties( id );
		}
		mProperties.setSaveLastPos( state );
		saveProperties();
	}

	public ArrayList<Entry> getEntries() {
		if(mEntries == null){
			try {
				mEntries = XMLParser.newInstance().parseDocumentWithId( getId() );
			} catch (SAXException | IOException e) {
				e.printStackTrace();
				Log.e( "Document " + getId(), e.toString() );
			}
		}
		return mEntries;
	}

	public void addEntry(Entry entry) throws Exception{
		mEntries = XMLParser.newInstance().parseDocumentWithId( id );
		mEntries.add(entry);
		Utils.saveDocumentEntries( id, mEntries );
	}

	public void removeEntry(Entry entry) throws IOException, SAXException {
		mEntries = XMLParser.newInstance().parseDocumentWithId( id );
		for(int i = 0; i < mEntries.size(); i++){
			if(mEntries.get( i ).getId().equals( entry.getId() )){
				mEntries.remove( i );
				break;
			}
		}
		Utils.saveDocumentEntries( id, mEntries );
	}

	public void addCategoryToIncludedInXml(String categoryId) throws IOException, SAXException {
		ArrayList<Category> categories = MainData.getCategoriesInWhichIncludedDocumentWithId( getId() );
		categories.add( MainData.getCategoryWithId( categoryId ) );
		saveInWhichCategoriesDocumentWithIdIncludedIn( categories );
	}

	public void removeCategoryFromIncludedXml( String categoryId) throws IOException, SAXException {
		ArrayList<Category> categories = getCategoriesInWhichIncludedDocument();
		categories.remove( MainData.getCategoryWithId( categoryId ) );
		saveInWhichCategoriesDocumentWithIdIncludedIn( categories );
	}

	public ArrayList<Category> getCategoriesInWhichIncludedDocument() throws IOException, SAXException {
		return XMLParser.newInstance().parseCategoriesInWhichIncludedDocumentWithId( id );
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

	public Properties readProperties() throws IOException, SAXException {
		this.mProperties = XMLParser.newInstance().parseDocumentProperties( id );
		return mProperties;
	}

	public Properties getProperties() {
		return mProperties;
	}

	public void saveProperties() throws IOException {
		File file = new File(Utils.getExternalStoragePath().getPath() + "/documents/" + id + "/properties.xml");
		if(!file.exists())
			file.createNewFile();

		FileWriter fw = new FileWriter(file);
		fw.write( Utils.xmlHeader );
		fw.append( "<properties>\n" )
				.append( "\t<saveLastPos value=\"" ).append( Boolean.toString( mProperties.isSaveLastPos() ) ).append( "\" /> \n" )
				.append( "</properties>" );
		fw.flush();
		fw.close();
	}

	public static class Properties{
		private boolean mSaveLastPos = true;

		public Properties() {}

		public boolean isSaveLastPos() {
			return mSaveLastPos;
		}

		public void setSaveLastPos(boolean saveLastPos) {
			mSaveLastPos = saveLastPos;
		}
	}
}
