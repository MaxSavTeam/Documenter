package com.maxsavitsky.documenter.utils;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class UpdatesDownloader {
	private UpdatesChecker.VersionInfo mVersionInfo;
	private UpdatesChecker.CheckResults mCheckResults;

	public UpdatesDownloader(UpdatesChecker.VersionInfo versionInfo, UpdatesChecker.CheckResults checkResults) {
		mVersionInfo = versionInfo;
		mCheckResults = checkResults;
	}

	public void download()  {
		File file = new File( Environment.getExternalStorageDirectory().getAbsolutePath() + "/.documenter" );
		String dUrl = mVersionInfo.getDownloadUrl();
		int s = dUrl.length();
		while ( dUrl.charAt( s - 1 ) != '/' ) {
			s--;
		}
		final String name = dUrl.substring( s );
		if(!file.exists())
			file.mkdir();
		file = new File( file.getPath() + "/" + name );

		URL url;
		InputStream in = null;
		FileOutputStream os = null;
		try {
			if(!file.exists())
				file.createNewFile();
			url = new URL( dUrl );
			in = url.openConnection().getInputStream();
			os = new FileOutputStream( file );
			byte[] buffer = new byte[ 1024 ];
			int count;
			while ( ( count = in.read( buffer, 0, 1024 ) ) != -1 ) {
				os.write( buffer, 0, count );
				if(Thread.currentThread().isInterrupted()){
					in.close();
					os.close();
					return;
				}
			}
			in.close();
			os.close();

			mCheckResults.downloaded( file, mVersionInfo );

		}catch (IOException e){
			try{
				if(in != null)
					in.close();
			}catch (IOException ignored){}

			try{
				if(os != null)
					os.close();
			}catch (IOException ignored){}

			mCheckResults.exceptionOccurred( e );
		}
	}
}
