package com.maxsavitsky.documenter.datatypes;

import android.graphics.Color;

import androidx.annotation.NonNull;

public class EntryProperty {
	public int textSize;
	private int bgColor = Color.WHITE;

	private int textColor = Color.BLACK;

	private int mScrollPosition = 0;

	public EntryProperty(int textSize, int bgColor, int textColor, int scrollPosition) {
		this.textSize = textSize;
		this.bgColor = bgColor;
		this.textColor = textColor;
		mScrollPosition = scrollPosition;
	}

	public EntryProperty(EntryProperty property) {
		this.textSize = property.getTextSize();
		this.bgColor = property.getBgColor();
		this.textColor = property.getTextColor();
		mScrollPosition = property.getScrollPosition();
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

	public EntryProperty() {
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

		EntryProperty that = (EntryProperty) o;

		if ( getTextSize() != that.getTextSize() ) {
			return false;
		}
		if ( getBgColor() != that.getBgColor() ) {
			return false;
		}
		return getTextColor() == that.getTextColor();
	}

	@Override
	public int hashCode() {
		int result = getTextSize();
		result = 31 * result + getBgColor();
		result = 31 * result + getTextColor();
		return result;
	}

	@NonNull
	@Override
	public String toString() {
		return Integer.toString( bgColor ) + "\n" +
				Integer.toString( textColor ) + "\n" +
				textSize + "\n" +
				mScrollPosition;
	}
}
