package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.util.Arrays;
import java.util.List;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.Entry;

public class EntriesPacket extends Packet {
	
	private @NotNull Entry[] entries;
	
	public EntriesPacket(Entry[] entries) {
		this.entries = entries;
	}
	public EntriesPacket(List<Entry> entries) {
		this(entries.toArray(new Entry[entries.size()]));
	}
	public EntriesPacket(Entry entry) {
		this.entries = new Entry[entry == null ? 0 : 1];
		if (entry != null)
			entries[0] = entry;
	}
	EntriesPacket() {
		// Deserialization
	}
	
	public Entry[] getEntries() {
		return entries;
	}
	public List<Entry> getEntriesList() {
		return Arrays.asList(entries);
	}
	public Entry getEntryNonNull() throws IllegalStateException {
		if (entries.length != 1)
			throw new IllegalStateException("Expected exactly 1 entry");
		return entries[0];
	}
	public Entry getEntryNullable() throws IllegalStateException {
		if (entries.length > 1)
			throw new IllegalStateException("Expected exactly 0 or 1 entry");
		if (entries.length == 0)
			return null;
		return entries[0];
	}
	
}
