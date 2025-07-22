package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.Config;

public class ConfigPacket extends Packet {
	
	private @NotNull Config config;
	
	public ConfigPacket(Config config) {
		this.config = config;
	}
	ConfigPacket() {
		// Deserialization
	}
	
	public Config getConfig() {
		return config;
	}
	
}
