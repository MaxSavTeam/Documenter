package com.maxsavitsky.documenter;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
import android.text.style.ImageSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.flask.colorpicker.ColorPickerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.data.Info;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.types.Document;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.utils.ChangeEntry;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.utils.HtmlImageLoader;
import com.maxsavitsky.documenter.utils.SpanEntry;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.widget.TextEditor;
import com.maxsavitsky.documenter.xml.XMLParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.maxsavitsky.documenter.codes.Requests.*;

public class EntryEditor extends ThemeActivity {
	private Document mDocument;
	private String type;
	private Entry mEntry;
	private String mId;
	private String title = "Create new entry";
	private Entry.Properties mProperties;
	private TextEditor mTextEditor;
	private Button btnBgColorPicker, btnTextColorPicker;
	private ImageButton btnBold, btnItalic, btnUnderline, btnStrike;
	private BottomSheetBehavior mBottomSheetLayout;
	private final ArrayList<ChangeEntry> mHistory = new ArrayList<>();
	private int mHistoryIterator = -1;
	private Menu mMenu;
	private final int UNDO_MENU_INDEX = 0;
	private final int REDO_MENU_INDEX = 1;
	private Editable mStartEditable = new Editable.Factory().newEditable( "" );
	private boolean mDarkTheme;
	private SharedPreferences sp;
	private final ArrayList<Integer> mColorHistory = new ArrayList<>();
	private final ArrayList<File> mMediaToMove = new ArrayList<>();

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
		Utils.clearTempFolder();
		finish();
	}

	private void backPressed(){
		if(mTextEditor.getText() == null && mStartEditable == null) {
			setResult( Results.OK );
			_finishActivity();
			return;
		}
		String t = mTextEditor.getText().toString();
		if ( !t.isEmpty() &&
				( !type.equals( "edit" )
						|| !mProperties.equals( mEntry.getProperties() )
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
							setResult( Results.OK );
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
			setResult( Results.OK );
			_finishActivity();
		}
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if(item.getItemId() == android.R.id.home){
			backPressed();
		}else if(item.getItemId() == R.id.item_undo){
			/*ChangeEntry changeEntry = mHistory.get( --mHistoryIterator );
			mTextEditor.setTextW( changeEntry.getSpannableString() );
			if(changeEntry.getCursorPosition() > mTextEditor.length())
				changeEntry.mCursorPosition = mTextEditor.length();
			mTextEditor.setSelection( changeEntry.getCursorPosition() );*/
			loadTextFromHistory( --mHistoryIterator );
			if(mHistoryIterator == 0){
				disableThisMenuItem( UNDO_MENU_INDEX );
			}
			enableThisMenuItem( REDO_MENU_INDEX );
		}else if(item.getItemId() == R.id.item_redo){
			/*mTextEditor.setTextW( mHistory.get( ++mHistoryIterator ).getSpannableString() );
			mTextEditor.setSelection( mHistory.get( mHistoryIterator ).getCursorPosition() );*/
			loadTextFromHistory( ++mHistoryIterator );
			if(mHistoryIterator == mHistory.size() - 1){
				disableThisMenuItem( REDO_MENU_INDEX );
			}
			enableThisMenuItem( UNDO_MENU_INDEX );
		}
		return super.onOptionsItemSelected( item );
	}

	private void hideUpButton(){
		FloatingActionButton fab = findViewById( R.id.fabUp );
		fab.animate().setDuration( 500 ).scaleX( 0 ).scaleY( 0 ).start();
	}

	private void showUpButton(){
		FloatingActionButton fab = findViewById( R.id.fabUp );
		fab.animate().setDuration( 500 ).scaleX( 1 ).scaleY( 1 ).start();
	}

	private ProgressDialog mLoadFromHistoryDialog;

	private void loadTextFromHistory(int historyIterator){
		final ChangeEntry changeEntry = mHistory.get( historyIterator );
		final String source = changeEntry.getText();
		final int scrollY = mTextEditor.getScrollY();
		final Editable s = mTextEditor.getText();
		if(s != null)
			s.clear();

		mLoadFromHistoryDialog = new ProgressDialog( this );
		mLoadFromHistoryDialog.setMessage( getResources().getString( R.string.loading ) );
		mLoadFromHistoryDialog.setCancelable( false );
		final OnLoadedTextListener listener = new OnLoadedTextListener() {
			@Override
			public void loaded(Spannable spannable, String originalText) {
				if(changeEntry.getCursorPosition() > mTextEditor.getSelectionStart())
					mTextEditor.setSelection( mTextEditor.getSelectionStart() );
				else {
					mTextEditor.setSelection( changeEntry.getCursorPosition() );
				}
				runOnUiThread( new Runnable() {
					@Override
					public void run() {
						mLoadFromHistoryDialog.dismiss();
					}
				} );
			}

			@Override
			public void exceptionOccurred(Exception e) {
				mLoadFromHistoryDialog.dismiss();
				if ( s != null ) s.clear();

				Utils.getErrorDialog( e, EntryEditor.this ).show();
			}
		};
		mLoadFromHistoryDialog.show();
		new Thread( new Runnable() {
			@Override
			public void run() {
				try {
					setTextInEditor( source );
					listener.loaded( null, null );
				}catch (Exception e){
					listener.exceptionOccurred( e );
					e.printStackTrace();
				}
			}
		} ).start();
	}

	@Override
	public void onBackPressed() {
		backPressed();
	}

	ProgressDialog mProgressDialogOnTextLoad;
	private long mStartLoadTextTime;
	private boolean scrolled = false;
	private boolean mWithoutDoc = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		//setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT );
		setContentView( R.layout.activity_create_entry );
		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );

		sp = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

		btnBgColorPicker = findViewById( R.id.btnBgColorPicker );
		btnTextColorPicker = findViewById( R.id.btnTextColorPicker );

		btnBold = findViewById( R.id.btnBold );
		btnItalic = findViewById( R.id.btnItalic );
		btnUnderline = findViewById( R.id.btnUnderline );
		btnStrike = findViewById( R.id.btnStrike );

		type = getIntent().getStringExtra( "type" );
		mProperties = new Entry.Properties();
		mTextEditor = findViewById( R.id.edittextEntry );
		mTextEditor.setListener( mOnSelectionChanges );
		if ( type != null && type.equals( "edit" ) ) {
			mEntry = MainData.getEntryWithId( getIntent().getStringExtra( "id" ) );
			try {
				mProperties = XMLParser.newInstance().parseEntryProperties( mEntry.getId() );
			}catch (Exception e){
				Utils.getErrorDialog( e, this ).show();
			}
			mId = mEntry.getId();
			mEntry.setProperties( new Entry.Properties( mProperties ) );
			mDefaultTextColor = mProperties.getDefaultTextColor();
			mTextEditor.setTextColor( mDefaultTextColor );
			getWindow().getDecorView().setBackgroundColor( mProperties.getBgColor() );
			title = getResources().getString( R.string.edit_entry );

			try {
				mProgressDialogOnTextLoad = new ProgressDialog( this );
				mProgressDialogOnTextLoad.setTitle( R.string.loading );
				mProgressDialogOnTextLoad.setMessage( getResources().getString( R.string.entry_is_loading ) );
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
			title = getResources().getString( R.string.create_new_entry );
			mWithoutDoc = getIntent().getBooleanExtra( "without_doc", false );
			if(!mWithoutDoc)
				mDocument = MainData.getDocumentWithId( getIntent().getStringExtra( "id" ) );
			mId = Utils.generateUniqueId() + "_ent";
			hideUpButton();
		}
		invalidateOptionsMenu();
		applyTheme();
		findViewById( R.id.fabUp ).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ScrollView scrollView = findViewById( R.id.scrollView );
				scrollView.smoothScrollTo( 0, 0 );
			}
		} );
		readColorHistory();

		(( ScrollView ) findViewById( R.id.scrollView )).setOnScrollChangeListener( new View.OnScrollChangeListener() {
			@Override
			public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
				if ( oldScrollY > scrollY && scrollY > 5 ) {
					showUpButton();
				} else if ( scrollY <= 5 || oldScrollY < scrollY ) {
					hideUpButton();
				}
			}
		} );

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



	private void setTextInEditor(String text){
		Editable e = mTextEditor.getText();
		if(e != null)
			e.clear();

		String[] splitStrings = text.split( "\n" );

		for (String s : splitStrings) {
			Spanned fromHtml  = Html.fromHtml( s, new HtmlImageLoader(this), null );
			mTextEditor.appendW( fromHtml );
		}
	}

	private final OnLoadedTextListener mOnLoadedTextListener = new OnLoadedTextListener() {
		@Override
		public void loaded(final Spannable spannable, final String originalText) {
			double seconds = (System.currentTimeMillis() - mStartLoadTextTime) / 1000.0;
			Log.v( "TextLoader", "mOnLoadedTextListener.loaded called. Seconds = " + seconds );

			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					setTextInEditor( originalText );
					mTextEditor.setScrollY( mProperties.getScrollPosition() );
					mStartEditable = mTextEditor.getText();
					mProgressDialogOnTextLoad.cancel();
					final double end = System.currentTimeMillis();
					final double seconds = (end - mStartLoadTextTime) / 1000;
					Log.v("Text loader", "Text loaded after " + seconds);
					ScrollView scrollView = findViewById( R.id.scrollView );
					WindowManager w = (WindowManager) getSystemService( Context.WINDOW_SERVICE );
					Display d = w.getDefaultDisplay();
					Point p = new Point();
					d.getSize( p );
					if(scrollView.getHeight() <= p.y)
						hideUpButton();
				}
			} );
		}

		@Override
		public void exceptionOccurred(final Exception e) {
			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					mProgressDialogOnTextLoad.cancel();
					Utils.getErrorDialog( e, EntryEditor.this ).show();
				}
			} );
		}
	};

	public void pickImages(View v){
		if(!isReadMemoryAccess()){
			AlertDialog.Builder builder = new AlertDialog.Builder( this );
			builder.setTitle( R.string.warning )
					.setMessage( R.string.image_picker_warning_memory_not_accessed )
					.setCancelable( true )
					.setPositiveButton( R.string.request, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							requestPermissions( new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100 );
						}
					} )
					.create()
					.show();
			return;
		}
		Intent picker = new Intent(Intent.ACTION_GET_CONTENT);
		picker.setType( "image/jpeg" );
		startActivityForResult( picker, PICK_IMAGE );
	}

	private boolean isReadMemoryAccess(){
		return ContextCompat.checkSelfPermission( this, Manifest.permission.READ_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult( requestCode, resultCode, data );
		if(requestCode == PICK_IMAGE){
			if(resultCode == Activity.RESULT_OK){
				if(data == null){
					Toast.makeText( this, "Data is null", Toast.LENGTH_SHORT ).show();
					return;
				}
				Uri uri = data.getData();
				if(uri == null){
					Toast.makeText( this, "Some error occurred", Toast.LENGTH_SHORT ).show();
					return;
				}
				InputStream in;
				try {
					in = getContentResolver().openInputStream( uri );
					if(in == null){
						Toast.makeText( this, "Some problems with reading", Toast.LENGTH_SHORT ).show();
						return;
					}
					File file = Utils.getEntryImagesMediaFolder( mId );
					file = new File(file.getPath() + "/" + Utils.generateUniqueId() + ".jpg");
					if(!MainData.isExists( mId ))
						mMediaToMove.add( file );
					FileOutputStream fos = new FileOutputStream(file);
					int len;
					byte[] buffer = new byte[1024];
					while((len = in.read( buffer )) != -1){
						fos.write( buffer, 0, len );
					}
					fos.close();
					in.close();

					setImageAtSelBounds( file );
				} catch (IOException | NullPointerException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private Point getScreenSize(){
		WindowManager windowManager = (WindowManager) getSystemService( Context.WINDOW_SERVICE );
		Display d = windowManager.getDefaultDisplay();
		Point p = new Point();
		d.getSize( p );
		return p;
	}

	private void setImageAtSelBounds(File file){
		Editable e = mTextEditor.getText();
		int s;
		if(e == null) {
			mTextEditor.setText( "\n \n" );
			mSelectionBounds[0] = 0;
			s = 0;
			e = mTextEditor.getText();
		}else{
			s = mSelectionBounds[0];
			e.insert( s, "\n \n" );
		}
		Bitmap b = BitmapFactory.decodeFile( file.getPath() );
		Point size = getScreenSize();
		Drawable d = new BitmapDrawable(b);
		if(b.getWidth() > size.x){
			int w = b.getWidth();
			int h = b.getHeight();
			int w1 = size.x;
			int h1 = (w1 * h) / w;
			d.setBounds( 0, 0, w1, h1 );
		}else{
			d.setBounds( 0, 0, b.getWidth(), b.getHeight() );
		}
		ImageSpan imageSpan = new ImageSpan(d, file.getPath());

		e.setSpan( imageSpan, s+1, s + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
	}

	private void enableThisMenuItem(int i){
		if(mMenu == null)
			return;
		mMenu.getItem( i ).setEnabled( true );
		mMenu.getItem( i ).setIcon( i == UNDO_MENU_INDEX ? R.drawable.ic_undo : R.drawable.ic_redo );
		//invalidateOptionsMenu();
	}

	private void disableThisMenuItem(int i){
		if(mMenu == null)
			return;
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
		mHistory.add( new ChangeEntry( Html.toHtml( spannableString ), pos ) );
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

	final TextEditor.OnSelectionChanges mOnSelectionChanges = new TextEditor.OnSelectionChanges() {
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
			btnBold.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
			btnItalic.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
			btnUnderline.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
			btnStrike.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
			mSelectionBounds = new int[]{newSelectionPosition, newSelectionPosition};
		}

		@Override
		public void onTextChanged(CharSequence text, final int start, final int lengthBefore, final int lengthAfter) {
			Editable e = mTextEditor.getText();
			if(lengthAfter > lengthBefore){
				int len = lengthAfter - lengthBefore;
				if(e == null)
					return;

				e.setSpan( new ForegroundColorSpan( mDefaultTextColor ), start, start + len - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
			}
			saveTextChange();
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
			btnBold.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );
		}else{
			btnBold.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
		}
		btnBold.setTag( isBoldThere );

		if(isItalicThere){
			btnItalic.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );
		}else{
			btnItalic.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
		}
		btnItalic.setTag( isItalicThere );

		if(isUnderlineThere){
			btnUnderline.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );
		}else{
			btnUnderline.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
		}
		btnUnderline.setTag(isUnderlineThere);

		btnStrike.setTag( isStrikeThere );
		if(isStrikeThere){
			btnStrike.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );
		}else{
			btnStrike.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
		}
	}

	private final View.OnClickListener onUnderlineBtnClick = new View.OnClickListener() {
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
				s.setSpan( e.getUnderlineSpan(), e.getStart(), e.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
			}

			v.setTag( !isUnderline );
			if(!isUnderline){ // apply
				s.setSpan( new UnderlineSpan(), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
				v.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );
			}else{ //delete
				v.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
			}
		}
	};

	private final View.OnClickListener onStrikeBtnClick = new View.OnClickListener(){
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
				s.setSpan( e.getStrikethroughSpan(), e.getStart(), e.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
			}

			v.setTag( !isStrike );
			if(!isStrike){ // apply
				s.setSpan( new StrikethroughSpan(), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
				v.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );
			}else{ //delete
				v.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
			}
		}
	};

	private final View.OnClickListener onTextAppearanceClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			int typeface = Typeface.BOLD;
			if(v.getId() == R.id.btnItalic)
				typeface = Typeface.ITALIC;

			if( !( (boolean) v.getTag() ) ) {
				applyTypeface( typeface, mSelectionBounds[ 0 ], mSelectionBounds[ 1 ], "apply" );
				v.setTag( true );
				v.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );
			}else{
				applyTypeface( typeface, mSelectionBounds[0], mSelectionBounds[1], "delete" );
				v.setTag( false );
				v.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
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
			s.setSpan( spanEntry.getStyleSpan(), spanEntry.getStart(), spanEntry.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
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

	private final View.OnClickListener btnBgColorPickerDefaultClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			View.OnClickListener whatToDo = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					btnBgColorPicker.setBackgroundTintList( ColorStateList.valueOf( mSelectedColor ) );
					//btnBgColorPicker.setBackground( getDrawable( R.drawable.btn_picker_borders ) );
					mTextEditor.setBackgroundColor( mSelectedColor );
					getWindow().getDecorView().setBackgroundColor( mSelectedColor );
					mProperties.setBgColor( mSelectedColor );
				}
			};
			AlertDialog alertDialog = getColorPickerDialog( R.string.set_background_color, mProperties.getBgColor(), whatToDo );
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
			s.setSpan( entry.getForegroundSpan(), entry.getStart(), entry.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		}
		s.setSpan( new ForegroundColorSpan( color ), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
	}

	private int[] mSelectionBounds;
	private final View.OnClickListener onClickOnSelectColorOfTextSegment = new View.OnClickListener() {
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
		final AlertDialog.Builder builder = new AlertDialog.Builder( EntryEditor.this)
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
	private int mDefaultTextColor = Color.BLACK;

	private final View.OnClickListener btnTextOnAllColor = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			final Editable e = mTextEditor.getText();
			if(e == null)
				return;

			final ForegroundColorSpan[] spans = e.getSpans( 0, e.length(), ForegroundColorSpan.class );
			AlertDialog alertDialog = getColorPickerDialog( R.string.set_text_color_of_all_text,
					(spans.length > 0 ? spans[0].getForegroundColor() : Color.BLACK),
					new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							for(ForegroundColorSpan span : spans){
								e.removeSpan( span );
							}
							e.setSpan( new ForegroundColorSpan( mSelectedColor ), 0, e.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
							btnTextColorPicker.setBackgroundTintList( ColorStateList.valueOf( mSelectedColor ) );
							mTextEditor.setTextColor( mSelectedColor );

							mDefaultTextColor = mSelectedColor;
							mProperties.setDefaultTextColor( mSelectedColor );
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

		btnBgColorPicker.setBackgroundTintList( ColorStateList.valueOf( mProperties.getBgColor() ) );
		mTextEditor.setBackgroundColor( mProperties.getBgColor() );
		getWindow().getDecorView().setBackgroundColor( mProperties.getBgColor() );

		ImageButton imageButton;
		if( mProperties.getTextAlignment() == Gravity.CENTER_HORIZONTAL)
			imageButton = findViewById( R.id.btnAlignCenter );
		else if( mProperties.getTextAlignment() == Gravity.START)
			imageButton = findViewById( R.id.btnAlignLeft );
		else
			imageButton = findViewById( R.id.btnAlignRight );

		chooseTextAlignment( imageButton );
	}

	public void plusTextSize(View view){
		if( mProperties.textSize < 45)
			mProperties.textSize++;
		setEditTextSize();
	}

	public void minusTextSize(View view){
		if( mProperties.textSize > 15)
			mProperties.textSize--;
		setEditTextSize();
	}

	private void setEditTextSize(){
		TextView t = findViewById( R.id.textViewTextSize );
		t.setText( String.format( Locale.ROOT, "%d", mProperties.textSize ) );
		Editable e = mTextEditor.getText();
		if(e == null)
			return;

		AbsoluteSizeSpan absoluteSizeSpan = new AbsoluteSizeSpan( mProperties.getTextSize(), true );
		AbsoluteSizeSpan[] spans = e.getSpans( 0, e.length(), AbsoluteSizeSpan.class );
		for(AbsoluteSizeSpan span : spans){
			e.removeSpan( span );
		}
		e.setSpan( absoluteSizeSpan, 0, e.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
	}

	private void createEntry(String name, Spannable text){
		mTextEditor.clearComposingText();
		mEntry = new Entry( mId, name );
		ArrayList<Entry> entries = MainData.getEntriesList();
		entries.add( mEntry );
		MainData.setEntriesList( entries );
		Utils.saveEntriesList( entries );
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries/" + mId );
		if(!file.exists())
			file.mkdirs();
		try {
			mEntry.setProperties( mProperties );
			mEntry.saveProperties();
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
		try {
			copyTempFiles();
			replaceTempImagesInSpans();
			removeUnusedImages();
			FileWriter fr = new FileWriter( file, false );
			fr.write( Utils.xmlHeader + "<documents>\n</documents>" );
			fr.flush();
			fr.close();
			mEntry.saveText( text, mProperties );
			mEntry.setAndSaveInfo( new Info( (int) new Date().getTime() ) );
			if(!mWithoutDoc) {
				mEntry.addDocumentToIncluded( mDocument.getId() );
				mDocument.addEntry( mEntry );
			}
			setResult( Results.REOPEN, new Intent().putExtra( "id", mId ) );
			finish();
		}catch (Exception e){
			Utils.getErrorDialog( e, this ).show();
			e.printStackTrace();
		}
	}

	private void copyTempFiles() throws IOException{
		for(File file : mMediaToMove){
			File dest = new File( mEntry.getImagesMediaFolder().getPath() + "/" + file.getName() );
			FileInputStream fis = new FileInputStream(file);
			FileOutputStream fos = new FileOutputStream(dest);
			byte[] b = new byte[1024];
			int len;
			while((len = fis.read( b )) != -1){
				fos.write( b, 0, len );
			}
			fis.close();
			fos.close();

			file.delete();
		}
		mMediaToMove.clear();
	}

	private void resetAlignmentButtons(){
		ImageButton btn;
		int[] btnIds = new int[]{R.id.btnAlignLeft, R.id.btnAlignCenter, R.id.btnAlignRight, R.id.btnAlignJustify};
		for(int id : btnIds){
			btn = findViewById( id );
			btn.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
		}
	}

	public void chooseTextAlignment(View v){
		resetAlignmentButtons();
		if(mDarkTheme)
			v.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.gray ) ) );
		else
			v.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );

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

		mProperties.setTextAlignment( alignment );
	}

	private void removeUnusedImages(){
		Editable e = mTextEditor.getText();
		if(e == null)
			return;
		Map<String, Boolean> usedResources = new HashMap<>();
		for(ImageSpan span : e.getSpans( 0, e.length(), ImageSpan.class )){
			File file = new File(span.getSource());
			usedResources.put( file.getName(), true );
		}
		File file = Utils.getEntryImagesMediaFolder( mId );
		File[] files = file.listFiles();
		if(files == null)
			return;
		for(File f : files){
			if(!usedResources.containsKey( f.getName() )){
				f.delete();
			}
		}
	}

	private void replaceTempImagesInSpans(){
		Editable e = mTextEditor.getText();
		if(e == null)
			return;
		for(ImageSpan span : e.getSpans( 0, e.length(), ImageSpan.class )){
			int st = e.getSpanStart( span );
			int end = e.getSpanEnd( span );
			int flags = e.getSpanFlags( span );
			File old = new File(span.getSource());
			File newFile = new File(Utils.getEntryImagesMediaFolder( mId ).getPath() + "/" + old.getName());
			ImageSpan newSpan = new ImageSpan( span.getDrawable(), newFile.getPath() );
			e.removeSpan( span );
			e.setSpan( newSpan, st, end, flags );
		}
	}

	private void saveEntry(){
		Editable e = mTextEditor.getText();
		if(e == null)
			return;
		String text = e.toString();
		while(e.length() > 0 && (e.charAt( 0 ) == ' ' || e.charAt( 0 ) == '\n')){
			if(e.getSpans( 0, 1, ImageSpan.class ).length == 0)
				e.delete( 0, 1 );
			else
				break;
		}
		while(e.length() > 0 && (e.charAt( e.length()-1 ) == ' ' || e.charAt( e.length() - 1 ) == '\n')){
			if(e.getSpans( e.length()-1, e.length(), ImageSpan.class ).length == 0)
				e.delete( e.length()-1, e.length() );
			else
				break;
		}
		if(type.equals( "create" )) {
			if ( text.length() != 0 ) {
				AlertDialog alertDialog;
				final EditText name = new EditText( this );
				name.setId( View.NO_ID );
				name.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT ) );
				name.requestFocus();
				name.setTextColor( getColor( super.mTextColor ) );
				name.setMaxLines( 1 );
				AlertDialog.Builder builder = new AlertDialog.Builder( this, super.mAlertDialogStyle )
						.setView( name )
						.setTitle( R.string.enter_name )
						.setMessage( R.string.name_yours_minds )
						.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
								final String n = name.getText().toString();
								if( n.isEmpty() || n.trim().equals( "" ) )
									Toast.makeText( EntryEditor.this, R.string.invalid_name, Toast.LENGTH_SHORT ).show();
								else{
									if ( Utils.isNameExist( n, "ent" ) ) {
										AlertDialog.Builder builder1 = new AlertDialog.Builder( EntryEditor.this )
												.setTitle( R.string.warning )
												.setMessage( getResources().getString( R.string.this_name_already_exist ) + "\n" +
														getResources().getString( R.string.do_you_want_to_continue ))
												.setPositiveButton( R.string.yes, new DialogInterface.OnClickListener() {
													@Override
													public void onClick(DialogInterface dialog, int which) {
														createEntry( n, mTextEditor.getText() );
														dialog.dismiss();
													}
												} )
												.setNegativeButton( R.string.no, new DialogInterface.OnClickListener() {
													@Override
													public void onClick(DialogInterface dialog, int which) {
														dialog.dismiss();
													}
												} );
										builder1.create().show();
									}else{
										createEntry( n, mTextEditor.getText() );
									}
								}
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
						Utils.showKeyboard( name, EntryEditor.this );
					}
				} );
				alertDialog.show();
			} else {
				mTextEditor.requestFocus();
			}
		}else if(type.equals( "edit" )){
			if(text.length() != 0){
				removeUnusedImages();
				try {
					mEntry.saveProperties( mProperties );
					mEntry.saveText( mTextEditor.getText(), mProperties );
					setResult( Results.REOPEN, new Intent(  ).putExtra( "id", mEntry.getId() ) );
				}catch (Exception ex){
					ex.printStackTrace();
					Utils.getErrorDialog( ex, this ).show();
					return;
				}
				_finishActivity();
			}
		}
	}

	final View.OnClickListener saveEntry = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			mTextEditor.clearComposingText();
			saveEntry();
		}
	};
}
