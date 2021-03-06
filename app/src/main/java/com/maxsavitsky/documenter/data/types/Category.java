package com.maxsavitsky.documenter.data.types;

import androidx.annotation.NonNull;

import com.maxsavitsky.documenter.data.Info;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.XMLParser;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Category extends Type {
	private String mId;
	private String mName;
	private Properties mProperties;

	private ArrayList<Document> mDocuments = null;

	private Info mInfo = new Info();

	protected Category(){}

	public Info getInfo() {
		return mInfo;
	}

	public void setInfo(Info info) {
		mInfo = info;
	}

	public void setAndSaveInfo(Info info) throws IOException {
		setInfo( info );

		File file = new File( Utils.getExternalStoragePath().getPath() + "/categories/" );
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
		fr.append( "<timestamp value=\"" ).append( String.valueOf( info.getTimeStamp() ) ).append( "\" />" );
		fr.append( "</info>" );
		fr.flush();
		fr.close();
	}

	public ArrayList<Document> getDocuments() {
		ArrayList<Document> documents = null;
		try {
			documents = XMLParser.newInstance().parseCategoryWithId( mId );
		} catch (IOException | SAXException e) {
			e.printStackTrace();
		}
		return documents;
	}

	public void addDocument(Document document){
		mDocuments.add(document);
	}

	public void removeDocumentWithId(String documentId) throws Exception{
		ArrayList<Document> documents = XMLParser.newInstance().parseCategoryWithId( getId() );
		documents.remove( MainData.getDocumentWithId( documentId ) );
		Utils.saveCategoryDocuments( getId(), documents );
	}

	public void removeDocument(Document document) throws IOException, SAXException {
		ArrayList<Document> documents = XMLParser.newInstance().parseCategoryWithId( getId() );
		//documents.remove( document );
		for(int i = 0; i < documents.size(); i++){
			if(documents.get( i ).getId().equals( document.getId() )){
				documents.remove( i );
				break;
			}
		}
		Utils.saveCategoryDocuments( getId(), documents );
	}

	public Document getDocumentWithId(String id){
		for(int i = 0; i < mDocuments.size(); i++){
			if(mDocuments.get(i).getId().equals(id)){
				return mDocuments.get(i);
			}
		}
		return null;
	}

	public Category(String id, String name){
		this.mId = id;
		this.mName = name;
	}

	@Override
	public String getId() {
		return mId;
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public String getType() {
		return "Category";
	}

	@NonNull
	@Override
	public String toString() {
		return "id=\"" + getId() + "\" name=\"" + getName() + "\"";
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Category category = (Category) o;

		return mId.equals( category.getId() );
	}

	@Override
	public int hashCode() {
		return mId != null ? mId.hashCode() : 0;
	}

	public Properties readProperties() throws IOException, SAXException {
		this.mProperties = XMLParser.newInstance().parseCategoryProperties( getId() );
		return this.mProperties;
	}

	public Properties getProperties() {
		return mProperties;
	}

	public void applySaveLastPos(boolean state) throws IOException, SAXException {
		mProperties.setSaveLastPos( state );
		saveProperties();
		for(Document document : getDocuments()){
			document.applySaveLastPosState( state );
		}
	}

	public void saveProperties() throws IOException {
		File path = new File( Utils.getExternalStoragePath().getPath() + "/categories/" + getId() + "/properties.xml" );
		if(!path.exists())
			path.createNewFile();
		FileWriter fw = new FileWriter( path );
		fw.write( Utils.xmlHeader );
		fw.append( "<properties>\n" );
		fw.append( "\t<saveLastPos value=\"" ).append( Boolean.toString( this.mProperties.isSaveLastPos() ) ).append( "\" />\n" );
		fw.append( "</properties>" );
		fw.flush();
		fw.close();
	}

	public static class Properties{
		private boolean mSaveLastPos = true;

		public Properties() {
		}

		public boolean isSaveLastPos() {
			return mSaveLastPos;
		}

		public void setSaveLastPos(boolean saveLastPos) {
			mSaveLastPos = saveLastPos;
		}
	}
}
