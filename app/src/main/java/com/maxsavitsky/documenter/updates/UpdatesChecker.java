package com.maxsavitsky.documenter.updates;

import androidx.annotation.NonNull;

import com.maxsavitsky.documenter.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class UpdatesChecker {

	private final Thread mThread;
	private final CheckResults mCheckResults;

	public interface CheckResults {
		void noUpdates();

		void onUpdateAvailable(VersionInfo versionInfo);

		void onDownloaded(File path);

		void onDownloadProgress(int bytesCount, int totalBytesCount);

		void onException(Exception e);
	}

	public UpdatesChecker(@NonNull CheckResults checkResults) {
		mThread = Thread.currentThread();
		mCheckResults = checkResults;
	}

	private void check(String result) {
		try {
			JSONObject jsonObject = new JSONObject(result);
			if(jsonObject.getString( "status" ).equals( "OK" )){
				if(jsonObject.getBoolean( "result" )){
					VersionInfo versionInfo = new VersionInfo();
					versionInfo.setVersionName( jsonObject.getString( "version" ) );
					versionInfo.setVersionCode( jsonObject.getInt( "versionCode" ) );
					versionInfo.setDownloadUrl( jsonObject.getString( "downloadLink" ) );
					versionInfo.setUpdateSize( jsonObject.getInt( "updateSize" ) );
					versionInfo.setImportant( jsonObject.getBoolean( "important" ) );
					mCheckResults.onUpdateAvailable( versionInfo );
				}else{
					mCheckResults.noUpdates();
				}
			}else{
				mCheckResults.noUpdates();
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void runCheck() {
		URL url;
		InputStream inputStream = null;
		try {
			String data = "";
			url = new URL( "https://maxsavteam.000webhostapp.com/checkForUpdates.php?appPackage=" + BuildConfig.APPLICATION_ID
					+ "&versionCode=" + BuildConfig.VERSION_CODE );
			inputStream = url.openConnection().getInputStream();
			int b = inputStream.read();
			while ( b != -1 && !mThread.isInterrupted() ) {
				data = String.format( "%s%c", data, (char) b );
				b = inputStream.read();
			}
			inputStream.close();
			if ( !mThread.isInterrupted() ) {
				check( data );
			}
		} catch (IOException e) {
			try {
				if ( inputStream != null ) {
					inputStream.close();
				}
			} catch (IOException ignored) {
			}
			mCheckResults.onException( e );
		}
	}
}