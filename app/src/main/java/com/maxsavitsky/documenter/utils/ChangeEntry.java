package com.maxsavitsky.documenter.utils;

import android.text.SpannableString;

public class ChangeEntry {
	private SpannableString mSpannableString;
	private final int mCursorPosition;
	private String mText;

	public ChangeEntry(SpannableString spannableString, int cursorPosition) {
		mSpannableString = spannableString;
		mCursorPosition = cursorPosition;
	}

	public ChangeEntry(String text, int cursorPosition) {
		mCursorPosition = cursorPosition;
		mText = text;
	}

	public String getText() {
		return mText;
	}

	public SpannableString getSpannableString() {
		return mSpannableString;
	}

	public int getCursorPosition() {
		return mCursorPosition;
	}
}
