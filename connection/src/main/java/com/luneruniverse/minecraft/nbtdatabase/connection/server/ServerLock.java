package com.luneruniverse.minecraft.nbtdatabase.connection.server;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import io.netty.channel.Channel;

public interface ServerLock {
	public <T> CompletableFuture<T> serverLockDuring(Channel channel, Supplier<CompletableFuture<T>> future);
	
	public default ServerLock alongWith(ServerLock lock) {
		return new ServerLock() {
			@Override
			public <T> CompletableFuture<T> serverLockDuring(Channel channel, Supplier<CompletableFuture<T>> future) {
				return ServerLock.this.serverLockDuring(channel, () -> lock.serverLockDuring(channel, future));
			}
		};
	}
}
