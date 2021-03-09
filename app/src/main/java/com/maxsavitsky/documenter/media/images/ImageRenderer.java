package com.maxsavitsky.documenter.media.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.exceptionhandler.ExceptionHandler;

import java.io.File;

public class ImageRenderer {

	public static Drawable renderDrawable(String source){
		return renderDrawable( source, 0 );
	}

	public static Drawable renderDrawable(String source, int width){
		File file = new File( source );

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(source, options); // get original sizes
		Bitmap b = BitmapFactory.decodeFile( source );
		if(b == null || !file.exists()) {
			Drawable d = ContextCompat.getDrawable( Utils.getContext(), R.drawable.image_not_found_or_damaged );
			int w = d.getIntrinsicWidth();
			int h = d.getIntrinsicHeight();
			int h1 = ( width * h) / w;
			d.setBounds( 0, 0, width, h1 );
			String msg = "";
			if(b == null)
				msg += "bitmap render error\n";
			if(!file.exists()){
				msg += "file not found: " + source;
			}
			ExceptionHandler.justWriteException( Thread.currentThread(), new Throwable(msg) );
			return d;
		}
		Drawable d = new BitmapDrawable(Utils.getContext().getResources(), b);
		if(width > 0 && options.outWidth > width){
			int w = options.outWidth;
			int h = options.outHeight;
			int h1 = ( width * h) / w;
			d.setBounds( 0, 0, width, h1 );
		}else{
			d.setBounds( 0, 0, options.outWidth, options.outHeight );
		}

		return d;
	}
}
