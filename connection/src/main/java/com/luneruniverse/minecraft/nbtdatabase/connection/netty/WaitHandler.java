package com.luneruniverse.minecraft.nbtdatabase.connection.netty;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

public class WaitHandler extends ChannelDuplexHandler {
	
	private boolean discard;
	private final List<Object> incoming;
	private final List<Map.Entry<Object, ChannelPromise>> outgoing;
	private boolean flush;
	
	public WaitHandler(boolean discard) {
		this.discard = discard;
		this.incoming = new ArrayList<>();
		this.outgoing = new ArrayList<>();
		this.flush = false;
	}
	
	public void setDiscard(boolean discard) {
		this.discard = discard;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		incoming.add(msg);
	}
	
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		outgoing.add(new AbstractMap.SimpleImmutableEntry<>(msg, promise));
	}
	
	@Override
	public void flush(ChannelHandlerContext ctx) throws Exception {
		flush = true;
	}
	
	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		if (discard) {
			for (Object msg : incoming)
				ReferenceCountUtil.release(msg);
			for (Map.Entry<Object, ChannelPromise> msg : outgoing) {
				ReferenceCountUtil.release(msg.getKey());
				msg.getValue().setSuccess();
			}
		} else {
			for (Object msg : incoming)
				ctx.fireChannelRead(msg);
			for (Map.Entry<Object, ChannelPromise> msg : outgoing)
				ctx.write(msg.getKey(), msg.getValue());
			if (flush)
				ctx.flush();
		}
		
		incoming.clear();
		outgoing.clear();
		flush = false;
	}
	
}
