package com.maxsavitsky.documenter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.utils.RequestCodes;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		Utils.setContext(this);

		Thread.setDefaultUncaughtExceptionHandler( new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
				MainActivity.this.uncaughtException( t, e );
			}
		} );

		try {
			initialize();
		}catch (Exception e){
			Utils.getErrorDialog( e, this ).show();
			return;
		}
		viewCategoryList( null );
	}

	private long getUsedMemory(){
		Runtime runtime =  Runtime.getRuntime();
		return runtime.totalMemory() - runtime.freeMemory();
	}

	public void getUsedMemory(View v){
		Toast.makeText( this, Long.toString( getUsedMemory() / (1024 * 1024) ) + " MB", Toast.LENGTH_SHORT ).show();
	}

	private void initialize() throws Exception {
		MainData.readAllCategories();
		MainData.readAllDocuments();
		MainData.readAllEntries();
	}

	public void reinitialize(View v){
		MainData.clearAll();

		try{
			initialize();

			Toast.makeText( this, "Successful\nUsed RAM: " + getUsedMemory() / (1024*1024) + "MB", Toast.LENGTH_SHORT ).show();
		}catch (Exception e){
			Utils.getErrorDialog( e, this ).show();
		}
	}

	public void clearRam(View v){
		MainData.clearAll();
		System.gc();
		int MB = 1024 * 1024;
		Toast.makeText( this, "Memory cleared\nRAM usage now: " + (getUsedMemory() / MB) + "MB", Toast.LENGTH_SHORT ).show();
	}

	public void viewCategoryList(View v){
		Intent intent = new Intent(this, CategoryList.class);
		startActivityForResult( intent, RequestCodes.CATEGORY_LIST );
	}

	public void makeError(View v){
		int x = 0;
		BigDecimal ans = BigDecimal.ONE.divide( BigDecimal.valueOf( 3 ) );
	}

	public void uncaughtException(@NonNull Thread t, @NonNull final Throwable e) {
		e.printStackTrace();
		PackageInfo mPackageInfo = null;
		try {
			mPackageInfo = getPackageManager().getPackageInfo( getPackageName(), 0 );
		} catch (PackageManager.NameNotFoundException ex) {
			ex.printStackTrace();
		}

		File file = new File( Utils.getExternalStoragePath().getPath() + "/stacktraces/" );
		if(!file.exists())
			file.mkdir();
		Date date = new Date( System.currentTimeMillis() );
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "dd.MM.yyyy HH:mm:ss", Locale.ROOT );
		SimpleDateFormat fileFormatter = new SimpleDateFormat( "dd-MM-yyyy_HH:mm:ss", Locale.ROOT );
		String formattedDate = fileFormatter.format( date );
		file = new File( file.getPath() + "/stacktrace-" + formattedDate + ".trace" );

		try {
			file.createNewFile();
		}catch (IOException ignored){
		}
		StringBuilder report = new StringBuilder(  );
		report.append( "Time: " ).append( simpleDateFormat.format( date ) ).append( "\n" )
				.append( "Thread name: " ).append( t.getName() ).append( "\n" )
				.append( "Thread id: " ).append( t.getId() ).append( "\n" )
				.append( "Thread state: " ).append( t.getState() ).append( "\n" )
				.append( "Manufacturer: " ).append( Build.MANUFACTURER ).append( "\n" )
				.append( "Model: " ).append( Build.MODEL ).append( "\n" )
				.append( "Brand: " ).append( Build.BRAND ).append( "\n" )
				.append( "Android Version: " ).append( Build.VERSION.RELEASE ).append( "\n" )
				.append( "Android SDK: " ).append( Build.VERSION.SDK_INT ).append( "\n" )
				.append( "Version name: " ).append( mPackageInfo.versionName ).append( "\n" )
				.append( "Version code: " ).append( mPackageInfo.versionCode ).append( "\n" );
		printStackTrace( e, report );
		report.append( "Caused by:\n" );
		for(StackTraceElement element : e.getCause().getStackTrace()){
			report.append( "\tat " ).append( element.toString() ).append( "\n" );
		}
		try {
			FileWriter fr = new FileWriter( file, false );
			fr.write( report.toString() );
			fr.flush();;
			fr.close();
		}catch (Exception ignored){
		}
		AlarmManager alarmManager = (AlarmManager) getSystemService( Context.ALARM_SERVICE );
		Intent intent = new Intent( this, ErrorHandlerActivity.class )
				.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK )
				.putExtra( "path", file.getAbsolutePath() );
		PendingIntent pendingIntent = PendingIntent.getActivity( this, 0, intent, PendingIntent.FLAG_ONE_SHOT );
		alarmManager.set( AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent );
		finish();
	}

	private void printStackTrace(Throwable t, StringBuilder builder){
		if(t == null)
			return;
		StackTraceElement[] stackTraceElements = t.getStackTrace();
		builder
				.append( "Exception: " ).append( t.getClass().getName() ).append( "\n" )
				.append( "Message: " ).append( t.getMessage() ).append( "\n" )
				.append( "Stacktrace:\n" );
		for(StackTraceElement stackTraceElement : stackTraceElements){
			builder.append( "\t" ).append( stackTraceElement.toString() ).append( "\n" );
		}
	}

	private void restartApp() {
		Intent intent = new Intent( this, MainActivity.class );
		startActivity( intent );
		this.finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if(requestCode == RequestCodes.CATEGORY_LIST ){
			if(resultCode == ResultCodes.EXIT ){
				finishAndRemoveTask();
			}
			if(resultCode == ResultCodes.RESTART_ACTIVITY ){
				Intent intent = new Intent( this, CategoryList.class );
				startActivityForResult( intent, RequestCodes.CATEGORY_LIST );
			}
		}
		if ( resultCode == ResultCodes.RESTART_APP ) {
			restartApp();
		}
		super.onActivityResult( requestCode, resultCode, data );
	}
}
