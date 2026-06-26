package com.luneruniverse.minecraft.nbtdatabase.connection.packets.exceptions;

import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.InternalServerException;

public class InternalServerExceptionPacket extends ServerExceptionPacket {
	
	public InternalServerExceptionPacket() {}
	
	@Override
	public InternalServerException getException() {
		return new InternalServerException();
	}
	
}
