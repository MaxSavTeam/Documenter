package com.maxsavitsky.documenter.data.types;

import android.graphics.Color;
import android.view.Gravity;

import org.json.JSONException;
import org.json.JSONObject;

public class EntryEntity extends Entity {

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

		public Properties(){}

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
