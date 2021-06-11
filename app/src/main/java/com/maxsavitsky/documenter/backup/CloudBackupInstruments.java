package com.maxsavitsky.documenter.backup;

import android.net.Uri;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.koushikdutta.ion.Ion;
import com.maxsavitsky.documenter.App;
import com.maxsavitsky.documenter.net.RequestMaker;
import com.maxsavitsky.documenter.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class CloudBackupInstruments {

	public static void createBackup(BackupInstruments.BackupCallback backupCallback, String description) {
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if ( user == null ) {
			return;
		}

		user.getIdToken( true )
				.addOnCompleteListener( task->{
					if ( task.isSuccessful() ) {
						new Thread( ()->{
							try {
								createBackup( backupCallback, description, task.getResult().getToken() );
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

	private static void createBackup(BackupInstruments.BackupCallback backupCallback, String description, String authToken) throws IOException {
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
			public void onSuccess(long timeOfCreation) {
				String url = Utils.DOCUMENTER_API + "backups/uploadBackup";
				Ion
						.with( App.getInstance().getApplicationContext() )
						.load( url )
						.setMultipartFile( "file", finalFile )
						.setMultipartParameter( "authToken", authToken )
						.setMultipartParameter( "desc", description )
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

	public static void createBackup(final BackupInstruments.BackupCallback cloudInterface, String backupName, final long loadTime) throws IOException {
		final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if ( user == null ) {
			return;
		}
		File file = new File( App.appStoragePath, "backups" );
		if ( !file.exists() ) {
			file.mkdirs();
		}
		file = new File( file, "cloud_backup.zip" );
		if ( !file.exists() ) {
			file.createNewFile();
		}

		BackupInstruments.createBackupToFile( file, null );

		StorageReference storageRef = FirebaseStorage.getInstance().getReference();

		StorageReference backupRef = storageRef.child( user.getUid() + "/documenter/backups/" + backupName + ".zip" );
		File finalFile = file;
		backupRef.putFile( Uri.fromFile( file ) )
				.addOnSuccessListener( taskSnapshot->{
					DatabaseReference ref = FirebaseDatabase.getInstance()
							.getReference( "documenter/" + user.getUid() + "/last_backup_time" );
					ref.setValue( loadTime )
							.addOnSuccessListener( aVoid->cloudInterface.onSuccess( loadTime ) )
							.addOnFailureListener( cloudInterface::onException );
					finalFile.delete();
				} )
				.addOnFailureListener( e->{
					e.printStackTrace();
					cloudInterface.onException( e );
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
					if(task.isSuccessful()){
						new Thread(()->{
							try {
								restoreFromBackup( backupCallback, time, task.getResult().getToken() );
							} catch (IOException e) {
								e.printStackTrace();
								backupCallback.onException( e );
							}
						}).start();
					}else{
						backupCallback.onException( task.getException() );
					}
				} );
	}

	private static void restoreFromBackup(BackupInstruments.BackupCallback backupCallback, long time, String authToken) throws IOException {
		String url = Utils.DOCUMENTER_API + "backups/getBackupAtTime?authToken=" + authToken + "&backupTime=" + time;
		HttpURLConnection connection = RequestMaker.resolveHttpProtocol( url );
		connection.setRequestMethod( "GET" );
		connection.connect();
		InputStream inputStream = connection.getInputStream();
		BackupInstruments.restoreFromStream( inputStream, backupCallback );
		connection.disconnect();
	}

}
