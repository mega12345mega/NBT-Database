package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

public class DisconnectPacket extends Packet {
	
	private String msg;
	
	public DisconnectPacket(String msg) {
		this.msg = msg;
	}
	DisconnectPacket() {
		// Deserialization
	}
	
	public String getMessage() {
		return msg;
	}
	
}
