package com.maxsavitsky.documenter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.maxsavitsky.documenter.backup.BackupInstruments;
import com.maxsavitsky.documenter.backup.CloudBackupMaker;
import com.maxsavitsky.documenter.codes.Requests;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.data.DataReformatter;
import com.maxsavitsky.documenter.data.EntitiesStorage;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.types.Entity;
import com.maxsavitsky.documenter.data.types.EntryEntity;
import com.maxsavitsky.documenter.data.types.Group;
import com.maxsavitsky.documenter.ui.CategoryList;
import com.maxsavitsky.documenter.ui.EntitiesListActivity;
import com.maxsavitsky.documenter.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends ThemeActivity {
	public static final String TAG = "Documenter";

	private static MainActivity instance;

	public static MainActivity getInstance() {
		return instance;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Utils.setContext( this );
		SharedPreferences sharedPreferences = getSharedPreferences( Utils.APP_PREFERENCES, Context.MODE_PRIVATE );
		Utils.setDefaultSharedPreferences( sharedPreferences );

		super.onCreate( savedInstanceState );
		instance = this;
		setContentView( R.layout.layout_onstartup );
		try {
			Toolbar toolbar = findViewById( R.id.toolbar );
			setSupportActionBar( toolbar );
		} catch (Exception e) {
			e.printStackTrace();
		}

		deleteInstalledApks();

		final CloudBackupMaker backupper = new CloudBackupMaker( this );
		new Thread( backupper::stateChanged, "AutoBackupper" ).start();
	}

	@Override
	protected void onPostCreate(@Nullable Bundle savedInstanceState) {
		super.onPostCreate( savedInstanceState );
		new Thread( ()->{
			try {
				runReformatIfNeeded();
				initialize();
				Log.i( TAG, "Initialized" );
			} catch (final Exception e) {
				runOnUiThread( ()->Utils.getErrorDialog( e, MainActivity.this ).show() );
				return;
			}
			try {
				Thread.sleep( 1000 );
				Intent intent = new Intent(this, EntitiesListActivity.class );
				startActivity( intent );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} ).start();
	}

	private void runReformatIfNeeded() throws Exception {
		File file = new File( Utils.getExternalStoragePath().getPath() + "/data.json" );
		if ( !file.exists() ) {
			BackupInstruments.createBackupToFile( new File( Utils.getExternalStoragePath().getPath() + "/before_reformat_backup.zip" ), null );
			DataReformatter.runReformat();
		}
	}

	private void initialize() throws Exception {
		JSONObject data;
		try (
				FileInputStream fis = new FileInputStream( Utils.getExternalStoragePath().getPath() + "/data.json" );
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
		) {
			int len;
			byte[] buffer = new byte[ 1024 ];
			while ( ( len = fis.read( buffer ) ) != -1 ) {
				outputStream.write( buffer, 0, len );
			}
			data = new JSONObject( outputStream.toString() );
		}
		ArrayList<EntryEntity> entries = new ArrayList<>();
		JSONArray array = data.getJSONArray( "entries" );
		for (int i = 0; i < array.length(); i++) {
			JSONObject jsonObject = array.getJSONObject( i );
			EntryEntity e = new EntryEntity( jsonObject.getString( "id" ), jsonObject.getString( "name" ) );
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
			for(Group g : groups){
				if(g.getId().equals( key )) {
					group = g;
					break;
				}
			}
			if(group != null) {
				ArrayList<Entity> containingEntities = new ArrayList<>();
				JSONArray containing = groupsContent.getJSONArray( key );
				for (int i = 0; i < containing.length(); i++) {
					String id = containing.getString( i );
					for(Group g : groups)
						if(g.getId().equals( id )){
							containingEntities.add( g );
							g.addParent( key );
							break;
						}
					for(EntryEntity e : entries)
						if(e.getId().equals( id )){
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

	public void clearAllTraces(View v) {
		File file = new File( Utils.getExternalStoragePath().getPath() + "/stacktraces/" );
		if ( file.exists() ) {
			int count = 0;
			File[] files = file.listFiles();
			for (File subFile : files) {
				subFile.delete();
				count++;
			}
			file.delete();
			Toast.makeText( this, "Deleted " + count + " files", Toast.LENGTH_SHORT ).show();
		}
	}

	private void deleteInstalledApks() {
		File file = new File( Environment.getExternalStorageDirectory().getPath() + "/.documenter" );
		if ( file.exists() ) {
			File[] files = file.listFiles();
			for (File subFile : files) {
				subFile.delete();
			}

			file.delete();
		}
	}

	private long getUsedMemory() {
		Runtime runtime = Runtime.getRuntime();
		return runtime.totalMemory() - runtime.freeMemory();
	}

	private String getMemoryInfo() {
		Runtime runtime = Runtime.getRuntime();
		final double MB = 1024 * 1024;
		return "Total memory: " + ( runtime.totalMemory() / MB ) + " MB\n" +
				"Free memory: " + ( runtime.freeMemory() / MB ) + " MB\n" +
				"Used memory: " + ( getUsedMemory() / MB ) + " MB";
	}

	public void getUsedMemory(View v) {
		Toast.makeText( this, getMemoryInfo(), Toast.LENGTH_LONG ).show();
	}

	public void reinitialize(View v) {
		MainData.clearAll();

		try {
			initialize();

			Toast.makeText( this, "Successful\n\n" + getMemoryInfo(), Toast.LENGTH_SHORT ).show();
		} catch (Exception e) {
			Utils.getErrorDialog( e, this ).show();
		}
	}

	public void clearRam(View v) {
		MainData.clearAll();
		System.gc();
		int MB = 1024 * 1024;
		Toast.makeText( this, "Memory cleared\n\n" + getMemoryInfo(), Toast.LENGTH_SHORT ).show();
	}

	public void viewCategoryList(View v) {
		Intent intent = new Intent( this, CategoryList.class );
		startActivityForResult( intent, Requests.CATEGORY_LIST );
	}

	public void makeError(View v) {
		throw new RuntimeException( "Test exception" );
	}

	private void restartApp() {
		Intent intent = new Intent( this, MainActivity.class );
		startActivity( intent );
		this.finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if ( requestCode == Requests.CATEGORY_LIST ) {
			if ( resultCode == Results.EXIT ) {
				finishAndRemoveTask();
			}
			if ( resultCode == Results.RESTART_ACTIVITY ) {
				viewCategoryList( null );
			}
			if ( resultCode != Results.LOOK_STARTUP ) {
				setContentView( R.layout.activity_main );
			}
		}
		if ( resultCode == Results.RESTART_APP ) {
			restartApp();
		}
		super.onActivityResult( requestCode, resultCode, data );
	}
}
