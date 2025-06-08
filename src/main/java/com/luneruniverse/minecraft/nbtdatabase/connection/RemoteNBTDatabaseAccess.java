package com.luneruniverse.minecraft.nbtdatabase.connection;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.luneruniverse.minecraft.nbtdatabase.EntryFilter;
import com.luneruniverse.minecraft.nbtdatabase.NBTEntry;
import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.TagFilter;
import com.luneruniverse.minecraft.nbtdatabase.Util;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.AddEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.AddTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.AddTagToEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.EntriesPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetEntriesRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetTagsRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.MetadataPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.MetadataRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packets;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.RemoveEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.RemoveTagFromEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.RemoveTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.TagsPacket;
import com.luneruniverse.simplepacketlibrary.Client;
import com.luneruniverse.simplepacketlibrary.packets.Packet;
import com.luneruniverse.simplepacketlibrary.packets.PrimitivePacket;

public class RemoteNBTDatabaseAccess implements NBTDatabaseAccess {
	
	private final Client client;
	private final ExecutorService executor;
	
	public RemoteNBTDatabaseAccess(String ip, int port) throws IOException {
		client = new Client(ip, port);
		client.addErrorHandler((e, client, context) -> e.printStackTrace());
		client.registerPackets(Packets.PACKETS);
		client.start();
		
		executor = Executors.newSingleThreadExecutor();
	}
	
	private <T, P> CompletableFuture<T> requestOptionalResponse(Packet packet, Class<P> responsePacketType, Function<P, Optional<T>> unpacker) {
		return Util.supplyAsync(() -> {
			try {
				Packet response = client.sendPacketWithResponse(packet);
				if (responsePacketType.isInstance(response)) {
					Optional<T> value = unpacker.apply(responsePacketType.cast(response));
					if (value.isPresent())
						return value.get();
				}
				if (response instanceof PrimitivePacket)
					throw new RuntimeException(((PrimitivePacket) response).getValue().toString());
				if (response == null)
					throw new RuntimeException("Request timed out");
				throw new RuntimeException("Request response was invalid");
			} catch (IOException e) {
				throw new RuntimeException("Error while sending request", e);
			} catch (InterruptedException e) {
				throw new RuntimeException("Request interrupted", e);
			}
		}, executor);
	}
	private <T, P> CompletableFuture<T> request(Packet packet, Class<P> responsePacketType, Function<P, T> unpacker) {
		return requestOptionalResponse(packet, responsePacketType, unpacker.andThen(Optional::of));
	}
	
	private CompletableFuture<Void> requestVoid(Packet packet) {
		return requestOptionalResponse(packet, PrimitivePacket.class, response -> response.isBoolean() && (boolean) response.getValue() ? Optional.of(true) : Optional.empty()).<Void>thenApply(success -> null);
	}
	
	@Override
	public CompletableFuture<NBTDatabaseMetadata> getMetadata() {
		return request(new MetadataRequestPacket(), MetadataPacket.class, MetadataPacket::getMetadata);
	}
	
	@Override
	public CompletableFuture<Long> addEntry(String name, byte[] nbt, int dataVersion, UUID authorUuid, String authorUsername, boolean verified) {
		return requestOptionalResponse(new AddEntryRequestPacket(name, nbt, dataVersion, authorUuid, authorUsername, verified), PrimitivePacket.class, response -> response.isLong() ? Optional.of((long) response.getValue()) : Optional.empty());
	}
	
	@Override
	public CompletableFuture<Void> removeEntry(long id) {
		return requestVoid(new RemoveEntryRequestPacket(id));
	}
	
	@Override
	public CompletableFuture<NBTEntry> getEntry(long id) {
		return request(new GetEntryRequestPacket(id), EntriesPacket.class, EntriesPacket::getEntryNullable);
	}
	
	@Override
	public CompletableFuture<List<NBTEntry>> getEntries(EntryFilter filter) {
		return request(new GetEntriesRequestPacket(filter), EntriesPacket.class, EntriesPacket::getEntriesList);
	}
	
	@Override
	public CompletableFuture<Void> addTag(String name, int color) {
		return requestVoid(new AddTagRequestPacket(name, color));
	}
	
	@Override
	public CompletableFuture<Void> removeTag(String name) {
		return requestVoid(new RemoveTagRequestPacket(name));
	}
	
	@Override
	public CompletableFuture<List<Tag>> getTags(TagFilter filter) {
		return request(new GetTagsRequestPacket(filter), TagsPacket.class, TagsPacket::getTagsList);
	}
	
	@Override
	public CompletableFuture<Void> addTagToEntry(long entry, String tag) {
		return requestVoid(new AddTagToEntryRequestPacket(entry, tag));
	}
	
	@Override
	public CompletableFuture<Void> removeTagFromEntry(long entry, String tag) {
		return requestVoid(new RemoveTagFromEntryRequestPacket(entry, tag));
	}
	
	@Override
	public CompletableFuture<Void> closeAsync() {
		return Util.finallyDo(Util.shutdown(executor), client::close);
	}
	
	@Override
	public void close() throws IOException, InterruptedException {
		try {
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} finally {
			client.close();
		}
	}
	
}
