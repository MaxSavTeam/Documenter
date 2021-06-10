package com.maxsavitsky.documenter.ui;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.maxsavitsky.documenter.BuildConfig;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.ThemeActivity;

public class AboutAppActivity extends ThemeActivity {

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if(item.getItemId() == android.R.id.home)
			onBackPressed();
		return super.onOptionsItemSelected( item );
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_about_app );

		Toolbar toolbar = findViewById( R.id.toolbar3 );
		setSupportActionBar( toolbar );

		ActionBar actionBar = getSupportActionBar();
		if(actionBar != null)
			actionBar.setDisplayHomeAsUpEnabled( true );

		TextView textView = findViewById(R.id.text_view_app_version);
		textView.setText( BuildConfig.VERSION_NAME );

		findViewById( R.id.appIcon ).setOnLongClickListener( v->{
			String s = "Android Version: " + Build.VERSION.RELEASE + "\n" +
					"Android SDK: " + Build.VERSION.SDK_INT + "\n\n" +
					BuildConfig.APPLICATION_ID + "\n" +
					"Build number: " + BuildConfig.VERSION_CODE + "\n" +
					"CD: " + BuildConfig.BUILD_CODE + "\n\n" +
					"Compilation date: " + BuildConfig.COMPILATION_DATE + "\n" +
					"Build type: " + BuildConfig.BUILD_TYPE;
			AlertDialog.Builder builder = new AlertDialog.Builder(this, super.mAlertDialogStyle);
			builder
					.setMessage( s )
					.setTitle( R.string.about_app )
					.setPositiveButton( R.string.close, (dialog, which) -> dialog.cancel() );
			builder.show();
			return true;
		} );
	}
}