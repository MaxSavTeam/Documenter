package com.maxsavitsky.documenter;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.maxsavitsky.documenter.datatypes.Document;
import com.maxsavitsky.documenter.datatypes.Entry;
import com.maxsavitsky.documenter.datatypes.Info;
import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;

public class CreateEntry extends AppCompatActivity {
	private Document mDocument;
	private String type;
	private Entry mEntry;
	private String title = "Create new entry";

	private void applyTheme(){
		ActionBar actionBar = getSupportActionBar();
		if(actionBar != null){
			Utils.applyDefaultActionBarStyle(actionBar);
			actionBar.setTitle( title );
		}
	}

	private void _finishActivity(){
		finish();
	}

	private void backPressed(){
		setResult( ResultCodes.OK );
		finish();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if(item.getItemId() == android.R.id.home){
			backPressed();
		}
		return super.onOptionsItemSelected( item );
	}

	@Override
	public void onBackPressed() {
		backPressed();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_create_entry );
		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );

		applyTheme();
		type = getIntent().getStringExtra( "type" );
		if(type.equals( "edit" )){
			mEntry = MainData.getEntryWithId( getIntent().getStringExtra( "id" ) );
			title = "Edit entry text";
			applyTheme();
			EditText editText = findViewById( R.id.edittextEntry );
			try {
				editText.setText( Html.fromHtml( mEntry.loadText() ) );
			}catch (Exception e){
				e.printStackTrace();
				Toast.makeText( this, "loadText()\n\n" + e.toString(), Toast.LENGTH_LONG ).show();
				return;
			}
		}else{
			mDocument = MainData.getDocumentWithId( getIntent().getStringExtra( "id" ) );
		}

		FloatingActionButton fab = findViewById( R.id.fabSaveEntry );
		fab.setOnClickListener( saveEntry );

		Utils.showKeyboard( (EditText) findViewById( R.id.edittextEntry ), this );
	}

	private void createEntry(String name, String text){
		String id = Utils.generateUniqueId() + "_ent";
		Entry entry = new Entry( id, name );
		ArrayList<Entry> entries = MainData.getEntriesList();
		entries.add( entry );
		MainData.setEntriesList( entries );
		Utils.saveEntriesList( entries );
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries" );
		if(!file.exists())
			file.mkdir();
		file = new File( Utils.getExternalStoragePath().getPath() + "/entries/" + id );
		file.mkdir();
		file = new File( file.getPath() + "/included_in.xml" );
		try{
			file.createNewFile();
		}catch (Exception e){
			Toast.makeText( this, "createNewFile\n\n" + e.toString(), Toast.LENGTH_LONG ).show();
		}
		try {
			FileWriter fr = new FileWriter( file, false );
			fr.write( Utils.xmlHeader + "<documents>\n</documents>" );
			fr.flush();
			fr.close();
			entry.addDocumentToIncluded( mDocument.getId() );
			entry.saveText( text );
			entry.setAndSaveInfo( new Info( (int) new Date().getTime() ) );
			mDocument.addEntry( entry );
			setResult( ResultCodes.NEED_TO_REFRESH );
			finish();
		}catch (Exception e){
			Toast.makeText( this, "createEntry\n\n" + e.toString(), Toast.LENGTH_LONG ).show();
			e.printStackTrace();
		}
	}

	View.OnClickListener saveEntry = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			final EditText editText = findViewById( R.id.edittextEntry );
			if(type.equals( "create" )) {
				if ( editText.getText().length() != 0 ) {
					AlertDialog alertDialog;
					final EditText name = new EditText( CreateEntry.this );
					name.setId( View.NO_ID );
					name.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT ) );
					AlertDialog.Builder builder = new AlertDialog.Builder( CreateEntry.this )
							.setView( name )
							.setTitle( R.string.enter_name )
							.setMessage( R.string.name_yours_minds )
							.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
									String n = name.getText().toString();
									createEntry( n, editText.getText().toString() );
								}
							} )
							.setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
								}
							} );

					alertDialog = builder.create();
					alertDialog.show();
				} else {
					editText.requestFocus();
				}
			}else if(type.equals( "edit" )){
				String text = editText.getText().toString();
				if(!text.isEmpty()){
					try {
						mEntry.saveText( text );
						setResult( ResultCodes.REOPEN, new Intent(  ).putExtra( "id", mEntry.getId() ) );
						_finishActivity();
					}catch (Exception e){
						Toast.makeText( CreateEntry.this, "edit text\n\n" + e.toString(), Toast.LENGTH_LONG ).show();
						e.printStackTrace();
					}
				}
			}
		}
	};

}
