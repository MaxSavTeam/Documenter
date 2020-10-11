package com.maxsavitsky.documenter.ui.editor;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.appcompat.widget.AppCompatEditText;

import com.maxsavitsky.documenter.MainActivity;

public class TextEditor extends AppCompatEditText {
	private final String TAG = MainActivity.TAG + " Editor";

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
		void onSelectionChanged();
	}

	public void setTextWithoutNotifying(CharSequence text){
		mIgnoreChanges = true;
		Log.i(TAG, "setTextW mIgnoreChanges=" + true );
		setText( text, BufferType.SPANNABLE );
	}

	public void appendW(CharSequence text){
		mIgnoreChanges = true;
		append( text );
	}

	@Override
	protected void onSelectionChanged(int selStart, int selEnd) {
		super.onSelectionChanged( selStart, selEnd );
		if(listener == null)
			return;
		if ( selStart == selEnd ) {
			listener.onTextSelectionBreak( selStart );
		} else {
			listener.onTextSelected( selStart, selEnd );
		}
		listener.onSelectionChanged();
	}

	@Override
	protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
		if(text.toString().equals( "" ))
			return;
		if(listener != null) {
			if(!mIgnoreChanges)
				listener.onTextChanged( text, start, lengthBefore, lengthAfter);
		}
		mIgnoreChanges = false;
	}
}
