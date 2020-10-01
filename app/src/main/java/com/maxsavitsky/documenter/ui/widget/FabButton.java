package com.maxsavitsky.documenter.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.github.clans.fab.FloatingActionButton;
import com.maxsavitsky.documenter.R;

public class FabButton extends FloatingActionButton {
	public FabButton(Context context) {
		super( context );
		init( context );
	}

	public FabButton(Context context, AttributeSet attrs) {
		super( context, attrs );
		init( context );
	}

	public FabButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super( context, attrs, defStyleAttr );
		init( context );
	}

	public FabButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super( context, attrs, defStyleAttr, defStyleRes );
		init( context );
	}

	private final View.OnClickListener mOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			View parent = (View) getParent();
			if(parent instanceof FabMenu){
				FabMenu menu = (FabMenu) parent;
				menu.close( true );
			}
		}
	};

	private void init(Context context){
		TypedValue value = new TypedValue();
		context.getTheme().resolveAttribute( R.attr.fabButtonColor, value, true );
		setColorNormal( value.data );
		setColorPressed( value.data );
	}

	@Override
	public void setOnClickListener(final OnClickListener l) {
		View.OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(View view) {
				mOnClickListener.onClick( FabButton.this );
				if(l != null){
					l.onClick( FabButton.this );
				}
			}
		};
		super.setOnClickListener( listener );
	}
}
