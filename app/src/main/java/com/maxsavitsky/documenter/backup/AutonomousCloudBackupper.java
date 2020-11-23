package com.maxsavitsky.documenter.backup;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.maxsavitsky.documenter.R;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class AutonomousCloudBackupper {
	private Context mContext;
	private Timer mTimer;

	@SuppressLint("StaticFieldLeak")
	private static AutonomousCloudBackupper instance = null;

	public static AutonomousCloudBackupper getInstance() {
		return instance;
	}

	public AutonomousCloudBackupper(Context context) {
		mContext = context;
		instance = this;
	}

	private long getInterval(int state) {
		long interval = (long) 1e9;
		if ( state == 1 ) { //Daily
			interval = 60 * 60 * 24;
		} else if ( state == 2 ) { // weekly
			interval = 60 * 60 * 24 * 7;
		} else if ( state == 3 ) { // monthly
			interval = 60 * 60 * 24 * 7 * 4;
		} else if ( state == 4 ) { // every minute
			interval = 60;
		}
		return interval;
	}

	/**
	 * @param lastBackup time of last backup in millis
	 * @param interval check interval in seconds
	 * */
	private void checkTime(long lastBackup, final long interval) {
		final long cur = System.currentTimeMillis() / 1000;
		lastBackup /= 1000;
		long r = Math.abs( cur - lastBackup );
		if ( r + 5 >= interval ) {
			doBackup();
		}
	}

	private void doBackup() {
		final NotificationManager manager = (NotificationManager) mContext.getSystemService( Context.NOTIFICATION_SERVICE );
		if ( manager == null ) {
			return;
		}
		NotificationCompat.Builder builder = new NotificationCompat.Builder( mContext, "documenter_app_channel" );
		builder.setContentTitle( mContext.getString( R.string.last_backup ) )
				.setContentText( mContext.getString( R.string.creating_backup ) )
				.setSmallIcon( R.drawable.documenter_icon )
				.setOngoing( true )
				.setProgress( 0, 0, true );

		final BackupInterface backupInterface = new BackupInterface() {
			@Override
			public void onSuccess(long timeOfCreation) {
				manager.cancelAll();
			}

			@Override
			public void onException(Exception e) {
				manager.cancelAll();
				e.printStackTrace();
				new Handler( Looper.getMainLooper() ).post( ()->Toast.makeText( mContext, R.string.backup_error, Toast.LENGTH_LONG ).show() );
			}
		};
		new Thread( new Runnable() {
			@Override
			public void run() {
				try {
					long time = System.currentTimeMillis();
					CloudBackupInstruments.createBackup( backupInterface, "backup_" + time, time );
				} catch (IOException e) {
					e.printStackTrace();
					backupInterface.onException( e );
				}
			}
		} ).start();
	}

	synchronized private void checkAndRun() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences( mContext.getApplicationContext() );
		int state = sp.getInt( "auto_backup_state", 0 );
		if ( state != 0 ) {
			long interval = getInterval( state ); // in seconds

			FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
			if ( user == null ) {
				return;
			}
			DatabaseReference ref = FirebaseDatabase.getInstance().getReference( "documenter/" + user.getUid() + "/last_backup_time" );
			final long finalInterval = interval;
			ref.addListenerForSingleValueEvent( new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
					long lastTime = (long) dataSnapshot.getValue();
					checkTime( lastTime, finalInterval );
				}

				@Override
				public void onCancelled(@NonNull DatabaseError databaseError) {

				}
			} );
		}
	}

	private void createScheduler() {
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if ( user == null ) {
			return;
		}
		DatabaseReference ref = FirebaseDatabase.getInstance().getReference( "documenter/" + user.getUid() + "/last_backup_time" );
		ref.addListenerForSingleValueEvent( new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
				if(dataSnapshot.getValue() == null)
					return;
				long time = (long) dataSnapshot.getValue();
				long cur = System.currentTimeMillis();
				long r = cur - time;
				int state = PreferenceManager.getDefaultSharedPreferences( mContext.getApplicationContext() ).getInt( "auto_backup_state", 0 );
				long interval = getInterval( state ) * 1000;
				long delay = 0;
				if ( r < interval ) {
					delay = interval - r;
				}
				mTimer = new Timer();
				mTimer.schedule( new MyTimerTask(), delay, interval );
			}

			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {

			}
		} );
	}

	synchronized public void stateChanged() {
		if ( mTimer != null ) {
			mTimer.cancel();
			mTimer = null;
		}
		createScheduler();
	}

	private class MyTimerTask extends TimerTask {
		@Override
		public void run() {
			checkAndRun();
		}
	}
}
