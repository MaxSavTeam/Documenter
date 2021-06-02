package com.maxsavitsky.documenter.backup;

import android.util.Log;

import androidx.annotation.Nullable;

import com.maxsavitsky.documenter.MainActivity;
import com.maxsavitsky.documenter.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupInstruments {
	private static final String THIS_TAG = MainActivity.TAG + " BInstruments";

	public static void restoreFromBackup(File backupFile, @Nullable BackupInterface backupInterface) throws IOException {
		File[] files = Utils.getExternalStoragePath().listFiles();
		for (File child : files) {
			if(child.getName().endsWith( ".zip" ))
				continue;
			if ( child.isDirectory() ) {
				Utils.deleteDirectory( child );
			} else {
				child.delete();
			}
		}

		File destinationDir = Utils.getExternalStoragePath();
		ZipInputStream zis = new ZipInputStream( new FileInputStream( backupFile ) );
		byte[] buffer = new byte[ 1024 ];
		ZipEntry zipEntry;
		while ( ( zipEntry = zis.getNextEntry() ) != null ) {
			File newFile = new File( destinationDir.getCanonicalPath(), zipEntry.getName() );
			if ( zipEntry.isDirectory() ) {
				newFile.mkdir();
				continue;
			}
			FileOutputStream fos = new FileOutputStream( newFile );

			int len;
			while ( ( len = zis.read( buffer ) ) > 0 ) {
				fos.write( buffer, 0, len );
			}
			fos.close();
		}
		zis.closeEntry();
		zis.close();
		if(backupInterface != null)
			backupInterface.onSuccess( System.currentTimeMillis() );
	}

	public static void createBackupToFile(File path, @Nullable BackupInterface backupInterface) throws IOException {
		long startTime = System.currentTimeMillis();
		Utils.removeAllUnusedImages();
		File dir = Utils.getExternalStoragePath();
		path.createNewFile();
		ZipOutputStream zipOutputStream = new ZipOutputStream( new FileOutputStream( path ) );
		zipOutputStream.setLevel( 9 );

		if ( dir == null || dir.listFiles() == null ) {
			zipOutputStream.close();
			return;
		}
		for (File file : dir.listFiles()) {
			if(!file.getName().equals( "cloud_backup.zip" ) && !file.getPath().equals( path.getPath() ))
				myPack( file, file.getName(), zipOutputStream );
		}
		zipOutputStream.close();

		long end = System.currentTimeMillis();
		Log.i( THIS_TAG, "packed in " + ((end - startTime) / 1000) + "s" );
		if(backupInterface != null)
			backupInterface.onSuccess( System.currentTimeMillis() );
	}

	private static void myPack(File path, String fileName, ZipOutputStream out) throws IOException {
		if ( path.isHidden() ) {
			return;
		}
		Log.i( THIS_TAG, "packing " + (path.getPath().replace( Utils.getExternalStoragePath().getPath(), "" )) + " size: " + (path.length() / 1024) + "KB" );
		if ( path.isDirectory() ) {
			if ( fileName.endsWith( "/" ) ) {
				out.putNextEntry( new ZipEntry( fileName ) );
			} else {
				out.putNextEntry( new ZipEntry( fileName + "/" ) );
			}
			out.closeEntry();

			File[] children = path.listFiles();
			for (File child : children) {
				myPack( child, fileName + "/" + child.getName(), out );
			}
			return;
		}

		byte[] content = Utils.readFileByBytes( path );
		ZipEntry zipEntry = new ZipEntry( fileName );
		out.putNextEntry( zipEntry );
		out.write( content, 0, content.length );
	}
}
