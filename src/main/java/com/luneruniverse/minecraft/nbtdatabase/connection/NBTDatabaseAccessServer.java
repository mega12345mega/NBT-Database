package com.luneruniverse.minecraft.nbtdatabase.connection;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.luneruniverse.minecraft.nbtdatabase.connection.packets.AddEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.AddTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.AddTagToEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.EntriesPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetEntriesByAuthorNameRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetEntriesByAuthorUUIDRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetEntriesByNameRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetEntriesByTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetEntriesRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetTagsByEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetTagsRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.MetadataPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.MetadataRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packets;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.RemoveEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.RemoveTagFromEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.RemoveTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.TagsPacket;
import com.luneruniverse.simplepacketlibrary.Connection;
import com.luneruniverse.simplepacketlibrary.Server;
import com.luneruniverse.simplepacketlibrary.listeners.TypedPacketListener;
import com.luneruniverse.simplepacketlibrary.listeners.WaitState;
import com.luneruniverse.simplepacketlibrary.packets.Packet;
import com.luneruniverse.simplepacketlibrary.packets.PrimitivePacket;

public class NBTDatabaseAccessServer implements AutoCloseable {
	
	private final NBTDatabaseAccess database;
	private final Server server;
	
	public NBTDatabaseAccessServer(NBTDatabaseAccess database, int port) throws IOException {
		this.database = database;
		
		server = new Server(port);
		server.addServerErrorHandler((e, server, context) -> e.printStackTrace());
		server.addConnectionErrorHandler((e, conn, context) -> e.printStackTrace());
		server.registerPackets(Packets.PACKETS);
		
		server.addPacketListener(new TypedPacketListener()
				.when(MetadataRequestPacket.class, this::metadataRequestPacket)
				.when(AddEntryRequestPacket.class, this::addEntryRequestPacket)
				.when(RemoveEntryRequestPacket.class, this::removeEntryRequestPacket)
				.when(GetEntryRequestPacket.class, this::getEntryRequestPacket)
				.when(GetEntriesRequestPacket.class, this::getEntriesRequestPacket)
				.when(GetEntriesByNameRequestPacket.class, this::getEntriesByNameRequestPacket)
				.when(GetEntriesByAuthorUUIDRequestPacket.class, this::getEntriesByAuthorUUIDRequestPacket)
				.when(GetEntriesByAuthorNameRequestPacket.class, this::getEntriesByAuthorNameRequestPacket)
				.when(AddTagRequestPacket.class, this::addTagRequestPacket)
				.when(RemoveTagRequestPacket.class, this::removeTagRequestPacket)
				.when(GetTagsRequestPacket.class, this::getTagsRequestPacket)
				.when(AddTagToEntryRequestPacket.class, this::addTagToEntryRequestPacket)
				.when(RemoveTagFromEntryRequestPacket.class, this::removeTagFromEntryRequestPacket)
				.when(GetTagsByEntryRequestPacket.class, this::getTagsByEntryRequestPacket)
				.when(GetEntriesByTagRequestPacket.class, this::getEntriesByTagRequestPacket));
		
		server.start();
	}
	
	private <T> void respond(Packet packet, Connection conn, CompletableFuture<T> request, Function<T, Packet> packer) {
		request.whenComplete((value, e) -> {
			try {
				if (e != null) {
					e.printStackTrace();
					conn.reply(packet, new PrimitivePacket(e.toString()));
				} else
					conn.reply(packet, packer.apply(value));
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		});
	}
	
	private void respondVoid(Packet packet, Connection conn, CompletableFuture<Void> request) {
		respond(packet, conn, request, v -> new PrimitivePacket(true));
	}
	
	private void metadataRequestPacket(MetadataRequestPacket packet, Connection conn, WaitState wait) {
		respond(packet, conn, database.getMetadata(), MetadataPacket::new);
	}
	
	private void addEntryRequestPacket(AddEntryRequestPacket packet, Connection conn, WaitState wait) {
		respond(packet, conn, database.addEntry(packet.getName(), packet.getNbt(), packet.getDataVersion(), packet.getAuthorUuid(), packet.getAuthorUsername(), packet.isVerified()), PrimitivePacket::new);
	}
	
	private void removeEntryRequestPacket(RemoveEntryRequestPacket packet, Connection conn, WaitState wait) {
		respondVoid(packet, conn, database.removeEntry(packet.getId()));
	}
	
	private void getEntryRequestPacket(GetEntryRequestPacket packet, Connection conn, WaitState wait) {
		respond(packet, conn, database.getEntry(packet.getId()), EntriesPacket::new);
	}
	
	private void getEntriesRequestPacket(GetEntriesRequestPacket packet, Connection conn, WaitState wait) {
		respond(packet, conn, database.getEntries(), EntriesPacket::new);
	}
	
	private void getEntriesByNameRequestPacket(GetEntriesByNameRequestPacket packet, Connection conn, WaitState wait) {
		respond(packet, conn, database.getEntriesByName(packet.getQuery()), EntriesPacket::new);
	}
	
	private void getEntriesByAuthorUUIDRequestPacket(GetEntriesByAuthorUUIDRequestPacket packet, Connection conn, WaitState wait) {
		respond(packet, conn, database.getEntriesByAuthorUUID(packet.getQuery()), EntriesPacket::new);
	}
	
	private void getEntriesByAuthorNameRequestPacket(GetEntriesByAuthorNameRequestPacket packet, Connection conn, WaitState wait) {
		respond(packet, conn, database.getEntriesByAuthorName(packet.getQuery()), EntriesPacket::new);
	}
	
	private void addTagRequestPacket(AddTagRequestPacket packet, Connection conn, WaitState wait) {
		respondVoid(packet, conn, database.addTag(packet.getName(), packet.getColor()));
	}
	
	private void removeTagRequestPacket(RemoveTagRequestPacket packet, Connection conn, WaitState wait) {
		respondVoid(packet, conn, database.removeTag(packet.getName()));
	}
	
	private void getTagsRequestPacket(GetTagsRequestPacket packet, Connection conn, WaitState wait) {
		respond(packet, conn, database.getTags(), TagsPacket::new);
	}
	
	private void addTagToEntryRequestPacket(AddTagToEntryRequestPacket packet, Connection conn, WaitState wait) {
		respondVoid(packet, conn, database.addTagToEntry(packet.getEntry(), packet.getTag()));
	}
	
	private void removeTagFromEntryRequestPacket(RemoveTagFromEntryRequestPacket packet, Connection conn, WaitState wait) {
		respondVoid(packet, conn, database.removeTagFromEntry(packet.getEntry(), packet.getTag()));
	}
	
	private void getTagsByEntryRequestPacket(GetTagsByEntryRequestPacket packet, Connection conn, WaitState wait) {
		respond(packet, conn, database.getTagsByEntry(packet.getEntry()), TagsPacket::new);
	}
	
	private void getEntriesByTagRequestPacket(GetEntriesByTagRequestPacket packet, Connection conn, WaitState wait) {
		respond(packet, conn, database.getEntriesByTag(packet.getTag()), EntriesPacket::new);
	}
	
	@Override
	public void close() throws InterruptedException, IOException {
		server.close();
	}
	
}
