package com.maxsavitsky.documenter.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.text.HtmlCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.maxsavitsky.documenter.App;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.ThemeActivity;
import com.maxsavitsky.documenter.backup.BackupInstruments;
import com.maxsavitsky.documenter.backup.CloudBackupInstruments;
import com.maxsavitsky.documenter.backup.CloudBackupMaker;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.net.RequestMaker;
import com.maxsavitsky.documenter.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

public class CloudBackupActivity extends ThemeActivity {
	private static final String TAG = App.TAG + " CloudBackupActivity";
	private CloudBackupsListActivity.BackupEntity lastBackup;

	private final ActivityResultLauncher<Intent> mCloudBackupsListActivity = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result->{
				if ( result.getResultCode() == RESULT_OK ) {
					Intent data = result.getData();
					if ( data == null ) {
						return;
					}
					long time = data.getLongExtra( "time", -1 );
					if ( time == -1 ) {
						return;
					}
					showWarningAndRestore( time );
				}
			}
	);

	private void applyTheme() {
		ActionBar actionBar = getSupportActionBar();
		if ( actionBar != null ) {
			Utils.applyDefaultActionBarStyle( actionBar );
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if ( item.getItemId() == android.R.id.home ) {
			onBackPressed();
		}
		return super.onOptionsItemSelected( item );
	}

	private interface Callback {
		void onSuccess();

		void onFailed();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_cloud_backup );
		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );

		applyTheme();

		( (TextView) findViewById( R.id.lblCloud1 ) )
				.setText(
						HtmlCompat.fromHtml(
								getString( R.string.cloud_backup_activity_text1 ),
								HtmlCompat.FROM_HTML_MODE_LEGACY )
				);

		final ProgressDialog pd = new ProgressDialog( this );
		pd.setMessage( getString( R.string.loading ) );
		pd.setButton( ProgressDialog.BUTTON_NEUTRAL, getString( R.string.cancel ), (dialog, which)->onBackPressed() );
		pd.show();
		final Callback callback = new Callback() {
			@Override
			public void onSuccess() {
				pd.dismiss();
			}

			@Override
			public void onFailed() {
				runOnUiThread( ()->{
					pd.dismiss();
					Toast.makeText( CloudBackupActivity.this, R.string.something_gone_wrong, Toast.LENGTH_SHORT ).show();
					onBackPressed();
				} );
			}
		};

		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if ( user == null ) {
			callback.onFailed();
			return;
		}

		user.getIdToken( true )
				.addOnCompleteListener( task->{
					if ( task.isSuccessful() ) {
						new Thread( ()->getLastBackup( task.getResult().getToken(), callback ) ).start();
					} else {
						callback.onFailed();
					}
				} );

		CloudBackupMaker.getInstance().addBackupCreationCallback( time->
				runOnUiThread( ()->{
					lastBackup = new CloudBackupsListActivity.BackupEntity(
							time,
							null,
							false
					);
					updateState();
				} )
		 );
	}

	private void getLastBackup(String authToken, Callback callback) {
		String url = Utils.DOCUMENTER_API + "backups/getLastBackup?authToken=" + authToken;
		try {
			String result = RequestMaker.getRequestTo( url );
			JSONObject jsonObject = new JSONObject( result );
			JSONObject last = jsonObject.optJSONObject( "backup" );
			if ( last == null ) {
				lastBackup = null;
			} else {
				lastBackup = new CloudBackupsListActivity.BackupEntity(
						last.getLong( "creationTime" ),
						last.optString( "description", null ),
						last.getBoolean( "isManually" )
				);
			}
			runOnUiThread( this::updateState );
			callback.onSuccess();
		} catch (IOException | JSONException e) {
			e.printStackTrace();
			Log.i( TAG, "getLastBackup: " + e );
			callback.onFailed();
		}
	}

	private void updateState() {
		if ( lastBackup == null ) {
			String str = String.format( "%s: %s", getString( R.string.last_backup ), getString( R.string.never ) );
			( (TextView) findViewById( R.id.lblLastBackup ) ).setText( str );
		} else {
			DateFormat format = DateFormat.getDateTimeInstance();
			String creationSource = getString( lastBackup.isManually ? R.string.created_manually : R.string.created_automatically ).toLowerCase();
			String str = String.format( "%s: %s (%s)", getString( R.string.last_backup ), format.format( new Date( lastBackup.time ) ), creationSource );
			( (TextView) findViewById( R.id.lblLastBackup ) ).setText( str );
		}
	}

	@Override
	protected void onPostCreate(@Nullable Bundle savedInstanceState) {
		super.onPostCreate( savedInstanceState );
		TextView t = findViewById( R.id.lblAutoBackupState );
		SharedPreferences sp = getSharedPreferences( Utils.APP_PREFERENCES, MODE_PRIVATE );
		int state = sp.getInt( "auto_backup_state", 0 );
		String[] strings = getResources().getStringArray( R.array.auto_backup_states );
		t.setText( strings[ state ] );
	}

	public void onAutoBackupClick(View v) {
		final TextView t = findViewById( R.id.lblAutoBackupState );
		final SharedPreferences sp = getSharedPreferences( Utils.APP_PREFERENCES, MODE_PRIVATE );
		final String[] strings = getResources().getStringArray( R.array.auto_backup_states );
		AlertDialog.Builder builder = new AlertDialog.Builder( this, super.mAlertDialogStyle )
				.setTitle( R.string.auto_backup )
				.setSingleChoiceItems( strings, sp.getInt( "auto_backup_state", 0 ), (dialog, which)->{
					sp.edit().putInt( "auto_backup_state", which ).apply();
					t.setText( strings[ which ] );
					CloudBackupMaker.getInstance().restartWorker( lastBackup.time );
					dialog.dismiss();
				} );
		builder.create().show();
	}

	public void previousBackups(View v) {
		mCloudBackupsListActivity.launch( new Intent( this, CloudBackupsListActivity.class ) );
	}

	public void createCloudBackup(View v) {
		showEnterDescriptionDialog();
	}

	private void showEnterDescriptionDialog() {
		EditText editText = new EditText( this );
		editText.setTextColor( getColor( super.mTextColor ) );
		editText.setHint( R.string.absent );
		ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT );
		editText.setLayoutParams( layoutParams );
		AlertDialog.Builder builder = new AlertDialog.Builder( this, super.mAlertDialogStyle );
		builder
				.setTitle( R.string.enter_description )
				.setView( editText )
				.setCancelable( false )
				.setNegativeButton( R.string.cancel, (dialog, which)->dialog.cancel() )
				.setPositiveButton( "OK", (dialog, which)->{
					dialog.cancel();
					String text = editText.getText().toString().trim();
					if ( text.isEmpty() ) {
						text = null;
					}
					createBackup( text );
				} );
		AlertDialog alertDialog = builder.create();
		alertDialog.setOnShowListener( dialog->Utils.showKeyboard( editText, this ) );
		alertDialog.show();
	}

	private void createBackup(String description) {
		final ProgressDialog pd = new ProgressDialog( this );
		pd.setMessage( HtmlCompat.fromHtml( getString( R.string.creating_backup ), HtmlCompat.FROM_HTML_MODE_COMPACT ) );
		pd.setCancelable( false );
		pd.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL );
		pd.setIndeterminate( true );

		final BackupInstruments.BackupCallback backupCallback = new BackupInstruments.BackupCallback() {
			@Override
			public void onBackupStateChanged(BackupInstruments.BackupState state) {
				if ( state == BackupInstruments.BackupState.UPLOADING ) {
					runOnUiThread( ()->{
						pd.setIndeterminate( true );
						pd.setMessage( getString( R.string.uploading_backup ) );
						pd.setProgress( 0 );
					} );
				}
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
					lastBackup = new CloudBackupsListActivity.BackupEntity(
							timeOfCreation,
							description,
							true
					);
					updateState();
					pd.dismiss();
					Toast.makeText( CloudBackupActivity.this, R.string.successfully, Toast.LENGTH_SHORT ).show();
				} );
				CloudBackupMaker.getInstance().restartWorker( timeOfCreation );
			}

			@Override
			public void onException(final Exception e) {
				runOnUiThread( ()->{
					pd.dismiss();
					Utils.getErrorDialog( e, CloudBackupActivity.this ).show();
				} );
			}
		};

		pd.show();
		CloudBackupInstruments.createBackup( backupCallback, true, description );
	}

	private void restore(long time) {
		final ProgressDialog pd = new ProgressDialog( this );
		pd.setMessage( getResources().getString( R.string.downloading ) );
		pd.setCancelable( false );
		pd.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL );

		final BackupInstruments.BackupCallback backupCallback = new BackupInstruments.BackupCallback() {
			@Override
			public void onBackupStateChanged(BackupInstruments.BackupState state) {
				if ( state == BackupInstruments.BackupState.UNPACKING ) {
					runOnUiThread( ()->{
						pd.setMessage( getString( R.string.unpacking ) );
						pd.setIndeterminate( true );
						pd.setProgress( 0 );
					} );
				}
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
					setResult( Results.RESTART_APP );
					onBackPressed();
				} );
			}

			@Override
			public void onException(final Exception e) {
				runOnUiThread( ()->{
					pd.dismiss();
					Utils.getErrorDialog( e, CloudBackupActivity.this ).show();
				} );
			}
		};

		pd.show();
		CloudBackupInstruments.restoreFromBackup( backupCallback, time );
	}

	public void restoreFromCloudBackup(View v) {
		if ( lastBackup == null ) {
			return;
		}
		showWarningAndRestore( lastBackup.time );
	}

	private void showWarningAndRestore(long backupTime) {
		AlertDialog.Builder builder = new AlertDialog.Builder( this, super.mAlertDialogStyle )
				.setTitle( R.string.confirmation )
				.setMessage( R.string.confirm_restore_message )
				.setCancelable( false )
				.setPositiveButton( "OK", (dialog, which)->{
					dialog.cancel();
					restore( backupTime );
				} )
				.setNeutralButton( R.string.cancel, (dialog, which)->dialog.cancel() );
		builder.create().show();
	}

}