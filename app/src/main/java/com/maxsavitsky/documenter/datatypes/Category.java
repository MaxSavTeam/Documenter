package com.maxsavitsky.documenter.datatypes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.ParseSeparate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Category extends Type {
	private String mId;
	private String mName;

	private ArrayList<Document> mDocuments = new ArrayList<>();

	private Info mInfo = new Info();

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
		fr.append( "<timestamp value=\"" + Integer.toString( info.getTimeStamp() ) + "\" />" );
		fr.append( "</info>" );
		fr.flush();
		fr.close();
	}

	public ArrayList<Document> getDocuments() {
		return mDocuments;
	}

	public void addDocument(Document document){
		mDocuments.add(document);
	}

	public void removeDocumentWithId(String documentId) throws Exception{
		ArrayList<Document> documents = ParseSeparate.parseCategoryWithId( getId() );
		documents.remove( MainData.getDocumentWithId( documentId ) );
		Utils.saveCategoryDocuments( getId(), documents );
	}

	public void removeDocument(Document document) throws Exception{
		ArrayList<Document> documents = ParseSeparate.parseCategoryWithId( getId() );
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

	@NonNull
	@Override
	public String toString() {
		return "id=\"" + getId() + "\" name=\"" + getName() + "\"";
	}
}
