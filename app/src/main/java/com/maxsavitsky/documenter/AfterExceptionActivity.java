package com.maxsavitsky.documenter;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.maxsavitsky.documenter.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class AfterExceptionActivity extends AppCompatActivity {
	private String path;

	private static final String TAG = MainActivity.TAG + " ErrHandler";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_errhandler );

		path = getIntent().getStringExtra( "path" );

		Log.i( TAG, "onCreate: path=" + path );

		findViewById( R.id.btnErrSendReport ).setOnClickListener( v->Utils.sendLog(this, path) );
		findViewById( R.id.btnErrViewReport ).setOnClickListener( v->{
			FileInputStream inputStream = null;
			String mes = "";
			try{
				inputStream = new FileInputStream( new File(path) );
				byte[] buffer = new byte[1024];
				int len;
				while((len = inputStream.read(buffer)) != -1){
					if(len < 1024)
						buffer = Arrays.copyOf(buffer, len);

					mes = String.format( "%s%s", mes, new String(buffer, StandardCharsets.UTF_8 ) );
				}
			}catch (Exception e){
				try {
					if(inputStream != null)
						inputStream.close();
				}catch (IOException io){
					//ignore
				}
				return;
			}
			AlertDialog.Builder builder = new AlertDialog.Builder( this )
					.setCancelable( false )
					.setNeutralButton( "OK", (dialog, which)->dialog.cancel() ).setMessage( mes );
			builder.create().show();
		} );

		findViewById( R.id.btnErrRestartApp ).setOnClickListener( v->restartApp() );
		AlertDialog.Builder builder = new AlertDialog.Builder( this )
				.setTitle( R.string.please )
				.setMessage( Html.fromHtml( getResources().getString( R.string.send_report_dialog_mes ) ) )
				.setCancelable( false )
				.setPositiveButton( "OK", (dialog, which)->{
					dialog.cancel();
					Utils.sendLog(this, path);
				} )
				.setNegativeButton( "Hide", ((dialog, which)->dialog.cancel() ) );

		builder.create().show();
	}

	private void restartApp() {
		Intent intent = new Intent( this, MainActivity.class );
		startActivity( intent );
		this.finish();
	}
}