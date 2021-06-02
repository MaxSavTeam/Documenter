package com.maxsavitsky.documenter.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.maxsavitsky.documenter.MainActivity;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.ThemeActivity;
import com.maxsavitsky.documenter.adapters.EntitiesAdapter;
import com.maxsavitsky.documenter.data.EntitiesStorage;
import com.maxsavitsky.documenter.data.types.Entity;
import com.maxsavitsky.documenter.data.types.Group;
import com.maxsavitsky.documenter.ui.widget.CustomRadioGroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

public class EntitiesListActivity extends ThemeActivity {

	private static final String TAG = MainActivity.TAG + " EntitiesList";
	private boolean isRoot;

	private Group mGroup;

	private int mSortOrder = 0, mSortingMode = 0, mAscendingDescendingOrder = 0;

	private EntitiesAdapter mEntitiesAdapter;

	private final EntitiesAdapter.AdapterCallback ADAPTER_CALLBACK = new EntitiesAdapter.AdapterCallback() {
		@Override
		public void onEntityClick(String id, Entity.Type type) {
			if ( type == Entity.Type.GROUP ) {
				startActivity( new Intent( EntitiesListActivity.this, EntitiesListActivity.class ).putExtra( "groupId", id ) );
			}
		}
	};

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int itemId = item.getItemId();
		if ( itemId == android.R.id.home ) {
			onBackPressed();
		} else if ( itemId == R.id.item_menu_sort_mode ) {
			showSortOptionsDialog();
		}
		return super.onOptionsItemSelected( item );
	}

	private void showSortOptionsDialog() {
		View view = LayoutInflater.from( this ).inflate( R.layout.sort_menu, null );
		CustomRadioGroup radioOrder = view.findViewById(R.id.radio_group_order);
		radioOrder.setCheckedIndex( mSortOrder );
		CustomRadioGroup radioSorting = view.findViewById( R.id.radio_group_sorting );
		radioSorting.setCheckedIndex( mSortingMode );
		CustomRadioGroup radioAscendingOrder = view.findViewById( R.id.radio_group_ascending_descending );
		radioAscendingOrder.setCheckedIndex( mAscendingDescendingOrder );
		AlertDialog.Builder builder = new AlertDialog.Builder( this, super.mAlertDialogStyle );
		builder
				.setCancelable( false )
				.setNegativeButton( R.string.cancel, (dialog, which)->{
					dialog.cancel();
				} )
				.setPositiveButton( R.string.apply, (dialog, which)->{
					mSortOrder = radioOrder.getCheckedItemIndex();
					mSortingMode = radioSorting.getCheckedItemIndex();
					mAscendingDescendingOrder = radioAscendingOrder.getCheckedItemIndex();
					sortAndUpdateList();
				} )
				.setView( view );
		builder.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate( R.menu.entities_list_menu, menu );
		if ( isRoot ) {
			menu.findItem( R.id.item_menu_delete ).setVisible( false );
			menu.findItem( R.id.item_menu_change_name ).setVisible( false );
		}
		return super.onCreateOptionsMenu( menu );
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_entities_list );

		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );

		String groupId = getIntent().getStringExtra( "groupId" );
		isRoot = groupId == null;

		if ( isRoot ) {
			initializeRecyclerView( EntitiesStorage.get().getRootEntities() );
		} else {
			ActionBar actionBar = getSupportActionBar();
			if ( actionBar != null ) {
				actionBar.setDisplayHomeAsUpEnabled( true );
			}

			Optional<Group> op = EntitiesStorage.get().getGroup( groupId );
			if ( !op.isPresent() ) {
				Toast.makeText( this, "Group not found", Toast.LENGTH_SHORT ).show();
				onBackPressed();
				return;
			}
			mGroup = op.get();

			setTitle( mGroup.getName() );

			initializeRecyclerView( mGroup.getContainingEntities() );
			sortAndUpdateList();
		}
	}

	private void initializeRecyclerView(ArrayList<? extends Entity> entities) {
		RecyclerView recyclerView = findViewById( R.id.recycler_view_entities );
		LinearLayoutManager manager = new LinearLayoutManager( this );
		manager.setOrientation( RecyclerView.VERTICAL );
		recyclerView.setLayoutManager( manager );
		mEntitiesAdapter = new EntitiesAdapter( entities, ADAPTER_CALLBACK );
		recyclerView.setAdapter( mEntitiesAdapter );
	}

	private void sortAndUpdateList() {
		ArrayList<? extends Entity> entities;
		if(isRoot)
			entities = EntitiesStorage.get().getRootEntities();
		else
			entities = mGroup.getContainingEntities();
		entities.sort( (Comparator<Entity>) (o1, o2)->{
			if(o1.getType() != o2.getType()){
				if(mSortOrder == 0)
					return o1.getType() == Entity.Type.GROUP ? -1 : 1;
				else if(mSortOrder == 1)
					return o1.getType() == Entity.Type.GROUP ? 1 : -1;
			}
			int comp = 0;
			if(mSortingMode == 0){
				comp = o1.getName().compareToIgnoreCase( o2.getName() );
			}else{
				comp = Long.compare( o1.getCreationTimestamp(), o2.getCreationTimestamp() );
			}
			if(mAscendingDescendingOrder == 1)
				comp = -comp;
			return comp;
		} );
		updateList( entities );
	}

	private void updateList(ArrayList<? extends Entity> entities) {
		mEntitiesAdapter.setElements( entities );
	}
}