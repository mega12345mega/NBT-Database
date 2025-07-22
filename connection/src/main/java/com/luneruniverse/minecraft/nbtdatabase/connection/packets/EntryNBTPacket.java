package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

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
