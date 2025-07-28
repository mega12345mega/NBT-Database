package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.util.Optional;
import java.util.UUID;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;

public class LoginPacket extends Packet {
	
	public static class User {
		private @NotNull UUID uuid;
		private @NotNull String username;
		
		public User(UUID uuid, String username) {
			this.uuid = uuid;
			this.username = username;
		}
		User() {
			// Deserialization
		}
		
		public UUID getUuid() {
			return uuid;
		}
		public String getUsername() {
			return username;
		}
	}
	
	private @NotNull Optional<User> user;
	private @NotNull byte[] encryptedSharedKey;
	private @NotNull byte[] encryptedChallenge;
	
	public LoginPacket(Optional<User> user, byte[] encryptedSharedKey, byte[] encryptedChallenge) {
		this.user = user;
		this.encryptedSharedKey = encryptedSharedKey;
		this.encryptedChallenge = encryptedChallenge;
	}
	LoginPacket() {
		// Deserialization
	}
	
	public Optional<User> getUser() {
		return user;
	}
	public byte[] getEncryptedSharedKey() {
		return encryptedSharedKey;
	}
	public byte[] getEncryptedChallenge() {
		return encryptedChallenge;
	}
	
}
