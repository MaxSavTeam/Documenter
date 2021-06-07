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

public class CopyMoveActivity extends EntitiesListActivity {

	private int mode;

	private final ActivityResultLauncher<Intent> mLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result->{
				if(result.getResultCode() == Results.DECLINED || result.getResultCode() == Results.ACCEPTED){
					setResult( result.getResultCode(), result.getData() );
					onBackPressed();
				}
			}
	);

	private final EntitiesAdapter.AdapterCallback mAdapterCallback = new EntitiesAdapter.AdapterCallback() {
		@Override
		public void onEntityClick(String id, Entity.Type type, int index) {
			if ( type == Entity.Type.GROUP ) {
				mLauncher.launch(
						new Intent( CopyMoveActivity.this, CopyMoveActivity.class )
								.putExtra( "groupId", id )
								.putExtra( "mode", mode )
				);
			}
		}

		@Override
		public void onLongClick(int index) {

		}

		@Override
		public View getViewAt(int position) {
			return null;
		}
	};

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if(item.getItemId() == R.id.item_cancel){
			setResult( Results.DECLINED );
			onBackPressed();
		}else if(item.getItemId() == R.id.item_apply){
			Intent data = new Intent();
			data.putExtra( "groupId", mGroup.getId() );
			data.putExtra( "mode", mode );
			setResult( Results.ACCEPTED, data );
			onBackPressed();
		}
		return super.onOptionsItemSelected( item );
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );

		ActionBar actionBar = getSupportActionBar();
		if ( actionBar != null ) {
			actionBar.setDisplayHomeAsUpEnabled( true );
		}

		mEntitiesAdapter.setAdapterCallback( mAdapterCallback );

		findViewById( R.id.fab_menu ).setVisibility( View.GONE );

		mode = getIntent().getIntExtra( "mode", 0 );

		int sId;
		if ( mode == 0 ) {
			sId = R.string.add_to;
		} else {
			sId = R.string.move_to;
		}
		if ( isRoot ) {
			setTitle( getString( sId ) + " " + getString( R.string.root ).toLowerCase() );
		} else {
			setTitle( getString( sId ) + " " + mGroup.getName() );
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate( R.menu.choose_menu, menu );
		return true;
	}
}