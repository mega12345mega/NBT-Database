package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

public class EntryIdPacket extends Packet {
	
	private long id;
	
	public EntryIdPacket(long id) {
		this.id = id;
	}
	EntryIdPacket() {
		// Deserialization
	}
	
	public long getId() {
		return id;
	}
	
}
