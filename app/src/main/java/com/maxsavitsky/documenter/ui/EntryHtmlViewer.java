package com.maxsavitsky.documenter.ui;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.maxsavitsky.documenter.MainActivity;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.ThemeActivity;
import com.maxsavitsky.documenter.codes.Requests;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.MediaLoader;
import com.maxsavitsky.documenter.data.TextLoader;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.utils.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class EntryHtmlViewer extends ThemeActivity {
	private static final String TAG = MainActivity.TAG + " EntryHtml";

	private WebView mWebView;
	private Entry mEntry;
	private boolean isFreeMode = false;

	private class WebInterface {

		@JavascriptInterface
		public void openSrc(String s) {
			Toast.makeText( EntryHtmlViewer.this, "Clicked", Toast.LENGTH_SHORT ).show();
		}

	}

	@Override
	protected void onDestroy() {
		mWebView.destroy();
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		mEntry.getProperties().setScrollPosition( mWebView.getScrollY() );
		try {
			mEntry.saveProperties();
		} catch (IOException e) {
			Utils.getErrorDialog( e, this ).show();
		}
		super.onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if(menu != null)
			menu.clear();
		getMenuInflater().inflate( R.menu.entry_menu, menu );
		if(!isFreeMode) {
			getMenuInflater().inflate( R.menu.common_menu, menu );
			MenuItem item = menu.findItem( R.id.item_common_remember_pos );
			item.setChecked( mEntry.getProperties().isSaveLastPos() );
			item.setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					boolean isChecked = !item.isChecked();
					item.setChecked( isChecked );
					try {
						mEntry.applySaveLastPos( isChecked );
					} catch (final IOException | SAXException e) {
						e.printStackTrace();
						runOnUiThread( ()->Utils.getErrorDialog( e, EntryHtmlViewer.this ).show() );
					}

					return true;
				}
			} );
		}
		return super.onCreateOptionsMenu( menu );
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int itemId = item.getItemId();
		if ( itemId == android.R.id.home ) {
			onBackPressed();
		} else if ( itemId == R.id.item_delete_entry ) {
			AlertDialog.Builder deletionBuilder = new AlertDialog.Builder( this )
					.setMessage( R.string.delete_confirmation_text )
					.setTitle( R.string.confirmation )
					.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							try {
								if ( MainData.finallyDeleteEntryWithId( mEntry.getId() ) ) {
									setResult( Results.NEED_TO_REFRESH );
									finish();
								} else {
									Toast.makeText( EntryHtmlViewer.this, "Failed", Toast.LENGTH_SHORT ).show();
								}
							} catch (Exception e) {
								e.printStackTrace();
								Toast.makeText( EntryHtmlViewer.this, "onOptionsItemSelected", Toast.LENGTH_LONG ).show();
								Utils.getErrorDialog( e, EntryHtmlViewer.this ).show();
							}
						}
					} ).setNeutralButton( R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					} ).setCancelable( false );
			deletionBuilder.create().show();
		} else if ( itemId == R.id.item_edit_entry_text ) {
			mEntry.getProperties().setScrollPosition( mWebView.getScrollY() );
			try {
				mEntry.saveProperties( mEntry.getProperties() );
			} catch (Exception e) {
				e.printStackTrace();
			}
			Intent intent = new Intent( this, EntryEditor.class );
			intent.putExtra( "type", "edit" );
			intent.putExtra( "id", mEntry.getId() );
			intent.putExtra( "scroll_position", mWebView.getScrollY() );
			startActivityForResult( intent, Requests.EDIT_ENTRY );
		}
		return super.onOptionsItemSelected( item );
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_entry_html_viewer );


		WebView.setWebContentsDebuggingEnabled( true );

		isFreeMode = getIntent().getBooleanExtra("free_mode", false);

		mEntry = MainData.getEntryWithId( getIntent().getStringExtra( "id" ) );
		Log.i( TAG, "onCreate: entry id=" + mEntry.getId() );
		try {
			mEntry.readProperties();
		} catch (IOException | SAXException e) {
			e.printStackTrace();
			Log.i( TAG, "onCreate: readProperties failed: " + e );
		}

		Toolbar toolbar = findViewById( R.id.toolbar );
		toolbar.setTitle( mEntry.getName() );
		setSupportActionBar( toolbar );

		mWebView = findViewById( R.id.webView );
		WebSettings settings = mWebView.getSettings();
		settings.setJavaScriptEnabled( true );
		settings.setDomStorageEnabled( true );
		settings.setAllowFileAccess( true );
		settings.setAllowFileAccessFromFileURLs( true );
		settings.setLoadsImagesAutomatically( true );
		settings.setUseWideViewPort( true );
		settings.setMixedContentMode( WebSettings.MIXED_CONTENT_ALWAYS_ALLOW );
		mWebView.addJavascriptInterface( new WebInterface(), "Android" );

		mWebView.setWebViewClient( new WebViewClient() {
			@Override
			public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
				Log.i( TAG, "onReceivedError: " + error.getDescription() );
				Toast.makeText( EntryHtmlViewer.this, error.getDescription(), Toast.LENGTH_LONG ).show();
			}
		} );


		/*mWebView.loadData( "<html>\n" +
				"     <head>\n" +
				"     </head>\n" +
				"     <body bgcolor=\"fffffb00\">\n" +
				"      <img src=\"file:///storage/emulated/0/Android/data/com.maxsavitsky.documenter/files/entries/fI65KkiHa0mrHldw18mF_ent/media/images/Q0MFC2tZLwOGZXEpi7Og.bmp\" alt=\"hey\">\n" +
				"     </body>\n" +
				"    </html>\n", "text/html", "UTF-8" );*/

		TextLoader.getInstance().loadText( new File( mEntry.getPathDir() + "text" ), new TextLoader.TextLoaderCallback() {
			@Override
			public void onLoaded(String data) {
				prepareHtml( data );
			}

			@Override
			public void onException(Exception e) {
				Log.i( TAG, "onException: " + e );
			}
		} );
	}

	private void prepareHtml(String data) {
		Document doc = Jsoup.parse( data );
		doc.body().attr( "bgcolor", Integer.toHexString( mEntry.getProperties().getBgColor() ) );
		doc.head().appendElement( "meta" )
				.attr( "name", "viewport" )
				.attr( "content", "width=device-width, initial-scale=1, user-scalable=no" );
		doc.head().appendElement( "style" )
				.text("img{max-width:100%;}");

		Point size = new Point();
		getWindowManager().getDefaultDisplay().getSize( size );
		int maxWidth = (int) (size.x * 0.95);

		for (Element element : doc.select( "img[src]" )) {
			String src = element.attr( "src" );

			try {
				String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension( MimeTypeMap.getFileExtensionFromUrl( src ) );
				String base64 = Base64.encodeToString( MediaLoader.getInstance().loadFileBytesSync( new File( src ) ), Base64.DEFAULT );
				element.attr( "src", "data:" + mime + ";base64," + base64 );

				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile( src, options );

				int width = Math.min( options.outWidth, maxWidth );
				Log.i( TAG, "prepareHtml: width=" + width );

				//element.attr( "style", String.format( Locale.ROOT, "width: %dpx;max-width:%dpx !important;", width, width ) );
				element.attr( "style", String.format( Locale.ROOT, "width: %dpx;", width ) );
			} catch (IOException e) {
				e.printStackTrace();
				Log.i( TAG, "prepareHtml: " + e );

				if ( !src.startsWith( "file:///" ) ) {
					src = "file://" + src;
				}

				element.attr( "src", src );
			}
			element.after( "<br>" );
			element.attr( "alt", "error" );
			element.attr( "onclick", String.format( "Android.openSource('%s')", src ) );
		}

		runOnUiThread( ()->{
			mWebView.loadData( Base64.encodeToString(doc.html().getBytes( StandardCharsets.UTF_8 ), Base64.DEFAULT), "text/html; charset=utf-8", "base64" );
		} );
	}
}