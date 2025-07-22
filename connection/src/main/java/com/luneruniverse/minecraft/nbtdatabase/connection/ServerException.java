package com.luneruniverse.minecraft.nbtdatabase.connection;

@SuppressWarnings("serial")
public class ServerException extends RequestFailedException {
	
	public ServerException(String message) {
		super(message);
	}
	
}
