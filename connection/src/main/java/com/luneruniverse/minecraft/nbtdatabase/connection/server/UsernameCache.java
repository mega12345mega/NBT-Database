package com.luneruniverse.minecraft.nbtdatabase.connection.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.FutureUtil;

import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.HttpResponse;

public class UsernameCache {
	
	private static final HttpClient CLIENT = new HttpClient();
	private static final Gson GSON = new Gson();
	private static final String LOOKUP_URL = "https://api.minecraftservices.com/minecraft/profile/lookup/";
	private static final long CACHE_TIME_NANOS = 24 * 60 * 60 * 1_000_000_000L;
	
	private final Map<UUID, CompletableFuture<String>> uuidToUsername;
	private final SortedMap<Long, UUID> timeToUuid;
	
	public UsernameCache() {
		uuidToUsername = new HashMap<>();
		timeToUuid = new TreeMap<>();
	}
	
	private synchronized void clean() {
		Map<Long, UUID> expired = timeToUuid.subMap(Long.MIN_VALUE, System.nanoTime() - CACHE_TIME_NANOS);
		uuidToUsername.keySet().removeAll(expired.values());
		expired.clear();
	}
	
	public synchronized void cache(UUID uuid, String username) {
		clean();
		
		uuidToUsername.put(uuid, CompletableFuture.completedFuture(username));
		timeToUuid.put(System.nanoTime(), uuid);
	}
	
	public synchronized CompletableFuture<String> getUsername(UUID uuid) {
		clean();
		
		CompletableFuture<String> username = uuidToUsername.get(uuid);
		if (username != null) {
			if (username.isCompletedExceptionally())
				uuidToUsername.remove(uuid);
			else
				return username;
		}
		
		username = FutureUtil.supplyAsync(() -> {
			HttpResponse response = CLIENT.execute(CLIENT.get(LOOKUP_URL + uuid));
			
			if (response.getStatusCode() == 404)
				return null;
			
			if (response.getStatusCode() == 200) {
				try {
					JsonObject obj = GSON.fromJson(response.getContent().getAsString(), JsonObject.class);
					JsonElement name = obj.get("name");
					if (name != null && name.isJsonPrimitive() && name.getAsJsonPrimitive().isString())
						return name.getAsString();
				} catch (JsonParseException e) {}
			}
			
			throw new IOException("Mojang API sent unexpected response");
		}, FutureUtil.DAEMON_EXECUTOR);
		
		uuidToUsername.put(uuid, username);
		timeToUuid.put(System.nanoTime(), uuid);
		return username;
	}
	
}
