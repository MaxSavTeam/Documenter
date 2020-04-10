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

import com.maxsavitsky.documenter.MyExceptionHandler;
import com.maxsavitsky.documenter.R;

import java.io.File;

public class HtmlImageLoader implements Html.ImageGetter {
	private final Context c;
	public HtmlImageLoader(Context context) {
		c = context;
	}

	@Override
	public Drawable getDrawable(String source) {
		Thread.currentThread().setName( "HtmlImageLoader thread" );

		return ImageRenderer.renderDrawable( source );
	}
}
