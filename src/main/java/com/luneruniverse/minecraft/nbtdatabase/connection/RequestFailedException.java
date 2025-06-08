package com.luneruniverse.minecraft.nbtdatabase.connection;

@SuppressWarnings("serial")
public class RequestFailedException extends Exception {
	
	public RequestFailedException(String message) {
		super(message);
	}
	public RequestFailedException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
