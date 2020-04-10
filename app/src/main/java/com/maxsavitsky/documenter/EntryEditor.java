package com.maxsavitsky.documenter;

import android.Manifest;
import android.animation.Animator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
import com.maxsavitsky.documenter.media.images.HtmlImageLoader;
import com.maxsavitsky.documenter.media.images.ImageRenderer;
import com.maxsavitsky.documenter.utils.SpanEntry;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.widget.TextEditor;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.maxsavitsky.documenter.codes.Requests.*;

public class EntryEditor extends ThemeActivity {
	private Document mDocument;
	private String type, mOriginalText;
	private Entry mEntry;
	private String mId;
	private String title = "Create new entry";
	private Entry.Properties mStartProperties;
	private TextEditor mTextEditor;
	private Button btnBgColorPicker, btnTextColorPicker, btnTextBackgroundColorPicker;
	private ImageButton btnBold, btnItalic, btnUnderline, btnStrike;
	private BottomSheetBehavior<View> mBottomSheetLayout;
	private final ArrayList<ChangeEntry> mHistory = new ArrayList<>();
	private int mHistoryIterator = -1;
	private Menu mMenu;
	private final int UNDO_MENU_INDEX = 0;
	private final int REDO_MENU_INDEX = 1;
	private Editable mStartEditable = new Editable.Factory().newEditable( "" );
	private SharedPreferences sp;
	private final ArrayList<Integer> mColorHistory = new ArrayList<>();
	private final ArrayList<File> mMediaToMove = new ArrayList<>();
	private int[] mSelectionBounds = new int[]{0, 0};
	private Alignment mMainAlignment = Alignment.ALIGN_NORMAL;

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
		if(super.isDarkTheme) {
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
		String t = "";
		if(mTextEditor.getText() != null){
			t = Html.toHtml( mTextEditor.getText() );
		}
		boolean firstCondition = type.equals( "create" )
				&& mTextEditor.getText() != null && !mTextEditor.getText().toString().equals( "" );

		boolean secondCondition = type.equals( "edit" )
				&& (!t.equals( mOriginalText ) || !mStartProperties.equals( mEntry.getProperties() ) );
		if ( firstCondition || secondCondition ) {
			showExitAlertDialog();
		} else {
			setResult( Results.OK );
			_finishActivity();
		}
	}

