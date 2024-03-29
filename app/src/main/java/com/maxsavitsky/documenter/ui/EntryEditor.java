package com.maxsavitsky.documenter.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.UnderlineSpan;
import android.transition.Fade;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.flask.colorpicker.ColorPickerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.App;
import com.maxsavitsky.documenter.BuildConfig;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.ThemeActivity;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.data.EntitiesStorage;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.data.types.Group;
import com.maxsavitsky.documenter.media.images.ImageRenderer;
import com.maxsavitsky.documenter.ui.editor.TextEditor;
import com.maxsavitsky.documenter.utils.ChangeEntry;
import com.maxsavitsky.documenter.utils.SpanEntry;
import com.maxsavitsky.documenter.utils.Utils;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class EntryEditor extends ThemeActivity {
	private String type, mOriginalText;
	private Entry mEntry, mCopyToEntry;
	private String mId;
	private String title = "Create new entry";
	private Entry.Properties tempProperties;
	private TextEditor mTextEditor;
	private ImageButton btnBgColorPicker, btnTextColorPicker;
	private ImageButton btnSubScript, btnSupScript;
	private ImageButton btnTextHighlight, btnReplaceColor;
	private ImageButton btnBold, btnItalic, btnUnderline, btnStrike;
	private final ArrayList<ChangeEntry> mHistory = new ArrayList<>();
	private int mHistoryIterator = -1;
	private Menu mMenu;
	private final int UNDO_MENU_INDEX = R.id.item_undo;
	private final int REDO_MENU_INDEX = R.id.item_redo;
	private Editable mStartEditable = new Editable.Factory().newEditable( "" );
	private SharedPreferences sp;
	private final ArrayList<Integer> mColorHistory = new ArrayList<>();
	private final ArrayList<File> mMediaToMove = new ArrayList<>();
	private int[] mSelectionBounds = new int[]{ 0, 0 };
	private Alignment mMainAlignment = Alignment.ALIGN_NORMAL;
	private int mDefaultTextColor = Color.BLACK;
	private final float INDEX_PROPORTION = 0.75f;
	private final int DEFAULT_TOOLS_COLOR = Color.WHITE;
	private final String TAG = App.TAG + " EntryEditor";
	private Group mParentGroup;

	private final ActivityResultLauncher<String> mPickImageLauncher = registerForActivityResult(
			new ActivityResultContracts.GetContent(),
			uri->{
				if ( uri == null ) {
					Toast.makeText( this, "Some error occurred", Toast.LENGTH_SHORT ).show();
					return;
				}
				try (InputStream in = getContentResolver().openInputStream( uri )) {
					String type = getContentResolver().getType( uri );
					if ( in == null || type == null ) {
						Toast.makeText( this, "Some problems with reading", Toast.LENGTH_SHORT ).show();
						return;
					}
					File file = Utils.getEntryImagesMediaFolder( mEntry.getId() );
					String format = MimeTypeMap.getSingleton().getExtensionFromMimeType( getContentResolver().getType( uri ) );
					file = new File( file, Utils.generateUniqueId() + "." + format );
					if ( mEntry.getId().equals( "temp_entry" ) ) {
						mMediaToMove.add( file );
					}
					FileOutputStream fos = new FileOutputStream( file );
					int len;
					byte[] buffer = new byte[ 1024 ];
					while ( ( len = in.read( buffer ) ) != -1 ) {
						fos.write( buffer, 0, len );
					}
					fos.close();
					in.close();

					setImageAtSelBounds( file );
				} catch (Exception e) {
					e.printStackTrace();
					Utils.getErrorDialog( e, this ).show();
					Toast.makeText( this, "Picture could be damaged.\n" +
							"We recommend deleting and re-adding.", Toast.LENGTH_LONG ).show();
				}
			}
	);

	private interface OnLoadedTextListener {
		void loaded(Spannable spannable, Entry entry);

		void exceptionOccurred(Exception e);
	}

	private void applyTheme() {
		ActionBar actionBar = getSupportActionBar();
		if ( actionBar != null ) {
			Utils.applyDefaultActionBarStyle( actionBar );
			actionBar.setTitle( title );
		}
	}

	private void _finishActivity() {
		Utils.clearTempFolder();
		finish();
	}

	private void backPressed() {
		if ( mTextEditor.getText() == null && mStartEditable == null ) {
			setResult( Results.OK );
			_finishActivity();
			return;
		}
		String t = "";
		if ( mTextEditor.getText() != null ) {
			mTextEditor.clearComposingText();
			t = Html.toHtml( trim( mTextEditor.getText() ) );
		}
		boolean firstCondition = type.equals( "create" ) &&
				!mTextEditor.getText().toString().isEmpty() &&
				!tempProperties.equals( new Entry.Properties() );

		Log.i( TAG, "backPressed: " + ( mTextEditor.getText().hashCode() ) );

		boolean secondCondition = type.equals( "edit" );
		//&& ( !t.equals( mOriginalText ) || !mStartProperties.equals( mEntry.getProperties() ) );

		if ( firstCondition || secondCondition ) {
			showExitAlertDialog();
		} else {
			ScrollView scrollView = findViewById( R.id.scrollView );
			setResult( Results.OK, new Intent().putExtra( "scroll_position", scrollView.getScrollY() ) );
			_finishActivity();
		}
	}

	private void showExitAlertDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder( this, super.mAlertDialogStyle )
				.setTitle( R.string.confirmation )
				.setMessage( R.string.create_entry_exit_mes ).setCancelable( false )
				.setNegativeButton( R.string.cancel, (dialog, which)->dialog.cancel() )
				.setPositiveButton( R.string.yes, (dialog, which)->{
					ScrollView scrollView = findViewById( R.id.scrollView );
					setResult( Results.OK, new Intent().putExtra( "scroll_position", scrollView.getScrollY() ) );
					_finishActivity();
				} ).setNeutralButton( R.string.save_and_exit, (dialog, which)->{
					dialog.cancel();
					saveEntry();
				} );

		builder.create().show();
	}

	private Spannable trim(Spannable e) {
		return trim( Editable.Factory.getInstance().newEditable( e ) );
	}

	private Editable trim(Editable editable) {
		if ( editable == null ) {
			return Editable.Factory.getInstance().newEditable( "" );
		}

		Editable e = Editable.Factory.getInstance().newEditable( editable );

		while ( e.length() > 0 && ( e.charAt( 0 ) == ' ' || e.charAt( 0 ) == '\n' ) ) {
			if ( e.getSpans( 0, 1, ImageSpan.class ).length == 0 ) {
				e.delete( 0, 1 );
			} else {
				break;
			}
		}
		while ( e.length() > 0 && ( e.charAt( e.length() - 1 ) == ' ' || e.charAt( e.length() - 1 ) == '\n' ) ) {
			if ( e.getSpans( e.length() - 1, e.length(), ImageSpan.class ).length == 0 ) {
				e.delete( e.length() - 1, e.length() );
			} else {
				break;
			}
		}

		return e;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if ( item.getItemId() == android.R.id.home ) {
			backPressed();
		} else if ( item.getItemId() == R.id.item_undo ) {
			/*ChangeEntry changeEntry = mHistory.get( --mHistoryIterator );
			mTextEditor.setTextW( changeEntry.getSpannableString() );
			if(changeEntry.getCursorPosition() > mTextEditor.length())
				changeEntry.mCursorPosition = mTextEditor.length();
			mTextEditor.setSelection( changeEntry.getCursorPosition() );*/
			if ( mHistoryIterator != 0 ) {
				loadTextFromHistory( --mHistoryIterator );
				if ( mHistoryIterator == 0 ) {
					disableThisMenuItem( UNDO_MENU_INDEX );
				}
				enableThisMenuItem( REDO_MENU_INDEX );
			}
		} else if ( item.getItemId() == R.id.item_redo ) {
			/*mTextEditor.setTextW( mHistory.get( ++mHistoryIterator ).getSpannableString() );
			mTextEditor.setSelection( mHistory.get( mHistoryIterator ).getCursorPosition() );*/
			loadTextFromHistory( ++mHistoryIterator );
			if ( mHistoryIterator == mHistory.size() - 1 ) {
				disableThisMenuItem( REDO_MENU_INDEX );
			}
			enableThisMenuItem( UNDO_MENU_INDEX );
		} else if ( item.getItemId() == R.id.item_apply ) {
			mTextEditor.clearComposingText();
			saveEntry();
		}
		return super.onOptionsItemSelected( item );
	}

	private void enableThisMenuItem(int i) {
		if ( mMenu == null ) {
			return;
		}
		mMenu.findItem( i ).setEnabled( true );
		mMenu.findItem( i ).setIcon( i == UNDO_MENU_INDEX ? R.drawable.ic_undo : R.drawable.ic_redo );
		//invalidateOptionsMenu();
	}

	private void disableThisMenuItem(int i) {
		if ( mMenu == null ) {
			return;
		}

		mMenu.findItem( i ).setEnabled( false );
		mMenu.findItem( i ).setIcon( i == UNDO_MENU_INDEX ? R.drawable.ic_disabled_undo : R.drawable.ic_disabled_redo );
		//invalidateOptionsMenu();
	}


	private void saveTextChange() {
		Spannable spannable = Spannable.Factory.getInstance().newSpannable( mTextEditor.getText() );
		int pos = mTextEditor.getSelectionEnd();
		if ( mHistoryIterator != mHistory.size() - 1 ) {
			ArrayList<ChangeEntry> historyCopy = new ArrayList<>();
			for (int i = 0; i <= mHistoryIterator; i++) {
				historyCopy.add( mHistory.get( i ) );
			}
			mHistory.clear();
			mHistory.addAll( historyCopy );
		}
		enableThisMenuItem( UNDO_MENU_INDEX );
		disableThisMenuItem( REDO_MENU_INDEX );
		ChangeEntry changeEntry = new ChangeEntry( spannable, pos );
		changeEntry.setScrollY( ( (ScrollView) findViewById( R.id.scrollView ) ).getScrollY() );
		mHistory.add( changeEntry );
		while ( mHistory.size() > 100 ) {
			mHistory.remove( 0 );
		}
		mHistoryIterator = mHistory.size() - 1;
	}

	private void loadTextFromHistory(int historyIterator) {
		final ChangeEntry changeEntry = mHistory.get( historyIterator );
		final Spannable source = changeEntry.getSpannable();
		final int scrollY = mTextEditor.getScrollY();
		final Editable s = mTextEditor.getText();
		if ( s != null ) {
			s.clear();
		}

		final OnLoadedTextListener listener = new OnLoadedTextListener() {
			@Override
			public void loaded(Spannable spannable, Entry entry) {
				mTextEditor.setSelection( changeEntry.getCursorPosition() );
				runOnUiThread( ()->( (ScrollView) findViewById( R.id.scrollView ) ).setScrollY( changeEntry.getScrollY() ) );
			}

			@Override
			public void exceptionOccurred(Exception e) {
				if ( s != null ) {
					s.clear();
				}

				Utils.getErrorDialog( e, EntryEditor.this ).show();
			}
		};
		new Thread( ()->{
			try {
				setTextInEditor( source );
				listener.loaded( null, null );
			} catch (Exception e) {
				listener.exceptionOccurred( e );
				e.printStackTrace();
			}
		} ).start();
	}

	@Override
	public void onBackPressed() {
		backPressed();
	}

	private ProgressDialog mProgressDialogOnTextLoad;
	private long mStartLoadTextTime;
	private boolean mWithoutDoc = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		//setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT );
		setContentView( R.layout.activity_create_entry );
		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );

		sp = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

		btnSupScript = findViewById( R.id.btnToolsSuperscript );
		btnSubScript = findViewById( R.id.btnToolsSubscript );

		btnBold = findViewById( R.id.btnToolsBold );
		btnItalic = findViewById( R.id.btnToolsItalic );
		btnUnderline = findViewById( R.id.btnToolsUnderline );
		btnStrike = findViewById( R.id.btnToolsStrike );

		btnBgColorPicker = findViewById( R.id.btnToolsBackgroundColor );
		btnBgColorPicker.setOnClickListener( btnBgColorPickerDefaultClickListener );

		btnTextColorPicker = findViewById( R.id.btnToolsTextColor );

		btnTextHighlight = findViewById( R.id.btnToolsHighlight );

		btnReplaceColor = findViewById( R.id.btnToolsReplaceColor );

		Intent startIntent = getIntent();
		type = startIntent.getStringExtra( "type" );
		mTextEditor = findViewById( R.id.edittextEntry );
		mTextEditor.setListener( mOnSelectionChanges );
		if ( type == null ) {
			Toast.makeText( this, getString( R.string.something_gone_wrong ), Toast.LENGTH_SHORT ).show();
			_finishActivity();
			return;
		}
		mId = startIntent.getStringExtra( "id" );

		if ( "edit".equals( type ) ) {
			Optional<Entry> op = EntitiesStorage.get().getEntry( mId );
			if ( !op.isPresent() ) {
				Toast.makeText( this, "Entry not found", Toast.LENGTH_SHORT ).show();
				super.onBackPressed();
				return;
			}
			mEntry = op.get();
			try {
				mEntry.loadProperties();
			} catch (IOException | JSONException e) {
				Utils.getErrorDialog( e, this ).show();
				return;
			}
			tempProperties = new Entry.Properties( mEntry.getProperties() );
			mDefaultTextColor = tempProperties.getDefaultTextColor();
			mTextEditor.setTextColor( mDefaultTextColor );
			setImageButtonColor( tempProperties.getBgColor(), btnBgColorPicker.getId() );

			title = getResources().getString( R.string.edit_entry ) + ": " + mEntry.getName();

			loadTextFromEntryInEditor( mId );
		} else if ( "create".equals( type ) ) {
			String parentId = startIntent.getStringExtra( "parentId" );
			Optional<Group> parentOptional = EntitiesStorage.get().getGroup( parentId );
			if ( !parentOptional.isPresent() ) {
				Toast.makeText( this, "Parent group not found", Toast.LENGTH_SHORT ).show();
				super.onBackPressed();
				return;
			}
			mParentGroup = parentOptional.get();

			title = getResources().getString( R.string.create_entry );
			mEntry = new Entry( "temp_entry", "" );
			tempProperties = new Entry.Properties();
			mId = Utils.generateUniqueId() + "_0";
		} else if ( "copy".equals( type ) ) {
			String toId = startIntent.getStringExtra( "to_id" );
			String fromId = startIntent.getStringExtra( "from_id" );
			Optional<Entry> op = EntitiesStorage.get().getEntry( fromId );
			if ( !op.isPresent() ) {
				Toast.makeText( this, "Entry not found", Toast.LENGTH_SHORT ).show();
				super.onBackPressed();
				return;
			}
			mEntry = op.get();
			tempProperties = new Entry.Properties( mEntry.getProperties() );

			op = EntitiesStorage.get().getEntry( toId );
			if ( !op.isPresent() ) {
				Toast.makeText( this, "Destination entry not found", Toast.LENGTH_SHORT ).show();
				super.onBackPressed();
				return;
			}

			mCopyToEntry = op.get();
			title = getResources().getString( R.string.edit_entry ) + ": " + mCopyToEntry.getName();
			loadTextFromEntryInEditor( fromId );
		}

		mTextEditor.setBackgroundColor( tempProperties.getBgColor() );
		getWindow().getDecorView().setBackgroundColor( tempProperties.getBgColor() );

		invalidateOptionsMenu();
		applyTheme();
		readColorHistory();

		ScrollView scrollView = findViewById( R.id.scrollView );
		scrollView.setSmoothScrollingEnabled( true );

		setEditTextSize();
		FloatingActionButton fab = findViewById( R.id.fabSaveEntry );
		fab.setOnClickListener( saveEntry );
		fab.setOnLongClickListener( v->{
			Toast.makeText( EntryEditor.this, "id: " + mId, Toast.LENGTH_SHORT ).show();
			return true;
		} );
		Utils.showKeyboard( mTextEditor, this );

		setHintToToolsButtons();

		mOnSelectionChanges.onTextSelectionBreak( 0 );
	}

	private void setHintToToolsButtons() {
		LinearLayout layout = findViewById( R.id.toolsLinearLayout );
		for (int i = 0; i < layout.getChildCount(); i++) {
			View v = layout.getChildAt( i );
			v.setTag( i );
			v.setOnLongClickListener( v1->{
				final int tag = (int) v1.getTag();
				runOnUiThread( ()->{
					String[] s = getResources().getStringArray( R.array.tools_hints );
					Toast t = Toast.makeText( EntryEditor.this, s[ tag ], Toast.LENGTH_LONG );
					t.setGravity( Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0 );
					t.show();
				} );

				return true;
			} );
		}
	}

	private void loadTextFromEntryInEditor(String id) {
		Optional<Entry> op = EntitiesStorage.get().getEntry( id );
		if ( !op.isPresent() ) {
			Log.i( TAG, "loadTextFromEntryInEditor: entry with id not found. id=" + id );
			Toast.makeText( this, "Loading entry not found", Toast.LENGTH_SHORT ).show();
			super.onBackPressed();
			return;
		}
		final Entry entry = op.get();
		mProgressDialogOnTextLoad = new ProgressDialog( this );
		mProgressDialogOnTextLoad.setTitle( R.string.loading );
		mProgressDialogOnTextLoad.setMessage( getResources().getString( R.string.entry_is_loading ) );
		mProgressDialogOnTextLoad.setCancelable( false );
		mProgressDialogOnTextLoad.show();
		new Thread( ()->{
			mStartLoadTextTime = System.currentTimeMillis();
			try {
				entry.loadProperties();
				Spannable loadedSpannable = entry.loadText( Utils.getScreenSize().x );
				mOnLoadedTextListener.loaded( loadedSpannable, entry );
			} catch (final IOException | JSONException e) {
				mOnLoadedTextListener.exceptionOccurred( e );
			}
		} ).start();
	}

	private void readColorHistory() {
		String history = sp.getString( "color_history", null );
		if ( history != null && !history.isEmpty() ) {
			String[] strings = history.split( ";" );
			for (String str : strings) {
				mColorHistory.add( Integer.parseInt( str ) );
			}
		}
	}

	private void addColorToHistory(int color) {
		if ( mColorHistory.size() > 0 && mColorHistory.get( 0 ) == color ) {
			return;
		}
		ArrayList<Integer> clone = new ArrayList<>( mColorHistory );
		mColorHistory.clear();
		mColorHistory.add( color );
		for (int i = 0; i < Math.min( 4, clone.size() ); i++) {
			mColorHistory.add( clone.get( i ) );
		}
		saveColorHistory();
	}

	private void saveColorHistory() {
		String save = "";
		for (Integer i : mColorHistory) {
			save = String.format( "%s%s;", save, i );
		}
		sp.edit().putString( "color_history", save ).apply();
	}

	private void setTextInEditor(Spannable spannable) {
		Editable e = mTextEditor.getText();
		if ( e != null ) {
			e.clear();
		}

		mTextEditor.setTextWithoutNotifying( spannable );
	}

	private final OnLoadedTextListener mOnLoadedTextListener = new OnLoadedTextListener() {
		@Override
		public void loaded(final Spannable spannable, final Entry entry) {
			double seconds = ( System.currentTimeMillis() - mStartLoadTextTime ) / 1000.0;
			Log.i( TAG + "L", "mOnLoadedTextListener.loaded called. Seconds = " + seconds );

			AbsoluteSizeSpan absoluteSizeSpan = new AbsoluteSizeSpan( entry.getProperties().getTextSize(), true );
			spannable.setSpan( absoluteSizeSpan, 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );

			mOriginalText = Html.toHtml( trim( spannable ) );

			runOnUiThread( ()->{
				setTextInEditor( spannable );
				mTextEditor.setScrollY( entry.getProperties().getScrollPosition() );
				mStartEditable = mTextEditor.getText();
				setEditTextSize();
				mProgressDialogOnTextLoad.cancel();
				final double end = System.currentTimeMillis();
				final double seconds1 = ( end - mStartLoadTextTime ) / 1000;
				Log.v( TAG + "L", "Text loaded after " + seconds1 );
				ScrollView scrollView = findViewById( R.id.scrollView );
				WindowManager w = (WindowManager) getSystemService( Context.WINDOW_SERVICE );
				Display d = w.getDefaultDisplay();
				Point p = new Point();
				d.getSize( p );

				calculateMainAlignment();

				scrollView.post( ()->{
					View focusView = getWindow().getDecorView().findFocus();
					if ( focusView != null ) {
						focusView.clearFocus();
					}
					scrollView.scrollTo( 0, getIntent().getIntExtra( "scroll_position", 0 ) );
				} );
			} );
		}

		@Override
		public void exceptionOccurred(final Exception e) {
			runOnUiThread( ()->{
				mProgressDialogOnTextLoad.cancel();
				Utils.getErrorDialog( e, EntryEditor.this ).show();
			} );
		}
	};

	private void calculateMainAlignment() {
		Editable e = mTextEditor.getText();
		if ( e == null ) {
			return;
		}
		AlignmentSpan.Standard[] spans = e.getSpans( 0, e.length(), AlignmentSpan.Standard.class );
		if ( spans.length == 1 ) {
			mMainAlignment = spans[ 0 ].getAlignment();
		}
	}

	private int getAlignmentButtonId(Alignment alignment) {
		if ( alignment == Alignment.ALIGN_NORMAL ) {
			return R.id.btnAlignLeft;
		} else if ( alignment == Alignment.ALIGN_CENTER ) {
			return R.id.btnAlignCenter;
		} else {
			return R.id.btnAlignRight;
		}
	}

	public void pickImages(View v) {
		mPickImageLauncher.launch( "image/*" );
	}

	private void setImageAtSelBounds(File file) {
		Editable e = mTextEditor.getText();
		int s;
		if ( e == null ) {
			mTextEditor.setText( " " );
			mSelectionBounds[ 0 ] = 0;
			s = 0;
			e = mTextEditor.getText();
		} else {
			s = mSelectionBounds[ 0 ];
			e.insert( s, " \n" );
		}

		Drawable d = ImageRenderer.renderDrawable( file.getPath(), mTextEditor.getWidth() );

		ImageSpan imageSpan = new ImageSpan( d, file.getName() );

		e.setSpan( imageSpan, s, s + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );

		mTextEditor.postInvalidate();
	}

	private void setBtnTextColorPickerColorAtBounds() {
		if ( mSelectionBounds[ 0 ] == mSelectionBounds[ 1 ] ||
				mTextEditor.getText() != null && mSelectionBounds[ 0 ] == 0 && mSelectionBounds[ 1 ] == mTextEditor.getText().length() ) {
			setImageButtonColor( mDefaultTextColor, btnTextColorPicker.getId() );
		} else {
			Editable e = mTextEditor.getText();
			if ( e == null ) {
				return;
			}
			ForegroundColorSpan[] foregroundColorSpans = e.getSpans( mSelectionBounds[ 0 ], mSelectionBounds[ 1 ], ForegroundColorSpan.class );
			int c;
			if ( foregroundColorSpans.length != 0 ) {
				c = foregroundColorSpans[ foregroundColorSpans.length - 1 ].getForegroundColor();
			} else {
				c = Color.BLACK;
			}
			setImageButtonColor( c, btnTextColorPicker.getId() );
		}
	}

	final TextEditor.OnSelectionChanges mOnSelectionChanges = new TextEditor.OnSelectionChanges() {
		@Override
		public void onTextSelected(final int start, final int end) {
			mTextEditor.clearComposingText();
			mSelectionBounds = new int[]{ start, end };

			applyStyleBtnState( start, end );
			btnBold.setOnClickListener( onTextAppearanceClick );
			btnItalic.setOnClickListener( onTextAppearanceClick );
			btnUnderline.setOnClickListener( onUnderlineBtnClick );
			btnStrike.setOnClickListener( onStrikeBtnClick );

			btnTextColorPicker.setOnClickListener( onClickOnSelectColorOfTextSegment );
			setBtnTextColorPickerColorAtBounds();

			Editable s = mTextEditor.getText();
			if ( s == null ) {
				return;
			}
			final BackgroundColorSpan[] backgroundColorSpans = s.getSpans( start, end, BackgroundColorSpan.class );
			final int editTextColor = ( (ColorDrawable) mTextEditor.getBackground() ).getColor();
			final int backgroundColor = ( backgroundColorSpans.length == 0 ? editTextColor : backgroundColorSpans[ 0 ].getBackgroundColor() );
			btnTextHighlight.setOnClickListener( v->{
				OnColorSelected listener = color->{
					Editable e = mTextEditor.getText();
					if ( e == null ) {
						return;
					}
					int selSt = mSelectionBounds[ 0 ];
					int selEnd = mSelectionBounds[ 1 ];
					removeAllSpansInBounds( mSelectionBounds[ 0 ], mSelectionBounds[ 1 ], BackgroundColorSpan.class );
					e.setSpan( new BackgroundColorSpan( color ), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
					setImageButtonColor( color, btnTextHighlight.getId() );
				};
				AlertDialog alertDialog = getColorPickerDialog( R.string.choose_text_background_of_segment, backgroundColor, listener );
				if ( backgroundColorSpans.length > 0 ) {
					alertDialog.setButton( AlertDialog.BUTTON_NEUTRAL, getString( R.string.delete ), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Editable e = mTextEditor.getText();
							if ( e == null ) {
								return;
							}
							removeAllSpansInBounds( mSelectionBounds[ 0 ], mSelectionBounds[ 1 ], BackgroundColorSpan.class );
						}
					} );
				}
				alertDialog.show();
			} );
			setImageButtonColor( backgroundColor, btnTextHighlight.getId() );
		}

		@Override
		public void onTextSelectionBreak(int newSelectionPosition) {
			mSelectionBounds = new int[]{ newSelectionPosition, newSelectionPosition };

			btnBold.setOnClickListener( null );
			btnItalic.setOnClickListener( null );
			btnUnderline.setOnClickListener( null );
			btnStrike.setOnClickListener( null );

			btnTextColorPicker.setOnClickListener( btnTextOnAllColor );
			setBtnTextColorPickerColorAtBounds();

			btnBold.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
			btnItalic.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
			btnUnderline.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
			btnStrike.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );

			btnTextHighlight.setOnClickListener( null );

			setImageButtonColor( DEFAULT_TOOLS_COLOR, btnTextHighlight.getId() );
		}

		@Override
		public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
			Editable e = mTextEditor.getText();
			if ( lengthAfter > lengthBefore ) {
				int len = lengthAfter - lengthBefore;
				int end = start + len;
				if ( e == null ) {
					return;
				}

				e.setSpan( new ForegroundColorSpan( mDefaultTextColor ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
				e.setSpan( new AlignmentSpan.Standard( mMainAlignment ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
				e.setSpan( new AbsoluteSizeSpan( tempProperties.getTextSize(), true ), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
			}
			saveTextChange();
			if ( lengthAfter == 0 ) {
				btnReplaceColor.setOnClickListener( null );
			}
		}

		@Override
		public void onSelectionChanged() {
			applySupSubButton( btnSubScript.getId() );
			applySupSubButton( btnSupScript.getId() );
			setButtonReplaceColorAppearance();
		}
	};

	private void setButtonReplaceColorAppearance() {
		Editable e = mTextEditor.getText();
		if ( e == null ) {
			btnReplaceColor.setOnClickListener( null );
			setImageButtonColor( Color.WHITE, btnReplaceColor.getId() );
			return;
		}
		int selSt = mSelectionBounds[ 0 ];
		int selEnd = mSelectionBounds[ 1 ];
		ForegroundColorSpan[] spans = e.getSpans( selSt, selEnd, ForegroundColorSpan.class );
		int colorToReplace;
		OnColorSelected onColorSelected;
		if ( spans.length > 0 ) {
			colorToReplace = spans[ 0 ].getForegroundColor();
			onColorSelected = color->{
				if ( colorToReplace != color ) {
					replaceColor( colorToReplace, color );
					setButtonReplaceColorAppearance();
				}
			};
		} else {
			colorToReplace = Color.BLACK;
			onColorSelected = color->{
				if ( color != colorToReplace ) {
					ForegroundColorSpan[] allSpans = e.getSpans( 0, e.length(), ForegroundColorSpan.class );
					int[] startPositions = new int[ e.length() + 1 ];
					int[] endPositions = new int[ e.length() + 1 ];
					for (ForegroundColorSpan span : allSpans) {
						startPositions[ e.getSpanStart( span ) ]++;
						endPositions[ e.getSpanEnd( span ) ]++;
					}
					int cnt = 0;
					ArrayList<SpanEntry<ForegroundColorSpan>> spansToApply = new ArrayList<>();
					int st = 0;
					for (int i = 0; i < startPositions.length; i++) {
						boolean wasZero = cnt == 0;
						cnt += startPositions[ i ] - endPositions[ i ];
						if ( !wasZero && cnt == 0 ) {
							st = i;
						} else if ( wasZero && cnt != 0 ) {
							spansToApply.add( new SpanEntry<>( new ForegroundColorSpan( color ), st, i ) );
						}
					}
					if ( cnt == 0 ) {
						spansToApply.add( new SpanEntry<>( new ForegroundColorSpan( color ), st, e.length() ) );
					}
					for (SpanEntry<ForegroundColorSpan> entry : spansToApply) {
						e.setSpan( entry.getSpan(), entry.getStart(), entry.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
					}
					setButtonReplaceColorAppearance();
				}
			};
		}
		setImageButtonColor( colorToReplace, btnReplaceColor.getId() );
		btnReplaceColor.setOnClickListener( v->{
			AlertDialog alertDialog = getReplaceColorPickerDialog( colorToReplace, onColorSelected );
			alertDialog.show();
		} );
	}

	private void replaceColor(int oldColor, int newColor) {
		Editable e = mTextEditor.getText();
		if ( e == null ) {
			return;
		}

		ForegroundColorSpan[] spans = e.getSpans( 0, e.length(), ForegroundColorSpan.class );
		ArrayList<SpanEntry<ForegroundColorSpan>> apply = new ArrayList<>();
		for (ForegroundColorSpan span : spans) {
			if ( span.getForegroundColor() == oldColor ) {
				int st = e.getSpanStart( span );
				int end = e.getSpanEnd( span );
				apply.add( new SpanEntry<>( new ForegroundColorSpan( newColor ), st, end ) );
				e.removeSpan( span );
			}
		}
		for (SpanEntry<ForegroundColorSpan> se : apply) {
			e.setSpan( se.getSpan(), se.getStart(), se.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		}
	}

	private void applySupSubButton(int id) {
		ImageButton btn = findViewById( id );

		if ( mSelectionBounds[ 0 ] == mSelectionBounds[ 1 ] ) {
			btn.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
			btn.setOnClickListener( null );
		} else {
			Editable e = mTextEditor.getText();
			if ( e == null ) {
				return;
			}
			int selSt = mSelectionBounds[ 0 ];
			int selEnd = mSelectionBounds[ 1 ];
			boolean apply;
			if ( id == R.id.btnToolsSuperscript ) {
				apply = e.getSpans( selSt, selEnd, SuperscriptSpan.class ).length > 0;
				btn.setOnClickListener( onSupBtnClick );
			} else {
				apply = e.getSpans( selSt, selEnd, SubscriptSpan.class ).length > 0;
				btn.setOnClickListener( onSubBtnClick );
			}

			if ( apply ) {
				btn.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );
			} else {
				btn.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
			}
			btn.setTag( apply );
		}
	}

	private final View.OnClickListener onSupBtnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			boolean isApplied = (boolean) v.getTag();
			int selSt = mSelectionBounds[ 0 ];
			int selEnd = mSelectionBounds[ 1 ];
			Editable e = mTextEditor.getText();
			if ( e == null ) {
				return;
			}
			removeAllSpansInBounds( selSt, selEnd, SuperscriptSpan.class );
			removeAllSpansInBounds( selSt, selEnd, SubscriptSpan.class );
			removeAllSpansInBounds( selSt, selEnd, RelativeSizeSpan.class );

			if ( !isApplied ) {
				e.setSpan( new SuperscriptSpan(), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
				e.setSpan( new RelativeSizeSpan( INDEX_PROPORTION ), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
			}
			applySupSubButton( v.getId() );
			applySupSubButton( btnSubScript.getId() );
		}
	};

	private final View.OnClickListener onSubBtnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			boolean isApplied = (boolean) v.getTag();
			int selSt = mSelectionBounds[ 0 ];
			int selEnd = mSelectionBounds[ 1 ];
			Editable e = mTextEditor.getText();
			if ( e == null ) {
				return;
			}
			removeAllSpansInBounds( selSt, selEnd, SubscriptSpan.class );
			removeAllSpansInBounds( selSt, selEnd, SuperscriptSpan.class );
			removeAllSpansInBounds( selSt, selEnd, RelativeSizeSpan.class );

			if ( !isApplied ) {
				e.setSpan( new SubscriptSpan(), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
				e.setSpan( new RelativeSizeSpan( INDEX_PROPORTION ), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
			}
			applySupSubButton( v.getId() );
			applySupSubButton( btnSupScript.getId() );
		}
	};

	private <T> void removeAllSpansInBounds(int selSt, int selEnd, Class<T> type) {
		//Utils.removeAllSpansInBounds( selSt, selEnd, type, mTextEditor.getText() );
		Editable e = mTextEditor.getText();
		if ( e == null ) {
			return;
		}

		ArrayList<SpanEntry<T>> arrayList = new ArrayList<>();
		T[] spans = e.getSpans( selSt, selEnd, type );
		for (T span : spans) {
			int st = e.getSpanStart( span );
			int end = e.getSpanEnd( span );
			if ( end > selEnd ) {
				arrayList.add( new SpanEntry<>( span, selEnd, end ) );
			}
			if ( st < selSt ) {
				arrayList.add( new SpanEntry<>( span, st, selSt ) );
			}
			e.removeSpan( span );
		}
		for (SpanEntry<T> se : arrayList) {
			e.setSpan( se.getSpan(), se.getStart(), se.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		}
	}

	private void applyStyleBtnState(int selS, int selE) {
		Editable e = mTextEditor.getText();
		if ( e == null ) {
			return;
		}

		StyleSpan[] ss = e.getSpans( selS, selE, StyleSpan.class );
		boolean isBoldThere = false;
		boolean isItalicThere = false;
		boolean isUnderlineThere = ( e.getSpans( selS, selE, UnderlineSpan.class ).length != 0 );
		boolean isStrikeThere = ( e.getSpans( selS, selE, StrikethroughSpan.class ).length != 0 );
		for (StyleSpan s : ss) {
			if ( s.getStyle() == Typeface.BOLD ) {
				isBoldThere = true;
			}

			if ( s.getStyle() == Typeface.ITALIC ) {
				isItalicThere = true;
			}
		}
		if ( isBoldThere ) {
			btnBold.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );
		} else {
			btnBold.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
		}
		btnBold.setTag( isBoldThere );

		if ( isItalicThere ) {
			btnItalic.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );
		} else {
			btnItalic.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
		}
		btnItalic.setTag( isItalicThere );

		if ( isUnderlineThere ) {
			btnUnderline.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );
		} else {
			btnUnderline.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
		}
		btnUnderline.setTag( isUnderlineThere );

		btnStrike.setTag( isStrikeThere );
		if ( isStrikeThere ) {
			btnStrike.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );
		} else {
			btnStrike.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
		}
	}

	private final View.OnClickListener onUnderlineBtnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Editable s = mTextEditor.getText();
			if ( s == null ) {
				return;
			}

			int selSt = mSelectionBounds[ 0 ];
			int selEnd = mSelectionBounds[ 1 ];
			boolean isUnderline = (boolean) v.getTag();
			removeAllSpansInBounds( selSt, selEnd, UnderlineSpan.class );

			v.setTag( !isUnderline );
			if ( !isUnderline ) { // apply
				s.setSpan( new UnderlineSpan(), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
				v.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );
			} else { //delete
				v.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
			}
		}
	};

	private final View.OnClickListener onStrikeBtnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Editable s = mTextEditor.getText();
			if ( s == null ) {
				return;
			}

			int selSt = mSelectionBounds[ 0 ];
			int selEnd = mSelectionBounds[ 1 ];
			boolean isStrike = (boolean) v.getTag();
			removeAllSpansInBounds( selSt, selEnd, StrikethroughSpan.class );

			v.setTag( !isStrike );
			if ( !isStrike ) { // apply
				s.setSpan( new StrikethroughSpan(), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
				v.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );
			} else { //delete
				v.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
			}
		}
	};

	private final View.OnClickListener onTextAppearanceClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			int typeface = Typeface.BOLD;
			if ( v.getId() == R.id.btnToolsItalic ) {
				typeface = Typeface.ITALIC;
			}

			if ( !( (boolean) v.getTag() ) ) {
				applyTypeface( typeface, mSelectionBounds[ 0 ], mSelectionBounds[ 1 ], "apply" );
				v.setTag( true );
				v.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );
			} else {
				applyTypeface( typeface, mSelectionBounds[ 0 ], mSelectionBounds[ 1 ], "delete" );
				v.setTag( false );
				v.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
			}
		}
	};

	private void applyTypeface(int typeface, int selSt, int selEnd, String action) {
		Editable s = mTextEditor.getText();
		if ( s == null ) {
			return;
		}

		StyleSpan[] ss = s.getSpans( selSt, selEnd, StyleSpan.class );
		ArrayList<SpanEntry<StyleSpan>> spansToApply = new ArrayList<>();
		for (StyleSpan span : ss) {
			if ( span.getStyle() == typeface ) {
				int st = s.getSpanStart( span );
				int end = s.getSpanEnd( span );

				if ( st < selSt ) {
					SpanEntry<StyleSpan> se = new SpanEntry<>( new StyleSpan( span.getStyle() ), st, selSt );
					spansToApply.add( se );
				}
				if ( end > selEnd ) {
					SpanEntry<StyleSpan> se = new SpanEntry<>( new StyleSpan( span.getStyle() ), selEnd, end );
					spansToApply.add( se );
				}
				s.removeSpan( span );
			}
		}
		for (SpanEntry<StyleSpan> spanEntry : spansToApply) {
			s.setSpan( spanEntry.getSpan(), spanEntry.getStart(), spanEntry.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		}
		if ( action.equals( "apply" ) ) {
			s.setSpan( new StyleSpan( typeface ), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		}
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
			OnColorSelected whatToDo = color->{
				setImageButtonColor( color, btnBgColorPicker.getId() );
				mTextEditor.setBackgroundColor( color );
				getWindow().getDecorView().setBackgroundColor( color );
				tempProperties.setBgColor( color );
			};
			AlertDialog alertDialog = getColorPickerDialog( R.string.set_background_color, tempProperties.getBgColor(), whatToDo );
			alertDialog.show();
		}
	};

	private void applySpanColorToText(int color, int selSt, int selEnd) {
		Editable s = mTextEditor.getText();
		if ( s == null ) {
			return;
		}
		removeAllSpansInBounds( selSt, selEnd, ForegroundColorSpan.class );
		s.setSpan( new ForegroundColorSpan( color ), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
	}

	private void setImageButtonColor(int color, int id) {
		ImageButton btn = findViewById( id );
		Drawable d = btn.getDrawable();
		d.setColorFilter( new PorterDuffColorFilter( color, PorterDuff.Mode.SRC_IN ) );
	}

	private final View.OnClickListener onClickOnSelectColorOfTextSegment = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			OnColorSelected onColorSelected = color->{
				applySpanColorToText( color, mSelectionBounds[ 0 ], mSelectionBounds[ 1 ] );
				setBtnTextColorPickerColorAtBounds();
			};
			Editable s = mTextEditor.getText();
			int c;
			if ( s == null ) {
				c = Color.BLACK;
			} else {
				ForegroundColorSpan[] spans = s.getSpans( mSelectionBounds[ 0 ], mSelectionBounds[ 1 ], ForegroundColorSpan.class );
				if ( spans.length == 0 ) {
					c = Color.BLACK;
				} else {
					c = spans[ spans.length - 1 ].getForegroundColor();
				}
			}
			AlertDialog alertDialog = getColorPickerDialog( R.string.set_text_color_of_selected_segment, c, onColorSelected );
			alertDialog.show();
		}
	};

	private void initializeColorButtons(final View layout, final View.OnClickListener clickListener) {
		int[] btnIds = new int[]{ R.id.btnColorHistory1, R.id.btnColorHistory2, R.id.btnColorHistory3, R.id.btnColorHistory4, R.id.btnColorHistory5 };
		for (int i = 0; i < btnIds.length; i++) {
			int id = btnIds[ i ];
			Button btn = layout.findViewById( id );
			//btn.setTag( 1, i );
			if ( i >= mColorHistory.size() ) {
				btn.setVisibility( View.INVISIBLE );
			} else {
				btn.setBackgroundTintList( ColorStateList.valueOf( mColorHistory.get( i ) ) );
				btn.setTag( R.id.color_int_in_history, mColorHistory.get( i ) );
				btn.setTag( R.id.color_pos_in_history, i );
				btn.setOnClickListener( clickListener );
				btn.setOnLongClickListener( new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(final View v) {
						PopupMenu popupMenu = new PopupMenu( EntryEditor.this, v );
						popupMenu.getMenu().add( EntryEditor.this.getString( R.string.delete ) );
						popupMenu.setOnMenuItemClickListener( item->{
							mColorHistory.remove( (int) v.getTag( R.id.color_pos_in_history ) );
							saveColorHistory();
							initializeColorButtons( (View) v.getParent(), clickListener );
							return true;
						} );
						popupMenu.show();
						return true;
					}
				} );
			}
		}
	}

	private interface OnColorSelected {
		void onColorSelected(int color);
	}

	private AlertDialog getReplaceColorPickerDialog(int colorToReplace, final OnColorSelected colorSelected) {
		return getColorPickerDialog( R.string.replace_this_color, colorToReplace, colorSelected, true );
	}

	private AlertDialog getColorPickerDialog(int title, int defColor, final OnColorSelected colorSelected) {
		return getColorPickerDialog( title, defColor, colorSelected, false );
	}

	private AlertDialog getColorPickerDialog(int title, int defColor, final OnColorSelected colorSelected, final boolean offerReplacement) {
		final ScrollView scrollView = (ScrollView) getLayoutInflater().inflate( R.layout.layout_color_picker, null );
		final ColorPickerView colorPickerView = scrollView.findViewById( R.id.color_picker );
		colorPickerView.setColor( defColor, true );
		final boolean[] fromBtn = { false };
		if ( offerReplacement ) {
			scrollView.findViewById( R.id.layout_radio_replacement_color ).setVisibility( View.VISIBLE );

			scrollView.findViewById( R.id.layout_from_color )
					.setBackgroundColor( defColor );
			scrollView.findViewById( R.id.layout_to_color )
					.setBackgroundColor( defColor );

			colorPickerView.addOnColorChangedListener( i->{
				fromBtn[ 0 ] = false;
				scrollView.findViewById( R.id.layout_to_color )
						.setBackgroundColor( i );
			} );
		}
		final AlertDialog.Builder builder = new AlertDialog.Builder( EntryEditor.this )
				.setTitle( title )
				.setView( scrollView )
				.setPositiveButton( "OK", (dialog, which)->{
					int color = colorPickerView.getSelectedColor();
					if ( !fromBtn[ 0 ] ) {
						addColorToHistory( color );
					}
					colorSelected.onColorSelected( color );
					dialog.cancel();
				} ).setNegativeButton( R.string.cancel, (dialog, which)->dialog.cancel() );
		final AlertDialog alertDialog = builder.create();
		if ( mColorHistory.size() == 0 ) {
			scrollView.findViewById( R.id.layoutColorsHistory ).setVisibility( View.GONE );
		} else {
			initializeColorButtons( scrollView, v->{
				int color = (int) v.getTag( R.id.color_int_in_history );
				colorSelected.onColorSelected( color );
				alertDialog.cancel();
			} );
		}

		return alertDialog;
	}

	private final View.OnClickListener btnTextOnAllColor = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			final Editable e = mTextEditor.getText();
			if ( e == null ) {
				return;
			}

			final ForegroundColorSpan[] spans = e.getSpans( 0, e.length(), ForegroundColorSpan.class );
			AlertDialog alertDialog = getColorPickerDialog( R.string.set_text_color_of_all_text,
					mDefaultTextColor,
					color->{
						for (ForegroundColorSpan span : spans) {
							e.removeSpan( span );
						}
						e.setSpan( new ForegroundColorSpan( color ), 0, e.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
						mTextEditor.setTextColor( color );

						mDefaultTextColor = color;
						tempProperties.setDefaultTextColor( color );
						setBtnTextColorPickerColorAtBounds();
					} );
			alertDialog.show();
		}
	};

	public void plusTextSize(View view) {
		int ts = tempProperties.getTextSize();
		if ( ts < 45 ) {
			tempProperties.setTextSize( ts + 1 );
		}

		setTextSizeInLbl( view );

		setEditTextSize();
	}

	public void minusTextSize(View view) {
		int ts = tempProperties.getTextSize();
		if ( ts > 15 ) {
			tempProperties.setTextSize( ts - 1 );
		}

		setTextSizeInLbl( view );

		setEditTextSize();
	}

	private void setEditTextSize() {
		Editable e = mTextEditor.getText();
		if ( e == null ) {
			return;
		}

		AbsoluteSizeSpan absoluteSizeSpan = new AbsoluteSizeSpan( tempProperties.getTextSize(), true );
		RelativeSizeSpan[] spans = e.getSpans( 0, e.length(), RelativeSizeSpan.class );
		ArrayList<SpanEntry<RelativeSizeSpan>> spanEntries = new ArrayList<>();
		for (RelativeSizeSpan span : spans) {
			int st = e.getSpanStart( span );
			int end = e.getSpanEnd( span );
			spanEntries.add( new SpanEntry<>( new RelativeSizeSpan( span.getSizeChange() ), st, end ) );
		}
		removeAllSpansInBounds( 0, e.length(), AbsoluteSizeSpan.class );
		removeAllSpansInBounds( 0, e.length(), RelativeSizeSpan.class );
		e.setSpan( absoluteSizeSpan, 0, e.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		for (SpanEntry<RelativeSizeSpan> se : spanEntries) {
			e.setSpan( se.getSpan(), se.getStart(), se.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		}
	}

	private void setTextSizeInLbl(View view) {
		int ts = tempProperties.getTextSize();
		View parent = (View) view.getRootView();
		( (TextView) parent.findViewById( R.id.textViewTextSize ) ).setText( String.format( Locale.ROOT, "%d", ts ) );
	}

	public void showTextSizePicker(View v) {
		PopupWindow window = new PopupWindow( this );
		View view = getLayoutInflater().inflate( R.layout.tools_layout_textsize, null );
		window.setContentView( view );
		setTextSizeInLbl( view );

		window.setEnterTransition( new Fade( Fade.IN ) );
		window.setExitTransition( new Fade( Fade.OUT ) );

		Drawable d = ContextCompat.getDrawable( this, R.drawable.button_rounded_corners );
		d.setColorFilter( new PorterDuffColorFilter( getColor( R.color.gray ), PorterDuff.Mode.SRC_IN ) );
		window.setBackgroundDrawable( d );

		window.setFocusable( true );

		window.showAtLocation( v, Gravity.CENTER_HORIZONTAL, (int) v.getX(), (int) ( v.getY() ) );
	}

	private Alignment getCurrentAlignment() {
		Editable e = mTextEditor.getText();
		if ( mSelectionBounds[ 0 ] == mSelectionBounds[ 1 ] || e == null ) {
			return mMainAlignment;
		}

		AlignmentSpan.Standard[] spans = e.getSpans( mSelectionBounds[ 0 ], mSelectionBounds[ 1 ], AlignmentSpan.Standard.class );
		if ( spans.length > 0 ) {
			return spans[ 0 ].getAlignment();
		}
		return mMainAlignment;
	}

	public void showTextAlignmentPicker(View v) {
		PopupWindow window = new PopupWindow( this );
		View view = getLayoutInflater().inflate( R.layout.tools_layout_alignment, null );

		resetAlignmentButtons( view );
		setAlignmentButtonClicked( view, getCurrentAlignment() );

		window.setContentView( view );

		window.setEnterTransition( new Fade( Fade.IN ) );
		window.setExitTransition( new Fade( Fade.OUT ) );

		Drawable d = ContextCompat.getDrawable( this, R.drawable.button_rounded_corners );
		d.setColorFilter( new PorterDuffColorFilter( getColor( R.color.gray ), PorterDuff.Mode.SRC_IN ) );
		window.setBackgroundDrawable( d );

		window.setFocusable( true );

		window.showAtLocation( v, Gravity.CENTER_HORIZONTAL, 0, (int) ( v.getY() ) );
	}

	private void createEntry(String name, Spannable text) {
		mTextEditor.clearComposingText();
		mEntry = EntitiesStorage.get().createEntry( name, mParentGroup.getId() );
		mEntry.setProperties( tempProperties );
		try {
			mEntry.saveProperties();
		} catch (IOException | JSONException e) {
			Utils.getErrorDialog( e, this ).show();
			return;
		}
		sendBroadcast( new Intent( BuildConfig.APPLICATION_ID + ".REFRESH_ENTITIES_LISTS" ) );
		try {
			copyTempFiles();
			mEntry.saveText( text );
			setResult( Results.REOPEN, new Intent().putExtra( "id", mEntry.getId() ) );
			super.onBackPressed();
		} catch (IOException | JSONException e) {
			Utils.getErrorDialog( e, this ).show();
			e.printStackTrace();
		}
	}

	private void copyTempFiles() throws IOException {
		for (File file : mMediaToMove) {
			File dest = new File( mEntry.getImagesMediaFolder(), file.getName() );
			FileInputStream fis = new FileInputStream( file );
			FileOutputStream fos = new FileOutputStream( dest );
			byte[] b = new byte[ 1024 ];
			int len;
			while ( ( len = fis.read( b ) ) != -1 ) {
				fos.write( b, 0, len );
			}
			fis.close();
			fos.close();

			file.delete();
		}
		mMediaToMove.clear();
	}

	private void resetAlignmentButtons(View parent) {
		ImageButton btn;
		int[] btnIds = new int[]{ R.id.btnAlignLeft, R.id.btnAlignCenter, R.id.btnAlignRight, R.id.btnAlignJustify };
		for (int id : btnIds) {
			btn = parent.findViewById( id );
			btn.setBackgroundTintList( ColorStateList.valueOf( getColor( android.R.color.transparent ) ) );
		}
	}

	private void setAlignmentButtonClicked(View parent, Alignment alignment) {
		parent.findViewById( getAlignmentButtonId( alignment ) )
				.setBackgroundTintList( ColorStateList.valueOf( getColor( R.color.btnClicked ) ) );
	}

	public void chooseTextAlignment(View v) {
		Editable e = mTextEditor.getText();
		if ( e == null ) {
			return;
		}
		if ( mSelectionBounds[ 0 ] != mSelectionBounds[ 1 ] ) {
			setGravityOfSegment( v.getId(), v );
			return;
		}

		Layout.Alignment alignment = Layout.Alignment.ALIGN_NORMAL;
		if ( v.getId() == R.id.btnAlignCenter ) {
			alignment = Layout.Alignment.ALIGN_CENTER;
		} else if ( v.getId() == R.id.btnAlignRight ) {
			alignment = Layout.Alignment.ALIGN_OPPOSITE;
		}

		mMainAlignment = alignment;

		AlignmentSpan.Standard[] spans = e.getSpans( 0, e.length(), AlignmentSpan.Standard.class );
		for (AlignmentSpan.Standard span : spans) {
			e.removeSpan( span );
		}
		e.setSpan( new AlignmentSpan.Standard( alignment ), 0, e.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );

		View root = v.getRootView();
		resetAlignmentButtons( root );
		setAlignmentButtonClicked( root, mMainAlignment );
	}

	private void setGravityOfSegment(int id, View v) {
		Editable e = mTextEditor.getText();
		if ( e == null ) {
			return;
		}
		Layout.Alignment alignment = Layout.Alignment.ALIGN_NORMAL;
		if ( id == R.id.btnAlignCenter ) {
			alignment = Layout.Alignment.ALIGN_CENTER;
		} else if ( id == R.id.btnAlignRight ) {
			alignment = Layout.Alignment.ALIGN_OPPOSITE;
		}

		int selSt = mSelectionBounds[ 0 ];
		int selEnd = mSelectionBounds[ 1 ];
		removeAllSpansInBounds( selSt, selEnd, AlignmentSpan.Standard.class );
		e.setSpan( new AlignmentSpan.Standard( alignment ), selSt, selEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );

		if ( v != null ) {
			View root = v.getRootView();
			resetAlignmentButtons( root );
			setAlignmentButtonClicked( root, alignment );
		}
	}

	private void removeUnusedImages() {
		Editable e = mTextEditor.getText();
		if ( e == null ) {
			return;
		}
		Map<String, Boolean> usedResources = new HashMap<>();
		for (ImageSpan span : e.getSpans( 0, e.length(), ImageSpan.class )) {
			File file = new File( span.getSource() );
			usedResources.put( file.getName(), true );
		}
		File file = Utils.getEntryImagesMediaFolder( mId );
		File[] files = file.listFiles();
		if ( files == null ) {
			return;
		}
		for (File f : files) {
			if ( !usedResources.containsKey( f.getName() ) ) {
				f.delete();
			}
		}
	}

	private void copyContentFiles() {
		mCopyToEntry.deleteContentFiles();
		ArrayList<File> contentFiles = mEntry.getContentFiles();
		for (File file : contentFiles) {
			String path = file.getPath().replaceAll( mEntry.getId(), mCopyToEntry.getId() );
			File nFile = new File( path );
			File parent = nFile.getParentFile();
			assert parent != null;
			if ( !parent.exists() ) {
				parent.mkdirs();
			}
			try {
				if ( !nFile.exists() ) {
					nFile.createNewFile();
				}
				FileInputStream fis = new FileInputStream( file );
				FileOutputStream fos = new FileOutputStream( nFile );
				byte[] buffer = new byte[ 1024 ];
				int len;
				while ( ( len = fis.read( buffer ) ) != -1 ) {
					fos.write( buffer, 0, len );
				}

				fis.close();
				fos.flush();
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
				Utils.getErrorDialog( e, this ).show();
				return;
			}
		}
	}

	private void replaceImages() {
		Editable e = mTextEditor.getText();
		if ( e == null ) {
			return;
		}
		ImageSpan[] spans = e.getSpans( 0, e.length(), ImageSpan.class );
		for (ImageSpan span : spans) {
			String source = span.getSource();
			assert source != null : "Image source is null";
			source = source.replaceAll( mEntry.getId(), mCopyToEntry.getId() );
			int st = e.getSpanStart( span );
			int end = e.getSpanEnd( span );
			e.removeSpan( span );
			Drawable d = ImageRenderer.renderDrawable( source );
			e.setSpan( new ImageSpan( d, source ), st, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		}
	}

	private void saveEntry() {
		Editable e = mTextEditor.getText();
		if ( e == null ) {
			return;
		}

		mTextEditor.setTextWithoutNotifying( trim( mTextEditor.getText() ) );
		removeAllSpansInBounds( 0, e.length(), SpanWatcher.class );

		switch ( type ) {
			case "create":
				if ( e.length() != 0 ) {
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
									if ( n.isEmpty() || n.trim().equals( "" ) ) {
										Toast.makeText( EntryEditor.this, R.string.invalid_name, Toast.LENGTH_SHORT ).show();
									} else {
										if ( EntitiesStorage.get().isEntryNameExists( n ) ) {
											AlertDialog.Builder builder1 = new AlertDialog.Builder( EntryEditor.this )
													.setTitle( R.string.warning )
													.setMessage( getResources().getString( R.string.this_name_already_exist ) + "\n" +
															getResources().getString( R.string.do_you_want_to_continue ) )
													.setPositiveButton( R.string.yes, (dialog1, which1)->{
														createEntry( n, mTextEditor.getText() );
														dialog1.dismiss();
													} )
													.setNegativeButton( R.string.no, (dialog12, which12)->dialog12.dismiss() );
											builder1.create().show();
										} else {
											createEntry( n, mTextEditor.getText() );
										}
									}
								}
							} )
							.setNegativeButton( R.string.cancel, (dialog, which)->dialog.cancel() )
							.setCancelable( false );

					alertDialog = builder.create();
					alertDialog.setOnShowListener( dialog->Utils.showKeyboard( name, EntryEditor.this ) );
					alertDialog.show();
				} else {
					mTextEditor.requestFocus();
				}
				break;
			case "edit":
				removeUnusedImages();
				if ( e.length() != 0 ) {
					try {
						mEntry.setProperties( tempProperties );
						mEntry.saveProperties();
						mEntry.saveText( mTextEditor.getText() );
						ScrollView scrollView = findViewById( R.id.scrollView );
						setResult( Results.REOPEN, new Intent().putExtra( "id", mEntry.getId() ).putExtra( "scroll_position", scrollView.getScrollY() ) );
					} catch (Exception ex) {
						ex.printStackTrace();
						Utils.getErrorDialog( ex, this ).show();
						return;
					}
					_finishActivity();
				}
				break;
			case "copy":
				removeUnusedImages();
				if ( e.length() != 0 ) {
					copyContentFiles();
					replaceImages();
					try {
						mCopyToEntry.setProperties( tempProperties );
						mCopyToEntry.saveProperties();
						mCopyToEntry.saveText( mTextEditor.getText() );

						setResult( Results.REOPEN, new Intent().putExtra( "id", mCopyToEntry.getId() ) );
						_finishActivity();
					} catch (IOException | JSONException ex) {
						ex.printStackTrace();
						Utils.getErrorDialog( ex, this ).show();
					}
				}
				break;
		}
	}

	private final View.OnClickListener saveEntry = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			mTextEditor.clearComposingText();
			saveEntry();
		}
	};
}
