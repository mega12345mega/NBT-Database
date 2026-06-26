package com.luneruniverse.minecraft.nbtdatabase.connection.packets.exceptions;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.IllegalRequestServerException;

public class IllegalRequestServerExceptionPacket extends ServerExceptionPacket {
	
	private @NotNull String message;
	
	public IllegalRequestServerExceptionPacket(IllegalRequestServerException e) {
		message = e.getMessage();
	}
	IllegalRequestServerExceptionPacket() {
		// Deserialization
	}
	
	@Override
	public IllegalRequestServerException getException() {
		return new IllegalRequestServerException(message);
	}
	
}
