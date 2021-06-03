package com.maxsavitsky.documenter.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RadioGroup;

import com.maxsavitsky.documenter.MainActivity;

import java.util.ArrayList;

public class CustomRadioGroup extends RadioGroup {
	private static final String TAG = MainActivity.TAG + " RadioGroup";

	private ArrayList<Integer> childrenIds;

	public CustomRadioGroup(Context context) {
		super( context );
	}

	public CustomRadioGroup(Context context, AttributeSet attrs) {
		super( context, attrs );
	}

	private void initializeChildren() {
		childrenIds = new ArrayList<>( getChildCount() );
		for (int i = 0; i < getChildCount(); i++) {
			childrenIds.add( getChildAt( i ).getId() );
		}
	}

	public void setCheckedIndex(int index) {
		if ( childrenIds == null ) {
			initializeChildren();
		}
		if ( index < 0 || index >= childrenIds.size() ) {
			throw new IndexOutOfBoundsException();
		}
		check( childrenIds.get( index ) );
	}

	public int getCheckedItemIndex() {
		if ( childrenIds == null ) {
			initializeChildren();
		}
		for (int i = 0; i < childrenIds.size(); i++)
			if ( childrenIds.get( i ) == getCheckedRadioButtonId() ) {
				return i;
			}
		return -1;
	}
}
