package com.maxsavitsky.documenter.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.maxsavitsky.documenter.MyExceptionHandler;
import com.maxsavitsky.documenter.R;

import java.io.File;

public class ImageRenderer {
	public static Drawable renderDrawable(String source){
		Thread.currentThread().setName( "Drawable renderer thread" );
		String path = source.startsWith( "file://" ) ? source.substring( "file://".length() ) : source;
		File file = new File( path );

		Point size = Utils.getScreenSize();

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options); // get original sizes
		Bitmap b = BitmapFactory.decodeFile( path );
		if(b == null || !file.exists()) {
			Drawable d = Utils.getContext().getDrawable( R.drawable.image_not_found_or_damaged );
			int w = d.getIntrinsicWidth();
			int h = d.getIntrinsicHeight();
			int w1 = size.x;
			int h1 = (w1 * h) / w;
			d.setBounds( 0, 0, w1, h1 );
			String msg = "";
			if(b == null)
				msg += "bitmap render error\n";
			if(!file.exists()){
				msg += "file not found: " + path;
			}
			new MyExceptionHandler( null ).justWriteException( Thread.currentThread(), new Throwable(msg) );
			return d;
		}
		Drawable d = new BitmapDrawable(Utils.getContext().getResources(), b);
		if(options.outWidth > size.x){
			int w = options.outWidth;
			int h = options.outHeight;
			int w1 = size.x;
			int h1 = (w1 * h) / w;
			d.setBounds( 0, 0, w1, h1 );
		}else{
			d.setBounds( 0, 0, options.outWidth, options.outHeight );
		}

		return d;
	}
}
