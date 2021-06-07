package com.maxsavitsky.documenter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.maxsavitsky.documenter.backup.BackupInstruments;
import com.maxsavitsky.documenter.backup.CloudBackupMaker;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.data.DataReformatter;
import com.maxsavitsky.documenter.data.EntitiesStorage;
import com.maxsavitsky.documenter.data.types.Entity;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.data.types.Group;
import com.maxsavitsky.documenter.ui.EntitiesListActivity;
import com.maxsavitsky.documenter.updates.UpdatesChecker;
import com.maxsavitsky.documenter.updates.UpdatesDownloader;
import com.maxsavitsky.documenter.updates.VersionInfo;
import com.maxsavitsky.documenter.utils.ApkInstaller;
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

	private ProgressDialog mDownloadPd = null;
	private Thread downloadThread;

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
		Utils.setContext( this );
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

		deleteInstalledApks();

		if ( sharedPreferences.getBoolean( "check_updates", true ) ) {
			final UpdatesChecker checker = new UpdatesChecker( mCheckResults );
			new Thread( checker::runCheck ).start();
		} else {
			startInitialization();
		}
	}

	private void startInitialization() {
		new Thread( ()->{
			try {
				runReformatIfNeeded();
				initialize();
				Log.i( TAG, "Initialized" );
			} catch (final Exception e) {
				runOnUiThread( ()->Utils.getErrorDialog( e, MainActivity.this ).show() );
				return;
			}
			final CloudBackupMaker backupMaker = new CloudBackupMaker( this );
			new Thread( backupMaker::stateChanged, "AutoBackupMaker" ).start();
			try {
				Thread.sleep( 1000 );
				Intent intent = new Intent( this, EntitiesListActivity.class );
				mEntitiesListLauncher.launch( intent );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} ).start();
	}

	private final UpdatesChecker.CheckResults mCheckResults = new UpdatesChecker.CheckResults() {
		@Override
		public void noUpdates() {
			startInitialization();
		}

		@Override
		public void onUpdateAvailable(final VersionInfo versionInfo) {
			runOnUiThread( ()->{
				AlertDialog.Builder builder = new AlertDialog.Builder( MainActivity.this, MainActivity.super.mAlertDialogStyle );
				builder.setTitle( String.format( getString( R.string.update_available ), versionInfo.getVersionName() ) )
						.setCancelable( false )
						.setMessage( R.string.would_you_like_to_download_and_install )
						.setPositiveButton( R.string.yes, (dialog, which)->{
							download( versionInfo );
							dialog.cancel();
						} )
						.setNegativeButton( R.string.no, (dialog, which)->{
							dialog.cancel();
							startInitialization();
						} );
				builder.create().show();
			} );
		}

		@Override
		public void onDownloaded(File path) {
			if ( mDownloadPd != null ) {
				mDownloadPd.dismiss();
			}

			ApkInstaller.installApk( MainActivity.this, path );
		}

		@Override
		public void onException(Exception e) {
			noUpdates();
		}

		@Override
		public void onDownloadProgress(final int bytesCount, final int totalBytesCount) {
			runOnUiThread( ()->{
				mDownloadPd.setIndeterminate( false );
				mDownloadPd.setMax( 100 );
				mDownloadPd.setProgress( bytesCount * 100 / totalBytesCount );
			} );
		}
	};

	private void download(VersionInfo versionInfo) {
		final UpdatesDownloader downloader = new UpdatesDownloader( versionInfo, mCheckResults );
		mDownloadPd = new ProgressDialog( this );
		mDownloadPd.setMessage( getString( R.string.downloading ) );
		mDownloadPd.setCancelable( false );
		mDownloadPd.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL );
		mDownloadPd.setIndeterminate( true );
		mDownloadPd.setButton( ProgressDialog.BUTTON_NEGATIVE, getResources().getString( R.string.cancel ), (dialog, which)->{
			downloadThread.interrupt();
			runOnUiThread( dialog::cancel );
		} );
		mDownloadPd.show();
		downloadThread = new Thread( downloader::download );
		downloadThread.start();
	}

	private void runReformatIfNeeded() throws Exception {
		File file = new File( App.appDataPath );
		if(!file.exists())
			file.mkdirs();
		file = new File( App.appDataPath, "data.json" );
		if ( !file.exists() ) {
			if(new File( App.appStoragePath, "entries.xml" ).exists() &&
					new File( App.appStoragePath, "documents.xml" ).exists() &&
					new File( App.appStoragePath, "categories.xml" ).exists()) {
				File f = new File( App.appStoragePath, "backups" );
				if ( !f.exists() ) {
					f.mkdirs();
				}
				BackupInstruments.createBackupToFile( new File( f, "before_reformat_backup.zip" ), null );
				DataReformatter.runReformat();
			}
		}
	}

	private void initialize() throws Exception {
		File dataFile = new File( App.appDataPath, "data.json" );
		if(!dataFile.exists()){
			dataFile.createNewFile();
			EntitiesStorage.get().setGroups( new ArrayList<>(){{
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
		File file = new File( Environment.getExternalStorageDirectory().getPath() + "/.documenter" );
		if ( file.exists() ) {
			File[] files = file.listFiles();
			for (File subFile : files) {
				subFile.delete();
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
