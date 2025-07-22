package com.luneruniverse.minecraft.nbtdatabase.connection.access;

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
import com.luneruniverse.minecraft.nbtdatabase.Entry;
import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.connection.RequestFailedException;
import com.luneruniverse.minecraft.nbtdatabase.connection.ServerException;
import com.luneruniverse.minecraft.nbtdatabase.connection.netty.ErrorHandler;
import com.luneruniverse.minecraft.nbtdatabase.connection.netty.NBTProtocol;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.AddEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.AddTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.AddTagToEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.ConfigPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.EditEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.EditTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.EntriesPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.EntryIdPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.EntryNBTPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetConfigRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetEntriesRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetEntryNBTRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.GetTagsRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.RemoveEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.RemoveTagFromEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.RemoveTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.ServerExceptionPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.SuccessPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.TagsPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.FutureUtil;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.NettyUtil;
import com.luneruniverse.minecraft.nbtdatabase.request.EntryFilter;
import com.luneruniverse.minecraft.nbtdatabase.request.EntryView;
import com.luneruniverse.minecraft.nbtdatabase.request.TagFilter;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class RemoteNBTDatabaseAccess implements NBTDatabaseAccess {
	
	private final String ip;
	private final int port;
	private final Channel client;
	private final ExecutorService executor;
	
	public RemoteNBTDatabaseAccess(String ip, int port) throws IOException, InterruptedException {
		this.ip = ip;
		this.port = port;
		
		EventLoopGroup group = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
		ChannelFuture clientFuture = new Bootstrap()
				.group(group)
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel channel) throws Exception {
						channel.pipeline().addLast("dummy", new ChannelHandlerAdapter() {});
						NBTProtocol.bind(channel.pipeline().context("dummy"));
						channel.pipeline().remove("dummy");
						channel.pipeline().addLast(new ErrorHandler());
					}
				})
				.connect(ip, port);
		client = NettyUtil.addGroupShutdown(clientFuture, group);
		NBTProtocol.sendMagic(client);
		
		executor = Executors.newSingleThreadExecutor();
	}
	
	@Override
	public String getName() {
		return "[Remote] " + ip + ":" + port;
	}
	
	private <T, P> CompletableFuture<T> request(Packet packet, Class<P> responsePacketType, Function<P, T> unpacker) {
		return FutureUtil.supplyAsync(() -> {
			try {
				Packet response = NBTProtocol.sendAndGetResponse(client, packet);
				if (responsePacketType.isInstance(response))
					return unpacker.apply(responsePacketType.cast(response));
				if (response instanceof ServerExceptionPacket)
					throw new ServerException(((ServerExceptionPacket) response).getMessage());
				if (response == null)
					throw new RequestFailedException("Request timed out");
				throw new RequestFailedException("Request response was invalid");
			} catch (InterruptedException e) {
				throw new RequestFailedException("Request interrupted", e);
			}
		}, executor);
	}
	
	private CompletableFuture<Void> requestVoid(Packet packet) {
		return request(packet, SuccessPacket.class, success -> null);
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
		return request(new AddEntryRequestPacket(name, nbt, dataVersion, authorUuid, authorUsername, verified), EntryIdPacket.class, EntryIdPacket::getId);
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
	public CompletableFuture<Entry> getEntry(long id) {
		return request(new GetEntryRequestPacket(id), EntriesPacket.class, EntriesPacket::getEntryNullable);
	}
	
	@Override
	public CompletableFuture<byte[]> getEntryNBT(long id) {
		return request(new GetEntryNBTRequestPacket(id), EntryNBTPacket.class, EntryNBTPacket::getNBT);
	}
	
	@Override
	public CompletableFuture<List<Entry>> getEntries(EntryFilter filter, EntryView view) {
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
		return FutureUtil.finallyDo(FutureUtil.shutdown(executor), client::close);
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
