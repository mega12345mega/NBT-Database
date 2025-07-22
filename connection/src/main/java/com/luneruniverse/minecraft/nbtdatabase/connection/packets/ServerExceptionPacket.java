package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;

public class ServerExceptionPacket extends Packet {
	
	private @NotNull String message;
	
	public ServerExceptionPacket(String message) {
		this.message = message;
	}
	ServerExceptionPacket() {
		// Deserialization
	}
	
	public String getMessage() {
		return message;
	}
	
}
