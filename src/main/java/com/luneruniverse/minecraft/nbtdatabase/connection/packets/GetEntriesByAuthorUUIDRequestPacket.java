package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class GetEntriesByAuthorUUIDRequestPacket extends Packet {
	
	private final UUID query;
	
	public GetEntriesByAuthorUUIDRequestPacket(UUID query) {
		this.query = query;
	}
	public GetEntriesByAuthorUUIDRequestPacket(DataInputStream in) throws IOException {
		this.query = new UUID(in.readLong(), in.readLong());
	}
	
	public UUID getQuery() {
		return query;
	}
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeLong(query.getMostSignificantBits());
		out.writeLong(query.getLeastSignificantBits());
	}
	
}
