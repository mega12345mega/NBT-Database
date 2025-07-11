package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import com.luneruniverse.simplepacketlibrary.PacketRegistry;

public class Packets {
	
	public static final PacketRegistry PACKETS = new PacketRegistry();
	static {
		// Client -> Server
		PACKETS.registerPacket(GetConfigRequestPacket.class);
		PACKETS.registerPacket(AddEntryRequestPacket.class);
		PACKETS.registerPacket(EditEntryRequestPacket.class);
		PACKETS.registerPacket(RemoveEntryRequestPacket.class);
		PACKETS.registerPacket(GetEntryRequestPacket.class);
		PACKETS.registerPacket(GetEntryNBTRequestPacket.class);
		PACKETS.registerPacket(GetEntriesRequestPacket.class);
		PACKETS.registerPacket(AddTagRequestPacket.class);
		PACKETS.registerPacket(EditTagRequestPacket.class);
		PACKETS.registerPacket(RemoveTagRequestPacket.class);
		PACKETS.registerPacket(GetTagRequestPacket.class);
		PACKETS.registerPacket(GetTagsRequestPacket.class);
		PACKETS.registerPacket(AddTagToEntryRequestPacket.class);
		PACKETS.registerPacket(RemoveTagFromEntryRequestPacket.class);
		
		// Server -> Client
		PACKETS.registerPacket(ConfigPacket.class);
		PACKETS.registerPacket(EntriesPacket.class);
		PACKETS.registerPacket(EntryNBTPacket.class);
		PACKETS.registerPacket(TagsPacket.class);
	}
	
}
