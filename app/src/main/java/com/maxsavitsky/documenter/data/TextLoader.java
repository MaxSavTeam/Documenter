package com.maxsavitsky.documenter.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TextLoader {

	public interface TextLoaderCallback {
		void onLoaded(String data);

		void onException(Exception e);
	}

	private static TextLoader instance;

	public static TextLoader getInstance() {
		if(instance == null)
			instance = new TextLoader();
		return instance;
	}

	public String loadTextSync(File file) throws IOException {
		String text = "";
		FileInputStream fileInputStream = new FileInputStream( file );
		byte[] buffer = new byte[1024];
		int c;
		while(((c = fileInputStream.read(buffer))) != -1){
			if(Thread.currentThread().isInterrupted()){
				throw new IOException("Thread is interrupted");
			}
			if(c < 1024){
				buffer = Arrays.copyOf(buffer, c);
			}

			text = String.format( "%s%s", text, new String( buffer, StandardCharsets.UTF_8 ) );
		}
		fileInputStream.close();
		return text;
	}

	public Thread loadText(File file, TextLoaderCallback callback){
		Thread thread = new Thread(()->{
			try {
				callback.onLoaded( loadTextSync( file ) );
			} catch (IOException e) {
				e.printStackTrace();
				callback.onException( e );
			}
		});
		thread.start();
		return thread;
	}

}
