package com.maxsavitsky.documenter.data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MediaLoader {

	private static MediaLoader instance;

	public static MediaLoader getInstance() {
		if(instance == null)
			instance = new MediaLoader();
		return instance;
	}

	private MediaLoader(){}

	public byte[] loadFileBytesSync(File file) throws IOException {
		FileInputStream fileInputStream = new FileInputStream( file );
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		byte[] buffer = new byte[1024];
		int len;
		while((len = fileInputStream.read(buffer)) != -1){
			outputStream.write( buffer, 0, len );
		}

		return outputStream.toByteArray();
	}
}
