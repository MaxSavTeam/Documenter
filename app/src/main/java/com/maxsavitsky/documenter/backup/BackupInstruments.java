package com.maxsavitsky.documenter.backup;

import android.os.Environment;
import android.widget.Toast;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupInstruments {

	public static void restoreFromBackup(File backupFile) throws IOException {
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
	}

	public static void createBackupToFile(File path) throws IOException {
		File dir = Utils.getExternalStoragePath();
		path.createNewFile();
		ZipOutputStream zipOutputStream = new ZipOutputStream( new FileOutputStream( path ) );
		zipOutputStream.setLevel( 9 );

		if ( dir == null || dir.listFiles() == null ) {
			zipOutputStream.close();
			return;
		}
		for (File file : dir.listFiles()) {
			if(!file.getName().equals( "cloud_backup.zip" ))
				myPack( file, file.getName(), zipOutputStream );
		}
		zipOutputStream.close();
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

		String content = Utils.readFile( path );
		ZipEntry zipEntry = new ZipEntry( fileName );
		out.putNextEntry( zipEntry );
		out.write( content.getBytes(), 0, content.getBytes().length );
	}
}
