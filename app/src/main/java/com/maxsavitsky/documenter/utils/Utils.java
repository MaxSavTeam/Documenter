package com.maxsavitsky.documenter.utils;

import android.annotation.SuppressLint;
import android.content.Context;

import com.maxsavitsky.documenter.datatypes.Category;
import com.maxsavitsky.documenter.datatypes.Document;
import com.maxsavitsky.documenter.datatypes.Entry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {
	@SuppressLint("StaticFieldLeak")
	private static Context sContext;
	private static File externalStoragePath;

	public static String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

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

	public static void saveCategoryDocuments(String id, ArrayList<Document> documents){
		File file = new File(getExternalStoragePath().getPath() + "/categories/");
		try {
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
		}catch (Exception e){
			e.printStackTrace();
		}
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
			fr.append( "</entries" );
			fr.flush();
			fr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void saveInWhichCategoriesDocumentWithIdIncludedIn(String id, ArrayList<Category> categories) throws IOException {
		File file = new File( getExternalStoragePath().getPath() + "/documents/" + id );
		if(!file.exists()){
			file.mkdir();
		}
		file = new File( file.getPath() + "/included_in.xml" );
		if(!file.exists())
			file.createNewFile();

		FileWriter fr = new FileWriter( file, false );
		fr.write( xmlHeader );
		fr.append( "<categories>\n" );
		for(Category category : categories){
			fr.append( "<category id=\"" + category.getId() + "\" />\n" );
		}
		fr.append( "</category>" );
		fr.flush();
		fr.close();
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
}
