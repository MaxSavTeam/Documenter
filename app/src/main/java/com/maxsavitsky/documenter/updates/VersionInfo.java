package com.maxsavitsky.documenter.updates;

import org.json.JSONException;
import org.json.JSONObject;

public class VersionInfo {
	private int mVersionCode;
	private String mVersionName;
	private String mDownloadUrl;
	private int mUpdateSize;
	private int mBuildCode;
	private boolean isNecessaryUpdate;

	public VersionInfo(int versionCode, String versionName, String downloadUrl, int updateSize, int buildCode, boolean isNecessary) {
		mVersionCode = versionCode;
		mVersionName = versionName;
		mDownloadUrl = downloadUrl;
		mUpdateSize = updateSize;
		mBuildCode = buildCode;
		isNecessaryUpdate = isNecessary;
	}

	/*public static VersionInfo parseInfo(String data){
		String[] strings = data.split( ";" );
		int versionCode = Integer.parseInt( strings[0] );
		String downloadUrl = strings[1];
		String versionName = strings[2];
		int size = Integer.parseInt( strings[3] );
		int buildCode = Integer.parseInt( strings[4] );

		return new VersionInfo( versionCode, versionName, downloadUrl, size, buildCode );
	}*/

	public static VersionInfo parseInfoFromJson(String json) throws JSONException {
		JSONObject jsonObject = new JSONObject( json );
		int versionCode = jsonObject.getInt( "versionCode" );
		int size = jsonObject.getInt( "apkSize" );
		int buildCode = jsonObject.getInt( "buildCode" );
		String downloadUrl = jsonObject.getString( "downloadUrl" );
		String versionName = jsonObject.getString( "versionName" );
		boolean necessary = jsonObject.getBoolean( "necessaryUpdate" );

		return new VersionInfo( versionCode, versionName, downloadUrl, size, buildCode, necessary );
	}

	public int getVersionCode() {
		return mVersionCode;
	}

	public String getDownloadUrl() {
		return mDownloadUrl;
	}

	public int getUpdateSize() {
		return mUpdateSize;
	}

	public int getBuildCode() {
		return mBuildCode;
	}

	public boolean isNecessaryUpdate() {
		return isNecessaryUpdate;
	}
}
