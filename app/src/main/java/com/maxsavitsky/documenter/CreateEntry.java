package com.maxsavitsky.documenter;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.datatypes.Document;
import com.maxsavitsky.documenter.datatypes.Entry;
import com.maxsavitsky.documenter.datatypes.EntryProperty;
import com.maxsavitsky.documenter.datatypes.Info;
import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.XMLParser;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import yuku.ambilwarna.AmbilWarnaDialog;

public class CreateEntry extends AppCompatActivity {
	private Document mDocument;
	private String type;
	private Entry mEntry;
	private String title = "Create new entry";
	private EntryProperty mEntryProperty;

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
		mEntryProperty = new EntryProperty();
		if ( type != null && type.equals( "edit" ) ) {
			mEntry = MainData.getEntryWithId( getIntent().getStringExtra( "id" ) );
			try {
				mEntryProperty = new XMLParser().parseEntryProperties( mEntry.getId() );
			}catch (Exception e){
				Utils.getErrorDialog( e, this ).show();
			}
			title = "Edit entry text";
			applyTheme();
			EditText editText = findViewById( R.id.edittextEntry );
			try {
				editText.setText( Html.fromHtml( mEntry.loadText() ) );
			}catch (Exception e){
				e.printStackTrace();
				Utils.getErrorDialog( e, this ).show();
				return;
			}
		}else{
			mDocument = MainData.getDocumentWithId( getIntent().getStringExtra( "id" ) );
		}
		setEditTextSize();
		FloatingActionButton fab = findViewById( R.id.fabSaveEntry );
		fab.setOnClickListener( saveEntry );

		Utils.showKeyboard( (EditText) findViewById( R.id.edittextEntry ), this );
	}

	@Override
	protected void onPostCreate(@Nullable Bundle savedInstanceState) {
		super.onPostCreate( savedInstanceState );
		final Button btnBgColorPicker = findViewById( R.id.btnBgColorPicker );
		final EditText editText = findViewById( R.id.edittextEntry );
		btnBgColorPicker.setOnClickListener( new View.OnClickListener() {
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
						editText.setBackgroundColor( color );
						mEntryProperty.setBgColor( color );
						//Toast.makeText( CreateEntry.this, Integer.toString( color ), Toast.LENGTH_SHORT ).show();
					}
				} ).show();
			}
		} );

		final Button btnTextColorPicker = findViewById( R.id.btnTextColorPicker );
		btnTextColorPicker.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new AmbilWarnaDialog( CreateEntry.this, mEntryProperty.getTextColor(), new AmbilWarnaDialog.OnAmbilWarnaListener() {
					@Override
					public void onCancel(AmbilWarnaDialog dialog) {

					}

					@Override
					public void onOk(AmbilWarnaDialog dialog, int color) {
						btnTextColorPicker.setBackgroundTintList( ColorStateList.valueOf( color ) );
						//btnBgColorPicker.setBackground( getDrawable( R.drawable.btn_picker_borders ) );
						editText.setTextColor( color );
						mEntryProperty.setTextColor( color );
						//Toast.makeText( CreateEntry.this, Integer.toString( color ), Toast.LENGTH_SHORT ).show();
					}
				} ).show();
			}
		} );

		btnBgColorPicker.setBackgroundTintList( ColorStateList.valueOf( mEntryProperty.getBgColor() ) );
		editText.setBackgroundColor( mEntryProperty.getBgColor() );

		btnTextColorPicker.setBackgroundTintList( ColorStateList.valueOf( mEntryProperty.getTextColor() ) );
		editText.setTextColor( mEntryProperty.getTextColor() );
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
		((EditText) findViewById( R.id.edittextEntry )).setTextSize( TypedValue.COMPLEX_UNIT_SP, (float) mEntryProperty.textSize );
		TextView t = findViewById( R.id.textViewTextSize );
		t.setText( String.format( Locale.ROOT, "%d", mEntryProperty.textSize ) );
	}

	private void createEntry(String name, String text){
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
			saveProperties();
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

	private void saveProperties()throws Exception{
		File file = new File( mEntry.getPathDir() + "properties.xml" );
		if ( !file.exists() )
			file.createNewFile();
		FileWriter fr = null;
		fr = new FileWriter( file, false );
		fr.write( Utils.xmlHeader );
		fr.append( "<properties>\n" )
				.append( "\t<textSize value=\"" ).append( String.format( Locale.ROOT, "%d", mEntryProperty.textSize ) ).append( "\" />\n" )
				.append( "\t<bgColor value=\"" ).append( String.format( Locale.ROOT, "%d", mEntryProperty.getBgColor() ) ).append( "\" />\n" )
				.append( "\t<textColor value=\"" ).append( String.format( Locale.ROOT, "%d", mEntryProperty.getTextColor() ) ).append( "\" />\n" )
				.append( "</properties>" );

		fr.flush();
		fr.close();
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
									createEntry( n, editText.getText().toString() );
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
					editText.requestFocus();
				}
			}else if(type.equals( "edit" )){
				String text = editText.getText().toString();
				if(!text.isEmpty()){
					try {
						saveProperties();
						mEntry.saveText( text, mEntryProperty );
						setResult( ResultCodes.REOPEN, new Intent(  ).putExtra( "id", mEntry.getId() ) );
					}catch (Exception e){
						Toast.makeText( CreateEntry.this, "edit text\n\n" + e.toString(), Toast.LENGTH_LONG ).show();
						e.printStackTrace();
						Utils.getErrorDialog( e, CreateEntry.this ).show();
						return;
					}
					_finishActivity();
				}
			}
		}
	};

}
