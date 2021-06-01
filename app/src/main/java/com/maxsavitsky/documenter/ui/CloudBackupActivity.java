package com.maxsavitsky.documenter.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.ThemeActivity;
import com.maxsavitsky.documenter.backup.BackupInterface;
import com.maxsavitsky.documenter.backup.CloudBackupInstruments;
import com.maxsavitsky.documenter.backup.CloudBackupMaker;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.utils.Utils;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

public class CloudBackupActivity extends ThemeActivity {
	private long mLastBackupTime;

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

	private interface Loader {
		void onDataLoaded();

		void failed();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_cloud_backup );
		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );

		applyTheme();

		( (TextView) findViewById( R.id.lblCloud1 ) ).setText( Html.fromHtml( getString( R.string.cloud_backup_activity_text1 ) ) );

		final ProgressDialog pd = new ProgressDialog( this );
		pd.setMessage( getString( R.string.loading ) );
		pd.setButton( ProgressDialog.BUTTON_NEUTRAL, getString( R.string.cancel ), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				onBackPressed();
			}
		} );
		pd.show();
		final Loader loader = new Loader() {
			@Override
			public void onDataLoaded() {
				pd.dismiss();
			}

			@Override
			public void failed() {
				pd.dismiss();
				Toast.makeText( CloudBackupActivity.this, R.string.something_gone_wrong, Toast.LENGTH_SHORT ).show();
				onBackPressed();
			}
		};

		Thread loadThread = new Thread( new Runnable() {
			@Override
			public void run() {
				FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
				if ( user == null ) {
					loader.failed();
					return;
				}
				DatabaseReference ref = FirebaseDatabase.getInstance().getReference( "documenter/" + user.getUid() + "/last_backup_time" );
				ref.addValueEventListener( new ValueEventListener() {
					@Override
					public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
						if ( dataSnapshot.getValue() != null ) {
							long time = (long) dataSnapshot.getValue();
							mLastBackupTime = time;
							DateFormat format = DateFormat.getDateTimeInstance();
							String str = String.format( "%s: %s", getString( R.string.last_backup ), format.format( new Date( time ) ) );
							( (TextView) findViewById( R.id.lblLastBackup ) ).setText( str );
						} else {
							String str = String.format( "%s: %s", getString( R.string.last_backup ), getString( R.string.never ) );
							( (TextView) findViewById( R.id.lblLastBackup ) ).setText( str );
							mLastBackupTime = -1;
						}
						loader.onDataLoaded();
					}

					@Override
					public void onCancelled(@NonNull DatabaseError databaseError) {
						loader.failed();
					}
				} );
			}
		} );
		loadThread.start();
	}

	@Override
	protected void onPostCreate(@Nullable Bundle savedInstanceState) {
		super.onPostCreate( savedInstanceState );
		TextView t = findViewById( R.id.lblAutoBackupState );
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
		int state = sp.getInt( "auto_backup_state", 0 );
		String[] strings = getResources().getStringArray( R.array.auto_backup_states );
		t.setText( strings[ state ] );
	}

	public void onAutoBackupClick(View v) {
		final TextView t = findViewById( R.id.lblAutoBackupState );
		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
		final String[] strings = getResources().getStringArray( R.array.auto_backup_states );
		AlertDialog.Builder builder = new AlertDialog.Builder( this, super.mAlertDialogStyle )
				.setTitle( R.string.auto_backup )
				.setSingleChoiceItems( strings, sp.getInt( "auto_backup_state", 0 ), (dialog, which)->{
					sp.edit().putInt( "auto_backup_state", which ).apply();
					t.setText( strings[ which ] );
					CloudBackupMaker.getInstance().stateChanged();
					dialog.dismiss();
				} );
		builder.create().show();
	}

	public void createCloudBackup(View v) {
		final ProgressDialog pd = new ProgressDialog( this );
		pd.setMessage( Html.fromHtml( getString( R.string.creating_backup ) ) );
		//pd.setMessage( "Preparing..." );
		pd.setCancelable( false );

		final BackupInterface backupInterface = new BackupInterface() {
			@Override
			public void onSuccess(long timeOfCreation) {
				runOnUiThread( ()->{
					pd.dismiss();
					Toast.makeText( CloudBackupActivity.this, R.string.successfully, Toast.LENGTH_SHORT ).show();
				} );
				CloudBackupMaker.getInstance().stateChanged();
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
		new Thread( ()->{
			try {
				long time = System.currentTimeMillis();
				CloudBackupInstruments.createBackup( backupInterface, "backup_" + time, time );
			} catch (IOException e) {
				e.printStackTrace();
				backupInterface.onException( e );
			}
		} ).start();
	}

	private void restore() {
		final ProgressDialog pd = new ProgressDialog( this );
		pd.setMessage( getResources().getString( R.string.loading ) );
		//pd.setMessage( "Preparing..." );
		pd.setCancelable( false );

		final BackupInterface backupInterface = new BackupInterface() {
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
		new Thread( new Runnable() {
			@Override
			public void run() {
				try {
					CloudBackupInstruments.restoreFromBackup( backupInterface, "backup_" + mLastBackupTime );
				} catch (IOException e) {
					e.printStackTrace();
					backupInterface.onException( e );
				}
			}
		} ).start();
	}

	public void restoreFromCloudBackup(View v) {
		if(mLastBackupTime == -1)
			return;
		androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder( this )
				.setTitle( R.string.confirmation )
				.setMessage( R.string.confirm_restore_message )
				.setCancelable( false )
				.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						restore();
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
}