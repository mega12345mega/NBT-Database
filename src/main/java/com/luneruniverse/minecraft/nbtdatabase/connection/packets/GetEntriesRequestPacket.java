package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import com.luneruniverse.minecraft.nbtdatabase.EntryFilter;
import com.luneruniverse.minecraft.nbtdatabase.EntryView;
import com.luneruniverse.minecraft.nbtdatabase.EntryView.Order;
import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class GetEntriesRequestPacket extends Packet {
	
	private final EntryFilter filter;
	private final EntryView view;
	
	public GetEntriesRequestPacket(EntryFilter filter, EntryView view) {
		this.filter = filter;
		this.view = view;
	}
	public GetEntriesRequestPacket(DataInputStream in) throws IOException {
		this.filter = new EntryFilter(
				in.readBoolean() ? in.readUTF() : null,
				in.readBoolean() ? in.readInt() : null,
				in.readBoolean() ? in.readInt() : null,
				in.readBoolean() ? new UUID(in.readLong(), in.readLong()) : null,
				in.readBoolean() ? in.readUTF() : null,
				null);
		for (int i = 0, numTags = in.readInt(); i < numTags; i++)
			filter.filterByTag(in.readUTF());
		this.view = new EntryView(Order.values()[in.readByte()], in.readBoolean(), in.readInt());
	}
	
	public EntryFilter getFilter() {
		return filter;
	}
	public EntryView getView() {
		return view;
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
		
		out.writeBoolean(filter.getAuthorUsername() != null);
		if (filter.getAuthorUsername() != null)
			out.writeUTF(filter.getAuthorUsername());
		
		out.writeInt(filter.getTags() == null ? 0 : filter.getTags().size());
		if (filter.getTags() != null) {
			for (String tag : filter.getTags())
				out.writeUTF(tag);
		}
		
		out.writeByte(view.getOrder().ordinal());
		out.writeBoolean(view.isReversedOrder());
		out.writeInt(view.getOffset());
	}
	
}
