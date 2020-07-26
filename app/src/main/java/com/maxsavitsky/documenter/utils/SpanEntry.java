package com.maxsavitsky.documenter.utils;

import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.UnderlineSpan;

public class SpanEntry<T> {
	private final T mSpan;
	private final int mStart;
	private final int mEnd;
	private final String mType;

	private Integer mInt;
	private Float mFloat;
	private Layout.Alignment mAlignment;
	private String mString;
	private Drawable mDrawable;

	public SpanEntry(T span, int st, int end) {
		mSpan = span;
		this.mStart = st;
		this.mEnd = end;

		mType = span.getClass().getName();
		if ( span instanceof ForegroundColorSpan ) {
			mInt = ( (ForegroundColorSpan) span ).getForegroundColor();
		} else if ( span instanceof StyleSpan ) {
			mInt = ( (StyleSpan) span ).getStyle();
		} else if ( span instanceof AlignmentSpan.Standard ) {
			mAlignment = ( (AlignmentSpan.Standard) span ).getAlignment();
		} else if ( span instanceof ImageSpan ) {
			mString = ( (ImageSpan) span ).getSource();
			mDrawable = ( (ImageSpan) span ).getDrawable();
		} else if ( span instanceof BackgroundColorSpan ) {
			mInt = ( (BackgroundColorSpan) span ).getBackgroundColor();
		}else if(span instanceof RelativeSizeSpan){
			mFloat = ((RelativeSizeSpan) span).getSizeChange();
		}
	}

	public T getSpan() {
		if ( mSpan instanceof ForegroundColorSpan ) {
			return (T) new ForegroundColorSpan( mInt );
		} else if ( mSpan instanceof StyleSpan ) {
			return (T) new StyleSpan( mInt );
		} else if ( mSpan instanceof AlignmentSpan.Standard ) {
			return (T) new AlignmentSpan.Standard( mAlignment );
		} else if ( mSpan instanceof ImageSpan ) {
			return (T) new ImageSpan( mDrawable, mString );
		} else if ( mSpan instanceof BackgroundColorSpan ) {
			return (T) new BackgroundColorSpan( mInt );
		} else if ( mSpan instanceof UnderlineSpan ) {
			return (T) new UnderlineSpan();
		} else if ( mSpan instanceof StrikethroughSpan ) {
			return (T) new StrikethroughSpan();
		} else if ( mSpan instanceof SuperscriptSpan ) {
			return (T) new SuperscriptSpan();
		}else if(mSpan instanceof RelativeSizeSpan ){
			return (T) new RelativeSizeSpan( mFloat );
		}
		throw new RuntimeException( "Stub! Unknown class " + mType + "\nNeed " + mType);
	}

	public int getStart() {
		return mStart;
	}

	public int getEnd() {
		return mEnd;
	}
}
