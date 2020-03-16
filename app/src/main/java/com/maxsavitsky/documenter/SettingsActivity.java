package com.maxsavitsky.documenter;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.OverScroller;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.maxsavitsky.documenter.utils.ApkInstaller;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.UpdatesChecker;
import com.maxsavitsky.documenter.utils.UpdatesDownloader;
import com.maxsavitsky.documenter.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SettingsActivity extends ThemeActivity {

	private boolean mMemoryAccessGranted = false;

	private void applyTheme() {
		ActionBar actionBar = getSupportActionBar();
		if ( actionBar != null ) {
			Utils.applyDefaultActionBarStyle( actionBar );
			actionBar.setTitle( R.string.settings );
		}
	}

	@Override
	public void onBackPressed() {
		setResult( ResultCodes.OK );
		finish();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if ( item.getItemId() == android.R.id.home ) {
			onBackPressed();
		}
		return super.onOptionsItemSelected( item );
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_settings );

		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );

		applyTheme();

		(( TextView ) findViewById( R.id.txtVersion )).setText( String.format( Locale.ROOT, "Version: %s\nBuild: %d", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE ) );


		final Switch swDarkHeader = findViewById( R.id.swDarkTheme );
		swDarkHeader.setChecked( sp.getBoolean( "dark_theme", false ) );
		swDarkHeader.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v){
				final boolean isChecked = swDarkHeader.isChecked();
				sp.edit().putBoolean( "dark_theme", isChecked ).apply();
				final AlertDialog.Builder builder = new AlertDialog.Builder( SettingsActivity.this, SettingsActivity.super.mAlertDialogStyle )
						.setMessage( "Need to restart app" )
						.setCancelable( false )
						.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
								setResult( ResultCodes.RESTART_APP );
								finish();
							}
						} ).setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
								sp.edit().putBoolean( "dark_theme", !isChecked ).apply();
								swDarkHeader.setChecked( !isChecked );
							}
						} );

				runOnUiThread( new Runnable() {
					@Override
					public void run() {
						builder.create().show();
					}
				} );
			}
		} );

		Switch swKeepScreenOn = findViewById( R.id.swKeepScreenOn );
		swKeepScreenOn.setChecked( sp.getBoolean( "keep_screen_on", true ) );
		swKeepScreenOn.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
				sp.edit().putBoolean( "keep_screen_on", isChecked ).apply();
			}
		} );
	}

	ProgressDialog mCheckUpdatesDialog = null;

	private boolean isMemoryAccessGranted(){
		boolean write = ContextCompat.checkSelfPermission( this, Manifest.permission.WRITE_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED;
		boolean read = ContextCompat.checkSelfPermission( this, Manifest.permission.READ_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED;
		return write && read;
	}

	private UpdatesChecker.CheckResults mCheckResults = new UpdatesChecker.CheckResults() {
		@Override
		public void noUpdates(UpdatesChecker.VersionInfo versionInfo) {
			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					if(mCheckUpdatesDialog != null)
						mCheckUpdatesDialog.dismiss();
					Toast.makeText( SettingsActivity.this, R.string.app_is_up_to_date, Toast.LENGTH_LONG ).show();
				}
			} );
		}

		@Override
		public void updateAvailable(final UpdatesChecker.VersionInfo versionInfo) {
			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					if(mCheckUpdatesDialog != null)
						mCheckUpdatesDialog.dismiss();
					AlertDialog.Builder builder = new AlertDialog.Builder( SettingsActivity.this, SettingsActivity.super.mAlertDialogStyle );
					builder.setTitle( R.string.update_available )
							.setCancelable( false )
							.setMessage( R.string.would_you_like_to_download_and_install )
							.setPositiveButton( R.string.yes, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									download( versionInfo.getDownloadUrl(), versionInfo );
									dialog.cancel();
								}
							} )
							.setNegativeButton( R.string.no, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.cancel();
								}
							} );
					builder.create().show();
				}
			} );
		}

		@Override
		public void downloaded(File path, UpdatesChecker.VersionInfo versionInfo) {
			if(mDownloadPd != null)
				mDownloadPd.dismiss();
			ApkInstaller.installApk( SettingsActivity.this, path );
		}

		@Override
		public void exceptionOccurred(final IOException e) {
			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					runOnUiThread( new Runnable() {
						@Override
						public void run() {
							if(mCheckUpdatesDialog != null)
								mCheckUpdatesDialog.dismiss();
							if(mDownloadPd != null)
								mDownloadPd.dismiss();
							Utils.getErrorDialog( e, SettingsActivity.this ).show();
						}
					} );
				}
			} );
		}
	};

	public void checkForUpdates(View v){
		mCheckUpdatesDialog = new ProgressDialog(this);
		mCheckUpdatesDialog.setMessage( getResources().getString( R.string.checking_for_updates ) );
		mCheckUpdatesDialog.setCancelable( false );
		mCheckUpdatesDialog.show();
		final UpdatesChecker checker = new UpdatesChecker( this, mCheckResults );
		new Thread( new Runnable() {
			@Override
			public void run() {
				checker.runCheck();
			}
		} ).start();
	}

	private Thread downloadThread;
	ProgressDialog mDownloadPd = null;
	String tempDownloadUrl;
	private UpdatesChecker.VersionInfo tempVersionInfo;

	private void download(final String dUrl, UpdatesChecker.VersionInfo versionInfo){
		if(!mMemoryAccessGranted ){
			if(!isMemoryAccessGranted()) {
				tempDownloadUrl = dUrl;
				tempVersionInfo = versionInfo;
				requestPermissions( new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE }, 1 );
				return;
			}
		}
		mMemoryAccessGranted = false;
		final UpdatesDownloader downloader = new UpdatesDownloader( versionInfo, mCheckResults );
		mDownloadPd = new ProgressDialog(SettingsActivity.this);
		mDownloadPd.setMessage( "Downloading..." );
		mDownloadPd.setCancelable( false );
		mDownloadPd.setButton( ProgressDialog.BUTTON_NEGATIVE, getResources().getString( R.string.cancel ), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, int which) {
				downloadThread.interrupt();
				runOnUiThread( new Runnable() {
					@Override
					public void run() {
						dialog.cancel();
					}
				} );
			}
		} );
		mDownloadPd.show();
		downloadThread = new Thread( new Runnable() {
			@Override
			public void run() {
				downloader.download();
			}
		} );
		downloadThread.start();
	}

	private String readFile(File file) {
		try {
			String s = "";
			FileInputStream fileInputStream = new FileInputStream(file);
			byte[] buffer = new byte[1024];
			int len;
			while((len = fileInputStream.read(buffer)) != -1){
				if(len < 1024){
					buffer = Arrays.copyOf(buffer, len);
				}

				s = String.format( "%s%s", s, new String(buffer, StandardCharsets.UTF_8 ) );
			}
			fileInputStream.close();
			return s;
		} catch (Exception e) {
			Utils.getErrorDialog( e, this ).show();
			return "null";
		}
	}

	private void myPack(File path, String fileName, ZipOutputStream out) throws IOException {
		if ( path.isHidden() ) {
			return;
		}

		if ( path.isDirectory() ) {
			if ( fileName.endsWith( "/" ) ) {
				out.putNextEntry( new ZipEntry( fileName ) );
			} else {
				out.putNextEntry( new ZipEntry( fileName + "/" ) );
			}
			out.closeEntry();

			File[] children = path.listFiles();
			for (File child : children) {
				myPack( child, fileName + "/" + child.getName(), out );
			}
			return;
		}

		String content = readFile( path );
		ZipEntry zipEntry = new ZipEntry( fileName );
		out.putNextEntry( zipEntry );
		out.write( content.getBytes(), 0, content.getBytes().length );
	}

	private void unpack() {
		File file = new File( Environment.getExternalStorageDirectory().getPath() + "/documenter_backup.zip" );
		if ( !file.exists() ) {
			Toast.makeText( this, R.string.file_not_found, Toast.LENGTH_SHORT ).show();
			return;
		}
		File[] files = Utils.getExternalStoragePath().listFiles();
		for (File child : files) {
			if ( child.isDirectory() ) {
				deleteDirectory( child );
			} else {
				child.delete();
			}
		}

		File destinationDir = Utils.getExternalStoragePath();
		try {
			ZipInputStream zis = new ZipInputStream( new FileInputStream( file.getAbsoluteFile() ) );
			byte[] buffer = new byte[ 1024 ];
			ZipEntry zipEntry;
			while ( ( zipEntry = zis.getNextEntry() ) != null ) {
				File newFile = new File( destinationDir.getCanonicalPath(), zipEntry.getName() );
				if ( zipEntry.isDirectory() ) {
					newFile.mkdir();
					continue;
				}
				FileOutputStream fos = new FileOutputStream( newFile );

				int len;
				while ( ( len = zis.read( buffer ) ) > 0 ) {
					fos.write( buffer, 0, len );
				}
				fos.close();
			}
			zis.closeEntry();
			zis.close();

			Toast.makeText( this, R.string.successful, Toast.LENGTH_SHORT ).show();
			setResult( ResultCodes.RESTART_APP );
			finish();
		} catch (Exception e) {
			Utils.getErrorDialog( e, this ).show();
		}
	}

	private void deleteDirectory(File dir) {
		File[] children = dir.listFiles();
		for (File file : children) {
			if ( file.isDirectory() ) {
				deleteDirectory( file );
			} else {
				file.delete();
			}
		}
	}

	public void initialUnpack(View v) {
		AlertDialog.Builder builder = new AlertDialog.Builder( this )
				.setTitle( R.string.confirmation )
				.setMessage( R.string.confirm_restore_message )
				.setCancelable( false )
				.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						if ( ContextCompat.checkSelfPermission( SettingsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_DENIED ) {
							requestPermissions( new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE }, 11 );
						} else {
							unpack();
						}
					}
				} )
				.setNeutralButton( R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				} );
		builder.create().show();
	}

	public void initialBackup(View v) {
		if ( ContextCompat.checkSelfPermission( this, Manifest.permission.WRITE_EXTERNAL_STORAGE )
				== PackageManager.PERMISSION_DENIED ||
				ContextCompat.checkSelfPermission( this, Manifest.permission.READ_EXTERNAL_STORAGE )
						== PackageManager.PERMISSION_DENIED ) {
			requestPermissions( new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE }, 10 );
			//requestPermissions( new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE}, 11 );
		} else {
			createMyBackup();
		}
	}

	private void createMyBackup() {
		File dir = Utils.getExternalStoragePath();
		try {
			File outputFile = new File( Environment.getExternalStorageDirectory().getPath() + "/documenter_backup.zip" );
			outputFile.createNewFile();
			ZipOutputStream zipOutputStream = new ZipOutputStream( new FileOutputStream( outputFile ) );
			zipOutputStream.setLevel( 9 );

			if ( dir.listFiles() == null ) {
				zipOutputStream.close();
				return;
			}
			for (File file : dir.listFiles()) {
				if ( !file.getName().equals( "stacktraces" ) ) {
					myPack( file, file.getName(), zipOutputStream );
				}
			}
			zipOutputStream.close();
			Toast.makeText( this, R.string.successful, Toast.LENGTH_SHORT ).show();
		} catch (Exception e) {
			Utils.getErrorDialog( e, this ).show();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if ( requestCode == 10 ) {
			if ( grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED && grantResults[ 1 ] == PackageManager.PERMISSION_GRANTED ) {
				createMyBackup();
			} else {
				Toast.makeText( this, "Denied", Toast.LENGTH_SHORT ).show();
			}
		} else if ( requestCode == 11 ) {
			if ( grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED ) {
				unpack();
			} else {
				Toast.makeText( this, "Denied", Toast.LENGTH_SHORT ).show();
			}
		}
		if(requestCode == 1){
			if ( grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED && grantResults[ 1 ] == PackageManager.PERMISSION_GRANTED ){
				mMemoryAccessGranted = true;
				download( tempDownloadUrl, tempVersionInfo );
				tempDownloadUrl = null;
				tempVersionInfo = null;
			}else{
				Toast.makeText( this, "Permission denied", Toast.LENGTH_SHORT ).show();
			}
		}
	}
}
