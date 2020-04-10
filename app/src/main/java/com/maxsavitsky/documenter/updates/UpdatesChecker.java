package com.maxsavitsky.documenter.updates;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.maxsavitsky.documenter.BuildConfig;
import com.maxsavitsky.documenter.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class UpdatesChecker {
	public static class VersionInfo{
		private int mVersionCode;
		private String mVersionName;
		private String mDownloadUrl;
		private int mUpdateSize;

		public VersionInfo(int versionCode, String versionName, String downloadUrl, int updateSize) {
			mVersionCode = versionCode;
			mVersionName = versionName;
			mDownloadUrl = downloadUrl;
			mUpdateSize = updateSize;
		}

		public static VersionInfo parseInfo(String data){
			String[] strings = data.split( ";" );
			int versionCode = Integer.parseInt( strings[0] );
			String downloadUrl = strings[1];
			String versionName = strings[2];
			int size = Integer.parseInt( strings[3] );

			return new VersionInfo( versionCode, versionName, downloadUrl, size );
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

		public int getUpdateSize() {
			return mUpdateSize;
		}

		public void setUpdateSize(int updateSize) {
			mUpdateSize = updateSize;
		}
	}

	private final Context mContext;
	private final Thread mThread;
	private final CheckResults mCheckResults;

	public interface CheckResults{
		void noUpdates(VersionInfo versionInfo);
		void updateAvailable(VersionInfo versionInfo);
		void downloaded(File path, VersionInfo versionInfo);
		void onDownloadProgress(int bytesCount, int totalBytesCount);
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
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext());
			if(sp.getInt( "ignore_update", 0 ) != info.getVersionCode() )
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