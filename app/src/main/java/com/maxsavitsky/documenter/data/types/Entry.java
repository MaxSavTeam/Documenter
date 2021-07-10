package com.maxsavitsky.documenter.data.types;

import android.graphics.Color;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;

import com.maxsavitsky.documenter.App;
import com.maxsavitsky.documenter.data.html.ImageSpanLoader;
import com.maxsavitsky.documenter.utils.SpanEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Entry extends Entity {

	private String rawText = null;
	private ArrayList<SpanEntry<AlignmentSpan.Standard>> alignments = null;
	private ArrayList<SpanEntry<RelativeSizeSpan>> relativeSizeSpans = null;

	private Properties mProperties = new Properties();

	private final String entryDir;

	public Entry(String id, String name) {
		super( id, name );
		entryDir = App.appDataPath + "/entries/" + id;
	}

	public Properties getProperties() {
		return mProperties;
	}

	public void setProperties(Properties properties) {
		mProperties = properties;
	}

	public Spannable loadText(int imageMaxWidth) throws JSONException, IOException {
		int flag;
		if ( getProperties().getDisplayingMode() == 0 ) {
			flag = Html.FROM_HTML_MODE_LEGACY;
		} else {
			flag = Html.FROM_HTML_MODE_COMPACT;
		}
		return loadText( imageMaxWidth, flag );
	}

	public Spannable loadText(int imageMaxWidth, int flags) throws IOException, JSONException {
		if ( rawText == null ) {
			rawText = loadTextFromStorage();
		}
		Spannable spannable = (Spannable) Html.fromHtml( rawText, flags, new ImageSpanLoader( getId(), imageMaxWidth, mProperties.stretchImages ), null );
		if ( alignments == null || relativeSizeSpans == null ) {
			loadAdditional();
		}
		for(SpanEntry<AlignmentSpan.Standard> se : alignments)
			applySpanEntry( spannable, se );

		for (SpanEntry<RelativeSizeSpan> se : relativeSizeSpans)
			applySpanEntry( spannable, se );

		return spannable;
	}

	private void applySpanEntry(Spannable spannable, SpanEntry<?> se) {
		int st = se.getStart(), end = se.getEnd();
		if ( st > end ) {
			int t = st;
			st = end;
			end = t;
		}
		if ( st > spannable.length() ) {
			int r = end - st;
			end = spannable.length();
			st = end - r;
		}
		if ( st < 0 ) {
			st = 0;
		}
		if ( end > spannable.length() ) {
			st -= end - st;
			if ( st < 0 ) {
				st = 0;
			}
			end = spannable.length();
		}

		spannable.setSpan( se.getSpan(), st, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
	}

	private void loadAdditional() throws IOException, JSONException {
		String data = loadFromEntryStorage( "additional.json" );
		JSONObject jsonObject = new JSONObject( data );
		JSONArray alignmentsArray = jsonObject.getJSONArray( "alignments" );
		alignments = new ArrayList<>( alignmentsArray.length() );
		for (int i = 0; i < alignmentsArray.length(); i++) {
			JSONObject a = alignmentsArray.getJSONObject( i );
			alignments.add(
					new SpanEntry<>(
							new AlignmentSpan.Standard( Layout.Alignment.valueOf( a.getString( "name" ) ) ),
							a.getInt( "start" ),
							a.getInt( "end" )
					)
			);
		}

		JSONArray relativesArray = jsonObject.getJSONArray( "relativeSpans" );
		relativeSizeSpans = new ArrayList<>( relativesArray.length() );
		for (int i = 0; i < relativesArray.length(); i++) {
			JSONObject a = relativesArray.getJSONObject( i );
			relativeSizeSpans.add(
					new SpanEntry<>(
							new RelativeSizeSpan( (float) a.getDouble( "value" ) ),
							a.getInt( "start" ),
							a.getInt( "end" )
					)
			);
		}
	}

	private String loadTextFromStorage() throws IOException {
		return loadFromEntryStorage( "text" );
	}

	private String loadFromEntryStorage(String path) throws IOException {
		File file = new File( entryDir, path );
		try (FileInputStream fis = new FileInputStream( file );
		     ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
			int len;
			byte[] buffer = new byte[ 1024 ];
			while ( ( len = fis.read( buffer ) ) != -1 ) {
				byteArrayOutputStream.write( buffer, 0, len );
			}
			return byteArrayOutputStream.toString();
		}
	}

	public void saveText(Spannable text) throws IOException, JSONException {
		File file = new File( entryDir );
		if ( !file.exists() ) {
			file.mkdirs();
		}
		file = new File( file, "text" );

		rawText = Html.toHtml( text, Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL );
		try (FileOutputStream fos = new FileOutputStream( file )) {
			fos.write( rawText.getBytes( StandardCharsets.UTF_8 ) );
		}

		JSONObject additional = new JSONObject();
		JSONArray alignments = new JSONArray();
		if ( this.alignments == null ) {
			this.alignments = new ArrayList<>();
		} else {
			this.alignments.clear();
		}
		for (AlignmentSpan.Standard span : text.getSpans( 0, text.length(), AlignmentSpan.Standard.class )) {
			int start = text.getSpanStart( span ), end = text.getSpanEnd( span );
			alignments.put(
					new JSONObject()
							.put( "name", span.getAlignment().name() )
							.put( "start", start )
							.put( "end", end )
			);
			this.alignments.add( new SpanEntry<>( span, start, end ) );
		}
		additional.put( "alignments", alignments );

		JSONArray relatives = new JSONArray();
		if ( relativeSizeSpans == null ) {
			relativeSizeSpans = new ArrayList<>();
		} else {
			relativeSizeSpans.clear();
		}
		for (RelativeSizeSpan span : text.getSpans( 0, text.length(), RelativeSizeSpan.class )) {
			int start = text.getSpanStart( span ), end = text.getSpanEnd( span );
			relatives.put(
					new JSONObject()
							.put( "value", span.getSizeChange() )
							.put( "start", start )
							.put( "end", end )
			);
			this.relativeSizeSpans.add( new SpanEntry<>( span, start, end ) );
		}
		additional.put( "relativeSpans", relatives );

		file = new File( entryDir, "additional.json" );
		if ( !file.exists() ) {
			file.createNewFile();
		}
		try (FileOutputStream fos = new FileOutputStream( file )) {
			fos.write( additional.toString().getBytes( StandardCharsets.UTF_8 ) );
		}
	}

	public void saveProperties() throws IOException, JSONException {
		File propsFile = new File( entryDir );
		if ( !propsFile.exists() ) {
			propsFile.mkdirs();
		}
		propsFile = new File( propsFile, "properties.json" );
		if ( !propsFile.exists() ) {
			propsFile.createNewFile();
		}
		String data = mProperties.convertToJSON().toString();
		try (FileOutputStream fos = new FileOutputStream( propsFile )) {
			fos.write( data.getBytes( StandardCharsets.UTF_8 ) );
		}
	}

	public void loadProperties() throws IOException, JSONException {
		File propsFile = new File( entryDir, "properties.json" );
		if ( propsFile.exists() ) {
			String result;
			try (FileInputStream fileInputStream = new FileInputStream( propsFile );
			     ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
				int len;
				byte[] buffer = new byte[ 1024 ];
				while ( ( len = fileInputStream.read( buffer ) ) != -1 ) {
					byteArrayOutputStream.write( buffer, 0, len );
				}
				result = byteArrayOutputStream.toString();
			}
			mProperties = Properties.restoreFromJSON( new JSONObject( result ) );
		} else {
			mProperties = new Properties();
		}
	}

	public void checkMediaDir() {
		File file = new File( entryDir + "/media/images" );
		if ( !file.exists() ) {
			file.mkdirs();
		}
	}

	public File getImagesMediaFolder() {
		checkMediaDir();
		return new File( entryDir + "/media/images" );
	}

	public ArrayList<File> getContentFiles() {
		File dir = new File( entryDir );

		return getDirectoryFiles( dir );
	}

	private ArrayList<File> getDirectoryFiles(File dir) {
		ArrayList<File> files = new ArrayList<>();
		File[] children = dir.listFiles();
		if ( children != null ) {
			for (File child : children) {
				if ( child.isFile() ) {
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

	public void removeUnusedImages() {
		try {
			Spanned spanned = Html.fromHtml( loadTextFromStorage() );
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
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Type getType() {
		return Type.ENTRY;
	}

	public static class Properties {
		private int textSize = 22;

		private int bgColor = Color.WHITE;

		private int textColor = Color.BLACK;

		private int mScrollPosition = 0;

		private int mTextAlignment = Gravity.START;

		private boolean mSaveLastPos = true;

		private int mDefaultTextColor = Color.BLACK;

		private int displayingMode = 1;

		private boolean stretchImages = false;

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
			this.displayingMode = other.displayingMode;
			this.stretchImages = other.stretchImages;
		}

		public Properties() {}

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

		public int getTextSize() {
			return textSize;
		}

		public void setTextSize(int textSize) {
			this.textSize = textSize;
		}

		public int getDisplayingMode() {
			return displayingMode;
		}

		public void setDisplayingMode(int displayingMode) {
			this.displayingMode = displayingMode;
		}

		public boolean isStretchImages() {
			return stretchImages;
		}

		public void setStretchImages(boolean stretchImages) {
			this.stretchImages = stretchImages;
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
			if( stretchImages != that.stretchImages )
				return false;
			return mDefaultTextColor == that.mDefaultTextColor;
		}

		public JSONObject convertToJSON() throws JSONException {
			JSONObject jsonObject = new JSONObject();
			jsonObject
					.put( "textSize", textSize )
					.put( "textColor", textColor )
					.put( "bgColor", bgColor )
					.put( "scrollPosition", mScrollPosition )
					.put( "saveLastPosition", mSaveLastPos )
					.put( "textAlignment", mTextAlignment )
					.put( "defaultTextColor", mDefaultTextColor )
					.put( "displayingMode", displayingMode )
					.put( "stretchImages", stretchImages );
			return jsonObject;
		}

		public static Properties restoreFromJSON(JSONObject jsonObject) {
			Properties properties = new Properties(); // initialized with default values
			properties.textSize = jsonObject.optInt( "textSize", properties.textSize );
			properties.textColor = jsonObject.optInt( "textColor", properties.textColor );
			properties.bgColor = jsonObject.optInt( "bgColor", properties.bgColor );
			properties.mScrollPosition = jsonObject.optInt( "scrollPosition", properties.mScrollPosition );
			properties.mSaveLastPos = jsonObject.optBoolean( "saveLastPosition", properties.mSaveLastPos );
			properties.mTextAlignment = jsonObject.optInt( "textAlignment", properties.mTextAlignment );
			properties.mDefaultTextColor = jsonObject.optInt( "defaultTextColor", properties.mDefaultTextColor );
			properties.displayingMode = jsonObject.optInt( "displayingMode", properties.displayingMode );
			properties.stretchImages = jsonObject.optBoolean( "stretchImages", properties.stretchImages );
			return properties;
		}

	}

}
