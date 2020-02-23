package com.maxsavitsky.documenter.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import com.maxsavitsky.documenter.BuildConfig;

import java.io.File;

public class ApkInstaller {
	public static void installApk(Context context, File file){
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(fromFile(context, file), "application/vnd.android.package-archive");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		context.startActivity(intent);
	}

	private static Uri fromFile(Context context, File file){
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
			return FileProvider.getUriForFile( context, BuildConfig.APPLICATION_ID + ".provider", file );
		}else{
			return Uri.fromFile(file);
		}
	}
}
