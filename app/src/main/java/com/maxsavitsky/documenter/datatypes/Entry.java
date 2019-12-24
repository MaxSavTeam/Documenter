package com.maxsavitsky.documenter.datatypes;

import androidx.annotation.NonNull;

import com.maxsavitsky.documenter.utils.Utils;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class Entry {
	private String id, name, pathDir;

	public Entry(String id, String name) {
		this.id = id;
		this.name = name;

		this.pathDir = Utils.getContext().getExternalFilesDir(null).getPath() + "/" + id + "/";
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getPathDir() {
		return pathDir;
	}

	public void addDocumentToIncluded(String documentId) throws ParserConfigurationException, SAXException, IOException {
		ArrayList<Document> documents = MainData.getDocumentsInWhichIncludedThisEntry( getId() );
		documents.add(MainData.getDocumentWithId( documentId ));
		saveInWhichDocumentsIncludedThisEntry( documents );
	}

	public void removeDocumentFromIncluded(String documentId) throws ParserConfigurationException, SAXException, IOException {
		ArrayList<Document> documents = MainData.getDocumentsInWhichIncludedThisEntry( getId() );
		documents.remove(MainData.getDocumentWithId( documentId ));
		saveInWhichDocumentsIncludedThisEntry( documents );
	}

	public void saveInWhichDocumentsIncludedThisEntry(ArrayList<Document> documents) throws IOException {
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries/" + getId() );
		if(!file.exists())
			file.mkdir();
		file = new File( file.getPath() + "/included_in.xml" );
		if(!file.exists())
			file.createNewFile();

		FileWriter fr = new FileWriter( file, false );
		fr.write( Utils.xmlHeader );
		fr.append( "<documents>\n" );
		for(Document document : documents){
			fr.append( "<document id=\"" + document.getId() + "\" />\n" );
		}
		fr.append( "</documents>" );
		fr.flush();
		fr.close();
	}

	@NonNull
	@Override
	public String toString() {
		return "id=\"" + getId() + "\" name=\"" + getName() + "\"";
	}
}
