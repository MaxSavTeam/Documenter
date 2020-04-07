package com.maxsavitsky.documenter;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;

import com.maxsavitsky.documenter.utils.Utils;
import com.rollbar.android.Rollbar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyExceptionHandler implements Thread.UncaughtExceptionHandler {
	private final Activity mActivity;
	private File mStackTraceFile;

	public MyExceptionHandler(Activity activity) {
		mActivity = activity;
	}

	@Override
	public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
		Rollbar.instance().critical( e );
		prepareStacktrace( t, e, false );

		Intent intent = new Intent( mActivity, MainActivity.class );
		intent.putExtra( "crash", true ).putExtra( "path", mStackTraceFile.getPath() );
		intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TASK
				| Intent.FLAG_ACTIVITY_NEW_TASK );

		PendingIntent pendingIntent = PendingIntent.getActivity( MyApplication.getInstance().getBaseContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT );
		AlarmManager mgr = (AlarmManager) MyApplication.getInstance().getBaseContext().getSystemService( Context.ALARM_SERVICE );
		mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent);

		mActivity.finish();
		System.exit( 2 );
	}

	public void justWriteException(Thread t, Throwable tr){
		prepareStacktrace( t, tr, true );
	}

	private void prepareStacktrace(Thread t, Throwable e, boolean calledManually) {
		e.printStackTrace();
		PackageInfo mPackageInfo = null;
		try {
			mPackageInfo = MyApplication.getInstance().getBaseContext().getPackageManager().getPackageInfo( MyApplication.getInstance().getBaseContext().getPackageName(), 0 );
		} catch (PackageManager.NameNotFoundException ex) {
			ex.printStackTrace();
		}

		File file = new File( Utils.getExternalStoragePath().getPath() + "/stacktraces/" );
		if ( !file.exists() ) {
			file.mkdir();
		}
		Date date = new Date( System.currentTimeMillis() );
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "dd.MM.yyyy HH:mm:ss", Locale.ROOT );
		SimpleDateFormat fileFormatter = new SimpleDateFormat( "dd-MM-yyyy_HH:mm:ss", Locale.ROOT );
		String formattedDate = fileFormatter.format( date );
		file = new File( file.getPath() + "/stacktrace-" + formattedDate + (calledManually ? "-m" : "") + ".txt" );

		try {
			file.createNewFile();
		} catch (IOException ignored) {
		}
		StringBuilder report = new StringBuilder();
		if(calledManually)
			report.append( "CALLED MANUALLY\n" );
		report.append( "Time: " ).append( simpleDateFormat.format( date ) ).append( "\n" )
				.append( "Thread name: " ).append( t.getName() ).append( "\n" )
				.append( "Thread id: " ).append( t.getId() ).append( "\n" )
				.append( "Thread state: " ).append( t.getState() ).append( "\n" )
				.append( "Package: " ).append( BuildConfig.APPLICATION_ID ).append( "\n" )
				.append( "Manufacturer: " ).append( Build.MANUFACTURER ).append( "\n" )
				.append( "Model: " ).append( Build.MODEL ).append( "\n" )
				.append( "Brand: " ).append( Build.BRAND ).append( "\n" )
				.append( "Android Version: " ).append( Build.VERSION.RELEASE ).append( "\n" )
				.append( "Android SDK: " ).append( Build.VERSION.SDK_INT ).append( "\n" )
				.append( "Version name: " ).append( mPackageInfo.versionName ).append( "\n" )
				.append( "Version code: " ).append( mPackageInfo.versionCode ).append( "\n" );
		printStackTrace( e, report );
		report.append( "Caused by:\n" );
		Throwable cause = e.getCause();
		if ( cause != null ) {
			for (StackTraceElement element : cause.getStackTrace()) {
				report.append( "\tat " ).append( element.toString() ).append( "\n" );
			}
		} else {
			report.append( "\tN/A\n" );
		}
		try {
			FileWriter fr = new FileWriter( file, false );
			fr.write( report.toString() );
			fr.flush();
			fr.close();
		} catch (IOException ignored) {
		}
		mStackTraceFile = file;
	}

	private void printStackTrace(Throwable t, StringBuilder builder) {
		if ( t == null ) {
			return;
		}
		StackTraceElement[] stackTraceElements = t.getStackTrace();
		builder
				.append( "Exception: " ).append( t.getClass().getName() ).append( "\n" )
				.append( "Message: " ).append( t.getMessage() ).append( "\n" )
				.append( "Stacktrace:\n" );
		for (StackTraceElement stackTraceElement : stackTraceElements) {
			builder.append( "\t" ).append( stackTraceElement.toString() ).append( "\n" );
		}
	}
}
