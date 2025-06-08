package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.luneruniverse.minecraft.nbtdatabase.TagFilter;
import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class GetTagsRequestPacket extends Packet {
	
	private final TagFilter filter;
	
	public GetTagsRequestPacket(TagFilter filter) {
		this.filter = filter;
	}
	public GetTagsRequestPacket(DataInputStream in) throws IOException {
		this.filter = new TagFilter(in.readBoolean() ? in.readUTF() : null, in.readBoolean() ? in.readLong() : null);
	}
	
	public TagFilter getFilter() {
		return filter;
	}
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeBoolean(filter.getName() != null);
		if (filter.getName() != null)
			out.writeUTF(filter.getName());
		
		out.writeBoolean(filter.getEntryId() != null);
		if (filter.getEntryId() != null)
			out.writeLong(filter.getEntryId());
	}
	
}
