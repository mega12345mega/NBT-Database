package com.luneruniverse.minecraft.nbtdatabase.connection.packets.config;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.Config;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

public class SetConfigRequestPacket extends Packet {
	
	private @NotNull Config config;
	
	public SetConfigRequestPacket(Config config) {
		this.config = config;
	}
	SetConfigRequestPacket() {
		// Deserialization
	}
	
	public Config getConfig() {
		return config;
	}
	
}
