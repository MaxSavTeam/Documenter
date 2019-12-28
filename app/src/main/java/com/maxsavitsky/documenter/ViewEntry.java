package com.maxsavitsky.documenter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import com.maxsavitsky.documenter.datatypes.Entry;
import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.utils.RequestCodes;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.Utils;

import java.util.ArrayList;

public class ViewEntry extends AppCompatActivity {

	private Entry mEntry;
	private SharedPreferences sp;
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
			try {
				if ( MainData.finallyDeleteEntryWithId( mEntry.getId() ) ){
					setResult( ResultCodes.NEED_TO_REFRESH );
					finish();
				}else{
					Toast.makeText( this, "Failed", Toast.LENGTH_SHORT ).show();
				}
			}catch (Exception e){
				e.printStackTrace();
				Toast.makeText( this, e.toString(), Toast.LENGTH_LONG ).show();
			}
		}else if(item.getItemId() == R.id.item_edit_entry_text){
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
		return super.onCreateOptionsMenu( menu );
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		Intent intent = getIntent();
		mEntry = MainData.getEntryWithId( intent.getStringExtra( "id" ) );
		applyTheme();

		sp = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

		WebView webView = new WebView( this );
		webView.getSettings().setAllowFileAccessFromFileURLs( true );
		webView.getSettings().setAllowFileAccess( true );
		webView.getSettings().setJavaScriptCanOpenWindowsAutomatically( false );
		webView.getSettings().setDefaultFontSize( sp.getInt( "default_webview_font_size_sp", 22 ) );
		webView.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT ) );
		webView.loadUrl( "file://" + mEntry.getPathDir() + "text.html" );
		setContentView( webView );
	}
}