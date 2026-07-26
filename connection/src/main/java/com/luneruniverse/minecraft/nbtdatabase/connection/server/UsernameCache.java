package com.luneruniverse.minecraft.nbtdatabase.connection.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.FutureUtil;

public class UsernameCache {
	
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
			try (CloseableHttpClient client = HttpClients.createDefault()) {
				return client.execute(ClassicRequestBuilder.get(LOOKUP_URL + uuid).build(), response -> {
					if (response.getCode() == 404)
						return null;
					if (response.getCode() == 200 && response.getEntity() != null) {
						InputStream in = response.getEntity().getContent();
						try {
							JsonObject obj = GSON.fromJson(new InputStreamReader(in), JsonObject.class);
							JsonElement name = obj.get("name");
							if (name != null && name.isJsonPrimitive() && name.getAsJsonPrimitive().isString())
								return name.getAsString();
						} catch (JsonParseException e) {}
					}
					throw new IOException("Mojang API sent unexpected response");
				});
			}
		}, FutureUtil.DAEMON_EXECUTOR);
		
		uuidToUsername.put(uuid, username);
		timeToUuid.put(System.nanoTime(), uuid);
		return username;
	}
	
}
