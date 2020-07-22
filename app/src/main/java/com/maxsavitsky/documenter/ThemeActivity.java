package com.maxsavitsky.documenter;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.maxsavitsky.documenter.utils.Utils;

public class ThemeActivity extends AppCompatActivity {
	protected int mAlertDialogStyle = R.style.AlertDialogDark;

	protected int mTextColor = R.color.black;

	protected boolean isDarkTheme = false;

	protected int BACKGROUND_COLOR;

	public static int CURRENT_THEME;

	private void applyDarkTheme(){
		setTheme( R.style.AppTheme_Dark );
		mAlertDialogStyle = R.style.AlertDialogDark;
		mTextColor = R.color.white;
		isDarkTheme = true;
		CURRENT_THEME = R.style.AppTheme_Dark;
		BACKGROUND_COLOR = Color.BLACK;
	}

	private void applyLightTheme(){
		BACKGROUND_COLOR = Color.WHITE;
		CURRENT_THEME = R.style.AppTheme;
		setTheme( R.style.AppTheme );
		mAlertDialogStyle = R.style.Theme_AppCompat_Light_Dialog;
		mTextColor = R.color.black;
	}

	// only for themes
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		SharedPreferences sharedPreferences = Utils.getDefaultSharedPreferences();
		int darkModeState = sharedPreferences.getInt( "theme_state", 2 );

		if(darkModeState == 0){
			applyLightTheme();
		}else if(darkModeState == 1){
			applyDarkTheme();
		}else{
			switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
				case Configuration.UI_MODE_NIGHT_YES:
					applyDarkTheme();
					break;
				case Configuration.UI_MODE_NIGHT_NO:
					applyLightTheme();
					break;
			}
		}

		super.onCreate( savedInstanceState );
	}
}