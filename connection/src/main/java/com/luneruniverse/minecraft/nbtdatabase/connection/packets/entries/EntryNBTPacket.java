package com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries;

import java.util.Optional;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

public class EntryNBTPacket extends Packet {
	
	private @NotNull Optional<byte[]> nbt;
	
	public EntryNBTPacket(Optional<byte[]> nbt) {
		this.nbt = nbt;
	}
	EntryNBTPacket() {
		// Deserialization
	}
	
	public Optional<byte[]> getNBT() {
		return nbt;
	}
	
}
