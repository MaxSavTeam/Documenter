package com.maxsavitsky.documenter.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.github.clans.fab.FloatingActionMenu;
import com.maxsavitsky.documenter.R;

public class FabMenu extends FloatingActionMenu {
	public FabMenu(Context context) {
		super( context );
		init(context);
	}

	public FabMenu(Context context, AttributeSet attrs) {
		super( context, attrs );
		init(context);
	}

	public FabMenu(Context context, AttributeSet attrs, int defStyleAttr) {
		super( context, attrs, defStyleAttr );
		init(context);
	}

	private void init(Context context){
		setIconAnimated( false );
		TypedValue value = new TypedValue();
		context.getTheme().resolveAttribute( R.attr.fabButtonColor, value, true );
		setMenuButtonColorNormal( value.data );
		setMenuButtonColorPressed( value.data );
		setClosedOnTouchOutside( true );
	}
}
