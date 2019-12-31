package com.maxsavitsky.documenter;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class ErrorHandlerActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );

		String path = getIntent().getStringExtra( "path" );
		Intent newIntent = new Intent( Intent.ACTION_SEND );
		newIntent.setType( "plain/text" );
		newIntent.putExtra( Intent.EXTRA_EMAIL, new String[]{"maxsavhelp@gmail.com"} );
		newIntent.putExtra (Intent.EXTRA_SUBJECT, "Documenter log file");
		newIntent.putExtra (Intent.EXTRA_STREAM, Uri.parse ("file://" + path));
		newIntent.putExtra (Intent.EXTRA_TEXT, "Log file attached.");
		startActivity( newIntent );
		Toast.makeText( this, "Started", Toast.LENGTH_SHORT ).show();
		System.exit( 1 );

	}
}
