package com.luneruniverse.minecraft.nbtdatabase.connection.netty;

import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.DisconnectException;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.login.ProtocolVersionPacket;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.ReferenceCountUtil;

public class ProtocolVersionHandler extends WaitHandler {
	
	public static ProtocolVersionHandler forNbtClient() {
		return new ProtocolVersionHandler();
	}
	
	public static ProtocolVersionHandler forNbtServer() {
		ProtocolVersionHandler handler = new ProtocolVersionHandler();
		handler.dontSendMagic();
		return handler;
	}
	
	public static ProtocolVersionHandler forWebSocketServer() {
		return new ProtocolVersionHandler() {
			@Override
			public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
				if (msg instanceof FullHttpResponse) {
					// Forward WebSocket handshake response
					// https://github.com/netty/netty/issues/17141
					ctx.write(msg, promise);
					ctx.flush();
					return;
				}
				
				super.write(ctx, msg, promise);
			}
			@Override
			public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
				if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete)
					dontSendMagic();
				
				ctx.fireUserEventTriggered(evt);
			}
		};
	}
	
	private Runnable magicQueued;
	private boolean flushQueued;
	private ChannelHandlerContext ctx;
	private boolean magicHandled;
	private boolean versionMatched;
	private String versionDidntMatchReason;
	
	private ProtocolVersionHandler() {
		super(false);
	}
	
	public void sendMagic() {
		if (magicHandled || magicQueued != null)
			throw new IllegalStateException("Magic already handled!");
		
		if (ctx == null) {
			magicQueued = this::sendMagic;
			return;
		}
		
		ctx.write(Unpooled.copiedBuffer(NBTProtocol.MAGIC))
				.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
				.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
		tryFlush();
		
		magicHandled();
	}
	public void dontSendMagic() {
		if (magicHandled || magicQueued != null)
			throw new IllegalStateException("Magic already handled!");
		
		if (ctx == null) {
			magicQueued = this::dontSendMagic;
			return;
		}
		
		magicHandled();
	}
	
	private void magicHandled() {
		magicHandled = true;
		
		ctx.write(new ProtocolVersionPacket(NBTProtocol.PROTOCOL_VERSION))
				.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
				.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
		tryFlush();
		
		tryFinish();
	}
	private void versionMatched() {
		versionMatched = true;
		tryFinish();
	}
	private void versionDidntMatch(String reason) {
		versionDidntMatchReason = reason;
		tryFinish();
	}
	
	private void tryFlush() {
		if (ctx.channel().isActive())
			ctx.flush();
		else
			flushQueued = true;
	}
	private void tryFinish() {
		if (magicHandled && (versionMatched || versionDidntMatchReason != null)) {
			if (versionMatched)
				ctx.pipeline().remove(this);
			else
				ctx.fireExceptionCaught(new DisconnectException(versionDidntMatchReason));
		}
	}
	
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		this.ctx = ctx;
		
		if (magicQueued != null) {
			Runnable magicQueued = this.magicQueued;
			this.magicQueued = null;
			magicQueued.run();
		}
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		if (flushQueued) {
			flushQueued = false;
			ctx.flush();
		}
		
		ctx.fireChannelActive();
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (versionMatched) {
			super.channelRead(ctx, msg);
			return;
		}
		
		if (versionDidntMatchReason != null) {
			ReferenceCountUtil.release(msg);
			return;
		}
		
		try {
			String failReason = null;
			if (msg instanceof ProtocolVersionPacket) {
				int version = ((ProtocolVersionPacket) msg).getVersion();
				if (version != NBTProtocol.PROTOCOL_VERSION)
					failReason = "Protocol version " + version + " doesn't match " + NBTProtocol.PROTOCOL_VERSION;
			} else {
				failReason = "Protocol version <unknown> doesn't match " + NBTProtocol.PROTOCOL_VERSION;
			}
			
			if (failReason == null) {
				versionMatched();
				return;
			}
			
			setDiscard(true);
			versionDidntMatch(failReason);
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}
	
}
