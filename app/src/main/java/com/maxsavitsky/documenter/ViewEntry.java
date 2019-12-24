package com.maxsavitsky.documenter;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.datatypes.Element;

import java.util.Objects;
import java.util.Queue;

public class ViewEntry extends AppCompatActivity {

	int backgroundColor;
	int textColor;
	int defaultLeftIndent = 30;
	int width = 1000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );

		Intent intent = getIntent();
		//String uri = intent.getStringExtra( "uri" );
		backgroundColor = intent.getIntExtra( "backgroundColor", Color.WHITE );
		textColor = intent.getIntExtra( "textColor", Color.BLACK );
		WindowManager windowManager = (WindowManager) getSystemService( Context.WINDOW_SERVICE );
		Display display = Objects.requireNonNull( windowManager ).getDefaultDisplay();
		Point point = new Point(  );
		display.getSize( point );
		width = point.x - defaultLeftIndent;
	}
	@SuppressLint("DrawAllocation")
	class Drawer extends View {
		private Context mContext;
		private Queue<Element> mElementQueue;

		public Drawer(Context context, Queue<Element> q) {
			super( context );
			mContext = context;
			mElementQueue = q;
		}

		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawColor( backgroundColor );
			while(!mElementQueue.isEmpty()){
				Element element = mElementQueue.poll();
				if(element == null)
					break;
				if(element.getType().equals( "text" )){
					Spanned spanned = Html.fromHtml(element.getHtmlText());
					TextPaint textPaint = new TextPaint(  );
					textPaint.setAntiAlias( true );
					textPaint.setColor( textColor );
					StaticLayout staticLayout;
					staticLayout = StaticLayout.Builder.obtain( spanned.toString(), 0, spanned.length(), textPaint, width )
							.setAlignment( Layout.Alignment.ALIGN_NORMAL )
							.setIndents( new int[]{defaultLeftIndent}, null )
							.build();
					staticLayout.draw( canvas );
					canvas.save();
					canvas.translate( 0, staticLayout.getHeight() );
					canvas.restore();
				}
			}
		}
	}
}