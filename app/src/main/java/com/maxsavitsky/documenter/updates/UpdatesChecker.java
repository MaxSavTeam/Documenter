package com.maxsavitsky.documenter.updates;

import android.util.Log;

import androidx.annotation.NonNull;

import com.maxsavitsky.documenter.App;
import com.maxsavitsky.documenter.BuildConfig;
import com.maxsavitsky.documenter.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class UpdatesChecker {

	public static final String TAG = App.TAG + " UpdatesChecker";
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
			JSONObject jsonObject = new JSONObject( result );
			if ( jsonObject.getBoolean( "updateExists" ) ) {
				VersionInfo versionInfo = new VersionInfo();
				versionInfo.setVersionName( jsonObject.getString( "versionName" ) );
				versionInfo.setVersionCode( jsonObject.getInt( "versionCode" ) );
				versionInfo.setDownloadUrl( jsonObject.getString( "downloadLink" ) );
				versionInfo.setUpdateSize( jsonObject.getInt( "updateSize" ) );
				versionInfo.setRevision( jsonObject.getInt( "revision" ) );
				Log.i( TAG, "check: " + versionInfo.getVersionCode() );
				mCheckResults.onUpdateAvailable( versionInfo );
			} else {
				mCheckResults.noUpdates();
			}
		} catch (JSONException e) {
			e.printStackTrace();
			Log.i( TAG, "check: " + e );
			mCheckResults.onException( e );
		}
	}

	public void runCheck() {
		try {
			URL url = new URL( "https://updates.maxsavteam.com/checkForUpdates?appPackage=" + BuildConfig.APPLICATION_ID +
					"&code=" + BuildConfig.VERSION_CODE +
					"&revision=" + Utils.getDefaultSharedPreferences().getInt( "updates_channel", 0 )
			);
			InputStream inputStream = url.openStream();
			byte[] buffer = new byte[1024];
			int len;
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			while(!mThread.isInterrupted() && (len = inputStream.read(buffer)) != -1){
				outputStream.write( buffer, 0, len );
			}
			inputStream.close();
			if ( !mThread.isInterrupted() ) {
				check( outputStream.toString() );
			}
		}catch (IOException e){
			mCheckResults.onException( e );
		}
	}
}