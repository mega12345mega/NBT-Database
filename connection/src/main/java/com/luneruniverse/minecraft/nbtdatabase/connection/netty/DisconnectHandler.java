package com.luneruniverse.minecraft.nbtdatabase.connection.netty;

import com.luneruniverse.minecraft.nbtdatabase.connection.DisconnectException;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.DisconnectPacket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class DisconnectHandler extends SimpleChannelInboundHandler<DisconnectPacket> {
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DisconnectPacket msg) throws Exception {
		throw new DisconnectException(msg.getMessage() == null ? "Disconnected by server" : msg.getMessage());
	}
	
}
