package com.maxsavitsky.documenter.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.ThemeActivity;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.data.EntitiesStorage;
import com.maxsavitsky.documenter.utils.Utils;

public class CreateGroupActivity extends ThemeActivity {

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if(item.getItemId() == android.R.id.home)
			onBackPressed();
		return super.onOptionsItemSelected( item );
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_create_group );

		String parentId = getIntent().getStringExtra( "parentId" );

		setSupportActionBar( findViewById( R.id.toolbar2 ) );

		ActionBar actionBar = getSupportActionBar();
		if(actionBar != null)
			actionBar.setDisplayHomeAsUpEnabled( true );

		findViewById( R.id.fabCreate ).setOnClickListener( v->{
			String name = (( EditText ) findViewById( R.id.editText )).getText().toString();
			name = name.trim();
			if(name.length() > 0){
				EntitiesStorage.get().createGroup( name, parentId );
				setResult( Results.NEED_TO_REFRESH );
				onBackPressed();
			}
		} );

		Utils.showKeyboard( findViewById( R.id.editText ), this );
	}
}