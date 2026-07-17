package com.luneruniverse.minecraft.nbtdatabase.connection.server.auth;

import java.util.concurrent.CompletableFuture;

import com.luneruniverse.minecraft.nbtdatabase.connection.access.NBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.AuthorizationServerException;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.Lock;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.LockCacheMap;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.ServerLock;
import com.luneruniverse.minecraft.nbtdatabase.connection.user.User;

public interface AuthorizationCheck<I extends Packet, O> {
	public static <I extends Packet, O> AuthorizationCheck<I, O> allow() {
		return new AuthorizationCheck<I, O>() {};
	}
	
	public default I checkRequest(User user, I request) throws AuthorizationServerException {
		return request;
	}
	
	public default ServerLock getLock(Lock configLock, LockCacheMap<Long> entryLocks, LockCacheMap<String> tagLocks, User user, I request) {
		return null;
	}
	public default CompletableFuture<I> checkRequestDuringLock(NBTDatabaseAccess access, User user, I request) {
		return null;
	}
	
	public default O checkResponse(User user, O response) throws AuthorizationServerException {
		return response;
	}
}
