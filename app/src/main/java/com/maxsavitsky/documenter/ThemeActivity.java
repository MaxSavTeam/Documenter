package com.maxsavitsky.documenter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

public class ThemeActivity extends AppCompatActivity {
	public int mAlertDialogStyle = R.style.AlertDialogDark;

	public int mEditTextDefaultColor = R.color.black;

	// only for themes
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if(sharedPreferences.getBoolean( "dark_header", false )){
			setTheme( R.style.AppTheme_Dark );
			mAlertDialogStyle = R.style.AlertDialogDark;
			mEditTextDefaultColor = R.color.white;
		}else{
			setTheme( R.style.AppTheme );
			mAlertDialogStyle = R.style.Theme_AppCompat_Light_Dialog;
			mEditTextDefaultColor = R.color.black;
		}
	}
}