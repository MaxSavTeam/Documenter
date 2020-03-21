package com.maxsavitsky.documenter;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Image;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.codes.Requests;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.utils.HtmlImageLoader;
import com.maxsavitsky.documenter.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;

public class ViewEntry extends ThemeActivity {

	private Entry mEntry;
	private SharedPreferences sp;
	private int mWebViewId;
	private boolean resultSet = false;

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

		//mEntry.getProperties().setScrollPosition( ( (WebView) findViewById( mWebViewId ) ).getScrollY() );
		try {
			mEntry.saveProperties( mEntry.getProperties() );
		} catch (Exception e) {
			Utils.getErrorDialog( e, this ).show();
			return;
		}
		finish();
	}

	@Override
	public void onBackPressed() {
		backPressed();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if(item.getItemId() == android.R.id.home){
			backPressed();
		}else if(item.getItemId() == R.id.item_edit_entry_name){
			AlertDialog changeNameDialog;
			final EditText editText = new EditText( this );
			editText.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT ) );
			editText.setText( mEntry.getName() );
			editText.requestFocus();
			//Utils.showKeyboard( editText, this );
			AlertDialog.Builder builder = new AlertDialog.Builder( this )
					.setTitle( R.string.edit_entry_name )
					.setView( editText )
					.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String newName = editText.getText().toString();
							if(!newName.isEmpty() && !newName.equals( mEntry.getName() )){
								MainData.removeEntryWithId( mEntry.getId() );
								ArrayList<Entry> entries = MainData.getEntriesList();
								mEntry = new Entry(mEntry.getId(), newName);
								entries.add( mEntry );
								MainData.setEntriesList( entries );
								Utils.saveEntriesList( entries );
								applyTheme();
								setResult( Results.NEED_TO_REFRESH );
								resultSet = true;
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
		}else if(item.getItemId() == R.id.item_delete_entry){
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
									Toast.makeText(ViewEntry.this, "Failed", Toast.LENGTH_SHORT).show();
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
		}else if(item.getItemId() == R.id.item_edit_entry_text){
			//mEntry.getProperties().setScrollPosition( ((WebView) findViewById( mWebViewId )).getScrollY() );
			try {
				mEntry.saveProperties( mEntry.getProperties() );
			} catch (Exception e) {
				e.printStackTrace();
			}
			Intent intent = new Intent( this, EntryEditor.class );
			intent.putExtra( "type", "edit" );
			intent.putExtra( "id", mEntry.getId() );
			startActivityForResult( intent, Requests.EDIT_ENTRY );
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
		getMenuInflater().inflate( R.menu.entry_menu, menu );
		getMenuInflater().inflate( R.menu.common_menu, menu );
		MenuItem item = menu.findItem(R.id.item_common_remember_pos);
		item.setChecked( mEntry.getProperties().isSaveLastPos() );
		item.setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				boolean isChecked = !item.isChecked();
				item.setChecked( isChecked );
				try{
					mEntry.applySaveLastPos( isChecked );
				} catch (final IOException e) {
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
		void loaded(ArrayList<String> strings);
		void exceptionOccurred(Exception e);
	}

	private TextLoaderCallback mCallback = new TextLoaderCallback() {
		@Override
		public void loaded(final ArrayList<String> strings) {
			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					TextView t = findViewById( R.id.textViewContent );
					t.setText( "" );
					for(String s : strings){
						SpannableString spannableString = new SpannableString( Html.fromHtml(s, new HtmlImageLoader( ViewEntry.this ), null ) );
						t.append( spannableString );
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

		sp = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

		TextView textView = findViewById(R.id.textViewContent);
		textView.setTextSize( TypedValue.COMPLEX_UNIT_DIP, mEntry.getProperties().getTextSize() );
		textView.setTextColor( mEntry.getProperties().getDefaultTextColor() );
		getWindow().getDecorView().setBackgroundColor( mEntry.getProperties().getBgColor() );
		final Thread loadThread = new Thread( new Runnable() {
			@Override
			public void run() {
				try {
					ArrayList<String> array = mEntry.loadTextLines();
					mCallback.loaded( array );
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

		/*WebView webView = findViewById( R.id.webView );
		mWebViewId = R.id.webView;
		WebSettings settings = webView.getSettings();
		settings.setAllowFileAccessFromFileURLs( true );
		settings.setAllowFileAccess( true );
		settings.setJavaScriptCanOpenWindowsAutomatically( false );
		//settings.setUseWideViewPort( true );
		//settings.setLoadWithOverviewMode(true);
		//webView.setBackgroundColor( entryProperty.getBgColor() );
		settings.setDefaultFontSize( mEntry.getProperties().textSize );
		webView.loadUrl( "file://" + mEntry.getPathDir() + "text.html" );
		if( mEntry.getProperties().isSaveLastPos())
			webView.setScrollY( mEntry.getProperties().getScrollPosition() );
		else
			webView.setScrollY( 0 );*/
	}
}