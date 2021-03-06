package com.maxsavitsky.documenter.ui.editor;

import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

import com.maxsavitsky.documenter.MainActivity;

public class TextEditor extends AppCompatEditText {
	private final String TAG = MainActivity.TAG + " Editor";

	private OnSelectionChanges listener = null;
	private boolean mIgnoreChanges = false;
	private boolean initialized = false;

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
		initialized = true;
		mIgnoreChanges = true;
		setText( text, BufferType.SPANNABLE );
	}

	public void appendW(CharSequence text){
		mIgnoreChanges = true;
		append( text );
	}

	public boolean isInitialized() {
		return initialized;
	}

	public TextEditor setInitialized(boolean initialized) {
		this.initialized = initialized;
		return this;
	}

	@Override
	protected void onSelectionChanged(int selStart, int selEnd) {
		super.onSelectionChanged( selStart, selEnd );
		if(listener == null)// || !initialized)
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
		if(!initialized)
			return;
		if(text.toString().equals( "" ))
			return;
		if(listener != null) {
			if(!mIgnoreChanges)
				listener.onTextChanged( text, start, lengthBefore, lengthAfter);
		}
		mIgnoreChanges = false;
	}

	public int getCurrentLine(){
		Layout layout = getLayout();
		if(layout == null)
			return -1;
		return layout.getLineForOffset( getSelectionStart() );
	}

	public void setCursorToLine(int line){
		setSelection( getLayout().getLineStart( line ) );
	}
}
