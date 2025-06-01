package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import com.luneruniverse.simplepacketlibrary.PacketRegistry;

public class Packets {
	
	public static final PacketRegistry PACKETS = new PacketRegistry();
	static {
		// Client -> Server
		PACKETS.registerPacket(MetadataRequestPacket.class);
		PACKETS.registerPacket(AddEntryRequestPacket.class);
		PACKETS.registerPacket(RemoveEntryRequestPacket.class);
		PACKETS.registerPacket(GetEntryRequestPacket.class);
		PACKETS.registerPacket(GetEntriesRequestPacket.class);
		PACKETS.registerPacket(GetEntriesByNameRequestPacket.class);
		PACKETS.registerPacket(GetEntriesByAuthorUUIDRequestPacket.class);
		PACKETS.registerPacket(GetEntriesByAuthorNameRequestPacket.class);
		PACKETS.registerPacket(AddTagRequestPacket.class);
		PACKETS.registerPacket(RemoveTagRequestPacket.class);
		PACKETS.registerPacket(GetTagsRequestPacket.class);
		PACKETS.registerPacket(AddTagToEntryRequestPacket.class);
		PACKETS.registerPacket(RemoveTagFromEntryRequestPacket.class);
		PACKETS.registerPacket(GetTagsByEntryRequestPacket.class);
		PACKETS.registerPacket(GetEntriesByTagRequestPacket.class);
		
		// Server -> Client
		PACKETS.registerPacket(MetadataPacket.class);
		PACKETS.registerPacket(EntriesPacket.class);
		PACKETS.registerPacket(TagsPacket.class);
	}
	
}
