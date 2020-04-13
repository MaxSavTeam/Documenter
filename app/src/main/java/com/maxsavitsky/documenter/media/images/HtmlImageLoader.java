package com.maxsavitsky.documenter.media.images;

import android.graphics.drawable.Drawable;
import android.text.Html;

public class HtmlImageLoader implements Html.ImageGetter {

	@Override
	public Drawable getDrawable(String source) {
		Thread.currentThread().setName( "HtmlImageLoader thread" );

		return ImageRenderer.renderDrawable( source );
	}
}
