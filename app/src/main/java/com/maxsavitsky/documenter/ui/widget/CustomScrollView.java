package com.maxsavitsky.documenter.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class CustomScrollView extends ScrollView {

	public CustomScrollView(Context context) {
		super( context );
	}

	public CustomScrollView(Context context, AttributeSet attrs) {
		super( context, attrs );
	}

	public CustomScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
		super( context, attrs, defStyleAttr );
	}

	public CustomScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super( context, attrs, defStyleAttr, defStyleRes );
	}

	public int getMaxVerticalScroll(){
		return computeVerticalScrollRange();
	}

}
