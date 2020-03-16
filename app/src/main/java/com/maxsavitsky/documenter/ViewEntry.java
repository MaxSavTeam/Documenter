package com.maxsavitsky.documenter;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.utils.RequestCodes;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.XMLParser;

import org.xml.sax.SAXException;

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
			setResult( ResultCodes.OK );

		mEntry.getProperties().setScrollPosition( ( (WebView) findViewById( mWebViewId ) ).getScrollY() );
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
								setResult( ResultCodes.NEED_TO_REFRESH );
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
									setResult( ResultCodes.NEED_TO_REFRESH );
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
			mEntry.getProperties().setScrollPosition( ((WebView) findViewById( mWebViewId )).getScrollY() );
			try {
				mEntry.saveProperties( mEntry.getProperties() );
			} catch (Exception e) {
				e.printStackTrace();
			}
			Intent intent = new Intent( this, CreateEntry.class );
			intent.putExtra( "type", "edit" );
			intent.putExtra( "id", mEntry.getId() );
			startActivityForResult( intent, RequestCodes.EDIT_ENTRY );
		}
		return super.onOptionsItemSelected( item );
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if(requestCode == RequestCodes.EDIT_ENTRY){
			if(resultCode == ResultCodes.REOPEN){
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

		WebView webView = findViewById( R.id.webView );
		mWebViewId = R.id.webView;
		WebSettings settings = webView.getSettings();
		settings.setAllowFileAccessFromFileURLs( true );
		settings.setAllowFileAccess( true );
		settings.setJavaScriptCanOpenWindowsAutomatically( false );
		settings.setDefaultFontSize( mEntry.getProperties().textSize );
		//webView.setBackgroundColor( entryProperty.getBgColor() );
		webView.loadUrl( "file://" + mEntry.getPathDir() + "text.html" );
		if( mEntry.getProperties().isSaveLastPos())
			webView.setScrollY( mEntry.getProperties().getScrollPosition() );
		else
			webView.setScrollY( 0 );
	}
}