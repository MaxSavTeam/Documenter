package com.maxsavitsky.documenter.utils;

import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;

public class SpanEntry {
	private ForegroundColorSpan mSpan;
	private StyleSpan mStyleSpan;
	private UnderlineSpan mUnderlineSpan;
	private StrikethroughSpan mStrikethroughSpan;
	private int st, end;

	public SpanEntry(ForegroundColorSpan span, int st, int end) {
		mSpan = span;
		this.st = st;
		this.end = end;
	}

	public SpanEntry(StyleSpan styleSpan, int st, int end) {
		mStyleSpan = styleSpan;
		this.st = st;
		this.end = end;
	}

	public SpanEntry(UnderlineSpan underlineSpan, int st, int end) {
		mUnderlineSpan = underlineSpan;
		this.st = st;
		this.end = end;
	}

	public SpanEntry(StrikethroughSpan strikethroughSpan, int st, int end) {
		mStrikethroughSpan = strikethroughSpan;
		this.st = st;
		this.end = end;
	}

	public StrikethroughSpan getStrikethroughSpan() {
		return mStrikethroughSpan;
	}

	public UnderlineSpan getUnderlineSpan() {
		return mUnderlineSpan;
	}

	public StyleSpan getStyleSpan() {
		return mStyleSpan;
	}

	public ForegroundColorSpan getForegroundSpan() {
		return mSpan;
	}

	public int getStart() {
		return st;
	}

	public int getEnd() {
		return end;
	}
}
