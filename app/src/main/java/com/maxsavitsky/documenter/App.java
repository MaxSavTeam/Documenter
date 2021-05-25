package com.maxsavitsky.documenter;

import android.app.Application;
import android.content.Context;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.maxsavitsky.exceptionhandler.ExceptionHandler;

public class App extends Application {

	public static App instance;

	@Override
	public Context getApplicationContext() {
		return super.getApplicationContext();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;

		ExceptionHandler handler = new ExceptionHandler( getApplicationContext(), AfterExceptionActivity.class );
		Thread.setDefaultUncaughtExceptionHandler( (t, tr)->{
			FirebaseCrashlytics.getInstance().recordException( tr );
			FirebaseCrashlytics.getInstance().sendUnsentReports();
			handler.uncaughtException( t, tr );
		} );
	}

	public static App getInstance() {
		return instance;
	}
}