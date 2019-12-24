package com.maxsavitsky.documenter.datatypes;

public class Element {
	private String mType;
	private String mImageSrc;
	private String mHtmlText;

	public Element(String type, String imageSrc, String htmlText) {
		mType = type;
		mImageSrc = imageSrc;
		mHtmlText = htmlText;
	}

	public String getType() {
		return mType;
	}

	public void setType(String type) {
		mType = type;
	}

	public String getImageSrc() {
		return mImageSrc;
	}

	public void setImageSrc(String imageSrc) {
		mImageSrc = imageSrc;
	}

	public String getHtmlText() {
		return mHtmlText;
	}

	public void setHtmlText(String htmlText) {
		mHtmlText = htmlText;
	}
}
