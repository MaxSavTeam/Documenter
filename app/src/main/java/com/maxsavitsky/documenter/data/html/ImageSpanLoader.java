package com.maxsavitsky.documenter.data.html;

import android.graphics.drawable.Drawable;
import android.text.Html;

import com.maxsavitsky.documenter.App;
import com.maxsavitsky.documenter.media.images.ImageRenderer;

public class ImageSpanLoader implements Html.ImageGetter {

   private final int width;
   private final String entryId;

   public ImageSpanLoader(String entryId, int width) {
      this.width = width;
      this.entryId = entryId;
   }

   @Override
   public Drawable getDrawable(String source) {
      return ImageRenderer.renderDrawable( App.appDataPath + "/entries/" + entryId + "/media/images/" + source, width );
   }
}
