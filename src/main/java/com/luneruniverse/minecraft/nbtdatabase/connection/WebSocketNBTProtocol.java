package com.luneruniverse.minecraft.nbtdatabase.connection;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class WebSocketNBTProtocol {
	
	public static ChannelHandlerContext bind(ChannelHandlerContext ctx) {
		ctx.pipeline().addAfter(ctx.name(), "nbt#websocket#protocol", new WebSocketServerProtocolHandler("/"));
		ctx.pipeline().addAfter("nbt#websocket#protocol", "nbt#websocket#decoder", new Decoder());
		ctx.pipeline().addAfter("nbt#websocket#decoder", "nbt#websocket#encoder", new Encoder());
		return NBTProtocol.bind(ctx.pipeline().context("nbt#websocket#encoder"));
	}
	
	private static class Decoder extends SimpleChannelInboundHandler<WebSocketFrame> {
		@Override
		protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {
			if (msg instanceof BinaryWebSocketFrame || msg instanceof TextWebSocketFrame ||
					msg instanceof ContinuationWebSocketFrame) {
				ctx.fireChannelRead(msg.content().retain());
			}
		}
	}
	
	private static class Encoder extends ChannelOutboundHandlerAdapter {
		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			if (!(msg instanceof ByteBuf)) {
				ctx.write(msg, promise);
				return;
			}
			
			ctx.write(new BinaryWebSocketFrame((ByteBuf) msg), promise);
		}
	}
	
}
