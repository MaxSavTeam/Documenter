package com.maxsavitsky.documenter.media.images;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;

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
