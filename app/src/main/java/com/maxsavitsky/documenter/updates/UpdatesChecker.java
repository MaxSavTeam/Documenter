package com.maxsavitsky.documenter.updates;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import com.maxsavitsky.documenter.BuildConfig;
import com.maxsavitsky.documenter.R;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class UpdatesChecker {

	private final Context mContext;
	private final Thread mThread;
	private final CheckResults mCheckResults;

	public interface CheckResults {
		void noUpdates(VersionInfo versionInfo);

		void updateAvailable(VersionInfo versionInfo);

		void downloaded(File path, VersionInfo versionInfo);

		void onDownloadProgress(int bytesCount, int totalBytesCount);

		void onNecessaryUpdate(VersionInfo versionInfo);

		void exceptionOccurred(Exception e);
	}

	public UpdatesChecker(Context context, @NonNull CheckResults checkResults) {
		mContext = context;
		mThread = Thread.currentThread();
		mCheckResults = checkResults;
	}

	private void check(String result) {
		VersionInfo info = null;
		try {
			info = VersionInfo.parseInfoFromJson( result );
		} catch (JSONException e) {
			e.printStackTrace();
			mCheckResults.exceptionOccurred( e );
			return;
		}
		if ( info.getVersionCode() > BuildConfig.VERSION_CODE && info.getMinSdk() <= Build.VERSION.SDK_INT ) {
			if ( info.isNecessaryUpdate() ) {
				mCheckResults.onNecessaryUpdate( info );
			} else {
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences( mContext.getApplicationContext() );
				if ( sp.getInt( "ignore_update", 0 ) != info.getBuildCode() ) {
					mCheckResults.updateAvailable( info );
				}
			}
		} else {
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
			mCheckResults.exceptionOccurred( e );
		}
	}
}