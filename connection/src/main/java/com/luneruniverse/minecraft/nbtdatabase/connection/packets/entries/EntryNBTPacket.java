package com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries;

import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

public class EntryNBTPacket extends Packet {
	
	private byte[] nbt;
	
	public EntryNBTPacket(byte[] nbt) {
		this.nbt = nbt;
	}
	EntryNBTPacket() {
		// Deserialization
	}
	
	public byte[] getNBT() {
		return nbt;
	}
	
}
