package com.luneruniverse.minecraft.nbtdatabase.connection.user;

import java.util.UUID;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;

public final class Profile {
	
	private @NotNull UUID uuid;
	private @NotNull String username;
	
	public Profile(UUID uuid, String username) {
		this.uuid = uuid;
		this.username = username;
	}
	Profile() {
		// Deserialization
	}
	
	public UUID getUuid() {
		return uuid;
	}
	public String getUsername() {
		return username;
	}
	
}
