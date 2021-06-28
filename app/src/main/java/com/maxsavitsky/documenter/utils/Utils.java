package com.maxsavitsky.documenter.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.net.Uri;
import android.os.StrictMode;
import android.text.Html;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.maxsavitsky.documenter.App;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.data.EntitiesStorage;
import com.maxsavitsky.documenter.data.types.Entry;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {
	@SuppressLint("StaticFieldLeak")
	private static Context sContext;
	private static SharedPreferences mDefaultSharedPreferences;

	public static final String APP_PREFERENCES = "main_settings";
	private static final String THIS_TAG = App.TAG + " Utils";

	public static final String DOCUMENTER_API = "https://documenter.maxsavteam.com/api/";

	public static Context getContext() {
		return sContext;
	}

	public static void setContext(Context context) {
		sContext = context;
	}

	public static SharedPreferences getDefaultSharedPreferences() {
		return mDefaultSharedPreferences;
	}

	public static void setDefaultSharedPreferences(SharedPreferences defaultSharedPreferences) {
		mDefaultSharedPreferences = defaultSharedPreferences;
	}

	/**
	 * This functions checks all entries and all images and removes unused images in this entries
	 * */
	public static void removeAllUnusedImages(){
		for(Entry e : EntitiesStorage.get().getEntryEntities()){
			e.removeUnusedImages();
		}
	}

	public static void sendLog(Context context, String path){
		Intent newIntent = new Intent( Intent.ACTION_SEND );
		newIntent.setType( "message/rfc822" );
		newIntent.putExtra( Intent.EXTRA_EMAIL, new String[]{ "maxsavhelp@gmail.com" } );
		newIntent.putExtra( Intent.EXTRA_SUBJECT, "Error in documenter" );
		newIntent.putExtra( Intent.EXTRA_STREAM, Uri.parse( "file://" + path ) );
		newIntent.putExtra( Intent.EXTRA_TEXT, "Log file attached." );
		StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
		StrictMode.setVmPolicy( builder.build() );
		context.startActivity( newIntent );
	}

	public static void sendLog(String path){
		sendLog( sContext, path );
	}

	public static Point getScreenSize(){
		WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		Display display = windowManager.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);

		return size;
	}

	public static void deleteDirectory(File dir) {
		File[] children = dir.listFiles();
		if ( children != null ) {
			for (File file : children) {
				if ( file.isDirectory() ) {
					deleteDirectory( file );
				} else {
					file.delete();
				}
			}
		}
		dir.delete();
	}

	public static byte[] readFileByBytes(File file) throws IOException{
		FileInputStream fis = new FileInputStream(file);
		ByteArrayOutputStream ous = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len;
		while((len = fis.read( buffer )) != -1){
			ous.write( buffer, 0, len );
		}

		return ous.toByteArray();
	}

	public static void copy(File file, File destinationDir) throws IOException {
		File dest = new File( destinationDir, file.getName() );
		if(file.isFile()) {
			if ( !dest.exists() )
				dest.createNewFile();
			try (FileInputStream fis = new FileInputStream( file );
			     FileOutputStream fos = new FileOutputStream( dest )) {
				int len;
				byte[] buffer = new byte[ 4096 ];
				while ( ( len = fis.read( buffer ) ) > 0 ) {
					fos.write( buffer, 0, len );
				}
			}
		}else{
			if(!dest.exists())
				dest.mkdirs();
			File[] files = file.listFiles();
			if(files != null){
				for(File f : files){
					copy(f, dest);
				}
			}
		}
	}

	public static void showKeyboard(EditText editText, Context context) {
		editText.requestFocus();
		InputMethodManager imm = (InputMethodManager) context.getSystemService( Context.INPUT_METHOD_SERVICE );
		if ( imm != null ) {
			imm.toggleSoftInput( InputMethodManager.HIDE_IMPLICIT_ONLY, 0 );
		}
	}

	public static void hideKeyboard(Activity activity) {
		InputMethodManager imm = (InputMethodManager) activity.getSystemService( Activity.INPUT_METHOD_SERVICE );
		View view = activity.getCurrentFocus();
		if ( view == null ) {
			view = new View( activity );
		}
		if ( imm != null ) {
			imm.hideSoftInputFromWindow( view.getWindowToken(), 0 );
		}
	}

	public static String generateUniqueId() {
		String id = "";
		ArrayList<Character> symbols = new ArrayList<>();
		for (int i = 'a'; i <= 'z'; i++) {
			symbols.add( (char) i );
		}
		for (int i = 'A'; i <= 'Z'; i++) {
			symbols.add( (char) i );
		}
		for (int i = '0'; i <= '9'; i++) {
			symbols.add( (char) i );
		}
		Collections.shuffle( symbols );

		for (int i = 0; i < 20; i++) {
			int pos = ThreadLocalRandom.current().nextInt( 0, symbols.size() );
			id = String.format( "%s%c", id, symbols.get( pos ) );
		}

		return id;
	}

	public static File getTempFolder() {
		return new File( App.appStoragePath, "temp" );
	}

	public static void clearTempFolder() {
		File file = getTempFolder();
		File[] files = file.listFiles();
		if ( files != null && files.length > 0 ) {
			for (File child : files) {
				child.delete();
			}
		}
	}

	public static File getEntryImagesMediaFolder(String id) {
		Optional<Entry> op = EntitiesStorage.get().getEntry(id);
		if ( op.isPresent() ) {
			return op.get().getImagesMediaFolder();
		} else {
			File tempFile = getTempFolder();
			if ( !tempFile.exists() ) {
				tempFile.mkdirs();
			}
			return tempFile;
		}
	}

	public static void applyDefaultActionBarStyle(ActionBar actionBar) {
		if ( actionBar != null ) {
			actionBar.setDisplayHomeAsUpEnabled( true );
			actionBar.setHomeAsUpIndicator( R.drawable.ic_arrow_back_white_32dp );
			actionBar.setHomeButtonEnabled( true );
		}
	}

	public static String getThrowableStackTrace(Throwable t) {
		return getStackTrace( t.getStackTrace() );
	}

	public static String getExceptionStackTrace(Exception e) {
		return getStackTrace( e.getStackTrace() );
	}

	@SuppressLint("DefaultLocale")
	public static String getStackTrace(StackTraceElement[] stackTraceElements) {
		String msg = "";
		int limit = stackTraceElements.length;
		for (int i = 0; i < limit; i++) {
			msg = String.format( "%s%d. %s<br>", msg, limit - i, stackTraceElements[ i ] );
		}

		return msg;
	}

	public static AlertDialog getErrorDialog(Exception e, Context context) {
		FirebaseCrashlytics.getInstance().recordException( e );
		FirebaseCrashlytics.getInstance().sendUnsentReports();
		e.printStackTrace();
		AlertDialog.Builder builder = new AlertDialog.Builder( context );
		builder
				.setTitle( "Error stacktrace" )
				.setMessage( Html.fromHtml( e.getClass().getName() + ": " + e.getMessage() + "<br><br><b>Stacktrace:</b><br><br>" + getExceptionStackTrace( e ) ) )
				.setPositiveButton( "OK", (dialog, which)->dialog.cancel() )
				.setCancelable( false );

		return builder.create();
	}
}
