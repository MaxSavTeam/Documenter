package com.maxsavitsky.documenter.backup;

import android.net.Uri;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.maxsavitsky.documenter.App;

import java.io.File;
import java.io.IOException;

public class CloudBackupInstruments {

	public static void createBackup(final BackupInterface cloudInterface, String backupName, final long loadTime) throws IOException {
		final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if ( user == null ) {
			return;
		}
		File file = new File( App.appStoragePath, "backups" );
		if( !file.exists() )
			file.mkdirs();
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

	public static void restoreFromBackup(final BackupInterface cloudInterface, String backupName) throws IOException {
		final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if ( user == null ) {
			return;
		}
		StorageReference ref = FirebaseStorage.getInstance().getReference().child( user.getUid() + "/documenter/backups/" + backupName + ".zip" );
		File file = new File( App.appStoragePath, "backups" );
		if( !file.exists() )
			file.mkdirs();
		file = new File( file, "cloud_backup.zip" );
		if ( !file.exists() ) {
			file.createNewFile();
		}

		File finalFile = file;
		ref.getFile( file )
				.addOnSuccessListener( taskSnapshot->{
					try {
						BackupInstruments.restoreFromBackup( finalFile, null );
						cloudInterface.onSuccess( 0 );
					} catch (IOException e) {
						e.printStackTrace();
						cloudInterface.onException( e );
					}
				} )
				.addOnFailureListener( cloudInterface::onException );
	}
}
