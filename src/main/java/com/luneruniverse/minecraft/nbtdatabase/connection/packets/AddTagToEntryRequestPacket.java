package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class AddTagToEntryRequestPacket extends Packet {
	
	private final long entry;
	private final String tag;
	
	public AddTagToEntryRequestPacket(long entry, String tag) {
		this.entry = entry;
		this.tag = tag;
	}
	public AddTagToEntryRequestPacket(DataInputStream in) throws IOException {
		this.entry = in.readLong();
		this.tag = in.readUTF();
	}
	
	public long getEntry() {
		return entry;
	}
	public String getTag() {
		return tag;
	}
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeLong(entry);
		out.writeUTF(tag);
	}
	
}
