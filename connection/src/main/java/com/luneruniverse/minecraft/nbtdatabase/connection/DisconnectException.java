package com.luneruniverse.minecraft.nbtdatabase.connection;

@SuppressWarnings("serial")
public class DisconnectException extends RuntimeException {
	
	public DisconnectException() {}
	public DisconnectException(String message) {
		super(message);
	}
	
}
