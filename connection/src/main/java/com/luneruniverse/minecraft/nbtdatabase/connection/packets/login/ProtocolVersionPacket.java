package com.luneruniverse.minecraft.nbtdatabase.connection.packets.login;

import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

public class ProtocolVersionPacket extends Packet {
	
	private final int version;
	
	public ProtocolVersionPacket(int version) {
		this.version = version;
	}
	
	public int getVersion() {
		return version;
	}
	
}
