package com.maxsavitsky.documenter.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.view.Display;
import android.view.WindowManager;

import com.maxsavitsky.documenter.R;

import java.io.File;

public class HtmlImageLoader implements Html.ImageGetter {
	private final Context c;
	public HtmlImageLoader(Context context) {
		c = context;
	}

	@Override
	public Drawable getDrawable(String source) {
		String path = source.startsWith( "file://" ) ? source.substring( "file://".length() ) : source;
		File file = new File( path );
		WindowManager windowManager = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		Display display = windowManager.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		Bitmap b = BitmapFactory.decodeFile(path);
		if(b == null || !file.exists()) {
			Drawable d = c.getDrawable( R.drawable.image_not_found_or_damaged );
			int w = d.getIntrinsicWidth();
			int h = d.getIntrinsicHeight();
			int w1 = size.x;
			int h1 = (w1 * h) / w;
			d.setBounds( 0, 0, w1, h1 );
			return d;
		}
		Drawable d = new BitmapDrawable(b);
		if(b.getWidth() > size.x){
			int w = b.getWidth();
			int h = b.getHeight();
			int w1 = size.x;
			int h1 = (w1 * h) / w;
			d.setBounds( 0, 0, w1, h1 );
		}else{
			d.setBounds( 0, 0, b.getWidth(), b.getHeight() );
		}

		return d;
	}
}
