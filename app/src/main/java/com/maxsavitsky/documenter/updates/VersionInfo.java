package com.maxsavitsky.documenter.updates;

public class VersionInfo {
	private int mVersionCode = -1;
	private String mVersionName = "";
	private String mDownloadUrl = "";
	private int mUpdateSize = -1;
	private int revision = 0;

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

	public int getRevision() {
		return revision;
	}

	public void setRevision(int revision) {
		this.revision = revision;
	}
}
