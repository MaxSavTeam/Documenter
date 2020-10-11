package com.maxsavitsky.documenter.data.html;

import android.graphics.drawable.Drawable;
import android.text.Html;

import com.maxsavitsky.documenter.media.images.ImageRenderer;

public class HtmlImageLoader implements Html.ImageGetter {

	@Override
	public Drawable getDrawable(String source) {
		Thread.currentThread().setName( "HtmlImageLoader thread" );

		return ImageRenderer.renderDrawable( source );
	}
}
