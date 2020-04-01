package com.maxsavitsky.documenter.backup;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.maxsavitsky.documenter.utils.Utils;

import java.io.File;
import java.io.IOException;

public class CloudBackupInstruments {

	public interface CloudInterface {
		void successfully(long timeOfCreation);

		void failed();

		void exceptionOccurred(Exception e);
	}

	public static void createBackup(final CloudInterface cloudInterface) throws IOException {
		final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if ( user == null ) {
			return;
		}
		final File file = new File( Utils.getExternalStoragePath().getPath() + "/cloud_backup.zip" );
		if ( !file.exists() ) {
			file.createNewFile();
		}

		BackupInstruments.createBackupToFile( file );

		StorageReference storageRef = FirebaseStorage.getInstance().getReference();

		StorageReference backupRef = storageRef.child( user.getUid() + "/documenter/backups/latest_backup.zip" );
		backupRef.putFile( Uri.fromFile( file ) )
				.addOnSuccessListener( new OnSuccessListener<UploadTask.TaskSnapshot>() {
					@Override
					public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
						DatabaseReference ref = FirebaseDatabase.getInstance()
								.getReference( "documenter/" + user.getUid() + "/last_backup_time" );
						final long time = System.currentTimeMillis();
						ref.setValue( time )
								.addOnSuccessListener( new OnSuccessListener<Void>() {
									@Override
									public void onSuccess(Void aVoid) {
										cloudInterface.successfully( time );
									}
								} )
								.addOnFailureListener( new OnFailureListener() {
									@Override
									public void onFailure(@NonNull Exception e) {
										cloudInterface.exceptionOccurred( e );
									}
								} );
						file.delete();
					}
				} )
				.addOnFailureListener( new OnFailureListener() {
					@Override
					public void onFailure(@NonNull Exception e) {
						e.printStackTrace();
						cloudInterface.exceptionOccurred( e );
					}
				} );
	}

	public static void restoreFromBackup(final CloudInterface cloudInterface) throws IOException {
		final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if ( user == null ) {
			return;
		}
		StorageReference ref = FirebaseStorage.getInstance().getReference().child( user.getUid() + "/documenter/backups/latest_backup.zip" );
		final File file = new File( Utils.getExternalStoragePath().getPath() + "/cloud_backup.zip" );
		if ( !file.exists() ) {
			file.createNewFile();
		}

		ref.getFile( file )
				.addOnSuccessListener( new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
					@Override
					public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
						try {
							BackupInstruments.restoreFromBackup( file );
							cloudInterface.successfully( 0 );
						} catch (IOException e) {
							e.printStackTrace();
							cloudInterface.exceptionOccurred( e );
						}
					}
				} )
				.addOnFailureListener( new OnFailureListener() {
					@Override
					public void onFailure(@NonNull Exception e) {
						cloudInterface.exceptionOccurred( e );
					}
				} );
	}
}
