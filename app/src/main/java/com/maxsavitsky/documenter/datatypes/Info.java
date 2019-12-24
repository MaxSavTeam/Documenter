package com.maxsavitsky.documenter.datatypes;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Info {
	private int timeStamp;

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

	public int getCreateYear(){
		return Integer.parseInt( formatDateWithParameter( "yyyy" ) );
	}

	public int getCreateMonth(){
		return Integer.parseInt( formatDateWithParameter( "MM" ) );
	}

	public int getCreateDay(){
		return Integer.parseInt( formatDateWithParameter( "dd" ) );
	}

	public int getCreateHour(){
		return Integer.parseInt( formatDateWithParameter( "hh" ) );
	}

	public int getCreateMinute(){
		return Integer.parseInt( formatDateWithParameter( "mm" ) );
	}

	public int getCreateSeconds(){
		return Integer.parseInt( formatDateWithParameter( "ss" ) );
	}
}
