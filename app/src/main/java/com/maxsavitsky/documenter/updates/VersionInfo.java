package com.maxsavitsky.documenter.updates;

import org.json.JSONException;
import org.json.JSONObject;

public class VersionInfo {
	private int mVersionCode = -1;
	private String mVersionName = "";
	private String mDownloadUrl = "";
	private int mUpdateSize = -1;
	private int mBuildCode = -1;
	private boolean isNecessaryUpdate = false;
	private int mMinSdk = -1;

	public VersionInfo(int versionCode, String versionName, String downloadUrl, int updateSize, int buildCode, boolean isNecessary) {
		mVersionCode = versionCode;
		mVersionName = versionName;
		mDownloadUrl = downloadUrl;
		mUpdateSize = updateSize;
		mBuildCode = buildCode;
		isNecessaryUpdate = isNecessary;
	}

	public VersionInfo() {}

	public static VersionInfo parseInfoFromJson(String json) throws JSONException {
		JSONObject jsonObject = new JSONObject( json );
		VersionInfo versionInfo = new VersionInfo();
		int versionCode = jsonObject.getInt( "versionCode" );
		versionInfo.setVersionCode( versionCode );

		int size = jsonObject.getInt( "apkSize" );
		versionInfo.setUpdateSize( size );

		int buildCode = jsonObject.getInt( "buildCode" );
		versionInfo.setBuildCode( buildCode );

		String downloadUrl = jsonObject.getString( "downloadUrl" );
		versionInfo.setDownloadUrl( downloadUrl );

		String versionName = jsonObject.getString( "versionName" );
		versionInfo.setVersionName( versionName );

		boolean necessary = jsonObject.getBoolean( "necessaryUpdate" );
		versionInfo.setNecessaryUpdate( necessary );

		int minSdk = jsonObject.getInt( "minSdk" );
		versionInfo.setMinSdk( minSdk );

		return versionInfo;
	}

	public int getVersionCode() {
		return mVersionCode;
	}

	public void setVersionCode(int versionCode) {
		mVersionCode = versionCode;
	}

	public String getVersionName() {
		return mVersionName;
	}

	public void setVersionName(String versionName) {
		mVersionName = versionName;
	}

	public String getDownloadUrl() {
		return mDownloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		mDownloadUrl = downloadUrl;
	}

	public int getUpdateSize() {
		return mUpdateSize;
	}

	public void setUpdateSize(int updateSize) {
		mUpdateSize = updateSize;
	}

	public int getBuildCode() {
		return mBuildCode;
	}

	public void setBuildCode(int buildCode) {
		mBuildCode = buildCode;
	}

	public boolean isNecessaryUpdate() {
		return isNecessaryUpdate;
	}

	public void setNecessaryUpdate(boolean necessaryUpdate) {
		isNecessaryUpdate = necessaryUpdate;
	}

	public int getMinSdk() {
		return mMinSdk;
	}

	public void setMinSdk(int minSdk) {
		mMinSdk = minSdk;
	}
}
