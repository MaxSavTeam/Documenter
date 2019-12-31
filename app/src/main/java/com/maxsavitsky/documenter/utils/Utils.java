package com.maxsavitsky.documenter.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.text.Html;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.datatypes.Category;
import com.maxsavitsky.documenter.datatypes.Document;
import com.maxsavitsky.documenter.datatypes.Entry;
import com.maxsavitsky.documenter.datatypes.MainData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {
	@SuppressLint("StaticFieldLeak")
	private static Context sContext;
	private static File externalStoragePath;

	public static final String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	public static final String htmlHeader = "<!DOCTYPE html>\n";

	public static Context getContext() {
		return sContext;
	}

	public static void setContext(Context context) {
		sContext = context;
		externalStoragePath = context.getExternalFilesDir(null);
	}

	public static File getExternalStoragePath() {
		return externalStoragePath;
	}

	public static void saveCategoriesList(ArrayList<Category> categories){
		try{
			File file = new File(getContext().getExternalFilesDir(null).getPath() + "/categories.xml");
			FileWriter fr = new FileWriter(file, false);
			fr.write(xmlHeader);
			fr.append("<categories>\n");
			for(int i = 0; i < categories.size(); i++){
				fr.append("<category " + categories.get(i).toString() +"/>\n");
			}
			fr.append("</categories>");
			fr.flush();
			fr.close();
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public static void saveDocumentsList(ArrayList<Document> documents){
		try{
			File file = new File(getContext().getExternalFilesDir(null).getPath() + "/documents.xml");
			FileWriter fr = new FileWriter(file, false);
			fr.write(xmlHeader);
			fr.append("<documents>\n");
			for(int i = 0; i < documents.size(); i++){
				fr.append("<document " + documents.get(i).toString() + " />\n");
			}
			fr.append("</documents>");
			fr.flush();
			fr.close();
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public static void showKeyboard(EditText editText, Context context){
		editText.requestFocus();
		InputMethodManager imm = (InputMethodManager) context.getSystemService( Context.INPUT_METHOD_SERVICE );
		if ( imm != null ) imm.toggleSoftInput( InputMethodManager.HIDE_IMPLICIT_ONLY, 0 );
	}

	public static void saveEntriesList(ArrayList<Entry> entries){
		try{
			File file = new File( getExternalStoragePath().getPath() + "/entries.xml" );
			if(!file.exists()){
				file.createNewFile();
			}
			FileWriter fr = new FileWriter( file, false );
			fr.write( xmlHeader );
			fr.append( "<entries>\n" );
			for(Entry entry : entries){
				fr.append( "<entry " + entry.toString() + " />\n" );
			}
			fr.append( "</entries>" );
			fr.flush();
			fr.close();
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public static void saveCategoryDocuments(String id, ArrayList<Document> documents) throws Exception{
		File file = new File(getExternalStoragePath().getPath() + "/categories/");
		if(!file.exists()){
			file.mkdir();
		}
		file = new File(file.getPath() + "/" + id);
		if(!file.exists()){
			file.mkdir();
		}
		file = new File(file.getPath() + "/" + id + ".xml");
		if (!file.exists()) {
			file.createNewFile();
		}
		FileWriter fr = new FileWriter(file, false);
		fr.write(xmlHeader);
		fr.append("<documents>\n");
		for(int i = 0; i < documents.size(); i++){
			Document cur = documents.get(i);
			fr.append("<document " + "id=\"" + cur.getId() + "\" />\n");
		}
		fr.append("</documents>");
		fr.flush();
		fr.close();
	}

	public static void saveDocumentEntries(String id, ArrayList<Entry> entries){
		File file = new File(getExternalStoragePath().getPath() + "/documents/");
		try{
			if(!file.exists())
				file.mkdir();
			file = new File( file.getPath() + "/" + id );
			if(!file.exists())
				file.mkdir();
			file = new File( file.getPath() + "/" + id + ".xml" );
			if(!file.exists())
				file.createNewFile();
			FileWriter fr = new FileWriter( file, false );
			fr.write( xmlHeader );
			fr.append( "<entries>\n" );
			for(Entry entry : entries){
				fr.append( "<entry id=\"" + entry.getId() + "\" />\n" );
			}
			fr.append( "</entries>" );
			fr.flush();
			fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void saveInWhichCategoriesDocumentWithIdIncludedIn(String id, ArrayList<Category> categories) throws IOException {
		MainData.getDocumentWithId( id ).saveInWhichCategoriesDocumentWithIdIncludedIn( categories );
	}

	public static void createAllNecessaryForDocument(String id) throws IOException {
		File file = new File( getExternalStoragePath().getPath() + "/documents/" );
		if(!file.exists()){
			file.mkdir();
		}
		file = new File( file.getPath() + "/" + id );
		if(!file.exists())
			file.mkdir();
		file = new File(file.getPath() + "/" + id + ".xml");
		if(!file.exists()){
			file.createNewFile();
			FileWriter fr = new FileWriter( file, false );
			fr.write( xmlHeader );
			fr.append( "<entries>\n</entries>" );
			fr.flush();
			fr.close();
		}
		file = new File( getExternalStoragePath().getPath() + "/documents/" + id + "/included_in.xml" );
		if(!file.exists()){
			file.createNewFile();
			FileWriter fr = new FileWriter( file, false );
			fr.write( xmlHeader );
			fr.append( "<categories>\n</categories>" );
			fr.flush();
			fr.close();
		}
	}

	public static String generateUniqueId(){
		String id = "";
		ArrayList<Character> symbols = new ArrayList<>();
		for(int i = 'a'; i <= 'z'; i++){
			symbols.add((char) i);
		}
		for(int i = 'A'; i <= 'Z'; i++){
			symbols.add((char) i);
		}
		for(int i = '0'; i <= '9'; i++){
			symbols.add((char) i);
		}
		Collections.shuffle(symbols);

		for(int i = 0; i < 20; i++){
			int pos = ThreadLocalRandom.current().nextInt(0, symbols.size());
			id = String.format("%s%c", id, symbols.get(pos));
		}

		return id;
	}

	public static void applyDefaultActionBarStyle(ActionBar actionBar){
		if(actionBar != null){
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeAsUpIndicator( R.drawable.ic_arrow_back_white_32dp);
			actionBar.setHomeButtonEnabled(true);
			actionBar.setBackgroundDrawable( new ColorDrawable( getContext().getResources().getColor( R.color.colorPrimary ) ) );
		}
	}

	public static String getThrowableStackTrace(Throwable t){
		return getStackTrace( t.getStackTrace() );
	}

	public static String getExceptionStackTrace(Exception e){
		return getStackTrace( e.getStackTrace() );
	}

	@SuppressLint("DefaultLocale")
	public static String getStackTrace(StackTraceElement[] stackTraceElements){
		String msg = "";

		for(int i = 0; i < Math.min( 5, stackTraceElements.length ); i++){
			msg = String.format( "%s%d. %s<br>", msg, 5 - i, stackTraceElements[i]);
		}

		return msg;
	}

	public static AlertDialog getErrorDialog(Exception e, Context context){
		e.printStackTrace();
		AlertDialog.Builder builder = new AlertDialog.Builder( context ).setTitle( "Error stacktrace" );
		builder.setMessage( Html.fromHtml( "<b>Stacktrace:</b><br><br>" + getExceptionStackTrace( e ) + "<br><br>" + e.getMessage()) )
			.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			} ).setCancelable( false );

		return builder.create();
	}

	public static AlertDialog getErrorDialog(Throwable t, Context context){
		AlertDialog.Builder builder = new AlertDialog.Builder( context ).setTitle( "Error stacktrace" );
		builder.setMessage( Html.fromHtml( "<b>Stacktrace:</b><br><br>" + getThrowableStackTrace( t ) + "<br><br>" + t.getMessage()) )
				.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				} ).setCancelable( false );

		return builder.create();
	}
}
