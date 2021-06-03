package com.maxsavitsky.documenter.data.types;

import android.graphics.Color;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;

import androidx.core.text.HtmlCompat;

import com.maxsavitsky.documenter.App;
import com.maxsavitsky.documenter.data.html.HtmlImageLoader;
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

public class EntryEntity extends Entity {

	private String rawText = null;
	private ArrayList<SpanEntry<AlignmentSpan.Standard>> alignments = null;
	private ArrayList<SpanEntry<RelativeSizeSpan>> relativeSizeSpans = null;

	private Properties mProperties = new Properties();

	public EntryEntity(String id, String name) {
		super( id, name );
	}

	public Properties getProperties() {
		return mProperties;
	}

	public void setProperties(Properties properties) {
		mProperties = properties;
	}

	public Spannable loadText(int imageMaxWidth) throws IOException, JSONException {
		if ( rawText == null ) {
			rawText = loadTextFromStorage();
		}
		Spannable spannable = (Spannable) HtmlCompat.fromHtml( rawText, HtmlCompat.FROM_HTML_MODE_COMPACT, new HtmlImageLoader( imageMaxWidth ), null );
		if(alignments == null || relativeSizeSpans == null)
			loadAdditional();
		for (SpanEntry<AlignmentSpan.Standard> se : alignments) {
			int st = se.getStart(), end = se.getEnd();
			if ( st < 0 ) {
				st = 0;
			}
			if ( end > spannable.length() ) {
				end = spannable.length();
			}

			spannable.setSpan( se.getSpan(), st, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		}

		for (SpanEntry<RelativeSizeSpan> se : relativeSizeSpans) {
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

		JSONArray relativesArray = jsonObject.getJSONArray("relativeSpans");
		relativeSizeSpans = new ArrayList<>(relativesArray.length());
		for(int i = 0; i < relativesArray.length(); i++){
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
		File file = new File( App.appStoragePath + "/entries/" + getId() + "/" + path );
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

	public void saveProperties() throws IOException, JSONException {
		File propsFile = new File( App.appStoragePath + "/entries/" + getId() + "/properties.json" );
		if(!propsFile.exists())
			propsFile.createNewFile();
		String data = mProperties.convertToJSON().toString();
		try(FileOutputStream fos = new FileOutputStream(propsFile)){
			fos.write( data.getBytes( StandardCharsets.UTF_8 ) );
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

		public JSONObject convertToJSON() throws JSONException {
			JSONObject jsonObject = new JSONObject();
			jsonObject
					.put( "textSize", textSize )
					.put( "textColor", textColor )
					.put( "bgColor", bgColor )
					.put( "scrollPosition", mScrollPosition )
					.put( "saveLastPosition", mSaveLastPos )
					.put( "textAlignment", mTextAlignment )
					.put( "defaultTextColor", mDefaultTextColor );
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
			return properties;
		}

	}

}
