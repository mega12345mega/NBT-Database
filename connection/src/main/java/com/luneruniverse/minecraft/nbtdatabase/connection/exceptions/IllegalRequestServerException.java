package com.luneruniverse.minecraft.nbtdatabase.connection.exceptions;

import com.luneruniverse.minecraft.nbtdatabase.connection.packets.exceptions.IllegalRequestServerExceptionPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.exceptions.ServerExceptionPacket;
import com.luneruniverse.minecraft.nbtdatabase.request.IllegalRequestException;

@SuppressWarnings("serial")
public class IllegalRequestServerException extends ServerException {
	
	public IllegalRequestServerException(String message) {
		super(message);
	}
	public IllegalRequestServerException(IllegalRequestException exception) {
		this(exception.getMessage());
	}
	
	@Override
	public ServerExceptionPacket toPacket() {
		return new IllegalRequestServerExceptionPacket(this);
	}
	
}
