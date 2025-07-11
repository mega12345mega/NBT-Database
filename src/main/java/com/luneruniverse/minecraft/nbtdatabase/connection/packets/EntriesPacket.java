package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.luneruniverse.minecraft.nbtdatabase.NBTEntry;
import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class EntriesPacket extends Packet {
	
	private final NBTEntry[] entries;
	
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
	public EntriesPacket(DataInputStream in) throws IOException {
		this.entries = new NBTEntry[in.readInt()];
		for (int i = 0; i < entries.length; i++) {
			long id = in.readLong();
			String name = in.readUTF();
			int nbtLength = in.readInt();
			int dataVersion = in.readInt();
			UUID authorUuid = new UUID(in.readLong(), in.readLong());
			String authorUsername = in.readUTF();
			long created = in.readLong();
			long modified = in.readLong();
			String hash = in.readUTF();
			boolean verified = in.readBoolean();
			this.entries[i] = new NBTEntry(id, name, nbtLength, dataVersion, authorUuid, authorUsername, created, modified, hash, verified);
		}
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
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(entries.length);
		for (NBTEntry entry : entries) {
			out.writeLong(entry.id);
			out.writeUTF(entry.name);
			out.writeInt(entry.nbtLength);
			out.writeInt(entry.dataVersion);
			out.writeLong(entry.authorUuid.getMostSignificantBits());
			out.writeLong(entry.authorUuid.getLeastSignificantBits());
			out.writeUTF(entry.authorUsername);
			out.writeLong(entry.created);
			out.writeLong(entry.modified);
			out.writeUTF(entry.hash);
			out.writeBoolean(entry.verified);
		}
	}
	
}
