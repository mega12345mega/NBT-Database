package com.luneruniverse.minecraft.nbtdatabase.connection.exceptions;

import com.luneruniverse.minecraft.nbtdatabase.connection.packets.exceptions.AuthorizationServerExceptionPacket;

@SuppressWarnings("serial")
public class AuthorizationServerException extends ServerException {
	
	public AuthorizationServerException(String message) {
		super(message);
	}
	
	@Override
	public AuthorizationServerExceptionPacket toPacket() {
		return new AuthorizationServerExceptionPacket(this);
	}
	
}
