package com.luneruniverse.minecraft.nbtdatabase.connection.exceptions;

import com.luneruniverse.minecraft.nbtdatabase.connection.packets.exceptions.ServerExceptionPacket;

@SuppressWarnings("serial")
public abstract class ServerException extends RequestFailedException {
	
	public ServerException(String message) {
		super(message);
	}
	
	public abstract ServerExceptionPacket toPacket();
	
}
