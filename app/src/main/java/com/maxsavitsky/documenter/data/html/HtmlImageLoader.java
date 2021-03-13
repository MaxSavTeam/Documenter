package com.maxsavitsky.documenter.data.html;

import android.graphics.drawable.Drawable;
import android.text.Html;

import com.maxsavitsky.documenter.media.images.ImageRenderer;

public class HtmlImageLoader implements Html.ImageGetter {

	private int width = 0;

	public HtmlImageLoader(){
		this(0);
	}

	public HtmlImageLoader(int width) {
		this.width = width;
	}

	@Override
	public Drawable getDrawable(String source) {
		return ImageRenderer.renderDrawable( source, width );
	}
}
