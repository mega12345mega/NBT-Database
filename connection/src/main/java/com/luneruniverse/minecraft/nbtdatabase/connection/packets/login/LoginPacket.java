package com.luneruniverse.minecraft.nbtdatabase.connection.packets.login;

import java.util.Optional;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;
import com.luneruniverse.minecraft.nbtdatabase.connection.user.Profile;

public class LoginPacket extends Packet {
	
	private @NotNull Optional<Profile> profile;
	private @NotNull byte[] encryptedSharedKey;
	private @NotNull byte[] encryptedChallenge;
	
	public LoginPacket(Optional<Profile> profile, byte[] encryptedSharedKey, byte[] encryptedChallenge) {
		this.profile = profile;
		this.encryptedSharedKey = encryptedSharedKey;
		this.encryptedChallenge = encryptedChallenge;
	}
	LoginPacket() {
		// Deserialization
	}
	
	public Optional<Profile> getProfile() {
		return profile;
	}
	public byte[] getEncryptedSharedKey() {
		return encryptedSharedKey;
	}
	public byte[] getEncryptedChallenge() {
		return encryptedChallenge;
	}
	
}
