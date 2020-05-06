package com.maxsavitsky.documenter.utils;

import android.text.Spannable;

public class ChangeEntry {
	private Spannable mSpannable;
	private final int mCursorPosition;
	private String mText;
	private int mScrollY;

	public ChangeEntry(Spannable spannable, int cursorPosition) {
		mSpannable = spannable;
		mCursorPosition = cursorPosition;
	}

	public ChangeEntry(String text, int cursorPosition) {
		mCursorPosition = cursorPosition;
		mText = text;
	}

	public int getScrollY() {
		return mScrollY;
	}

	public void setScrollY(int scrollY) {
		mScrollY = scrollY;
	}

	public String getText() {
		return mText;
	}


	public int getCursorPosition() {
		return mCursorPosition;
	}

	public Spannable getSpannable() {
		return mSpannable;
	}
}
