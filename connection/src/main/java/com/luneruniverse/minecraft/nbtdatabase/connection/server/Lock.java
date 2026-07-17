package com.luneruniverse.minecraft.nbtdatabase.connection.server;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.luneruniverse.minecraft.nbtdatabase.connection.access.NBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.ServerException;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.FutureUtil;
import com.luneruniverse.minecraft.nbtdatabase.request.IllegalRequestException;

import io.netty.channel.Channel;

public class Lock implements ServerLock {
	
	private static class LockBatch {
		
		public static LockBatch forClientLock(Channel channel, CompletableFuture<Void> lockFuture) {
			return new LockBatch(channel, true, lockFuture, new ArrayDeque<>(), null);
		}
		public static LockBatch forServerLock(Channel channel, CompletableFuture<Void> serverLockFuture) {
			return new LockBatch(channel, false, new CompletableFuture<>(),
					new ArrayDeque<>(Arrays.asList(serverLockFuture)), new CompletableFuture<>());
		}
		
		private final Channel channel;
		private final boolean isClientLock;
		private final CompletableFuture<Void> lockFuture;
		private final Queue<CompletableFuture<Void>> serverLockFutures;
		private CompletableFuture<Void> unlockFuture;
		
		private LockBatch(Channel channel, boolean isClientLock, CompletableFuture<Void> lockFuture,
				Queue<CompletableFuture<Void>> serverLockFutures, CompletableFuture<Void> unlockFuture) {
			this.channel = channel;
			this.isClientLock = isClientLock;
			this.lockFuture = lockFuture;
			this.serverLockFutures = serverLockFutures;
			this.unlockFuture = unlockFuture;
		}
		
	}
	
	public static Lock forConfig(NBTDatabaseAccess access) {
		return new Lock(access::lockConfig, access::unlockConfig,
				"Config already locked", "Config already unlocked");
	}
	public static Lock forEntry(NBTDatabaseAccess access, long id) {
		return new Lock(() -> access.lockEntry(id), () -> access.unlockEntry(id),
				"Entry already locked: " + id, "Entry already unlocked: " + id);
	}
	public static Lock forTag(NBTDatabaseAccess access, String name) {
		return new Lock(() -> access.lockTag(name), () -> access.unlockTag(name),
				"Tag already locked: " + name, "Tag already unlocked: " + name);
	}
	
	private final Supplier<CompletableFuture<Void>> lockAccess;
	private final Supplier<CompletableFuture<Void>> unlockAccess;
	private final String alreadyLockedMsg;
	private final String alreadyUnlockedMsg;
	private final Queue<LockBatch> locks;
	private final Map<Channel, LockBatch> unclosedLocks;
	
	private Lock(Supplier<CompletableFuture<Void>> lockAccess, Supplier<CompletableFuture<Void>> unlockAccess,
			String alreadyLockedMsg, String alreadyUnlockedMsg) {
		this.lockAccess = lockAccess;
		this.unlockAccess = unlockAccess;
		this.alreadyLockedMsg = alreadyLockedMsg;
		this.alreadyUnlockedMsg = alreadyUnlockedMsg;
		this.locks = new ArrayDeque<>();
		this.unclosedLocks = new HashMap<>();
	}
	
	public synchronized Channel getCurrentLockHolder() {
		LockBatch batch = locks.peek();
		if (batch == null)
			return null;
		return batch.channel;
	}
	
	public synchronized CompletableFuture<Void> clientLock(Channel channel) {
		if (unclosedLocks.containsKey(channel))
			return FutureUtil.failedFuture(new IllegalRequestException(alreadyLockedMsg));
		
		CompletableFuture<Void> future = new CompletableFuture<>();
		LockBatch batch = LockBatch.forClientLock(channel, future);
		
		locks.add(batch);
		unclosedLocks.put(channel, batch);
		
		if (locks.size() == 1)
			startLock();
		
		return future;
	}
	
	public synchronized CompletableFuture<Void> clientUnlock(Channel channel) {
		LockBatch batch = unclosedLocks.get(channel);
		
		if (batch == null)
			return FutureUtil.failedFuture(new IllegalRequestException(alreadyUnlockedMsg));
		
		CompletableFuture<Void> future = new CompletableFuture<>();
		batch.unlockFuture = future;
		
		unclosedLocks.remove(channel);
		
		if (locks.peek() == batch && batch.serverLockFutures.isEmpty() && batch.lockFuture.isDone())
			finishLock();
		
		return future;
	}
	
	@Override
	public <T> CompletableFuture<T> serverLockDuring(Channel channel, Supplier<CompletableFuture<T>> future) {
		return FutureUtil.thenCompose(serverLock(channel), v -> FutureUtil.finallyDo(future.get(), this::serverUnlock));
	}
	
	private synchronized CompletableFuture<Void> serverLock(Channel channel) {
		LockBatch batch = unclosedLocks.get(channel);
		CompletableFuture<Void> future = new CompletableFuture<>();
		
		if (batch == null) {
			locks.add(LockBatch.forServerLock(channel, future));
			
			if (locks.size() == 1)
				startLock();
		} else {
			if (batch.lockFuture.isCompletedExceptionally()) {
				future.completeExceptionally(createServerLockException(FutureUtil.exceptionNow(batch.lockFuture), true));
			} else {
				batch.serverLockFutures.add(future);
				
				if (locks.peek() == batch && batch.serverLockFutures.size() == 1 && batch.lockFuture.isDone())
					future.complete(null);
			}
		}
		
		return future;
	}
	
	private synchronized void serverUnlock() {
		LockBatch batch = locks.peek();
		
		batch.serverLockFutures.remove();
		
		if (batch.serverLockFutures.isEmpty()) {
			if (batch.unlockFuture != null)
				finishLock();
		} else {
			batch.serverLockFutures.peek().complete(null);
		}
	}
	
	public synchronized void disconnect(Channel channel) {
		if (locks.isEmpty())
			return;
		
		boolean finishLock = (locks.peek().channel == channel);
		if (finishLock)
			unlockAccess.get();
		
		locks.removeIf(batch -> batch.channel == channel);
		unclosedLocks.remove(channel);
		
		if (finishLock && !locks.isEmpty())
			startLock();
	}
	
	private Throwable createServerLockException(Throwable lockException, boolean isClientLock) {
		if (!isClientLock)
			return lockException;
		
		return new IllegalRequestException(
				"Already failed to lock: " + ServerException.from(lockException, false).getMessage());
	}
	
	private void startLock() {
		LockBatch batch = locks.peek();
		
		CompletableFuture<Void> accessFuture = lockAccess.get();
		accessFuture.whenComplete((v, e) -> {
			synchronized (this) {
				FutureUtil.transferResult(accessFuture, batch.lockFuture);
				
				if (e == null) {
					if (!batch.serverLockFutures.isEmpty()) {
						batch.serverLockFutures.peek().complete(null);
						return;
					}
				} else {
					Throwable serverLockException = createServerLockException(e, batch.isClientLock);
					while (!batch.serverLockFutures.isEmpty())
						batch.serverLockFutures.remove().completeExceptionally(serverLockException);
				}
				
				if (batch.unlockFuture != null)
					finishLock();
			}
		});
	}
	
	private void finishLock() {
		FutureUtil.transferResult(unlockAccess.get(), locks.remove().unlockFuture);
		
		if (!locks.isEmpty())
			startLock();
	}
	
}
