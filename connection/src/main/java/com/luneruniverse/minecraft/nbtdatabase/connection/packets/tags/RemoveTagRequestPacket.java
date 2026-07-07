package com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

public class RemoveTagRequestPacket extends Packet {
	
	private @NotNull String name;
	
	public RemoveTagRequestPacket(String name) {
		this.name = name;
	}
	RemoveTagRequestPacket() {
		// Deserialization
	}
	
	public String getName() {
		return name;
	}
	
}
