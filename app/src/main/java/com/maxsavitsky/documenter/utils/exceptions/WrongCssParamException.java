package com.maxsavitsky.documenter.utils.exceptions;

public class WrongCssParamException extends RuntimeException {

	public WrongCssParamException() {
		super();
	}

	public WrongCssParamException(String message) {
		super( message );
	}

	public WrongCssParamException(String message, Throwable cause) {
		super( message, cause );
	}

	public WrongCssParamException(Throwable cause) {
		super( cause );
	}
}
