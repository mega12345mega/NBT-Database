package com.luneruniverse.minecraft.nbtdatabase.connection;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

import com.luneruniverse.minecraft.nbtdatabase.IllegalRequestException;
import com.luneruniverse.minecraft.nbtdatabase.Util;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.AddEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.AddTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.AddTagToEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.ConfigPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.EditEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.EditTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.EntriesPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetConfigRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetEntriesRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetTagsRequestPacket;
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
				.when(ConfigPacket.class, this::configPacket)
				.when(GetConfigRequestPacket.class, this::getConfigRequestPacket)
				.when(AddEntryRequestPacket.class, this::addEntryRequestPacket)
				.when(EditEntryRequestPacket.class, this::editEntryRequestPacket)
				.when(RemoveEntryRequestPacket.class, this::removeEntryRequestPacket)
				.when(GetEntryRequestPacket.class, this::getEntryRequestPacket)
				.when(GetEntriesRequestPacket.class, this::getEntriesRequestPacket)
				.when(AddTagRequestPacket.class, this::addTagRequestPacket)
				.when(EditTagRequestPacket.class, this::editTagRequestPacket)
				.when(RemoveTagRequestPacket.class, this::removeTagRequestPacket)
				.when(GetTagRequestPacket.class, this::getTagRequestPacket)
				.when(GetTagsRequestPacket.class, this::getTagsRequestPacket)
				.when(AddTagToEntryRequestPacket.class, this::addTagToEntryRequestPacket)
				.when(RemoveTagFromEntryRequestPacket.class, this::removeTagFromEntryRequestPacket));
		
		server.start();
	}
	
	public int getPort() {
		return server.getPort();
	}
	
	private <T> void respond(Packet packet, Connection conn, CompletableFuture<T> request, Function<T, Packet> packer) {
		request.whenComplete((value, e) -> {
			try {
				if (e != null) {
					if (e instanceof IllegalRequestException)
						conn.reply(packet, new PrimitivePacket(e.getMessage()));
					else {
						e.printStackTrace();
						conn.reply(packet, new PrimitivePacket("An internal server error occurred"));
					}
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
	
	private void configPacket(ConfigPacket packet, Connection conn, WaitState wait) {
		respondVoid(packet, conn, database.setConfig(packet.getConfig()));
	}
	
	private void getConfigRequestPacket(GetConfigRequestPacket packet, Connection conn, WaitState wait) {
		respond(packet, conn, database.getConfig(), ConfigPacket::new);
	}
	
	private void addEntryRequestPacket(AddEntryRequestPacket packet, Connection conn, WaitState wait) {
		respond(packet, conn, database.addEntry(packet.getName(), packet.getNbt(), packet.getDataVersion(),
				packet.getAuthorUuid(), packet.getAuthorUsername(), packet.isVerified()), PrimitivePacket::new);
	}
	
	private void editEntryRequestPacket(EditEntryRequestPacket packet, Connection conn, WaitState wait) {
		respondVoid(packet, conn, database.editEntry(packet.getId(), packet.getName(), packet.getNbt(),
				packet.getDataVersion(), packet.getAuthorUuid(), packet.getAuthorUsername(), packet.isVerified()));
	}
	
	private void removeEntryRequestPacket(RemoveEntryRequestPacket packet, Connection conn, WaitState wait) {
		respondVoid(packet, conn, database.removeEntry(packet.getId()));
	}
	
	private void getEntryRequestPacket(GetEntryRequestPacket packet, Connection conn, WaitState wait) {
		respond(packet, conn, database.getEntry(packet.getId()), EntriesPacket::new);
	}
	
	private void getEntriesRequestPacket(GetEntriesRequestPacket packet, Connection conn, WaitState wait) {
		respond(packet, conn, database.getEntries(packet.getFilter(), packet.getView()), EntriesPacket::new);
	}
	
	private void addTagRequestPacket(AddTagRequestPacket packet, Connection conn, WaitState wait) {
		respondVoid(packet, conn, database.addTag(packet.getName(), packet.getColor()));
	}
	
	private void editTagRequestPacket(EditTagRequestPacket packet, Connection conn, WaitState wait) {
		respondVoid(packet, conn, database.editTag(packet.getCurrentName(), packet.getName(), packet.getColor()));
	}
	
	private void removeTagRequestPacket(RemoveTagRequestPacket packet, Connection conn, WaitState wait) {
		respondVoid(packet, conn, database.removeTag(packet.getName()));
	}
	
	private void getTagRequestPacket(GetTagRequestPacket packet, Connection conn, WaitState wait) {
		respond(packet, conn, database.getTag(packet.getName()), TagsPacket::new);
	}
	
	private void getTagsRequestPacket(GetTagsRequestPacket packet, Connection conn, WaitState wait) {
		respond(packet, conn, database.getTags(packet.getFilter()), TagsPacket::new);
	}
	
	private void addTagToEntryRequestPacket(AddTagToEntryRequestPacket packet, Connection conn, WaitState wait) {
		respondVoid(packet, conn, database.addTagToEntry(packet.getEntry(), packet.getTag()));
	}
	
	private void removeTagFromEntryRequestPacket(RemoveTagFromEntryRequestPacket packet, Connection conn, WaitState wait) {
		respondVoid(packet, conn, database.removeTagFromEntry(packet.getEntry(), packet.getTag()));
	}
	
	public CompletableFuture<Void> closeAsync() {
		return Util.runAsync(server::close, ForkJoinPool.commonPool());
	}
	
	@Override
	public void close() throws IOException, InterruptedException {
		server.close();
	}
	
}
