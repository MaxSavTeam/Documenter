package com.maxsavitsky.documenter;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.codes.Requests;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.media.images.HtmlImageLoader;
import com.maxsavitsky.documenter.utils.SpanEntry;
import com.maxsavitsky.documenter.utils.Utils;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

public class ViewEntry extends ThemeActivity {

	private Entry mEntry;
	private SharedPreferences sp;
	private boolean resultSet = false;
	private ScrollView mScrollView;
	private boolean isFreeMode = false;

	private void applyTheme(){
		ActionBar actionBar = getSupportActionBar();
		if ( actionBar != null ) {
			Utils.applyDefaultActionBarStyle(actionBar);
			actionBar.setTitle( mEntry.getName() );
		}
	}

	private void backPressed(){
		if(!resultSet)
			setResult( Results.OK );
		mEntry.getProperties().setScrollPosition( mScrollView.getScrollY() );
		try {
			mEntry.saveProperties();
			finishAndRemoveTask();
		} catch (IOException e) {
			Utils.getErrorDialog( e, this ).show();
		}
	}

	@Override
	public void onBackPressed() {
		backPressed();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch ( item.getItemId() ) {
			case android.R.id.home:
				backPressed();
				break;
			case R.id.item_edit_entry_name:
				AlertDialog changeNameDialog;
				final EditText editText = new EditText( this );
				editText.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT ) );
				editText.setText( mEntry.getName() );
				editText.requestFocus();
				editText.setTextColor( getColor(super.mTextColor) );
				AlertDialog.Builder builder = new AlertDialog.Builder( this )
						.setTitle( R.string.edit_entry_name )
						.setView( editText )
						.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								String newName = editText.getText().toString();
								newName = newName.trim();
								if ( !newName.isEmpty() && !newName.equals( mEntry.getName() ) ) {
									if ( Utils.isNameExist( newName, "ent" ) ) {
										Toast.makeText( ViewEntry.this, R.string.this_name_already_exist, Toast.LENGTH_SHORT ).show();
										return;
									}
									MainData.removeEntryWithId( mEntry.getId() );
									ArrayList<Entry> entries = MainData.getEntriesList();
									mEntry = new Entry( mEntry.getId(), newName );
									entries.add( mEntry );
									MainData.setEntriesList( entries );
									Utils.saveEntriesList( entries );
									applyTheme();
									setResult( Results.NEED_TO_REFRESH );
									resultSet = true;
								} else {
									Toast.makeText( ViewEntry.this, R.string.invalid_name, Toast.LENGTH_SHORT ).show();
								}
							}
						} )
						.setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						} ).setCancelable( false );
				changeNameDialog = builder.create();
				changeNameDialog.show();
				break;
			case R.id.item_delete_entry:
				AlertDialog.Builder deletionBuilder = new AlertDialog.Builder( this )
						.setMessage( R.string.delete_confirmation_text )
						.setTitle( R.string.confirmation )
						.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
								try {
									if ( MainData.finallyDeleteEntryWithId( mEntry.getId() ) ) {
										setResult( Results.NEED_TO_REFRESH );
										finish();
									} else {
										Toast.makeText( ViewEntry.this, "Failed", Toast.LENGTH_SHORT ).show();
									}
								} catch (Exception e) {
									e.printStackTrace();
									Toast.makeText( ViewEntry.this, "onOptionsItemSelected", Toast.LENGTH_LONG ).show();
									Utils.getErrorDialog( e, ViewEntry.this ).show();
								}
							}
						} ).setNeutralButton( R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						} ).setCancelable( false );
				deletionBuilder.create().show();
				break;
			case R.id.item_edit_entry_text:
				mEntry.getProperties().setScrollPosition( mScrollView.getScrollY() );
				try {
					mEntry.saveProperties( mEntry.getProperties() );
				} catch (Exception e) {
					e.printStackTrace();
				}
				Intent intent = new Intent( this, EntryEditor.class );
				intent.putExtra( "type", "edit" );
				intent.putExtra( "id", mEntry.getId() );
				startActivityForResult( intent, Requests.EDIT_ENTRY );
				break;
		}
		return super.onOptionsItemSelected( item );
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if(requestCode == Requests.EDIT_ENTRY){
			if(resultCode == Results.REOPEN){
				setResult( resultCode, data );
				finish();
			}
		}
		super.onActivityResult( requestCode, resultCode, data );
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if(menu != null)
			menu.clear();
		getMenuInflater().inflate( R.menu.entry_menu, menu );
		if(!isFreeMode) {
			getMenuInflater().inflate( R.menu.common_menu, menu );
			MenuItem item = menu.findItem( R.id.item_common_remember_pos );
			item.setChecked( mEntry.getProperties().isSaveLastPos() );
			item.setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					boolean isChecked = !item.isChecked();
					item.setChecked( isChecked );
					try {
						mEntry.applySaveLastPos( isChecked );
					} catch (final IOException | SAXException e) {
						e.printStackTrace();
						runOnUiThread( new Runnable() {
							@Override
							public void run() {
								Utils.getErrorDialog( e, ViewEntry.this ).show();
							}
						} );
					}

					return true;
				}
			} );
		}
		return super.onCreateOptionsMenu( menu );
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(sp.getBoolean( "keep_screen_on", true )){
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		getWindow().clearFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private ProgressDialog mProgressDialog;

	private interface TextLoaderCallback{
		void exceptionOccurred(Exception e);
		void loaded(String text);
		void loaded(Spannable spannable);
	}

	private final TextLoaderCallback mCallback = new TextLoaderCallback() {
		@Override
		public void loaded(final String text) {
			new Thread( new Runnable() {
				@Override
				public void run() {
					final TextView t = findViewById( R.id.textViewContent );
					final Spannable spannable = (Spannable) Html.fromHtml(text, new HtmlImageLoader(), null);
					ArrayList<SpanEntry<AlignmentSpan.Standard>> spanEntries = mEntry.getAlignments();
					for(SpanEntry<AlignmentSpan.Standard> se : spanEntries){
						spannable.setSpan( se.getSpan(), se.getStart(), se.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
					}
					t.post( new Runnable() {
						@Override
						public void run() {
							t.setText( spannable );
							if(mEntry.getProperties().isSaveLastPos()){
								mScrollView.post( new Runnable() {
									@Override
									public void run() {
										mScrollView.smoothScrollTo(0, mEntry.getProperties().getScrollPosition() );
									}
								} );
							}
							mProgressDialog.dismiss();
						}
					} );
				}
			} ).start();
		}

		@Override
		public void loaded(final Spannable spannable) {
			final TextView t = findViewById( R.id.textViewContent );
			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					t.setText( spannable );
					if(mEntry.getProperties().isSaveLastPos()){
						mScrollView.post( new Runnable() {
							@Override
							public void run() {
								mScrollView.smoothScrollTo(0, mEntry.getProperties().getScrollPosition() );
							}
						} );
					}
					mProgressDialog.dismiss();
				}
			} );
		}

		@Override
		public void exceptionOccurred(Exception e) {
			Utils.getErrorDialog( e, ViewEntry.this ).show();
		}
	};

	private void hideUpButton(){
		FloatingActionButton fab = findViewById( R.id.fabUpView );
		fab.animate().setDuration( 500 ).scaleX( 0 ).scaleY( 0 ).start();
	}

	private void showUpButton(){
		FloatingActionButton fab = findViewById( R.id.fabUpView );
		fab.animate().setDuration( 500 ).scaleX( 1 ).scaleY( 1 ).start();
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_view_entry );
		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );
		Intent intent = getIntent();
		mEntry = MainData.getEntryWithId( intent.getStringExtra( "id" ) );
		try{
			mEntry.readProperties();
		}catch (Exception e){
			Utils.getErrorDialog( e, this ).show();
		}
		applyTheme();
		isFreeMode = intent.getBooleanExtra("free_mode", false);
		invalidateOptionsMenu();

		sp = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

		TextView textView = findViewById(R.id.textViewContent);
		textView.setTextSize( TypedValue.COMPLEX_UNIT_DIP, mEntry.getProperties().getTextSize() );
		textView.setTextColor( mEntry.getProperties().getDefaultTextColor() );

		mScrollView = findViewById( R.id.viewEntryScrollView );
		mScrollView.setOnScrollChangeListener( new View.OnScrollChangeListener() {
			@Override
			public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
				if ( oldScrollY > scrollY && scrollY > 5 ) {
					showUpButton();
				} else if ( scrollY <= 5 || oldScrollY < scrollY ) {
					hideUpButton();
				}
			}
		} );
		findViewById( R.id.fabUpView ).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mScrollView.smoothScrollTo( 0, 0 );
			}
		} );

		getWindow().getDecorView().setBackgroundColor( mEntry.getProperties().getBgColor() );
		final Thread loadThread = new Thread( new Runnable() {
			@Override
			public void run() {
				try {
					//ArrayList<String> array = mEntry.loadTextLines();
					mCallback.loaded( mEntry.loadAndPrepareText() );
				} catch (IOException e) {
					e.printStackTrace();
					mCallback.exceptionOccurred( e );
				}
			}
		} );
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setTitle( R.string.loading );
		mProgressDialog.setMessage( getResources().getString( R.string.entry_is_loading ) );
		mProgressDialog.setCancelable( false );
		mProgressDialog.setButton( ProgressDialog.BUTTON_NEUTRAL,
				getResources().getString( R.string.cancel ),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						loadThread.interrupt();
						dialog.cancel();
						backPressed();
					}
				} );
		mProgressDialog.show();
		loadThread.start();
	}
}