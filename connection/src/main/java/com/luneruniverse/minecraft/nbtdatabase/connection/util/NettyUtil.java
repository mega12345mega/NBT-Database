package com.luneruniverse.minecraft.nbtdatabase.connection.util;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;

public class NettyUtil {
	
	public static Channel addGroupShutdown(ChannelFuture future, EventLoopGroup group) throws IOException, InterruptedException {
		boolean success = false;
		try {
			try {
				future.sync();
			} catch (Throwable e) {
				throw new IOException("Failed to start channel", e);
			}
			Channel channel = future.channel();
			channel.closeFuture().addListener(v -> group.shutdownGracefully(100, 100, TimeUnit.MILLISECONDS));
			success = true;
			return channel;
		} finally {
			if (!success)
				group.shutdownGracefully(100, 100, TimeUnit.MILLISECONDS);
		}
	}
	
	public static void println(ByteBuf buf) {
		byte[] bytes = new byte[buf.readableBytes()];
		buf.getBytes(buf.readerIndex(), bytes);
		for (byte b : bytes)
			System.out.print(String.format("%02x ", b));
		System.out.println();
	}
	
}
