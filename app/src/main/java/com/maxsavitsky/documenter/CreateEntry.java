package com.maxsavitsky.documenter;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;
import android.view.View;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.Utils;

public class CreateEntry extends AppCompatActivity {

	private void applyTheme(){
		ActionBar actionBar = getSupportActionBar();
		if(actionBar != null){
			Utils.applyDefaultActionBarStyle(actionBar);
			actionBar.setTitle( "Create new entry" );
		}
	}

	private void backPressed(){
		setResult( ResultCodes.RESULT_CODE_OK );
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

		FloatingActionButton fab = findViewById( R.id.fabSaveEntry );
		fab.setOnClickListener( saveEntry );
	}

	View.OnClickListener saveEntry = new View.OnClickListener() {
		@Override
		public void onClick(View v) {

		}
	};

}
