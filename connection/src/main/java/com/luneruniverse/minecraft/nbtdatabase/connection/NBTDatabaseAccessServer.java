package com.luneruniverse.minecraft.nbtdatabase.connection;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

import com.luneruniverse.minecraft.nbtdatabase.connection.access.NBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.netty.ErrorHandler;
import com.luneruniverse.minecraft.nbtdatabase.connection.netty.NBTProtocol;
import com.luneruniverse.minecraft.nbtdatabase.connection.netty.TypedPacketHandler;
import com.luneruniverse.minecraft.nbtdatabase.connection.netty.WebSocketNBTProtocol;
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
import com.luneruniverse.minecraft.nbtdatabase.request.IllegalRequestException;
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

public class NBTDatabaseAccessServer implements AutoCloseable {
	
	private final NBTDatabaseAccess database;
	private final int port;
	private final Channel server;
	
	public NBTDatabaseAccessServer(NBTDatabaseAccess database, int port) throws IOException, InterruptedException {
		this.database = database;
		this.port = port;
		
		TypedPacketHandler nbtHandler = new TypedPacketHandler()
				.when(ConfigPacket.class, this::configPacket)
				.when(GetConfigRequestPacket.class, this::getConfigRequestPacket)
				.when(AddEntryRequestPacket.class, this::addEntryRequestPacket)
				.when(EditEntryRequestPacket.class, this::editEntryRequestPacket)
				.when(RemoveEntryRequestPacket.class, this::removeEntryRequestPacket)
				.when(GetEntryRequestPacket.class, this::getEntryRequestPacket)
				.when(GetEntryNBTRequestPacket.class, this::getEntryNBTRequestPacket)
				.when(GetEntriesRequestPacket.class, this::getEntriesRequestPacket)
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
						channel.pipeline().addLast(NettyByteMultiplexer.builder()
								.addProtocol(new MagicByteProtocol("nbt", NBTProtocol.MAGIC, true, ctx -> {
									ctx.pipeline().addAfter(NBTProtocol.bind(ctx).name(), "nbt#handler", nbtHandler);
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
												ctx2.pipeline().addAfter(
														WebSocketNBTProtocol.bind(ctx2).name(), "nbt#handler", nbtHandler);
											}))
											.build());
								}))
								.build());
						channel.pipeline().addLast(new ErrorHandler());
					}
				})
				.bind(port);
		server = NettyUtil.addGroupShutdown(serverFuture, group);
	}
	
	public int getPort() {
		return port;
	}
	
	private <T> void respond(Packet packet, Channel channel, CompletableFuture<T> request, Function<T, Packet> packer) {
		request.whenComplete((value, e) -> {
			if (e != null) {
				if (e instanceof IllegalRequestException)
					NBTProtocol.reply(channel, packet, new ServerExceptionPacket(e.getMessage()));
				else {
					e.printStackTrace();
					NBTProtocol.reply(channel, packet, new ServerExceptionPacket("An internal server error occurred"));
				}
			} else
				NBTProtocol.reply(channel, packet, packer.apply(value));
		});
	}
	
	private void respondVoid(Packet packet, Channel channel, CompletableFuture<Void> request) {
		respond(packet, channel, request, v -> new SuccessPacket());
	}
	
	private void configPacket(ConfigPacket packet, Channel channel) {
		respondVoid(packet, channel, database.setConfig(packet.getConfig()));
	}
	
	private void getConfigRequestPacket(GetConfigRequestPacket packet, Channel channel) {
		respond(packet, channel, database.getConfig(), ConfigPacket::new);
	}
	
	private void addEntryRequestPacket(AddEntryRequestPacket packet, Channel channel) {
		respond(packet, channel, database.addEntry(packet.getName(), packet.getNbt(), packet.getType(), packet.getDataVersion(),
				packet.getAuthorUuid(), packet.getAuthorUsername(), packet.isVerified()), EntryIdPacket::new);
	}
	
	private void editEntryRequestPacket(EditEntryRequestPacket packet, Channel channel) {
		respondVoid(packet, channel, database.editEntry(packet.getId(), packet.getName(), packet.getNbt(), packet.getType(),
				packet.getDataVersion(), packet.getAuthorUuid(), packet.getAuthorUsername(), packet.isVerified()));
	}
	
	private void removeEntryRequestPacket(RemoveEntryRequestPacket packet, Channel channel) {
		respondVoid(packet, channel, database.removeEntry(packet.getId()));
	}
	
	private void getEntryRequestPacket(GetEntryRequestPacket packet, Channel channel) {
		respond(packet, channel, database.getEntry(packet.getId()), EntriesPacket::new);
	}
	
	private void getEntryNBTRequestPacket(GetEntryNBTRequestPacket packet, Channel channel) {
		respond(packet, channel, database.getEntryNBT(packet.getId()), EntryNBTPacket::new);
	}
	
	private void getEntriesRequestPacket(GetEntriesRequestPacket packet, Channel channel) {
		respond(packet, channel, database.getEntries(packet.getFilter(), packet.getView()), EntriesPacket::new);
	}
	
	private void addTagRequestPacket(AddTagRequestPacket packet, Channel channel) {
		respondVoid(packet, channel, database.addTag(packet.getName(), packet.getColor()));
	}
	
	private void editTagRequestPacket(EditTagRequestPacket packet, Channel channel) {
		respondVoid(packet, channel, database.editTag(packet.getCurrentName(), packet.getName(), packet.getColor()));
	}
	
	private void removeTagRequestPacket(RemoveTagRequestPacket packet, Channel channel) {
		respondVoid(packet, channel, database.removeTag(packet.getName()));
	}
	
	private void getTagRequestPacket(GetTagRequestPacket packet, Channel channel) {
		respond(packet, channel, database.getTag(packet.getName()), TagsPacket::new);
	}
	
	private void getTagsRequestPacket(GetTagsRequestPacket packet, Channel channel) {
		respond(packet, channel, database.getTags(packet.getFilter()), TagsPacket::new);
	}
	
	private void addTagToEntryRequestPacket(AddTagToEntryRequestPacket packet, Channel channel) {
		respondVoid(packet, channel, database.addTagToEntry(packet.getEntry(), packet.getTag()));
	}
	
	private void removeTagFromEntryRequestPacket(RemoveTagFromEntryRequestPacket packet, Channel channel) {
		respondVoid(packet, channel, database.removeTagFromEntry(packet.getEntry(), packet.getTag()));
	}
	
	public CompletableFuture<Void> closeAsync() {
		return FutureUtil.runAsync(server::close, ForkJoinPool.commonPool());
	}
	
	@Override
	public void close() throws IOException, InterruptedException {
		server.close();
	}
	
}
