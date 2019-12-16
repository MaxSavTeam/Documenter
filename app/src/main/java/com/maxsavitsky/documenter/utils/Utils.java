package com.maxsavitsky.documenter.utils;

import android.annotation.SuppressLint;
import android.content.Context;

import com.maxsavitsky.documenter.datatypes.Category;
import com.maxsavitsky.documenter.datatypes.Document;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
				fr.append("<document " + documents.get(i).toString() + " />");
			}
			fr.append("</documents");
			fr.flush();
			fr.close();
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public static void saveCategoryDocuments(String id, ArrayList<Document> documents){
		File file = new File(getContext().getExternalFilesDir(null).getPath() + "/categories/");
		try {
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
				fr.append("<document " + cur.toString() + "/>\n");
			}
			fr.append("</documents>");
			fr.flush();
			fr.close();
		}catch (Exception e){
			e.printStackTrace();
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
}
