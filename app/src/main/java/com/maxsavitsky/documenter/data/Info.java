package com.maxsavitsky.documenter.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Info {
	private int timeStamp = 0;

	public Info(int timeStamp) {
		this.timeStamp = timeStamp;
	}

	public Info() {}

	public int getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(int timeStamp) {
		this.timeStamp = timeStamp;
	}

	private String formatDateWithParameter(String param){
		Date date = new Date( timeStamp );
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat( param, Locale.ROOT );
		return simpleDateFormat.format( date );
	}
}
