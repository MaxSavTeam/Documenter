package com.maxsavitsky.documenter;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.data.types.Document;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.data.types.EntryProperty;
import com.maxsavitsky.documenter.data.Info;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.widget.TextEditor;
import com.maxsavitsky.documenter.xml.XMLParser;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import yuku.ambilwarna.AmbilWarnaDialog;

public class CreateEntry extends ThemeActivity {
	private Document mDocument;
	private String type;
	private Entry mEntry;
	private String title = "Create new entry";
	private EntryProperty mEntryProperty;
	private TextEditor mTextEditor;
	private Button btnBgColorPicker, btnTextColorPicker;
	private BottomSheetBehavior mBottomSheetLayout;
	private ArrayList<ChangeEntry> mHistory = new ArrayList<>();
	private int mHistoryIterator = -1;
	private Menu mMenu;
	private final int UNDO_MENU_INDEX = 0;
	private final int REDO_MENU_INDEX = 1;
	private Editable mStartEditable = new Editable.Factory().newEditable( "" );
	private boolean mDarkTheme;

	private static class ChangeEntry{
		private SpannableString mSpannableString;
		private int mCursorPosition;

		public ChangeEntry(SpannableString spannableString, int cursorPosition) {
			mSpannableString = spannableString;
			mCursorPosition = cursorPosition;
		}

		public SpannableString getSpannableString() {
			return mSpannableString;
		}

		public int getCursorPosition() {
			return mCursorPosition;
		}
	}

	private interface OnLoadedTextListener{
		void loaded(Spannable spannable, String originalText);
		void exceptionOccurred(Exception e);
	}

