package com.maxsavitsky.documenter;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Layout;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
		String t = ( (EditText) findViewById( R.id.edittextEntry ) ).getText().toString();
		if ( !t.isEmpty() && ( !type.equals( "edit" ) || !mEntryProperty.equals( mEntry.getProperty() ) ) ) {
			AlertDialog.Builder builder = new AlertDialog.Builder( this )
					.setTitle( R.string.confirmation )
					.setMessage( R.string.create_entry_exit_mes ).setCancelable( false )
					.setNeutralButton( R.string.cancel, new DialogInterface.OnClickListener() {
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
			mEntry.setProperty( new EntryProperty( mEntryProperty ) );
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

		ImageButton imageButton;
		if(mEntryProperty.getTextAlignment() == Gravity.CENTER_HORIZONTAL)
			imageButton = findViewById( R.id.btnAlignCenter );
		else if(mEntryProperty.getTextAlignment() == Gravity.START)
			imageButton = findViewById( R.id.btnAlignLeft );
		else
			imageButton = findViewById( R.id.btnAlignRight );

		/*if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ){
			findViewById( R.id.btnAlignJustify ).setVisibility( View.VISIBLE );
			if(mEntryProperty.getTextAlignment() == Layout.JUSTIFICATION_MODE_INTER_WORD){
				imageButton = findViewById( R.id.btnAlignJustify );
			}
		}*/

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
						mEntry.saveProperties( mEntryProperty );
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
