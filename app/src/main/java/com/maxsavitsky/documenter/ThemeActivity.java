package com.maxsavitsky.documenter;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

public class ThemeActivity extends AppCompatActivity {
	protected int mAlertDialogStyle = R.style.AlertDialogDark;

	protected int mTextColor = R.color.black;

	protected boolean isDarkTheme = false;

	protected int BACKGROUND_COLOR;

	protected int CURRENT_THEME;

	// only for themes
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if(sharedPreferences.getBoolean( "dark_theme", false )){
			setTheme( R.style.AppTheme_Dark );
			mAlertDialogStyle = R.style.AlertDialogDark;
			mTextColor = R.color.white;
			isDarkTheme = true;
			CURRENT_THEME = R.style.AppTheme_Dark;
			BACKGROUND_COLOR = Color.BLACK;
		}else{
			BACKGROUND_COLOR = Color.WHITE;
			CURRENT_THEME = R.style.AppTheme;
			setTheme( R.style.AppTheme );
			mAlertDialogStyle = R.style.Theme_AppCompat_Light_Dialog;
			mTextColor = R.color.black;
		}
		super.onCreate( savedInstanceState );
	}
}