	private void applyTheme(){
		ActionBar actionBar = getSupportActionBar();
		if(actionBar != null){
			Utils.applyDefaultActionBarStyle(actionBar);
			actionBar.setTitle( title );
		}
		mDarkTheme = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() ).getBoolean( "dark_header", false );
		if(mDarkTheme) {
			ImageButton btn = findViewById( R.id.btnAlignLeft );
			btn.setImageDrawable( getDrawable( R.drawable.ic_align_left_white ) );
			btn = findViewById( R.id.btnAlignCenter );
			btn.setImageDrawable( getDrawable( R.drawable.ic_align_center_white ) );
			btn = findViewById( R.id.btnAlignRight );
			btn.setImageDrawable( getDrawable( R.drawable.ic_align_right_white ) );
		}
	}

	private void _finishActivity(){
		finish();
	}

	private void backPressed(){
		if(mTextEditor.getText() == null && mStartEditable == null) {
			setResult( ResultCodes.OK );
			_finishActivity();
			return;
		}
		String t = mTextEditor.getText().toString();
		if ( !t.isEmpty() &&
				( !type.equals( "edit" )
						|| !mEntryProperty.equals( mEntry.getProperty() )
						|| !mStartEditable.equals( mTextEditor.getEditableText() )
				)
		) {
			AlertDialog.Builder builder = new AlertDialog.Builder( this, super.mAlertDialogStyle )
					.setTitle( R.string.confirmation )
					.setMessage( R.string.create_entry_exit_mes ).setCancelable( false )
					.setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					} )
					.setPositiveButton( R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							setResult( ResultCodes.OK );
							_finishActivity();
						}
					} ).setNeutralButton( R.string.save_and_exit, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							saveEntry();
						}
					} );

			builder.create().show();
		} else {
			setResult( ResultCodes.OK );
			_finishActivity();
		}
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if(item.getItemId() == android.R.id.home){
			backPressed();
		}else if(item.getItemId() == R.id.item_undo){
			ChangeEntry changeEntry = mHistory.get( --mHistoryIterator );
			mTextEditor.setTextW( changeEntry.getSpannableString() );
			if(changeEntry.getCursorPosition() > mTextEditor.length())
				changeEntry.mCursorPosition = mTextEditor.length();
			mTextEditor.setSelection( changeEntry.getCursorPosition() );
			if(mHistoryIterator == 0){
				disableThisMenuItem( UNDO_MENU_INDEX );
			}
			enableThisMenuItem( REDO_MENU_INDEX );
		}else if(item.getItemId() == R.id.item_redo){
			mTextEditor.setTextW( mHistory.get( ++mHistoryIterator ).getSpannableString() );
			mTextEditor.setSelection( mHistory.get( mHistoryIterator ).getCursorPosition() );
			if(mHistoryIterator == mHistory.size() - 1){
				disableThisMenuItem( REDO_MENU_INDEX );
			}
			enableThisMenuItem( UNDO_MENU_INDEX );
		}
		return super.onOptionsItemSelected( item );
	}

	@Override
	public void onBackPressed() {
		backPressed();
	}

	ProgressDialog mProgressDialogOnTextLoad;
	private long mStartLoadTextTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_create_entry );
		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );

		applyTheme();

		btnBgColorPicker = findViewById( R.id.btnBgColorPicker );
		btnTextColorPicker = findViewById( R.id.btnTextColorPicker );

		type = getIntent().getStringExtra( "type" );
		mEntryProperty = new EntryProperty();
		mTextEditor = findViewById( R.id.edittextEntry );
		mTextEditor.setListener( mOnSelectionChanges );
		if ( type != null && type.equals( "edit" ) ) {
			mEntry = MainData.getEntryWithId( getIntent().getStringExtra( "id" ) );
			try {
				mEntryProperty = new XMLParser().parseEntryProperties( mEntry.getId() );
			}catch (Exception e){
				Utils.getErrorDialog( e, this ).show();
			}
			mEntry.setProperty( new EntryProperty( mEntryProperty ) );
			title = "Edit entry text";
			applyTheme();

			try {
				mProgressDialogOnTextLoad = new ProgressDialog( this );
				mProgressDialogOnTextLoad.setTitle( "Loading..." );
				mProgressDialogOnTextLoad.setMessage( "We're loading your entry..." );
				mProgressDialogOnTextLoad.setCancelable( false );
				mProgressDialogOnTextLoad.show();
				new Thread( new Runnable() {
					@Override
					public void run() {
						String loadedText;
						mStartLoadTextTime = System.currentTimeMillis();
						Log.v("TextLoader", "Start");
						try {
							loadedText = mEntry.loadText();
						}catch (final Exception e){
							mOnLoadedTextListener.exceptionOccurred( e );
							Log.v( "TextLoader", "exception = " + e.toString() );
							return;
						}
						Log.v("TextLoader", "Text loaded");
						Spannable spannable = (Spannable) Html.fromHtml( loadedText );
						mOnLoadedTextListener.loaded( spannable, loadedText );
					}
				} ).start();
			}catch (Exception e){
				e.printStackTrace();
				Utils.getErrorDialog( e, this ).show();
				//return;
			}
		}else{
			mDocument = MainData.getDocumentWithId( getIntent().getStringExtra( "id" ) );
		}

		setEditTextSize();
		FloatingActionButton fab = findViewById( R.id.fabSaveEntry );
		fab.setOnClickListener( saveEntry );
		Utils.showKeyboard( mTextEditor, this );

		View llBottomSheet = findViewById( R.id.bottom_sheet );
		mBottomSheetLayout = BottomSheetBehavior.from( llBottomSheet );

		LinearLayout peek = findViewById( R.id.layoutBottomSheetCeil );
		peek.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mBottomSheetLayout.getState() == BottomSheetBehavior.STATE_COLLAPSED)
					mBottomSheetLayout.setState( BottomSheetBehavior.STATE_EXPANDED );
				else
					mBottomSheetLayout.setState( BottomSheetBehavior.STATE_COLLAPSED );
			}
		} );

	}

	private  void setTextInEditor(String text, Spannable spannable){
		AbsoluteSizeSpan[] absoluteSizeSpans = mTextEditor.getText().getSpans( 0, mTextEditor.getText().length(), AbsoluteSizeSpan.class );
		if(absoluteSizeSpans.length > 0){
			for(AbsoluteSizeSpan a : absoluteSizeSpans) {
				mTextEditor.getText().removeSpan( a );
			}
		}

		String[] splitStrings = text.split( "\n" );

		//ArrayList<SpannableString> spannableStrings = split( new SpannableString( spannable ), "\n" );
		for (String s : splitStrings) {
			Spanned fromHtml  = Html.fromHtml( s );
			/*if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
				fromHtml = Html.fromHtml(s, Html.FROM_HTML_MODE_COMPACT);
			}else{
				fromHtml = Html.fromHtml( s );
			}*/
			mTextEditor.appendW( fromHtml );
			/*if(i != splitStrings.length - 1){
				mTextEditor.appendW( Html.fromHtml( "<br>" ) );
			}*/
		}
	}

	private OnLoadedTextListener mOnLoadedTextListener = new OnLoadedTextListener() {
		@Override
		public void loaded(final Spannable spannable, final String originalText) {
			double seconds = (System.currentTimeMillis() - mStartLoadTextTime) / 1000.0;
			Log.v( "TextLoader", "mOnLoadedTextListener.loaded called. Seconds = " + seconds );

			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					setTextInEditor( originalText, spannable );
					mTextEditor.setScrollY( mEntryProperty.getScrollPosition() );
					mStartEditable = mTextEditor.getText();
					mProgressDialogOnTextLoad.cancel();
					final double end = System.currentTimeMillis();
					final double seconds = (end - mStartLoadTextTime) / 1000;
					Log.v( "TextLoader", "runOnUiCalled" );
					//Toast.makeText( CreateEntry.this, "Text loaded after " + seconds + " seconds", Toast.LENGTH_LONG ).show();
				}
			} );
		}

		@Override
		public void exceptionOccurred(final Exception e) {
			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					mProgressDialogOnTextLoad.cancel();
					double end = System.currentTimeMillis();
					//Toast.makeText( CreateEntry.this, (end - mStartLoadTextTime) / 1000 + " seconds passed", Toast.LENGTH_LONG ).show();
					Utils.getErrorDialog( e, CreateEntry.this ).show();
				}
			} );
		}
	};

	private void enableThisMenuItem(int i){
		mMenu.getItem( i ).setEnabled( true );
		mMenu.getItem( i ).setIcon( i == UNDO_MENU_INDEX ? R.drawable.ic_undo : R.drawable.ic_redo );
		//invalidateOptionsMenu();
	}

	private void disableThisMenuItem(int i){
		mMenu.getItem( i ).setEnabled( false );
		mMenu.getItem( i ).setIcon( i == UNDO_MENU_INDEX ? R.drawable.ic_disabled_undo : R.drawable.ic_disabled_redo );
		//invalidateOptionsMenu();
	}


	private void saveTextChange(){
		SpannableString spannableString = new SpannableString( mTextEditor.getText() );
		int pos = mTextEditor.getSelectionEnd();
		if(mHistoryIterator != mHistory.size() - 1) {
			ArrayList<ChangeEntry> historyCopy = new ArrayList<>();
			for (int i = mHistoryIterator; i < mHistory.size(); i++) {
				historyCopy.add( mHistory.get( i ) );
			}
			mHistory.clear();
			mHistory.addAll( historyCopy );
		}
		enableThisMenuItem( UNDO_MENU_INDEX );
		disableThisMenuItem( REDO_MENU_INDEX );
		mHistory.add( new ChangeEntry( spannableString, pos ) );
		while(mHistory.size() > 100){
			mHistory.remove( 0 );
		}
		mHistoryIterator = mHistory.size() - 1;
	}

	private void setBtnTextColorPickerBackground(){
		ForegroundColorSpan[] foregroundColorSpans = mTextEditor.getText().getSpans( 0, mTextEditor.getText().length(), ForegroundColorSpan.class);
		btnTextColorPicker.setBackgroundTintList( ColorStateList.valueOf(
				foregroundColorSpans.length != 0 ? foregroundColorSpans[ foregroundColorSpans.length-1 ].getForegroundColor() : mTextEditor.getCurrentTextColor() ) );
	}

	TextEditor.OnSelectionChanges mOnSelectionChanges = new TextEditor.OnSelectionChanges() {
		@Override
		public void onTextSelected(int start, int end) {
			Editable s = mTextEditor.getText();
			ForegroundColorSpan[] foregroundColorSpans = s.getSpans( start, end, ForegroundColorSpan.class );
			if(foregroundColorSpans.length != 0){
				mTextColor = foregroundColorSpans[foregroundColorSpans.length-1].getForegroundColor();
			}else{
				mTextColor = Color.WHITE;
			}
			mSelectionBounds = new int[]{start, end};
			btnTextColorPicker.setBackgroundTintList( ColorStateList.valueOf( mTextColor ) );
			btnTextColorPicker.setOnClickListener( onClickOnSelectColorOfTextSegment );
		}

		@Override
		public void onTextSelectionBreak(int newSelectionPosition) {
			btnTextColorPicker.setOnClickListener( btnTextOnAllColor );
			if(newSelectionPosition != -100) {
				if ( mTextEditor.getText() == null || mTextEditor.getText().length() == 0 ) {
					return;
				}
			}
			setBtnTextColorPickerBackground();
		}

		@Override
		public void onTextChanged() {
			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					saveTextChange();
				}
			} );
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate( R.menu.create_entry_menu, menu );
		mMenu = menu;
		disableThisMenuItem( UNDO_MENU_INDEX );
		disableThisMenuItem( REDO_MENU_INDEX );
		return super.onCreateOptionsMenu( menu );
	}

	private View.OnClickListener btnBgColorPickerDefaultClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			new AmbilWarnaDialog( CreateEntry.this, mEntryProperty.getBgColor(), new AmbilWarnaDialog.OnAmbilWarnaListener() {
				@Override
				public void onCancel(AmbilWarnaDialog dialog) {

				}

				@Override
				public void onOk(AmbilWarnaDialog dialog, int color) {
					btnBgColorPicker.setBackgroundTintList( ColorStateList.valueOf( color ) );
					//btnBgColorPicker.setBackground( getDrawable( R.drawable.btn_picker_borders ) );
					mTextEditor.setBackgroundColor( color );
					mEntryProperty.setBgColor( color );
					//Toast.makeText( CreateEntry.this, Integer.toString( color ), Toast.LENGTH_SHORT ).show();
				}
			} ).show();
		}
	};
	private int mTextColor;
	private int[] mSelectionBounds;
	private View.OnClickListener onClickOnSelectColorOfTextSegment = new View.OnClickListener() {
		@Override
		public void onClick(View v) {

			android.app.AlertDialog alertDialog = new AmbilWarnaDialog( CreateEntry.this, mTextColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
				@Override
				public void onCancel(AmbilWarnaDialog dialog) {

				}

				@Override
				public void onOk(AmbilWarnaDialog dialog, int color) {
					Editable s = mTextEditor.getText();
					s.setSpan( new ForegroundColorSpan( color ), mSelectionBounds[0], mSelectionBounds[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
					runOnUiThread( new Runnable() {
						@Override
						public void run() {
							saveTextChange();
						}
					} );
				}
			} ).getDialog();
			alertDialog.setTitle( R.string.set_text_color_of_selected_segment );
			alertDialog.show();
		}
	};

	private View.OnClickListener btnTextOnAllColor = new View.OnClickListener() {
		@Override
		public void onClick(View v) {

			android.app.AlertDialog alertDialog = new AmbilWarnaDialog( CreateEntry.this, btnTextColorPicker.getBackgroundTintList().getDefaultColor(), new AmbilWarnaDialog.OnAmbilWarnaListener() {
				@Override
				public void onCancel(AmbilWarnaDialog dialog) {

				}

				@Override
				public void onOk(AmbilWarnaDialog dialog, int color) {
					mTextEditor.getText().setSpan( new ForegroundColorSpan( color ), 0, mTextEditor.getText().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
					btnTextColorPicker.setBackgroundTintList( ColorStateList.valueOf( color ) );
				}
			} ).getDialog();
			alertDialog.setTitle( R.string.set_text_color_of_all_text );
			alertDialog.show();
		}
	};

	@Override
	protected void onPostCreate(@Nullable Bundle savedInstanceState) {
		super.onPostCreate( savedInstanceState );
		btnBgColorPicker.setOnClickListener( btnBgColorPickerDefaultClickListener );
		setBtnTextColorPickerBackground();
		btnTextColorPicker.setOnClickListener( btnTextOnAllColor );

		btnBgColorPicker.setBackgroundTintList( ColorStateList.valueOf( mEntryProperty.getBgColor() ) );
		mTextEditor.setBackgroundColor( mEntryProperty.getBgColor() );

		ImageButton imageButton;
		if(mEntryProperty.getTextAlignment() == Gravity.CENTER_HORIZONTAL)
			imageButton = findViewById( R.id.btnAlignCenter );
		else if(mEntryProperty.getTextAlignment() == Gravity.START)
			imageButton = findViewById( R.id.btnAlignLeft );
		else
			imageButton = findViewById( R.id.btnAlignRight );

		chooseTextAlignment( imageButton );
	}

	public void plusTextSize(View view){
		if(mEntryProperty.textSize < 45)
			mEntryProperty.textSize++;
		setEditTextSize();
	}

	public void minusTextSize(View view){
		if(mEntryProperty.textSize > 15)
			mEntryProperty.textSize--;
		setEditTextSize();
	}

	private void setEditTextSize(){
		/*((EditText) findViewById( R.id.edittextEntry )).setTextSize( TypedValue.COMPLEX_UNIT_SP, (float) mEntryProperty.textSize );*/
		TextView t = findViewById( R.id.textViewTextSize );
		t.setText( String.format( Locale.ROOT, "%d", mEntryProperty.textSize ) );

		/*mTextEditor.getText().setSpan( new AbsoluteSizeSpan( (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_SP, mEntryProperty.textSize, getResources().getDisplayMetrics() ) ),
				0, mTextEditor.getText().length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);*/
		mTextEditor.setTextSize( TypedValue.COMPLEX_UNIT_SP,  mEntryProperty.textSize );
	}

	private void createEntry(String name, Spannable text){
		String id = Utils.generateUniqueId() + "_ent";
		mEntry = new Entry( id, name );
		ArrayList<Entry> entries = MainData.getEntriesList();
		entries.add( mEntry );
		MainData.setEntriesList( entries );
		Utils.saveEntriesList( entries );
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries" );
		if(!file.exists())
			file.mkdir();
		file = new File( Utils.getExternalStoragePath().getPath() + "/entries/" + id );
		file.mkdir();
		try {
			mEntry.saveProperties( mEntryProperty );
		}catch (Exception e){
			Utils.getErrorDialog( e, this ).show();
			return;
		}
		file = new File( file.getPath() + "/included_in.xml" );
		try{
			file.createNewFile();
		}catch (Exception e){
			Utils.getErrorDialog( e, this ).show();
		}
		//Toast.makeText( this, Html.escapeHtml( text ), Toast.LENGTH_LONG ).show();
		//Toast.makeText( this, Html.fromHtml( Html.escapeHtml( text ) ), Toast.LENGTH_LONG ).show();
		try {
			FileWriter fr = new FileWriter( file, false );
			fr.write( Utils.xmlHeader + "<documents>\n</documents>" );
			fr.flush();
			fr.close();
			mEntry.addDocumentToIncluded( mDocument.getId() );
			mEntry.saveText( text, mEntryProperty );
			mEntry.setAndSaveInfo( new Info( (int) new Date().getTime() ) );
			mDocument.addEntry( mEntry );
			setResult( ResultCodes.REOPEN, new Intent().putExtra( "id", id ) );
			finish();
		}catch (Exception e){
			Utils.getErrorDialog( e, this ).show();
			e.printStackTrace();
		}
	}

	private void resetAlignmentButtons(){
		ImageButton btn;
		int[] btnIds = new int[]{R.id.btnAlignLeft, R.id.btnAlignCenter, R.id.btnAlignRight, R.id.btnAlignJustify};
		for(int id : btnIds){
			btn = findViewById( id );
			btn.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( android.R.color.transparent ) ) );
		}
	}

	public void chooseTextAlignment(View v){
		resetAlignmentButtons();
		if(mDarkTheme)
			v.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( R.color.gray ) ) );
		else
			v.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( R.color.btnClicked ) ) );

		EditText editText = findViewById( R.id.edittextEntry );
		if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ){
			editText.setJustificationMode( Layout.JUSTIFICATION_MODE_NONE );
		}
		int alignment = Gravity.START;
		if(v.getId() == R.id.btnAlignCenter){
			alignment = Gravity.CENTER_HORIZONTAL;
		}else if(v.getId() == R.id.btnAlignRight){
			alignment = Gravity.END;
		}else{
			if(v.getId() == R.id.btnAlignJustify) {
				if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
					editText.setJustificationMode( Layout.JUSTIFICATION_MODE_INTER_WORD );
					editText.setGravity( Gravity.NO_GRAVITY );
					editText.setTextAlignment( View.TEXT_ALIGNMENT_INHERIT );
					alignment = Layout.JUSTIFICATION_MODE_INTER_WORD;
					Toast.makeText( this, "Done", Toast.LENGTH_SHORT ).show();
				}
			}
		}
		if(v.getId() != R.id.btnAlignJustify)
			editText.setGravity( alignment );

		mEntryProperty.setTextAlignment( alignment );
	}

	private void saveEntry(){
		Editable e = mTextEditor.getText();
		if(e == null)
			return;
		String text = e.toString();
		while(e.length() > 0 && (e.charAt( 0 ) == ' ' || e.charAt( 0 ) == '\n')){
			e.delete( 0, 1 );
		}
		while(e.length() > 0 && (e.charAt( e.length()-1 ) == ' ' || e.charAt( e.length() - 1 ) == '\n')){
			e.delete( e.length()-1, e.length() );
		}
		if(type.equals( "create" )) {
			if ( text.length() != 0 ) {
				AlertDialog alertDialog;
				final EditText name = new EditText( CreateEntry.this );
				name.setId( View.NO_ID );
				name.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT ) );
				name.requestFocus();
				AlertDialog.Builder builder = new AlertDialog.Builder( CreateEntry.this )
						.setView( name )
						.setTitle( R.string.enter_name )
						.setMessage( R.string.name_yours_minds )
						.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
								String n = name.getText().toString();
								createEntry( n, mTextEditor.getText() );
							}
						} )
						.setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						} ).setCancelable( false );

				alertDialog = builder.create();
				alertDialog.setOnShowListener( new DialogInterface.OnShowListener() {
					@Override
					public void onShow(DialogInterface dialog) {
						Utils.showKeyboard( name, CreateEntry.this );
					}
				} );
				alertDialog.show();
			} else {
				mTextEditor.requestFocus();
			}
		}else if(type.equals( "edit" )){
			if(text.length() != 0){
				try {
					mEntry.saveProperties( mEntryProperty );
					mEntry.saveText( mTextEditor.getText(), mEntryProperty );
					setResult( ResultCodes.REOPEN, new Intent(  ).putExtra( "id", mEntry.getId() ) );
				}catch (Exception ex){
					Toast.makeText( CreateEntry.this, "edit text\n\n" + ex.toString(), Toast.LENGTH_LONG ).show();
					ex.printStackTrace();
					Utils.getErrorDialog( ex, CreateEntry.this ).show();
					return;
				}
				_finishActivity();
			}
		}
	}

	View.OnClickListener saveEntry = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			mTextEditor.clearComposingText();
			saveEntry();
		}
	};

}
