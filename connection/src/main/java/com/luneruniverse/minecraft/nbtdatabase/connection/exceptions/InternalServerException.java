package com.luneruniverse.minecraft.nbtdatabase.connection.exceptions;

import com.luneruniverse.minecraft.nbtdatabase.connection.packets.exceptions.InternalServerExceptionPacket;

@SuppressWarnings("serial")
public class InternalServerException extends ServerException {
	
	public InternalServerException() {
		super("An internal server error occurred");
	}
	
	@Override
	public InternalServerExceptionPacket toPacket() {
		return new InternalServerExceptionPacket();
	}
	
}
