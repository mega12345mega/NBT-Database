package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.luneruniverse.minecraft.nbtdatabase.Config;
import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class ConfigPacket extends Packet {
	
	private final Config config;
	
	public ConfigPacket(Config config) {
		this.config = config;
	}
	public ConfigPacket(DataInputStream in) throws IOException {
		this.config = new Config(in.readInt(), in.readInt());
	}
	
	public Config getConfig() {
		return config;
	}
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(config.getMaxNbtSize());
		out.writeInt(config.getMaxNumResults());
	}
	
}
