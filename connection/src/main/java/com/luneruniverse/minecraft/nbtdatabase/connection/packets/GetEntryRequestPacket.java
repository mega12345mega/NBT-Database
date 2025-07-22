package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

public class GetEntryRequestPacket extends Packet {
	
	private long id;
	
	public GetEntryRequestPacket(long id) {
		this.id = id;
	}
	GetEntryRequestPacket() {
		// Deserialization
	}
	
	public long getId() {
		return id;
	}
	
}
