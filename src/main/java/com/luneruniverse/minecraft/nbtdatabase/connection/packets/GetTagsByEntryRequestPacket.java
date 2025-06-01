package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class GetTagsByEntryRequestPacket extends Packet {
	
	private final long entry;
	
	public GetTagsByEntryRequestPacket(long entry) {
		this.entry = entry;
	}
	public GetTagsByEntryRequestPacket(DataInputStream in) throws IOException {
		this.entry = in.readLong();
	}
	
	public long getEntry() {
		return entry;
	}
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeLong(entry);
	}
	
}
