package com.maxsavitsky.documenter.data.types;

import android.graphics.Color;
import android.text.Html;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.maxsavitsky.documenter.MainActivity;
import com.maxsavitsky.documenter.data.Info;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.TextLoader;
import com.maxsavitsky.documenter.data.html.HtmlImageLoader;
import com.maxsavitsky.documenter.utils.SpanEntry;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.XMLParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Entry extends Type {

	private static final String TAG = MainActivity.TAG + " Entry";
	private final String mId;
	private final String mName;
	private final String mPathDir;
	private Info mInfo;

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
		return "Entry";
	}

	@Nullable
	public ArrayList<Document> getParentDocuments() {
		try {
			return getDocumentsInWhichIncludedThisEntry();
		} catch (IOException | SAXException e) {
			e.printStackTrace();
			return null;
		}
	}

	private Properties mProperties;

	public Properties getProperties() {
		if ( mProperties == null ) {
			if ( mId.equals( "temp_entry" ) ) {
				mProperties = new Properties();
				return mProperties;
			}
			try {
				return readProperties();
			} catch (IOException | SAXException e) {
				e.printStackTrace();
			}
		}
		return mProperties;
	}

	public void setProperties(Properties properties) {
		mProperties = properties;
	}

	public Entry(String id, String name) {
		this.mId = id;
		this.mName = name;

		this.mPathDir = Utils.getExternalStoragePath().getPath() + "/entries/" + id + "/";
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
		if ( !file.exists() ) {
			file.mkdir();
		}

		file = new File( file.getPath() + "/" + getId() );
		if ( !file.exists() ) {
			file.mkdir();
		}

		file = new File( file.getPath() + "/info.xml" );
		if ( !file.exists() ) {
			file.createNewFile();
		}

		FileWriter fr = new FileWriter( file, false );
		fr.write( Utils.xmlHeader );
		fr.append( "<info>\n" );
		fr.append( "<timestamp value=\"" ).append( String.valueOf( info.getTimeStamp() ) ).append( "\" />\n" );
		fr.append( "</info>" );
		fr.flush();
		fr.close();
	}

	public String getPathDir() {
		return mPathDir;
	}

	public void saveContent(Spannable text) throws IOException {
		File file = new File( mPathDir + "text" );
		if ( !file.exists() ) {
			file.createNewFile();
		}

		saveAllAlignments( text.getSpans( 0, text.length(), AlignmentSpan.Standard.class ), text );
		for (AlignmentSpan.Standard span : text.getSpans( 0, text.length(), AlignmentSpan.Standard.class )) {
			text.removeSpan( span );
		}

		saveAllRelativeSpans( text.getSpans( 0, text.length(), RelativeSizeSpan.class ), text );
		for (RelativeSizeSpan span : text.getSpans( 0, text.length(), RelativeSizeSpan.class )) {
			text.removeSpan( span );
		}

		String htmlText = null;
		if ( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N ) {
			htmlText = Html.toHtml( text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE );
		} else {
			htmlText = Html.toHtml( text );
		}
		FileWriter fr = new FileWriter( file );
		fr.append( htmlText );
		fr.flush();
		fr.close();
	}

	private void saveAllAlignments(AlignmentSpan.Standard[] spans, Spannable text) {
		File file = new File( mPathDir + "alignment" );
		if ( spans.length != 0 ) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < spans.length; i++) {
				AlignmentSpan.Standard span = spans[ i ];
				sb.append( span.getAlignment().toString() ).append( " " ).append( text.getSpanStart( span ) ).append( " " ).append( text.getSpanEnd( span ) );
				if ( i != spans.length - 1 ) {
					sb.append( "\n" );
				}
			}
			try {
				FileWriter fr = new FileWriter( file );
				fr.write( sb.toString() );
				fr.flush();
				fr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			if ( file.exists() ) {
				file.delete();
			}
		}
	}

	private void saveAllRelativeSpans(RelativeSizeSpan[] spans, Spannable text) {
		File file = new File( mPathDir + "relative_spans" );
		if ( spans.length != 0 ) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < spans.length; i++) {
				RelativeSizeSpan span = spans[ i ];
				sb.append( span.getSizeChange() ).append( " " ).append( text.getSpanStart( span ) ).append( " " ).append( text.getSpanEnd( span ) );
				if ( i != spans.length - 1 ) {
					sb.append( "\n" );
				}
			}
			try {
				FileWriter fr = new FileWriter( file );
				fr.write( sb.toString() );
				fr.flush();
				fr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			if ( file.exists() ) {
				file.delete();
			}
		}
	}

	public ArrayList<File> getContentFiles() {
		File dir = new File( mPathDir );

		return getDirectoryFiles( dir );
	}

	private ArrayList<File> getDirectoryFiles(File dir) {
		ArrayList<File> files = new ArrayList<>();
		File[] children = dir.listFiles();
		if ( children != null ) {
			for (File child : children) {
				if ( child.isFile() && !child.getName().equals( "included_in.xml" ) ) {
					files.add( child );
				} else if ( child.isDirectory() ) {
					files.addAll( getDirectoryFiles( child ) );
				}
			}
		}
		return files;
	}

	public void deleteContentFiles() {
		ArrayList<File> files = getContentFiles();
		for (File file : files) {
			file.delete();
		}
	}

	public ArrayList<SpanEntry<AlignmentSpan.Standard>> getAlignments() {
		ArrayList<SpanEntry<AlignmentSpan.Standard>> arrayList = new ArrayList<>();
		File file = new File( mPathDir + "alignment" );
		if ( file.exists() ) {
			try {
				BufferedReader br = new BufferedReader( new FileReader( file ) );
				String line;
				while ( ( line = br.readLine() ) != null ) {
					if ( Thread.currentThread().isInterrupted() ) {
						break;
					}

					String[] strings = line.split( " " );
					SpanEntry<AlignmentSpan.Standard> se = new SpanEntry<AlignmentSpan.Standard>( new AlignmentSpan.Standard( Alignment.valueOf( strings[ 0 ] ) ),
							Integer.parseInt( strings[ 1 ] ),
							Integer.parseInt( strings[ 2 ] )
					);
					arrayList.add( se );
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return arrayList;
	}

	public ArrayList<SpanEntry<RelativeSizeSpan>> getRelativeSpans() {
		ArrayList<SpanEntry<RelativeSizeSpan>> arrayList = new ArrayList<>();
		File file = new File( mPathDir + "relative_spans" );
		if ( file.exists() ) {
			try {
				BufferedReader br = new BufferedReader( new FileReader( file ) );
				String line;
				while ( ( line = br.readLine() ) != null ) {
					if ( Thread.currentThread().isInterrupted() ) {
						break;
					}

					String[] strings = line.split( " " );
					SpanEntry<RelativeSizeSpan> se = new SpanEntry<>( new RelativeSizeSpan( Float.parseFloat( strings[ 0 ] ) ),
							Integer.parseInt( strings[ 1 ] ),
							Integer.parseInt( strings[ 2 ] )
					);
					arrayList.add( se );
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return arrayList;
	}

	private String formatInt(int x) {
		return String.format( Locale.ROOT, "%d", x );
	}

	public void saveProperties() throws IOException {
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries/" + mId + "/properties.xml" );
		if ( !file.exists() ) {
			file.createNewFile();
		}
		FileWriter fw = new FileWriter( file );
		fw.write( Utils.xmlHeader );
		fw.append( "<properties>\n" )
				.append( "\t<textSize value=\"" ).append( formatInt( mProperties.textSize ) ).append( "\" />\n" )
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

	public ArrayList<String> removeUnusedImages() {
		ArrayList<String> removed = new ArrayList<>();
		try {
			Spanned spanned = Html.fromHtml( loadText() );
			ImageSpan[] spans = spanned.getSpans( 0, spanned.length(), ImageSpan.class );
			Map<String, Boolean> used = new HashMap<>();
			for (ImageSpan span : spans) {
				File f = new File( span.getSource() );
				used.put( f.getName(), true );
			}
			File dir = getImagesMediaFolder();

			File[] files = dir.listFiles();
			if ( files != null ) {
				for (File file : files) {
					if ( !used.containsKey( file.getName() ) ) {
						file.delete();
						removed.add( file.getPath().replace( Utils.getExternalStoragePath().getPath(), "" ) );
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return removed;
	}

	public Entry.Properties readProperties() throws IOException, SAXException {
		this.mProperties = XMLParser.newInstance().parseEntryProperties( mId );
		return mProperties;
	}

	public void checkMediaDir() {
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries/" + mId + "/media/images" );
		if ( !file.exists() ) {
			file.mkdirs();
		}
	}

	public File getImagesMediaFolder() {
		checkMediaDir();
		return new File( Utils.getExternalStoragePath().getPath() + "/entries/" + mId + "/media/images" );
	}

	public void saveProperties(Properties properties) throws IOException {
		mProperties = new Properties( properties );
		saveProperties();
	}

	public void applySaveLastPos(boolean state) throws IOException, SAXException {
		if ( mProperties == null ) {
			mProperties = XMLParser.newInstance().parseEntryProperties( mId );
		}
		mProperties.setSaveLastPos( state );
		saveProperties();
	}

	public String loadText() throws IOException {
		return TextLoader.getInstance().loadTextSync( new File( mPathDir + "text" ) );
	}

	private String prepareDivs(String text) {
		org.jsoup.nodes.Document doc = Jsoup.parse( text );
		Elements elements = doc.select( "div[align]" );
		for (Element element : elements) {
			String styleAttr = element.attr( "style" );
			element.attr( "style", styleAttr + "text-align:" + element.attr( "align" ) + ";" );
			element.removeAttr( "align" );
		}
		Log.i( TAG, "prepareDivs: " + doc.html() );
		return doc.html();
	}

	public Spannable loadAndPrepareText() throws IOException {
		String text = prepareDivs( loadText() );
		Spannable spannable = (Spannable) Html.fromHtml( text, new HtmlImageLoader(), null );
		Log.i( TAG, "loadAndPrepareText: found alignments " + spannable.getSpans( 0, spannable.length(), Alignment.class ).length + " " +
				spannable.getSpans( 0, spannable.length(), AlignmentSpan.class ).length );
		for (SpanEntry<AlignmentSpan.Standard> se : getAlignments()) {
			int st = se.getStart(), end = se.getEnd();
			if ( st < 0 ) {
				st = 0;
			}
			if ( end > spannable.length() ) {
				end = spannable.length();
			}

			spannable.setSpan( se.getSpan(), st, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		}

		for (SpanEntry<RelativeSizeSpan> se : getRelativeSpans()) {
			int st = se.getStart(), end = se.getEnd();
			if ( st < 0 ) {
				st = 0;
			}
			if ( end > spannable.length() ) {
				end = spannable.length();
			}

			spannable.setSpan( se.getSpan(), st, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		}

		return spannable;
	}

	public void addDocumentToIncluded(String documentId) throws IOException, SAXException {
		ArrayList<Document> documents = getDocumentsInWhichIncludedThisEntry();
		for (Document document : documents) {
			if ( document.getId().equals( documentId ) ) {
				return;
			}
		}
		documents.add( MainData.getDocumentWithId( documentId ) );
		saveInWhichDocumentsIncludedThisEntry( documents );
	}

	public void removeDocumentFromIncluded(String documentId) throws IOException, SAXException {
		ArrayList<Document> documents = getDocumentsInWhichIncludedThisEntry();
		//documents.remove(MainData.getDocumentWithId( documentId ));
		for (int i = 0; i < documents.size(); i++) {
			if ( documents.get( i ).getId().equals( documentId ) ) {
				documents.remove( i );
				break;
			}
		}
		saveInWhichDocumentsIncludedThisEntry( documents );
	}

	public ArrayList<Document> getDocumentsInWhichIncludedThisEntry() throws IOException, SAXException {
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries" );
		if ( !file.exists() ) {
			throw new IllegalArgumentException( "MainData.getDocumentsInWhichIncludedThisEntry: entries dir does not exist" );
		}
		file = new File( file.getPath() + "/" + mId );
		if ( !file.exists() ) {
			throw new IllegalArgumentException( "MainData.getDocumentsInWhichIncludedThisEntry: entry dir with id=" + mId + " does not exist" );
		}
		file = new File( file.getPath() + "/included_in.xml" );
		if ( !file.exists() ) {
			return new ArrayList<>();
		}

		return XMLParser.newInstance().getDocumentsInWhichIncludedEntryWithId( mId );
	}

	public void saveInWhichDocumentsIncludedThisEntry(ArrayList<Document> documents) throws IOException {
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries/" + getId() );
		if ( !file.exists() ) {
			file.mkdir();
		}
		file = new File( file.getPath() + "/included_in.xml" );
		if ( !file.exists() ) {
			file.createNewFile();
		}

		FileWriter fr = new FileWriter( file, false );
		fr.write( Utils.xmlHeader );
		fr.append( "<documents>\n" );
		for (Document document : documents) {
			fr.append( "<document id=\"" ).append( document.getId() ).append( "\" />\n" );
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
		private int textSize = 22;

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

		public Properties(Properties other) {
			this.textSize = other.textSize;
			this.bgColor = other.bgColor;
			this.textColor = other.textColor;
			this.mScrollPosition = other.mScrollPosition;
			this.mTextAlignment = other.mTextAlignment;
			this.mSaveLastPos = other.mSaveLastPos;
			this.mDefaultTextColor = other.mDefaultTextColor;
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
			if ( mSaveLastPos != that.mSaveLastPos ) {
				return false;
			}
			return mDefaultTextColor == that.mDefaultTextColor;
		}

		@Override
		public int hashCode() {
			int result = textSize;
			result = 31 * result + bgColor;
			result = 31 * result + textColor;
			result = 31 * result + mScrollPosition;
			result = 31 * result + mTextAlignment;
			result = 31 * result + ( mSaveLastPos ? 1 : 0 );
			result = 31 * result + mDefaultTextColor;
			return result;
		}

		@Override
		public String toString() {
			return "Properties{" +
					"textSize=" + textSize +
					", bgColor=" + bgColor +
					", textColor=" + textColor +
					", mScrollPosition=" + mScrollPosition +
					", mTextAlignment=" + mTextAlignment +
					", mSaveLastPos=" + mSaveLastPos +
					", mDefaultTextColor=" + mDefaultTextColor +
					'}';
		}
	}
}
