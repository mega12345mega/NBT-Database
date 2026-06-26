package com.luneruniverse.minecraft.nbtdatabase.connection.netty;

import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.DisconnectException;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.ProtocolVersionPacket;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class ProtocolVersionHandler extends SimpleChannelInboundHandler<Packet> {
	
	public static ChannelHandler waitForWebSocket(boolean sendMagic) {
		return new ChannelInboundHandlerAdapter() {
			@Override
			public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
				if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete)
					ctx.pipeline().replace(this, ctx.name(), new ProtocolVersionHandler(sendMagic));
				
				super.userEventTriggered(ctx, evt);
			}
		};
	}
	
	private final boolean sendMagic;
	private ChannelFuture versionFuture;
	
	public ProtocolVersionHandler(boolean sendMagic) {
		this.sendMagic = sendMagic;
	}
	
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		if (sendMagic)
			ctx.write(Unpooled.copiedBuffer(NBTProtocol.MAGIC)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
		versionFuture = ctx.write(new ProtocolVersionPacket(NBTProtocol.PROTOCOL_VERSION))
				.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
		
		if (ctx.channel().isActive())
			ctx.flush();
	}
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
		ctx.fireChannelActive();
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet msg) throws Exception {
		if (msg instanceof ProtocolVersionPacket && ((ProtocolVersionPacket) msg).getVersion() == NBTProtocol.PROTOCOL_VERSION)
			ctx.pipeline().remove(this);
		else if (versionFuture != null) {
			String disconnectReason = msg instanceof ProtocolVersionPacket ? "Protocol version " +
					((ProtocolVersionPacket) msg).getVersion() + " doesn't match " + NBTProtocol.PROTOCOL_VERSION : null;
			versionFuture.addListener(future -> ctx.pipeline().fireExceptionCaught(new DisconnectException(disconnectReason)));
			versionFuture = null;
		}
	}
	
}
