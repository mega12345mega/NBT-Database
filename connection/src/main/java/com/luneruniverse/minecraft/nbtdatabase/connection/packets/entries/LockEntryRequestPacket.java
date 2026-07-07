package com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries;

import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

public class LockEntryRequestPacket extends Packet {
	
	private long id;
	
	public LockEntryRequestPacket(long id) {
		this.id = id;
	}
	LockEntryRequestPacket() {
		// Deserialization
	}
	
	public long getId() {
		return id;
	}
	
}
