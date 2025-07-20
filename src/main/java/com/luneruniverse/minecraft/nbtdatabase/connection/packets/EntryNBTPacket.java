package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;

public class EntryNBTPacket extends Packet {
	
	private @NotNull byte[] nbt;
	
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
