package com.maxsavitsky.documenter;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.maxsavitsky.documenter.backup.BackupInstruments;
import com.maxsavitsky.documenter.backup.CloudBackupInstruments;
import com.maxsavitsky.documenter.codes.Requests;
import com.maxsavitsky.documenter.utils.ApkInstaller;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.utils.UpdatesChecker;
import com.maxsavitsky.documenter.utils.UpdatesDownloader;
import com.maxsavitsky.documenter.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class SettingsActivity extends ThemeActivity {

	private boolean mMemoryAccessGranted = false;
	private FirebaseAuth mAuth;

	private void applyTheme() {
		ActionBar actionBar = getSupportActionBar();
		if ( actionBar != null ) {
			Utils.applyDefaultActionBarStyle( actionBar );
			actionBar.setTitle( R.string.settings );
		}
	}

	@Override
	public void onBackPressed() {
		setResult( Results.OK );
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
		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_settings );

		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );

		applyTheme();

		( (TextView) findViewById( R.id.txtVersion ) ).setText( String.format( Locale.ROOT, "Version: %s\nCode: %d Build: %d", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, BuildConfig.BUILD_CODE ) );

		final Switch swDarkHeader = findViewById( R.id.swDarkTheme );
		swDarkHeader.setChecked( sp.getBoolean( "dark_theme", false ) );
		swDarkHeader.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final boolean isChecked = swDarkHeader.isChecked();
				sp.edit().putBoolean( "dark_theme", isChecked ).apply();
				final AlertDialog.Builder builder = new AlertDialog.Builder( SettingsActivity.this, SettingsActivity.super.mAlertDialogStyle )
						.setMessage( R.string.need_to_restart_app )
						.setCancelable( false )
						.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
								setResult( Results.RESTART_APP );
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
		Switch swCheckForUpdates = findViewById( R.id.switch_check_for_updates );
		swCheckForUpdates.setChecked( sp.getBoolean( "check_updates", true ) );
		swCheckForUpdates.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				sp.edit().putBoolean( "check_updates", isChecked ).apply();
				if ( !isChecked ) {
					Toast.makeText( SettingsActivity.this, ":(", Toast.LENGTH_SHORT ).show();
				}
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

		mAuth = FirebaseAuth.getInstance();
		updateUserUi( mAuth.getCurrentUser() );
	}

	private void updateUserUi(FirebaseUser user) {
		if ( user == null ) {
			findViewById( R.id.layout_authorised_backup ).setVisibility( View.GONE );
			findViewById( R.id.layout_not_authorised_backup ).setVisibility( View.VISIBLE );
		} else {
			findViewById( R.id.layout_authorised_backup ).setVisibility( View.VISIBLE );
			findViewById( R.id.layout_not_authorised_backup ).setVisibility( View.GONE );
		}
	}

	public void signButtonsAction(View v) {
		if ( v.getId() == R.id.btnSignIn ) {
			startActivityForResult( AuthUI.getInstance()
							.createSignInIntentBuilder()
							.build(),

					Requests.SIGN_IN );
		} else if ( v.getId() == R.id.btnSignOut ) {
			AuthUI.getInstance().signOut( this )
					.addOnCompleteListener( new OnCompleteListener<Void>() {
						@Override
						public void onComplete(@NonNull Task<Void> task) {
							updateUserUi( mAuth.getCurrentUser() );
						}
					} );
		}
	}

	public void cloudBackupParams(View v){
		Intent intent = new Intent(this, CloudBackupActivity.class);
		startActivityForResult( intent, Requests.CLOUD_BACKUP_PARAMS );
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult( requestCode, resultCode, data );
		if ( requestCode == Requests.SIGN_IN ) {
			updateUserUi( mAuth.getCurrentUser() );
		}
		if(resultCode == Results.RESTART_APP){
			setResult( resultCode );
			finish();
		}
	}

	private ProgressDialog mCheckUpdatesDialog = null;

	private boolean isMemoryAccessGranted() {
		boolean write = ContextCompat.checkSelfPermission( this, Manifest.permission.WRITE_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED;
		boolean read = ContextCompat.checkSelfPermission( this, Manifest.permission.READ_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED;
		return write && read;
	}

	private final UpdatesChecker.CheckResults mCheckResults = new UpdatesChecker.CheckResults() {
		@Override
		public void noUpdates(UpdatesChecker.VersionInfo versionInfo) {
			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					if ( mCheckUpdatesDialog != null ) {
						mCheckUpdatesDialog.dismiss();
					}
					Toast.makeText( SettingsActivity.this, R.string.app_is_up_to_date, Toast.LENGTH_LONG ).show();
				}
			} );
		}

		@Override
		public void updateAvailable(final UpdatesChecker.VersionInfo versionInfo) {
			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					if ( mCheckUpdatesDialog != null ) {
						mCheckUpdatesDialog.dismiss();
					}
					AlertDialog.Builder builder = new AlertDialog.Builder( SettingsActivity.this, SettingsActivity.super.mAlertDialogStyle );
					builder.setTitle( R.string.update_available )
							.setCancelable( false )
							.setMessage( R.string.would_you_like_to_download_and_install )
							.setPositiveButton( R.string.yes, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									download( versionInfo );
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
			if ( mDownloadPd != null ) {
				mDownloadPd.dismiss();
			}
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
							if ( mCheckUpdatesDialog != null ) {
								mCheckUpdatesDialog.dismiss();
							}
							if ( mDownloadPd != null ) {
								mDownloadPd.dismiss();
							}
							Utils.getErrorDialog( e, SettingsActivity.this ).show();
						}
					} );
				}
			} );
		}
	};

	public void checkForUpdates(View v) {
		mCheckUpdatesDialog = new ProgressDialog( this );
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
	private ProgressDialog mDownloadPd = null;
	private UpdatesChecker.VersionInfo tempVersionInfo;

	private void download(UpdatesChecker.VersionInfo versionInfo) {
		if ( !mMemoryAccessGranted ) {
			if ( !isMemoryAccessGranted() ) {
				tempVersionInfo = versionInfo;
				requestPermissions( new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE }, 1 );
				return;
			}
		}
		mMemoryAccessGranted = false;
		final UpdatesDownloader downloader = new UpdatesDownloader( versionInfo, mCheckResults );
		mDownloadPd = new ProgressDialog( SettingsActivity.this );
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

	private void unpack() {
		File file = new File( Environment.getExternalStorageDirectory().getPath() + "/documenter_backup.zip" );
		if ( !file.exists() ) {
			Toast.makeText( this, R.string.file_not_found, Toast.LENGTH_SHORT ).show();
			return;
		}
		try {
			BackupInstruments.restoreFromBackup( file );

			setResult( Results.RESTART_APP );
			finish();
		} catch (IOException e) {
			e.printStackTrace();
			Utils.getErrorDialog( e, this ).show();
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
		File outputFile = new File( Environment.getExternalStorageDirectory().getPath() + "/documenter_backup.zip" );
		try {
			outputFile.createNewFile();

			BackupInstruments.createBackupToFile( outputFile );

			Toast.makeText( this, R.string.successfully, Toast.LENGTH_SHORT ).show();
		} catch (IOException e) {
			e.printStackTrace();
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
		if ( requestCode == 1 ) {
			if ( grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED && grantResults[ 1 ] == PackageManager.PERMISSION_GRANTED ) {
				mMemoryAccessGranted = true;
				download( tempVersionInfo );
				tempVersionInfo = null;
			} else {
				Toast.makeText( this, "Permission denied", Toast.LENGTH_SHORT ).show();
			}
		}
	}
}
