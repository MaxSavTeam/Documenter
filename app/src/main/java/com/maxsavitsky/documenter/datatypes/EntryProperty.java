package com.maxsavitsky.documenter.datatypes;

import android.graphics.Color;

public class EntryProperty {
	public int textSize;
	private int bgColor = Color.WHITE;

	private int textColor = Color.BLACK;

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
}
