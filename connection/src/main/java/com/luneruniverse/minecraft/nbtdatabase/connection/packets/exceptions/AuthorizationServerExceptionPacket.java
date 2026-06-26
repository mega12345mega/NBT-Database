package com.luneruniverse.minecraft.nbtdatabase.connection.packets.exceptions;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.AuthorizationServerException;

public class AuthorizationServerExceptionPacket extends ServerExceptionPacket {
	
	private @NotNull String message;
	
	public AuthorizationServerExceptionPacket(AuthorizationServerException e) {
		message = e.getMessage();
	}
	AuthorizationServerExceptionPacket() {
		// Deserialization
	}
	
	@Override
	public AuthorizationServerException getException() {
		return new AuthorizationServerException(message);
	}
	
}
