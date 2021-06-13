package com.maxsavitsky.documenter.backup;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.maxsavitsky.documenter.App;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.net.RequestMaker;
import com.maxsavitsky.documenter.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class CloudBackupMaker {
	private static final String TAG = App.TAG + " CloudBackupMaker";
	private final Context mContext;
	private Timer mTimer;

	@SuppressLint("StaticFieldLeak")
	private static CloudBackupMaker instance = null;

	private final SharedPreferences sp;

	public static CloudBackupMaker getInstance() {
		return instance;
	}

	public CloudBackupMaker(Context context) {
		mContext = context;
		instance = this;
		sp = App.getInstance().getSharedPreferences( Utils.APP_PREFERENCES, Context.MODE_PRIVATE );
	}

	private interface Result<T>{
		void onGet(T result);
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
	 * @param interval   check interval in seconds
	 */
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

		final BackupInstruments.BackupCallback backupCallback = new BackupInstruments.BackupCallback() {
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
		CloudBackupInstruments.createBackup( backupCallback, null );
	}

	synchronized private void checkAndRun() {
		int state = sp.getInt( "auto_backup_state", 0 );
		if ( state != 0 ) {
			long interval = getInterval( state ); // in seconds

			getLastBackupTime( time->checkTime( time, interval ) );
		}
	}

	private void createScheduler() {
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if ( user == null ) {
			return;
		}

		getLastBackupTime( time->{
			long cur = System.currentTimeMillis();
			long r = cur - time;
			int state = sp.getInt( "auto_backup_state", 0 );
			long interval = getInterval( state ) * 1000;
			long delay = 0;
			if ( r < interval ) {
				delay = interval - r;
			}
			mTimer = new Timer();
			Log.i( TAG, "" + interval );
			mTimer.schedule( new MyTimerTask(), delay, interval );
		} );
	}

	private void getLastBackupTime(Result<Long> r) {
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if ( user == null ) {
			return;
		}

		user.getIdToken( true )
				.addOnSuccessListener( getTokenResult->{
					new Thread(()->{
						String url = Utils.DOCUMENTER_API + "backups/getLastBackup?authToken=" + getTokenResult.getToken();
						try {
							String result = RequestMaker.getRequestTo( url );
							JSONObject jsonObject = new JSONObject( result );
							JSONObject last = jsonObject.optJSONObject( "backup" );
							if ( last == null ) {
								return;
							}
							long time = last.getLong( "creationTime" );
							r.onGet( time );
						} catch (IOException | JSONException e) {
							e.printStackTrace();
						}
					}).start();
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
