package com.luneruniverse.minecraft.nbtdatabase.connection.user;

import java.util.Optional;
import java.util.UUID;

public class LoggedInUser extends User {
	
	private final UUID uuid;
	private final String username;
	
	public LoggedInUser(ClientType clientType, String ip, UUID uuid, String username) {
		super(clientType, ip);
		this.uuid = uuid;
		this.username = username;
	}
	
	public UUID getUuid() {
		return uuid;
	}
	
	public String getUsername() {
		return username;
	}
	
	@Override
	public boolean isLoggedIn() {
		return true;
	}
	
	@Override
	public Optional<LoggedInUser> asLoggedInUser() {
		return Optional.of(this);
	}
	
	@Override
	public String toString() {
		return username + " (" + uuid + ")";
	}
	
}