	private void showExitAlertDialog(){
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
		btnTextBackgroundColorPicker = findViewById(R.id.btnTextBackgroundColorPicker);

		btnBold = findViewById( R.id.btnBold );
		btnItalic = findViewById( R.id.btnItalic );
		btnUnderline = findViewById( R.id.btnUnderline );
		btnStrike = findViewById( R.id.btnStrike );

		type = getIntent().getStringExtra( "type" );
		mTextEditor = findViewById( R.id.edittextEntry );
		mTextEditor.setListener( mOnSelectionChanges );
		mTextEditor.setMovementMethod( new LinkMovementMethod(){
			@Override
			public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
				Selection.removeSelection(buffer);
				widget.setHighlightColor(Color.argb(0,0,0,0));
				return super.onTouchEvent(widget, buffer, event);
			}
		} );
		if(type == null){
			Toast.makeText( this, getString(R.string.something_gone_wrong), Toast.LENGTH_SHORT ).show();
			_finishActivity();
			return;
		}
		if ( type.equals( "edit" ) ) {
			mEntry = MainData.getEntryWithId( getIntent().getStringExtra( "id" ) );
			try {
				mEntry.readProperties();
			}catch (IOException | SAXException e){
				Utils.getErrorDialog( e, this ).show();
				return;
			}
			mId = mEntry.getId();
			mStartProperties = new Entry.Properties( mEntry.getProperties() );
			mDefaultTextColor = mEntry.getProperties().getDefaultTextColor();
			mTextEditor.setTextColor( mDefaultTextColor );
			getWindow().getDecorView().setBackgroundColor( mEntry.getProperties().getBgColor() );
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
			mEntry = new Entry( "temp_entry", "" );
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
		fab.setOnLongClickListener( new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Toast.makeText( EntryEditor.this, "id: " + mId, Toast.LENGTH_SHORT ).show();
				return true;
			}
		} );
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
		setAlignmentButtonClicked( getAlignmentButtonId( mMainAlignment ) );
	}

	private void readColorHistory(){
		String history = sp.getString( "color_history", null );
		if(history != null){
			String[] strings = history.split( ";" );
			for(String str : strings){
				mColorHistory.add( Integer.parseInt( str ) );
			}
		}
	}

	private void addColorToHistory(int color){
		if(mColorHistory.size() > 0 && mColorHistory.get( 0 ) == color)
			return;
		ArrayList<Integer> clone = new ArrayList<>(mColorHistory);
		mColorHistory.clear();
		mColorHistory.add( color );
		for(int i = 0; i < Math.min(4, clone.size()); i++){
			mColorHistory.add( clone.get( i ) );
		}
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

		Spannable spannable = (Spannable) Html.fromHtml( text, new HtmlImageLoader( this ), null );
		for(SpanEntry se : mEntry.getAlignments()){
			spannable.setSpan( se.getAlignmentSpan(), se.getStart(), se.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		}
		mTextEditor.setTextW( spannable );
	}

	private final OnLoadedTextListener mOnLoadedTextListener = new OnLoadedTextListener() {
		@Override
		public void loaded(final Spannable spannable, final String originalText) {
			double seconds = (System.currentTimeMillis() - mStartLoadTextTime) / 1000.0;
			Log.v( "TextLoader", "mOnLoadedTextListener.loaded called. Seconds = " + seconds );
			mOriginalText = originalText;

			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					setTextInEditor( originalText );
					mTextEditor.setScrollY( mEntry.getProperties().getScrollPosition() );
					mStartEditable = mTextEditor.getText();
					setEditTextSize();
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

					calculateMainAlignment();
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

	private void calculateMainAlignment(){
		Editable e = mTextEditor.getText();
		if(e == null)
			return;
		AlignmentSpan.Standard[] spans = e.getSpans( 0, e.length(), AlignmentSpan.Standard.class );
		if(spans.length == 1){
			mMainAlignment = spans[0].getAlignment();
		}
		setAlignmentButtonClicked( getAlignmentButtonId( mMainAlignment ) );
	}

	private int getAlignmentButtonId(Alignment alignment){
		if(alignment == Alignment.ALIGN_NORMAL)
			return R.id.btnAlignLeft;
		else if(alignment == Alignment.ALIGN_CENTER)
			return R.id.btnAlignCenter;
		else
			return R.id.btnAlignRight;
	}

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
		picker.setType( "image/*" );
		startActivityForResult( picker, PICK_IMAGE );
	}

	private boolean isReadMemoryAccess(){
		return ContextCompat.checkSelfPermission( this, Manifest.permission.READ_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
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
					String type = getContentResolver().getType(uri);
					if(in == null || type == null) {
						Toast.makeText( this, "Some problems with reading", Toast.LENGTH_SHORT ).show();
						return;
					}
					File file = Utils.getEntryImagesMediaFolder( mId );
					file = new File(file.getPath() + "/" + Utils.generateUniqueId());
					if(type.equals( "image/png" )){
						file = new File( file.getPath() + ".png" );
					}else if(type.equals( "image/jpeg" )){
						file = new File(file.getPath() + ".jpg");
					}else{
						Toast.makeText( this, R.string.file_type_is_not_supported, Toast.LENGTH_LONG ).show();
						in.close();
						return;
					}
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
					Utils.getErrorDialog( e, this ).show();
					Toast.makeText( this, "Picture could be damaged.\n" +
														"We recommend deleting and re-adding.", Toast.LENGTH_LONG ).show();
				}
			}
		}
		super.onActivityResult( requestCode, resultCode, data );
	}

	private void setImageAtSelBounds(File file){
		Editable e = mTextEditor.getText();
		int s;
		if(e == null) {
			mTextEditor.setText( " " );
			mSelectionBounds[0] = 0;
			s = 0;
			e = mTextEditor.getText();
		}else{
			s = mSelectionBounds[0];
			e.insert( s, " " );
		}

		Drawable d = ImageRenderer.renderDrawable( file.getPath() );

		ImageSpan imageSpan = new ImageSpan(d, file.getPath());

		e.setSpan( imageSpan, s, s + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
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
			mTextEditor.clearComposingText();
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
			final BackgroundColorSpan[] backgroundColorSpans = s.getSpans( start, end, BackgroundColorSpan.class );
			final int editTextColor = (( ColorDrawable ) mTextEditor.getBackground()).getColor();
			final int color = (backgroundColorSpans.length == 0 ? editTextColor : backgroundColorSpans[0].getBackgroundColor());
			btnTextBackgroundColorPicker.setBackgroundTintList( ColorStateList.valueOf( color ) );
			btnTextBackgroundColorPicker.setOnClickListener( new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					View.OnClickListener listener = new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Editable e = mTextEditor.getText();
							if(e == null)
								return;
							int selSt = mSelectionBounds[0];
							int selEnd = mSelectionBounds[1];
							ArrayList<SpanEntry> arrayList = new ArrayList<>();
							for(BackgroundColorSpan span : e.getSpans( selSt, selEnd, BackgroundColorSpan.class )){
								int st = e.getSpanStart( span );
								int end = e.getSpanEnd( span );
								if(st < selSt){
									arrayList.add( new SpanEntry( new BackgroundColorSpan( span.getBackgroundColor() ), st, selSt ) );
								}
								if(end > selEnd){
									arrayList.add( new SpanEntry( new BackgroundColorSpan( span.getBackgroundColor() ), selEnd, end ) );
								}
								e.removeSpan( span );
							}
							for(SpanEntry se : arrayList){
								e.setSpan( se.getBackgroundColorSpan(), se.getStart(), se.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
							}
							e.setSpan( new BackgroundColorSpan( mSelectedColor ), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
							btnTextBackgroundColorPicker.setBackgroundTintList( ColorStateList.valueOf( mSelectedColor ) );
						}
					};
					AlertDialog alertDialog = getColorPickerDialog( R.string.choose_text_background_of_segment, color, listener );
					if(backgroundColorSpans.length > 0){
						alertDialog.setButton( AlertDialog.BUTTON_NEUTRAL, getString( R.string.delete ), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								Editable e = mTextEditor.getText();
								if(e == null)
									return;
								int selSt = mSelectionBounds[0];
								int selEnd = mSelectionBounds[1];
								ArrayList<SpanEntry> arrayList = new ArrayList<>();
								for(BackgroundColorSpan span : e.getSpans( selSt, selEnd, BackgroundColorSpan.class )){
									int st = e.getSpanStart( span );
									int end = e.getSpanEnd( span );
									if(st < selSt){
										arrayList.add( new SpanEntry( span, st, selSt ) );
									}
									if(end > selEnd){
										arrayList.add( new SpanEntry( span, selEnd, end ) );
									}
									e.removeSpan( span );
								}
								for(SpanEntry se : arrayList){
									e.setSpan( se.getBackgroundColorSpan(), se.getStart(), se.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
								}
								btnTextBackgroundColorPicker.setBackgroundTintList( ColorStateList.valueOf( editTextColor ) );
							}
						} );
					}
					alertDialog.show();
				}
			} );
			applyStyleBtnState( start, end );
			btnBold.setOnClickListener(onTextAppearanceClick);
			btnItalic.setOnClickListener(onTextAppearanceClick);
			btnUnderline.setOnClickListener( onUnderlineBtnClick );
			btnStrike.setOnClickListener( onStrikeBtnClick );
			findViewById( R.id.textBackgroundLayout ).animate().scaleY( 1 ).setDuration( 500 ).setListener( new Animator.AnimatorListener() {
				@Override
				public void onAnimationStart(Animator animation) {
					findViewById( R.id.textBackgroundLayout ).setVisibility( View.VISIBLE );
				}

				@Override
				public void onAnimationEnd(Animator animation) {

				}

				@Override
				public void onAnimationCancel(Animator animation) {

				}

				@Override
				public void onAnimationRepeat(Animator animation) {

				}
			} ).start();

			AlignmentSpan.Standard[] spans = s.getSpans( mSelectionBounds[0], mSelectionBounds[1], AlignmentSpan.Standard.class );
			if(spans.length > 0){
				Layout.Alignment a = spans[0].getAlignment();
				setAlignmentButtonClicked( getAlignmentButtonId( a ) );
			}
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
			findViewById( R.id.textBackgroundLayout ).animate().scaleY( 0 ).setDuration( 500 ).setListener( new Animator.AnimatorListener()     {
				@Override
				public void onAnimationStart(Animator animation) {

				}

				@Override
				public void onAnimationEnd(Animator animation) {
					findViewById( R.id.textBackgroundLayout ).setVisibility( View.GONE );
				}

				@Override
				public void onAnimationCancel(Animator animation) {

				}

				@Override
				public void onAnimationRepeat(Animator animation) {

				}
			} ).start();

			if(mTextEditor.getText() != null && mTextEditor.getText().getSpans( newSelectionPosition, newSelectionPosition, AlignmentSpan.Standard.class ).length > 0)
				setAlignmentButtonClicked(
						getAlignmentButtonId(
								mTextEditor.getText().getSpans( newSelectionPosition, newSelectionPosition, AlignmentSpan.Standard.class )[0].getAlignment()
						)
				);
			else
				setAlignmentButtonClicked( getAlignmentButtonId( mMainAlignment ) );
		}

		@Override
		public void onTextChanged(CharSequence text, final int start, final int lengthBefore, final int lengthAfter) {
			Editable e = mTextEditor.getText();
			if(lengthAfter > lengthBefore){
				int len = lengthAfter - lengthBefore;
				int end = start + len;
				if(e == null)
					return;

				e.setSpan( new ForegroundColorSpan( mDefaultTextColor ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
				e.setSpan( new AlignmentSpan.Standard( mMainAlignment ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
				e.setSpan( new AbsoluteSizeSpan( mEntry.getProperties().getTextSize(), true ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
			}
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
					mTextEditor.setBackgroundColor( mSelectedColor );
					getWindow().getDecorView().setBackgroundColor( mSelectedColor );
					mEntry.getProperties().setBgColor( mSelectedColor );
				}
			};
			AlertDialog alertDialog = getColorPickerDialog( R.string.set_background_color, mEntry.getProperties().getBgColor(), whatToDo );
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

	private void initializeColorButtons(final View layout, final View.OnClickListener clickListener){
		int[] btnIds = new int[]{ R.id.btnColorHistory1, R.id.btnColorHistory2, R.id.btnColorHistory3, R.id.btnColorHistory4, R.id.btnColorHistory5 };
		for(int i = 0; i < btnIds.length; i++){
			int id = btnIds[i];
			Button btn = layout.findViewById(id);
			//btn.setTag( 1, i );
			if(i >= mColorHistory.size()){
				btn.setVisibility( View.INVISIBLE );
			}else{
				btn.setBackgroundTintList( ColorStateList.valueOf( mColorHistory.get( i ) ) );
				btn.setTag( R.id.color_int_in_history, mColorHistory.get( i ) );
				btn.setTag( R.id.color_pos_in_history, i );
				btn.setOnClickListener( clickListener );
				btn.setOnLongClickListener( new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(final View v) {
						PopupMenu popupMenu = new PopupMenu( EntryEditor.this, v );
						popupMenu.getMenu().add( EntryEditor.this.getString( R.string.delete ) );
						popupMenu.setOnMenuItemClickListener( new PopupMenu.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem item) {
								mColorHistory.remove( (int) v.getTag(R.id.color_pos_in_history) );
								saveColorHistory();
								initializeColorButtons( (View) v.getParent(), clickListener );
								return true;
							}
						} );
						popupMenu.show();
						return true;
					}
				} );
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
					mSelectedColor = (int) v.getTag(R.id.color_int_in_history);
					whatToDo.onClick(null);
				}
			} );
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
							mEntry.getProperties().setDefaultTextColor( mSelectedColor );
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

		btnBgColorPicker.setBackgroundTintList( ColorStateList.valueOf( mEntry.getProperties().getBgColor() ) );
		mTextEditor.setBackgroundColor( mEntry.getProperties().getBgColor() );
		getWindow().getDecorView().setBackgroundColor( mEntry.getProperties().getBgColor() );
	}

	public void plusTextSize(View view){
		int ts = mEntry.getProperties().getTextSize();
		if( ts < 45)
			mEntry.getProperties().setTextSize( ts + 1 );
		setEditTextSize();
	}

	public void minusTextSize(View view){
		int ts = mEntry.getProperties().getTextSize();
		if( ts > 15 )
			mEntry.getProperties().setTextSize( ts - 1 );
		setEditTextSize();
	}

	private void setEditTextSize(){
		TextView t = findViewById( R.id.textViewTextSize );
		t.setText( String.format( Locale.ROOT, "%d", mEntry.getProperties().getTextSize() ) );
		Editable e = mTextEditor.getText();
		if(e == null)
			return;

		AbsoluteSizeSpan absoluteSizeSpan = new AbsoluteSizeSpan( mEntry.getProperties().getTextSize(), true );
		AbsoluteSizeSpan[] spans = e.getSpans( 0, e.length(), AbsoluteSizeSpan.class );
		for(AbsoluteSizeSpan span : spans){
			e.removeSpan( span );
		}
		e.setSpan( absoluteSizeSpan, 0, e.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
	}

	private void createEntry(String name, Spannable text){
		mTextEditor.clearComposingText();
		Entry.Properties props = mEntry.getProperties();
		mEntry = new Entry( mId, name );
		ArrayList<Entry> entries = MainData.getEntriesList();
		entries.add( mEntry );
		MainData.setEntriesList( entries );
		Utils.saveEntriesList( entries );
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries/" + mId );
		if(!file.exists())
			file.mkdirs();
		try {
			mEntry.setProperties( props );
			mEntry.saveProperties();
		}catch (IOException e){
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
			mEntry.saveInWhichDocumentsIncludedThisEntry( new ArrayList<Document>() );
			mEntry.saveContent( text );
			mEntry.setAndSaveInfo( new Info( (int) System.currentTimeMillis() ) );
			if(!mWithoutDoc) {
				mEntry.addDocumentToIncluded( mDocument.getId() );
				mDocument.addEntry( mEntry );
			}
			setResult( Results.REOPEN, new Intent().putExtra( "id", mId ) );
			finish();
		}catch (IOException | SAXException e){
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
		Editable e = mTextEditor.getText();
		if(e == null)
			return;
		if(mSelectionBounds[0] != mSelectionBounds[1]){
			setGravityOfSegment( v.getId() );
			return;
		}
		setAlignmentButtonClicked( v.getId() );

		Layout.Alignment alignment = Layout.Alignment.ALIGN_NORMAL;
		if(v.getId() == R.id.btnAlignCenter)
			alignment = Layout.Alignment.ALIGN_CENTER;
		else if(v.getId() == R.id.btnAlignRight)
			alignment = Layout.Alignment.ALIGN_OPPOSITE;

		mMainAlignment = alignment;

		AlignmentSpan.Standard[] spans = e.getSpans( 0, e.length(), AlignmentSpan.Standard.class );
		for(AlignmentSpan.Standard span : spans){
			e.removeSpan( span );
		}
		e.setSpan( new AlignmentSpan.Standard( alignment ), 0, e.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
	}

	private void setAlignmentButtonClicked(int id){
		resetAlignmentButtons();
		View v = findViewById( id );
		if(super.isDarkTheme)
			v.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.gray ) ) );
		else
			v.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );
	}

	private void setGravityOfSegment(int id){
		Editable e = mTextEditor.getText();
		if(e == null)
			return;
		Layout.Alignment alignment = Layout.Alignment.ALIGN_NORMAL;
		if(id == R.id.btnAlignCenter)
			alignment = Layout.Alignment.ALIGN_CENTER;
		else if(id == R.id.btnAlignRight)
			alignment = Layout.Alignment.ALIGN_OPPOSITE;

		int selSt = mSelectionBounds[0];
		int selEnd = mSelectionBounds[1];
		AlignmentSpan.Standard[] spans = e.getSpans( mSelectionBounds[0], mSelectionBounds[1], AlignmentSpan.Standard.class );
		ArrayList<SpanEntry> spansToApply = new ArrayList<>();
		for(AlignmentSpan.Standard span : spans){
			int st = e.getSpanStart( span );
			int end = e.getSpanEnd( span );
			if(st < selSt){
				SpanEntry se = new SpanEntry( span, st, selSt );
				spansToApply.add( se );
			}
			if(end > selEnd){
				SpanEntry se = new SpanEntry( span, selEnd, end );
				spansToApply.add(se);
			}
			e.removeSpan( span );
		}
		for(SpanEntry se : spansToApply){
			e.setSpan( se.getAlignmentSpan(), se.getStart(), se.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		}
		e.setSpan( new AlignmentSpan.Standard( alignment ), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
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
			removeUnusedImages();
			if(text.length() != 0){
				try {
					mEntry.saveProperties( mEntry.getProperties() );
					mEntry.saveContent( mTextEditor.getText() );
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
