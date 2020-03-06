package com.maxsavitsky.documenter;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
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

import com.flask.colorpicker.ColorPickerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.data.Info;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.types.Document;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.data.types.EntryProperty;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.widget.TextEditor;
import com.maxsavitsky.documenter.xml.XMLParser;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class CreateEntry extends ThemeActivity {
	private Document mDocument;
	private String type;
	private Entry mEntry;
	private String title = "Create new entry";
	private EntryProperty mEntryProperty;
	private TextEditor mTextEditor;
	private Button btnBgColorPicker, btnTextColorPicker;
	private ImageButton btnBold, btnItalic, btnUnderline, btnStrike;
	private BottomSheetBehavior mBottomSheetLayout;
	private ArrayList<ChangeEntry> mHistory = new ArrayList<>();
	private int mHistoryIterator = -1;
	private Menu mMenu;
	private final int UNDO_MENU_INDEX = 0;
	private final int REDO_MENU_INDEX = 1;
	private Editable mStartEditable = new Editable.Factory().newEditable( "" );
	private boolean mDarkTheme;
	private SharedPreferences sp;
	private ArrayList<Integer> mColorHistory = new ArrayList<>();

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

	private static class SpanEntry{
		private ForegroundColorSpan mSpan;
		private StyleSpan mStyleSpan;
		private UnderlineSpan mUnderlineSpan;
		private StrikethroughSpan mStrikethroughSpan;
		private int st, end;

		public SpanEntry(ForegroundColorSpan span, int st, int end) {
			mSpan = span;
			this.st = st;
			this.end = end;
		}

		public SpanEntry(StyleSpan styleSpan, int st, int end) {
			mStyleSpan = styleSpan;
			this.st = st;
			this.end = end;
		}

		public SpanEntry(UnderlineSpan underlineSpan, int st, int end) {
			mUnderlineSpan = underlineSpan;
			this.st = st;
			this.end = end;
		}

		public SpanEntry(StrikethroughSpan strikethroughSpan, int st, int end) {
			mStrikethroughSpan = strikethroughSpan;
			this.st = st;
			this.end = end;
		}

		public StrikethroughSpan getStrikethroughSpan() {
			return mStrikethroughSpan;
		}

		public UnderlineSpan getUnderlineSpan() {
			return mUnderlineSpan;
		}

		public StyleSpan getStyleSpan() {
			return mStyleSpan;
		}

		public ForegroundColorSpan getForegroundSpan() {
			return mSpan;
		}

		public int getSt() {
			return st;
		}

		public int getEnd() {
			return end;
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
		mDarkTheme = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() ).getBoolean( "dark_theme", false );
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
		setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT );
		setContentView( R.layout.activity_create_entry );
		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );

		applyTheme();

		sp = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

		btnBgColorPicker = findViewById( R.id.btnBgColorPicker );
		btnTextColorPicker = findViewById( R.id.btnTextColorPicker );

		btnBold = findViewById( R.id.btnBold );
		btnItalic = findViewById( R.id.btnItalic );
		btnUnderline = findViewById( R.id.btnUnderline );
		btnStrike = findViewById( R.id.btnStrike );

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

		readColorHistory();

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

	private void readColorHistory(){
		String history = sp.getString( "color_history", null );
		if(history != null){
			int i = 0;
			while(i < history.length()){
				String entry = "";
				while(history.charAt( i ) != ';'){
					entry = String.format( "%s%c", entry, history.charAt( i ) );
					i++;
				}
				mColorHistory.add( Integer.parseInt( entry ) );
				i++;
			}
		}
	}

	private void addColorToHistory(int color){
		if(mColorHistory.size() == 5){
			mColorHistory.remove( 0 );
		}
		mColorHistory.add( color );
		saveColorHistory();
	}

	private void saveColorHistory(){
		String save = "";
		for(Integer i : mColorHistory){
			save = String.format( "%s%s;", save, i );
		}
		sp.edit().putString( "color_history", save ).apply();
	}

	private  void setTextInEditor(String text){
		AbsoluteSizeSpan[] absoluteSizeSpans = mTextEditor.getText().getSpans( 0, mTextEditor.getText().length(), AbsoluteSizeSpan.class );
		if(absoluteSizeSpans.length > 0){
			for(AbsoluteSizeSpan a : absoluteSizeSpans) {
				mTextEditor.getText().removeSpan( a );
			}
		}

		String[] splitStrings = text.split( "\n" );

		for (String s : splitStrings) {
			Spanned fromHtml  = Html.fromHtml( s );
			mTextEditor.appendW( fromHtml );
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
					setTextInEditor( originalText );
					mTextEditor.setScrollY( mEntryProperty.getScrollPosition() );
					mStartEditable = mTextEditor.getText();
					mProgressDialogOnTextLoad.cancel();
					final double end = System.currentTimeMillis();
					final double seconds = (end - mStartLoadTextTime) / 1000;
					Log.v("Text loader", "Text loaded after " + seconds);
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
			if(s == null)
				return;
			ForegroundColorSpan[] foregroundColorSpans = s.getSpans( start, end, ForegroundColorSpan.class );
			int c;
			if(foregroundColorSpans.length != 0){
				c = foregroundColorSpans[foregroundColorSpans.length-1].getForegroundColor();
			}else{
				c = Color.BLACK;
			}
			mSelectionBounds = new int[]{start, end};
			btnTextColorPicker.setBackgroundTintList( ColorStateList.valueOf( c ) );
			btnTextColorPicker.setOnClickListener( onClickOnSelectColorOfTextSegment );
			applyStyleBtnState( start, end );
			btnBold.setOnClickListener(onTextAppearanceClick);
			btnItalic.setOnClickListener(onTextAppearanceClick);
			btnUnderline.setOnClickListener( onUnderlineBtnClick );
			btnStrike.setOnClickListener( onStrikeBtnClick );
		}

		@Override
		public void onTextSelectionBreak(int newSelectionPosition) {
			btnTextColorPicker.setOnClickListener( btnTextOnAllColor );
			btnBold.setOnClickListener( null );
			btnItalic.setOnClickListener( null );
			btnUnderline.setOnClickListener( null );
			btnStrike.setOnClickListener( null );
			setBtnTextColorPickerBackground();
			btnBold.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( android.R.color.transparent ) ) );
			btnItalic.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( android.R.color.transparent ) ) );
			btnUnderline.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( android.R.color.transparent ) ) );
			btnStrike.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( android.R.color.transparent ) ) );
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

	private void applyStyleBtnState(int selS, int selE){
		Editable e = mTextEditor.getText();
		if(e == null)
			return;

		StyleSpan[] ss = e.getSpans( selS, selE, StyleSpan.class );
		boolean isBoldThere = false;
		boolean isItalicThere = false;
		boolean isUnderlineThere = (e.getSpans( selS, selE, UnderlineSpan.class ).length != 0);
		boolean isStrikeThere = (e.getSpans( selS, selE, StrikethroughSpan.class ).length != 0);
		for(StyleSpan s : ss){
			if(s.getStyle() == Typeface.BOLD)
				isBoldThere = true;

			if(s.getStyle() == Typeface.ITALIC)
				isItalicThere = true;
		}
		if(isBoldThere){
			btnBold.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( R.color.btnClicked ) ) );
		}else{
			btnBold.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( android.R.color.transparent ) ) );
		}
		btnBold.setTag( isBoldThere );

		if(isItalicThere){
			btnItalic.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( R.color.btnClicked ) ) );
		}else{
			btnItalic.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( android.R.color.transparent ) ) );
		}
		btnItalic.setTag( isItalicThere );

		if(isUnderlineThere){
			btnUnderline.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( R.color.btnClicked ) ) );
		}else{
			btnUnderline.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( android.R.color.transparent ) ) );
		}
		btnUnderline.setTag(isUnderlineThere);

		btnStrike.setTag( isStrikeThere );
		if(isStrikeThere){
			btnStrike.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( R.color.btnClicked ) ) );
		}else{
			btnStrike.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( android.R.color.transparent ) ) );
		}
	}

	private View.OnClickListener onUnderlineBtnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Editable s = mTextEditor.getText();
			if(s == null)
				return;

			int selSt = mSelectionBounds[0];
			int selEnd = mSelectionBounds[1];
			boolean isUnderline = (boolean) v.getTag();
			UnderlineSpan[] spans = s.getSpans( selSt, selEnd, UnderlineSpan.class );
			ArrayList<SpanEntry> spansToApply = new ArrayList<>();
			for(UnderlineSpan span : spans){
				int st = s.getSpanStart( span );
				int end = s.getSpanEnd( span );
				if(st < selSt){
					spansToApply.add( new SpanEntry( new UnderlineSpan(), st, selSt ) );
				}
				if(end > selEnd){
					spansToApply.add( new SpanEntry( new UnderlineSpan(), selEnd, end ) );
				}
				s.removeSpan( span );
			}
			for(SpanEntry e : spansToApply){
				s.setSpan( e.getUnderlineSpan(), e.getSt(), e.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
			}

			v.setTag( !isUnderline );
			if(!isUnderline){ // apply
				s.setSpan( new UnderlineSpan(), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
				v.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( R.color.btnClicked ) ) );
			}else{ //delete
				v.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( android.R.color.transparent ) ) );
			}
		}
	};

	private View.OnClickListener onStrikeBtnClick = new View.OnClickListener(){
		@Override
		public void onClick(View v) {
			Editable s = mTextEditor.getText();
			if(s == null)
				return;

			int selSt = mSelectionBounds[0];
			int selEnd = mSelectionBounds[1];
			boolean isStrike = (boolean) v.getTag();
			StrikethroughSpan[] spans = s.getSpans( selSt, selEnd, StrikethroughSpan.class );
			ArrayList<SpanEntry> spansToApply = new ArrayList<>();
			for(StrikethroughSpan span : spans){
				int st = s.getSpanStart( span );
				int end = s.getSpanEnd( span );
				if(st < selSt){
					spansToApply.add( new SpanEntry( new StrikethroughSpan(), st, selSt ) );
				}
				if(end > selEnd){
					spansToApply.add( new SpanEntry( new StrikethroughSpan(), selEnd, end ) );
				}
				s.removeSpan( span );
			}
			for(SpanEntry e : spansToApply){
				s.setSpan( e.getStrikethroughSpan(), e.getSt(), e.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
			}

			v.setTag( !isStrike );
			if(!isStrike){ // apply
				s.setSpan( new StrikethroughSpan(), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
				v.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( R.color.btnClicked ) ) );
			}else{ //delete
				v.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( android.R.color.transparent ) ) );
			}
		}
	};

	private View.OnClickListener onTextAppearanceClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			int typeface = Typeface.BOLD;
			if(v.getId() == R.id.btnItalic)
				typeface = Typeface.ITALIC;

			if( !( (boolean) v.getTag() ) ) {
				applyTypeface( typeface, mSelectionBounds[ 0 ], mSelectionBounds[ 1 ], "apply" );
				v.setTag( true );
				v.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( R.color.btnClicked ) ) );
			}else{
				applyTypeface( typeface, mSelectionBounds[0], mSelectionBounds[1], "delete" );
				v.setTag( false );
				v.setBackgroundTintList( ColorStateList.valueOf( getResources().getColor( android.R.color.transparent ) ) );
			}
		}
	};

	private void applyTypeface(int typeface, int selSt, int selEnd, String action){
		Editable s = mTextEditor.getText();
		if(s == null)
			return;

		StyleSpan[] ss = s.getSpans( selSt, selEnd, StyleSpan.class );
		ArrayList<SpanEntry> spansToApply = new ArrayList<>();
		for(StyleSpan span : ss){
			if(span.getStyle() == typeface){
				int st = s.getSpanStart( span );
				int end = s.getSpanEnd( span );

				if(st < selSt){
					SpanEntry se = new SpanEntry( new StyleSpan( span.getStyle() ), st, selSt );
					spansToApply.add( se );
				}
				if(end > selEnd){
					SpanEntry se = new SpanEntry( new StyleSpan( span.getStyle() ), selEnd, end );
					spansToApply.add(se);
				}
				s.removeSpan( span );
			}
		}
		for(SpanEntry spanEntry : spansToApply){
			s.setSpan( spanEntry.getStyleSpan(), spanEntry.getSt(), spanEntry.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		}
		if(action.equals( "apply" ))
			s.setSpan( new StyleSpan( typeface ), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
	}

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
			View.OnClickListener whatToDo = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					btnBgColorPicker.setBackgroundTintList( ColorStateList.valueOf( mSelectedColor ) );
					//btnBgColorPicker.setBackground( getDrawable( R.drawable.btn_picker_borders ) );
					mTextEditor.setBackgroundColor( mSelectedColor );
					getWindow().getDecorView().setBackgroundColor( mSelectedColor );
					mEntryProperty.setBgColor( mSelectedColor );
				}
			};
			AlertDialog alertDialog = getColorPickerDialog( R.string.set_background_color, mEntryProperty.getBgColor(), whatToDo );
			alertDialog.show();
		}
	};

	private void applySpanColorToText(int color, int selSt, int selEnd){
		Editable s = mTextEditor.getText();
		if(s == null)
			return;
		ForegroundColorSpan[] spans = s.getSpans( mSelectionBounds[0], mSelectionBounds[1], ForegroundColorSpan.class );
		ArrayList<SpanEntry> spansToApply = new ArrayList<>();
		for(ForegroundColorSpan span : spans){
			int st = s.getSpanStart( span );
			int end = s.getSpanEnd( span );
			if(st < selSt){
				spansToApply.add( new SpanEntry( new ForegroundColorSpan( span.getForegroundColor() ), st, selSt ) );
			}
			if(end > selEnd){
				spansToApply.add( new SpanEntry( new ForegroundColorSpan( span.getForegroundColor() ), selEnd, end ) );
			}
			s.removeSpan( span );
		}
		for(SpanEntry entry : spansToApply){
			s.setSpan( entry.getForegroundSpan(), entry.getSt(), entry.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		}
		s.setSpan( new ForegroundColorSpan( color ), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
	}

	private int[] mSelectionBounds;
	private View.OnClickListener onClickOnSelectColorOfTextSegment = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			View.OnClickListener onClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					applySpanColorToText( mSelectedColor, mSelectionBounds[0], mSelectionBounds[1] );
				}
			};
			Editable s = mTextEditor.getText();
			int c;
			if(s == null){
				c = Color.BLACK;
			}else {
				ForegroundColorSpan[] spans = s.getSpans( mSelectionBounds[ 0 ], mSelectionBounds[ 1 ], ForegroundColorSpan.class );
				if(spans.length == 0){
					c = Color.BLACK;
				}else{
					c = spans[spans.length - 1].getForegroundColor();
				}
			}
			AlertDialog alertDialog = getColorPickerDialog( R.string.set_text_color_of_selected_segment, c, onClickListener );
			alertDialog.show();
		}
	};

	private void initializeColorButtons(final View layout, final View.OnClickListener clickListener, final AlertDialog alertDialog){
		int[] btnIds = new int[]{ R.id.btnColorHistory5, R.id.btnColorHistory4, R.id.btnColorHistory3, R.id.btnColorHistory2, R.id.btnColorHistory1 };
		for(int i = 0; i < btnIds.length; i++){
			int id = btnIds[i];
			Button btn = layout.findViewById(id);
			//btn.setTag( 1, i );
			if(i >= mColorHistory.size()){
				btn.setVisibility( View.INVISIBLE );
			}else{
				btn.setBackgroundTintList( ColorStateList.valueOf( mColorHistory.get( i ) ) );
				btn.setTag( mColorHistory.get( i ) );
				btn.setOnClickListener( clickListener );
				/*btn.setOnLongClickListener( new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						int tag = (int) v.getTag(1);
						mColorHistory.remove( tag );
						alertDialog.cancel();
						Toast.makeText( CreateEntry.this, "Entry was deleted from history", Toast.LENGTH_SHORT ).show();
						return true;
					}
				} );*/
			}
		}
	}

	private AlertDialog getColorPickerDialog(int title, int defColor, final View.OnClickListener whatToDo){
		View layout = getLayoutInflater().inflate( R.layout.layout_color_picker, null );
		final ColorPickerView colorPickerView = layout.findViewById( R.id.color_picker );
		colorPickerView.setColor( defColor, true );
		final AlertDialog.Builder builder = new AlertDialog.Builder(CreateEntry.this)
				.setTitle( title )
				.setView( layout )
				.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mSelectedColor = colorPickerView.getSelectedColor();
						addColorToHistory( mSelectedColor );
						whatToDo.onClick( null );
						dialog.cancel();
					}
				} ).setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		} );
		final AlertDialog alertDialog = builder.create();
		if(mColorHistory.size() == 0){
			layout.findViewById( R.id.layoutColorsHistory ).setVisibility( View.GONE );
		}else{
			initializeColorButtons( layout, new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					alertDialog.cancel();
					mSelectedColor = (int) v.getTag();
					whatToDo.onClick(null);
				}
			}, alertDialog );
		}

		return alertDialog;
	}

	private int mSelectedColor;

	private View.OnClickListener btnTextOnAllColor = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			final Editable e = mTextEditor.getText();
			final ForegroundColorSpan[] spans = e.getSpans( 0, e.length(), ForegroundColorSpan.class );
			AlertDialog alertDialog = getColorPickerDialog( R.string.set_text_color_of_all_text,
					(spans.length > 0 ? spans[0].getForegroundColor() : Color.BLACK),
					new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							for(ForegroundColorSpan span : spans){
								e.removeSpan( span );
							}
							e.setSpan( new ForegroundColorSpan( mSelectedColor ), 0, mTextEditor.getText().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
							btnTextColorPicker.setBackgroundTintList( ColorStateList.valueOf( mSelectedColor ) );
						}
					} );
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
		getWindow().getDecorView().setBackgroundColor( mEntryProperty.getBgColor() );

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
				name.setTextColor( getResources().getColor( CreateEntry.super.mEditTextColor ) );
				AlertDialog.Builder builder = new AlertDialog.Builder( CreateEntry.this, CreateEntry.super.mAlertDialogStyle )
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
