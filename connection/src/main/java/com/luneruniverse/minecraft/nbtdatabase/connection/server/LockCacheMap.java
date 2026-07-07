package com.luneruniverse.minecraft.nbtdatabase.connection.server;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import com.luneruniverse.minecraft.nbtdatabase.connection.access.NBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.FutureUtil;

import io.netty.channel.Channel;

public class LockCacheMap<K> {
	
	private static class ReferenceQueuePoller<K> extends Thread {
		
		private final Map<K, SoftReference<Lock>> keyToRefMap;
		private final Map<SoftReference<Lock>, K> refToKeyMap;
		private final ReferenceQueue<Object> refQueue;
		private final PhantomReference<LockCacheMap<K>> cacheRef;
		
		public ReferenceQueuePoller(LockCacheMap<K> cache) {
			this.keyToRefMap = cache.keyToRefMap;
			this.refToKeyMap = cache.refToKeyMap;
			this.refQueue = cache.refQueue;
			this.cacheRef = new PhantomReference<>(cache, refQueue);
			
			setDaemon(true);
		}
		
		@Override
		public void run() {
			while (true) {
				Reference<? extends Object> ref;
				try {
					ref = refQueue.remove();
				} catch (InterruptedException e) {
					return;
				}
				
				if (ref == cacheRef)
					return;
				
				keyToRefMap.remove(refToKeyMap.remove(ref));
			}
		}
		
	}
	
	public static LockCacheMap<Long> forEntries(NBTDatabaseAccess access) {
		return new LockCacheMap<>(id -> Lock.forEntry(access, id));
	}
	public static LockCacheMap<String> forTags(NBTDatabaseAccess access) {
		return new LockCacheMap<>(name -> Lock.forTag(access, name));
	}
	
	private final Function<K, Lock> newLock;
	private final Map<K, SoftReference<Lock>> keyToRefMap;
	private final Map<SoftReference<Lock>, K> refToKeyMap;
	private final Set<Lock> lockedLocks;
	private final ReferenceQueue<Object> refQueue;
	
	private LockCacheMap(Function<K, Lock> newLock) {
		this.newLock = newLock;
		this.keyToRefMap = new ConcurrentHashMap<>();
		this.refToKeyMap = new ConcurrentHashMap<>();
		this.lockedLocks = new HashSet<>();
		this.refQueue = new ReferenceQueue<>();
		
		new ReferenceQueuePoller<>(this).start();
	}
	
	private Lock get(K key) {
		SoftReference<Lock> ref = keyToRefMap.get(key);
		if (ref != null) {
			Lock lock = ref.get();
			if (lock != null)
				return lock;
		}
		
		Lock lock = newLock.apply(key);
		ref = new SoftReference<>(lock, refQueue);
		
		keyToRefMap.put(key, ref);
		refToKeyMap.put(ref, key);
		
		return lock;
	}
	
	public synchronized Channel getCurrentLockHolder(K key) {
		SoftReference<Lock> ref = keyToRefMap.get(key);
		if (ref == null)
			return null;
		
		Lock lock = ref.get();
		if (lock == null)
			return null;
		
		return lock.getCurrentLockHolder();
	}
	
	public synchronized CompletableFuture<Void> clientLock(K key, Channel channel) {
		Lock lock = get(key);
		lockedLocks.add(lock);
		return lock.clientLock(channel);
	}
	
	public synchronized CompletableFuture<Void> clientUnlock(K key, Channel channel) {
		Lock lock = get(key);
		return FutureUtil.finallyDo(lock.clientUnlock(channel), () -> {
			synchronized (this) {
				if (lock.getCurrentLockHolder() == null)
					lockedLocks.remove(lock);
			}
		});
	}
	
	public synchronized <T> CompletableFuture<T> serverLockDuring(K key, Channel channel, Supplier<CompletableFuture<T>> future) {
		Lock lock = get(key);
		lockedLocks.add(lock);
		return FutureUtil.finallyDo(lock.serverLockDuring(channel, future), () -> {
			synchronized (this) {
				if (lock.getCurrentLockHolder() == null)
					lockedLocks.remove(lock);
			}
		});
	}
	
	public synchronized void disconnect(Channel channel) {
		for (Iterator<Lock> i = lockedLocks.iterator(); i.hasNext();) {
			Lock lock = i.next();
			
			lock.disconnect(channel);
			if (lock.getCurrentLockHolder() == null)
				i.remove();
		}
	}
	
}
