package com.maxsavitsky.documenter.updates;

public class VersionInfo {
	private int mVersionCode = -1;
	private String mVersionName = "";
	private String mDownloadUrl = "";
	private int mUpdateSize = -1;
	private boolean mImportant = false;

	public VersionInfo() {}

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

	public boolean isImportant() {
		return mImportant;
	}

	public VersionInfo setImportant(boolean important) {
		mImportant = important;
		return this;
	}
}
