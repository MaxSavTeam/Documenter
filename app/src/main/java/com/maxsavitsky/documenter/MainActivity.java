package com.maxsavitsky.documenter;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.text.Html;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.maxsavitsky.documenter.backup.AutonomousCloudBackupper;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.codes.Requests;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.utils.Utils;
import com.rollbar.android.Rollbar;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MainActivity extends ThemeActivity {
	private String path;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Rollbar.init(this);
		Thread.setDefaultUncaughtExceptionHandler( new MyExceptionHandler( this ) );
		try {
			Toolbar toolbar = findViewById( R.id.toolbar );
			setSupportActionBar( toolbar );
		}catch (Exception e){
			e.printStackTrace();
		}

		Utils.setContext(this);

		deleteInstalledApks();

		Intent startIntent = getIntent();
		if(startIntent.getBooleanExtra( "crash", false )){
			path = startIntent.getStringExtra( "path" );
			setContentView( R.layout.activity_errhandler );
			getSupportActionBar().setTitle( "Documenter bug report" );
			preparationAfterCrash();
		}else{
			try {
				initialize();
			}catch (Exception e){
				Utils.getErrorDialog( e, this ).show();
				return;
			}
			viewCategoryList( null );

			final AutonomousCloudBackupper backupper = new AutonomousCloudBackupper( this );
			new Thread( new Runnable() {
				@Override
				public void run() {
					backupper.stateChanged();
				}
			}, "AutoBackupper" ).start();
		}

	}

	private void preparationAfterCrash(){
		findViewById( R.id.btnErrSendReport ).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sendLog();
			}
		} );
		findViewById( R.id.btnErrViewReport ).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FileReader fr = null;
				String mes = "";
				try{
					fr  = new FileReader( new File(path) );
					while(fr.ready()){
						mes = String.format( "%s%c", mes, (char) fr.read() );
					}
					fr.close();
				}catch (Exception e){
					try {
						if(fr != null)
							fr.close();
					}catch (IOException io){
						//ignore
					}
					return;
				}
				AlertDialog.Builder builder = new AlertDialog.Builder( MainActivity.this )
						.setCancelable( false )
						.setNeutralButton( "OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						} ).setMessage( mes );
				builder.create().show();
			}
		} );

		findViewById( R.id.btnErrRestartApp ).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				restartApp();
			}
		} );
		AlertDialog.Builder builder = new AlertDialog.Builder( this )
				.setTitle( R.string.please )
				.setMessage( Html.fromHtml( getResources().getString( R.string.send_report_dialog_mes ) ) )
				.setCancelable( false ).setPositiveButton( "OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						sendLog();
					}
				} );

		builder.create().show();
	}

	private void sendLog() {
		Intent newIntent = new Intent( Intent.ACTION_SEND );
		newIntent.setType( "message/rfc822" );
		newIntent.putExtra( Intent.EXTRA_EMAIL, new String[]{ "maxsavhelp@gmail.com" } );
		newIntent.putExtra( Intent.EXTRA_SUBJECT, "Error in documenter" );
		newIntent.putExtra( Intent.EXTRA_STREAM, Uri.parse( "file://" + path ) );
		newIntent.putExtra( Intent.EXTRA_TEXT, "Log file attached." );
		StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
		StrictMode.setVmPolicy( builder.build() );
		try {
			startActivity( newIntent );
		} catch (Exception e) {
			Utils.getErrorDialog( e, this ).show();
			e.printStackTrace();
		}
	}

	public void clearAllTraces(View v){
		File file = new File( Utils.getExternalStoragePath().getPath() + "/stacktraces/" );
		if(file.exists()){
			int count = 0;
			File[] files = file.listFiles();
			for(File subFile : files){
				subFile.delete();
				count++;
			}
			file.delete();
			Toast.makeText( this, "Deleted " + count + " files", Toast.LENGTH_SHORT ).show();
		}
	}

	private void deleteInstalledApks(){
		File file = new File( Environment.getExternalStorageDirectory().getPath() + "/.documenter" );
		if(file.exists()){
			File[] files = file.listFiles();
			for(File subFile : files){
				subFile.delete();
			}

			file.delete();
		}
	}

	private long getUsedMemory(){
		Runtime runtime =  Runtime.getRuntime();
		return runtime.totalMemory() - runtime.freeMemory();
	}

	private String getMemoryInfo(){
		Runtime runtime = Runtime.getRuntime();
		final double MB = 1024 * 1024;
		return "Total memory: " + (runtime.totalMemory() / MB) + " MB\n" +
				"Free memory: " + (runtime.freeMemory() / MB) + " MB\n" +
				"Used memory: " + (getUsedMemory() / MB) + " MB";
	}

	public void getUsedMemory(View v){
		Toast.makeText( this, getMemoryInfo(), Toast.LENGTH_LONG ).show();
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

			Toast.makeText( this, "Successful\n\n" + getMemoryInfo(), Toast.LENGTH_SHORT ).show();
		}catch (Exception e){
			Utils.getErrorDialog( e, this ).show();
		}
	}

	public void clearRam(View v){
		MainData.clearAll();
		System.gc();
		int MB = 1024 * 1024;
		Toast.makeText( this, "Memory cleared\n\n" + getMemoryInfo(), Toast.LENGTH_SHORT ).show();
	}

	public void viewCategoryList(View v){
		Intent intent = new Intent(this, CategoryList.class);
		startActivityForResult( intent, Requests.CATEGORY_LIST );
	}

	public void makeError(View v){
		throw new NullPointerException( "Test exception" );
	}

	private void restartApp() {
		Intent intent = new Intent( this, MainActivity.class );
		startActivity( intent );
		this.finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if(requestCode == Requests.CATEGORY_LIST ){
			if(resultCode == Results.EXIT ){
				finishAndRemoveTask();
			}
			if(resultCode == Results.RESTART_ACTIVITY ){
				viewCategoryList( null );
			}
		}
		if ( resultCode == Results.RESTART_APP ) {
			restartApp();
		}
		super.onActivityResult( requestCode, resultCode, data );
	}
}
