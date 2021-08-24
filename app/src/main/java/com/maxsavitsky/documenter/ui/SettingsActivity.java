package com.maxsavitsky.documenter.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.text.HtmlCompat;

import com.firebase.ui.auth.AuthUI;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.maxsavitsky.documenter.App;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.ThemeActivity;
import com.maxsavitsky.documenter.backup.BackupInstruments;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.ui.widget.ButtonWithDropdown;
import com.maxsavitsky.documenter.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SettingsActivity extends ThemeActivity {

	private FirebaseAuth mAuth;

	private final ActivityResultLauncher<Intent> mSignInLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result->{
				updateUserUi( mAuth.getCurrentUser() );
				if ( mAuth.getCurrentUser() != null && !mAuth.getCurrentUser().isEmailVerified() ) {
					mAuth.getCurrentUser().sendEmailVerification();
				}
			}
	);

	private final ActivityResultLauncher<Intent> mCloudBackupParamsLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result->{
				if(result.getResultCode() == Results.RESTART_APP){
					setResult( result.getResultCode() );
					onBackPressed();
				}
			}
	);

	private final ActivityResultLauncher<Intent> mChooseBackupFileLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result->{
				if ( result.getResultCode() == Activity.RESULT_OK ) {
					Intent data = result.getData();
					if ( data != null ) {
						Uri uri = data.getData();
						AlertDialog.Builder builder = new AlertDialog.Builder( this )
								.setTitle( R.string.confirmation )
								.setMessage( R.string.confirm_restore_message )
								.setCancelable( false )
								.setPositiveButton( "OK", (dialog, which)->{
									dialog.cancel();
									restoreFromUri( uri );
								} )
								.setNeutralButton( R.string.cancel, (dialog, which)->dialog.cancel() );
						builder.create().show();
					}
				}
			}
	);

	private final ActivityResultLauncher<Intent> mCreateBackupLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result->{
				if ( result.getResultCode() == Activity.RESULT_OK ) {
					Intent data = result.getData();
					if ( data != null ) {
						Uri uri = data.getData();
						createBackupToFileUri( uri );
					}
				}
			}
	);

	private void applyTheme() {
		ActionBar actionBar = getSupportActionBar();
		if ( actionBar != null ) {
			Utils.applyDefaultActionBarStyle( actionBar );
			actionBar.setTitle( R.string.settings );
		}
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
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_settings );

		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );

		applyTheme();

		SharedPreferences sp = Utils.getDefaultSharedPreferences();

		ButtonWithDropdown button = findViewById( R.id.theme_dropdown_button );
		String[] elements = getResources().getStringArray( R.array.theme_states );
		button.setElements( elements );
		button.setSelection( sp.getInt( "theme_state", 2 ) );
		button.setOnItemSelectedListener( index->{
			sp.edit().putInt( "theme_state", index ).apply();
			setResult( Results.RESTART_APP );
			finish();
		} );

		ButtonWithDropdown updatesChannelButton = findViewById( R.id.updates_channel_dropdown_button );
		updatesChannelButton.setElements( getResources().getStringArray( R.array.updates_channels ) );
		updatesChannelButton.setSelection( sp.getInt( "updates_channel", 0 ) );
		updatesChannelButton.setOnItemSelectedListener( index->sp.edit().putInt( "updates_channel", index ).apply() );

		SwitchMaterial swCheckForUpdates = findViewById( R.id.switch_check_for_updates );
		swCheckForUpdates.setChecked( sp.getBoolean( "check_updates", true ) );
		swCheckForUpdates.setOnCheckedChangeListener( (buttonView, isChecked)->sp.edit().putBoolean( "check_updates", isChecked ).apply() );

		SwitchMaterial swKeepScreenOn = findViewById( R.id.swKeepScreenOn );
		swKeepScreenOn.setChecked( sp.getBoolean( "keep_screen_on", true ) );
		swKeepScreenOn.setOnCheckedChangeListener( (buttonView, isChecked)->sp.edit().putBoolean( "keep_screen_on", isChecked ).apply() );

		mAuth = FirebaseAuth.getInstance();
		if ( mAuth.getCurrentUser() != null ) {
			mAuth.getCurrentUser().reload();
		}
		updateUserUi( mAuth.getCurrentUser() );

		findViewById( R.id.btn_about_app ).setOnClickListener( v->startActivity( new Intent( this, AboutAppActivity.class ) ) );
	}

	private void updateUserUi(FirebaseUser user) {
		if ( user == null ) {
			findViewById( R.id.layout_authorised_backup ).setVisibility( View.GONE );
			findViewById( R.id.layout_email_not_verified ).setVisibility( View.GONE );
			findViewById( R.id.layout_not_authorised_backup ).setVisibility( View.VISIBLE );
			findViewById( R.id.lblLoggedIn ).setVisibility( View.GONE );
		} else {
			if ( user.isEmailVerified() ) {
				findViewById( R.id.layout_authorised_backup ).setVisibility( View.VISIBLE );
				findViewById( R.id.layout_email_not_verified ).setVisibility( View.GONE );
			} else {
				findViewById( R.id.layout_authorised_backup ).setVisibility( View.GONE );
				findViewById( R.id.layout_email_not_verified ).setVisibility( View.VISIBLE );
			}
			findViewById( R.id.layout_not_authorised_backup ).setVisibility( View.GONE );
			TextView textViewLoggedIn = findViewById( R.id.lblLoggedIn );
			textViewLoggedIn.setVisibility( View.VISIBLE );
			textViewLoggedIn.setText( String.format( "%s %s", getString( R.string.logged_in ), user.getEmail() ) );
		}
	}

	public void signButtonsAction(View v) {
		if ( v.getId() == R.id.btnSignIn ) {
			mSignInLauncher.launch( AuthUI.getInstance()
					.createSignInIntentBuilder()
					.build() );
		} else if ( v.getId() == R.id.btnSignOut || v.getId() == R.id.btnSignOutVer ) {
			AuthUI.getInstance().signOut( this )
					.addOnCompleteListener( task->updateUserUi( mAuth.getCurrentUser() ) );
		} else if ( v.getId() == R.id.btnSendVerification ) {
			FirebaseUser user = mAuth.getCurrentUser();
			if ( user != null ) {
				user.sendEmailVerification()
						.addOnCompleteListener( task->{
							if ( task.isSuccessful() ) {
								Toast.makeText( SettingsActivity.this, "Email sent", Toast.LENGTH_SHORT ).show();
							}
						} );
			}
		}
	}

	public void cloudBackupParams(View v) {
		Intent intent = new Intent( this, CloudBackupActivity.class );
		mCloudBackupParamsLauncher.launch( intent );
	}

	public void checkForUpdates(View v) {
		ProgressDialog progressDialog = new ProgressDialog( this );
		progressDialog.setMessage( getResources().getString( R.string.checking_for_updates ) );
		progressDialog.setCancelable( false );
		progressDialog.show();
		com.maxsavteam.updateschecker.Utils.runFullCheck( this, Utils.getDefaultSharedPreferences().getInt( "updates_channel", 0 ), new com.maxsavteam.updateschecker.Utils.FullCheckCallback() {
			@Override
			public void onUpdateAvailable(com.maxsavteam.updateschecker.VersionInfo versionInfo) {
				runOnUiThread( progressDialog::dismiss );
			}

			@Override
			public File createDestinationFile(com.maxsavteam.updateschecker.VersionInfo versionInfo) {
				File file = new File( App.appStoragePath, "updates" );
				if(!file.exists())
					file.mkdirs();
				file = new File( file, versionInfo.getVersionName() );
				if(!file.exists()) {
					try {
						file.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
						onFailure( e );
						return null;
					}
				}
				return file;
			}

			@Override
			public void onNoUpdates() {
				runOnUiThread( ()->{
						progressDialog.dismiss();
					Toast.makeText( SettingsActivity.this, R.string.app_is_up_to_date, Toast.LENGTH_LONG ).show();
				} );
			}

			@Override
			public void onFailure(Exception e) {
				runOnUiThread( ()->{
						progressDialog.dismiss();
					Toast.makeText( SettingsActivity.this, e.toString(), Toast.LENGTH_LONG ).show();
				} );
			}
		} );
	}

	public void initialUnpack(View v) {
		Intent intent = new Intent( Intent.ACTION_GET_CONTENT );
		intent.setType( "application/zip" );
		mChooseBackupFileLauncher.launch( intent );
	}

	private void restoreFromUri(Uri uri) {
		final ProgressDialog pd = new ProgressDialog( this );
		pd.setMessage( HtmlCompat.fromHtml( getString( R.string.restoring_please_donot_close_app ), HtmlCompat.FROM_HTML_MODE_COMPACT ) );
		pd.setCancelable( false );
		pd.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL );
		pd.setIndeterminate( true );
		final BackupInstruments.BackupCallback backupCallback = new BackupInstruments.BackupCallback() {
			@Override
			public void onBackupStateChanged(BackupInstruments.BackupState state) {

			}

			@Override
			public void onProgress(int percent) {
				runOnUiThread( ()->{
					pd.setIndeterminate( false );
					pd.setProgress( percent );
				} );
			}

			@Override
			public void onSuccess(long timeOfCreation) {
				runOnUiThread( pd::dismiss );
				setResult( Results.RESTART_APP );
				finish();
			}

			@Override
			public void onException(final Exception e) {
				runOnUiThread( ()->Utils.getErrorDialog( e, SettingsActivity.this ).show() );
			}
		};
		pd.show();
		new Thread( ()->{
			try {
				InputStream is = getContentResolver().openInputStream( uri );
				BackupInstruments.restoreFromStream( is, backupCallback );
			} catch (IOException e) {
				e.printStackTrace();
				backupCallback.onException( e );
			}
		} ).start();
	}

	public void initialBackup(View v) {
		Intent intent = new Intent( Intent.ACTION_CREATE_DOCUMENT );
		intent.setType( "application/zip" );
		intent.putExtra( Intent.EXTRA_TITLE, "Documenter backup" );
		mCreateBackupLauncher.launch( intent );
	}

	private void createBackupToFileUri(Uri uri){
		final ProgressDialog pd = new ProgressDialog( this );
		pd.setMessage( getString( R.string.creating_backup ) );
		pd.setCancelable( false );
		pd.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL );
		pd.setIndeterminate( true );
		final BackupInstruments.BackupCallback backupCallback = new BackupInstruments.BackupCallback() {
			@Override
			public void onBackupStateChanged(BackupInstruments.BackupState state) {

			}

			@Override
			public void onProgress(int percent) {
				runOnUiThread( ()->{
					pd.setIndeterminate( false );
					pd.setProgress( percent );
				} );
			}

			@Override
			public void onSuccess(long timeOfCreation) {
				runOnUiThread( ()->{
					pd.dismiss();
					Toast.makeText( SettingsActivity.this, R.string.successfully, Toast.LENGTH_SHORT ).show();
				} );
			}

			@Override
			public void onException(final Exception e) {
				runOnUiThread( ()->{
					pd.dismiss();
					Utils.getErrorDialog( e, SettingsActivity.this ).show();
				} );
			}
		};
		pd.show();
		new Thread( ()->{
			try {
				OutputStream os = getContentResolver().openOutputStream( uri );
				BackupInstruments.createBackupToOutputStream( os, backupCallback );
			} catch (IOException e) {
				e.printStackTrace();
				backupCallback.onException( e );
			}
		} ).start();
	}
}
