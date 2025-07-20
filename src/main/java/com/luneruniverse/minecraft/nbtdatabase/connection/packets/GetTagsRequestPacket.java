package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.TagFilter;

public class GetTagsRequestPacket extends Packet {
	
	private @NotNull TagFilter filter;
	
	public GetTagsRequestPacket(TagFilter filter) {
		this.filter = filter;
	}
	GetTagsRequestPacket() {
		// Deserialization
	}
	
	public TagFilter getFilter() {
		return filter;
	}
	
}
