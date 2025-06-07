package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import com.luneruniverse.minecraft.nbtdatabase.EntryFilter;
import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class GetEntriesRequestPacket extends Packet {
	
	private final EntryFilter filter;
	
	public GetEntriesRequestPacket(EntryFilter filter) {
		this.filter = filter;
	}
	public GetEntriesRequestPacket(DataInputStream in) throws IOException {
		this.filter = new EntryFilter(
				in.readBoolean() ? in.readUTF() : null,
				in.readBoolean() ? in.readInt() : null,
				in.readBoolean() ? in.readInt() : null,
				in.readBoolean() ? new UUID(in.readLong(), in.readLong()) : null,
				in.readBoolean() ? in.readUTF() : null);
	}
	
	public EntryFilter getFilter() {
		return filter;
	}
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeBoolean(filter.getName() != null);
		if (filter.getName() != null)
			out.writeUTF(filter.getName());
		
		out.writeBoolean(filter.getMinDataVersion() != null);
		if (filter.getMinDataVersion() != null)
			out.writeInt(filter.getMinDataVersion());
		
		out.writeBoolean(filter.getMaxDataVersion() != null);
		if (filter.getMaxDataVersion() != null)
			out.writeInt(filter.getMaxDataVersion());
		
		out.writeBoolean(filter.getAuthorUuid() != null);
		if (filter.getAuthorUuid() != null) {
			out.writeLong(filter.getAuthorUuid().getMostSignificantBits());
			out.writeLong(filter.getAuthorUuid().getLeastSignificantBits());
		}
		
		out.writeBoolean(filter.getAuthorName() != null);
		if (filter.getAuthorName() != null)
			out.writeUTF(filter.getAuthorName());
	}
	
}
