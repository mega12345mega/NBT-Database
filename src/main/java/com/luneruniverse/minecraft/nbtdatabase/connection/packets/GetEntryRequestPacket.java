package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class GetEntryRequestPacket extends Packet {
	
	private final long id;
	
	public GetEntryRequestPacket(long id) {
		this.id = id;
	}
	public GetEntryRequestPacket(DataInputStream in) throws IOException {
		this.id = in.readLong();
	}
	
	public long getId() {
		return id;
	}
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeLong(id);
	}
	
}
