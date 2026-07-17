package com.luneruniverse.minecraft.nbtdatabase.connection.user;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.crypto.SecretKey;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.FutureUtil;

public class MojangAuth {
	
	private static final byte[] BASE_SERVER_ID = "nbt".getBytes(StandardCharsets.ISO_8859_1);
	
	public static String generateServerId(PublicKey publicKey, SecretKey sharedKey) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		digest.update(BASE_SERVER_ID);
		digest.update(sharedKey.getEncoded());
		digest.update(publicKey.getEncoded());
		return new BigInteger(digest.digest()).toString(16);
	}
	
	private static final Gson GSON = new Gson();
	private static final String JOIN_URL = "https://sessionserver.mojang.com/session/minecraft/join";
	private static final String HAS_JOINED_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined";
	
	public static void join(String serverId, UUID uuid, String accessToken) throws IOException {
		JsonObject request = new JsonObject();
		request.addProperty("serverId", serverId);
		request.addProperty("selectedProfile", uuid.toString().replace("-", ""));
		request.addProperty("accessToken", accessToken);
		
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			client.execute(ClassicRequestBuilder.post(JOIN_URL)
					.setEntity(GSON.toJson(request), ContentType.APPLICATION_JSON).build(), response -> {
				if (response.getCode() >= 400) {
					throw new IOException(
							"Join request failed with status code " + response.getCode() + " " + response.getReasonPhrase());
				}
				return null;
			});
		}
	}
	
	public static boolean hasJoined(String serverId, UUID uuid, String username) throws IOException {
		if (!username.matches("\\w{3,16}"))
			return false;
		
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			return client.execute(ClassicRequestBuilder.get(HAS_JOINED_URL)
					.addParameter("serverId", serverId).addParameter("username", username).build(), response -> {
				if (response.getCode() >= 400 || response.getEntity() == null)
					return false;
				InputStream in = response.getEntity().getContent();
				try {
					JsonObject obj = new Gson().fromJson(new InputStreamReader(in), JsonObject.class);
					JsonElement id = obj.get("id");
					return id != null && id.isJsonPrimitive() && id.getAsJsonPrimitive().isString() &&
							id.getAsString().equals(uuid.toString().replace("-", ""));
				} catch (JsonParseException e) {
					return false;
				}
			});
		}
	}
	
	public static CompletableFuture<Void> joinAsync(String serverId, UUID uuid, String accessToken) {
		return FutureUtil.runAsync(() -> join(serverId, uuid, accessToken), FutureUtil.DAEMON_EXECUTOR);
	}
	
	public static CompletableFuture<Boolean> hasJoinedAsync(String serverId, UUID uuid, String username) {
		return FutureUtil.supplyAsync(() -> hasJoined(serverId, uuid, username), FutureUtil.DAEMON_EXECUTOR);
	}
	
}
