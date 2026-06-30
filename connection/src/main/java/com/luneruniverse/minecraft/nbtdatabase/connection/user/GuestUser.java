package com.luneruniverse.minecraft.nbtdatabase.connection.user;

import java.util.Optional;

public class GuestUser extends User {
	
	public GuestUser(ClientType clientType, String ip) {
		super(clientType, ip);
	}
	
	@Override
	public boolean isLoggedIn() {
		return false;
	}
	
	@Override
	public Optional<LoggedInUser> asLoggedInUser() {
		return Optional.empty();
	}
	
	@Override
	public String toString() {
		return "<guest>";
	}
	
}
