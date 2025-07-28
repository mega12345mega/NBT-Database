package com.luneruniverse.minecraft.nbtdatabase.connection.netty;

import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class NBTProtocol extends ChannelDuplexHandler {
	
	public static final byte[] MAGIC = "nbt".getBytes(StandardCharsets.US_ASCII);
	public static final int PROTOCOL_VERSION = 2;
	private static final int TIMEOUT = 5000;
	
	public static ChannelHandlerContext bind(ChannelHandlerContext ctx) {
		ctx.pipeline().addAfter(ctx.name(), "nbt#length", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 12, 4));
		ctx.pipeline().addAfter("nbt#length", "nbt#protocol", new NBTProtocol(TIMEOUT));
		return ctx.pipeline().context("nbt#protocol");
	}
	
	public static void send(Channel channel, Packet packet) {
		channel.writeAndFlush(packet).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
	}
	public static void reply(Channel channel, Packet toReply, Packet packet) {
		send(channel, packet.replyTo(toReply));
	}
	public static Packet sendAndGetResponse(Channel channel, Packet packet) throws InterruptedException {
		if (!channel.isOpen())
			return null;
		CompletableFuture<Packet> future = new CompletableFuture<>();
		packet.addResponseListener((response, channel2) -> future.complete(response));
		channel.closeFuture().addListener(closeFuture -> future.complete(null));
		send(channel, packet);
		try {
			return future.get(TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			return null;
		} catch (ExecutionException e) { // Impossible
			e.printStackTrace();
			return null;
		}
	}
	public static Packet replyAndGetResponse(Channel channel, Packet toReply, Packet packet) throws InterruptedException {
		return sendAndGetResponse(channel, packet.replyTo(toReply));
	}
	
	private final int timeout;
	private final WeakHashMap<Packet, Integer> receivedPacketIds;
	private final Map<Integer, Map.Entry<Long, BiConsumer<Packet, Channel>>> responseListeners;
	private int lastPacketId;
	
	public NBTProtocol(int timeout) {
		this.timeout = timeout;
		this.receivedPacketIds = new WeakHashMap<>();
		this.responseListeners = new HashMap<>();
		this.lastPacketId = -1;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (!(msg instanceof ByteBuf)) {
			super.channelRead(ctx, msg);
			return;
		}
		ByteBuf buf = (ByteBuf) msg;
		
		int id;
		int responseId;
		Packet packet;
		try {
			id = buf.readInt();
			responseId = buf.readInt();
			int packetType = buf.readInt();
			if (buf.readInt() != buf.readableBytes())
				throw new DecoderException("Packet length doesn't match available data!");
			packet = Packets.read(packetType, buf);
		} finally {
			buf.release();
		}
		
		receivedPacketIds.put(packet, id);
		if (responseId != -1) {
			Map.Entry<Long, BiConsumer<Packet, Channel>> listener = responseListeners.get(responseId);
			if (listener != null && listener.getKey() != -1 && listener.getKey() < System.currentTimeMillis())
				responseListeners.remove(responseId);
			else if (listener != null)
				listener.getValue().accept(packet, ctx.channel());
		} else
			ctx.fireChannelRead(packet);
	}
	
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (!(msg instanceof Packet)) {
			super.write(ctx, msg, promise);
			return;
		}
		Packet packet = (Packet) msg;
		
		cleanResponseListeners();
		
		int id = ++lastPacketId;
		
		BiConsumer<Packet, Channel> responseListener = packet.getResponseListener();
		if (responseListener != null) {
			long packetTimeout = timeout == -1 ? -1 : System.currentTimeMillis() + timeout;
			responseListeners.put(id, new AbstractMap.SimpleImmutableEntry<>(packetTimeout, responseListener));
		}
		
		ByteBuf buf = Unpooled.buffer();
		buf.writeInt(id);
		buf.writeInt(packet.getReplyTo() == null ? -1 : receivedPacketIds.get(packet.getReplyTo()));
		Packets.write(packet, buf);
		ctx.write(buf, promise);
	}
	
	private void cleanResponseListeners() {
		long time = System.currentTimeMillis();
		responseListeners.entrySet().removeIf(entry ->
			entry.getValue().getKey() != -1 && entry.getValue().getKey() < time);
	}
	
}
