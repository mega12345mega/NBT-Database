package com.luneruniverse.minecraft.nbtdatabase.connection.netty;

import com.luneruniverse.minecraft.nbtdatabase.connection.DisconnectException;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.ProtocolVersionPacket;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ProtocolVersionHandler extends SimpleChannelInboundHandler<Packet> {
	
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
