package com.maxsavitsky.documenter.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.ThemeActivity;
import com.maxsavitsky.documenter.adapters.BackupsListAdapter;
import com.maxsavitsky.documenter.net.RequestMaker;
import com.maxsavitsky.documenter.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class CloudBackupsListActivity extends ThemeActivity {

	private SwipeRefreshLayout mSwipeRefreshLayout;

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if(item.getItemId() == android.R.id.home)
			onBackPressed();
		return super.onOptionsItemSelected( item );
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_cloud_backups_list );

		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );

		ActionBar actionBar = getSupportActionBar();
		if(actionBar != null)
			actionBar.setDisplayHomeAsUpEnabled( true );

		mSwipeRefreshLayout = findViewById( R.id.swipe_refresh_layout );
		mSwipeRefreshLayout.setOnRefreshListener( this::update );
		update();
	}

	private void update() {
		mSwipeRefreshLayout.setRefreshing( true );

		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if ( user == null ) {
			Toast.makeText( this, "Failed to get user", Toast.LENGTH_SHORT ).show();
			onBackPressed();
			return;
		}

		user.getIdToken( true )
				.addOnCompleteListener( task->{
					if ( task.isSuccessful() ) {
						update( task.getResult().getToken() );
					} else {
						onException( task.getException() );
					}
				} );
	}

	private void update(String authToken) {
		new Thread( ()->{
			String url = Utils.DOCUMENTER_API + "backups/getUserBackupsList?authToken=" + authToken;
			try {
				String result = RequestMaker.getRequestTo( url );
				JSONObject jsonObject = new JSONObject( result );
				if ( jsonObject.getString( "status" ).equals( "OK" ) ) {
					JSONArray jsonArray = jsonObject.getJSONArray( "list" );
					ArrayList<BackupEntity> entities = new ArrayList<>();
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject j = jsonArray.getJSONObject( i );
						entities.add( new BackupEntity(
								j.getLong( "creationTime" ),
								j.optString( "description", null ),
								j.getBoolean( "isManually" )
						) );
						runOnUiThread( ()->update( entities ) );
					}
				}else{
					onException( new RuntimeException( jsonObject.getString( "error_description" ) ) );
				}
			} catch (IOException | JSONException e) {
				e.printStackTrace();
				onException( e );
			}
		} ).start();
	}

	private void update(ArrayList<BackupEntity> backupEntities) {
		RecyclerView recyclerView = findViewById( R.id.recycler_view_backups_list );
		LinearLayoutManager manager = new LinearLayoutManager(this);
		manager.setOrientation( RecyclerView.VERTICAL );
		recyclerView.setLayoutManager(manager);

		BackupsListAdapter adapter = new BackupsListAdapter( backupEntities, new BackupsListAdapter.AdapterCallback() {
			@Override
			public void onBackupClick(BackupEntity backupEntity) {
				setResult( RESULT_OK, new Intent().putExtra( "time", backupEntity.time ) );
				onBackPressed();
			}
		} );
		recyclerView.setAdapter(adapter);

		mSwipeRefreshLayout.setRefreshing( false );
	}

	private void onException(Exception e) {
		runOnUiThread( ()->{
			Toast.makeText( this, e.toString(), Toast.LENGTH_LONG ).show();
			onBackPressed();
		} );
	}

	public static class BackupEntity {
		public final long time;
		public final String description;
		public final boolean isManually;

		public BackupEntity(long time, String description, boolean isManually) {
			this.time = time;
			this.description = description;
			this.isManually = isManually;
		}
	}

}