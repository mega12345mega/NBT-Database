package com.luneruniverse.minecraft.nbtdatabase.connection.user;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.crypto.SecretKey;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.FutureUtil;

import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.constants.ContentTypes;
import net.lenni0451.commons.httpclient.content.impl.StringContent;
import net.lenni0451.commons.httpclient.utils.URLWrapper;

public class MojangAuth {
	
	private static final byte[] BASE_SERVER_ID = "nbt".getBytes(StandardCharsets.ISO_8859_1);
	
	public static String generateServerId(PublicKey publicKey, SecretKey sharedKey) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		digest.update(BASE_SERVER_ID);
		digest.update(sharedKey.getEncoded());
		digest.update(publicKey.getEncoded());
		return new BigInteger(digest.digest()).toString(16);
	}
	
	private static final HttpClient CLIENT = new HttpClient();
	private static final Gson GSON = new Gson();
	private static final String JOIN_URL = "https://sessionserver.mojang.com/session/minecraft/join";
	private static final String HAS_JOINED_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined";
	
	public static void join(String serverId, UUID uuid, String accessToken) throws IOException {
		JsonObject request = new JsonObject();
		request.addProperty("serverId", serverId);
		request.addProperty("selectedProfile", uuid.toString().replace("-", ""));
		request.addProperty("accessToken", accessToken);
		
		HttpResponse response = CLIENT.execute(CLIENT.post(JOIN_URL)
				.setContent(new StringContent(ContentTypes.APPLICATION_JSON, GSON.toJson(request))));
		
		if (response.getStatusCode() != 204) {
			throw new IOException(
					"Join request failed with status code " + response.getStatusCode() + " " + response.getStatusMessage());
		}
	}
	
	public static boolean hasJoined(String serverId, UUID uuid, String username) throws IOException {
		if (!username.matches("\\w{3,16}"))
			return false;
		
		HttpResponse response = CLIENT.execute(CLIENT.get(URLWrapper.ofURL(HAS_JOINED_URL)
				.wrapQueryParameters().addParameter("serverId", serverId).addParameter("username", username).apply().toURL()));
		
		if (response.getStatusCode() != 200)
			return false;
		
		try {
			JsonObject obj = GSON.fromJson(response.getContent().getAsString(), JsonObject.class);
			JsonElement id = obj.get("id");
			return id != null && id.isJsonPrimitive() && id.getAsJsonPrimitive().isString() &&
					id.getAsString().equals(uuid.toString().replace("-", ""));
		} catch (JsonParseException e) {
			return false;
		}
	}
	
	public static CompletableFuture<Void> joinAsync(String serverId, UUID uuid, String accessToken) {
		return FutureUtil.runAsync(() -> join(serverId, uuid, accessToken), FutureUtil.DAEMON_EXECUTOR);
	}
	
	public static CompletableFuture<Boolean> hasJoinedAsync(String serverId, UUID uuid, String username) {
		return FutureUtil.supplyAsync(() -> hasJoined(serverId, uuid, username), FutureUtil.DAEMON_EXECUTOR);
	}
	
}
