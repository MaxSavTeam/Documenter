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
import android.text.Html;
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
import com.maxsavitsky.documenter.backup.BackupInterface;
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

		( (TextView) findViewById( R.id.txtVersion ) ).setText( String.format( Locale.ROOT, "%s: %s", getString( R.string.version ), BuildConfig.VERSION_NAME ) );

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
		if(mAuth.getCurrentUser() != null)
			mAuth.getCurrentUser().reload();
		updateUserUi( mAuth.getCurrentUser() );
	}

	private void updateUserUi(FirebaseUser user) {
		if ( user == null ) {
			findViewById( R.id.layout_authorised_backup ).setVisibility( View.GONE );
			findViewById( R.id.layout_email_not_verified ).setVisibility( View.GONE );
			findViewById( R.id.layout_not_authorised_backup ).setVisibility( View.VISIBLE );
			findViewById( R.id.lblLoggedIn ).setVisibility( View.GONE );
		} else {
			if(user.isEmailVerified()) {
				findViewById( R.id.layout_authorised_backup ).setVisibility( View.VISIBLE );
				findViewById( R.id.layout_not_authorised_backup ).setVisibility( View.GONE );
				findViewById( R.id.layout_email_not_verified ).setVisibility( View.GONE );
			}else{
				findViewById( R.id.layout_authorised_backup ).setVisibility( View.GONE );
				findViewById( R.id.layout_not_authorised_backup ).setVisibility( View.GONE );
				findViewById( R.id.layout_email_not_verified ).setVisibility( View.VISIBLE );
			}
			TextView textViewLoggedIn = findViewById(R.id.lblLoggedIn);
			textViewLoggedIn.setVisibility( View.VISIBLE );
			textViewLoggedIn.setText( String.format( "%s %s", getString(R.string.logged_in), user.getEmail() ) );
		}
	}

	public void signButtonsAction(View v) {
		if ( v.getId() == R.id.btnSignIn ) {
			startActivityForResult( AuthUI.getInstance()
							.createSignInIntentBuilder()
							.build(),

					Requests.SIGN_IN );
		} else if ( v.getId() == R.id.btnSignOut || v.getId() == R.id.btnSignOutVer ) {
			AuthUI.getInstance().signOut( this )
					.addOnCompleteListener( new OnCompleteListener<Void>() {
						@Override
						public void onComplete(@NonNull Task<Void> task) {
							updateUserUi( mAuth.getCurrentUser() );
						}
					} );
		}else if(v.getId() == R.id.btnSendVerification){
			FirebaseUser user = mAuth.getCurrentUser();
			if(user != null) {
				user.sendEmailVerification()
						.addOnCompleteListener( new OnCompleteListener<Void>() {
							@Override
							public void onComplete(@NonNull Task<Void> task) {
								if(task.isSuccessful()){
									Toast.makeText( SettingsActivity.this, "Email sent", Toast.LENGTH_SHORT ).show();
								}
							}
						} );
			}
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
			if(mAuth.getCurrentUser() != null && !mAuth.getCurrentUser().isEmailVerified()){
				mAuth.getCurrentUser().sendEmailVerification();
			}
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
		public void onDownloadProgress(final int bytesCount, final int totalBytesCount) {
			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					mDownloadPd.setIndeterminate( false );
					mDownloadPd.setMax( 100 );
					mDownloadPd.setProgress( bytesCount * 100 / totalBytesCount );
				}
			} );
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
		mDownloadPd.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL );
		mDownloadPd.setMessage( getString( R.string.downloading ) );
		mDownloadPd.setCancelable( false );
		mDownloadPd.setIndeterminate( true );
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
		final File file = new File( Environment.getExternalStorageDirectory().getPath() + "/documenter_backup.zip" );
		if ( !file.exists() ) {
			Toast.makeText( this, R.string.file_not_found, Toast.LENGTH_SHORT ).show();
			return;
		}
		final ProgressDialog pd = new ProgressDialog( this );
		pd.setMessage( Html.fromHtml( getString(R.string.restoring_please_donot_close_app) ) );
		pd.setCancelable( false );
		final BackupInterface backupInterface = new BackupInterface() {
			@Override
			public void successfully(long timeOfCreation) {
				runOnUiThread( new Runnable() {
					@Override
					public void run() {
						pd.dismiss();
					}
				} );
				setResult( Results.RESTART_APP );
				finish();
			}

			@Override
			public void failed() {
				runOnUiThread( new Runnable() {
					@Override
					public void run() {
						pd.dismiss();
						Toast.makeText( SettingsActivity.this, R.string.something_gone_wrong, Toast.LENGTH_SHORT ).show();
					}
				} );
			}

			@Override
			public void exceptionOccurred(final Exception e) {
				runOnUiThread( new Runnable() {
					@Override
					public void run() {
						Utils.getErrorDialog( e, SettingsActivity.this ).show();
					}
				} );
			}
		};
		pd.show();
		new Thread( new Runnable() {
			@Override
			public void run() {
				try {
					BackupInstruments.restoreFromBackup( file, backupInterface );
				} catch (IOException e) {
					e.printStackTrace();
					backupInterface.exceptionOccurred( e );
				}
			}
		} ).start();
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
		final File outputFile = new File( Environment.getExternalStorageDirectory().getPath() + "/documenter_backup.zip" );
		final ProgressDialog pd = new ProgressDialog( this );
		pd.setMessage( Html.fromHtml( getString(R.string.creating_backup) ) );
		pd.setCancelable( false );
		final BackupInterface backupInterface = new BackupInterface() {
			@Override
			public void successfully(long timeOfCreation) {
				runOnUiThread( new Runnable() {
					@Override
					public void run() {
						pd.dismiss();
						Toast.makeText( SettingsActivity.this, R.string.successfully, Toast.LENGTH_SHORT ).show();
					}
				} );
			}

			@Override
			public void failed() {
				runOnUiThread( new Runnable() {
					@Override
					public void run() {
						pd.dismiss();
						Toast.makeText( SettingsActivity.this, R.string.something_gone_wrong, Toast.LENGTH_SHORT ).show();
					}
				} );
			}

			@Override
			public void exceptionOccurred(final Exception e) {
				runOnUiThread( new Runnable() {
					@Override
					public void run() {
						pd.dismiss();
						Utils.getErrorDialog( e, SettingsActivity.this ).show();
					}
				} );
			}
		};
		pd.show();
		new Thread( new Runnable() {
			@Override
			public void run() {
				try {
					BackupInstruments.createBackupToFile( outputFile, backupInterface );
				} catch (IOException e) {
					e.printStackTrace();
					backupInterface.exceptionOccurred( e );
				}
			}
		} ).start();
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
