package com.maxsavitsky.documenter.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.net.Uri;
import android.os.StrictMode;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import com.maxsavitsky.documenter.App;
import com.maxsavitsky.documenter.MainActivity;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.data.EntitiesStorage;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.types.Category;
import com.maxsavitsky.documenter.data.types.Document;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.data.types.EntryEntity;
import com.maxsavitsky.documenter.data.types.Type;
import com.maxsavitsky.exceptionhandler.ExceptionHandler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {
	@SuppressLint("StaticFieldLeak")
	private static Context sContext;
	private static File externalStoragePath;
	private static SharedPreferences mDefaultSharedPreferences;

	public static final String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	public static final String htmlHeader = "<!DOCTYPE html>\n";
	public static final String APP_PREFERENCES = "main_settings";
	private static final String THIS_TAG = MainActivity.TAG + " Utils";

	public static Context getContext() {
		return sContext;
	}

	public static void setContext(Context context) {
		sContext = context;
		externalStoragePath = context.getApplicationContext().getExternalFilesDir( null );
	}

	public static SharedPreferences getDefaultSharedPreferences() {
		return mDefaultSharedPreferences;
	}

	public static void setDefaultSharedPreferences(SharedPreferences defaultSharedPreferences) {
		mDefaultSharedPreferences = defaultSharedPreferences;
	}

	public static File getExternalStoragePath() {
		return externalStoragePath;
	}

	/**
	 * This functions checks all entries and all images and removes unused images in this entries
	 * */
	public static void removeAllUnusedImages(){
		Log.i(THIS_TAG, "removing all unused images");
		for(EntryEntity e : EntitiesStorage.get().getEntryEntities()){
			ArrayList<String> r = e.removeUnusedImages();
			for(String s : r){
				Log.i(THIS_TAG, "removed " + s);
			}
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

	/*public static  <T> void removeAllSpansInBounds(int selSt, int selEnd, Class<T> type, Spannable e){
		if(e == null)
			return;

		ArrayList<SpanEntry<T>> arrayList = new ArrayList<>();
		T[] spans = e.getSpans( selSt, selEnd, type );
		for(T span : spans){
			int st = e.getSpanStart( span );
			int end = e.getSpanEnd( span );
			if(st < selSt){
				arrayList.add( new SpanEntry<T>( span, st, selSt ) );
			}
			if(end > selEnd){
				arrayList.add( new SpanEntry<T>( span, selEnd, end ) );
			}
			e.removeSpan( span );
		}
		for(SpanEntry<T> se : arrayList){
			e.setSpan( se.getSpan(), se.getStart(), se.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE );
		}
	}*/

	/**
	 * Check if category, document or entry exists
	 *
	 * @param type can be of three types: cat, doc or ent
	 */
	public static boolean isNameExist(String name, String type) {
		name = name.trim();
		ArrayList<? extends Type> categories = type.equals( "cat" ) ? MainData.getCategoriesList() :
				(
						type.equals( "doc" ) ? MainData.getDocumentsList() : MainData.getEntriesList()
				);
		for (Type category : categories) {
			if ( name.equals( category.getName() ) ) {
				return true;
			}
		}

		return false;
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

	public static void saveCategoriesList(ArrayList<Category> categories) {
		try {
			File file = new File( getContext().getExternalFilesDir( null ).getPath() + "/categories.xml" );
			FileWriter fr = new FileWriter( file, false );
			fr.write( xmlHeader );
			fr.append( "<categories>\n" );
			for (int i = 0; i < categories.size(); i++) {
				fr.append( "\t<category " + categories.get( i ).toString() + "/>\n" );
			}
			fr.append( "</categories>" );
			fr.flush();
			fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void saveDocumentsList(ArrayList<Document> documents) {
		try {
			File file = new File( getContext().getExternalFilesDir( null ).getPath() + "/documents.xml" );
			FileWriter fr = new FileWriter( file, false );
			fr.write( xmlHeader );
			fr.append( "<documents>\n" );
			for (int i = 0; i < documents.size(); i++) {
				fr.append( "\t<document " + documents.get( i ).toString() + " />\n" );
			}
			fr.append( "</documents>" );
			fr.flush();
			fr.close();
		} catch (Exception e) {
			e.printStackTrace();
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

	public static void saveEntriesList(ArrayList<Entry> entries) {
		try {
			File file = new File( getExternalStoragePath().getPath() + "/entries.xml" );
			if ( !file.exists() ) {
				file.createNewFile();
			}
			FileWriter fr = new FileWriter( file, false );
			fr.write( xmlHeader );
			fr.append( "<entries>\n" );
			for (Entry entry : entries) {
				fr.append( "\t<entry " + entry.toString() + " />\n" );
			}
			fr.append( "</entries>" );
			fr.flush();
			fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void saveCategoryDocuments(String id, ArrayList<Document> documents) throws IOException {
		File file = new File( getExternalStoragePath().getPath() + "/categories/" );
		if ( !file.exists() ) {
			file.mkdir();
		}
		file = new File( file.getPath() + "/" + id );
		if ( !file.exists() ) {
			file.mkdir();
		}
		file = new File( file.getPath() + "/" + id + ".xml" );
		if ( !file.exists() ) {
			file.createNewFile();
		}
		FileWriter fr = new FileWriter( file, false );
		fr.write( xmlHeader );
		fr.append( "<documents>\n" );
		for (int i = 0; i < documents.size(); i++) {
			Document cur = documents.get( i );
			fr.append( "\t<document " + "id=\"" + cur.getId() + "\" />\n" );
		}
		fr.append( "</documents>" );
		fr.flush();
		fr.close();
	}

	public static void saveDocumentEntries(String id, ArrayList<Entry> entries) {
		File file = new File( getExternalStoragePath().getPath() + "/documents/" );
		try {
			if ( !file.exists() ) {
				file.mkdir();
			}
			file = new File( file.getPath() + "/" + id );
			if ( !file.exists() ) {
				file.mkdir();
			}
			file = new File( file.getPath() + "/" + id + ".xml" );
			if ( !file.exists() ) {
				file.createNewFile();
			}
			FileWriter fr = new FileWriter( file, false );
			fr.write( xmlHeader );
			fr.append( "<entries>\n" );
			for (Entry entry : entries) {
				fr.append( "\t<entry id=\"" + entry.getId() + "\" />\n" );
			}
			fr.append( "</entries>" );
			fr.flush();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void saveInWhichCategoriesDocumentWithIdIncludedIn(String id, ArrayList<Category> categories) throws IOException {
		MainData.getDocumentWithId( id ).saveInWhichCategoriesDocumentWithIdIncludedIn( categories );
	}

	public static void createAllNecessaryForDocument(String id) throws IOException {
		File file = new File( getExternalStoragePath().getPath() + "/documents/" + id );
		if ( !file.exists() ) {
			file.mkdirs();
		}
		file = new File( file.getPath() + "/" + id + ".xml" );
		if ( !file.exists() ) {
			file.createNewFile();
			FileWriter fr = new FileWriter( file, false );
			fr.write( xmlHeader );
			fr.append( "<entries>\n</entries>" );
			fr.flush();
			fr.close();
		}
		file = new File( getExternalStoragePath().getPath() + "/documents/" + id + "/included_in.xml" );
		if ( !file.exists() ) {
			file.createNewFile();
			FileWriter fr = new FileWriter( file, false );
			fr.write( xmlHeader );
			fr.append( "<categories>\n</categories>" );
			fr.flush();
			fr.close();
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
		if ( MainData.isExists( id ) ) {
			return MainData.getEntryWithId( id ).getImagesMediaFolder();
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
		int limit = Math.min( 10, stackTraceElements.length );
		for (int i = 0; i < limit; i++) {
			msg = String.format( "%s%d. %s<br>", msg, limit - i, stackTraceElements[ i ] );
		}

		return msg;
	}

	public static AlertDialog getErrorDialog(Exception e, Context context, boolean silentWriteToFile, boolean showSendLogButton) {
		File stacktraceFile = null;
		if(showSendLogButton){
			stacktraceFile = ExceptionHandler.prepareLogToSend( Thread.currentThread(), e );
		}else {
			if ( silentWriteToFile )
				ExceptionHandler.justWriteException( Thread.currentThread(), e );
		}

		e.printStackTrace();
		AlertDialog.Builder builder = new AlertDialog.Builder( context ).setTitle( "Error stacktrace" );
		builder.setMessage( Html.fromHtml( e.getClass().getName() + ": " + e.getMessage() + "<br><br><b>Stacktrace:</b><br><br>" + getExceptionStackTrace( e ) ) )
				.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				} ).setCancelable( false );
		if(showSendLogButton){
			final File finalStacktraceFile = stacktraceFile;
			builder.setNeutralButton( R.string.send_report, (dialog, which)->sendLog( finalStacktraceFile.getPath() ) );
		}

		return builder.create();
	}

	public static AlertDialog getErrorDialog(Exception e, Context context){
		return getErrorDialog( e, context, true, true );
	}

	public static AlertDialog getErrorDialog(Throwable t, Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder( context ).setTitle( "Error stacktrace" );
		builder.setMessage( Html.fromHtml( "<b>Stacktrace:</b><br><br>" + getThrowableStackTrace( t ) + "<br><br>" + t.getMessage() ) )
				.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				} ).setCancelable( false );

		return builder.create();
	}

	public static Comparator<? super Type> getSortByNamesComparator(){
		return new Comparator<Type>() {
			@Override
			public int compare(Type o1, Type o2) {
				return o1.getName().compareToIgnoreCase( o2.getName() );
			}
		};
	}
}
