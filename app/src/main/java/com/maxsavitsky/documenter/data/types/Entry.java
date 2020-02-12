package com.maxsavitsky.documenter.data.types;

import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.view.Gravity;

import androidx.annotation.NonNull;

import com.maxsavitsky.documenter.data.Info;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.ParseSeparate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class Entry extends Type {

	private String id, name, pathDir;
	private Info mInfo;
	private EntryProperty mProperty;

	public EntryProperty getProperty() {
		return mProperty;
	}

	public void setProperty(EntryProperty property) {
		mProperty = property;
	}

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

	public void saveText(Spannable text, EntryProperty property) throws Exception {
		//text = text.replaceAll( "\n", "<br>" );
		File file = new File( pathDir + "text.html" );
		if(!file.exists())
			file.createNewFile();
		FileWriter fr = new FileWriter( file );
		fr.write( Utils.htmlHeader );
		String alignment;
		if(property.getTextAlignment() == Gravity.START ){
			alignment = "left";
		}else if(property.getTextAlignment() == Gravity.CENTER_HORIZONTAL){
			alignment = "center";
		}else if(property.getTextAlignment() == Layout.JUSTIFICATION_MODE_INTER_WORD ){
			alignment = "justify";
		}else{
			alignment = "right";
		}
		String htmlText = Html.toHtml( text );
		fr.append( "<html>\n" )
				.append( "\t<body bgcolor=\"" ).append( "#" ).append( Integer.toHexString( property.getBgColor() ).substring( 2 ) ).append( "\" " )
				.append( "align=\"" ).append( alignment ).append( "\">\n" )
				.append( htmlText )
				.append( "\n\t</body>" )
				.append( "\n</html>" );
		fr.flush();

		fr = new FileWriter( pathDir + "text" );
		fr.write( htmlText );
		fr.flush();
		fr.close();
	}

	public void saveProperties(EntryProperty entryProperty) throws Exception {
		File file = new File( getPathDir() + "properties.xml" );
		if ( !file.exists() ) {
			file.createNewFile();
		}
		FileWriter fr = null;
		fr = new FileWriter( file, false );
		fr.write( Utils.xmlHeader );
		fr.append( "<properties>\n" )
				.append( "\t<textSize value=\"" ).append( String.format( Locale.ROOT, "%d", entryProperty.textSize ) ).append( "\" />\n" )
				.append( "\t<bgColor value=\"" ).append( String.format( Locale.ROOT, "%d", entryProperty.getBgColor() ) ).append( "\" />\n" )
				.append( "\t<textColor value=\"" ).append( String.format( Locale.ROOT, "%d", entryProperty.getTextColor() ) ).append( "\" />\n" )
				.append( "\t<scrollPosition value=\"" ).append( String.format( Locale.ROOT, "%d", entryProperty.getScrollPosition() ) ).append( "\" />\n" )
				.append( "\t<textAlignment value=\"" ).append( String.format( Locale.ROOT, "%d", entryProperty.getTextAlignment() ) ).append( "\" />\n" )
				.append( "</properties>" );

		fr.flush();
		fr.close();
	}

	/*public String loadText() throws Exception{
		String text = "";
		FileReader fr = new FileReader( pathDir + "text" );
		while(fr.ready()){
			text = String.format( "%s%c", text, (char)fr.read() );
		}
		return text;
	}*/

	public String loadText() throws IOException{
		String text = "";
		FileInputStream fileInputStream = new FileInputStream( pathDir + "text" );
		byte[] buffer = new byte[1024];
		int c;
		while(((c = fileInputStream.read(buffer))) != -1){
			if(c < 1024){
				buffer = Arrays.copyOf(buffer, c);
			}

			text = String.format( "%s%s", text, new String( buffer, StandardCharsets.UTF_8 ) );
		}
		fileInputStream.close();
		return text;
	}

	public void addDocumentToIncluded(String documentId) throws Exception {
		ArrayList<Document> documents = getDocumentsInWhichIncludedThisEntry();
		documents.add( MainData.getDocumentWithId( documentId ));
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
