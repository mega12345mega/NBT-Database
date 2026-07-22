package com.luneruniverse.minecraft.nbtdatabase.connection.server;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.luneruniverse.minecraft.nbtdatabase.connection.AsyncCloseable;
import com.luneruniverse.minecraft.nbtdatabase.connection.access.NBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.AuthorizationServerException;
import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.ServerException;
import com.luneruniverse.minecraft.nbtdatabase.connection.netty.ExceptionHandler;
import com.luneruniverse.minecraft.nbtdatabase.connection.netty.NBTProtocol;
import com.luneruniverse.minecraft.nbtdatabase.connection.netty.ProtocolVersionHandler;
import com.luneruniverse.minecraft.nbtdatabase.connection.netty.ServerLoginHandler;
import com.luneruniverse.minecraft.nbtdatabase.connection.netty.TypedPacketHandler;
import com.luneruniverse.minecraft.nbtdatabase.connection.netty.WebSocketNBTProtocol;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.SuccessPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.config.ConfigPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.config.GetConfigRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.config.LockConfigRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.config.SetConfigRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.config.UnlockConfigRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.AddEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.EditEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.EntriesPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.EntryIdPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.EntryNBTPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.GetEntriesRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.GetEntryNBTRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.GetEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.LockEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.RemoveEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.entries.UnlockEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.exceptions.InternalServerExceptionPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.AddTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.AddTagToEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.EditTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.GetTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.GetTagsRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.LockTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.RemoveTagFromEntryRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.RemoveTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.TagsPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags.UnlockTagRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.AuthorizationCheck;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.AuthorizationManager;
import com.luneruniverse.minecraft.nbtdatabase.connection.user.ClientType;
import com.luneruniverse.minecraft.nbtdatabase.connection.user.User;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.FutureUtil;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.NettyUtil;
import com.luneruniverse.minecraft.nbtdatabase.ui.website.WebsiteHandler;
import com.luneruniverse.nettymux.byteprotocol.HttpByteProtocol;
import com.luneruniverse.nettymux.byteprotocol.MagicByteProtocol;
import com.luneruniverse.nettymux.byteprotocol.NettyByteMultiplexer;
import com.luneruniverse.nettymux.messageprotocol.NettyMessageMultiplexer;
import com.luneruniverse.nettymux.messageprotocol.NormalHttpMessageProtocol;
import com.luneruniverse.nettymux.messageprotocol.WebSocketHttpMessageProtocol;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class NBTDatabaseAccessServer implements AsyncCloseable {
	
	private final NBTDatabaseAccess database;
	private final int port;
	private final AuthorizationManager auth;
	private final Lock configLock;
	private final LockCacheMap<Long> entryLocks;
	private final LockCacheMap<String> tagLocks;
	private final Map<Channel, User> users;
	private final CompletableFuture<Void> closeFuture;
	private final Channel server;
	
	public NBTDatabaseAccessServer(NBTDatabaseAccess database, int port, AuthorizationManager auth) throws IOException {
		this.database = database;
		this.port = port;
		this.auth = auth;
		
		configLock = Lock.forConfig(database);
		entryLocks = LockCacheMap.forEntries(database);
		tagLocks = LockCacheMap.forTags(database);
		
		users = new HashMap<>();
		
		closeFuture = new CompletableFuture<>();
		
		KeyPairGenerator keysGenerator;
		try {
			keysGenerator = KeyPairGenerator.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("Failed to initialize encryption", e);
		}
		keysGenerator.initialize(1024);
		KeyPair keys = keysGenerator.generateKeyPair();
		
		TypedPacketHandler nbtHandler = new TypedPacketHandler()
				.when(LockConfigRequestPacket.class, this::lockConfigRequestPacket)
				.when(UnlockConfigRequestPacket.class, this::unlockConfigRequestPacket)
				.when(SetConfigRequestPacket.class, this::setConfigRequestPacket)
				.when(GetConfigRequestPacket.class, this::getConfigRequestPacket)
				.when(LockEntryRequestPacket.class, this::lockEntryRequestPacket)
				.when(UnlockEntryRequestPacket.class, this::unlockEntryRequestPacket)
				.when(AddEntryRequestPacket.class, this::addEntryRequestPacket)
				.when(EditEntryRequestPacket.class, this::editEntryRequestPacket)
				.when(RemoveEntryRequestPacket.class, this::removeEntryRequestPacket)
				.when(GetEntryRequestPacket.class, this::getEntryRequestPacket)
				.when(GetEntryNBTRequestPacket.class, this::getEntryNBTRequestPacket)
				.when(GetEntriesRequestPacket.class, this::getEntriesRequestPacket)
				.when(LockTagRequestPacket.class, this::lockTagRequestPacket)
				.when(UnlockTagRequestPacket.class, this::unlockTagRequestPacket)
				.when(AddTagRequestPacket.class, this::addTagRequestPacket)
				.when(EditTagRequestPacket.class, this::editTagRequestPacket)
				.when(RemoveTagRequestPacket.class, this::removeTagRequestPacket)
				.when(GetTagRequestPacket.class, this::getTagRequestPacket)
				.when(GetTagsRequestPacket.class, this::getTagsRequestPacket)
				.when(AddTagToEntryRequestPacket.class, this::addTagToEntryRequestPacket)
				.when(RemoveTagFromEntryRequestPacket.class, this::removeTagFromEntryRequestPacket);
		
		WebsiteHandler websiteHandler = new WebsiteHandler(database);
		
		EventLoopGroup group = new MultiThreadIoEventLoopGroup(3, NioIoHandler.newFactory());
		ChannelFuture serverFuture = new ServerBootstrap()
				.group(group)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					protected void initChannel(SocketChannel channel) throws Exception {
						channel.closeFuture().addListener(future -> onUserDisconnect(channel));
						channel.pipeline().addLast(NettyByteMultiplexer.builder()
								.addProtocol(new MagicByteProtocol("nbt", NBTProtocol.MAGIC, true, ctx -> {
									ctx.pipeline().addAfter(
											NBTProtocol.bind(ctx).name(), "nbt#version", new ProtocolVersionHandler(false));
									ctx.pipeline().addAfter("nbt#version", "nbt#login", new ServerLoginHandler(keys,
											user -> onUserConnect(channel, User.fromConnect(channel, user, ClientType.RAW_SOCKET))));
									ctx.pipeline().addAfter("nbt#login", "nbt#handler", nbtHandler);
								}))
								.addProtocol(new HttpByteProtocol(ctx -> {
									ctx.pipeline().addAfter(ctx.name(), "http#codec", new HttpServerCodec());
									ctx.pipeline().addAfter("http#codec", "http#aggregator", new HttpObjectAggregator(65536));
									ctx.pipeline().addAfter("http#aggregator", null,
											NettyMessageMultiplexer.builder(FullHttpRequest.class)
											.addProtocol(new NormalHttpMessageProtocol(ctx2 -> {
												ctx2.pipeline().addAfter(ctx2.name(), "website", websiteHandler);
											}))
											.addProtocol(new WebSocketHttpMessageProtocol(ctx2 -> {
												ctx2.pipeline().addAfter(WebSocketNBTProtocol.bind(ctx2).name(),
														"nbt#version", ProtocolVersionHandler.waitForWebSocket(false));
												ctx2.pipeline().addAfter("nbt#version", "nbt#login", new ServerLoginHandler(keys,
														user -> onUserConnect(channel, User.fromConnect(channel, user, ClientType.WEB_SOCKET))));
												ctx2.pipeline().addAfter("nbt#login", "nbt#handler", nbtHandler);
											}))
											.build());
								}))
								.build());
						channel.pipeline().addLast(new ExceptionHandler(null));
					}
				})
				.bind(port);
		server = NettyUtil.addGroupShutdown(serverFuture, group);
	}
	
	public NBTDatabaseAccess getDatabase() {
		return database;
	}
	
	public int getPort() {
		return port;
	}
	
	private void onUserConnect(Channel channel, User user) {
		try {
			auth.connect(user);
		} catch (AuthorizationServerException e) {
			NBTProtocol.disconnect(channel, e.getMessage());
			return;
		}
		
		users.put(channel, user);
		System.out.println("[Server] Connected: " + user);
	}
	
	private void onUserDisconnect(Channel channel) {
		User user = users.remove(channel);
		if (user == null)
			return;
		System.out.println("[Server] Disconnected: " + user);
		
		configLock.disconnect(channel);
		entryLocks.disconnect(channel);
		tagLocks.disconnect(channel);
	}
	
	private <T> void respond(Packet packet, Channel channel, CompletableFuture<T> request, Function<T, Packet> packer) {
		FutureUtil.whenCompleteAsync(request, (value, e) -> {
			if (e != null)
				NBTProtocol.reply(channel, packet, ServerException.from(e, true).toPacket());
			else
				NBTProtocol.reply(channel, packet, packer.apply(value));
		}, channel.eventLoop());
	}
	
	private <I extends Packet, O> void checkAuthAndRespond(I packet, Channel channel,
			AuthorizationCheck<I, O> check, Function<I, CompletableFuture<O>> request, Function<O, Packet> packer) {
		User user = users.get(channel);
		
		if (user == null) {
			NBTProtocol.reply(channel, packet, new InternalServerExceptionPacket());
			return;
		}
		
		I checkedPacket;
		try {
			checkedPacket = check.checkRequest(user, packet);
		} catch (AuthorizationServerException e) {
			NBTProtocol.reply(channel, packet, e.toPacket());
			return;
		}
		
		CompletableFuture<O> requestFuture;
		
		ServerLock lock = check.getLock(configLock, entryLocks, tagLocks, user, checkedPacket);
		if (lock == null) {
			requestFuture = checkRequestDuringLockAndRun(database, check, request, user, checkedPacket);
		} else {
			requestFuture = lock.serverLockDuring(channel,
					() -> checkRequestDuringLockAndRun(database, check, request, user, checkedPacket));
		}
		
		respond(packet, channel, FutureUtil.thenApply(requestFuture, value -> check.checkResponse(user, value)), packer);
	}
	private static <I extends Packet, O> CompletableFuture<O> checkRequestDuringLockAndRun(NBTDatabaseAccess database,
			AuthorizationCheck<I, O> check, Function<I, CompletableFuture<O>> request, User user, I checkedPacket) {
		CompletableFuture<I> checkFuture = check.checkRequestDuringLock(database, user, checkedPacket);
		if (checkFuture == null)
			return request.apply(checkedPacket);
		return FutureUtil.thenCompose(checkFuture, request::apply);
	}
	
	private <I extends Packet> void checkAuthAndRespondVoid(I packet, Channel channel,
			AuthorizationCheck<I, Void> check, Function<I, CompletableFuture<Void>> request) {
		checkAuthAndRespond(packet, channel, check, request, v -> new SuccessPacket());
	}
	
	private void lockConfigRequestPacket(LockConfigRequestPacket packet, Channel channel) {
		checkAuthAndRespondVoid(packet, channel, auth.lockConfig(),
				request -> configLock.clientLock(channel));
	}
	
	private void unlockConfigRequestPacket(UnlockConfigRequestPacket packet, Channel channel) {
		checkAuthAndRespondVoid(packet, channel, auth.unlockConfig(),
				request -> configLock.clientUnlock(channel));
	}
	
	private void setConfigRequestPacket(SetConfigRequestPacket packet, Channel channel) {
		checkAuthAndRespondVoid(packet, channel, auth.setConfig(),
				request -> database.setConfig(request.getConfig()));
	}
	
	private void getConfigRequestPacket(GetConfigRequestPacket packet, Channel channel) {
		checkAuthAndRespond(packet, channel, auth.getConfig(),
				request -> database.getConfig(), ConfigPacket::new);
	}
	
	private void lockEntryRequestPacket(LockEntryRequestPacket packet, Channel channel) {
		checkAuthAndRespondVoid(packet, channel, auth.lockEntry(),
				request -> entryLocks.clientLock(request.getId(), channel));
	}
	
	private void unlockEntryRequestPacket(UnlockEntryRequestPacket packet, Channel channel) {
		checkAuthAndRespondVoid(packet, channel, auth.unlockEntry(),
				request -> entryLocks.clientUnlock(request.getId(), channel));
	}
	
	private void addEntryRequestPacket(AddEntryRequestPacket packet, Channel channel) {
		checkAuthAndRespond(packet, channel, auth.addEntry(),
				request -> database.addEntry(request.getName(), request.getNbt(), request.getType(), request.getDataVersion(),
						request.getAuthorUuid(), request.getAuthorUsername(), request.isVerified()), EntryIdPacket::new);
	}
	
	private void editEntryRequestPacket(EditEntryRequestPacket packet, Channel channel) {
		checkAuthAndRespondVoid(packet, channel, auth.editEntry(),
				request -> database.editEntry(request.getId(), request.getName(), request.getNbt(), request.getType(),
						request.getDataVersion(), request.getAuthorUuid(), request.getAuthorUsername(), request.isVerified()));
	}
	
	private void removeEntryRequestPacket(RemoveEntryRequestPacket packet, Channel channel) {
		checkAuthAndRespondVoid(packet, channel, auth.removeEntry(),
				request -> database.removeEntry(request.getId()));
	}
	
	private void getEntryRequestPacket(GetEntryRequestPacket packet, Channel channel) {
		checkAuthAndRespond(packet, channel, auth.getEntry(),
				request -> database.getEntry(request.getId()), EntriesPacket::new);
	}
	
	private void getEntryNBTRequestPacket(GetEntryNBTRequestPacket packet, Channel channel) {
		checkAuthAndRespond(packet, channel, auth.getEntryNBT(),
				request -> database.getEntryNBT(request.getId()), EntryNBTPacket::new);
	}
	
	private void getEntriesRequestPacket(GetEntriesRequestPacket packet, Channel channel) {
		checkAuthAndRespond(packet, channel, auth.getEntries(),
				request -> database.getEntries(request.getFilter(), request.getView()), EntriesPacket::new);
	}
	
	private void lockTagRequestPacket(LockTagRequestPacket packet, Channel channel) {
		checkAuthAndRespondVoid(packet, channel, auth.lockTag(),
				request -> tagLocks.clientLock(request.getName(), channel));
	}
	
	private void unlockTagRequestPacket(UnlockTagRequestPacket packet, Channel channel) {
		checkAuthAndRespondVoid(packet, channel, auth.unlockTag(),
				request -> tagLocks.clientUnlock(request.getName(), channel));
	}
	
	private void addTagRequestPacket(AddTagRequestPacket packet, Channel channel) {
		checkAuthAndRespondVoid(packet, channel, auth.addTag(),
				request -> database.addTag(request.getName(), request.getColor()));
	}
	
	private void editTagRequestPacket(EditTagRequestPacket packet, Channel channel) {
		checkAuthAndRespondVoid(packet, channel, auth.editTag(),
				request -> database.editTag(request.getCurrentName(), request.getName(), request.getColor()));
	}
	
	private void removeTagRequestPacket(RemoveTagRequestPacket packet, Channel channel) {
		checkAuthAndRespondVoid(packet, channel, auth.removeTag(),
				request -> database.removeTag(request.getName()));
	}
	
	private void getTagRequestPacket(GetTagRequestPacket packet, Channel channel) {
		checkAuthAndRespond(packet, channel, auth.getTag(),
				request -> database.getTag(request.getName()), TagsPacket::new);
	}
	
	private void getTagsRequestPacket(GetTagsRequestPacket packet, Channel channel) {
		checkAuthAndRespond(packet, channel, auth.getTags(),
				request -> database.getTags(request.getFilter()), TagsPacket::new);
	}
	
	private void addTagToEntryRequestPacket(AddTagToEntryRequestPacket packet, Channel channel) {
		checkAuthAndRespondVoid(packet, channel, auth.addTagToEntry(),
				request -> database.addTagToEntry(request.getEntry(), request.getTag()));
	}
	
	private void removeTagFromEntryRequestPacket(RemoveTagFromEntryRequestPacket packet, Channel channel) {
		checkAuthAndRespondVoid(packet, channel, auth.removeTagFromEntry(),
				request -> database.removeTagFromEntry(request.getEntry(), request.getTag()));
	}
	
	@Override
	public CompletableFuture<Void> getCloseFuture() {
		return closeFuture;
	}
	
	@Override
	public CompletableFuture<Void> closeAsync() {
		closeFuture.complete(null);
		return NettyUtil.toJava(server.close());
	}
	
	@Override
	public void close() throws IOException, InterruptedException {
		closeFuture.complete(null);
		NettyUtil.awaitClose(server.close());
	}
	
}
