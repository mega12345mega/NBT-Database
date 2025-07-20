package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;

public class GetTagRequestPacket extends Packet {
	
	private @NotNull String name;
	
	public GetTagRequestPacket(String name) {
		this.name = name;
	}
	GetTagRequestPacket() {
		// Deserialization
	}
	
	public String getName() {
		return name;
	}
	
}
