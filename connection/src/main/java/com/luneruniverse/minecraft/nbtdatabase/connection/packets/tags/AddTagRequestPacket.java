package com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

public class AddTagRequestPacket extends Packet {
	
	private @NotNull String name;
	private int color;
	
	public AddTagRequestPacket(String name, int color) {
		this.name = name;
		this.color = color;
	}
	AddTagRequestPacket() {
		// Deserialization
	}
	
	public String getName() {
		return name;
	}
	public int getColor() {
		return color;
	}
	
}
