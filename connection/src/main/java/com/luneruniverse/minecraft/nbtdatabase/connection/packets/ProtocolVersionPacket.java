package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

public class ProtocolVersionPacket extends Packet {
	
	private final int version;
	
	public ProtocolVersionPacket(int version) {
		this.version = version;
	}
	
	public int getVersion() {
		return version;
	}
	
}
