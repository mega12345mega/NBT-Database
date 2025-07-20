package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.util.Arrays;
import java.util.List;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.NBTEntry;

public class EntriesPacket extends Packet {
	
	private @NotNull NBTEntry[] entries;
	
	public EntriesPacket(NBTEntry[] entries) {
		this.entries = entries;
	}
	public EntriesPacket(List<NBTEntry> entries) {
		this(entries.toArray(new NBTEntry[entries.size()]));
	}
	public EntriesPacket(NBTEntry entry) {
		this.entries = new NBTEntry[entry == null ? 0 : 1];
		if (entry != null)
			entries[0] = entry;
	}
	EntriesPacket() {
		// Deserialization
	}
	
	public NBTEntry[] getEntries() {
		return entries;
	}
	public List<NBTEntry> getEntriesList() {
		return Arrays.asList(entries);
	}
	public NBTEntry getEntryNonNull() throws IllegalStateException {
		if (entries.length != 1)
			throw new IllegalStateException("Expected exactly 1 entry");
		return entries[0];
	}
	public NBTEntry getEntryNullable() throws IllegalStateException {
		if (entries.length > 1)
			throw new IllegalStateException("Expected exactly 0 or 1 entry");
		if (entries.length == 0)
			return null;
		return entries[0];
	}
	
}
