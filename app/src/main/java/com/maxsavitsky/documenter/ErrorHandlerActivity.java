package com.maxsavitsky.documenter;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Html;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.maxsavitsky.documenter.utils.Utils;

public class ErrorHandlerActivity extends AppCompatActivity {
	String path;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Looper.prepare();
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_errhandler );
		getSupportActionBar().setTitle( "Documenter bug report" );

		//final String path = "/storage/emulated/0/Android/data/com.maxsavitsky.documenter.alpha/files/stacktraces/stacktrace-06-01-2020_13:08:59.trace";
		path = getIntent().getStringExtra( "path" );

		findViewById( R.id.btnErrSendReport ).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sendLog();
			}
		} );

		AlertDialog.Builder builder = new AlertDialog.Builder( this )
				.setTitle( R.string.please )
				.setMessage( Html.fromHtml( getResources().getString( R.string.send_report_dialog_mes ) ) )
				.setCancelable( false ).setPositiveButton( "OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						sendLog();
					}
				} );

		builder.create().show();
	}


	private void sendLog() {
		Intent newIntent = new Intent( Intent.ACTION_SEND );
		newIntent.setType( "message/rfc822" );
		newIntent.putExtra( Intent.EXTRA_EMAIL, new String[]{ "maxsavhelp@gmail.com" } );
		newIntent.putExtra( Intent.EXTRA_SUBJECT, "Error in documenter" );
		newIntent.putExtra( Intent.EXTRA_STREAM, Uri.parse( "file://" + path ) );
		newIntent.putExtra( Intent.EXTRA_TEXT, "Log file attached." );
		StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
		StrictMode.setVmPolicy( builder.build() );
		try {
			//startActivity( Intent.createChooser( newIntent, "Choose" ) );
			startActivity( newIntent );
		} catch (Exception e) {
			Utils.getErrorDialog( e, ErrorHandlerActivity.this ).show();
			e.printStackTrace();
		}
	}
}
