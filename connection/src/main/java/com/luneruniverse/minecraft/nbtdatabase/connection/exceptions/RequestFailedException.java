package com.luneruniverse.minecraft.nbtdatabase.connection.exceptions;

@SuppressWarnings("serial")
public class RequestFailedException extends Exception {
	
	public RequestFailedException(String message) {
		super(message);
	}
	public RequestFailedException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
