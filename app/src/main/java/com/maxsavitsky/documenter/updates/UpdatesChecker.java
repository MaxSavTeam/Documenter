package com.maxsavitsky.documenter.updates;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import com.maxsavitsky.documenter.BuildConfig;
import com.maxsavitsky.documenter.R;

import org.json.JSONException;
import org.json.JSONObject;

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
		private int mBuildCode;

		public VersionInfo(int versionCode, String versionName, String downloadUrl, int updateSize, int buildCode) {
			mVersionCode = versionCode;
			mVersionName = versionName;
			mDownloadUrl = downloadUrl;
			mUpdateSize = updateSize;
			mBuildCode = buildCode;
		}

		public static VersionInfo parseInfo(String data){
			String[] strings = data.split( ";" );
			int versionCode = Integer.parseInt( strings[0] );
			String downloadUrl = strings[1];
			String versionName = strings[2];
			int size = Integer.parseInt( strings[3] );
			int buildCode = Integer.parseInt( strings[4] );

			return new VersionInfo( versionCode, versionName, downloadUrl, size, buildCode );
		}

		public static VersionInfo parseInfoFromJson(String json) throws JSONException {
			JSONObject jsonObject = new JSONObject( json );
			int versionCode = jsonObject.getInt( "versionCode" );
			int size = jsonObject.getInt( "apkSize" );
			int buildCode = jsonObject.getInt( "buildCode" );
			String downloadUrl = jsonObject.getString( "downloadUrl" );
			String versionName = jsonObject.getString( "versionName" );

			return new VersionInfo( versionCode, versionName, downloadUrl, size, buildCode );
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

		public int getBuildCode() {
			return mBuildCode;
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
		void exceptionOccurred(Exception e);
	}

	public UpdatesChecker(Context context, @NonNull CheckResults checkResults) {
		mContext = context;
		mThread = Thread.currentThread();
		mCheckResults = checkResults;
	}

	private void check(String result){
		VersionInfo info = null;
		try {
			info = VersionInfo.parseInfoFromJson( result );
		} catch (JSONException e) {
			e.printStackTrace();
			mCheckResults.exceptionOccurred( e );
			return;
		}
		if(info.getVersionCode() > BuildConfig.VERSION_CODE ){
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext());
			if(sp.getInt( "ignore_update", 0 ) != info.getBuildCode() )
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
			url = new URL( mContext.getResources().getString( R.string.resources_url ) + "/apk/documenter/version.json" );
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