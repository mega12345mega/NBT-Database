package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

public class RemoveEntryRequestPacket extends Packet {
	
	private long id;
	
	public RemoveEntryRequestPacket(long id) {
		this.id = id;
	}
	RemoveEntryRequestPacket() {
		// Deserialization
	}
	
	public long getId() {
		return id;
	}
	
}
