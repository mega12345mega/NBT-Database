package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import io.netty.channel.Channel;

public class Packet {
	
	private transient List<BiConsumer<Packet, Channel>> responseListeners;
	private transient Packet replyTo;
	
	public Packet() {}
	
	public Packet addResponseListener(BiConsumer<Packet, Channel> listener) {
		if (responseListeners == null)
			responseListeners = new ArrayList<>();
		responseListeners.add(listener);
		return this;
	}
	
	public BiConsumer<Packet, Channel> getResponseListener() {
		if (responseListeners == null)
			return null;
		return (packet, channel) -> responseListeners.forEach(listener -> listener.accept(packet, channel));
	}
	
	public Packet replyTo(Packet packet) {
		replyTo = packet;
		return this;
	}
	
	public Packet getReplyTo() {
		return replyTo;
	}
	
}
