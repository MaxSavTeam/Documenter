package com.maxsavitsky.documenter.utils;

import android.content.Context;

import com.maxsavitsky.documenter.BuildConfig;
import com.maxsavitsky.documenter.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class UpdatesChecker {
	public static class VersionInfo{
		private int mVersionCode;
		private String mVersionName;
		private String mDownloadUrl;

		public VersionInfo(int versionCode, String versionName, String downloadUrl) {
			mVersionCode = versionCode;
			mVersionName = versionName;
			mDownloadUrl = downloadUrl;
		}

		public static VersionInfo parseInfo(String data){
			int i = 0;
			int versionCode = 0;
			while(data.charAt( i ) != ';'){
				versionCode *= 10;
				versionCode += Integer.parseInt( String.valueOf( data.charAt( i ) ) );
				i++;
			}
			i++;
			String downloadUrl = "";
			while(data.charAt( i ) != ';'){
				downloadUrl = String.format( "%s%c", downloadUrl, data.charAt( i ) );
				i++;
			}
			i++;
			String versionName = "";
			while(data.charAt( i ) != ';'){
				versionName = String.format( "%s%c", versionCode, data.charAt( i ) );
				i++;
			}

			return new VersionInfo( versionCode, versionName, downloadUrl );
		}

		public int getVersionCode() {
			return mVersionCode;
		}

		public void setVersionCode(int versionCode) {
			mVersionCode = versionCode;
		}

		public String getVersionName() {
			return mVersionName;
		}

		public void setVersionName(String versionName) {
			mVersionName = versionName;
		}

		public String getDownloadUrl() {
			return mDownloadUrl;
		}

		public void setDownloadUrl(String downloadUrl) {
			mDownloadUrl = downloadUrl;
		}
	}

	private Context mContext;
	private Thread mThread;
	private CheckResults mCheckResults;

	public interface CheckResults{
		void noUpdates(VersionInfo versionInfo);
		void updateAvailable(VersionInfo versionInfo);
		void downloaded(File path, VersionInfo versionInfo);
		void exceptionOccurred(IOException e);
	}

	public UpdatesChecker(Context context, CheckResults checkResults) {
		mContext = context;
		mThread = Thread.currentThread();
		mCheckResults = checkResults;
	}

	private void check(String result){
		VersionInfo info = VersionInfo.parseInfo( result );
		if(info.getVersionCode() > BuildConfig.VERSION_CODE ){
			mCheckResults.updateAvailable( info );
		}else{
			mCheckResults.noUpdates( info );
		}
	}

	public void runCheck() {
		URL url;
		InputStream inputStream = null;
		try {
			String data = "";
			url = new URL( mContext.getResources().getString( R.string.resources_url ) + "/apk/documenter/version" );
			inputStream = url.openConnection().getInputStream();
			int b = inputStream.read();
			while ( b != -1 && !mThread.isInterrupted() ) {
				data = String.format( "%s%c", data, (char) b );
				b = inputStream.read();
			}
			inputStream.close();
			check( data );
		}catch (IOException e){
			try {
				if ( inputStream != null )
					inputStream.close();
			}catch (IOException ignored){}
			mCheckResults.exceptionOccurred( e );
		}
	}
}