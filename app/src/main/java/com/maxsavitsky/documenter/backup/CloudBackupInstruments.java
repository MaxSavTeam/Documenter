package com.maxsavitsky.documenter.backup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.koushikdutta.ion.Ion;
import com.maxsavitsky.documenter.App;
import com.maxsavitsky.documenter.net.RequestMaker;
import com.maxsavitsky.documenter.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class CloudBackupInstruments {

	private static final String TAG = App.TAG + " CloudBInstruments";

	public static void createBackup(BackupInstruments.BackupCallback backupCallback, boolean isManually, String description) {
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if ( user == null ) {
			return;
		}

		user.getIdToken( true )
				.addOnCompleteListener( task->{
					if ( task.isSuccessful() ) {
						try {
							createBackup( backupCallback, isManually, description, task.getResult().getToken() );
						} catch (IOException e) {
							e.printStackTrace();
							backupCallback.onException( e );
						}
					} else {
						backupCallback.onException( task.getException() );
					}
				} );
	}

	private static void createBackup(BackupInstruments.BackupCallback backupCallback, boolean isManually, String description, String authToken) throws IOException {
		File file = new File( App.appStoragePath, "backups" );
		if ( !file.exists() ) {
			file.mkdirs();
		}
		file = new File( file, "cloud_backup.zip" );
		if ( !file.exists() ) {
			file.createNewFile();
		}
		File finalFile = file;
		BackupInstruments.createBackupToFile( file, new BackupInstruments.BackupCallback() {
			@Override
			public void onBackupStateChanged(BackupInstruments.BackupState state) {
				backupCallback.onBackupStateChanged( state );
			}

			@Override
			public void onProgress(int percent) {
				backupCallback.onProgress( percent );
			}

			@Override
			public void onSuccess(long timeOfCreation) {
				String url = Utils.DOCUMENTER_API + "backups/uploadBackup";
				backupCallback.onBackupStateChanged( BackupInstruments.BackupState.UPLOADING );
				Ion
						.with( App.getInstance().getApplicationContext() )
						.load( url )
						.setMultipartFile( "file", finalFile )
						.setMultipartParameter( "authToken", authToken )
						.setMultipartParameter( "desc", description )
						.setMultipartParameter( "isManually", String.valueOf( isManually ) )
						.asString()
						.setCallback( (e, result)->{
							if ( e == null ) {
								try {
									JSONObject jsonObject = new JSONObject( result );
									if ( jsonObject.getString( "status" ).equals( "OK" ) ) {
										backupCallback.onSuccess( jsonObject.getLong( "time" ) );
									} else {
										backupCallback.onException( new RuntimeException( jsonObject.getString( "error_description" ) ) );
									}
								} catch (JSONException jsonException) {
									jsonException.printStackTrace();
									backupCallback.onException( jsonException );
								}
							} else {
								backupCallback.onException( e );
							}
						} );
			}

			@Override
			public void onException(Exception e) {
				backupCallback.onException( e );
			}
		} );
	}

	public static void restoreFromBackup(BackupInstruments.BackupCallback backupCallback, long time) {
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if ( user == null ) {
			backupCallback.onException( new RuntimeException( "User is not authenticated" ) );
			return;
		}
		user.getIdToken( true )
				.addOnCompleteListener( task->{
					if ( task.isSuccessful() ) {
						new Thread( ()->{
							try {
								restoreFromBackup( backupCallback, time, task.getResult().getToken() );
							} catch (IOException e) {
								e.printStackTrace();
								backupCallback.onException( e );
							}
						} ).start();
					} else {
						backupCallback.onException( task.getException() );
					}
				} );
	}

	private static void restoreFromBackup(BackupInstruments.BackupCallback backupCallback, long time, String authToken) throws IOException {
		String url = Utils.DOCUMENTER_API + "backups/getBackupAtTime?authToken=" + authToken + "&backupTime=" + time;
		HttpURLConnection connection = RequestMaker.resolveHttpProtocol( url );
		connection.setRequestMethod( "GET" );
		connection.connect();
		File file = new File( App.appStoragePath, "updates" );
		if ( !file.exists() ) {
			file.mkdir();
		}
		file = new File( file, "downloaded_backup" );
		if ( !file.exists() ) {
			file.createNewFile();
		}
		try (InputStream inputStream = connection.getInputStream();
			 FileOutputStream fos = new FileOutputStream( file )) {
			long totalSize = connection.getContentLengthLong();
			int downloadedSize = 0;
			int len;
			byte[] buffer = new byte[ 1024 ];
			while ( ( len = inputStream.read( buffer ) ) != -1 ) {
				fos.write( buffer, 0, len );
				downloadedSize += len;
				backupCallback.onProgress( (int) ( downloadedSize * 100L / totalSize ) );
			}
		}
		connection.disconnect();
		backupCallback.onBackupStateChanged( BackupInstruments.BackupState.UNPACKING );
		BackupInstruments.restoreFromBackup( file, backupCallback );
	}

}
