package com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries;

import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

public class GetEntryNBTRequestPacket extends Packet {
	
	private long id;
	
	public GetEntryNBTRequestPacket(long id) {
		this.id = id;
	}
	GetEntryNBTRequestPacket() {
		// Deserialization
	}
	
	public long getId() {
		return id;
	}
	
}
