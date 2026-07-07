package com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

public class LockTagRequestPacket extends Packet {
	
	private @NotNull String name;
	
	public LockTagRequestPacket(String name) {
		this.name = name;
	}
	LockTagRequestPacket() {
		// Deserialization
	}
	
	public String getName() {
		return name;
	}
	
}
