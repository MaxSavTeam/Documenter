package com.maxsavitsky.documenter.backup;

import android.util.Log;

import androidx.annotation.Nullable;

import com.maxsavitsky.documenter.App;
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
		File dataDir = new File( App.appDataPath );
		if(!dataDir.exists())
			dataDir.mkdirs();
		File[] files = dataDir.listFiles();
		if(files != null) {
			for (File child : files) {
				if ( child.isDirectory() ) {
					Utils.deleteDirectory( child );
				} else {
					child.delete();
				}
			}
		}

		File dest = dataDir.getParentFile();
		ZipInputStream zis = new ZipInputStream( new FileInputStream( backupFile ) );
		byte[] buffer = new byte[ 1024 ];
		ZipEntry zipEntry;
		while ( ( zipEntry = zis.getNextEntry() ) != null ) {
			File newFile = new File( dest, zipEntry.getName() );
			if ( zipEntry.isDirectory() ) {
				if(!newFile.exists())
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
		File dir = new File( App.appDataPath );
		path.createNewFile();
		ZipOutputStream zipOutputStream = new ZipOutputStream( new FileOutputStream( path ) );
		zipOutputStream.setLevel( 9 );

		myPack( dir, dir.getName(), zipOutputStream );
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
