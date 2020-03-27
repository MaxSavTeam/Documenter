package com.maxsavitsky.documenter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

public class ThemeActivity extends AppCompatActivity {
	public int mAlertDialogStyle = R.style.AlertDialogDark;

	public int mTextColor = R.color.black;

	public boolean isDarkTheme = false;

	// only for themes
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if(sharedPreferences.getBoolean( "dark_theme", false )){
			setTheme( R.style.AppTheme_Dark );
			mAlertDialogStyle = R.style.AlertDialogDark;
			mTextColor = R.color.white;
			isDarkTheme = true;
		}else{
			setTheme( R.style.AppTheme );
			mAlertDialogStyle = R.style.Theme_AppCompat_Light_Dialog;
			mTextColor = R.color.black;
		}
	}
}