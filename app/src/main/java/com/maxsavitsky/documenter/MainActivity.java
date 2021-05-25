package com.maxsavitsky.documenter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.maxsavitsky.documenter.backup.AutonomousCloudBackupper;
import com.maxsavitsky.documenter.codes.Requests;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.ui.CategoryList;
import com.maxsavitsky.documenter.utils.Utils;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

public class MainActivity extends ThemeActivity {
	public static final String TAG = "Documenter";

	private static MainActivity instance;

	public static MainActivity getInstance() {
		return instance;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Utils.setContext( this );
		SharedPreferences sharedPreferences = getSharedPreferences( Utils.APP_PREFERENCES, Context.MODE_PRIVATE );
		Utils.setDefaultSharedPreferences( sharedPreferences );

		super.onCreate( savedInstanceState );
		instance = this;
		setContentView( R.layout.layout_onstartup );
		try {
			Toolbar toolbar = findViewById( R.id.toolbar );
			setSupportActionBar( toolbar );
		} catch (Exception e) {
			e.printStackTrace();
		}

		deleteInstalledApks();

		final AutonomousCloudBackupper backupper = new AutonomousCloudBackupper( this );
		new Thread( backupper::stateChanged, "AutoBackupper" ).start();
	}

	@Override
	protected void onPostCreate(@Nullable Bundle savedInstanceState) {
		super.onPostCreate( savedInstanceState );
		new Thread( ()->{
			try {
				initialize();
				Log.i( TAG, "Initialized" );
			} catch (final Exception e) {
				runOnUiThread( new Runnable() {
					@Override
					public void run() {
						Utils.getErrorDialog( e, MainActivity.this ).show();
					}
				} );
				return;
			}
			try {
				Thread.sleep( 1000 );
				viewCategoryList( null );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} ).start();
	}

	public void clearAllTraces(View v) {
		File file = new File( Utils.getExternalStoragePath().getPath() + "/stacktraces/" );
		if ( file.exists() ) {
			int count = 0;
			File[] files = file.listFiles();
			for (File subFile : files) {
				subFile.delete();
				count++;
			}
			file.delete();
			Toast.makeText( this, "Deleted " + count + " files", Toast.LENGTH_SHORT ).show();
		}
	}

	private void deleteInstalledApks() {
		File file = new File( Environment.getExternalStorageDirectory().getPath() + "/.documenter" );
		if ( file.exists() ) {
			File[] files = file.listFiles();
			for (File subFile : files) {
				subFile.delete();
			}

			file.delete();
		}
	}

	private long getUsedMemory() {
		Runtime runtime = Runtime.getRuntime();
		return runtime.totalMemory() - runtime.freeMemory();
	}

	private String getMemoryInfo() {
		Runtime runtime = Runtime.getRuntime();
		final double MB = 1024 * 1024;
		return "Total memory: " + ( runtime.totalMemory() / MB ) + " MB\n" +
				"Free memory: " + ( runtime.freeMemory() / MB ) + " MB\n" +
				"Used memory: " + ( getUsedMemory() / MB ) + " MB";
	}

	public void getUsedMemory(View v) {
		Toast.makeText( this, getMemoryInfo(), Toast.LENGTH_LONG ).show();
	}

	private void initialize() throws IOException, SAXException {
		MainData.readAllCategories();
		MainData.readAllDocuments();
		MainData.readAllEntries();
	}

	public void reinitialize(View v) {
		MainData.clearAll();

		try {
			initialize();

			Toast.makeText( this, "Successful\n\n" + getMemoryInfo(), Toast.LENGTH_SHORT ).show();
		} catch (Exception e) {
			Utils.getErrorDialog( e, this ).show();
		}
	}

	public void clearRam(View v) {
		MainData.clearAll();
		System.gc();
		int MB = 1024 * 1024;
		Toast.makeText( this, "Memory cleared\n\n" + getMemoryInfo(), Toast.LENGTH_SHORT ).show();
	}

	public void viewCategoryList(View v) {
		Intent intent = new Intent( this, CategoryList.class );
		startActivityForResult( intent, Requests.CATEGORY_LIST );
	}

	public void makeError(View v) {
		BigDecimal a = BigDecimal.ONE.divide( BigDecimal.valueOf( 3 ) );
	}

	private void restartApp() {
		Intent intent = new Intent( this, MainActivity.class );
		startActivity( intent );
		this.finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if ( requestCode == Requests.CATEGORY_LIST ) {
			if ( resultCode == Results.EXIT ) {
				finishAndRemoveTask();
			}
			if ( resultCode == Results.RESTART_ACTIVITY ) {
				viewCategoryList( null );
			}
			if ( resultCode != Results.LOOK_STARTUP ) {
				setContentView( R.layout.activity_main );
			}
		}
		if ( resultCode == Results.RESTART_APP ) {
			restartApp();
		}
		super.onActivityResult( requestCode, resultCode, data );
	}
}
