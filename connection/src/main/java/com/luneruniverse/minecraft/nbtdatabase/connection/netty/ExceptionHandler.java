package com.luneruniverse.minecraft.nbtdatabase.connection.netty;

import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;

import com.luneruniverse.minecraft.nbtdatabase.connection.DisconnectException;
import com.luneruniverse.nettymux.InvalidProtocolException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ExceptionHandler extends ChannelInboundHandlerAdapter {
	
	private final CompletableFuture<Void> closeFuture;
	
	public ExceptionHandler(CompletableFuture<Void> closeFuture) {
		this.closeFuture = closeFuture;
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (closeFuture != null && closeFuture.isDone())
			return;
		
		if (cause instanceof InvalidProtocolException || cause.getCause() instanceof InvalidProtocolException) {
			ctx.close();
			return;
		}
		
		if (cause instanceof GeneralSecurityException) {
			cause.printStackTrace();
			cause = new DisconnectException("Login failed: Encryption failed");
		}
		
		if (cause instanceof DisconnectException) {
			if (closeFuture != null)
				closeFuture.completeExceptionally(cause);
			ctx.close();
			return;
		}
		
		cause.printStackTrace();
	}
	
}
