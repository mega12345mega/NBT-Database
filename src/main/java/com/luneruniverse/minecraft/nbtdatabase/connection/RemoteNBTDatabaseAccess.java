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

import com.luneruniverse.minecraft.nbtdatabase.Config;
import com.luneruniverse.minecraft.nbtdatabase.EntryFilter;
import com.luneruniverse.minecraft.nbtdatabase.EntryView;
import com.luneruniverse.minecraft.nbtdatabase.NBTEntry;
import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.TagFilter;
import com.luneruniverse.minecraft.nbtdatabase.Util;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.AddEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.AddTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.AddTagToEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.ConfigPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.EditEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.EditTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.EntriesPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.EntryNBTPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetConfigRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetEntriesRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetEntryNBTRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetTagsRequestPacket;
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
	
	@Override
	public String getName() {
		return "[Remote] " + client.getIp() + ":" + client.getPort();
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
					throw new ServerException(((PrimitivePacket) response).getValue().toString());
				if (response == null)
					throw new RequestFailedException("Request timed out");
				throw new RequestFailedException("Request response was invalid");
			} catch (IOException e) {
				throw new RequestFailedException("Error while sending request", e);
			} catch (InterruptedException e) {
				throw new RequestFailedException("Request interrupted", e);
			}
		}, executor);
	}
	private <T, P> CompletableFuture<T> request(Packet packet, Class<P> responsePacketType, Function<P, T> unpacker) {
		return requestOptionalResponse(packet, responsePacketType, unpacker.andThen(Optional::of));
	}
	
	private CompletableFuture<Void> requestVoid(Packet packet) {
		return Util.thenApply(requestOptionalResponse(packet, PrimitivePacket.class, response -> response.isBoolean() &&
				(boolean) response.getValue() ? Optional.of(true) : Optional.empty()), success -> null);
	}
	
	@Override
	public CompletableFuture<Void> setConfig(Config config) {
		return requestVoid(new ConfigPacket(config));
	}
	
	@Override
	public CompletableFuture<Config> getConfig() {
		return request(new GetConfigRequestPacket(), ConfigPacket.class, ConfigPacket::getConfig);
	}
	
	@Override
	public CompletableFuture<Long> addEntry(String name, byte[] nbt, int dataVersion, UUID authorUuid, String authorUsername, boolean verified) {
		return requestOptionalResponse(new AddEntryRequestPacket(name, nbt, dataVersion, authorUuid, authorUsername, verified),
				PrimitivePacket.class, response -> response.isLong() ? Optional.of((long) response.getValue()) : Optional.empty());
	}
	
	@Override
	public CompletableFuture<Void> editEntry(long id, Optional<String> name, Optional<byte[]> nbt, Optional<Integer> dataVersion,
			Optional<UUID> authorUuid, Optional<String> authorUsername, Optional<Boolean> verified) {
		return requestVoid(new EditEntryRequestPacket(id, name, nbt, dataVersion, authorUuid, authorUsername, verified));
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
	public CompletableFuture<byte[]> getEntryNBT(long id) {
		return request(new GetEntryNBTRequestPacket(id), EntryNBTPacket.class, EntryNBTPacket::getNBT);
	}
	
	@Override
	public CompletableFuture<List<NBTEntry>> getEntries(EntryFilter filter, EntryView view) {
		return request(new GetEntriesRequestPacket(filter, view), EntriesPacket.class, EntriesPacket::getEntriesList);
	}
	
	@Override
	public CompletableFuture<Void> addTag(String name, int color) {
		return requestVoid(new AddTagRequestPacket(name, color));
	}
	
	@Override
	public CompletableFuture<Void> editTag(String currentName, Optional<String> name, Optional<Integer> color) {
		return requestVoid(new EditTagRequestPacket(currentName, name, color));
	}
	
	@Override
	public CompletableFuture<Void> removeTag(String name) {
		return requestVoid(new RemoveTagRequestPacket(name));
	}
	
	@Override
	public CompletableFuture<Tag> getTag(String name) {
		return request(new GetTagRequestPacket(name), TagsPacket.class, TagsPacket::getTagNullable);
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
