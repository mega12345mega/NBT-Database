package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.request.EntryFilter;
import com.luneruniverse.minecraft.nbtdatabase.request.EntryView;

public class GetEntriesRequestPacket extends Packet {
	
	private @NotNull EntryFilter filter;
	private @NotNull EntryView view;
	
	public GetEntriesRequestPacket(EntryFilter filter, EntryView view) {
		this.filter = filter;
		this.view = view;
	}
	GetEntriesRequestPacket() {
		// Deserialization
	}
	
	public EntryFilter getFilter() {
		return filter;
	}
	public EntryView getView() {
		return view;
	}
	
}
