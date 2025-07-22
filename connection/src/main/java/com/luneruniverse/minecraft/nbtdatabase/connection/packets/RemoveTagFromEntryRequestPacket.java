package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;

public class RemoveTagFromEntryRequestPacket extends Packet {
	
	private long entry;
	private @NotNull String tag;
	
	public RemoveTagFromEntryRequestPacket(long entry, String tag) {
		this.entry = entry;
		this.tag = tag;
	}
	RemoveTagFromEntryRequestPacket() {
		// Deserialization
	}
	
	public long getEntry() {
		return entry;
	}
	public String getTag() {
		return tag;
	}
	
}
