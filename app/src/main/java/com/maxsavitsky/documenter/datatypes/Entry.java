package com.maxsavitsky.documenter.datatypes;

import android.text.Html;

import androidx.annotation.NonNull;

import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.ParseSeparate;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class Entry extends Type {

	private String id, name, pathDir;
	private Info mInfo;

	public Entry(String id, String name) {
		this.id = id;
		this.name = name;

		this.pathDir = Utils.getExternalStoragePath().getPath() + "/entries/" + id + "/";
	}

	public void setInfo(Info info) {
		mInfo = info;
	}

	public Info getInfo() {
		return mInfo;
	}

	public void setAndSaveInfo(Info info) throws IOException {
		setInfo( info );

		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries/" );
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
		fr.append( "<timestamp value=\"" + Integer.toString( info.getTimeStamp() ) + "\" />\n" );
		fr.append( "</info>" );
		fr.flush();
		fr.close();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	public String getPathDir() {
		return pathDir;
	}

	public void saveText(String text) throws Exception{
		text = text.replaceAll( "\n", "<br>" );
		File file = new File( pathDir + "text.html" );
		if(!file.exists())
			file.createNewFile();
		FileWriter fr = new FileWriter( file );
		fr.write( Utils.htmlHeader );
		fr.append( "<html>\n<body>\n" )
				.append( text )
				.append( "\n</body>\n</html>" );
		fr.flush();

		fr = new FileWriter( pathDir + "text" );
		fr.write( text );
		fr.flush();
		fr.close();
	}

	public String loadText() throws Exception{
		String text = "";
		FileReader fr = new FileReader( pathDir + "text" );
		while(fr.ready()){
			text = String.format( "%s%c", text, (char)fr.read() );
		}

		return text;
	}

	public void addDocumentToIncluded(String documentId) throws Exception {
		ArrayList<Document> documents = getDocumentsInWhichIncludedThisEntry();
		documents.add(MainData.getDocumentWithId( documentId ));
		saveInWhichDocumentsIncludedThisEntry( documents );
	}

	public void removeDocumentFromIncluded(String documentId) throws Exception {
		ArrayList<Document> documents = getDocumentsInWhichIncludedThisEntry();
		//documents.remove(MainData.getDocumentWithId( documentId ));
		for(int i = 0; i < documents.size(); i++){
			if(documents.get( i ).getId().equals( documentId )){
				documents.remove( i );
			}
		}
		saveInWhichDocumentsIncludedThisEntry( documents );
	}

	public ArrayList<Document> getDocumentsInWhichIncludedThisEntry() throws Exception {
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries" );
		if(!file.exists())
			throw new IllegalArgumentException( "MainData.getDocumentsInWhichIncludedThisEntry: entries dir does not exist" );
		file = new File( file.getPath() + "/" + id );
		if(!file.exists())
			throw new IllegalArgumentException( "MainData.getDocumentsInWhichIncludedThisEntry: entry dir with id=" + id + " does not exist" );
		file = new File( file.getPath() + "/included_in.xml" );
		if(!file.exists())
			return new ArrayList<>(  );

		return ParseSeparate.getDocumentsInWhichIncludedEntryWithId( id );
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
