package com.maxsavitsky.documenter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.datatypes.Element;
import com.maxsavitsky.documenter.datatypes.Entry;
import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.Utils;

import java.util.Objects;
import java.util.Queue;

public class ViewEntry extends AppCompatActivity {

	private Entry mEntry;
	private SharedPreferences sp;

	private void applyTheme(){
		ActionBar actionBar = getSupportActionBar();
		if ( actionBar != null ) {
			Utils.applyDefaultActionBarStyle(actionBar);
			actionBar.setTitle( mEntry.getName() );
		}
	}

	private void backPressed(){
		setResult( ResultCodes.RESULT_CODE_OK );
		finish();
	}

	@Override
	public void onBackPressed() {
		backPressed();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if(item.getItemId() == android.R.id.home){
			backPressed();
		}
		return super.onOptionsItemSelected( item );
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		Intent intent = getIntent();
		mEntry = MainData.getEntryWithId( intent.getStringExtra( "id" ) );
		applyTheme();

		sp = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

		WebView webView = new WebView( this );
		webView.getSettings().setAllowFileAccessFromFileURLs( true );
		webView.getSettings().setAllowFileAccess( true );
		webView.getSettings().setJavaScriptCanOpenWindowsAutomatically( false );
		webView.getSettings().setDefaultFontSize( sp.getInt( "default_webview_font_size_sp", 22 ) );
		webView.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT ) );
		webView.loadUrl( "file://" + mEntry.getPathDir() + "text.html" );
		setContentView( webView );
	}
}