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

public class HtmlImageLoader implements Html.ImageGetter {
	private Context c;
	public HtmlImageLoader(Context context) {
		c = context;
	}

	@Override
	public Drawable getDrawable(String source) {
		String path = source.startsWith( "file://" ) ? source.substring( "file://".length() ) : source;
		Bitmap b = BitmapFactory.decodeFile(path);
		Drawable d = new BitmapDrawable(b);
		WindowManager windowManager = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		Display display = windowManager.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
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
