package com.maxsavitsky.documenter.backup;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.maxsavitsky.documenter.App;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.utils.Utils;

import java.util.concurrent.TimeUnit;

public class CloudBackupMaker {

	private static final String WORKER_ID = "AutoBackupMaker";

	private static final String TAG = App.TAG + " CloudBackupMaker";
	private final Context context;

	@SuppressLint("StaticFieldLeak")
	private static CloudBackupMaker instance = null;

	private final SharedPreferences sp;

	public static CloudBackupMaker getInstance() {
		return instance;
	}

	public static CloudBackupMaker init(Context context) {
		instance = new CloudBackupMaker( context );
		return instance;
	}

	private CloudBackupMaker(Context context) {
		this.context = context;
		sp = App.getInstance().getSharedPreferences( Utils.APP_PREFERENCES, Context.MODE_PRIVATE );
	}

	private static void createNotificationChannel(NotificationManager manager) {
		if ( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O ) {
			NotificationChannel notificationChannel = new NotificationChannel(
					App.NOTIFICATION_CHANNEL_ID,
					"Documenter",
					NotificationManager.IMPORTANCE_DEFAULT
			);
			manager.createNotificationChannel( notificationChannel );
		}
	}

	private static void showNotificationAboutProgress(NotificationManager manager, int progress, Context context) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder( context, App.NOTIFICATION_CHANNEL_ID );
		builder
				.setContentTitle( context.getString( R.string.creating_backup ) )
				.setSmallIcon( R.drawable.documenter_icon )
				.setOngoing( true );
		if ( progress == -1 ) {
			builder.setProgress( 0, 0, true );
		} else {
			builder.setProgress( 100, progress, false );
		}
		manager.notify( 1, builder.build() );
	}

	public static void doBackup(Context context, BackupInstruments.BackupCallback callback) {
		final NotificationManager manager = context.getSystemService( NotificationManager.class );
		if ( manager == null ) {
			return;
		}
		createNotificationChannel( manager );
		showNotificationAboutProgress( manager, -1, context );

		final BackupInstruments.BackupCallback backupCallback = new BackupInstruments.BackupCallback() {
			@Override
			public void onBackupStateChanged(BackupInstruments.BackupState state) {
				callback.onBackupStateChanged( state );
			}

			@Override
			public void onProgress(int percent) {
				showNotificationAboutProgress( manager, percent, context );
				callback.onProgress( percent );
			}

			@Override
			public void onSuccess(long timeOfCreation) {
				manager.cancelAll();
				callback.onSuccess( timeOfCreation );
			}

			@Override
			public void onException(Exception e) {
				manager.cancelAll();
				e.printStackTrace();
				NotificationCompat.Builder builder = new NotificationCompat.Builder( context, App.NOTIFICATION_CHANNEL_ID );
				builder
						.setContentTitle( context.getString( R.string.backup_error ) )
						.setContentText( context.getString( R.string.backup_error_desc ) )
						.setSmallIcon( R.drawable.documenter_icon );
				manager.notify( 1, builder.build() );
				callback.onException( e );
			}
		};
		CloudBackupInstruments.createBackup( backupCallback, false, null );
	}

	private Pair<Long, TimeUnit> getDurationForWorkManager(int state) {
		if ( state == 1 ) { //Daily
			return Pair.create( 1L, TimeUnit.DAYS );
		} else if ( state == 2 ) { // weekly
			return Pair.create( 7L, TimeUnit.DAYS );
		} else if ( state == 3 ) { // monthly
			return Pair.create( 28L, TimeUnit.DAYS );
		}
		return null;
	}

	public void startWorker() {
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if ( user == null ) {
			return;
		}

		int state = sp.getInt( "auto_backup_state", 0 );
		Pair<Long, TimeUnit> pair = getDurationForWorkManager( state );
		if ( pair == null ) {
			return;
		}
		PeriodicWorkRequest request = new PeriodicWorkRequest.Builder( BackupWorker.class, pair.first, pair.second )
				.build();
		WorkManager
				.getInstance( context )
				.enqueueUniquePeriodicWork( WORKER_ID, ExistingPeriodicWorkPolicy.REPLACE, request );
	}

	public void stopWorker() {
		WorkManager
				.getInstance( context )
				.cancelUniqueWork( WORKER_ID );
	}

	public void restartWorker() {
		stopWorker();
		startWorker();
	}

	public static class BackupWorker extends ListenableWorker {

		/**
		 * @param appContext   The application {@link Context}
		 * @param workerParams Parameters to setup the internal state of this worker
		 */
		public BackupWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
			super( appContext, workerParams );
		}

		@NonNull
		@Override
		public ListenableFuture<Result> startWork() {
			return CallbackToFutureAdapter.getFuture( completer->{
				BackupInstruments.BackupCallback callback = new BackupInstruments.BackupCallback() {
					@Override
					public void onBackupStateChanged(BackupInstruments.BackupState state) {

					}

					@Override
					public void onProgress(int percent) {

					}

					@Override
					public void onSuccess(long timeOfCreation) {
						completer.set( Result.success() );
					}

					@Override
					public void onException(Exception e) {
						completer.setException( e );
					}
				};

				new Thread( ()->doBackup( getApplicationContext(), callback ) ).start();


				return null;
			} );
		}

	}

}
