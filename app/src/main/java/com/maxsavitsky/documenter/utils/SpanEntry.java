package com.maxsavitsky.documenter.utils;

public class SpanEntry<T> {
	private final T mSpan;
	private final int mStart;
	private final int mEnd;

	public SpanEntry(T span, int st, int end) {
		mSpan = span;
		this.mStart = st;
		this.mEnd = end;
	}

	public T getSpan() {
		return mSpan;
	}

	public int getStart() {
		return mStart;
	}

	public int getEnd() {
		return mEnd;
	}
}
