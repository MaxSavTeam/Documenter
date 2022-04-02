package com.maxsavitsky.documenter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.maxsavitsky.documenter.backup.BackupInstruments;
import com.maxsavitsky.documenter.backup.CloudBackupMaker;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.data.DataReformatter;
import com.maxsavitsky.documenter.data.EntitiesStorage;
import com.maxsavitsky.documenter.data.types.Entity;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.data.types.Group;
import com.maxsavitsky.documenter.net.RequestMaker;
import com.maxsavitsky.documenter.ui.EntitiesListActivity;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavteam.updateschecker.VersionInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends ThemeActivity {

	private final ActivityResultLauncher<Intent> mEntitiesListLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result->{
				if ( result.getResultCode() == Results.RESTART_APP ) {
					restartApp();
				} else if ( result.getResultCode() == RESULT_OK || result.getResultCode() == RESULT_CANCELED ) {
					onBackPressed();
				}
			}
	);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		SharedPreferences sharedPreferences = getSharedPreferences( Utils.APP_PREFERENCES, Context.MODE_PRIVATE );
		Utils.setDefaultSharedPreferences( sharedPreferences );

		super.onCreate( savedInstanceState );
		setContentView( R.layout.layout_onstartup );
		try {
			Toolbar toolbar = findViewById( R.id.toolbar );
			setSupportActionBar( toolbar );
		} catch (Exception e) {
			e.printStackTrace();
		}

		CloudBackupMaker.init( this );

		deleteInstalledApks();

		checkForUpdates();
		startInitialization();
	}

	private void checkForUpdates() {
		Activity a = this;
		com.maxsavteam.updateschecker.Utils.runFullCheck( this, Utils.getDefaultSharedPreferences().getInt( "updates_channel", 0 ), new com.maxsavteam.updateschecker.Utils.FullCheckCallback() {
			@Override
			public void onUpdateAvailable(VersionInfo versionInfo) {

			}

			@Override
			public File createDestinationFile(com.maxsavteam.updateschecker.VersionInfo versionInfo) {
				File file = new File( App.appStoragePath, "updates" );
				if ( !file.exists() ) {
					file.mkdirs();
				}
				file = new File( file, versionInfo.getVersionName() );
				if ( !file.exists() ) {
					try {
						file.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
						onFailure( e );
						return null;
					}
				}
				return file;
			}

			@Override
			public void onNoUpdates() {

			}

			@Override
			public void onFailure(Exception e) {
				runOnUiThread( ()->{
					Toast.makeText( a, e.toString(), Toast.LENGTH_SHORT ).show();
				} );
			}
		} );
	}

	private void startInitialization() {
		new Thread( ()->{
			try {
				runReformatIfNeeded();
				initialize();
				Log.i( App.TAG, "Initialized" );
			} catch (final Exception e) {
				runOnUiThread( ()->Utils.getErrorDialog( e, MainActivity.this ).show() );
				return;
			}
			getLastBackupTimeAndStartWorker();
			Intent intent = new Intent( this, EntitiesListActivity.class );
			mEntitiesListLauncher.launch( intent );
		} ).start();
	}

	private void runReformatIfNeeded() throws Exception {
		File file = new File( App.appDataPath );
		if ( !file.exists() ) {
			file.mkdirs();
		}
		file = new File( App.appDataPath, "data.json" );
		if ( !file.exists() ) {
			if ( new File( App.appStoragePath, "entries.xml" ).exists() &&
					new File( App.appStoragePath, "documents.xml" ).exists() &&
					new File( App.appStoragePath, "categories.xml" ).exists() ) {
				File f = new File( App.appStoragePath, "backups" );
				if ( !f.exists() ) {
					f.mkdirs();
				}
				BackupInstruments.createOldBackup( new File( f, "before_reformat_backup.zip" ), null );
				DataReformatter.runReformat();
			}
		}
	}

	private void getLastBackupTimeAndStartWorker(){
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if(user == null)
			return;

		user.getIdToken( true )
				.addOnSuccessListener( getTokenResult->getLastBackupTime( getTokenResult.getToken() ) );
	}

	private void getLastBackupTime(String token){
		new Thread(()->{
			String url = Utils.DOCUMENTER_API + "backups/getLastBackup?authToken=" + token;
			try {
				String result = RequestMaker.getRequestTo( url );
				JSONObject jsonObject = new JSONObject( result );
				JSONObject last = jsonObject.optJSONObject( "backup" );
				if ( last == null ) {
					return;
				}
				long time = last.getLong( "creationTime" );

				runOnUiThread( ()->CloudBackupMaker.getInstance().startWorker(time) );
			} catch (IOException | JSONException e) {
				e.printStackTrace();
			}
		}).start();
	}

	private void initialize() throws Exception {
		File dataFile = new File( App.appDataPath, "data.json" );
		if ( !dataFile.exists() ) {
			dataFile.createNewFile();
			EntitiesStorage.get().setGroups( new ArrayList<>() {{
				add( new Group( "root", "root" ) );
			}} );
			EntitiesStorage.get().save(); // create file with empty data
			return;
		}
		JSONObject data;
		try (
				FileInputStream fis = new FileInputStream( dataFile );
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
		) {
			int len;
			byte[] buffer = new byte[ 1024 ];
			while ( ( len = fis.read( buffer ) ) != -1 ) {
				outputStream.write( buffer, 0, len );
			}
			data = new JSONObject( outputStream.toString() );
		}
		ArrayList<Entry> entries = new ArrayList<>();
		JSONArray array = data.getJSONArray( "entries" );
		for (int i = 0; i < array.length(); i++) {
			JSONObject jsonObject = array.getJSONObject( i );
			Entry e = new Entry( jsonObject.getString( "id" ), jsonObject.getString( "name" ) );
			e.setCreationTimestamp( jsonObject.getLong( "timestamp" ) );
			entries.add( e );
		}

		ArrayList<Group> groups = new ArrayList<>();
		array = data.getJSONArray( "groups" );
		for (int i = 0; i < array.length(); i++) {
			JSONObject jsonObject = array.getJSONObject( i );
			Group e = new Group( jsonObject.getString( "id" ), jsonObject.getString( "name" ) );
			e.setCreationTimestamp( jsonObject.getLong( "timestamp" ) );
			groups.add( e );
		}

		JSONObject groupsContent = data.getJSONObject( "groupsContent" );
		Iterator<String> keys = groupsContent.keys();
		while ( keys.hasNext() ) {
			String key = keys.next();
			Group group = null;
			for (Group g : groups) {
				if ( g.getId().equals( key ) ) {
					group = g;
					break;
				}
			}
			if ( group != null ) {
				ArrayList<Entity> containingEntities = new ArrayList<>();
				JSONArray containing = groupsContent.getJSONArray( key );
				for (int i = 0; i < containing.length(); i++) {
					String id = containing.getString( i );
					for (Group g : groups)
						if ( g.getId().equals( id ) ) {
							containingEntities.add( g );
							g.addParent( key );
							break;
						}
					for (Entry e : entries)
						if ( e.getId().equals( id ) ) {
							containingEntities.add( e );
							e.addParent( key );
							break;
						}

				}
				group.setContainingEntities( containingEntities );
			}
		}
		EntitiesStorage.get().setEntryEntities( entries );
		EntitiesStorage.get().setGroups( groups );
	}

	private void deleteInstalledApks() {
		File file = new File( App.appStoragePath, "updates" );
		if ( file.exists() ) {
			File[] files = file.listFiles();
			if ( files != null ) {
				for (File subFile : files) {
					subFile.delete();
				}
			}

			file.delete();
		}
	}

	private void restartApp() {
		Intent intent = new Intent( this, MainActivity.class );
		startActivity( intent );
		this.finish();
	}
}
