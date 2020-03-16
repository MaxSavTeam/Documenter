package com.maxsavitsky.documenter.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

@SuppressLint("AppCompatCustomView")
public class TextEditor extends AppCompatEditText {

	private OnSelectionChanges listener = null;
	private boolean mIgnoreChanges = false;

	public TextEditor(Context context) {
		super( context );
	}

	public TextEditor(Context context, AttributeSet attrs) {
		super( context, attrs );
	}

	public TextEditor(Context context, AttributeSet attrs, int defStyleAttr) {
		super( context, attrs, defStyleAttr );
	}

	public void setListener(OnSelectionChanges onSelectionChanges){
		listener = onSelectionChanges;
	}

	public interface OnSelectionChanges {
		void onTextSelected(int start, int end);
		void onTextSelectionBreak(int newSelectionPosition);
		void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter);
	}

	public void setTextW(CharSequence text){
		mIgnoreChanges = true;
		super.setText( text, BufferType.SPANNABLE );
	}

	public void appendW(CharSequence text){
		mIgnoreChanges = true;
		super.append( text );
	}

	@Override
	protected void onSelectionChanged(int selStart, int selEnd) {
		if ( selStart == selEnd ) {
			if(listener != null) {
				listener.onTextSelectionBreak( selStart );
			}
		} else {
			if(listener != null) {
				listener.onTextSelected( selStart, selEnd );
			}
		}
		super.onSelectionChanged( selStart, selEnd );
	}

	@Override
	protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
		super.onTextChanged( text, start, lengthBefore, lengthAfter );
		if(listener != null && !mIgnoreChanges) {
			listener.onTextChanged( text, start, lengthBefore, lengthAfter);
		}
		mIgnoreChanges = false;
	}
}
