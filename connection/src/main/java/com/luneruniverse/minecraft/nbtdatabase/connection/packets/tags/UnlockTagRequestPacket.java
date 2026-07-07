package com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

public class UnlockTagRequestPacket extends Packet {
	
	private @NotNull String name;
	
	public UnlockTagRequestPacket(String name) {
		this.name = name;
	}
	UnlockTagRequestPacket() {
		// Deserialization
	}
	
	public String getName() {
		return name;
	}
	
}
