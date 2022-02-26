package com.maxsavitsky.documenter;

import android.app.Application;
import android.content.Context;

import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.exceptionhandler.ExceptionHandler;

import java.lang.Thread.UncaughtExceptionHandler;

public class App extends Application {

	public static final String TAG = "Documenter";

	public static final String NOTIFICATION_CHANNEL_ID = "documenter_app_channel";

	public static String appDataPath, appStoragePath;

	public static App instance;

	@Override
	public Context getApplicationContext() {
		return super.getApplicationContext();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;

		Utils.setContext(getApplicationContext());

		appStoragePath = getExternalFilesDir( null ).getPath();
		appDataPath = appStoragePath + "/data";

		UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
		ExceptionHandler handler = new ExceptionHandler( getApplicationContext(), AfterExceptionActivity.class );
		Thread.setDefaultUncaughtExceptionHandler( (t, tr)->{
			handler.uncaughtException( t, tr );
			if(previousHandler != null)
				previousHandler.uncaughtException( t, tr );
		} );
	}

	public static App getInstance() {
		return instance;
	}
}
