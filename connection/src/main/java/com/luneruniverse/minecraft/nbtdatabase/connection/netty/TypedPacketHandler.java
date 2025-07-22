package com.luneruniverse.minecraft.nbtdatabase.connection.netty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@Sharable
public class TypedPacketHandler extends SimpleChannelInboundHandler<Packet> {
	
	private final Map<Class<? extends Packet>, List<BiConsumer<Packet, Channel>>> listeners;
	
	public TypedPacketHandler() {
		listeners = new HashMap<>();
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Packet> TypedPacketHandler when(Class<T> clazz, BiConsumer<T, Channel> listener) {
		listeners.computeIfAbsent(clazz, key -> new ArrayList<>()).add((BiConsumer<Packet, Channel>) listener);
		return this;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet msg) throws Exception {
		List<BiConsumer<Packet, Channel>> matchedListeners = listeners.entrySet().stream()
				.filter(type -> type.getKey().isAssignableFrom(msg.getClass()))
				.flatMap(entry -> entry.getValue().stream())
				.collect(Collectors.toList());
		
		if (matchedListeners.isEmpty()) {
			ctx.fireChannelRead(msg);
			return;
		}
		
		matchedListeners.forEach(listener -> listener.accept(msg, ctx.channel()));
	}
	
}
