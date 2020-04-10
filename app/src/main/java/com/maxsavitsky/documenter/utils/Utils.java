package com.maxsavitsky.documenter.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import com.maxsavitsky.documenter.MainActivity;
import com.maxsavitsky.documenter.MyExceptionHandler;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.types.Category;
import com.maxsavitsky.documenter.data.types.Document;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.data.types.Type;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {
	@SuppressLint("StaticFieldLeak")
	private static Context sContext;
	private static File externalStoragePath;

	public static final String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	public static final String htmlHeader = "<!DOCTYPE html>\n";
	private static final String THIS_TAG = MainActivity.TAG + " Utils";

	public static Context getContext() {
		return sContext;
	}

	public static void setContext(Context context) {
		sContext = context;
		externalStoragePath = context.getApplicationContext().getExternalFilesDir( null );
	}

	public static File getExternalStoragePath() {
		return externalStoragePath;
	}

	/**
	 * This functions checks all entries and all images and removes unused images in this entries
	 * */
	public static void removeAllUnusedImages(){
		Log.i(THIS_TAG, "removing all unused images");
		for(Entry entry : MainData.getEntriesList()){
			ArrayList<String> r = entry.removeUnusedImages();
			for(String s : r){
				Log.i(THIS_TAG, "removed " + s);
			}
		}
	}

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

	public static String readFile(File file) throws IOException {
		String s = "";
		FileInputStream fileInputStream = new FileInputStream( file );
		byte[] buffer = new byte[ 1024 ];
		int len;
		while ( ( len = fileInputStream.read( buffer ) ) != -1 ) {
			if ( len < 1024 ) {
				buffer = Arrays.copyOf( buffer, len );
			}

			s = String.format( "%s%s", s, new String( buffer, StandardCharsets.UTF_8 ) );
		}
		fileInputStream.close();
		return s;
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

	public static void saveCategoryDocuments(String id, ArrayList<Document> documents) {
		try {
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
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		File file = new File( getExternalStoragePath().getPath() + "/documents/" );
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
		return new File( getExternalStoragePath().getPath() + "/temp" );
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

	public static AlertDialog getErrorDialog(Exception e, Context context, boolean silentWriteToFile) {
		if(silentWriteToFile)
			new MyExceptionHandler( null ).justWriteException( Thread.currentThread(), e );

		e.printStackTrace();
		AlertDialog.Builder builder = new AlertDialog.Builder( context ).setTitle( "Error stacktrace" );
		builder.setMessage( Html.fromHtml( "<b>Stacktrace:</b><br><br>" + getExceptionStackTrace( e ) + "<br><br>" + e.getClass().getName() + ": " + e.getMessage() ) )
				.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				} ).setCancelable( false );

		return builder.create();
	}

	public static AlertDialog getErrorDialog(Exception e, Context context){
		return getErrorDialog( e, context, true );
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
}
