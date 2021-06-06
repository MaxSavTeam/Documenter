package com.maxsavitsky.documenter.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.adapters.EntitiesAdapter;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.data.types.Entity;

public class ChooseEntryActivity extends EntitiesListActivity {

	private final ActivityResultLauncher<Intent> mLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result->{
				if(result.getResultCode() == Results.ACCEPTED || result.getResultCode() == Results.DECLINED){
					setResult( result.getResultCode(), result.getData() );
					onBackPressed();
				}
			}
	);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );

		ActionBar actionBar = getSupportActionBar();
		if ( actionBar != null ) {
			actionBar.setDisplayHomeAsUpEnabled( true );
		}

		findViewById( R.id.fab_menu ).setVisibility( View.GONE );

		mEntitiesAdapter.setAdapterCallback( new EntitiesAdapter.AdapterCallback() {
			@Override
			public void onEntityClick(String id, Entity.Type type, int index) {
				if(type == Entity.Type.ENTRY){
					setResult( Results.ACCEPTED, new Intent().putExtra( "id", id ) );
					onBackPressed();
				}else{
					mLauncher.launch( new Intent(ChooseEntryActivity.this, ChooseEntryActivity.class).putExtra( "groupId", id ) );
				}
			}

			@Override
			public void onLongClick(int index) {

			}
		} );
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if(item.getItemId() == R.id.item_cancel){
			setResult( Results.DECLINED );
			onBackPressed();
		}
		return super.onOptionsItemSelected( item );
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate( R.menu.choose_menu, menu );
		menu.findItem( R.id.item_apply ).setVisible( false );
		return true;
	}
}