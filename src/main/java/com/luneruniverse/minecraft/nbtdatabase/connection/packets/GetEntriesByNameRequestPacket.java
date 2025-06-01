package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class GetEntriesByNameRequestPacket extends Packet {
	
	private final String query;
	
	public GetEntriesByNameRequestPacket(String query) {
		this.query = query;
	}
	public GetEntriesByNameRequestPacket(DataInputStream in) throws IOException {
		this.query = in.readUTF();
	}
	
	public String getQuery() {
		return query;
	}
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeUTF(query);
	}
	
}
