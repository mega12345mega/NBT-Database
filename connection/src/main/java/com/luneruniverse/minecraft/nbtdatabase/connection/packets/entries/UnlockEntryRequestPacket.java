package com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries;

import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

public class UnlockEntryRequestPacket extends Packet {
	
	private long id;
	
	public UnlockEntryRequestPacket(long id) {
		this.id = id;
	}
	UnlockEntryRequestPacket() {
		// Deserialization
	}
	
	public long getId() {
		return id;
	}
	
}
