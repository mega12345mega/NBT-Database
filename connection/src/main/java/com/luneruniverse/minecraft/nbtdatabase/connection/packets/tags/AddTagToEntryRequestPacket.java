package com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

public class AddTagToEntryRequestPacket extends Packet {
	
	private long entry;
	private @NotNull String tag;
	
	public AddTagToEntryRequestPacket(long entry, String tag) {
		this.entry = entry;
		this.tag = tag;
	}
	AddTagToEntryRequestPacket() {
		// Deserialization
	}
	
	public long getEntry() {
		return entry;
	}
	public String getTag() {
		return tag;
	}
	
}
