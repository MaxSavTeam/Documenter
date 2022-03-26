package com.maxsavitsky.documenter.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.ThemeActivity;
import com.maxsavitsky.documenter.adapters.EntitiesAdapter;
import com.maxsavitsky.documenter.adapters.SearchEntitiesAdapter;
import com.maxsavitsky.documenter.data.EntitiesStorage;
import com.maxsavitsky.documenter.data.types.Entity;
import com.maxsavitsky.documenter.data.types.Group;
import com.maxsavitsky.documenter.utils.SearchUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class SearchActivity extends ThemeActivity {

	private final ActivityResultLauncher<Intent> entitiesListLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result->{}
	);

	private final ActivityResultLauncher<Intent> entryViewerLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result->{}
	);

	private boolean isRoot = false;

	private Group group;

	private RecyclerView recyclerView;
	private SearchEntitiesAdapter adapter;
	private ProgressBar progressBar;

	private final HashMap<String, String> parentIds = new HashMap<>();

	private final EntitiesAdapter.AdapterCallback adapterCallback = new EntitiesAdapter.AdapterCallback() {
		@Override
		public void onEntityClick(String id, Entity.Type type, int index) {
			String parentId = parentIds.get( id );
			if ( type == Entity.Type.GROUP ) {
				entitiesListLauncher.launch(
						new Intent( SearchActivity.this, EntitiesListActivity.class )
								.putExtra( "groupId", id )
								.putExtra( "parentId", parentId )
				);
			}else{
				entryViewerLauncher.launch(
						new Intent( SearchActivity.this, EntryViewer.class )
								.putExtra( "id", id )
								.putExtra( "parentId", parentId )
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
		if ( item.getItemId() == android.R.id.home ) {
			onBackPressed();
		}
		return super.onOptionsItemSelected( item );
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_search );

		String groupId = getIntent().getStringExtra( "groupId" );
		if ( groupId == null || groupId.equals( "root" ) ) {
			isRoot = true;
		}

		Optional<Group> op = EntitiesStorage.get().getGroup( groupId );
		if ( !op.isPresent() ) {
			Toast.makeText( this, "Group not found", Toast.LENGTH_SHORT ).show();
			onBackPressed();
			return;
		}
		group = op.get();

		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );

		if ( isRoot ) {
			setTitle( R.string.search );
		} else {
			setTitle( getString( R.string.search_in ) + " " + group.getName() );
		}

		ActionBar actionBar = getSupportActionBar();
		if ( actionBar != null ) {
			actionBar.setDisplayHomeAsUpEnabled( true );
		}

		recyclerView = findViewById( R.id.recycler_view );
		adapter = new SearchEntitiesAdapter( adapterCallback );
		recyclerView.setAdapter( adapter );
		recyclerView.setLayoutManager( new LinearLayoutManager( this ) );

		progressBar = findViewById( R.id.progress_bar );

		EditText editText = findViewById( R.id.search_edit_text );
		editText.setOnEditorActionListener( (v, actionId, event)->{
			if ( actionId == getResources().getInteger( R.integer.search_action_id ) ) {
				String text = v.getText().toString();
				performSearch( text );
				progressBar.setIndeterminate( true );
				return true;
			}
			return false;
		} );
	}

	private void onEntityFound(Entity entity, List<Entity> path) {
		parentIds.put( entity.getId(), path.get( path.size() - 1 ).getId() );
		StringBuilder pathString = new StringBuilder();
		for(int i = 1; i < path.size(); i++){ // ignore first, because it is current group
			pathString.append( path.get( i ).getName() );
			if(i != path.size() - 1){
				pathString.append( " > " );
			}
		}
		adapter.add( entity, pathString.toString() );
	}

	private void performSearch(String text) {
		progressBar.setIndeterminate( true );
		adapter.clear();
		parentIds.clear();
		new Thread( ()->{
			SearchUtil.search( group, text, new SearchUtil.SearchCallback() {
				@Override
				public void onEntityFound(Entity entity, List<Entity> path) {
					// we should copy list, because path is passed by reference
					// and by the time runnable is called, in list will be another data
					// because search util will continue to work
					ArrayList<Entity> copy = new ArrayList<>(path);
					runOnUiThread( ()->SearchActivity.this.onEntityFound( entity, copy ) );
				}

				@Override
				public void onFinish() {
					runOnUiThread( ()->progressBar.setIndeterminate( false ) );
				}
			} );
		} ).start();
	}

}