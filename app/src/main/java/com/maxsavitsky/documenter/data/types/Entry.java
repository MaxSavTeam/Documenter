package com.maxsavitsky.documenter.data.types;

import android.graphics.Color;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.view.Gravity;

import androidx.annotation.NonNull;

import com.maxsavitsky.documenter.data.Info;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.XMLParser;

import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class Entry extends Type {

	private final String id;
	private final String name;
	private final String pathDir;
	private Info mInfo;
	private Properties mProperties;

	public Properties getProperties() {
		return mProperties;
	}

	public void setProperties(Properties properties) {
		mProperties = properties;
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
		fr.append( "<timestamp value=\"" ).append( String.valueOf( info.getTimeStamp() ) ).append( "\" />\n" );
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

	public void saveText(Spannable text, Properties properties) throws Exception {
		//text = text.replaceAll( "\n", "<br>" );
		File file = new File( pathDir + "text.html" );
		if(!file.exists())
			file.createNewFile();
		FileWriter fr = new FileWriter( file );
		fr.write( Utils.htmlHeader );
		String alignment;
		if( properties.getTextAlignment() == Gravity.START ){
			alignment = "left";
		}else if( properties.getTextAlignment() == Gravity.CENTER_HORIZONTAL){
			alignment = "center";
		}else if( properties.getTextAlignment() == Layout.JUSTIFICATION_MODE_INTER_WORD ){
			alignment = "justify";
		}else{
			alignment = "right";
		}
		String htmlText = Html.toHtml( text );
		fr.append( "<html>\n" )
				.append( "\t<body bgcolor=\"" ).append( "#" ).append( Integer.toHexString( properties.getBgColor() ).substring( 2 ) ).append( "\" " )
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

	private String formatInt(int x){
		return String.format( Locale.ROOT, "%d", x );
	}

	public void saveProperties() throws IOException {
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries/" + id + "/properties.xml" );
		if ( !file.exists() ) {
			file.createNewFile();
		}
		FileWriter fw = new FileWriter( file );
		fw.write( Utils.xmlHeader);
		fw.append( "<properties>\n" )
				.append("\t<textSize value=\"" ).append( formatInt( mProperties.textSize ) ).append( "\" />\n" )
				.append( "\t<bgColor value=\"" ).append( formatInt( mProperties.getBgColor() ) ).append( "\" />\n" )
				.append( "\t<textColor value=\"" ).append( formatInt( mProperties.getTextColor() ) ).append( "\" />\n" )
				.append( "\t<scrollPosition value=\"" ).append( formatInt( mProperties.getScrollPosition() ) ).append( "\" />\n" )
				.append( "\t<textAlignment value=\"" ).append( formatInt( mProperties.getTextAlignment() ) ).append( "\" />\n" )
				.append( "\t<saveLastPos value=\"" ).append( Boolean.toString( mProperties.isSaveLastPos() ) ).append( "\" />\n" )
				.append( "\t<defaultColor value=\"" ).append( formatInt( mProperties.getDefaultTextColor() ) ).append( "\" />\n" )
				.append( "</properties>" );
		fw.flush();
		fw.close();
	}

	public Entry.Properties readProperties() throws IOException, SAXException {
		this.mProperties = XMLParser.newInstance().parseEntryProperties( id );
		return mProperties;
	}

	public void checkMediaDir(){
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries/" + id + "/media/images" );
		if(!file.exists())
			file.mkdirs();
	}

	public File getImagesMediaFolder(){
		checkMediaDir();
		return new File( Utils.getExternalStoragePath().getPath() + "/entries/" + id + "/media/images" );
	}

	public void saveProperties(Properties properties) throws IOException{
		mProperties = new Properties( properties );
		saveProperties();
	}

	public void applySaveLastPos(boolean state) throws IOException, SAXException {
		if( mProperties == null){
			mProperties = XMLParser.newInstance().parseEntryProperties( id );
		}
		mProperties.setSaveLastPos( state );
		saveProperties();
	}

	public ArrayList<String> loadTextLines() throws IOException{
		BufferedReader br = new BufferedReader( new FileReader( new File( pathDir + "text" ) ) );
		String line;
		ArrayList<String> strings = new ArrayList<>();
		while(((line = br.readLine())) != null){
			if(Thread.currentThread().isInterrupted())
				break;
			strings.add( line );
		}

		return strings;
	}

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

	public void addDocumentToIncluded(String documentId) throws IOException, SAXException {
		ArrayList<Document> documents = getDocumentsInWhichIncludedThisEntry();
		for(Document document : documents){
			if(document.getId().equals( documentId ))
				return;
		}
		documents.add( MainData.getDocumentWithId( documentId ));
		saveInWhichDocumentsIncludedThisEntry( documents );
	}

	public void removeDocumentFromIncluded(String documentId) throws IOException, SAXException {
		ArrayList<Document> documents = getDocumentsInWhichIncludedThisEntry();
		//documents.remove(MainData.getDocumentWithId( documentId ));
		for(int i = 0; i < documents.size(); i++){
			if(documents.get( i ).getId().equals( documentId )){
				documents.remove( i );
			}
		}
		saveInWhichDocumentsIncludedThisEntry( documents );
	}

	public ArrayList<Document> getDocumentsInWhichIncludedThisEntry() throws IOException, SAXException {
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries" );
		if(!file.exists())
			throw new IllegalArgumentException( "MainData.getDocumentsInWhichIncludedThisEntry: entries dir does not exist" );
		file = new File( file.getPath() + "/" + id );
		if(!file.exists())
			throw new IllegalArgumentException( "MainData.getDocumentsInWhichIncludedThisEntry: entry dir with id=" + id + " does not exist" );
		file = new File( file.getPath() + "/included_in.xml" );
		if(!file.exists())
			return new ArrayList<>(  );

		return XMLParser.newInstance().getDocumentsInWhichIncludedEntryWithId( id );
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

	public static class Properties {
		public int textSize;
		private int bgColor = Color.WHITE;

		private int textColor = Color.BLACK;

		private int mScrollPosition = 0;

		private int mTextAlignment = Gravity.START;

		private boolean mSaveLastPos = true;

		private int mDefaultTextColor = Color.BLACK;

		public int getDefaultTextColor() {
			return mDefaultTextColor;
		}

		public void setDefaultTextColor(int defaultTextColor) {
			mDefaultTextColor = defaultTextColor;
		}

		public boolean isSaveLastPos() {
			return mSaveLastPos;
		}

		public void setSaveLastPos(boolean saveLastPos) {
			Properties.this.mSaveLastPos = saveLastPos;
		}

		public int getTextAlignment() {
			return mTextAlignment;
		}

		public void setTextAlignment(int textAlignment) {
			mTextAlignment = textAlignment;
		}

		public Properties(Properties properties) {
			this.textSize = properties.getTextSize();
			this.bgColor = properties.getBgColor();
			this.textColor = properties.getTextColor();
			mScrollPosition = properties.getScrollPosition();
			mTextAlignment = properties.getTextAlignment();
			this.mSaveLastPos = properties.isSaveLastPos();
			this.mDefaultTextColor = properties.getDefaultTextColor();
		}

		public int getScrollPosition() {
			return mScrollPosition;
		}

		public void setScrollPosition(int scrollPosition) {
			this.mScrollPosition = scrollPosition;
		}

		public int getTextColor() {
			return textColor;
		}

		public void setTextColor(int textColor) {
			this.textColor = textColor;
		}

		public int getBgColor() {
			return bgColor;
		}

		public void setBgColor(int bgColor) {
			this.bgColor = bgColor;
		}

		public Properties() {
			this.textSize = 22;
		}

		public int getTextSize() {
			return textSize;
		}

		public void setTextSize(int textSize) {
			this.textSize = textSize;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Properties that = (Properties) o;

			if ( textSize != that.textSize ) {
				return false;
			}
			if ( bgColor != that.bgColor ) {
				return false;
			}
			if ( textColor != that.textColor ) {
				return false;
			}
			if ( mScrollPosition != that.mScrollPosition ) {
				return false;
			}
			if ( mTextAlignment != that.mTextAlignment ) {
				return false;
			}
			return mSaveLastPos == that.mSaveLastPos;
		}

		@Override
		public int hashCode() {
			int result = textSize;
			result = 31 * result + bgColor;
			result = 31 * result + textColor;
			result = 31 * result + mScrollPosition;
			result = 31 * result + mTextAlignment;
			result = 31 * result + ( mSaveLastPos ? 1 : 0 );
			return result;
		}

		@NonNull
		@Override
		public String toString() {
			return bgColor + "\n" +
					textColor + "\n" +
					textSize + "\n" +
					mScrollPosition;
		}
	}
}
